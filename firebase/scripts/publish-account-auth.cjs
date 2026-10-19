'use strict';
// Account rollout authorized by the student. Default is validation only.
// No user/account/document APIs; only rules and the two reviewed Auth config fields.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const crypto = require('node:crypto');
const {spawnSync} = require('node:child_process');
const {tokens, block} = require('./compare-history-rules.cjs');
const {values} = require('node:util').parseArgs({options:{out:{type:'string'},apply:{type:'boolean'}}});
const root = path.resolve(__dirname, '../..');
assert(values.out, 'Use --out NEW_EVIDENCE_DIRECTORY [--apply]');
assert(!process.env.FIRESTORE_EMULATOR_HOST && !process.env.FIREBASE_AUTH_EMULATOR_HOST && !process.env.FIREBASE_TOKEN);
const out = path.resolve(values.out);
assert(!fs.existsSync(out), 'Use a new evidence directory');
const project = 'roadmap-utepsa-c9409';
const expectedBefore = '4fc14fb7e06bf3de81b8791bf67db1942202c191868561a8bfbbe61da8ecfc02';
const expectedCandidate = '06702f203f93a60a485188407cbecae7705038facb74c10e495c67a3342fd870';
const expectedProposal = 'f7596706cb9deaed5121bf084f5333757187dee4977da6e95e0fb1e6fff95217';
const hash = value => crypto.createHash('sha256').update(value).digest('hex');
const rulesPath = path.join(root, 'firebase/firestore.rules');
const configPath = path.join(root, 'firebase.json');
const proposalPath = path.join(root, 'firebase/account-auth-config.proposed.json');
const configHash = hash(fs.readFileSync(configPath));
const proposal = JSON.parse(fs.readFileSync(proposalPath));
const prepared = JSON.parse(fs.readFileSync(path.join(root,'app/build/reports/account-auth/rollout-prepared.json')));
const lib = process.env.ROADMAP_FIREBASE_TOOLS_LIB;
assert(lib, 'Set ROADMAP_FIREBASE_TOOLS_LIB to the reviewed Firebase CLI lib');
assert.equal(require(path.join(lib,'../package.json')).version,'15.34.0');
assert.equal(proposal.project,project);
assert.equal(proposal.updateMask,'signIn.email.enabled,signIn.email.passwordRequired');
assert.deepEqual(proposal.body,{signIn:{email:{enabled:true,passwordRequired:true}}});
const config = JSON.parse(fs.readFileSync(configPath));
assert.equal(config.firestore.database,'(default)');
assert.equal(config.firestore.rules,'firebase/firestore.rules');
const evidence = path.join(root,'app/build/reports/account-auth');
assert(fs.readFileSync(path.join(evidence,'Regressions.txt'),'utf8').includes('OK (68 tests)'));
assert.equal((fs.readFileSync(path.join(evidence,'continuation-rules.txt'),'utf8').match(/^PASS /gm)||[]).length,13);
assert.equal((fs.readFileSync(path.join(evidence,'continuation-rules-regressions.txt'),'utf8').match(/^PASS /gm)||[]).length,6);
fs.mkdirSync(out,{recursive:true});
const report = {project,database:'(default)',startedAt:new Date().toISOString(),
  candidateSha256:expectedCandidate,previousSha256:expectedBefore,authUpdateMask:proposal.updateMask,
  documentReads:0,documentWrites:0,userAccountMutations:0,rulesPublicationAttempted:false,
  authPatchAttempted:false,stage:'preflight',outcome:'preparing'};
const save = () => fs.writeFileSync(path.join(out,'rollout.json'),JSON.stringify(report,null,2));
const safe = value => String(value||'').replace(/(Bearer\s+)[^\s"']+/gi,'$1[REDACTED]')
  .replace(/\bya29\.[A-Za-z0-9_.-]+/g,'[REDACTED]')
  .replace(/((?:access_token|refresh_token|id_token|authorization)["']?\s*[:=]\s*["']?)[^\s,"'}]+/gi,'$1[REDACTED]');
const env = {...process.env,CI:'true'}; delete env.DEBUG; delete env.NODE_DEBUG;
function run(name,args) {
  const result = spawnSync(process.execPath,args,{cwd:root,env,encoding:'utf8',timeout:240000,maxBuffer:8*1024*1024});
  fs.writeFileSync(path.join(out,`${name}.log`),safe(result.stdout)+safe(result.stderr));
  return result.status;
}
function unchanged() {
  assert.equal(hash(fs.readFileSync(rulesPath)),expectedCandidate);
  assert.equal(hash(fs.readFileSync(proposalPath)),expectedProposal);
  assert.equal(hash(fs.readFileSync(configPath)),configHash);
}
function inspect(name) {
  assert.equal(run(name,[path.join(__dirname,'compare-history-rules.cjs'),'--out',path.join(out,name)]),0);
  const read = JSON.parse(fs.readFileSync(path.join(out,name,'comparison.json')));
  assert.equal(read.outcome,'compared'); assert(read.releaseStableDuringRead);
  return read;
}
function preserve(name) {
  const remote = fs.readFileSync(path.join(out,name,'firestore.remote.rules'),'utf8');
  const local = fs.readFileSync(rulesPath,'utf8');
  const header = 'function isOwner(uid)';
  assert.equal(tokens(local.replace(block(local,header),'')),tokens(remote.replace(block(remote,header),'')));
  report.academicContractsPreserved = true;
}
const canonical = value => JSON.stringify(sort(value));
function sort(value) {
  if(Array.isArray(value)) return value.map(sort);
  if(value && typeof value==='object') return Object.fromEntries(Object.keys(value).sort().map(key=>[key,sort(value[key])]));
  return value;
}
function otherConfig(config) {
  const copy = structuredClone(config);
  if(copy.signIn?.email) {
    delete copy.signIn.email.enabled; delete copy.signIn.email.passwordRequired;
    if(Object.keys(copy.signIn.email).length===0) delete copy.signIn.email;
  }
  if(copy.signIn && Object.keys(copy.signIn).length===0) delete copy.signIn;
  return canonical(copy);
}
const cli = path.join(lib,'bin/firebase.js');
const deployArgs = [cli,'deploy','--only','firestore:rules','--project',project,'--config','firebase.json','--non-interactive'];
(async()=>{
  unchanged();
  const before = inspect('before'); preserve('before');
  assert.equal(before.remoteRulesSha256,expectedBefore);
  assert.equal(before.release.rulesetName,prepared.remoteRelease.rulesetName);
  assert.equal(before.release.updateTime,prepared.remoteRelease.updateTime);
  const auth = require(path.join(lib,'auth'));
  const options = {project,nonInteractive:true};
  auth.setActiveAccount(options,auth.getGlobalDefaultAccount());
  await require(path.join(lib,'requireAuth')).requireAuth(options);
  const {Client} = require(path.join(lib,'apiv2'));
  const client = new Client({auth:true,urlPrefix:'https://identitytoolkit.googleapis.com'});
  const authPath = `/admin/v2/projects/${project}/config`;
  const requestOptions = {headers:{'x-goog-user-project':project}};
  const readConfig = async() => (await client.get(authPath,requestOptions)).body;
  report.stage = 'auth-preflight'; save();
  const authBefore = await readConfig();
  assert.equal(authBefore.signIn?.email?.enabled ?? false,false);
  assert.equal(authBefore.signIn?.email?.passwordRequired ?? false,false);
  report.previousEmailConfig = {enabled:false,passwordRequired:false};
  report.otherAuthConfigSha256 = hash(otherConfig(authBefore)); save();
  report.stage = 'dry-run'; save();
  assert.equal(run('dry-run',[...deployArgs,'--dry-run']),0);
  report.outcome = 'validated'; save();
  if(!values.apply) return;
  const immediate = inspect('immediately-before'); preserve('immediately-before'); unchanged();
  assert.equal(immediate.remoteRulesSha256,expectedBefore);
  assert.equal(immediate.release.rulesetName,before.release.rulesetName);
  assert.equal(immediate.release.updateTime,before.release.updateTime);
  assert.equal(canonical(await readConfig()),canonical(authBefore));
  report.stage = 'publish-rules'; report.rulesPublicationAttempted = true; report.outcome = 'requires-reconciliation'; save();
  report.deployExitCode = run('deploy',deployArgs);
  const after = inspect('after');
  assert.equal(after.remoteRulesSha256,expectedCandidate); assert(after.identicalBytes);
  report.rulesVerified = true; report.publishedRelease = after.release; save();
  assert.equal(canonical(await readConfig()),canonical(authBefore)); unchanged();
  report.stage = 'enable-email'; report.authPatchAttempted = true; save();
  await client.patch(authPath,proposal.body,{...requestOptions,queryParams:{updateMask:proposal.updateMask}});
  const authAfter = await readConfig();
  assert.equal(authAfter.signIn.email.enabled,true);
  assert.equal(authAfter.signIn.email.passwordRequired,true);
  assert.equal(otherConfig(authAfter),otherConfig(authBefore));
  report.emailConfig = authAfter.signIn.email;
  report.otherAuthConfigPreserved = true; report.authVerified = true;
  report.stage = 'complete'; report.outcome = 'verified-published-and-enabled';
})().catch(error=>{
  report.failureType = error.constructor.name;
  report.outcome = report.rulesPublicationAttempted || report.authPatchAttempted ? 'requires-reconciliation':'aborted-before-publication';
  process.exitCode = 1;
}).finally(()=>{report.finishedAt=new Date().toISOString();save();console.log(JSON.stringify(report,null,2));});

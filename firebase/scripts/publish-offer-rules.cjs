'use strict';
// Explicitly authorized Offer rollout. Default: inspect and validate only.
// No student document APIs, Auth changes or catalog writes.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const crypto = require('node:crypto');
const {spawnSync} = require('node:child_process');
const {tokens, block} = require('./compare-history-rules.cjs');
const {values} = require('node:util').parseArgs({options:{out:{type:'string'},apply:{type:'boolean'}}});
const root = path.resolve(__dirname, '../..');
assert(values.out, 'Use --out NEW_EVIDENCE_DIRECTORY [--apply]');
assert(!process.env.FIRESTORE_EMULATOR_HOST && !process.env.FIREBASE_TOKEN);
const out = path.resolve(values.out);
assert(!fs.existsSync(out), 'Use a new evidence directory');
const expectedLocal = '4fc14fb7e06bf3de81b8791bf67db1942202c191868561a8bfbbe61da8ecfc02';
const expectedRemote = '7b70043cfb047d57009062fa1f3ebdd8b87f6cc444e90464855cdb37327479dd';
const hash = value => crypto.createHash('sha256').update(value).digest('hex');
const rulesPath = path.join(root, 'firebase/firestore.rules');
const configPath = path.join(root, 'firebase.json');
assert.equal(hash(fs.readFileSync(rulesPath)), expectedLocal);
const configHash = hash(fs.readFileSync(configPath));
const config = JSON.parse(fs.readFileSync(configPath));
assert.equal(config.firestore.database, '(default)');
assert.equal(config.firestore.rules, 'firebase/firestore.rules');
assert.equal(JSON.parse(fs.readFileSync(path.join(root, 'app/build/reports/offer/all-rules.json'))).success, true);
assert.equal(JSON.parse(fs.readFileSync(path.join(root, 'app/build/reports/history-sdk-emulator',
  fs.readdirSync(path.join(root, 'app/build/reports/history-sdk-emulator')).filter(n => /^seed-.*\.json$/.test(n)).sort().at(-1)))).rulesSha256, expectedLocal);
fs.mkdirSync(out, {recursive:true});
const report = {project:'roadmap-utepsa-c9409', database:'(default)', candidateSha256:expectedLocal,
  expectedRemoteSha256:expectedRemote, documentReads:0, documentWrites:0,
  publicationAttempted:false, outcome:'preparing'};
const save = () => fs.writeFileSync(path.join(out, 'rollout.json'), JSON.stringify(report,null,2));
const env = {...process.env, CI:'true'};
delete env.DEBUG; delete env.NODE_DEBUG;
const safe = text => String(text || '').replace(/(Bearer\s+)[^\s"']+/gi, '$1[REDACTED]')
  .replace(/\bya29\.[A-Za-z0-9_.-]+/g, '[REDACTED]')
  .replace(/((?:access_token|refresh_token|id_token|authorization)["']?\s*[:=]\s*["']?)[^\s,"'}]+/gi, '$1[REDACTED]');
function run(name, args) {
  const result = spawnSync(process.execPath,args,{cwd:root,env,encoding:'utf8',timeout:240000,maxBuffer:8*1024*1024});
  fs.writeFileSync(path.join(out, `${name}.log`), safe(result.stdout)+safe(result.stderr));
  return result.status;
}
function unchangedCandidate() {
  assert.equal(hash(fs.readFileSync(rulesPath)),expectedLocal);
  assert.equal(hash(fs.readFileSync(configPath)),configHash);
}
function inspect(name) {
  assert.equal(run(name,[path.join(__dirname,'compare-history-rules.cjs'),'--out',path.join(out,name)]),0);
  const read = JSON.parse(fs.readFileSync(path.join(out,name,'comparison.json')));
  assert.equal(read.outcome,'compared'); assert(read.releaseStableDuringRead);
  return read;
}
function preserveExisting(name) {
  const remote = fs.readFileSync(path.join(out,name,'firestore.remote.rules'),'utf8');
  let stripped = fs.readFileSync(rulesPath,'utf8');
  for (const header of ['function offerOwner(uid)', 'function offerSubjectIds()',
    'function validOffer(data)', 'match /ofertas/actual']) stripped = stripped.replace(block(stripped,header),'');
  assert.equal(tokens(stripped),tokens(remote),'Changes outside the reviewed Offer blocks');
  report.existingRulesPreserved = true;
}
const cli = path.join(root,'app/build/firebase-admin-tools/node_modules/firebase-tools/lib/bin/firebase.js');
const args = [cli,'deploy','--only','firestore:rules','--project',report.project,'--config','firebase.json','--non-interactive'];
try {
  const before = inspect('before');
  assert.equal(before.remoteRulesSha256,expectedRemote);
  preserveExisting('before'); unchangedCandidate();
  assert.equal(run('catalog-check',[path.join(root,'firebase/tests/offer-catalog.test.cjs')]),0);
  assert.equal(run('dry-run',[...args,'--dry-run']),0,'Server validation failed');
  report.outcome = 'validated'; save();
  if (values.apply) {
    const immediate = inspect('immediately-before');
    assert.equal(immediate.remoteRulesSha256,expectedRemote);
    assert.equal(immediate.release.rulesetName,before.release.rulesetName);
    assert.equal(immediate.release.updateTime,before.release.updateTime);
    preserveExisting('immediately-before'); unchangedCandidate();
    report.publicationAttempted = true; report.outcome = 'sent-result-uncertain'; save();
    report.deployExitCode = run('deploy',args);
    const after = inspect('after');
    assert.equal(after.remoteRulesSha256,expectedLocal); assert(after.identicalBytes);
    report.outcome = 'verified-published';
  }
} catch (error) {
  report.outcome = report.publicationAttempted ? 'requires-reconciliation' : 'aborted-before-publication';
  report.failureType = error.constructor.name;
  process.exitCode = 1;
} finally { save(); console.log(JSON.stringify(report)); }

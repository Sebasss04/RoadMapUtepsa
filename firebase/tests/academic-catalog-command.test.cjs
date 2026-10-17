// Command-level tests with a fake administrative transport. Never contact Firebase.
const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { spawnSync } = require('node:child_process');
const { BASE, PROJECT, expectedDocuments } = require('../scripts/academic-catalog-plan.cjs');
const root = path.resolve(__dirname, '../..');
const fixtures = fs.mkdtempSync(path.join(root, 'app/build/catalog-command-test-'));
const statePath = path.join(fixtures, 'state.json');
const lib = path.join(fixtures, 'lib');
fs.mkdirSync(lib);
fs.writeFileSync(path.join(lib, 'auth.js'), 'exports.getGlobalDefaultAccount=()=>null; exports.setActiveAccount=()=>{};');
fs.writeFileSync(path.join(lib, 'requireAuth.js'), 'exports.requireAuth=async()=>{};');
fs.writeFileSync(path.join(lib, 'apiv2.js'), `
const fs=require('node:fs');
const statePath=process.env.ROADMAP_TEST_STATE;
exports.Client=class {
  async get(url) {
    const s=JSON.parse(fs.readFileSync(statePath)); s.calls.push(['GET',url]);
    fs.writeFileSync(statePath,JSON.stringify(s));
    if(url.includes('/releases/'))return {body:{name:'projects/${PROJECT}/releases/cloud.firestore',rulesetName:'projects/${PROJECT}/rulesets/original'}};
    if(url.includes('/rulesets/'))return {body:{source:{files:[{name:'firestore.rules',content:s.rules||'remote-profile-rules'}]}}};
    const db={name:'projects/${PROJECT}/databases/(default)',databaseEdition:'STANDARD',type:'FIRESTORE_NATIVE',locationId:s.region||'southamerica-east1'};
    return {body:url.endsWith('/databases')?{databases:[db]}:db};
  }
  async post(url,body) {
    const s=JSON.parse(fs.readFileSync(statePath)); s.calls.push(['POST',url]);
    fs.writeFileSync(statePath,JSON.stringify(s));
    if(url.endsWith(':beginTransaction'))return {body:{transaction:'test-transaction'}};
    if(url.endsWith(':rollback'))return {body:{}};
    if(url.endsWith(':batchGet'))return {body:Object.entries(s.references).map(([p,fields])=>({found:{name:'${BASE}/'+p,fields}}))};
    if(url.endsWith(':runQuery')) {
      const parent=url.slice(1,-':runQuery'.length).slice('${BASE}'.length+1);
      const prefix=(parent?parent+'/':'')+body.structuredQuery.from[0].collectionId+'/';
      return {body:Object.entries(s.remote).filter(([p])=>p.startsWith(prefix)&&!p.slice(prefix.length).includes('/')).map(([p,fields])=>({document:{name:'${BASE}/'+p,fields}}))};
    }
    if(url.endsWith(':commit')) {
      if(s.failCommit)throw Error('simulated lost commit response');
      for(const w of body.writes) {
        const p=w.update.name.slice('${BASE}'.length+1);
        if(w.currentDocument.exists!==false||Object.hasOwn(s.remote,p))throw Error('overwrite');
        s.remote[p]=w.update.fields;
      }
      s.writes=body.writes;fs.writeFileSync(statePath,JSON.stringify(s));
      return {body:{commitTime:'2026-10-16T09:00:00Z'}};
    }
    throw Error('Unexpected API call '+url);
  }
};`);
function reset(extra = {}) {
  const references = Object.fromEntries(Object.entries(JSON.parse(fs.readFileSync(path.join(root, 'firebase/profile-references.json'))))
    .map(([p,fields]) => [p,Object.fromEntries(Object.entries(fields).map(([k,v])=>[k,{stringValue:v}]))]));
  fs.writeFileSync(statePath, JSON.stringify({ remote: {}, references, calls: [], ...extra }));
}
let sequence = 0;
function run(reviewed) {
  const out = path.join(fixtures, `report-${sequence++}.json`);
  const args = [path.join(root, 'firebase/scripts/academic-catalog.cjs'), '--docs',
    process.env.ROADMAP_DOCS || 'C:/UNIVERSIDAD/DEV APP MOVILES', '--out', out];
  if (reviewed) args.push('--apply', reviewed);
  const result = spawnSync(process.execPath, args, { cwd: root, encoding: 'utf8', env: {
    ...process.env, FIRESTORE_EMULATOR_HOST: '', ROADMAP_FIREBASE_TOOLS_LIB: lib, ROADMAP_TEST_STATE: statePath
  }});
  return { ...result, out, report: fs.existsSync(out) ? JSON.parse(fs.readFileSync(out)) : null,
    state: JSON.parse(fs.readFileSync(statePath)) };
}
test('dry-run transport never sends transaction/commit; approved apply verifies 78 and retry creates zero', () => {
  reset(); const dry = run(); assert.equal(dry.status, 0, dry.stderr);
  assert.equal(dry.report.plan.create.length, 78);
  assert(!dry.state.calls.some(([,url])=>/:(commit|beginTransaction|rollback)$/.test(url)));
  const apply = run(dry.out); assert.equal(apply.status, 0, apply.stderr);
  assert.equal(apply.report.writesCommitted, 78); assert.equal(apply.report.outcome, 'verified');
  assert.equal(apply.report.verification.unchanged.length, 78);
  const retry = run(dry.out); assert.equal(retry.status, 0, retry.stderr);
  assert.equal(retry.report.writesCommitted, 0);
});
test('changed remote rules block apply before transaction or commit', () => {
  reset(); const dry = run(); reset({rules:'changed-by-someone-else'});
  const apply = run(dry.out); assert.equal(apply.status, 1);
  assert.match(apply.stderr, /Remote rules changed/);
  assert(!apply.state.calls.some(([,url])=>url.endsWith(':beginTransaction')||url.endsWith(':commit')));
});
test('conflicting fields block commit and rollback the transaction', () => {
  reset(); const dry=run(); const expected=expectedDocuments(); const subject=Object.keys(expected)[0];
  reset({remote:{[subject]:{...expected[subject],nombre:{stringValue:'unexpected'}}}});
  const apply=run(dry.out); assert.equal(apply.status,1);
  assert.equal(apply.report.plan.conflicts.length,1);
  assert(apply.state.calls.some(([,url])=>url.endsWith(':rollback')));
  assert(!apply.state.calls.some(([,url])=>url.endsWith(':commit')));
});
test('lost commit response is recorded as unconfirmed rather than success', () => {
  reset();const dry=run();reset({failCommit:true});const apply=run(dry.out);
  assert.equal(apply.status,1);assert.equal(apply.report.outcome,'commit-sent-result-unconfirmed');
  assert.match(apply.report.error,/lost commit response/);
});
test('wrong location blocks before any data request', () => {
  reset({region:'us-central1'});const result=run();assert.equal(result.status,1);
  assert(!result.state.calls.some(([method])=>method==='POST'));
});

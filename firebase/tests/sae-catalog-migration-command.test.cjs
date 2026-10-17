// Administrative transport simulation. Never connects to Firebase.
const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { spawnSync } = require('node:child_process');
const { legacyDocuments } = require('../scripts/migrate-sae-catalog.cjs');
const { BASE, PROJECT } = require('../scripts/academic-catalog-plan.cjs');
const root = path.resolve(__dirname, '../..');
const dir = fs.mkdtempSync(path.join(root, 'app/build/sae-migration-command-'));
const lib = path.join(dir, 'lib'); fs.mkdirSync(lib);
const stateFile = path.join(dir, 'state.json');
fs.writeFileSync(path.join(lib, 'auth.js'), 'exports.getGlobalDefaultAccount=()=>null;exports.setActiveAccount=()=>{};');
fs.writeFileSync(path.join(lib, 'requireAuth.js'), 'exports.requireAuth=async()=>{};');
fs.writeFileSync(path.join(lib, 'apiv2.js'), `
const fs=require('node:fs'),assert=require('node:assert/strict');
exports.Client=class {
 async get(url) {return {body:{name:'projects/${PROJECT}/databases/(default)',databaseEdition:'STANDARD',type:'FIRESTORE_NATIVE',locationId:'southamerica-east1'}};}
 async post(url,body) {
  const s=JSON.parse(fs.readFileSync(process.env.SAE_TEST_STATE));s.calls.push(url);
  const save=()=>fs.writeFileSync(process.env.SAE_TEST_STATE,JSON.stringify(s));save();
  if(url.endsWith(':beginTransaction'))return {body:{transaction:'atomic-test'}};
  if(url.endsWith(':rollback'))return {body:{}};
  if(url.endsWith(':runQuery')) {
   const parent=url.slice(1,-':runQuery'.length).slice('${BASE}'.length+1);
   const prefix=(parent?parent+'/':'')+body.structuredQuery.from[0].collectionId+'/';
   return {body:Object.entries(s.remote).filter(([p])=>p.startsWith(prefix)&&!p.slice(prefix.length).includes('/')).map(([p,d])=>({document:{name:'${BASE}/'+p,...d}}))};
  }
  if(url.endsWith(':commit')) {
   assert.equal(body.transaction,'atomic-test');
   for(const w of body.writes) {
    const key=(w.update?.name||w.delete).slice('${BASE}'.length+1);assert(key.startsWith('materias/'));
    if(w.currentDocument.exists===false)assert(!s.remote[key]);
    else assert.equal(s.remote[key].updateTime,w.currentDocument.updateTime);
   }
   for(const w of body.writes) {
    const key=(w.update?.name||w.delete).slice('${BASE}'.length+1);
    if(w.delete)delete s.remote[key];else s.remote[key]={fields:w.update.fields,updateTime:'2026-10-17T18:00:00Z'};
   }
   s.commits=(s.commits||0)+1;save();
   if(s.loseResponse)throw Error('lost commit response');
   return {body:{commitTime:'2026-10-17T18:00:00Z'}};
  }
  throw Error('Unexpected request');
 }
};`);
let sequence = 0;
const installedHash = 'a'.repeat(64);
function reset(extra = {}) {
  fs.writeFileSync(stateFile, JSON.stringify({ remote: Object.fromEntries(Object.entries(legacyDocuments())
    .map(([key, fields]) => [key, { fields, updateTime: '2026-10-17T12:00:00Z' }])), calls: [], ...extra }));
}
function run(review) {
  const out = path.join(dir, `report-${sequence++}.json`);
  const args = ['firebase/scripts/migrate-sae-catalog.cjs', '--out', out, '--installed-apk-sha256', installedHash];
  if (review) args.push('--apply', review);
  const result = spawnSync(process.execPath, args, { cwd: root, encoding: 'utf8', env: {
    ...process.env, FIRESTORE_EMULATOR_HOST: '', ROADMAP_FIREBASE_TOOLS_LIB: lib, SAE_TEST_STATE: stateFile
  }});
  return { ...result, out, report: fs.existsSync(out) ? JSON.parse(fs.readFileSync(out)) : null,
    state: JSON.parse(fs.readFileSync(stateFile)) };
}
test('dry-run is read-only; reviewed apply commits 55 changes atomically; retry writes zero', () => {
  reset(); const dry = run(); assert.equal(dry.status, 0, dry.stderr);
  assert(!dry.state.calls.some(url => /:(commit|beginTransaction)$/.test(url)));
  const applied = run(dry.out); assert.equal(applied.status, 0, applied.stderr);
  assert.equal(applied.report.writesCommitted, 55); assert.equal(applied.report.outcome, 'verified');
  const retried = run(dry.out); assert.equal(retried.status, 0, retried.stderr);
  assert.equal(retried.report.writesCommitted, 0); assert.equal(retried.state.commits, 1);
});
test('concurrent catalog change blocks the commit and rolls back', () => {
  reset(); const dry = run(); const changed = dry.state;
  changed.remote[Object.keys(changed.remote)[0]].updateTime = '2026-10-17T14:00:00Z';
  fs.writeFileSync(stateFile, JSON.stringify(changed));
  const applied = run(dry.out); assert.equal(applied.status, 1);
  assert.match(applied.stderr, /changed since dry-run/); assert.equal(applied.state.commits, undefined);
  assert(applied.state.calls.some(url => url.endsWith(':rollback')));
});
test('lost commit response stays unconfirmed and retry verifies target without another write', () => {
  reset({ loseResponse: true }); const dry = run(); const applied = run(dry.out);
  assert.equal(applied.status, 1); assert.equal(applied.report.outcome, 'commit-sent-result-unconfirmed');
  const retry = run(dry.out); assert.equal(retry.status, 0, retry.stderr);
  assert.equal(retry.report.outcome, 'verified'); assert.equal(retry.state.commits, 1);
});

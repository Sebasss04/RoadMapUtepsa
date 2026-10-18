// Entire T2 rules verification, failing immediately on any failed subprocess.
const assert = require('node:assert/strict');
const {spawnSync} = require('node:child_process');
const fs = require('node:fs');
const path = require('node:path');
assert.equal(process.env.FIRESTORE_EMULATOR_HOST, '127.0.0.1:8089');
assert.equal(process.env.GCLOUD_PROJECT, 'demo-roadmap-utepsa');
const root = path.resolve(__dirname, '../..');
const output = path.join(root, 'app/build/reports/offer');
fs.mkdirSync(output,{recursive:true});
const stages = [
 'firebase/tests/run-rules-tests.cjs',
 'firebase/tests/history-preparation-rules.test.cjs',
 'firebase/tests/academic-revision-rules.test.cjs',
 'firebase/scripts/prepare-history-sdk-emulator.cjs',
 'firebase/tests/offer-catalog.test.cjs',
 'firebase/tests/offer-rules.test.cjs'
];
const results=[];
for(const stage of stages) {
 const run = spawnSync(process.execPath,[path.join(root,stage)],{encoding:'utf8',timeout:180000,cwd:root});
 const log=(run.stdout||'')+(run.stderr||'');
 fs.writeFileSync(path.join(output,path.basename(stage)+'.log'),log);
 results.push({stage,exitCode:run.status,error:run.error?.message});
 console.log(`${run.status === 0 && !run.error ? 'PASS' : 'FAIL'} ${stage}`);
 if(run.status !== 0 || run.error) { console.error(log); break; }
}
const success=results.length === stages.length && results.every(r=>r.exitCode===0 && !r.error);
fs.writeFileSync(path.join(output,'all-rules.json'),JSON.stringify({success,project:'demo-roadmap-utepsa',results},null,2));
if(!success) process.exitCode=1;

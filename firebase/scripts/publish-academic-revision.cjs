'use strict';
// Targeted rollout for the academic revision contract. Default: validation only.
// Never reads/writes student documents or changes catalog, Auth, or other services.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const crypto = require('node:crypto');
const {spawnSync} = require('node:child_process');
const {values} = require('node:util').parseArgs({options:{out:{type:'string'},apply:{type:'boolean'}}});
assert(values.out, 'Use --out NEW_EVIDENCE_DIRECTORY [--apply]');
assert(!process.env.FIRESTORE_EMULATOR_HOST, 'Production rollout must not inherit emulator variables');
const root = path.resolve(__dirname,'../..');
const out = path.resolve(values.out);
assert(!fs.existsSync(out),'Use a new evidence directory');
const hash = value => crypto.createHash('sha256').update(value).digest('hex');
const rulesPath = path.join(root,'firebase/firestore.rules');
const expectedLocal = '7b70043cfb047d57009062fa1f3ebdd8b87f6cc444e90464855cdb37327479dd';
const expectedRemote = '6c1ad83d09335bc3337996b82e4806fc78ec5c31917437fe143738caa3088ed4';
const cli = path.join(root,'app/build/firebase-admin-tools/node_modules/firebase-tools/lib/bin/firebase.js');
assert.equal(hash(fs.readFileSync(rulesPath)),expectedLocal,'Candidate changed since validation');
assert(JSON.parse(fs.readFileSync(path.join(root,'app/build/reports/optimization-revision-rules.json'))).success);
assert(JSON.parse(fs.readFileSync(path.join(root,'app/build/reports/history-preparation-rules/resultados.json'))).success);
assert(JSON.parse(fs.readFileSync(path.join(root,'app/build/reports/history-write-limits/resultados.json'))).success);
const evidence = path.join(root,'app/build/reports');
for (const test of ['stale-isolated','fiftyChangesUseElevenTransactionsAndOneFinalSnapshot',
  'catalogIsReusedButStatesStayFresh','firstHistoryPersistsApprovedAndCurrentCourseAfterRepositoryRecreation',
  'fiveStateGroupInterruptionRecoversWithNewRepositoryAndDeletesPending','postponePersistsServerTimestampAndRepositoryRecreation']) {
  assert(fs.readFileSync(path.join(evidence,`optimization-sdk-${test}.txt`),'utf8').includes('OK (1 test)'),`Missing SDK test ${test}`);
}
assert(fs.readFileSync(path.join(evidence,'optimization-device-sdk.txt'),'utf8')
  .includes('test=concurrentMallaEditBetweenGroupsPreservesRemoteStateAndJournal\nINSTRUMENTATION_STATUS_CODE: 0') ||
  fs.readFileSync(path.join(evidence,'optimization-device-sdk.txt'),'utf8')
  .includes('test=concurrentMallaEditBetweenGroupsPreservesRemoteStateAndJournal\r\nINSTRUMENTATION_STATUS_CODE: 0'));
const apkHash = hash(fs.readFileSync(path.join(root,'app/build/outputs/apk/debug/app-debug.apk')));
assert.equal(fs.readFileSync(path.join(evidence,'optimization-rollout/installed-device-sha256.txt'),'utf8').trim(),apkHash,
  'Install the compatible APK and verify extracted device APK before publishing');
fs.mkdirSync(out,{recursive:true});
const report={project:'roadmap-utepsa-c9409',database:'(default)',candidateSha256:expectedLocal,
  expectedRemoteSha256:expectedRemote,apkSha256:apkHash,studentDocumentReads:0,studentDocumentWrites:0,
  publicationAttempted:false,outcome:'validating'};
const save=()=>fs.writeFileSync(path.join(out,'rollout.json'),JSON.stringify(report,null,2));
const env={...process.env,CI:'true'};
delete env.DEBUG;delete env.NODE_DEBUG;delete env.FIREBASE_TOKEN;
function run(name,args) {
  const result=spawnSync(process.execPath,args,{cwd:root,env,encoding:'utf8',timeout:180000,maxBuffer:8*1024*1024});
  const safe=String(result.stdout||'')+String(result.stderr||'');
  fs.writeFileSync(path.join(out,`${name}.log`),safe
    .replace(/(Bearer\s+)[^\s"']+/gi,'$1[REDACTED]')
    .replace(/\bya29\.[A-Za-z0-9_.-]+/g,'[REDACTED]'));
  return result.status;
}
function inspect(name) {
  const folder=path.join(out,name);
  assert.equal(run(name,[path.join(__dirname,'compare-history-rules.cjs'),'--out',folder]),0,'Remote inspection failed');
  const read=JSON.parse(fs.readFileSync(path.join(folder,'comparison.json')));
  assert.equal(read.outcome,'compared');assert(read.releaseStableDuringRead);
  return read;
}
try {
  const before=inspect('before');
  assert.equal(before.remoteRulesSha256,expectedRemote,'Unexpected current rules: abort');
  assert(before.preservation.filter(x=>!x.unchangedTokens).every(x=>x.header==='match /estados/{idMateria}'));
  const args=[cli,'deploy','--only','firestore:rules','--project',report.project,'--config','firebase.json','--non-interactive'];
  assert.equal(run('dry-run',[...args,'--dry-run']),0,'Server syntax validation failed');
  report.outcome='validated';save();
  if (values.apply) {
    const immediate=inspect('immediately-before');
    assert.equal(immediate.remoteRulesSha256,expectedRemote);
    assert.equal(immediate.release.rulesetName,before.release.rulesetName);
    assert.equal(immediate.release.updateTime,before.release.updateTime);
    assert.equal(hash(fs.readFileSync(rulesPath)),expectedLocal);
    report.publicationAttempted=true;report.outcome='sent-result-uncertain';save();
    report.deployExitCode=run('deploy',args);
    // Reconcile once regardless of CLI result; do not blindly retry or roll back.
    const after=inspect('after');
    assert.equal(after.remoteRulesSha256,expectedLocal,'Publication not confirmed');
    assert(after.identicalBytes);
    report.outcome='verified-published';
  }
} catch(e) {
  report.outcome=report.publicationAttempted?'requires-reconciliation':'aborted-before-publication';
  report.error=String(e.message).slice(0,240);process.exitCode=1;
} finally {save();console.log(JSON.stringify(report));}

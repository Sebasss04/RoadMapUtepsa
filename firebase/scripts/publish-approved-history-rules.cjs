// Explicitly authorized publication only. Preparing this file does not run it.
'use strict';
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const assert = require('node:assert/strict');
const { spawnSync } = require('node:child_process');
const PROJECT = 'roadmap-utepsa-c9409';
const DATABASE = '(default)';
const APPROVED_LOCAL = '6c1ad83d09335bc3337996b82e4806fc78ec5c31917437fe143738caa3088ed4';
const EXPECTED_REMOTE = 'd294aa427b0fc919a0b09e0f7044a2389ee2a648d225a0627dfb173c71d6432d';
const EXPECTED_RULESET = `projects/${PROJECT}/rulesets/1237c28d-832f-420e-8941-d6ea75627216`;
const EXPECTED_RELEASE_TIME = '2026-10-16T08:22:35.650157Z';
const hash = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
// Defense in depth: default CLI output is not debug; redact credential patterns
// from saved output as well. Never serialize SDK request/error objects.
function sanitized(text) {
  return String(text || '')
    .replace(/(Bearer\s+)[^\s"']+/gi, '$1[REDACTED]')
    .replace(/((?:access_token|refresh_token|id_token|authorization)["']?\s*[:=]\s*["']?)[^\s,"'}]+/gi, '$1[REDACTED]')
    .replace(/\bya29\.[A-Za-z0-9_.-]+/g, '[REDACTED]');
}
async function main() {
  const { values } = require('node:util').parseArgs({ options: {
    'apply-approved': { type: 'boolean' }, out: { type: 'string' }
  } });
  assert(values['apply-approved'] === true, 'Publication requires --apply-approved');
  assert(values.out, 'Use --out NEW_EVIDENCE_DIRECTORY');
  assert(!process.env.FIRESTORE_EMULATOR_HOST, 'Emulator environment is forbidden for approved production publication');
  assert(!process.env.FIREBASE_TOKEN, 'Use the existing CLI login; do not pass FIREBASE_TOKEN');
  const root = path.resolve(__dirname, '../..');
  const out = path.resolve(values.out);
  assert(!fs.existsSync(out), 'Use a new publication evidence directory');
  const rulesPath = path.join(root, 'firebase/firestore.rules');
  const configPath = path.join(root, 'firebase.json');
  const bin = path.join(root, 'app/build/firebase-admin-tools/node_modules/firebase-tools/lib/bin/firebase.js');
  const config = JSON.parse(fs.readFileSync(configPath, 'utf8'));
  assert(config.firestore && !Array.isArray(config.firestore), 'Review multi-database configuration separately');
  assert.equal(config.firestore.rules, 'firebase/firestore.rules', 'Unexpected rules path');
  assert(!config.firestore.database || config.firestore.database === DATABASE, 'Unexpected configured database');
  assert(fs.existsSync(bin), 'Existing Firebase CLI runtime is unavailable; do not install or log in here');
  const configHash = hash(fs.readFileSync(configPath));
  const verifyLocal = () => {
    assert.equal(hash(fs.readFileSync(rulesPath)), APPROVED_LOCAL, 'Local rules differ from the approved hash');
    assert.equal(hash(fs.readFileSync(configPath)), configHash, 'Firebase configuration changed during preparation');
  };
  verifyLocal();
  fs.mkdirSync(out, { recursive: true });
  fs.writeFileSync(path.join(out, 'firestore.approved.rules'), fs.readFileSync(rulesPath), { flag: 'wx' });
  fs.writeFileSync(path.join(out, 'firebase.config.json'), fs.readFileSync(configPath), { flag: 'wx' });
  const report = { startedAt: new Date().toISOString(), project: PROJECT, database: DATABASE,
    approvedRulesSha256: APPROVED_LOCAL, expectedRemoteRulesSha256: EXPECTED_REMOTE,
    expectedRuleset: EXPECTED_RULESET, configSha256: configHash, publicationAttempted: false,
    documentReads: 0, documentWrites: 0, outcome: 'preparing', stages: [] };
  const save = () => fs.writeFileSync(path.join(out, 'publication.json'), JSON.stringify(report, null, 2) + '\n');
  const env = { ...process.env, CI: 'true' };
  // Firebase CLI inherits the already configured login. No debug tracing.
  delete env.DEBUG;
  delete env.NODE_DEBUG;
  const inspect = name => {
    const directory = path.join(out, name);
    const result = spawnSync(process.execPath,
      [path.join(__dirname, 'compare-history-rules.cjs'), '--out', directory],
      { cwd: root, env, encoding: 'utf8', timeout: 180000, maxBuffer: 8 * 1024 * 1024 });
    fs.writeFileSync(path.join(out, `${name}.log`), sanitized(result.stdout) + sanitized(result.stderr), { flag: 'wx' });
    const filename = path.join(directory, 'comparison.json');
    assert(result.status === 0 && fs.existsSync(filename), 'Read-only rules inspection unavailable');
    const read = JSON.parse(fs.readFileSync(filename, 'utf8'));
    assert.equal(read.outcome, 'compared');
    assert.equal(read.project, PROJECT);
    assert.equal(read.database.name, `projects/${PROJECT}/databases/${DATABASE}`);
    assert.equal(read.database.databaseEdition, 'STANDARD');
    assert.equal(read.database.type, 'FIRESTORE_NATIVE');
    assert.equal(read.database.locationId, 'southamerica-east1');
    assert.equal(read.releaseStableDuringRead, true);
    report.stages.push({ stage: name, report: filename, remoteRulesSha256: read.remoteRulesSha256,
      ruleset: read.release.rulesetName, releaseUpdateTime: read.release.updateTime });
    save();
    return read;
  };
  const expectedBefore = read => {
    assert.equal(read.remoteRulesSha256, EXPECTED_REMOTE, 'Remote source changed since approval; abort publication');
    assert.equal(read.release.rulesetName, EXPECTED_RULESET, 'Remote release changed since approval; abort publication');
    assert.equal(read.release.updateTime, EXPECTED_RELEASE_TIME, 'Remote release timestamp changed since approval');
    assert.equal(read.localRulesSha256, APPROVED_LOCAL, 'Inspection did not read the approved candidate');
    assert.equal(read.existingBlocksPreserved, true, 'Existing rules blocks differ; new review required');
  };
  let failure = false;
  let postInspectionAttempted = false;
  try {
    expectedBefore(inspect('preflight'));
    verifyLocal();
    // Fresh second inspection immediately before invoking deploy; no cached release.
    expectedBefore(inspect('immediately-before-publication'));
    verifyLocal();
    report.publicationAttempted = true;
    report.outcome = 'publication-sent-result-uncertain';
    report.deployArguments = ['deploy', '--only', 'firestore:rules', '--project', PROJECT,
      '--config', 'firebase.json', '--non-interactive'];
    save();
    console.log(JSON.stringify({ stage: 'approved-rules-publication', project: PROJECT, evidence: out }));
    const deployed = spawnSync(process.execPath, [bin, ...report.deployArguments],
      { cwd: root, env, encoding: 'utf8', timeout: 240000, maxBuffer: 12 * 1024 * 1024 });
    fs.writeFileSync(path.join(out, 'deploy.log'), sanitized(deployed.stdout) + sanitized(deployed.stderr), { flag: 'wx' });
    report.deploy = { exitCode: deployed.status, signal: deployed.signal,
      timedOut: deployed.error?.code === 'ETIMEDOUT', completedAt: new Date().toISOString() };
    save();
    // Always inspect once, even if CLI reports failure, timeout, or lost response.
    // No deployment retry and no rollback is performed in any branch.
    postInspectionAttempted = true;
    const after = inspect('after-publication');
    verifyLocal();
    report.verifiedRemoteRulesSha256 = after.remoteRulesSha256;
    if (after.remoteRulesSha256 === APPROVED_LOCAL && after.identicalBytes === true) {
      report.outcome = 'verified-published';
      report.publicationResponseUncertain = deployed.status !== 0;
    } else {
      report.outcome = 'publication-not-confirmed';
      failure = true;
    }
  } catch (error) {
    failure = true;
    report.failure = { type: error?.constructor?.name || 'Error',
      reason: report.publicationAttempted ? 'Publication result requires evidence review; no retry or rollback attempted'
        : 'Preconditions or read-only verification failed; publication was not invoked' };
    if (!report.publicationAttempted) report.outcome = 'aborted-before-publication';
    else if (!postInspectionAttempted) {
      // An intervening local I/O failure must not cause a second publication.
      // Make at most one read-only reconciliation attempt instead.
      postInspectionAttempted = true;
      try {
        const reconciled = inspect('reconcile-after-error');
        report.verifiedRemoteRulesSha256 = reconciled.remoteRulesSha256;
        if (reconciled.remoteRulesSha256 === APPROVED_LOCAL && reconciled.identicalBytes === true) {
          report.outcome = 'verified-published';
          report.publicationResponseUncertain = true;
          report.reconciledAfterLocalError = true;
          failure = false;
        }
      } catch (_) { /* Keep the saved uncertain outcome; no retry and no rollback. */ }
    }
    // If failure occurs after deploy but before verified reading, preserve uncertainty.
  } finally {
    report.finishedAt = new Date().toISOString();
    save();
  }
  console.log(JSON.stringify({ outcome: report.outcome, publicationAttempted: report.publicationAttempted,
    documentReads: 0, documentWrites: 0, report: path.join(out, 'publication.json') }));
  if (failure) process.exitCode = 1;
}
if (require.main === module) main().catch(() => {
  console.error('Approved publication setup unavailable; inspect preconditions. No automatic retry.');
  process.exitCode = 1;
});

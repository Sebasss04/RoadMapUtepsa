// READ ONLY remotely: database metadata + current release/ruleset source only.
// No Firestore document APIs, deployment, ruleset creation or release mutation.
'use strict';
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const assert = require('node:assert/strict');
const { spawnSync } = require('node:child_process');
const PROJECT = 'roadmap-utepsa-c9409';
const DATABASE = '(default)';
const sha256 = text => crypto.createHash('sha256').update(text).digest('hex');
const lf = text => text.replace(/\r\n/g, '\n');

// Removes comments/formatting outside quoted strings for exact token comparison.
function tokens(source) {
  return (source.match(/'(?:\\.|[^'\\])*'|"(?:\\.|[^"\\])*"|\/\/[^\n]*|\/\*[\s\S]*?\*\/|\s+|./g) || [])
    .filter(t => !/^\s+$/.test(t) && !t.startsWith('//') && !t.startsWith('/*')).join('');
}
function block(source, header) {
  const start = source.indexOf(header);
  assert(start >= 0, `Missing reviewed block: ${header}`);
  const opening = source.indexOf('{', start + header.length);
  assert(opening >= 0);
  let level = 0;
  const parts = source.slice(opening).match(/'(?:\\.|[^'\\])*'|"(?:\\.|[^"\\])*"|\/\/[^\n]*|\/\*[\s\S]*?\*\/|./gs) || [];
  let length = 0;
  for (const part of parts) {
    length += part.length;
    if (part === '{') level++;
    if (part === '}' && --level === 0) return source.slice(start, opening + length);
  }
  throw new Error('Unbalanced reviewed block');
}
function preservation(remote, local) {
  const functions = ['isOwner(uid)', 'validName(value)', 'validUser(data)', 'validPerson(data)',
    'validStudent(data)', 'completeRegistration(uid)', 'academicProfile(uid)',
    'catalogSubject(idMateria)', 'validAcademicState(data)'];
  const matches = ['/usuarios/{uid}', '/personas/{uid}', '/estados/{idMateria}',
    '/carreras/{id}', '/planes_estudio/{id}', '/materias/{idMateria}', '/{document=**}'];
  const checks = [...functions.map(f => `function ${f}`), ...matches.map(m => `match ${m}`)]
    .map(header => ({ header, unchangedTokens: tokens(block(remote, header)) === tokens(block(local, header)) }));
  const studentPrefix = text => block(text, 'match /estudiantes/{uid}').split('match /estados/{idMateria}')[0];
  checks.push({ header: 'match /estudiantes/{uid}: profile permissions before nested states',
    unchangedTokens: tokens(studentPrefix(remote)) === tokens(studentPrefix(local)) });
  return checks;
}

async function main() {
  const { values } = require('node:util').parseArgs({ options: { out: { type: 'string' } } });
  assert(values.out, 'Usage: node compare-history-rules.cjs --out NEW_EVIDENCE_DIRECTORY');
  assert(!process.env.FIRESTORE_EMULATOR_HOST, 'Remove emulator environment for metadata inspection');
  const root = path.resolve(__dirname, '../..');
  const out = path.resolve(values.out);
  assert(!fs.existsSync(out), 'Evidence directory already exists; use a new path');
  fs.mkdirSync(out, { recursive: true });
  const localRules = fs.readFileSync(path.join(root, 'firebase/firestore.rules'), 'utf8');
  fs.writeFileSync(path.join(out, 'firestore.local.rules'), localRules, { flag: 'wx' });
  const report = { observedAt: new Date().toISOString(), mode: 'read-only', project: PROJECT,
    databaseId: DATABASE, localRulesSha256: sha256(localRules), localRulesLfSha256: sha256(lf(localRules)),
    remoteWrites: 0, documentReads: 0, outcome: 'blocked', stage: 'authentication' };
  try {
    const lib = process.env.ROADMAP_FIREBASE_TOOLS_LIB || path.join(root, 'app/build/firebase-admin-tools/node_modules/firebase-tools/lib');
    const auth = require(path.join(lib, 'auth'));
    const options = { project: PROJECT, nonInteractive: true };
    auth.setActiveAccount(options, auth.getGlobalDefaultAccount());
    await require(path.join(lib, 'requireAuth')).requireAuth(options);
    const { Client } = require(path.join(lib, 'apiv2'));
    const firestore = new Client({ auth: true, apiVersion: 'v1', urlPrefix: 'https://firestore.googleapis.com' });
    const rules = new Client({ auth: true, apiVersion: 'v1', urlPrefix: 'https://firebaserules.googleapis.com' });
    report.stage = 'database-metadata';
    const databases = (await firestore.get(`/projects/${PROJECT}/databases`)).body;
    const database = (await firestore.get(`/projects/${PROJECT}/databases/${DATABASE}`)).body;
    assert.equal(database.name, `projects/${PROJECT}/databases/${DATABASE}`);
    assert.equal(database.databaseEdition, 'STANDARD');
    assert.equal(database.type, 'FIRESTORE_NATIVE');
    assert.equal(database.locationId, 'southamerica-east1');
    report.database = database;
    report.databaseNames = (databases.databases || []).map(item => item.name);
    report.stage = 'release-and-ruleset-get';
    const releasePath = `/projects/${PROJECT}/releases/cloud.firestore`;
    const release = (await rules.get(releasePath)).body;
    assert(release.rulesetName.startsWith(`projects/${PROJECT}/rulesets/`));
    const ruleset = (await rules.get(`/${release.rulesetName}`)).body;
    const files = ruleset.source.files;
    assert(Array.isArray(files) && files.length === 1 && typeof files[0].content === 'string', 'Review multi-file rules separately');
    report.release = release;
    report.rulesetName = ruleset.name;
    report.remoteFilesSha256 = sha256(JSON.stringify(files));
    report.remoteRulesSha256 = sha256(files[0].content);
    report.remoteRulesLfSha256 = sha256(lf(files[0].content));
    report.identicalBytes = files[0].content === localRules;
    report.identicalAfterLf = lf(files[0].content) === lf(localRules);
    fs.writeFileSync(path.join(out, 'firestore.remote.rules'), files[0].content, { flag: 'wx' });
    fs.writeFileSync(path.join(out, 'remote-source.json'), JSON.stringify({ release, ruleset: { name: ruleset.name, createTime: ruleset.createTime, source: { files } } }, null, 2) + '\n', { flag: 'wx' });
    report.stage = 'comparison';
    report.preservation = preservation(files[0].content, localRules);
    report.existingBlocksPreserved = report.preservation.every(item => item.unchangedTokens);
    const diff = spawnSync('git', ['diff', '--no-index', '--no-ext-diff', '--text', '--',
      path.join(out, 'firestore.remote.rules'), path.join(out, 'firestore.local.rules')], { encoding: 'utf8' });
    assert([0, 1].includes(diff.status), 'Unable to prepare local rules diff');
    fs.writeFileSync(path.join(out, 'rules.diff'), diff.stdout, { flag: 'wx' });
    const finalRelease = (await rules.get(releasePath)).body;
    report.releaseStableDuringRead = finalRelease.rulesetName === release.rulesetName && finalRelease.updateTime === release.updateTime;
    assert(report.releaseStableDuringRead, 'Release changed during read; rerun comparison');
    report.outcome = 'compared';
    report.stage = 'complete';
  } catch (error) {
    // Never serialize SDK errors, auth options, request headers, or raw messages.
    report.failure = { type: error?.constructor?.name || 'Error', reason: 'Read or comparison unavailable; no remote mutation attempted' };
    process.exitCode = 1;
  } finally {
    fs.writeFileSync(path.join(out, 'comparison.json'), JSON.stringify(report, null, 2) + '\n', { flag: 'wx' });
  }
  console.log(JSON.stringify({ outcome: report.outcome, stage: report.stage, project: PROJECT,
    remoteWrites: 0, documentReads: 0, existingBlocksPreserved: report.existingBlocksPreserved, report: path.join(out, 'comparison.json') }));
}
if (require.main === module) main().catch(() => { console.error('Local comparison setup unavailable; no remote mutation attempted'); process.exitCode = 1; });
module.exports = { tokens, block, preservation };

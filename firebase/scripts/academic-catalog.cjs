// Dry-run by default. No personal documents, deletes, updates or rules publication.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { spawnSync } = require('node:child_process');
const { PROJECT, DATABASE, BASE, APPROVED_SHA256, sha256, expectedDocuments, planDocuments, creationWrites } = require('./academic-catalog-plan.cjs');
async function main() {
  const args = require('node:util').parseArgs({ options: {
    out: { type: 'string' }, docs: { type: 'string' }, apply: { type: 'string' }
  }}).values;
  assert(args.out && args.docs, 'Usage: node academic-catalog.cjs --docs DOCUMENTATION_DIR --out NEW_REPORT.json [--apply REVIEWED_REPORT.json]');
  assert(!fs.existsSync(args.out), 'Use a new report path; evidence must not be overwritten.');
  assert(!process.env.FIRESTORE_EMULATOR_HOST, 'Remove emulator environment before production inspection.');
  const validation = spawnSync(process.env.ROADMAP_PYTHON || 'python', ['-B', path.join(__dirname, 'validate-academic-catalog.py'),
    '--source-pdf', path.join(args.docs, 'Pensum Ingenieria de Sistemas.pdf')], { encoding: 'utf8' });
  assert.equal(validation.status, 0, validation.stderr || 'Catalog validator failed.');
  const expected = expectedDocuments();
  const lib = process.env.ROADMAP_FIREBASE_TOOLS_LIB;
  assert(lib, 'Set ROADMAP_FIREBASE_TOOLS_LIB to firebase-tools/lib.');
  const auth = require(path.join(lib, 'auth'));
  const options = { project: PROJECT, nonInteractive: true };
  auth.setActiveAccount(options, auth.getGlobalDefaultAccount());
  await require(path.join(lib, 'requireAuth')).requireAuth(options);
  const { Client } = require(path.join(lib, 'apiv2'));
  const api = new Client({ auth: true, apiVersion: 'v1', urlPrefix: 'https://firestore.googleapis.com' });
  const rulesApi = new Client({ auth: true, apiVersion: 'v1', urlPrefix: 'https://firebaserules.googleapis.com' });
  const databases = (await api.get(`/projects/${PROJECT}/databases`)).body;
  const database = (await api.get(`/projects/${PROJECT}/databases/${DATABASE}`)).body;
  assert.equal(database.name, `projects/${PROJECT}/databases/${DATABASE}`);
  assert.equal(database.databaseEdition, 'STANDARD');
  assert.equal(database.type, 'FIRESTORE_NATIVE');
  assert.equal(database.locationId, 'southamerica-east1');
  const releasePath = `/projects/${PROJECT}/releases/cloud.firestore`;
  const release = (await rulesApi.get(releasePath)).body;
  const ruleset = (await rulesApi.get(`/${release.rulesetName}`)).body;
  const localRules = fs.readFileSync(path.join(__dirname, '../firestore.rules'), 'utf8');
  const rulesHash = sha256(JSON.stringify(ruleset.source.files));
  const reviewed = args.apply ? JSON.parse(fs.readFileSync(args.apply, 'utf8')) : null;
  if (reviewed) {
    assert.equal(reviewed.project, PROJECT); assert.equal(reviewed.database.name, database.name);
    assert.equal(reviewed.catalogSha256, APPROVED_SHA256);
    assert.equal(reviewed.remoteRulesSha256, rulesHash, 'Remote rules changed since review.');
    assert.equal(reviewed.localRulesSha256, sha256(localRules), 'Local rules changed since review.');
    assert.equal(reviewed.plan.conflicts.length, 0);
  }
  let transaction;
  const inspect = async () => {
    const remote = {};
    const query = async (parent, collection) => {
      const response = (await api.post(`/${parent}:runQuery`, {
        structuredQuery: { from: [{ collectionId: collection }] }, ...(transaction ? { transaction } : {})
      })).body;
      assert(Array.isArray(response), 'Unexpected query response.');
      return response.filter(row => row.document).map(row => row.document);
    };
    const subjects = await query(BASE, 'materias');
    for (const doc of subjects) remote[doc.name.slice(BASE.length + 1)] = doc.fields || {};
    // Query even absent approved parents to detect orphan prerequisite documents.
    const parents = new Set([...Object.keys(expected).filter(p => !p.includes('/prerrequisitos/')),
      ...subjects.filter(d => d.fields?.id_plan?.stringValue === 'sistemas-rm-615-2016').map(d => d.name.slice(BASE.length + 1))]);
    for (const parent of parents) for (const doc of await query(`${BASE}/${parent}`, 'prerrequisitos')) {
      remote[doc.name.slice(BASE.length + 1)] = doc.fields || {};
    }
    const references = (await api.post(`/${BASE}:batchGet`, {
      documents: ['planes_estudio/sistemas-rm-615-2016', 'carreras/ingenieria-sistemas'].map(p => `${BASE}/${p}`),
      ...(transaction ? { transaction } : {})
    })).body;
    const approvedRefs = JSON.parse(fs.readFileSync(path.join(__dirname, '../profile-references.json'), 'utf8'));
    for (const [p, fields] of Object.entries(approvedRefs)) {
      const actual = references.find(r => r.found?.name === `${BASE}/${p}`)?.found;
      assert(actual, `Missing reference ${p}; no reference writes allowed.`);
      assert.deepEqual(actual.fields, Object.fromEntries(Object.entries(fields).map(([k,v]) => [k,{stringValue:v}])), `Reference differs: ${p}`);
    }
    return { plan: planDocuments(expected, remote), remote, references };
  };
  let report;
  try {
    if (reviewed) transaction = (await api.post(`/${BASE}:beginTransaction`, { options: { readWrite: {} } })).body.transaction;
    const inspection = await inspect();
    report = { observedAt: new Date().toISOString(), mode: reviewed ? 'apply' : 'dry-run', project: PROJECT,
      database, databases, catalogSha256: APPROVED_SHA256, validation: JSON.parse(validation.stdout),
      release, remoteRulesSha256: rulesHash, localRulesSha256: sha256(localRules),
      rulesFiles: ruleset.source.files, ...inspection, writesCommitted: 0, outcome: 'prepared' };
    if (reviewed) {
      // A fully applied catalog is also a valid retry after a lost commit response.
      assert(inspection.plan.create.every(p => reviewed.plan.create.includes(p)), 'Scope grew since review.');
      const writes = creationWrites(expected, inspection.plan);
      assert.equal((await rulesApi.get(releasePath)).body.rulesetName, release.rulesetName, 'Rules changed during preparation.');
      if (writes.length) {
        report.outcome = 'commit-sent-result-unconfirmed';
        const committed = (await api.post(`/${BASE}:commit`, { writes, transaction })).body;
        report.commitTime = committed.commitTime; report.writesCommitted = writes.length;
        report.outcome = 'committed-verification-pending';
        transaction = undefined;
      } else {
        await api.post(`/${BASE}:rollback`, { transaction }); transaction = undefined;
      }
      report.verification = (await inspect()).plan;
      assert.equal(report.verification.conflicts.length, 0);
      assert.equal(report.verification.create.length, 0);
      report.outcome = 'verified';
    }
  } catch (error) {
    if (report) report.error = error.message;
    throw error;
  } finally {
    if (transaction) {
      try { await api.post(`/${BASE}:rollback`, { transaction }); }
      catch (error) { if (report) report.rollbackError = error.message; }
    }
    if (report) {
      fs.mkdirSync(path.dirname(path.resolve(args.out)), { recursive: true });
      fs.writeFileSync(args.out, JSON.stringify(report, null, 2) + '\n', { flag: 'wx' });
    }
  }
  console.log(JSON.stringify({ mode: report.mode, destination: database.name, create: report.plan.create.length,
    unchanged: report.plan.unchanged.length, conflicts: report.plan.conflicts, writesCommitted: report.writesCommitted, report: args.out }, null, 2));
  if (report.plan.conflicts.length) process.exitCode = 1;
}
main().catch(error => { console.error(error.message); process.exitCode = 1; });

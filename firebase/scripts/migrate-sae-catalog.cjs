// Dry-run by default. Atomic migration of the catalog only; never reads/writes students.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { isDeepStrictEqual } = require('node:util');
const { PROJECT, DATABASE, BASE, sha256, expectedDocuments } = require('./academic-catalog-plan.cjs');
const LEGACY_SHA = '06979a0bfa980eb7fb844ffe778eaeeb9c1c514a5a4b6420f2b1dcd511fae0e9';

function legacyDocuments() {
  const bytes = fs.readFileSync(path.join(__dirname, '../catalogo/sistemas-rm-615-2016-v1.json'));
  assert.equal(sha256(bytes), LEGACY_SHA, 'Legacy baseline changed.');
  const catalog = JSON.parse(bytes.toString('utf8').replace(/^\uFEFF/, ''));
  const documents = {};
  for (const { id, ...row } of catalog.materias) documents[`materias/${id}`] = Object.fromEntries(
    Object.entries(row).map(([key, value]) => [key, typeof value === 'number' ? { integerValue: String(value) } : { stringValue: value }]));
  for (const e of catalog.prerrequisitos) documents[`materias/${e.id_objetivo}/prerrequisitos/${e.id_requisito}`] = {};
  return documents;
}

function planMigration(remote) {
  const before = legacyDocuments(), after = expectedDocuments();
  const actual = Object.fromEntries(Object.entries(remote).map(([key, doc]) => [key, doc.fields || {}]));
  if (isDeepStrictEqual(actual, after)) return { state: 'already-applied', updates: [], creates: [], deletes: [] };
  assert(isDeepStrictEqual(actual, before), 'Remote catalog is neither the complete reviewed legacy catalog nor SAE; refusing partial/unknown data.');
  const updates = Object.keys(after).filter(key => Object.hasOwn(before, key) && !isDeepStrictEqual(before[key], after[key])).sort();
  const creates = Object.keys(after).filter(key => !Object.hasOwn(before, key)).sort();
  const deletes = Object.keys(before).filter(key => !Object.hasOwn(after, key)).sort();
  assert.equal(updates.length, 50); assert.equal(creates.length, 2); assert.equal(deletes.length, 3);
  return { state: 'ready', updates, creates, deletes };
}

function migrationWrites(remote, plan) {
  // Recompute the entire reviewed plan; callers cannot broaden a deletion/update list.
  assert.deepEqual(planMigration(remote), plan);
  const after = expectedDocuments();
  const writes = [];
  for (const key of plan.updates) {
    assert(remote[key].updateTime, 'Missing updateTime.');
    writes.push({ update: { name: `${BASE}/${key}`, fields: after[key] }, currentDocument: { updateTime: remote[key].updateTime } });
  }
  for (const key of plan.creates) writes.push({ update: { name: `${BASE}/${key}`, fields: after[key] }, currentDocument: { exists: false } });
  for (const key of plan.deletes) {
    assert(key.includes('/prerrequisitos/') && remote[key].updateTime, 'Only reviewed prerequisite deletions are allowed.');
    writes.push({ delete: `${BASE}/${key}`, currentDocument: { updateTime: remote[key].updateTime } });
  }
  return writes;
}

async function main() {
  const args = require('node:util').parseArgs({ options: {
    out: { type: 'string' }, apply: { type: 'string' }, 'installed-apk-sha256': { type: 'string' }
  }}).values;
  assert(args.out && !fs.existsSync(args.out), 'Provide a new --out report path.');
  assert(!process.env.FIRESTORE_EMULATOR_HOST, 'Production catalog migration cannot target an emulator.');
  const installed = args['installed-apk-sha256'];
  assert(installed && /^[a-f0-9]{64}$/.test(installed), 'Provide --installed-apk-sha256 after installing the compatible APK on all used installations.');
  const lib = process.env.ROADMAP_FIREBASE_TOOLS_LIB;
  assert(lib, 'Set ROADMAP_FIREBASE_TOOLS_LIB.');
  const auth = require(path.join(lib, 'auth'));
  const options = { project: PROJECT, nonInteractive: true };
  auth.setActiveAccount(options, auth.getGlobalDefaultAccount());
  await require(path.join(lib, 'requireAuth')).requireAuth(options);
  const { Client } = require(path.join(lib, 'apiv2'));
  const api = new Client({ auth: true, apiVersion: 'v1', urlPrefix: 'https://firestore.googleapis.com' });
  const database = (await api.get(`/projects/${PROJECT}/databases/${DATABASE}`)).body;
  assert.equal(database.name, `projects/${PROJECT}/databases/${DATABASE}`);
  assert.equal(database.databaseEdition, 'STANDARD');
  assert.equal(database.type, 'FIRESTORE_NATIVE');
  assert.equal(database.locationId, 'southamerica-east1');
  let transaction;
  const inspect = async () => {
    const query = async (parent, collectionId) => {
      const rows = (await api.post(`/${parent}:runQuery`, {
        structuredQuery: { from: [{ collectionId }], ...(collectionId === 'materias' ? {
          where: { fieldFilter: { field: { fieldPath: 'id_plan' }, op: 'EQUAL', value: { stringValue: 'sistemas-rm-615-2016' } } }
        } : {}) }, ...(transaction ? { transaction } : {})
      })).body;
      assert(Array.isArray(rows));
      return rows.filter(row => row.document).map(row => row.document);
    };
    const remote = {};
    const subjects = await query(BASE, 'materias');
    for (const doc of subjects) remote[doc.name.slice(BASE.length + 1)] = { fields: doc.fields || {}, updateTime: doc.updateTime };
    const parents = new Set([...subjects.map(doc => doc.name), ...Object.keys(expectedDocuments()).filter(key => !key.includes('/prerrequisitos/')).map(key => `${BASE}/${key}`)]);
    for (const parent of parents) for (const doc of await query(parent, 'prerrequisitos')) {
      remote[doc.name.slice(BASE.length + 1)] = { fields: doc.fields || {}, updateTime: doc.updateTime };
    }
    return remote;
  };
  const targetSha = sha256(JSON.stringify(expectedDocuments()));
  const reviewed = args.apply ? JSON.parse(fs.readFileSync(args.apply, 'utf8')) : null;
  if (reviewed) {
    assert.equal(reviewed.project, PROJECT); assert.equal(reviewed.database.name, database.name);
    assert.equal(reviewed.targetSha256, targetSha); assert.equal(reviewed.installedApkSha256, installed);
    assert.equal(reviewed.mode, 'dry-run'); assert.equal(reviewed.outcome, 'prepared');
  }
  let report = { project: PROJECT, database, mode: reviewed ? 'apply' : 'dry-run', installedApkSha256: installed,
    targetSha256: targetSha, observedAt: new Date().toISOString(), writesCommitted: 0, outcome: 'inspection-pending' };
  try {
    if (reviewed) transaction = (await api.post(`/${BASE}:beginTransaction`, { options: { readWrite: {} } })).body.transaction;
    const remote = await inspect();
    report.remote = remote; report.plan = planMigration(remote); report.outcome = 'prepared';
    if (reviewed) {
      if (report.plan.state !== 'already-applied') assert.deepEqual(remote, reviewed.remote, 'Catalog changed since dry-run.');
      const writes = migrationWrites(remote, report.plan);
      if (writes.length) {
        report.outcome = 'commit-sent-result-unconfirmed';
        const commit = (await api.post(`/${BASE}:commit`, { writes, transaction })).body;
        transaction = undefined; report.commitTime = commit.commitTime;
        report.writesCommitted = writes.length; report.outcome = 'committed-verification-pending';
      } else {
        await api.post(`/${BASE}:rollback`, { transaction }); transaction = undefined;
      }
      report.verification = planMigration(await inspect());
      assert.equal(report.verification.state, 'already-applied'); report.outcome = 'verified';
    }
  } catch (error) { report.error = error.message; throw error; }
  finally {
    if (transaction) await api.post(`/${BASE}:rollback`, { transaction }).catch(() => {});
    fs.mkdirSync(path.dirname(path.resolve(args.out)), { recursive: true });
    fs.writeFileSync(args.out, JSON.stringify(report, null, 2) + '\n', { flag: 'wx' });
  }
  console.log(JSON.stringify({ outcome: report.outcome, plan: report.plan, writesCommitted: report.writesCommitted, report: args.out }, null, 2));
}
module.exports = { legacyDocuments, planMigration, migrationWrites };
if (require.main === module) main().catch(error => { console.error(error.message); process.exitCode = 1; });

// Controlled administrative load for the TWO approved references; never loads student data.
// Uses the existing Firebase CLI login without printing tokens or credentials.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const cliLib = process.env.ROADMAP_FIREBASE_TOOLS_LIB;
if (!cliLib) throw new Error('Set ROADMAP_FIREBASE_TOOLS_LIB to the installed firebase-tools/lib directory.');
if (!['inspect', 'seed'].includes(process.argv[2])) throw new Error('Usage: node profile-references.cjs inspect|seed');
const project = 'roadmap-utepsa-c9409';
const database = '(default)';
const base = `projects/${project}/databases/${database}/documents`;
const seed = JSON.parse(fs.readFileSync(path.join(__dirname, '..', 'profile-references.json'), 'utf8'));
assert.deepEqual(Object.keys(seed).sort(), ['carreras/ingenieria-sistemas', 'planes_estudio/sistemas-rm-615-2016']);
const fields = data => Object.fromEntries(Object.entries(data).map(([key, val]) => {
  assert.equal(typeof val, 'string');
  return [key, { stringValue: val }];
}));
(async () => {
  const auth = require(path.join(cliLib, 'auth'));
  const options = { project, nonInteractive: true };
  auth.setActiveAccount(options, auth.getGlobalDefaultAccount());
  await require(path.join(cliLib, 'requireAuth')).requireAuth(options);
  const firestore = require(path.join(cliLib, 'gcp', 'firestore'));
  const records = await firestore.getDocuments(project, Object.keys(seed), database);
  if (process.argv[2] === 'inspect') {
    console.log(JSON.stringify({ database: await firestore.getDatabase(project, database), references: records }, null, 2));
    return;
  }
  const writes = [];
  for (const [document, data] of Object.entries(seed)) {
    const name = `${base}/${document}`;
    const found = records.documents.find(record => record.name === name);
    if (found) {
      assert.deepEqual(found.fields, fields(data), `Existing reference differs: ${document}. Refusing overwrite.`);
    } else {
      assert(records.missing.includes(name), `No confirmed missing result for ${document}`);
      writes.push({ update: { name, fields: fields(data) }, currentDocument: { exists: false } });
    }
  }
  if (writes.length) {
    const { Client } = require(path.join(cliLib, 'apiv2'));
    const client = new Client({ auth: true, apiVersion: 'v1', urlPrefix: 'https://firestore.googleapis.com' });
    await client.post(`${base}:commit`, { writes });
  }
  console.log(JSON.stringify({ created: writes.length, references: await firestore.getDocuments(project, Object.keys(seed), database) }, null, 2));
})().catch(error => { console.error(error.message); process.exitCode = 1; });

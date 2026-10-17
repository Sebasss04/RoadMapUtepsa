// Only local demo fixtures. Does not start services, install dependencies or contact Firebase real.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const crypto = require('node:crypto');
const root = path.resolve(__dirname, '../..');
const project = 'demo-roadmap-utepsa';
const origin = 'http://127.0.0.1:8089';
const out = path.join(root, 'app/build/reports/history-sdk-emulator');
fs.mkdirSync(out, { recursive: true });
const config = {
  firestore: { rules: path.join(root, 'firebase/firestore.rules') },
  emulators: { firestore: { host: '127.0.0.1', port: 8089 },
    auth: { host: '127.0.0.1', port: 9099 }, ui: { enabled: false }, singleProjectMode: true }
};
fs.writeFileSync(path.join(out, 'firebase.local.json'), JSON.stringify(config, null, 2));
if (process.argv.includes('--config-only')) {
  console.log(path.join(out, 'firebase.local.json'));
} else {
  assert.equal(process.env.FIRESTORE_EMULATOR_HOST, '127.0.0.1:8089');
  assert.equal(process.env.FIREBASE_AUTH_EMULATOR_HOST, '127.0.0.1:9099');
  assert.equal(process.env.GCLOUD_PROJECT, project);
  const rules = fs.readFileSync(config.firestore.rules, 'utf8');
  const catalog = JSON.parse(fs.readFileSync(path.join(root, 'firebase/catalogo/sistemas-rm-615-2016.json')));
  const refs = JSON.parse(fs.readFileSync(path.join(root, 'firebase/profile-references.json')));
  const name = `projects/${project}/databases/(default)/documents`;
  const enc = v => typeof v === 'string' ? { stringValue: v }
    : typeof v === 'number' ? { integerValue: String(v) }
    : { mapValue: { fields: Object.fromEntries(Object.entries(v).map(([k, value]) => [k, enc(value)])) } };
  const write = (doc, data) => ({ update: { name: `${name}/${doc}`,
    fields: Object.fromEntries(Object.entries(data).map(([k, v]) => [k, enc(v)])) } });
  const report = { project, environment: 'Android SDK + Auth/Firestore LOCAL emulators',
    rulesSha256: crypto.createHash('sha256').update(rules).digest('hex'),
    catalogSubjects: catalog.materias.length, prerequisites: catalog.prerrequisitos.length };
  (async () => {
    const compiled = await fetch(`${origin}/emulator/v1/projects/${project}:securityRules`, {
      method: 'PUT', headers: { 'Content-Type': 'application/json', Authorization: 'Bearer owner' },
      body: JSON.stringify({ rules: { files: [{ name: 'firestore.rules', content: rules }] } }),
      signal: AbortSignal.timeout(60000) });
    assert.equal(compiled.status, 200, await compiled.text());
    const response = await fetch(`${origin}/v1/${name}:commit`, {
      method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: 'Bearer owner' },
      body: JSON.stringify({ writes: [
        ...Object.entries(refs).map(([doc, data]) => write(doc, data)),
        ...catalog.materias.map(({ id, ...data }) => write(`materias/${id}`, data)),
        ...catalog.prerrequisitos.map(r => write(`materias/${r.id_objetivo}/prerrequisitos/${r.id_requisito}`, {}))
      ] }), signal: AbortSignal.timeout(60000) });
    assert.equal(response.status, 200, await response.text());
    report.seedSuccess = true;
    fs.writeFileSync(path.join(out, `seed-${Date.now()}.json`), JSON.stringify(report, null, 2));
    console.log('PASS local demo rules, references, 50 subjects and prerequisite fixtures seeded; no student data seeded');
  })().catch(error => { console.error(error.message); process.exitCode = 1; });
}

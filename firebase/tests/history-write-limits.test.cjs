// Dedicated demo Firestore emulator only. Never reads credentials or touches real Firebase.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const project = 'demo-roadmap-utepsa';
assert.equal(process.env.FIRESTORE_EMULATOR_HOST, '127.0.0.1:8089');
assert.equal(process.env.GCLOUD_PROJECT, project);
const origin = `http://${process.env.FIRESTORE_EMULATOR_HOST}`;
const root = `projects/${project}/databases/(default)/documents`;
const endpoint = `${origin}/v1/${root}`;
const plan = 'sistemas-rm-615-2016';
const catalog = JSON.parse(fs.readFileSync(path.join(__dirname, '../catalogo', `${plan}.json`)));
const output = path.resolve(__dirname, '../../app/build/reports/history-write-limits');
fs.mkdirSync(output, { recursive: true });
const uid = `limits-${Date.now()}`;
const b64 = value => Buffer.from(JSON.stringify(value)).toString('base64url');
const token = `${b64({ alg: 'none', typ: 'JWT' })}.${b64({ iss: `https://securetoken.google.com/${project}`,
  aud: project, sub: uid, user_id: uid, iat: Math.floor(Date.now() / 1000),
  exp: Math.floor(Date.now() / 1000) + 3600,
  firebase: { sign_in_provider: 'google.com', identities: {} } })}.`;
const headers = owner => ({ 'Content-Type': 'application/json', Authorization: `Bearer ${owner ? 'owner' : token}` });
const encode = value => typeof value === 'number' ? { integerValue: String(value) }
  : typeof value === 'boolean' ? { booleanValue: value } : typeof value === 'string' ? { stringValue: value } : value;
const write = (doc, fields) => ({ update: { name: `${root}/${doc}`,
  fields: Object.fromEntries(Object.entries(fields).map(([key, value]) => [key, encode(value)])) } });
const state = index => `estudiantes/${uid}/estados/${catalog.materias[index].id}`;
const updates = (n, value = 'APROBADA') => Array.from({ length: n }, (_, i) => write(state(i), { estado: value }));
const deletes = n => Array.from({ length: n }, (_, i) => ({ delete: `${root}/${state(i)}` }));
const report = { project, host: process.env.FIRESTORE_EMULATOR_HOST, uid,
  rulesSha256: crypto.createHash('sha256').update(fs.readFileSync(path.join(__dirname, '../firestore.rules'))).digest('hex'),
  environment: 'local Firestore emulator; synthetic profile; real catalog fixture; no production access', cases: [] };
const { withRevision } = require('./academic-revision-helper.cjs');
async function request(suffix, body, owner = false) {
  if (suffix === ':commit' && !owner) body = { ...body,
    writes: await withRevision(endpoint, root, user => headers(user === 'owner'), uid, body.writes) };
  const response = await fetch(`${endpoint}${suffix}`, {
    method: 'POST', headers: headers(owner), body: JSON.stringify(body), signal: AbortSignal.timeout(60000) });
  const text = await response.text();
  return { status: response.status, body: text ? JSON.parse(text) : null };
}
async function check(name, writes, expected, transaction) {
  const result = await request(':commit', { writes, ...(transaction ? { transaction } : {}) });
  report.cases.push({ name, writes: writes.length, transaction: !!transaction, ...result });
  fs.writeFileSync(path.join(output, 'resultados.json'), JSON.stringify(report, null, 2) + '\n');
  console.log(`${result.status === 200 ? 'ALLOW' : 'DENY'} ${name}: ${result.status}`);
  if (expected !== undefined) assert.equal(result.status, expected, JSON.stringify(result.body));
  else assert.ok([200, 403].includes(result.status), JSON.stringify(result.body));
  return result.status;
}
async function transactionCheck(name, writes, expected) {
  const begin = await request(':beginTransaction', { options: { readWrite: {} } });
  assert.equal(begin.status, 200, JSON.stringify(begin.body));
  const transaction = begin.body.transaction;
  const read = await request(':batchGet', { documents: [`${root}/estudiantes/${uid}`], transaction });
  assert.equal(read.status, 200, JSON.stringify(read.body));
  return check(name, writes, expected, transaction);
}
(async () => {
  assert.equal(catalog.materias.length, 50);
  const refs = JSON.parse(fs.readFileSync(path.join(__dirname, '../profile-references.json')));
  const fixtures = Object.entries(refs).map(([doc, fields]) => write(doc, fields));
  for (const { id, ...fields } of catalog.materias) fixtures.push(write(`materias/${id}`, fields));
  fixtures.push(write(`usuarios/${uid}`, { fecha_creacion: { timestampValue: new Date().toISOString() } }),
    write(`personas/${uid}`, { nombres: 'Limites', apellidos: 'Prueba sintetica local' }),
    write(`estudiantes/${uid}`, { id_plan: plan }));
  assert.equal((await request(':commit', { writes: fixtures }, true)).status, 200);
  // Very large denied batches can exceed the local emulator's HTTP/2 error
  // header size. Probe the useful range around the application's five-state group.
  const fullStatus = 403;
  let maximum = 0;
  report.probedStateWriteRange = [1, 10];
  if (fullStatus !== 200) {
    // Test every smaller size: does not assume rule-cache behavior is monotonic.
    for (let n = 1; n <= 10; n++) {
      if (await check(`${n} distinct approved state writes`, updates(n)) === 200) maximum = n;
    }
  }
  report.maximumObservedStateWrites = maximum;
  await check('50 state deletions in atomic batch', deletes(50), 200);
  await check(`create ${maximum} approved states at observed boundary`, updates(maximum), 200);
  await check(`update ${maximum} states to in progress at observed boundary`, updates(maximum, 'EN_CURSO'), 200);
  if (maximum < 50) {
    await check(`create/update ${maximum + 1} states exceeds observed boundary`, updates(maximum + 1), 403);
    const read = await fetch(`${endpoint}/${state(maximum)}`, { headers: headers(false) });
    assert.equal(read.status, 404, 'Rejected atomic batch must not create its new final state');
  }
  await check('50 deletions also allowed when some state documents are absent', deletes(50), 200);
  await transactionCheck('transaction with 10 approved states after reading own student', updates(10), 403);
  await transactionCheck(`transaction at observed ${maximum}-write boundary`, updates(maximum), 200);
  if (maximum < 50) {
    await transactionCheck('transaction beyond observed boundary', updates(maximum + 1), 403);
  }
  await check('cleanup own synthetic states', deletes(50), 200);
  report.success = true;
  fs.writeFileSync(path.join(output, 'resultados.json'), JSON.stringify(report, null, 2) + '\n');
  console.log(`PASS Maximum observed state writes: ${maximum}; 50 deletes allowed; demo emulator only`);
})().catch(error => {
  report.success = false;
  report.error = error.stack;
  fs.writeFileSync(path.join(output, 'resultados.json'), JSON.stringify(report, null, 2) + '\n');
  console.error(error);
  process.exitCode = 1;
});

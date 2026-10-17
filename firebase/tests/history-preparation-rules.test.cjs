// Local prototype security tests; no real Firebase credentials or deployment.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const project = 'demo-roadmap-utepsa';
assert.equal(process.env.FIRESTORE_EMULATOR_HOST, '127.0.0.1:8089');
assert.equal(process.env.GCLOUD_PROJECT, project);
const origin = 'http://127.0.0.1:8089';
const root = `projects/${project}/databases/(default)/documents`;
const endpoint = `${origin}/v1/${root}`;
const plan = 'sistemas-rm-615-2016';
const catalog = JSON.parse(fs.readFileSync(path.join(__dirname, '../catalogo', `${plan}.json`)));
const out = path.resolve(__dirname, '../../app/build/reports/history-preparation-rules');
fs.mkdirSync(out, { recursive: true });
const prefix = `preparation-${Date.now()}`;
const uid = `${prefix}-alice`;
const config = user => `estudiantes/${user}/configuracion_academica/historial`;
const journal = user => `estudiantes/${user}/operaciones_historial/actual`;
const state = (user, id) => `estudiantes/${user}/estados/${id}`;
const b64 = value => Buffer.from(JSON.stringify(value)).toString('base64url');
const token = user => `${b64({ alg: 'none', typ: 'JWT' })}.${b64({
  iss: `https://securetoken.google.com/${project}`, aud: project, sub: user, user_id: user,
  iat: Math.floor(Date.now() / 1000), exp: Math.floor(Date.now() / 1000) + 3600,
  firebase: { sign_in_provider: 'google.com', identities: {} } })}.`;
const headers = user => ({ 'Content-Type': 'application/json',
  ...(user ? { Authorization: `Bearer ${user === 'owner' ? 'owner' : token(user)}` } : {}) });
const enc = value => typeof value === 'string' ? { stringValue: value }
  : typeof value === 'number' ? { integerValue: String(value) }
  : typeof value === 'boolean' ? { booleanValue: value }
  : Array.isArray(value) ? { arrayValue: { values: value.map(enc) } }
  : value?.timestampValue ? value
  : { mapValue: { fields: Object.fromEntries(Object.entries(value).map(([k, v]) => [k, enc(v)])) } };
const write = (doc, fields, server = true) => ({ update: { name: `${root}/${doc}`,
  fields: Object.fromEntries(Object.entries(fields).map(([k, v]) => [k, enc(v)])) },
  ...(server ? { updateTransforms: [{ fieldPath: 'actualizado_en', setToServerValue: 'REQUEST_TIME' }] } : {}) });
const prep = (decision = 'POSPUESTO', extras = {}) => ({ version_formato: 1, decision, ...extras });
const delta = n => Object.fromEntries(catalog.materias.slice(0, n).map(m => [m.id, 'PA']));
const op = (extras = {}) => ({ version_formato: 1, id_operacion: crypto.randomUUID(),
  huella_base: 'a'.repeat(64), huella_objetivo: 'b'.repeat(64), revision_preparacion: 'AUSENTE',
  cambios: delta(2), aplicados: [], estado: 'EN_PROGRESO', ...extras });
const report = { project, host: '127.0.0.1:8089', rulesSha256: crypto.createHash('sha256')
  .update(fs.readFileSync(path.join(__dirname, '../firestore.rules'))).digest('hex'), cases: [] };
const save = () => fs.writeFileSync(path.join(out, 'resultados.json'), JSON.stringify(report, null, 2) + '\n');
async function req(user, suffix, body, method = 'POST') {
  const response = await fetch(`${endpoint}${suffix}`, { method, headers: headers(user),
    ...(body ? { body: JSON.stringify(body) } : {}), signal: AbortSignal.timeout(60000) });
  const text = await response.text();
  return { status: response.status, body: text ? JSON.parse(text) : null };
}
const { withRevision } = require('./academic-revision-helper.cjs');
const commit = async (user, writes, transaction) => req(user, ':commit', { writes: await withRevision(endpoint, root, headers, user, writes), ...(transaction ? { transaction } : {}) });
const get = (user, doc) => req(user, `/${doc}`, null, 'GET');
async function expect(name, promise, expected) {
  const result = await promise;
  report.cases.push({ name, ...result }); save();
  assert.equal(result.status, expected, `${name}: ${JSON.stringify(result.body)}`);
  console.log(`PASS ${name}`);
  return result.body;
}
async function seedProfile(user) {
  return expect(`seed synthetic complete profile ${user}`, commit('owner', [
    write(`usuarios/${user}`, { fecha_creacion: { timestampValue: '2026-10-17T00:00:00Z' } }, false),
    write(`personas/${user}`, { nombres: 'Prueba', apellidos: 'Local' }, false),
    write(`estudiantes/${user}`, { id_plan: plan }, false) ]), 200);
}
async function exactTransaction(user, current, n) {
  const start = await req(user, ':beginTransaction', { options: { readWrite: {} } });
  assert.equal(start.status, 200);
  const transaction = start.body.transaction;
  const documents = [...catalog.materias.map(m => `${root}/${state(user, m.id)}`),
    `${root}/${config(user)}`, `${root}/${journal(user)}`];
  await expect(`read all 50 states + config + journal before group ${n}`, req(user, ':batchGet', { documents, transaction }), 200);
  return commit(user, [...catalog.materias.slice(0, n).map(m => write(state(user, m.id), { estado: 'APROBADA' }, false)),
    write(journal(user), { ...current, aplicados: catalog.materias.slice(0, n).map(m => m.id) })], transaction);
}
(async () => {
  const rules = fs.readFileSync(path.join(__dirname, '../firestore.rules'), 'utf8');
  const compiled = await fetch(`${origin}/emulator/v1/projects/${project}:securityRules`, {
    method: 'PUT', headers: headers('owner'), body: JSON.stringify({ rules: { files: [{ name: 'firestore.rules', content: rules }] } }) });
  assert.equal(compiled.status, 200, await compiled.text());
  const refs = JSON.parse(fs.readFileSync(path.join(__dirname, '../profile-references.json')));
  await expect('seed local reference/catalog fixtures', commit('owner', [
    ...Object.entries(refs).map(([doc, data]) => write(doc, data, false)),
    ...catalog.materias.map(({ id, ...data }) => write(`materias/${id}`, data, false)) ]), 200);
  await seedProfile(uid);
  await seedProfile(`${prefix}-bob`);
  for (const doc of [config(uid), journal(uid)]) {
    await expect(`own missing document ${doc}`, get(uid, doc), 404);
    for (const user of [null, `${prefix}-bob`, `${prefix}-partial`]) {
      await expect(`foreign/unauth get denied ${user} ${doc}`, get(user, doc), 403);
      await expect(`foreign/unauth write denied ${user} ${doc}`, commit(user, [write(doc, doc === config(uid) ? prep() : op())]), 403);
    }
    await expect(`no list ${doc}`, get(uid, doc.slice(0, doc.lastIndexOf('/'))), 403);
    await expect(`no delete ${doc}`, commit(uid, [{ delete: `${root}/${doc}` }]), 403);
  }
  await expect('incomplete profile cannot get own config', get(`${prefix}-partial`, config(`${prefix}-partial`)), 403);
  await expect('incomplete profile cannot create own config', commit(`${prefix}-partial`, [write(config(`${prefix}-partial`), prep())]), 403);
  await expect('incomplete profile cannot create own journal', commit(`${prefix}-partial`, [write(journal(`${prefix}-partial`), op())]), 403);
  await expect('postpone without operation', commit(uid, [write(config(uid), prep())]), 200);
  for (const [name, data] of [
    ['extra field', prep('POSPUESTO', { admin: true })], ['wrong version', prep('POSPUESTO', { version_formato: 2 })],
    ['wrong version type', prep('POSPUESTO', { version_formato: '1' })], ['unknown decision', prep('OTRA')],
    ['wrong decision type', prep(false)], ['missing decision', { version_formato: 1 }] ]) {
    await expect(`configuration ${name}`, commit(uid, [write(config(uid), data)]), 403);
  }
  for (const fecha of ['hoy', { timestampValue: '2000-01-01T00:00:00Z' }, { timestampValue: '2099-01-01T00:00:00Z' }]) {
    await expect(`configuration timestamp ${JSON.stringify(fecha)}`, commit(uid, [write(config(uid), prep('POSPUESTO', { actualizado_en: fecha }), false)]), 403);
  }
  await expect('confirmation without final operation denied', commit(uid, [write(config(uid), prep('CONFIRMADO'))]), 403);
  const base = op();
  for (const [name, data] of [
    ['extra field', { ...base, admin: true }], ['version', { ...base, version_formato: 2 }],
    ['unknown state', { ...base, estado: 'OTRA' }], ['bad uuid', { ...base, id_operacion: 'bad' }],
    ['bad hash', { ...base, huella_base: 'a' }], ['bad revision', { ...base, revision_preparacion: 'hoy' }],
    ['hash wrong type', { ...base, huella_objetivo: 64 }], ['version wrong type', { ...base, version_formato: '1' }],
    ['revision wrong type', { ...base, revision_preparacion: false }], ['applied wrong type', { ...base, aplicados: {} }],
    ['bad code', { ...base, cambios: { [catalog.materias[0].id]: 'PP' } }],
    ['bad uuid key', { ...base, cambios: { bad: 'PA' } }], ['wrong map', { ...base, cambios: [] }],
    ['duplicates', { ...base, aplicados: [catalog.materias[0].id, catalog.materias[0].id] }],
    ['applied outside map', { ...base, aplicados: [catalog.materias[3].id] }],
    ['initial applied', { ...base, aplicados: [catalog.materias[0].id] }],
    ['initial terminal', { ...base, estado: 'COMPLETADA', aplicados: Object.keys(base.cambios) }],
    ['too many changes', { ...base, cambios: Object.fromEntries(Array.from({ length: 51 }, () => [crypto.randomUUID(), 'PA'])) }] ]) {
    await expect(`journal ${name}`, commit(uid, [write(journal(uid), data)]), 403);
  }
  await expect('journal future timestamp denied', commit(uid, [write(journal(uid), { ...base, actualizado_en: { timestampValue: '2099-01-01T00:00:00Z' } }, false)]), 403);
  await expect('start operation', commit(uid, [write(journal(uid), base)]), 200);
  await expect('read journal after reopen', get(uid, journal(uid)), 200);
  await expect('cannot postpone active operation', commit(uid, [write(config(uid), prep())]), 403);
  await expect('cannot replace active operation', commit(uid, [write(journal(uid), op())]), 403);
  for (const field of ['huella_base', 'huella_objetivo', 'revision_preparacion', 'cambios']) {
    const value = field === 'cambios' ? delta(3) : field === 'revision_preparacion' ? '1:0' : 'c'.repeat(64);
    await expect(`immutable journal ${field}`, commit(uid, [write(journal(uid), { ...base, [field]: value })]), 403);
  }
  const progressed = { ...base, aplicados: [catalog.materias[0].id] };
  await expect('advance partial progress', commit(uid, [write(journal(uid), progressed)]), 200);
  await expect('progress cannot shrink', commit(uid, [write(journal(uid), base)]), 403);
  await expect('cannot complete partial progress', commit(uid, [write(journal(uid), { ...progressed, estado: 'COMPLETADA' })]), 403);
  await expect('cancellation cannot discard progress', commit(uid, [write(journal(uid), { ...base, estado: 'CANCELADA' })]), 403);
  await expect('cancellation cannot add progress', commit(uid, [write(journal(uid), {
    ...base, estado: 'CANCELADA', aplicados: Object.keys(base.cambios) })]), 403);
  await expect('cancel preserves applied progress', commit(uid, [write(journal(uid), { ...progressed, estado: 'CANCELADA' })]), 200);
  await expect('terminal cannot reopen same operation', commit(uid, [write(journal(uid), progressed)]), 403);
  await expect('terminal journal cannot be edited in place', commit(uid, [write(journal(uid), { ...progressed, estado: 'CANCELADA' })]), 403);
  await expect('postpone after cancellation', commit(uid, [write(config(uid), prep())]), 200);
  const replacement = op({ cambios: {} });
  await expect('replace terminal with new operation', commit(uid, [write(journal(uid), replacement)]), 200);
  await expect('finish empty operation and confirm in same commit', commit(uid, [
    write(journal(uid), { ...replacement, estado: 'COMPLETADA' }), write(config(uid), prep('CONFIRMADO')) ]), 200);
  await expect('cannot confirm later reusing old completed operation', commit(uid, [write(config(uid), prep('CONFIRMADO'))]), 403);
  for (const n of [5, 6, 7, 8]) {
    const user = `${prefix}-group-${n}`;
    await seedProfile(user);
    const current = op({ cambios: delta(50) });
    await expect(`start operation group ${n}`, commit(user, [write(journal(user), current)]), 200);
    const response = await exactTransaction(user, current, n);
    report.cases.push({ name: `exact transaction group ${n} plus journal`, ...response }); save();
    console.log(`OBSERVED group ${n} plus journal: ${response.status}`);
    assert.equal(response.status, n <= 6 ? 200 : 403, JSON.stringify(response.body));
    if (n >= 7) {
      await expect('rejected group leaves first state absent', get(user, state(user, catalog.materias[0].id)), 404);
      const retained = await expect('rejected group preserves journal', get(user, journal(user)), 200);
      assert.equal(retained.fields.aplicados.arrayValue.values?.length ?? 0, 0);
    }
  }
  report.maximumObservedStateWritesPlusJournal = 6;
  report.success = true; save();
  console.log(`PASS ${report.cases.length} local preparation checks`);
})().catch(error => { report.success = false; report.error = error.stack; save(); console.error(error); process.exitCode = 1; });

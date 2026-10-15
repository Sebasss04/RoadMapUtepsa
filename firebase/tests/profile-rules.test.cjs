// Runs only against the local Firestore emulator; never against the real project.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const project = 'demo-roadmap-utepsa';
const host = process.env.FIRESTORE_EMULATOR_HOST;
if (host !== '127.0.0.1:8089') throw new Error('Requires the dedicated local emulator at 127.0.0.1:8089');
const origin = `http://${host}`;
const root = `projects/${project}/databases/(default)/documents`;
const endpoint = `${origin}/v1/${root}`;
const plan = 'sistemas-rm-615-2016';
const b64 = value => Buffer.from(JSON.stringify(value)).toString('base64url');
const token = uid => `${b64({ alg: 'none', typ: 'JWT' })}.${b64({
  iss: `https://securetoken.google.com/${project}`, aud: project,
  sub: uid, user_id: uid, iat: Math.floor(Date.now() / 1000),
  exp: Math.floor(Date.now() / 1000) + 3600,
  firebase: { sign_in_provider: 'google.com', identities: {} }
})}.`;
const headers = uid => ({ 'Content-Type': 'application/json',
  ...(uid ? { Authorization: `Bearer ${uid === 'owner' ? 'owner' : token(uid)}` } : {}) });
const value = input => typeof input === 'string' ? { stringValue: input }
  : typeof input === 'boolean' ? { booleanValue: input }
  : typeof input === 'number' ? { integerValue: String(input) } : input;
const write = (path, data) => ({ update: { name: `${root}/${path}`,
  fields: Object.fromEntries(Object.entries(data).map(([k, v]) => [k, value(v)])) } });
const registration = (uid, names = 'María José', surnames = 'Pérez Díaz', idPlan = plan) => [
  { ...write(`usuarios/${uid}`, {}), updateTransforms: [{ fieldPath: 'fecha_creacion', setToServerValue: 'REQUEST_TIME' }] },
  write(`personas/${uid}`, { nombres: names, apellidos: surnames }),
  write(`estudiantes/${uid}`, { id_plan: idPlan })
];
const commit = (uid, writes) => fetch(`${endpoint}:commit`, {
  method: 'POST', headers: headers(uid), body: JSON.stringify({ writes })
});
const get = (uid, path) => fetch(`${endpoint}/${path}`, { headers: headers(uid) });
const results = [];
async function expect(name, promise, status) {
  const response = await promise;
  const body = await response.text();
  assert.equal(response.status, status, `${name}: ${body}`);
  results.push(name);
  console.log(`PASS ${name}`);
  return body ? JSON.parse(body) : null;
}
async function references() {
  const seed = JSON.parse(fs.readFileSync(require('node:path').join(__dirname, '..', 'profile-references.json'), 'utf8'));
  await expect('admin loads only local reference fixtures', commit('owner', Object.entries(seed).map(([path, fields]) => write(path, fields))), 200);
}
(async () => {
  await expect('clear dedicated demo emulator', fetch(`${origin}/emulator/v1/projects/${project}/databases/(default)/documents`,
    { method: 'DELETE', headers: headers('owner') }), 200);
  await references();
  await expect('owner can distinguish missing profile', get('alice', 'personas/alice'), 404);
  await expect('unauthenticated private read denied', get(null, 'personas/alice'), 403);
  await expect('unauthenticated list denied', get(null, 'personas'), 403);
  await expect('authenticated private list denied', get('alice', 'personas'), 403);
  await expect('reference readable by authenticated student', get('alice', `planes_estudio/${plan}`), 200);
  await expect('reference not public', get(null, `planes_estudio/${plan}`), 403);
  await expect('career not public', get(null, 'carreras/ingenieria-sistemas'), 403);
  await expect('single user document cannot create partial registration', commit('alice', registration('alice').slice(0, 1)), 403);
  await expect('failed partial batch leaves no user document', get('alice', 'usuarios/alice'), 404);
  await expect('single person cannot create partial registration', commit('alice', [registration('alice')[1]]), 403);
  await expect('single student cannot create partial registration', commit('alice', [registration('alice')[2]]), 403);
  await expect('atomic first registration', commit('alice', registration('alice')), 200);
  const before = await expect('read own creation timestamp', get('alice', 'usuarios/alice'), 200);
  for (const collection of ['usuarios', 'personas', 'estudiantes']) {
    await expect(`other account cannot read ${collection}`, get('bob', `${collection}/alice`), 403);
  }
  await expect('second account gets independent registration', commit('bob', registration('bob', 'Otro', 'Estudiante')), 200);
  await expect('correct own names', commit('alice', [write('personas/alice', { nombres: 'María', apellidos: 'Pérez Corregido' })]), 200);
  const after = await expect('creation date still readable', get('alice', 'usuarios/alice'), 200);
  assert.deepEqual(before.fields, after.fields);
  const updated = await expect('new request recovers corrected values', get('alice', 'personas/alice'), 200);
  assert.equal(updated.fields.apellidos.stringValue, 'Pérez Corregido');
  const other = await expect('other account unchanged', get('bob', 'personas/bob'), 200);
  assert.equal(other.fields.nombres.stringValue, 'Otro');
  await expect('cannot write another persons profile', commit('bob', [write('personas/alice', { nombres: 'Intruso', apellidos: 'Intruso' })]), 403);
  await expect('cannot create under another UID', commit('bob', registration('third')), 403);
  await expect('cannot delete profile', commit('alice', [{ delete: `${root}/personas/alice` }]), 403);
  await expect('creation timestamp immutable', commit('alice', [registration('alice')[0]]), 403);
  await expect('student cannot change plan', commit('alice', [write('estudiantes/alice', { id_plan: 'otro-plan' })]), 403);
  await expect('student cannot add roles', commit('alice', [write('estudiantes/alice', { id_plan: plan, role: 'admin' })]), 403);
  await expect('user cannot add privilege field', commit('alice', [write('usuarios/alice', {
    fecha_creacion: before.fields.fecha_creacion, isAdmin: true })]), 403);
  await expect('timestamp must have timestamp type', commit('new-date', [
    write('usuarios/new-date', { fecha_creacion: 'hoy' }), ...registration('new-date').slice(1)
  ]), 403);
  await expect('client cannot forge historical creation date', commit('past', [
    write('usuarios/past', { fecha_creacion: { timestampValue: '2000-01-01T00:00:00Z' } }), ...registration('past').slice(1)
  ]), 403);
  for (const [label, names] of [
    ['empty', ''], ['whitespace', '   '], ['unicode whitespace', '\u00a0\u2003'],
    ['too long', 'a'.repeat(81)], ['huge', 'x'.repeat(100_000)], ['wrong type', 42],
    ['newline', 'Nombre\nApellido'], ['tab', 'Nombre\tApellido'], ['control', 'Nom\u0000bre'],
    ['unicode line separator', 'Nom\u2028bre']
  ]) {
    await expect(`invalid names on update: ${label}`, commit('alice', [write('personas/alice', { nombres: names, apellidos: 'Pérez' })]), 403);
    await expect(`invalid names on create: ${label}`, commit('new', registration('new', names)), 403);
  }
  await expect('missing required field on update', commit('alice', [write('personas/alice', { nombres: 'María' })]), 403);
  await expect('unknown person fields denied', commit('alice', [write('personas/alice', { nombres: 'María', apellidos: 'Pérez', uid: 'bob' })]), 403);
  await expect('wrong surname type denied', commit('alice', [write('personas/alice', { nombres: 'María', apellidos: false })]), 403);
  await expect('empty surnames denied', commit('alice', [write('personas/alice', { nombres: 'María', apellidos: '' })]), 403);
  await expect('long surnames denied', commit('alice', [write('personas/alice', { nombres: 'María', apellidos: 'a'.repeat(81) })]), 403);
  await expect('names with accents and punctuation allowed', commit('alice', [write('personas/alice', { nombres: 'María José', apellidos: 'O’Connor-Díaz' })]), 200);
  for (const path of ['carreras/ingenieria-sistemas', `planes_estudio/${plan}`]) {
    await expect(`student cannot update reference ${path}`, commit('alice', [write(path, { nombre: 'Alterado' })]), 403);
    await expect(`student cannot delete reference ${path}`, commit('alice', [{ delete: `${root}/${path}` }]), 403);
  }
  await expect('student cannot create catalog reference', commit('alice', [write('carreras/otra', { nombre: 'Otra' })]), 403);
  await expect('unimplemented academic subcollection denied', commit('alice', [write('estudiantes/alice/estados/m001', { estado: 'APROBADA' })]), 403);
  await expect('unknown collection denied', commit('alice', [write('otra/alice', { dato: 'valor' })]), 403);
  await expect('atomic batch with invalid person fails', commit('invalid-batch', registration('invalid-batch', '')), 403);
  await expect('invalid batch created no user', get('invalid-batch', 'usuarios/invalid-batch'), 404);
  await expect('admin creates partial fixture', commit('owner', [registration('partial')[0]]), 200);
  await expect('owner can repair incomplete registration atomically', commit('partial', registration('partial').slice(1)), 200);
  await expect('admin removes plan fixture', commit('owner', [{ delete: `${root}/planes_estudio/${plan}` }]), 200);
  await expect('missing reference denies registration', commit('no-plan', registration('no-plan')), 403);
  await references();
  await expect('admin removes career fixture', commit('owner', [{ delete: `${root}/carreras/ingenieria-sistemas` }]), 200);
  await expect('missing career denies registration', commit('no-career', registration('no-career')), 403);
  await references();
  console.log(`\n${results.length} local rule checks passed. No production data touched.`);
})().catch(error => { console.error(error); process.exitCode = 1; });

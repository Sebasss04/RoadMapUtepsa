// Local emulator only. All privileged writes below are synthetic local fixtures.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const project = 'demo-roadmap-utepsa';
const host = process.env.FIRESTORE_EMULATOR_HOST;
if (host !== '127.0.0.1:8089') throw new Error('Requires dedicated emulator 127.0.0.1:8089');
if (process.env.GCLOUD_PROJECT && process.env.GCLOUD_PROJECT !== project) throw new Error('Requires demo project');
const origin = `http://${host}`;
const root = `projects/${project}/databases/(default)/documents`;
const endpoint = `${origin}/v1/${root}`;
const plan = 'sistemas-rm-615-2016';
const catalog = JSON.parse(fs.readFileSync(path.join(__dirname, '../catalogo', `${plan}.json`), 'utf8'));
const subject = code => catalog.materias.find(row => row.codigo === code);
const b64 = value => Buffer.from(JSON.stringify(value)).toString('base64url');
const token = uid => `${b64({ alg: 'none', typ: 'JWT' })}.${b64({
  iss: `https://securetoken.google.com/${project}`, aud: project,
  sub: uid, user_id: uid, iat: Math.floor(Date.now() / 1000), exp: Math.floor(Date.now() / 1000) + 3600,
  firebase: { sign_in_provider: 'google.com', identities: {} }
})}.`;
const headers = uid => ({ 'Content-Type': 'application/json',
  ...(uid ? { Authorization: `Bearer ${uid === 'owner' ? 'owner' : token(uid)}` } : {}) });
const encode = input => typeof input === 'string' ? { stringValue: input }
  : typeof input === 'number' ? { integerValue: String(input) }
  : typeof input === 'boolean' ? { booleanValue: input } : input;
const write = (doc, fields) => ({ update: { name: `${root}/${doc}`,
  fields: Object.fromEntries(Object.entries(fields).map(([key, value]) => [key, encode(value)])) } });
const remove = doc => ({ delete: `${root}/${doc}` });
const { withRevision } = require('./academic-revision-helper.cjs');
const commit = async (uid, writes) => fetch(`${endpoint}:commit`, {
  method: 'POST', headers: headers(uid), body: JSON.stringify({ writes: await withRevision(endpoint, root, headers, uid, writes) }) });
const get = (uid, doc) => fetch(`${endpoint}/${doc}`, { headers: headers(uid) });
const list = (uid, collection) => fetch(`${endpoint}/${collection}`, { headers: headers(uid) });
const query = (uid, collection, where, parent = '', group = false) => fetch(
  `${endpoint}${parent ? `/${parent}` : ''}:runQuery`, {
    method: 'POST', headers: headers(uid), body: JSON.stringify({ structuredQuery: {
      from: [{ collectionId: collection, ...(group ? { allDescendants: true } : {}) }],
      ...(where ? { where } : {})
    } })
  });
const planFilter = value => ({ fieldFilter: { field: { fieldPath: 'id_plan' }, op: 'EQUAL', value: encode(value) } });
const statePath = (uid, code = 'SIS-306') => `estudiantes/${uid}/estados/${subject(code).id}`;
const register = uid => [
  { ...write(`usuarios/${uid}`, {}), updateTransforms: [{ fieldPath: 'fecha_creacion', setToServerValue: 'REQUEST_TIME' }] },
  write(`personas/${uid}`, { nombres: 'Estudiante', apellidos: 'Prueba local' }),
  write(`estudiantes/${uid}`, { id_plan: plan })
];
let checks = 0;
async function expect(name, promise, status) {
  const response = await promise;
  const text = await response.text();
  assert.equal(response.status, status, `${name}: ${text}`);
  checks++;
  console.log(`PASS ${name}`);
  return text ? JSON.parse(text) : null;
}

(async () => {
  await expect('clear only dedicated demo emulator', fetch(
    `${origin}/emulator/v1/projects/${project}/databases/(default)/documents`,
    { method: 'DELETE', headers: headers('owner') }), 200);
  const refs = JSON.parse(fs.readFileSync(path.join(__dirname, '../profile-references.json'), 'utf8'));
  const fixtures = Object.entries(refs).map(([doc, fields]) => write(doc, fields));
  for (const { id, ...fields } of catalog.materias) fixtures.push(write(`materias/${id}`, fields));
  for (const edge of catalog.prerrequisitos) {
    fixtures.push(write(`materias/${edge.id_objetivo}/prerrequisitos/${edge.id_requisito}`, {}));
  }
  fixtures.push(write('materias/other-plan', { ...subject('SIS-306'), id_plan: 'otro-plan' }));
  fixtures.push(write('materias/orphan/prerrequisitos/unknown', {}));
  await expect('seed validated catalog only inside demo emulator', commit('owner', fixtures), 200);
  await expect('register alice', commit('alice', register('alice')), 200);
  await expect('register bob', commit('bob', register('bob')), 200);

  const curriculum = await expect('query catalog constrained to v1 plan', query('alice', 'materias', planFilter(plan)), 200);
  assert.equal(curriculum.filter(row => row.document).length, 50);
  await expect('catalog get authenticated without academic profile', get('new', `materias/${subject('SIS-306').id}`), 200);
  await expect('catalog get unauthenticated denied', get(null, `materias/${subject('SIS-306').id}`), 403);
  await expect('catalog query unauthenticated denied', query(null, 'materias', planFilter(plan)), 403);
  await expect('catalog unfiltered query denied', query('alice', 'materias'), 403);
  await expect('catalog unfiltered list denied', list('alice', 'materias'), 403);
  await expect('catalog other plan query denied', query('alice', 'materias', planFilter('otro-plan')), 403);
  await expect('catalog other plan get denied', get('alice', 'materias/other-plan'), 403);
  const requirements = await expect('list integrator requirements',
    list('alice', `materias/${subject('SIS-306').id}/prerrequisitos`), 200);
  assert.equal(requirements.documents.length, 2);
  assert.deepEqual(requirements.documents.map(doc => doc.name.split('/').pop()).sort(),
    [subject('SIS-305').id, subject('SCC-305').id].sort());
  const relation = `materias/${subject('SIS-306').id}/prerrequisitos/${subject('SCC-305').id}`;
  await expect('get prerequisite authenticated', get('alice', relation), 200);
  await expect('prerequisites unauthenticated list denied', list(null, `materias/${subject('SIS-306').id}/prerrequisitos`), 403);
  await expect('orphan prerequisite get denied', get('alice', 'materias/orphan/prerrequisitos/unknown'), 403);
  await expect('orphan prerequisite list denied', list('alice', 'materias/orphan/prerrequisitos'), 403);
  await expect('other plan prerequisite list denied', list('alice', 'materias/other-plan/prerrequisitos'), 403);
  await expect('prerequisite collection group denied', query('alice', 'prerrequisitos', null, '', true), 403);
  for (const [label, doc, fields] of [
    ['new subject', 'materias/new-subject', { id_plan: plan }],
    ['existing subject', `materias/${subject('SIS-306').id}`, { id_plan: plan }],
    ['new prerequisite', `materias/${subject('SIS-306').id}/prerrequisitos/${subject('BMS-300').id}`, {}],
    ['existing prerequisite', relation, {}]
  ]) {
    await expect(`student cannot write ${label}`, commit('alice', [write(doc, fields)]), 403);
    await expect(`student cannot delete ${label}`, commit('alice', [remove(doc)]), 403);
  }

  await expect('own empty states query allowed', query('alice', 'estados', null, 'estudiantes/alice'), 200);
  await expect('own empty states list allowed', list('alice', 'estudiantes/alice/estados'), 200);
  await expect('own missing state is absent', get('alice', statePath('alice')), 404);
  await expect('create approved integrator without preceding history allowed',
    commit('alice', [write(statePath('alice'), { estado: 'APROBADA' })]), 200);
  await expect('update approved to in progress allowed',
    commit('alice', [write(statePath('alice'), { estado: 'EN_CURSO' })]), 200);
  await expect('new request recovers state', get('alice', statePath('alice')), 200);
  const own = await expect('own states query returns saved state', query('alice', 'estados', null, 'estudiantes/alice'), 200);
  assert.equal(own.filter(row => row.document).length, 1);
  assert.equal(own.find(row => row.document).document.fields.estado.stringValue, 'EN_CURSO');
  await expect('bob has independent empty states', list('bob', 'estudiantes/bob/estados'), 200);
  for (const uid of [null, 'bob']) {
    await expect(`${uid || 'anonymous'} cannot get alice state`, get(uid, statePath('alice')), 403);
    await expect(`${uid || 'anonymous'} cannot list alice states`, list(uid, 'estudiantes/alice/estados'), 403);
    await expect(`${uid || 'anonymous'} cannot query alice states`, query(uid, 'estados', null, 'estudiantes/alice'), 403);
    await expect(`${uid || 'anonymous'} cannot create alice state`, commit(uid, [write(statePath('alice', 'BMS-300'), { estado: 'APROBADA' })]), 403);
    await expect(`${uid || 'anonymous'} cannot update alice state`, commit(uid, [write(statePath('alice'), { estado: 'APROBADA' })]), 403);
    await expect(`${uid || 'anonymous'} cannot delete alice state`, commit(uid, [remove(statePath('alice'))]), 403);
  }
  await expect('states collection group denied', query('alice', 'estados', null, '', true), 403);
  for (const [label, fields] of [
    ['pending must be deletion', { estado: 'PENDIENTE' }], ['empty value', { estado: '' }],
    ['unknown value', { estado: 'aprobada' }], ['wrong type', { estado: 1 }],
    ['boolean type', { estado: true }], ['null type', { estado: { nullValue: null } }],
    ['map type', { estado: { mapValue: { fields: {} } } }], ['missing field', {}],
    ['extra owner', { estado: 'APROBADA', uid: 'bob' }], ['extra role', { estado: 'APROBADA', role: 'admin' }],
    ['extra plan', { estado: 'APROBADA', id_plan: plan }],
    ['huge value', { estado: 'x'.repeat(100000) }]
  ]) {
    await expect(`invalid state create: ${label}`, commit('alice', [write(statePath('alice', 'BMS-300'), fields)]), 403);
    await expect(`invalid state update: ${label}`, commit('alice', [write(statePath('alice'), fields)]), 403);
  }
  await expect('state for missing subject denied', commit('alice', [write('estudiantes/alice/estados/unknown', { estado: 'APROBADA' })]), 403);
  await expect('state for other plan denied', commit('alice', [write('estudiantes/alice/estados/other-plan', { estado: 'APROBADA' })]), 403);
  for (const uid of ['unregistered', 'partial', 'bad-person', 'bad-plan']) {
    if (uid === 'partial') await expect('seed partial profile', commit('owner', [write(`estudiantes/${uid}`, { id_plan: plan })]), 200);
    if (uid === 'bad-person') await expect('seed invalid person profile', commit('owner', [
      ...register(uid), write(`personas/${uid}`, { nombres: '', apellidos: 'Prueba' })]), 200);
    if (uid === 'bad-plan') await expect('seed other plan profile', commit('owner', [
      ...register(uid), write(`estudiantes/${uid}`, { id_plan: 'otro-plan' })]), 200);
    // Subcollection fixture deliberately exists under a missing/invalid parent.
    await expect(`seed orphan state ${uid}`, commit('owner', [write(statePath(uid), { estado: 'APROBADA' })]), 200);
    await expect(`incomplete profile ${uid} cannot get state`, get(uid, statePath(uid)), 403);
    await expect(`incomplete profile ${uid} cannot list states`, list(uid, `estudiantes/${uid}/estados`), 403);
    await expect(`incomplete profile ${uid} cannot update state`, commit(uid, [write(statePath(uid), { estado: 'EN_CURSO' })]), 403);
    await expect(`incomplete profile ${uid} cannot create state`, commit(uid, [write(statePath(uid, 'BMS-300'), { estado: 'APROBADA' })]), 403);
    await expect(`incomplete profile ${uid} cannot delete state`, commit(uid, [remove(statePath(uid))]), 403);
  }
  await expect('registration and first state in one atomic batch allowed', commit('batch-user', [
    ...register('batch-user'), write(statePath('batch-user'), { estado: 'APROBADA' })]), 200);
  await expect('mixed valid and invalid state batch denied', commit('alice', [
    write(statePath('alice', 'BMS-300'), { estado: 'APROBADA' }),
    write(statePath('alice', 'BFG-300'), { estado: 'PENDIENTE' })]), 403);
  await expect('failed batch leaves valid part absent', get('alice', statePath('alice', 'BMS-300')), 404);
  await expect('return to pending deletes state', commit('alice', [remove(statePath('alice'))]), 200);
  await expect('deleted state is absent', get('alice', statePath('alice')), 404);
  await expect('owner can delete already absent state', commit('alice', [remove(statePath('alice'))]), 200);
  await expect('seed state whose catalog subject was removed', commit('owner', [
    write('estudiantes/alice/estados/retired', { estado: 'APROBADA' })]), 200);
  await expect('owner can read orphan for diagnosis', get('alice', 'estudiantes/alice/estados/retired'), 200);
  await expect('orphan update cannot reintroduce state', commit('alice', [write('estudiantes/alice/estados/retired', { estado: 'EN_CURSO' })]), 403);
  await expect('owner can clean orphan state', commit('alice', [remove('estudiantes/alice/estados/retired')]), 200);
  for (const doc of ['estudiantes/alice/ofertas/actual', `estudiantes/alice/ofertas/actual/materias/${subject('BMS-300').id}`,
    'estudiantes/alice/estados/unknown/nested/doc', 'unknown/doc']) {
    await expect(`future or unknown read denied ${doc}`, get('alice', doc), 403);
    await expect(`future or unknown write denied ${doc}`, commit('alice', [write(doc, {})]), 403);
  }
  await expect('remove career locally', commit('owner', [remove('carreras/ingenieria-sistemas')]), 200);
  await expect('missing career denies state read', list('alice', 'estudiantes/alice/estados'), 403);
  await expect('missing career denies state write', commit('alice', [write(statePath('alice'), { estado: 'APROBADA' })]), 403);
  await expect('restore local career', commit('owner', [write('carreras/ingenieria-sistemas', refs['carreras/ingenieria-sistemas'])]), 200);
  await expect('remove plan locally', commit('owner', [remove(`planes_estudio/${plan}`)]), 200);
  await expect('missing plan denies states query', query('alice', 'estados', null, 'estudiantes/alice'), 403);
  await expect('missing plan denies state deletion', commit('alice', [remove(statePath('alice'))]), 403);
  console.log(`\n${checks} academic rule checks passed. No production data touched.`);
})().catch(error => { console.error(error); process.exitCode = 1; });

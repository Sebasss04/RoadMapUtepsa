const { test } = require('node:test');
const assert = require('node:assert/strict');
const { expectedDocuments, planDocuments, creationWrites, BASE } = require('../scripts/academic-catalog-plan.cjs');
const expected = expectedDocuments();
test('empty catalog creates exactly 50 subjects and 28 empty edge docs with create-only preconditions', () => {
  const plan = planDocuments(expected, {});
  const writes = creationWrites(expected, plan);
  assert.equal(writes.length, 78);
  assert.equal(writes.filter(w => w.update.name.includes('/prerrequisitos/')).length, 28);
  for (const w of writes) {
    assert.deepEqual(w.currentDocument, { exists: false });
    assert(w.update.name.startsWith(`${BASE}/materias/`));
    assert(!Object.hasOwn(w.update.fields, 'id'));
    if (w.update.name.includes('/prerrequisitos/')) assert.deepEqual(w.update.fields, {});
    assert(!w.delete && !w.transform);
  }
});
test('exact second run performs zero writes', () => {
  const plan = planDocuments(expected, structuredClone(expected));
  assert.equal(plan.unchanged.length, 78);
  assert.deepEqual(creationWrites(expected, plan), []);
});
test('partial load creates only missing documents', () => {
  const [key] = Object.keys(expected);
  assert.equal(creationWrites(expected, planDocuments(expected, { [key]: expected[key] })).length, 77);
});
test('different name, extra fields, wrong type or nonempty edge blocks the entire load', () => {
  const subject = Object.keys(expected)[0];
  const edge = Object.keys(expected).find(k => k.includes('/prerrequisitos/'));
  for (const [key, fields] of [[subject, { ...expected[subject], nombre: { stringValue: 'changed' } }],
    [subject, { ...expected[subject], extra: { stringValue: 'bad' } }],
    [subject, { ...expected[subject], semestre: { stringValue: '1' } }], [edge, { extra: { stringValue: 'bad' } }]]) {
    const plan = planDocuments(expected, { [key]: fields });
    assert.equal(plan.conflicts.length, 1);
    assert.throws(() => creationWrites(expected, plan));
  }
});
test('duplicate code under another ID in this plan blocks; another plan is preserved', () => {
  const subject = Object.keys(expected)[0];
  assert.equal(planDocuments(expected, { 'materias/duplicate': expected[subject] }).conflicts.length, 1);
  assert.equal(planDocuments(expected, { 'materias/other': { id_plan: { stringValue: 'other' } } }).conflicts.length, 0);
});
test('extra/orphan edge under an approved parent blocks without deleting it', () => {
  const subject = Object.keys(expected)[0];
  const plan = planDocuments(expected, { [`${subject}/prerrequisitos/unexpected`]: {} });
  assert.equal(plan.conflicts.length, 1);
  assert.equal(plan.deletes, 0);
  assert.throws(() => creationWrites(expected, plan));
});

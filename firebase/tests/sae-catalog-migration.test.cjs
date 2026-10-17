const { test } = require('node:test');
const assert = require('node:assert/strict');
const { legacyDocuments, planMigration, migrationWrites } = require('../scripts/migrate-sae-catalog.cjs');
const { expectedDocuments, BASE } = require('../scripts/academic-catalog-plan.cjs');
const snapshot = docs => Object.fromEntries(Object.entries(docs).map(([key, fields]) => [key, { fields, updateTime: '2026-10-17T12:00:00Z' }]));

test('migration updates 50 stable subjects and exactly the five reviewed edge changes', () => {
  const remote = snapshot(legacyDocuments());
  const plan = planMigration(remote), writes = migrationWrites(remote, plan);
  assert.deepEqual([plan.updates.length, plan.creates.length, plan.deletes.length], [50, 2, 3]);
  assert.equal(writes.length, 55);
  for (const write of writes) {
    const name = write.update?.name || write.delete;
    assert(name.startsWith(`${BASE}/materias/`));
    if (write.delete) assert(write.delete.includes('/prerrequisitos/'));
    assert(write.currentDocument.updateTime || write.currentDocument.exists === false);
  }
  for (const write of writes) {
    const key = (write.update?.name || write.delete).slice(BASE.length + 1);
    if (write.delete) delete remote[key];
    else remote[key] = { fields: write.update.fields, updateTime: '2026-10-17T13:00:00Z' };
  }
  assert.equal(planMigration(remote).state, 'already-applied');
  assert.deepEqual(migrationWrites(remote, planMigration(remote)), []);
});

test('partial, changed, extra or unknown catalog blocks every write', () => {
  for (const change of [
    docs => { delete docs[Object.keys(docs)[0]]; },
    docs => { docs[Object.keys(docs)[0]].fields.nombre = { stringValue: 'Unexpected' }; },
    docs => { docs['materias/unknown'] = { fields: {}, updateTime: 'now' }; },
    docs => { docs[Object.keys(docs)[0]].fields.horas_totales = { integerValue: '170' }; }
  ]) {
    const docs = snapshot(legacyDocuments()); change(docs);
    assert.throws(() => planMigration(docs), /refusing/);
  }
});

test('callers cannot broaden scope or omit version preconditions', () => {
  const remote = snapshot(legacyDocuments()), plan = planMigration(remote);
  assert.throws(() => migrationWrites(remote, { ...plan, deletes: [...plan.deletes, 'estudiantes/private'] }));
  delete remote[plan.updates[0]].updateTime;
  assert.throws(() => migrationWrites(remote, plan), /updateTime/);
});

test('SAE target classifies 40 modular, nine bimodular and one special, preserving all UUIDs', () => {
  const target = expectedDocuments(), legacy = legacyDocuments();
  const subjects = Object.keys(target).filter(key => !key.includes('/prerrequisitos/'));
  assert.deepEqual(subjects.sort(), Object.keys(legacy).filter(key => !key.includes('/prerrequisitos/')).sort());
  const hours = subjects.map(key => target[key].horas_totales.integerValue);
  assert.equal(hours.filter(value => value === '85').length, 40);
  assert.equal(hours.filter(value => value === '170').length, 9);
  assert.equal(hours.filter(value => value === '400').length, 1);
});

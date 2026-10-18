const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
test('Offer rules allowlist equals all 50 technical catalog identities', () => {
  const rules = fs.readFileSync(path.join(__dirname, '../firestore.rules'), 'utf8');
  const body = rules.split('function offerSubjectIds() {')[1].split('function validOffer')[0];
  const actual = [...body.matchAll(/'([a-f0-9-]{36})'/g)].map(m => m[1]);
  const catalog = JSON.parse(fs.readFileSync(path.join(__dirname, '../catalogo/sistemas-rm-615-2016.json')));
  assert.equal(actual.length, 50);
  assert.equal(new Set(actual).size, 50);
  assert.deepEqual(actual.sort(), catalog.materias.map(m => m.id).sort());
});

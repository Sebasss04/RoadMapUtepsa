const assert = require('node:assert/strict');
const crypto = require('node:crypto');
const fs = require('node:fs');
const path = require('node:path');
const PROJECT = 'roadmap-utepsa-c9409';
const DATABASE = '(default)';
const BASE = `projects/${PROJECT}/databases/${DATABASE}/documents`;
const APPROVED_SHA256 = '0a7f05273ea814081c1b2060fc270504a27efadcb3dd796f2cff9be3aef6286f';
const sha256 = value => crypto.createHash('sha256').update(value).digest('hex');
function expectedDocuments(file = path.join(__dirname, '../catalogo/sistemas-rm-615-2016.json')) {
  const bytes = fs.readFileSync(file);
  assert.equal(sha256(bytes), APPROVED_SHA256, 'Catalog differs from the reviewed package; review required.');
  const catalog = JSON.parse(bytes.toString('utf8').replace(/^\uFEFF/, ''));
  const documents = {};
  for (const { id, ...data } of catalog.materias) {
    documents[`materias/${id}`] = Object.fromEntries(Object.entries(data).map(([key, value]) =>
      [key, typeof value === 'number' ? { integerValue: String(value) } : { stringValue: value }]));
  }
  for (const edge of catalog.prerrequisitos) documents[`materias/${edge.id_objetivo}/prerrequisitos/${edge.id_requisito}`] = {};
  assert.equal(Object.keys(documents).length, 78);
  return documents;
}
function planDocuments(expected, remote) {
  const create = [], unchanged = [], conflicts = [];
  for (const [document, fields] of Object.entries(expected)) {
    if (!Object.hasOwn(remote, document)) create.push(document);
    else {
      try { assert.deepEqual(remote[document], fields); unchanged.push(document); }
      catch { conflicts.push({ path: document, reason: 'Existing fields differ; overwrite forbidden.' }); }
    }
  }
  // Other plans can coexist. Extra subjects of this plan and extra edges of our
  // subjects cannot: the Android mapper rejects incomplete or altered catalogs.
  for (const [document, fields] of Object.entries(remote)) {
    if (!Object.hasOwn(expected, document) && (fields.id_plan?.stringValue === 'sistemas-rm-615-2016' ||
        (document.includes('/prerrequisitos/') && Object.hasOwn(expected, document.split('/prerrequisitos/')[0])))) {
      conflicts.push({ path: document, reason: 'Unexpected document in approved plan; no deletion allowed.' });
    }
  }
  return { create: create.sort(), unchanged: unchanged.sort(), conflicts, overwrites: 0, deletes: 0 };
}
function creationWrites(expected, plan) {
  assert.equal(plan.conflicts.length, 0, 'Remote conflicts; refusing all writes.');
  return plan.create.map(document => {
    assert(Object.hasOwn(expected, document));
    return { update: { name: `${BASE}/${document}`, fields: expected[document] }, currentDocument: { exists: false } };
  });
}
module.exports = { PROJECT, DATABASE, BASE, APPROVED_SHA256, sha256, expectedDocuments, planDocuments, creationWrites };

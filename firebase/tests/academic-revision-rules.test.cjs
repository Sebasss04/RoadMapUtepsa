'use strict';
// Contract and adversarial checks, strictly local synthetic users.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
assert.equal(process.env.FIRESTORE_EMULATOR_HOST, '127.0.0.1:8089');
assert.equal(process.env.GCLOUD_PROJECT, 'demo-roadmap-utepsa');
const project = 'demo-roadmap-utepsa';
const root = `projects/${project}/databases/(default)/documents`;
const endpoint = `http://127.0.0.1:8089/v1/${root}`;
const uid = `revision-${Date.now()}`;
const b64 = v => Buffer.from(JSON.stringify(v)).toString('base64url');
const token = user => `${b64({ alg: 'none', typ: 'JWT' })}.${b64({ aud: project,
  iss: `https://securetoken.google.com/${project}`, sub: user, user_id: user,
  iat: Math.floor(Date.now()/1000), exp: Math.floor(Date.now()/1000)+3600,
  firebase: { sign_in_provider: 'google.com', identities: {} } })}.`;
const headers = user => ({ 'Content-Type': 'application/json',
  ...(user ? { Authorization: `Bearer ${user === 'owner' ? 'owner' : token(user)}` } : {}) });
const enc = v => typeof v === 'string' ? { stringValue: v } : typeof v === 'boolean' ? { booleanValue: v }
  : typeof v === 'number' ? { integerValue: String(v) } : v;
const write = (doc, fields) => ({ update: { name: `${root}/${doc}`,
  fields: Object.fromEntries(Object.entries(fields).map(([k,v])=>[k,enc(v)])) } });
const revisionPath = `estudiantes/${uid}/control_academico/revision`;
const rev = (valor, extra = {}) => write(revisionPath, { version_formato: 1, valor, ...extra });
const catalog = JSON.parse(fs.readFileSync(path.join(__dirname, '../catalogo/sistemas-rm-615-2016.json')));
const statePath = i => `estudiantes/${uid}/estados/${catalog.materias[i].id}`;
const state = (i, estado = 'APROBADA') => write(statePath(i), { estado });
const commit = (user,writes) => fetch(`${endpoint}:commit`, { method:'POST', headers:headers(user), body:JSON.stringify({writes}) });
const cases = [];
async function check(name, request, expected) {
  const response = await request;
  const detail = await response.text();
  assert.equal(response.status, expected, `${name}: ${detail}`);
  cases.push({name,status:response.status});
  console.log(`PASS ${name}`);
}
(async()=>{
  const refs = JSON.parse(fs.readFileSync(path.join(__dirname,'../profile-references.json')));
  await check('seed synthetic profile and readonly catalog',commit('owner',[
    ...Object.entries(refs).map(([p,v])=>write(p,v)),
    ...catalog.materias.map(({id,...data})=>write(`materias/${id}`,data)),
    write(`usuarios/${uid}`,{fecha_creacion:{timestampValue:new Date().toISOString()}}),
    write(`personas/${uid}`,{nombres:'Revision',apellidos:'Local'}),
    write(`estudiantes/${uid}`,{id_plan:'sistemas-rm-615-2016'})]),200);
  await check('legacy state creation without revision denied',commit(uid,[state(0)]),403);
  await check('atomic first revision and state accepted',commit(uid,[state(0),rev(1)]),200);
  await check('legacy update denied',commit(uid,[state(0,'EN_CURSO')]),403);
  await check('legacy delete denied',commit(uid,[{delete:`${root}/${statePath(0)}`}]),403);
  await check('legacy absent delete denied',commit(uid,[{delete:`${root}/${statePath(1)}`}]),403);
  await check('stale revision cannot overwrite',commit(uid,[state(0,'EN_CURSO'),rev(1)]),403);
  for (const [name,value,extra] of [['negative',-1,{}],['zero',0,{}],['jump',3,{}],
    ['wrong type','2',{}],['double',{doubleValue:2},{}],['schema pollution',2,{admin:true}],
    ['wrong version',2,{version_formato:2}],['overflow',{integerValue:'9223372036854775807'},{}]]) {
    await check(`revision ${name} denied`,commit(uid,[state(1),rev(value,extra)]),403);
  }
  await check('revision delete denied',commit(uid,[{delete:`${root}/${revisionPath}`}]),403);
  for (const user of [null,`${uid}-other`]) {
    await check('foreign/unauth revision read denied',fetch(`${endpoint}/${revisionPath}`,{headers:headers(user)}),403);
    await check('foreign/unauth revision write denied',commit(user,[rev(2)]),403);
  }
  await check('revision list denied',fetch(`${endpoint}/estudiantes/${uid}/control_academico`,{headers:headers(uid)}),403);
  await check('atomic valid update and increment accepted',commit(uid,[state(0,'EN_CURSO'),rev(2)]),200);
  await check('atomic valid delete and increment accepted',commit(uid,[{delete:`${root}/${statePath(0)}`},rev(3)]),200);
  // The worst final group includes five new states, journal, config and revision.
  const ids = catalog.materias.slice(0,5).map(m=>m.id);
  const operation = { version_formato:1,id_operacion:require('node:crypto').randomUUID(),
    huella_base:'a'.repeat(64),huella_objetivo:'b'.repeat(64),revision_preparacion:'AUSENTE',
    cambios:{mapValue:{fields:Object.fromEntries(ids.map(id=>[id,{stringValue:'PA'}]))}},
    aplicados:{arrayValue:{values:[]}},estado:'EN_PROGRESO' };
  const journal = data => ({...write(`estudiantes/${uid}/operaciones_historial/actual`,data),
    updateTransforms:[{fieldPath:'actualizado_en',setToServerValue:'REQUEST_TIME'}]});
  await check('legacy journal without revision denied',commit(uid,[journal(operation)]),403);
  await check('start journal atomically with revision',commit(uid,[journal(operation),rev(4)]),200);
  const config = {...write(`estudiantes/${uid}/configuracion_academica/historial`,{version_formato:1,decision:'CONFIRMADO'}),
    updateTransforms:[{fieldPath:'actualizado_en',setToServerValue:'REQUEST_TIME'}]};
  await check('final group of five plus journal config revision accepted',commit(uid,[
    ...ids.map((_,i)=>state(i)), journal({...operation,estado:'COMPLETADA',
      aplicados:{arrayValue:{values:ids.map(id=>({stringValue:id}))}}}),config,rev(5)]),200);
  const out=path.join(__dirname,'../../app/build/reports/optimization-revision-rules.json');
  fs.writeFileSync(out,JSON.stringify({success:true,environment:'local-demo-only',cases},null,2));
})().catch(e=>{console.error(e);process.exitCode=1});

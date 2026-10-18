// Synthetic identities and dedicated demo emulator only. No production transport.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
assert.equal(process.env.FIRESTORE_EMULATOR_HOST, '127.0.0.1:8089');
assert.equal(process.env.GCLOUD_PROJECT, 'demo-roadmap-utepsa');
const project = 'demo-roadmap-utepsa';
const root = `projects/${project}/databases/(default)/documents`;
const endpoint = `http://127.0.0.1:8089/v1/${root}`;
const uid = `offer-${Date.now()}`;
const b64 = v => Buffer.from(JSON.stringify(v)).toString('base64url');
const token = (user, provider = "google.com") => `${b64({alg:'none',typ:'JWT'})}.${b64({aud:project,iss:`https://securetoken.google.com/${project}`,sub:user,user_id:user,iat:Math.floor(Date.now()/1000),exp:Math.floor(Date.now()/1000)+3600,firebase:{sign_in_provider:provider,identities:{}}})}.`;
const headers = user => ({'Content-Type':'application/json',...(user ? {Authorization:`Bearer ${user === 'owner' ? 'owner' : token(user)}`} : {})});
const enc = v => typeof v === 'string' ? {stringValue:v} : typeof v === 'number' ? {integerValue:String(v)} : Array.isArray(v) ? {arrayValue:{values:v.map(enc)}} : v;
const write = (doc,fields) => ({update:{name:`${root}/${doc}`,fields:Object.fromEntries(Object.entries(fields).map(([k,v])=>[k,enc(v)]))}});
const catalog = JSON.parse(fs.readFileSync(path.join(__dirname,'../catalogo/sistemas-rm-615-2016.json')));
const ids = catalog.materias.map(m=>m.id);
const offerPath = `estudiantes/${uid}/ofertas/actual`;
const offer = (materias=[],revision=1,extra={}) => ({...write(offerPath,{version_formato:1,id_plan:'sistemas-rm-615-2016',materias,revision,...extra}),updateTransforms:[{fieldPath:'actualizado_en',setToServerValue:'REQUEST_TIME'}]});
const commit = (user,writes) => fetch(`${endpoint}:commit`,{method:'POST',headers:headers(user),body:JSON.stringify({writes})});
const cases=[];
async function check(name,request,status) {
 const response=await request; const text=await response.text();
 assert.equal(response.status,status,`${name}: ${text}`); cases.push({name,status}); console.log(`PASS ${name}`);
}
(async()=>{
 const refs=JSON.parse(fs.readFileSync(path.join(__dirname,'../profile-references.json')));
 await check('seed demo profiles',commit('owner',[
  ...Object.entries(refs).map(([p,d])=>write(p,d)),
  write(`usuarios/${uid}`,{fecha_creacion:{timestampValue:new Date().toISOString()}}),
  write(`personas/${uid}`,{nombres:'Oferta',apellidos:'Prueba'}),
  write(`estudiantes/${uid}`,{id_plan:'sistemas-rm-615-2016'})]),200);
 await check('owner confirmed absence',fetch(`${endpoint}/${offerPath}`,{headers:headers(uid)}),404);
 for (const user of [null,uid+'-other']) {
  await check('anonymous or third party read denied',fetch(`${endpoint}/${offerPath}`,{headers:headers(user)}),403);
  await check('anonymous or third party create denied',commit(user,[offer()]),403);
 }
 await check('incomplete profile cannot read',fetch(`${endpoint}/estudiantes/${uid}-incomplete/ofertas/actual`,{headers:headers(uid+'-incomplete')}),403);
 await check('incomplete profile cannot write',commit(uid+'-incomplete',[{...offer(),update:{...offer().update,name:`${root}/estudiantes/${uid}-incomplete/ofertas/actual`}}]),403);
 for (const [name,list,revision,extra] of [
  ['foreign UUID',['00000000-0000-0000-0000-000000000000'],1,{}],
  ['duplicates',[ids[0],ids[0]],1,{}],['over fifty',[...ids,ids[0]],1,{}],
  ['non-list',{stringValue:'invalid'},1,{}],['extra field',[],1,{admin:{booleanValue:true}}],
  ['wrong plan',[],1,{id_plan:'other'}],['wrong version',[],1,{version_formato:2}],
  ['format float',[],1,{version_formato:{doubleValue:1}}],['initial revision not one',[],2,{}],
  ['zero revision',[],0,{}],['float revision',[],{doubleValue:1},{}]
 ]) await check(`${name} denied`,commit(uid,[offer(list,revision,extra)]),403);
 const badTime=offer(); delete badTime.updateTransforms;
 badTime.update.fields.actualizado_en={timestampValue:'2020-01-01T00:00:00Z'};
 await check('client timestamp denied',commit(uid,[badTime]),403);
 const missing=offer(); delete missing.update.fields.materias;
 await check('missing field denied',commit(uid,[missing]),403);
 const anonHeaders = {'Content-Type':'application/json',Authorization:`Bearer ${token(uid, 'anonymous')}`};
 await check('anonymous auth with complete profile cannot read',fetch(`${endpoint}/${offerPath}`,{headers:anonHeaders}),403);
 await check('anonymous auth with complete profile cannot write',fetch(`${endpoint}:commit`,{method:'POST',headers:anonHeaders,body:JSON.stringify({writes:[offer()]})}),403);
 await check('all fifty permitted within rules read limits',commit(uid,[offer(ids)]),200);
 await check('owner reads saved offer',fetch(`${endpoint}/${offerPath}`,{headers:headers(uid)}),200);
 await check('stale revision denied',commit(uid,[offer([],1)]),403);
 await check('revision skip denied',commit(uid,[offer([],3)]),403);
 await check('replace selection',commit(uid,[offer([ids[49]],2)]),200);
 await check('registered empty accepted',commit(uid,[offer([],3)]),200);
 await check('delete denied',commit(uid,[{delete:`${root}/${offerPath}`}]),403);
 await check('list denied',fetch(`${endpoint}/estudiantes/${uid}/ofertas`,{headers:headers(uid)}),403);
 const other=offer([],1); other.update.name=`${root}/estudiantes/${uid}/ofertas/other`;
 await check('other offer document denied',commit(uid,[other]),403);
 for (const user of [null,uid+'-other']) await check('foreign update denied',commit(user,[offer([],4)]),403);
 await check('offer does not create global academic revision',fetch(`${endpoint}/estudiantes/${uid}/control_academico/revision`,{headers:headers(uid)}),404);
 const out=path.join(__dirname,'../../app/build/reports/offer'); fs.mkdirSync(out,{recursive:true});
 fs.writeFileSync(path.join(out,'rules.json'),JSON.stringify({success:true,project,cases},null,2));
})().catch(e=>{console.error(e);process.exitCode=1});

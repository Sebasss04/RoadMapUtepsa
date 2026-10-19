// All transports are fixed to demo localhost. No production endpoint or personal fixtures.
const assert = require('node:assert/strict');
assert.equal(process.env.FIRESTORE_EMULATOR_HOST, '127.0.0.1:8089');
assert.equal(process.env.GCLOUD_PROJECT, 'demo-roadmap-utepsa');
const project = 'demo-roadmap-utepsa';
const base = `projects/${project}/databases/(default)/documents`;
const endpoint = `http://127.0.0.1:8089/v1/${base}`;
const b64 = value => Buffer.from(JSON.stringify(value)).toString('base64url');
const uid = `account-security-${Date.now()}`;
const headers = (provider, verified, subject = uid) => ({'Content-Type':'application/json', Authorization: `Bearer ${b64({alg:'none',typ:'JWT'})}.${b64({
  aud:project,iss:`https://securetoken.google.com/${project}`,sub:subject,user_id:subject,
  iat:Math.floor(Date.now()/1000),exp:Math.floor(Date.now()/1000)+3600,email_verified:verified,
  firebase:{sign_in_provider:provider,identities:{}}})}.`});
async function check(label, response, status) {
  const result = await response; const text = await result.text();
  assert.equal(result.status, status, `${label}: ${text}`); console.log(`PASS ${label}`);
}
(async () => {
  for (const path of [`usuarios/${uid}`,`personas/${uid}`,`estudiantes/${uid}`,
    `estudiantes/${uid}/estados/fixture`,`estudiantes/${uid}/ofertas/actual`,
    `estudiantes/${uid}/control_academico/revision`,`estudiantes/${uid}/configuracion_academica/historial`,
    `estudiantes/${uid}/operaciones_historial/actual`]) {
    await check(`unverified password cannot read ${path}`, fetch(`${endpoint}/${path}`, {headers:headers('password', false)}),403);
  }
  const write = {update:{name:`${base}/personas/${uid}`,fields:{nombres:{stringValue:'Demo'},apellidos:{stringValue:'Cuenta'}}}};
  await check('unverified password cannot write profile',fetch(`${endpoint}:commit`,{method:'POST',headers:headers('password',false),body:JSON.stringify({writes:[write]})}),403);
  await check('verified password can read own absent profile',fetch(`${endpoint}/personas/${uid}`,{headers:headers('password',true)}),404);
  await check('Google can read own absent profile',fetch(`${endpoint}/personas/${uid}`,{headers:headers('google.com',true)}),404);
  await check('anonymous cannot read profile',fetch(`${endpoint}/personas/${uid}`,{headers:headers('anonymous',true)}),403);
  await check('verified other account cannot read profile',fetch(`${endpoint}/personas/${uid}`,{headers:headers('password',true,uid+'-other')}),403);
})().catch(error => {console.error(error.message);process.exitCode=1;});

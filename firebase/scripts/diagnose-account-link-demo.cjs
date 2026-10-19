// Reproduce against the fixed local demo Auth emulator. Never contact production.
// A failed link remains a failed command, even when identity preservation passes.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const base = 'http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/';
const project = 'demo-roadmap-utepsa';
const report = { project, productionContacted: false, linkSucceeded: false };
const email = `link-diagnosis-${require('node:crypto').randomUUID()}@example.test`;
const password = 'Demo-only-password-2026';
const b64 = value => Buffer.from(JSON.stringify(value)).toString('base64url');
const token = b64({alg:'none',typ:'JWT'}) + '.' + b64({
  iss:'https://accounts.google.com', aud:'demo-client', sub:email, email,
  email_verified:true, iat:Math.floor(Date.now()/1000), exp:Math.floor(Date.now()/1000)+3600
}) + '.';
async function call(method, body) {
  const response = await fetch(`${base}${method}?key=local-emulator-only-key`, {
    method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify(body),
    signal:AbortSignal.timeout(15000)
  });
  return {ok:response.ok, status:response.status, data:await response.json()};
}
function successful(result) {
  assert(result.ok, `Demo request failed: ${result.data.error?.message}`);
  return result.data;
}
function demoToken(idToken) {
  assert.equal(JSON.parse(Buffer.from(idToken.split('.')[1], 'base64url')).aud, project);
}
const googleBody = {requestUri:'http://localhost', postBody:`providerId=google.com&id_token=${token}`, returnSecureToken:true};
(async () => {
  const first = successful(await call('accounts:signInWithIdp', googleBody));
  demoToken(first.idToken);
  const reauth = successful(await call('accounts:signInWithIdp', {...googleBody, autoCreate:false}));
  assert.equal(reauth.localId, first.localId);
  demoToken(reauth.idToken);
  report.reauthenticationSameUid = true;
  const link = await call('accounts:signUp', {idToken:reauth.idToken, email, password, returnSecureToken:true});
  report.linkSucceeded = link.ok;
  report.linkStatus = link.status;
  report.linkError = link.data.error?.message;
  const after = successful(await call('accounts:lookup', {idToken:reauth.idToken})).users[0];
  assert.equal(after.localId, first.localId);
  report.uidPreserved = true;
  report.providersAfterLink = after.providerUserInfo.map(provider => provider.providerId).sort();
  if (!link.ok) {
    assert.deepEqual(report.providersAfterLink, ['google.com']);
    const googleAgain = successful(await call('accounts:signInWithIdp', googleBody));
    assert.equal(googleAgain.localId, first.localId);
    report.googleStillRecoversSameUid = true;
    const passwordAttempt = await call('accounts:signInWithPassword', {email, password, returnSecureToken:true});
    assert.equal(passwordAttempt.ok, false);
    report.passwordAccessStillRejected = true;
    throw Error(`Google -> password link failed: ${report.linkError}`);
  }
  assert.equal(link.data.localId, first.localId);
  assert.deepEqual(report.providersAfterLink, ['google.com', 'password']);
  const passwordLogin = successful(await call('accounts:signInWithPassword', {email, password, returnSecureToken:true}));
  assert.equal(passwordLogin.localId, first.localId);
  report.passwordRecoversSameUid = true;
})().catch(error => {
  report.failure = error.message;
  process.exitCode = 1;
}).finally(() => {
  const out = path.resolve(__dirname, '../../app/build/reports/account-auth');
  fs.mkdirSync(out, {recursive:true});
  fs.writeFileSync(path.join(out, 'link-demo-diagnosis.json'), JSON.stringify(report, null, 2));
  console.log(JSON.stringify(report, null, 2));
});

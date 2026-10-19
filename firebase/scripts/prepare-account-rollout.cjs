// Local preparation from the read-only remote snapshot. Does not publish or contact document APIs.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const crypto = require('node:crypto');
const {tokens, block} = require('./compare-history-rules.cjs');
const root = path.resolve(__dirname, '../..');
const out = path.join(root, 'app/build/reports/account-auth');
const before = path.join(out, 'remote-before-review');
const comparison = JSON.parse(fs.readFileSync(path.join(before, 'comparison.json')));
assert.equal(comparison.outcome, 'compared'); assert(comparison.releaseStableDuringRead);
const remote = fs.readFileSync(path.join(before, 'firestore.remote.rules'), 'utf8');
const local = fs.readFileSync(path.join(root, 'firebase/firestore.rules'), 'utf8');
const reviewed = 'function isOwner(uid)';
assert.equal(tokens(local.replace(block(local, reviewed), '')), tokens(remote.replace(block(remote, reviewed), '')),
  'Unexpected change outside the reviewed identity gate. Re-review before rollout.');
const proposal = JSON.parse(fs.readFileSync(path.join(root, 'firebase/account-auth-config.proposed.json')));
assert.equal(proposal.project, 'roadmap-utepsa-c9409');
assert.equal(proposal.updateMask, 'signIn.email.enabled,signIn.email.passwordRequired');
assert.deepEqual(proposal.body, {signIn:{email:{enabled:true,passwordRequired:true}}});
const sha = content => crypto.createHash('sha256').update(content).digest('hex');
const report = {preparedAt: new Date().toISOString(), publicationAttempted: false,
  project: proposal.project, database: '(default)', reviewedChange: reviewed,
  remainingRulesIdenticalTokens: true, remoteRulesSha256: comparison.remoteRulesSha256,
  candidateRulesSha256: sha(local), remoteRelease: comparison.release,
  authProposalSha256: sha(fs.readFileSync(path.join(root, 'firebase/account-auth-config.proposed.json'))),
  order: ['Recheck release and candidate hashes', 'Publish verified identity gate', 'Read back rules and hash',
    'Enable email/password using exact proposed update mask', 'Read back provider and retain templates/privacy policy',
    'Student checks real email, both methods with same UID, and biometrics on updated V2529']};
fs.mkdirSync(out, {recursive:true});
fs.writeFileSync(path.join(out, 'rollout-prepared.json'), JSON.stringify(report,null,2));
console.log(JSON.stringify(report,null,2));

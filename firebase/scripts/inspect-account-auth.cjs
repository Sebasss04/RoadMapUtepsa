// Read-only production configuration check. No users, credentials or message bodies are exported.
const path = require('node:path');
const fs = require('node:fs');
const root = path.resolve(__dirname, '../..');
const lib = process.env.ROADMAP_FIREBASE_TOOLS_LIB;
if (!lib) throw new Error('Set ROADMAP_FIREBASE_TOOLS_LIB to firebase-tools/lib');
const project = 'roadmap-utepsa-c9409';
(async () => {
  const auth = require(path.join(lib, 'auth'));
  const account = auth.getGlobalDefaultAccount();
  if (!account) throw new Error('Firebase CLI login is required for read-only inspection');
  const options = { project, nonInteractive: true };
  auth.setActiveAccount(options, account);
  await require(path.join(lib, 'requireAuth')).requireAuth(options);
  const { Client } = require(path.join(lib, 'apiv2'));
  const client = new Client({ auth: true, urlPrefix: 'https://identitytoolkit.googleapis.com' });
  const { body } = await client.get(`/admin/v2/projects/${project}/config`, { headers: { 'x-goog-user-project': project } });
  const report = { project, inspectedAt: new Date().toISOString(), readOnly: true,
    emailPassword: body.signIn?.email || {enabled: false, passwordRequired: false},
    passwordPolicy: body.passwordPolicyConfig || {configured: false},
    enumerationProtection: body.emailPrivacyConfig,
    notificationLocale: body.notification?.defaultLocale || 'default',
    templates: Object.fromEntries(Object.entries(body.notification?.sendEmail || {}).map(([key, value]) =>
      [key, value && typeof value === 'object' ? { configured: true, method: value.method,
        subjectConfigured: !!value.subject, bodyConfigured: !!value.body,
        actionLinkPlaceholder: typeof value.body === 'string' && value.body.includes('%LINK%') } : value])) };
  const out = path.join(root, 'app/build/reports/account-auth'); fs.mkdirSync(out, {recursive: true});
  fs.writeFileSync(path.join(out, 'production-config.json'), JSON.stringify(report, null, 2));
  console.log(JSON.stringify(report, null, 2));
})().catch(error => { console.error(error.message); process.exitCode = 1; });

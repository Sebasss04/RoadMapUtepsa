// Execute both suites serially against the dedicated emulator and save evidence.
const { spawnSync } = require('node:child_process');
const fs = require('node:fs');
const path = require('node:path');
if (process.env.FIRESTORE_EMULATOR_HOST !== '127.0.0.1:8089') throw new Error('Dedicated emulator required');
if (process.env.GCLOUD_PROJECT !== 'demo-roadmap-utepsa') throw new Error('Demo project required');
const root = path.resolve(__dirname, '../..');
const output = path.join(root, 'app/build/reports/firestore-rules-step3');
fs.mkdirSync(output, { recursive: true });
const suites = [];
for (const file of ['profile-rules.test.cjs', 'academic-rules.test.cjs']) {
  const result = spawnSync(process.execPath, [path.join(__dirname, file)], { encoding: 'utf8', timeout: 180000 });
  const stdout = result.stdout || '';
  const stderr = result.stderr || '';
  process.stdout.write(stdout);
  process.stderr.write(stderr);
  fs.writeFileSync(path.join(output, `${file}.log`), stdout + stderr);
  suites.push({ file, exitCode: result.status, error: result.error?.message,
    checks: stdout.split(/\r?\n/).filter(line => line.startsWith('PASS ')).map(line => line.slice(5)) });
  if (result.status !== 0 || result.error) break;
}
const report = { project: 'demo-roadmap-utepsa', host: process.env.FIRESTORE_EMULATOR_HOST,
  success: suites.length === 2 && suites.every(s => s.exitCode === 0 && !s.error), suites };
fs.writeFileSync(path.join(output, 'resultados.json'), JSON.stringify(report, null, 2) + '\n');
process.exitCode = report.success ? 0 : 1;

const { execFile } = require('child_process');
const path = require('path');
const fs = require('fs');
const http = require('http');

const edgePath = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';
const outDir = path.join(__dirname, 'screenshots');
if (!fs.existsSync(outDir)) fs.mkdirSync(outDir, { recursive: true });

function post(url, data) {
  return new Promise((resolve, reject) => {
    const u = new URL(url);
    const body = JSON.stringify(data);
    const req = http.request({
      hostname: u.hostname,
      port: u.port,
      path: u.pathname,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(body)
      }
    }, res => {
      let d = ''; res.on('data', c => d += c);
      res.on('end', () => resolve(JSON.parse(d)));
    });
    req.on('error', reject);
    req.write(body);
    req.end();
  });
}

function capture(url, filename) {
  return new Promise((resolve, reject) => {
    const targetFile = path.join(outDir, filename);
    execFile(edgePath, [
      '--headless',
      '--disable-gpu',
      `--screenshot=${targetFile}`,
      '--window-size=1440,960',
      '--virtual-time-budget=2000',
      url
    ], (error) => {
      if (error) {
        reject(error);
      } else {
        const sz = fs.statSync(targetFile).size;
        console.log(`[PASS] ${filename}: ${sz} bytes`);
        resolve({ filename, size: sz });
      }
    });
  });
}

async function main() {
  console.log('Authenticating as admin & officer...');
  const adminRes = await post('http://127.0.0.1:8080/api/auth/login', { username: 'admin', password: 'Admin@123' });
  const adminUser = adminRes.data;
  const adminAuthParam = encodeURIComponent(JSON.stringify(adminUser));

  const officerRes = await post('http://127.0.0.1:8080/api/auth/login', { username: 'officer_police', password: 'Officer@123' });
  const officerUser = officerRes.data;
  const officerAuthParam = encodeURIComponent(JSON.stringify(officerUser));

  const pages = [
    { url: 'http://127.0.0.1:5173/file', file: '02-file-grievance.png' },
    { url: 'http://127.0.0.1:5173/track?id=RSK-2026-001', file: '03-track-grievance.png' },
    { url: `http://127.0.0.1:5173/admin?__auth=${adminAuthParam}`, file: '04-admin-dashboard.png' },
    { url: `http://127.0.0.1:5173/officer?__auth=${officerAuthParam}`, file: '05-officer-dashboard.png' },
    { url: `http://127.0.0.1:5173/complaints/1?__auth=${adminAuthParam}`, file: '06-complaint-details.png' },
    { url: `http://127.0.0.1:5173/reminders?__auth=${adminAuthParam}`, file: '07-reminder-centre.png' },
    { url: `http://127.0.0.1:5173/escalations?__auth=${adminAuthParam}`, file: '08-escalation-centre.png' },
    { url: `http://127.0.0.1:5173/history?__auth=${adminAuthParam}`, file: '09-action-history.png' },
    { url: `http://127.0.0.1:5173/analytics?__auth=${adminAuthParam}`, file: '10-analytics.png' },
    { url: `http://127.0.0.1:5173/settings?__auth=${adminAuthParam}`, file: '11-settings.png' }
  ];

  for (const p of pages) {
    await capture(p.url, p.file);
  }

  console.log('\nAll 10 pages successfully rendered and captured with 0 errors.');
}

main().catch(console.error);

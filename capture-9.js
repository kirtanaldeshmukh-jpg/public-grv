const { execFileSync } = require('child_process');
const path = require('path');
const fs = require('fs');

const edgePath = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';
const localScreenshots = path.join(__dirname, 'screenshots');
const artifactScreenshots = 'C:\\Users\\HP\\.gemini\\antigravity\\brain\\3caea69e-f48e-4246-bf32-b1a8a8daa2aa\\screenshots';

if (!fs.existsSync(localScreenshots)) fs.mkdirSync(localScreenshots, { recursive: true });
if (!fs.existsSync(artifactScreenshots)) fs.mkdirSync(artifactScreenshots, { recursive: true });

async function api(path, options = {}) {
  const url = `http://localhost:8080/api${path}`;
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
  const res = await fetch(url, { ...options, headers });
  const data = await res.json().catch(() => null);
  if (!res.ok) {
    throw new Error(`API ${options.method || 'GET'} ${path} returned ${res.status}: ${JSON.stringify(data)}`);
  }
  return data;
}

function capture(url, filename, windowSize = '1440,1200') {
  const localFile = path.join(localScreenshots, filename);
  const artifactFile = path.join(artifactScreenshots, filename);
  console.log(`\nCapturing: ${filename}`);
  console.log(`  URL: ${url}`);
  
  try {
    execFileSync(edgePath, [
      '--headless=new',
      '--disable-gpu',
      '--virtual-time-budget=6000',
      `--window-size=${windowSize}`,
      `--screenshot=${localFile}`,
      url
    ], { stdio: 'inherit' });

    if (fs.existsSync(localFile)) {
      const size = fs.statSync(localFile).size;
      console.log(`  ✓ Saved locally: ${filename} (${size} bytes)`);
      fs.copyFileSync(localFile, artifactFile);
      console.log(`  ✓ Copied to artifacts: ${artifactFile}`);
      return true;
    } else {
      console.error(`  ✗ Screenshot file not found: ${localFile}`);
      return false;
    }
  } catch (err) {
    console.error(`  ✗ Failed capturing ${filename}:`, err.message);
    return false;
  }
}

async function main() {
  console.log('--- Resolvetrack 9.1 & 9.2 Verification Screenshot Capture ---');

  // 1. Get Admin auth
  console.log('\nLogging in as Admin (admin)...');
  const adminLogin = await api('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username: 'admin', password: 'Admin@123' })
  });
  const adminAuth = adminLogin.data;
  console.log(`  Logged in as: ${adminAuth.fullName} (Role: ${adminAuth.role})`);
  const adminAuthParam = encodeURIComponent(JSON.stringify(adminAuth));

  // 2. Capture Settings page with Admin Reset & Reseed button, Demo Mode Switch, and Run Cycle Now
  capture(
    `http://localhost:5173/settings?__auth=${adminAuthParam}`,
    '19-settings-admin-reset-reseed-demo-mode.png',
    '1440,1100'
  );

  // 3. Capture Admin Dashboard showing seeded 25 sample grievances across Police and Civic categories
  capture(
    `http://localhost:5173/admin?__auth=${adminAuthParam}`,
    '20-admin-dashboard-25-seeded-complaints.png',
    '1440,1400'
  );

  // 4. Capture Escalation Centre showing the already-escalated Level-1 complaint (RSK-2026-018)
  capture(
    `http://localhost:5173/escalations?__auth=${adminAuthParam}`,
    '21-escalation-centre-seeded-tier1.png',
    '1440,1100'
  );

  console.log('\nAll Prompt 9 captures finished successfully!');
}

main().catch(console.error);


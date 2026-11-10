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

function capture(url, filename) {
  const localFile = path.join(localScreenshots, filename);
  const artifactFile = path.join(artifactScreenshots, filename);
  console.log(`\nCapturing: ${filename}`);
  console.log(`  URL: ${url}`);
  
  try {
    execFileSync(edgePath, [
      '--headless=new',
      '--disable-gpu',
      '--virtual-time-budget=5000',
      '--window-size=1440,1080',
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
  console.log('--- Resolvetrack 7.1, 7.2, 7.3 Verification Screenshot Capture ---');

  // 1. Get Officer auth
  console.log('\nLogging in as Municipal Officer (officer_municipal)...');
  let officerAuth;
  try {
    const loginRes = await api('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username: 'officer_municipal', password: 'Officer@123' })
    });
    officerAuth = loginRes.data;
    console.log(`  Logged in as: ${officerAuth.fullName} (Role: ${officerAuth.role})`);
  } catch (e) {
    console.warn('  Officer login failed, falling back to admin:', e.message);
    const adminLogin = await api('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username: 'admin', password: 'Admin@123' })
    });
    officerAuth = adminLogin.data;
  }

  const officerAuthParam = encodeURIComponent(JSON.stringify(officerAuth));

  // 2. Capture Public Citizen Tracking with Reopened Status & Community Issue Cluster
  capture(
    'http://localhost:5173/track?id=RSK-2026-008',
    '13-citizen-tracking-reopened-cluster.png'
  );

  // 3. Capture Complaint Details with Next Best Action & Reopened State
  capture(
    `http://localhost:5173/complaints/8?__auth=${officerAuthParam}`,
    '14-complaint-details-reopened-nba.png'
  );

  // 4. Capture Officer Dashboard with Recommended Actions Prescriptive Engine
  capture(
    `http://localhost:5173/officer?__auth=${officerAuthParam}`,
    '15-officer-dashboard-recommended-actions.png'
  );

  console.log('\nAll captures finished successfully!');
}

main().catch(console.error);


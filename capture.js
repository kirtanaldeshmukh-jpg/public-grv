const { execFileSync } = require('child_process');
const path = require('path');
const fs = require('fs');

const edgePath = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';
const outDir = path.join(__dirname, 'screenshots');

const targets = [
  { url: 'http://localhost:5173/track', file: '13-citizen-tracking-reopened-cluster.png' },
  { url: 'http://localhost:5173/complaints/8', file: '14-complaint-details-reopened-nba.png' },
  { url: 'http://localhost:5173/officer', file: '15-officer-dashboard-recommended-actions.png' }
];

for (const t of targets) {
  const filePath = path.join(outDir, t.file);
  console.log(`Capturing ${t.url} to ${t.file}...`);
  try {
    execFileSync(edgePath, [
      '--headless=new',
      '--disable-gpu',
      '--virtual-time-budget=4000',
      '--window-size=1400,1050',
      `--screenshot=${filePath}`,
      t.url
    ], { stdio: 'inherit' });
    if (fs.existsSync(filePath)) {
      console.log(`Saved: ${t.file} (${fs.statSync(filePath).size} bytes)`);
    } else {
      console.log(`File not generated for ${t.file}`);
    }
  } catch (e) {
    console.error(`Failed ${t.file}:`, e.message);
  }
}


const { execFile } = require('child_process');
const path = require('path');
const fs = require('fs');

const edgePath = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';
const outDir = path.join(__dirname, 'screenshots');
if (!fs.existsSync(outDir)) fs.mkdirSync(outDir, { recursive: true });

function capture(url, filename) {
  return new Promise((resolve, reject) => {
    const targetFile = path.join(outDir, filename);
    console.log(`Capturing ${url} -> ${filename}...`);
    execFile(edgePath, [
      '--headless',
      '--disable-gpu',
      `--screenshot=${targetFile}`,
      '--window-size=1400,1050',
      url
    ], (error) => {
      if (error) {
        console.error(`Error capturing ${filename}:`, error);
        reject(error);
      } else {
        console.log(`Captured ${filename} (${fs.statSync(targetFile).size} bytes)`);
        resolve(targetFile);
      }
    });
  });
}

async function main() {
  await capture('http://localhost:5173/track?id=RSK-2026-008', '13-citizen-tracking-reopened-cluster.png');
  await capture('http://localhost:5173/complaints/8', '14-complaint-details-reopened-nba.png');
  await capture('http://localhost:5173/officer', '15-officer-dashboard-recommended-actions.png');
  console.log('All screenshots captured successfully.');
}

main().catch(console.error);


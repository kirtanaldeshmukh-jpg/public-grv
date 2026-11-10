const { execFile } = require('child_process');
const path = require('path');
const fs = require('fs');

const edgePath = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';
const outDir = path.join(__dirname, 'screenshots');
if (!fs.existsSync(outDir)) fs.mkdirSync(outDir, { recursive: true });

const targetFile = path.join(outDir, '01-login-screen.png');

console.log('Capturing screenshot of http://localhost:5173/login ...');
execFile(edgePath, [
  '--headless',
  '--disable-gpu',
  `--screenshot=${targetFile}`,
  '--window-size=1400,900',
  'http://localhost:5173/login'
], (error, stdout, stderr) => {
  if (error) {
    console.error('Edge error:', error);
  } else {
    console.log('Edge completed.');
    if (fs.existsSync(targetFile)) {
      console.log('Screenshot saved successfully:', targetFile, fs.statSync(targetFile).size, 'bytes');
    } else {
      console.log('File does not exist yet at:', targetFile);
    }
  }
});


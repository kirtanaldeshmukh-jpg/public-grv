const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');

const root = __dirname;
const classesDir = path.join(root, 'backend', 'target', 'classes');
const extractedCp = fs.readFileSync(path.join(root, 'backend', 'extracted_cp.txt'), 'utf8').trim();
const fullCp = `${classesDir};${extractedCp}`;

const javaBin = 'C:\\Program Files\\Java\\jdk-25.0.4.1\\bin\\java.exe';
const env = {
  ...process.env,
  JAVA_HOME: 'C:\\Program Files\\Java\\jdk-25.0.4.1',
  CLASSPATH: fullCp,
  SPRING_PROFILES_ACTIVE: 'dev'
};

console.log('Starting Spring Boot backend on http://localhost:8080 ...');
const child = spawn(javaBin, ['-Dspring.profiles.active=dev', 'com.resolvetrack.ResolveTrackApplication'], {
  cwd: path.join(root, 'backend'),
  env,
  stdio: 'inherit'
});

child.on('error', (err) => {
  console.error('Failed to start backend process:', err);
});

child.on('exit', (code) => {
  console.log('Backend process exited with code:', code);
});


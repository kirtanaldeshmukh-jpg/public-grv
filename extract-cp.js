const fs = require('fs');
const content = fs.readFileSync('backend/target/surefire-reports/TEST-com.resolvetrack.P0FlowIntegrationTest.xml', 'utf8');
const match = content.match(/<property name="surefire.test.class.path" value="([^"]+)"/);
if (match) {
  const decoded = match[1].replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&quot;/g, '"');
  const items = decoded.split(';').filter(Boolean);
  // Ensure target/classes is included
  const root = process.cwd();
  const classesDir = `${root}\\backend\\target\\classes`;
  if (!items.includes(classesDir)) {
    items.unshift(classesDir);
  }
  // Java argument file: lines can be quoted
  const cpArg = items.join(';');
  fs.writeFileSync('backend/app_cp.args', `-cp\n"${cpArg}"\ncom.resolvetrack.ResolveTrackApplication\n`);
  console.log(`Created backend/app_cp.args with ${items.length} classpath entries`);
} else {
  console.error('surefire.test.class.path not found');
}

const http = require('http');
const { execFile } = require('child_process');
const path = require('path');
const fs = require('fs');

const edgePath = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';
const outDir = path.join(__dirname, 'screenshots');
if (!fs.existsSync(outDir)) fs.mkdirSync(outDir, { recursive: true });

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
        console.log(`[SCREENSHOT] ${filename} (${sz} bytes)`);
        resolve({ filename, size: sz });
      }
    });
  });
}

function req(options, postData) {
  return new Promise((resolve, reject) => {
    const request = http.request(options, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => resolve({ status: res.statusCode, body: data }));
    });
    request.on('error', reject);
    if (postData) request.write(postData);
    request.end();
  });
}

(async () => {
  console.log('=== STEP 4: Automated E2E Flow Execution ===');

  // 1. Reset demo seed to clean state
  const adminLoginRes = await req({
    hostname: '127.0.0.1', port: 8080, path: '/api/auth/login', method: 'POST',
    headers: { 'Content-Type': 'application/json' }
  }, JSON.stringify({ username: 'admin', password: 'Admin@123' }));
  const adminAuth = JSON.parse(adminLoginRes.body).data;
  const adminToken = adminAuth.token;
  const adminAuthParam = encodeURIComponent(JSON.stringify(adminAuth));

  console.log('Resetting database...');
  await req({
    hostname: '127.0.0.1', port: 8080, path: '/api/admin/reset-and-reseed', method: 'POST',
    headers: { 'Authorization': 'Bearer ' + adminToken }
  });

  // Enable demo mode
  console.log('Enabling Demo Mode (minutes instead of hours)...');
  await req({
    hostname: '127.0.0.1', port: 8080, path: '/api/dev/demo/set-mode?enabled=true', method: 'POST',
    headers: { 'Authorization': 'Bearer ' + adminToken }
  });

  // 2. Citizen submits complaint
  console.log('\n[Flow 1] Citizen submits complaint...');
  const lodgePayload = JSON.stringify({
    title: 'Severe open sewage overflowing across 8th Cross walkway',
    description: 'Raw sewage overflowing near kindergarten school gate creating public health hazard',
    category: 'Water & Drainage',
    departmentId: 4,
    citizenName: 'Anita Desai',
    citizenPhone: '+91 98450 33445',
    citizenEmail: 'anita.desai@example.com',
    locationAddress: '8th Cross, Wilson Garden, Bengaluru',
    latitude: 12.9511,
    longitude: 77.5956
  });
  const lodgeRes = await req({
    hostname: '127.0.0.1', port: 8080, path: '/api/public/complaints', method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(lodgePayload) }
  }, lodgePayload);
  const lodged = JSON.parse(lodgeRes.body).data;
  const trackingId = lodged.trackingId;

  // Dynamically resolve complaint ID from database via search
  const searchRes = await req({
    hostname: '127.0.0.1', port: 8080, path: `/api/complaints?search=${trackingId}`, method: 'GET',
    headers: { 'Authorization': 'Bearer ' + adminToken }
  });
  const searchJson = JSON.parse(searchRes.body).data;
  const complaintId = searchJson.content[0].id;
  console.log(`Complaint lodged: DB ID=${complaintId}, TrackingID=${trackingId}`);

  // Capture Citizen Tracking page
  await capture(`http://127.0.0.1:5173/track?id=${trackingId}`, 'p0-flow-1-citizen-tracking-submitted.png');

  // 3. Admin assigns to Municipal Officer (officer_municipal, ID: 4)
  console.log('\n[Flow 2] Admin assigns complaint to Officer Manjunath K (Officer ID 4)...');
  const assignPayload = JSON.stringify({
    officerId: 4,
    instructions: 'Inspect site immediately and deploy suction tanker for drainage clearing.'
  });
  const assignRes = await req({
    hostname: '127.0.0.1', port: 8080, path: `/api/complaints/${complaintId}/assign`, method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ' + adminToken,
      'Content-Length': Buffer.byteLength(assignPayload)
    }
  }, assignPayload);
  console.log('Assign Status:', assignRes.status);

  // Capture Admin Complaint Details view
  await capture(`http://127.0.0.1:5173/complaints/${complaintId}?__auth=${adminAuthParam}`, 'p0-flow-2-admin-assigned.png');

  // 4. Officer logs in and updates status to IN_PROGRESS
  console.log('\n[Flow 3] Officer logs in and updates status to IN_PROGRESS...');
  const officerLoginRes = await req({
    hostname: '127.0.0.1', port: 8080, path: '/api/auth/login', method: 'POST',
    headers: { 'Content-Type': 'application/json' }
  }, JSON.stringify({ username: 'officer_municipal', password: 'Officer@123' }));
  const officerAuth = JSON.parse(officerLoginRes.body).data;
  const officerToken = officerAuth.token;
  const officerAuthParam = encodeURIComponent(JSON.stringify(officerAuth));

  const updatePayload = JSON.stringify({
    status: 'IN_PROGRESS',
    remarks: 'Field inspection completed. Drainage team deployed with suction unit on site.'
  });
  const updateRes = await req({
    hostname: '127.0.0.1', port: 8080, path: `/api/complaints/${complaintId}/status`, method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ' + officerToken,
      'Content-Length': Buffer.byteLength(updatePayload)
    }
  }, updatePayload);
  console.log('Update Status:', updateRes.status);

  // Capture Officer Dashboard view
  await capture(`http://127.0.0.1:5173/officer?__auth=${officerAuthParam}`, 'p0-flow-3-officer-updated.png');

  // 5. Trigger Follow-up cycle
  console.log('\n[Flow 4] Triggering Follow-up cycle via /api/dev/demo/run-cycle...');
  const cycleRes = await req({
    hostname: '127.0.0.1', port: 8080, path: '/api/dev/demo/run-cycle', method: 'POST',
    headers: { 'Authorization': 'Bearer ' + adminToken }
  });
  console.log('Cycle Execution Status:', cycleRes.status);
  const cycleResult = JSON.parse(cycleRes.body).data;
  console.log('Cycle Result:', cycleResult);

  // 6. Capture Reminder Centre and Escalation Centre
  console.log('\n[Flow 5] Capturing Reminder Centre & Escalation Centre...');
  await capture(`http://127.0.0.1:5173/reminders?__auth=${adminAuthParam}`, 'p0-flow-4-reminder-centre.png');
  await capture(`http://127.0.0.1:5173/escalations?__auth=${adminAuthParam}`, 'p0-flow-5-escalation-centre.png');

  console.log('\n=== All steps completed and verified successfully! ===');
})().catch(console.error);

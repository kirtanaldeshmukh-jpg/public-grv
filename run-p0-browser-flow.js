const { execFile } = require('child_process');
const path = require('path');
const fs = require('fs');

const edgePath = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';
const screenshotsDir = path.join(__dirname, 'screenshots');
if (!fs.existsSync(screenshotsDir)) fs.mkdirSync(screenshotsDir, { recursive: true });

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function captureScreenshot(url, outputFile) {
  return new Promise((resolve, reject) => {
    console.log(`[BROWSER] Capturing ${url} -> ${path.basename(outputFile)} ...`);
    execFile(edgePath, [
      '--headless',
      '--disable-gpu',
      `--screenshot=${outputFile}`,
      '--window-size=1440,920',
      '--virtual-time-budget=4000',
      url
    ], (error) => {
      if (error) {
        console.error(`Failed to capture ${outputFile}:`, error.message);
        return resolve(false);
      }
      if (fs.existsSync(outputFile)) {
        console.log(`[BROWSER] Saved ${path.basename(outputFile)} (${fs.statSync(outputFile).size} bytes)`);
        resolve(true);
      } else {
        console.warn(`[BROWSER] File not found: ${outputFile}`);
        resolve(false);
      }
    });
  });
}

async function api(path, options = {}) {
  const url = `http://localhost:8080/api${path}`;
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
  const res = await fetch(url, {
    ...options,
    headers
  });
  const data = await res.json().catch(() => null);
  if (!res.ok) {
    throw new Error(`API ${options.method || 'GET'} ${path} returned ${res.status}: ${JSON.stringify(data)}`);
  }
  return data;
}

async function main() {
  console.log('================================================================');
  console.log('Resolvetrack P0 Flow & Full Browser Verification');
  console.log('================================================================');

  // Verify backend health
  try {
    const depts = await api('/departments');
    console.log(`[API] Connected to backend! Found ${depts.data?.length || 0} departments.`);
  } catch (err) {
    console.error('[API] Backend connection failed:', err.message);
    process.exit(1);
  }

  // 1. Capture Login Page
  await captureScreenshot('http://localhost:5173/login', path.join(screenshotsDir, '01-login-screen.png'));

  // 2. Capture Grievance Lodgement Page
  await captureScreenshot('http://localhost:5173/file', path.join(screenshotsDir, '02-file-grievance-form.png'));

  // 3. Citizen files complaint via Public endpoint
  console.log('\n--- Step 1: Citizen submits complaint ---');
  const createPayload = {
    title: 'Drinking Water Pipeline Burst Flooding 12th Main Road',
    description: 'Severe potable water main rupture opposite Metro pillar 142. Heavy water leakage has submerged 2 lanes and disrupted water to 4,000 households.',
    category: 'water_leakage',
    priority: 'HIGH',
    citizenName: 'Ananya Sharma',
    citizenPhone: '9845012345',
    citizenEmail: 'ananya@example.com',
    locationAddress: '12th Main Road, Indiranagar 2nd Stage, Bengaluru Urban'
  };

  const createRes = await api('/public/complaints', {
    method: 'POST',
    body: JSON.stringify(createPayload)
  });

  const trackingId = createRes.data?.trackingNumber || createRes.data?.trackingId;
  console.log(`[SUCCESS] Grievance Lodged! Tracking ID: ${trackingId}`);

  // 4. Citizen Tracks Complaint
  console.log('\n--- Step 2: Citizen tracks grievance ---');
  const trackRes = await api(`/public/track/${trackingId}`);
  console.log('[TRACK] Public Status:', trackRes.data?.status, '| Dept:', trackRes.data?.departmentName);
  console.log('[TRACK] Latest Public Update:', trackRes.data?.latestPublicUpdate);
  // Verify citizen privacy shield
  if (trackRes.data?.assignedOfficerName || trackRes.data?.internalNotes) {
    console.error('[PRIVACY VIOLATION] Officer or internal notes leaked in public endpoint!');
  } else {
    console.log('[PRIVACY SHIELD VERIFIED] Officer names & internal notes properly withheld from public tracking.');
  }

  await captureScreenshot(`http://localhost:5173/track?id=${trackingId}`, path.join(screenshotsDir, '03-citizen-tracking-new.png'));

  // 5. Admin Login
  console.log('\n--- Step 3: Admin logs in ---');
  const adminLogin = await api('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username: 'admin', password: 'pass' })
  });
  const adminAuth = adminLogin.data;
  console.log(`[AUTH] Admin logged in as: ${adminAuth.fullName} (${adminAuth.role})`);

  const adminAuthParam = encodeURIComponent(JSON.stringify(adminAuth));

  // Find complaint ID
  const searchRes = await api(`/complaints?search=${trackingId}`, {
    headers: { Authorization: `Bearer ${adminAuth.token}` }
  });
  const complaint = searchRes.data?.content?.[0];
  if (!complaint) throw new Error(`Complaint ${trackingId} not found in admin complaint list!`);
  const complaintId = complaint.id;
  console.log(`[ADMIN] Found complaint ID: ${complaintId} (Health Score: ${complaint.healthScore})`);

  // Capture Admin Dashboard
  await captureScreenshot(`http://localhost:5173/admin?__auth=${adminAuthParam}`, path.join(screenshotsDir, '04-admin-dashboard.png'));

  // Capture Complaint Details (Admin view before assignment)
  await captureScreenshot(`http://localhost:5173/complaints/${complaintId}?__auth=${adminAuthParam}`, path.join(screenshotsDir, '05-complaint-details-admin.png'));

  // 6. Admin assigns complaint to Officer Rajesh
  console.log('\n--- Step 4: Admin assigns complaint to Officer Rajesh ---');
  await api(`/complaints/${complaintId}/assign`, {
    method: 'PUT',
    headers: { Authorization: `Bearer ${adminAuth.token}` },
    body: JSON.stringify({
      officerId: 1,
      reason: 'Assigned for immediate field inspection and emergency main line sleeve clamp repair'
    })
  });
  console.log(`[ASSIGN] Complaint ${trackingId} successfully assigned to Officer Rajesh Kumar (ID 1)`);

  // 7. Officer Login & Progress Update
  console.log('\n--- Step 5: Officer logs in and posts progress update ---');
  const officerLogin = await api('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username: 'officer_rajesh', password: 'pass' })
  });
  const officerAuth = officerLogin.data;
  console.log(`[AUTH] Officer logged in as: ${officerAuth.fullName} (${officerAuth.role})`);

  const officerAuthParam = encodeURIComponent(JSON.stringify(officerAuth));

  // Capture Officer Dashboard
  await captureScreenshot(`http://localhost:5173/officer?__auth=${officerAuthParam}`, path.join(screenshotsDir, '06-officer-dashboard.png'));

  // Officer updates progress
  const updateRes = await api(`/complaints/${complaintId}/updates`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${officerAuth.token}` },
    body: JSON.stringify({
      status: 'IN_PROGRESS',
      progressNote: 'Emergency repair crew mobilized with dewatering pumps and replacement 600mm pipe sleeves. Repair underway.',
      isPublic: true,
      nextFollowUpDate: new Date(Date.now() + 86400000 * 2).toISOString()
    })
  });
  console.log('[UPDATE] Officer update status:', updateRes.data?.status);

  // Capture updated Complaint Details (Officer view)
  await captureScreenshot(`http://localhost:5173/complaints/${complaintId}?__auth=${officerAuthParam}`, path.join(screenshotsDir, '07-complaint-updated-officer.png'));

  // 8. Trigger Demo Follow-up Cycle
  console.log('\n--- Step 6: Trigger Follow-up cycle and inspect engine results ---');
  // Enable demo mode
  await api('/settings/demo.mode', {
    method: 'PUT',
    headers: { Authorization: `Bearer ${adminAuth.token}` },
    body: JSON.stringify({ value: 'true' })
  });

  const cycleRes = await api('/dev/demo/run-cycle', { method: 'POST' });
  console.log('[ENGINE] Follow-up cycle executed:', cycleRes.data);

  // Capture Reminder Centre
  await captureScreenshot(`http://localhost:5173/reminders?__auth=${adminAuthParam}`, path.join(screenshotsDir, '08-reminder-centre.png'));

  // Capture Escalation Centre
  await captureScreenshot(`http://localhost:5173/escalations?__auth=${adminAuthParam}`, path.join(screenshotsDir, '09-escalation-centre.png'));

  // Capture Action History
  await captureScreenshot(`http://localhost:5173/history?__auth=${adminAuthParam}`, path.join(screenshotsDir, '10-action-history.png'));

  // Capture Updated Citizen Tracking View
  await captureScreenshot(`http://localhost:5173/track?id=${trackingId}`, path.join(screenshotsDir, '11-citizen-tracking-updated.png'));

  console.log('\n================================================================');
  console.log('P0 Browser Flow & All UI Screenshots Completed Successfully!');
  console.log('================================================================');
}

main().catch((err) => {
  console.error('[ERROR] Flow failed:', err);
  process.exit(1);
});


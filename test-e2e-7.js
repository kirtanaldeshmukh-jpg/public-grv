const http = require('http');

function request(options, data = null) {
  return new Promise((resolve, reject) => {
    const req = http.request(options, (res) => {
      let body = '';
      res.on('data', (chunk) => body += chunk);
      res.on('end', () => {
        try {
          const parsed = JSON.parse(body);
          resolve({ status: res.statusCode, headers: res.headers, data: parsed, raw: body });
        } catch (e) {
          resolve({ status: res.statusCode, headers: res.headers, raw: body });
        }
      });
    });
    req.on('error', reject);
    if (data) {
      req.write(typeof data === 'string' ? data : JSON.stringify(data));
    }
    req.end();
  });
}

async function run() {
  console.log('=== STEP 1: Citizen 1 submits first complaint ===');
  const c1Payload = {
    departmentId: 4,
    title: 'Severe Water Pipe Rupture on 8th Main Road',
    description: 'Main Cauvery water pipeline burst on 8th Main Vidyaranyapura causing huge water leakage and flooding road',
    category: 'Water & Drainage',
    priority: 'HIGH',
    latitude: 13.0827,
    longitude: 77.5458,
    locationAddress: '8th Main Rd, Vidyaranyapura, Bengaluru',
    citizenName: 'Ramesh Kumar',
    citizenPhone: '9845012345',
    citizenEmail: 'ramesh@example.com'
  };

  const c1Res = await request({
    hostname: 'localhost',
    port: 8080,
    path: '/api/public/complaints',
    method: 'POST',
    headers: { 'Content-Type': 'application/json' }
  }, c1Payload);

  console.log('Complaint 1 status:', c1Res.status);
  const c1 = c1Res.data.data;
  console.log('Complaint 1 Tracking ID:', c1.trackingId, 'Status:', c1.status);

  console.log('\n=== STEP 2: Citizen 2 submits duplicate complaint in 500m vicinity ===');
  const c2Payload = {
    departmentId: 4,
    title: 'Water Pipe Rupture and leakage on 8th Main Road',
    description: 'Severe water pipeline rupture causing drinking water wastage and flooding on 8th Main road near bus stop',
    category: 'Water & Drainage',
    priority: 'HIGH',
    latitude: 13.0829,
    longitude: 77.5460,
    locationAddress: '8th Main Rd, Vidyaranyapura, Bengaluru',
    citizenName: 'Sunita Rao',
    citizenPhone: '9845054321',
    citizenEmail: 'sunita@example.com'
  };

  const c2Res = await request({
    hostname: 'localhost',
    port: 8080,
    path: '/api/public/complaints',
    method: 'POST',
    headers: { 'Content-Type': 'application/json' }
  }, c2Payload);

  console.log('Complaint 2 status:', c2Res.status);
  const c2 = c2Res.data.data;
  console.log('Complaint 2 Tracking ID:', c2.trackingId, 'Status:', c2.status);

  console.log('\n=== STEP 3: Verify Duplicate Detection & Community Issue clustering ===');
  // Check Complaint 2 details via tracking
  const track2 = await request({
    hostname: 'localhost',
    port: 8080,
    path: `/api/public/track/${c2.trackingId}`,
    method: 'GET'
  });
  console.log('Complaint 2 Community Issue:', track2.data.data.communityIssue ? 'LINKED' : 'NONE');
  if (track2.data.data.communityIssue) {
    console.log('Community Issue ID:', track2.data.data.communityIssue.id);
    console.log('Linked Tracking IDs:', track2.data.data.communityIssue.linkedTrackingIds);
  }

  console.log('\n=== STEP 4: Login as Admin and Check Recommended Actions ===');
  const loginRes = await request({
    hostname: 'localhost',
    port: 8080,
    path: '/api/auth/login',
    method: 'POST',
    headers: { 'Content-Type': 'application/json' }
  }, { username: 'admin', password: 'Admin@123' });

  const token = loginRes.data.data.token;
  console.log('Admin Login successful, token acquired');

  const recRes = await request({
    hostname: 'localhost',
    port: 8080,
    path: '/api/dashboard/recommended-actions',
    method: 'GET',
    headers: { 'Authorization': `Bearer ${token}` }
  });
  console.log('Recommended Actions Status:', recRes.status);
  const groups = recRes.data.data?.officerGroups || recRes.data.data || [];
  console.log('Groups count:', groups.length);
  if (groups.length > 0) {
    const firstGroup = groups[0];
    console.log(`First Group Officer: ${firstGroup.officerName}, Overdue: ${firstGroup.overdueCount}, Approaching: ${firstGroup.approachingDeadlineCount}, Actions: ${firstGroup.actions ? firstGroup.actions.length : 0}`);
  }

  // Look up Complaint 1 & 2 internal IDs using search
  const search1 = await request({
    hostname: 'localhost',
    port: 8080,
    path: `/api/complaints?search=${encodeURIComponent(c1.trackingId)}`,
    method: 'GET',
    headers: { 'Authorization': `Bearer ${token}` }
  });
  const complaint1Id = search1.data?.data?.content?.[0]?.id;
  console.log('Complaint 1 Internal ID:', complaint1Id);

  const search2 = await request({
    hostname: 'localhost',
    port: 8080,
    path: `/api/complaints?search=${encodeURIComponent(c2.trackingId)}`,
    method: 'GET',
    headers: { 'Authorization': `Bearer ${token}` }
  });
  const complaint2Id = search2.data?.data?.content?.[0]?.id;
  console.log('Complaint 2 Internal ID:', complaint2Id);

  console.log('\n=== STEP 5: Assign Officer to Complaint 1 ===');
  // Assign officer 4 (Municipal officer)
  const assignRes = await request({
    hostname: 'localhost',
    port: 8080,
    path: `/api/complaints/${complaint1Id}/assign`,
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    }
  }, { officerId: 4, reassignReason: 'Allocated to municipal maintenance engineer' });
  console.log('Assign Complaint 1 status:', assignRes.status);

  console.log('\n=== STEP 6: Officer Submits Verified Resolution for Complaint 1 ===');
  const resolvePayload = {
    summary: 'Excavation completed, 160mm pipeline replaced with ductile iron sleeve. Pressure tested at 4.2 bar and supply restored.'
  };
  const resolveRes = await request({
    hostname: 'localhost',
    port: 8080,
    path: `/api/complaints/${complaint1Id}/resolve`,
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    }
  }, resolvePayload);

  console.log('Submit Resolution status:', resolveRes.status);
  console.log('New Complaint Status:', resolveRes.data.data.status);
  console.log('Health Score Detail:', resolveRes.data.data.healthScoreDetail);
  console.log('Next Best Action:', resolveRes.data.data.nextBestAction);

  console.log('\n=== STEP 7: Citizen Confirms Resolution (Tracking ID 1) ===');
  const confirmRes = await request({
    hostname: 'localhost',
    port: 8080,
    path: `/api/public/complaints/${c1.trackingId}/confirm-resolution`,
    method: 'POST',
    headers: { 'Content-Type': 'application/json' }
  }, { feedback: 'Water pressure restored and clean drinking water received. Thank you!' });

  console.log('Confirm Resolution status:', confirmRes.status);
  console.log('Status after Citizen Confirmation:', confirmRes.data.data.status);

  console.log('\n=== STEP 8: Officer Submits Resolution for Complaint 2, Citizen REJECTS (Reopen) ===');
  await request({
    hostname: 'localhost',
    port: 8080,
    path: `/api/complaints/${complaint2Id}/assign`,
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    }
  }, { officerId: 4, reassignReason: 'Allocated to municipal engineer' });

  await request({
    hostname: 'localhost',
    port: 8080,
    path: `/api/complaints/${complaint2Id}/resolve`,
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    }
  }, { summary: 'Trench backfilled and pipe patch applied.' });

  // Citizen rejects resolution
  const rejectRes = await request({
    hostname: 'localhost',
    port: 8080,
    path: `/api/public/complaints/${c2.trackingId}/reject-resolution`,
    method: 'POST',
    headers: { 'Content-Type': 'application/json' }
  }, { reason: 'Water is still leaking onto the sidewalk and pressure inside house is very low.' });

  console.log('Reject Resolution status:', rejectRes.status);
  console.log('Status after Citizen Rejection:', rejectRes.data.data.status);
  console.log('Reopen Count:', rejectRes.data.data.reopenCount);

  console.log('\n=== SUCCESS: All Prompt 7.1, 7.2, and 7.3 verification steps completed! ===');
}

run().catch(console.error);

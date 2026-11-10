# ResolveTrack Grievance Portal - REST API Specification

Base URL: `http://localhost:8080` (or `http://localhost:8080/api/v1`)
CORS Allowed Origins: `http://localhost:5173`, `http://127.0.0.1:5173`

---

## 1. Authentication & Security (`/api/auth`)

### 1.1 Login
- **Endpoint**: `POST /api/auth/login` (also `/api/v1/auth/login`)
- **Access**: Public
- **Request Body**:
  ```json
  {
    "username": "admin",
    "password": "pass"
  }
  ```
- **Response** (`200 OK`):
  ```json
  {
    "success": true,
    "message": "Login successful",
    "data": {
      "token": "eyJhbGciOiJIUzI1NiJ9...",
      "username": "admin",
      "role": "ADMIN",
      "fullName": "System Administrator",
      "departmentId": null,
      "officerId": null
    }
  }
  ```

### 1.2 Register
- **Endpoint**: `POST /api/auth/register` (also `/api/v1/auth/register`)
- **Access**: Public
- **Request Body**:
  ```json
  {
    "username": "citizen_user",
    "password": "Password123!",
    "fullName": "Ramesh Kumar",
    "email": "ramesh@example.com",
    "phone": "9876543210",
    "role": "CITIZEN"
  }
  ```

---

## 2. Public Citizen Services (`/api/public`)

### 2.1 File Grievance (Multipart with Files)
- **Endpoint**: `POST /api/public/complaints` (also `/api/v1/public/complaints`)
- **Access**: Public
- **Content-Type**: `multipart/form-data`
- **Form Data Fields**:
  - `departmentId` (Long, optional)
  - `title` (String, required)
  - `description` (String, required)
  - `category` (String, required)
  - `priority` (Enum: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`, optional)
  - `citizenName` (String, optional)
  - `citizenPhone` (String, optional)
  - `citizenEmail` (String, optional)
  - `locationAddress` (String, optional)
  - `latitude` (BigDecimal, optional)
  - `longitude` (BigDecimal, optional)
  - `files` (Array of MultipartFile, max 5 MB each, image/jpeg, image/png, application/pdf)
- **Response** (`201 Created`):
  ```json
  {
    "success": true,
    "message": "Grievance lodged successfully",
    "data": {
      "trackingNumber": "RSK-2026-001",
      "status": "NEW",
      "title": "Underground Pipeline Burst",
      "category": "water_leakage",
      "departmentNameEn": "Water Supply & Sewerage Board",
      "departmentNameKn": "ಜಲಮಂಡಳಿ",
      "followUpDeadline": "2026-11-10T13:30:00",
      "createdAt": "2026-11-08T13:30:00"
    }
  }
  ```

### 2.2 Public Tracking Dossier
- **Endpoint**: `GET /api/public/track/{id}` (also `GET /api/public/complaints/track/{id}`)
- **Access**: Public
- **Privacy Rule**: Citizens never see officer names, internal notes, or other citizens' data.
- **Response** (`200 OK`):
  ```json
  {
    "success": true,
    "message": "Public tracking information retrieved",
    "data": {
      "trackingId": "RSK-2026-001",
      "status": "ASSIGNED",
      "departmentName": "Water Supply & Sewerage Board",
      "latestPublicUpdate": "Complaint registered under Karnataka Sakala Services Act.",
      "isResolved": false,
      "followUpDeadline": "2026-11-10T13:30:00",
      "createdAt": "2026-11-08T13:30:00"
    }
  }
  ```

### 2.3 Active Public Departments
- **Endpoint**: `GET /api/public/departments`
- **Access**: Public
- **Response** (`200 OK`): Array of active departments with Kannada and English names.

---

## 3. Complaint Management (`/api/complaints`)

### 3.1 List / Search Complaints (Role Scoped)
- **Endpoint**: `GET /api/complaints`
- **Access**: Authenticated (`ADMIN`, `SUPERVISOR`, `OFFICER`, `DATA_ENTRY`)
- **Query Parameters**:
  - `departmentId` (Long, optional)
  - `status` (Enum string, e.g. `NEW`, `ASSIGNED`, `IN_PROGRESS`, `PENDING_EXTERNAL`, `RESOLVED`, `ESCALATED_L1`, `ESCALATED_L2`)
  - `priority` (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`)
  - `search` / `q` (Free text search)
  - `startDate` / `endDate` (`YYYY-MM-DD` or ISO string)
  - `page` (default `0`)
  - `size` (default `10`)
  - `sortBy` (default `healthScore`, also `createdAt`, `followUpDeadline`)
  - `sortDir` (`asc` / `desc`)
- **Response** (`200 OK`):
  ```json
  {
    "success": true,
    "data": {
      "content": [
        {
          "id": 1,
          "trackingNumber": "RSK-2026-001",
          "title": "Water Main Burst",
          "status": "ASSIGNED",
          "priority": "HIGH",
          "category": "water_leakage",
          "healthScore": 82,
          "departmentName": "BWSSB",
          "assignedOfficerName": "Rajesh Kumar",
          "followUpDeadline": "2026-11-10T14:00:00",
          "createdAt": "2026-11-08T12:00:00"
        }
      ],
      "page": 0,
      "size": 10,
      "totalElements": 24,
      "totalPages": 3,
      "first": true,
      "last": false
    }
  }
  ```

### 3.2 Complaint Detail
- **Endpoint**: `GET /api/complaints/{id}`
- **Access**: Authenticated (Admin sees all, Supervisor sees department, Officer sees assigned)
- **Response** (`200 OK`): Detailed grievance object including citizen info, GPS coords, attachments, AI suggestion, and SLA info.

### 3.3 Assign Complaint
- **Endpoint**: `PUT /api/complaints/{id}/assign` (also `POST /api/complaints/{id}/assign`)
- **Access**: `ADMIN`, `SUPERVISOR`
- **Request Body**:
  ```json
  {
    "officerId": 1,
    "reassignReason": "Assigned to Ward Junior Engineer for immediate pipe excavation"
  }
  ```

### 3.4 Officer Progress Update
- **Endpoint**: `POST /api/complaints/{id}/updates`
- **Access**: Assigned `OFFICER`, department `SUPERVISOR`, `ADMIN`
- **Request Body**:
  ```json
  {
    "status": "IN_PROGRESS",
    "progressNote": "Excavation crew deployed at site; replacement sleeve fitting underway.",
    "isPublic": true,
    "nextFollowUpDate": "2026-11-09T18:00:00"
  }
  ```
- **Side Effects**: Automatically resets follow-up deadline, acknowledges pending reminders, clears escalation flags when appropriate, updates health score, and writes to `action_history`.

### 3.5 AI Department Suggestion
- **Endpoint**: `GET /api/complaints/suggest-department?category=...&text=...`
- **Access**: Public / Authenticated
- **Response**: Suggested department, confidence score (0.0 to 1.0), and matched keywords.

---

## 4. Dashboard & Analytics

### 4.1 Dashboard Summary Counts
- **Endpoint**: `GET /api/dashboard/summary`
- **Access**: Authenticated (`ADMIN`, `SUPERVISOR`, `OFFICER`, `DATA_ENTRY`)
- **Response** (`200 OK`):
  ```json
  {
    "success": true,
    "data": {
      "total": 35,
      "newCount": 6,
      "assignedCount": 11,
      "inProgressCount": 9,
      "pendingCount": 3,
      "overdueCount": 2,
      "resolvedCount": 15
    }
  }
  ```

### 4.2 Executive Analytics Overview
- **Endpoint**: `GET /api/analytics/overview`
- **Access**: `OFFICER`, `SUPERVISOR`, `ADMIN`
- **Response**: System SLA compliance %, average resolution hours, weekly inflow stats, and department rankings.

---

## 5. Reminders & Dispatcher (`/api/reminders`)

### 5.1 List All Reminders
- **Endpoint**: `GET /api/reminders`
- **Access**: `OFFICER`, `SUPERVISOR`, `ADMIN`
- **Response**: Array of dispatch records with `trackingNumber`, `reminderType`, `nudgeCount`, `channel`, `sentAt`, `acknowledged`.

### 5.2 Acknowledge Reminder
- **Endpoint**: `PUT /api/reminders/{id}/acknowledge` (also `POST`)
- **Access**: Authenticated Officer/Supervisor/Admin

### 5.3 Trigger Manual Nudge Scan
- **Endpoint**: `POST /api/reminders/trigger-manual-nudge`
- **Access**: `SUPERVISOR`, `ADMIN`

---

## 6. Statutory Escalations (`/api/escalations`)

### 6.1 List All Escalations
- **Endpoint**: `GET /api/escalations`
- **Access**: `OFFICER`, `SUPERVISOR`, `ADMIN`
- **Response**: Array of escalation records with `complaintTrackingNumber`, `fromLevel`, `toLevel`, `triggerReason`, `escalatedAt`, `acknowledged`.

### 6.2 Acknowledge Escalation
- **Endpoint**: `PUT /api/escalations/{id}/acknowledge` (also `POST`)
- **Access**: `SUPERVISOR`, `ADMIN`

### 6.3 Trigger Escalation Scan
- **Endpoint**: `POST /api/escalations/trigger-scan`
- **Access**: `ADMIN`

---

## 7. Action History Audit Log (`/api/history`)

### 7.1 Global Audit Log
- **Endpoint**: `GET /api/history`
- **Access**: `OFFICER`, `SUPERVISOR`, `ADMIN`
- **Response**: Array of audit log rows (`complaintId`, `actorName`, `actorRole`, `action`, `fromStatus`, `toStatus`, `remarks`, `isInternal`, `createdAt`).

### 7.2 Complaint Internal Audit Trail
- **Endpoint**: `GET /api/history/complaint/{complaintId}/internal`

### 7.3 Complaint Citizen Timeline
- **Endpoint**: `GET /api/history/complaint/{complaintId}/citizen` (Public)

---

## 8. In-App Notifications (`/api/notifications`)

- `GET /api/notifications` - Logged-in user's notifications.
- `GET /api/notifications/unread-count` - Returns `{ "unreadCount": 3 }`.
- `PUT /api/notifications/{id}/read` - Mark single notification as read.
- `PUT /api/notifications/mark-all-read` - Mark all user notifications as read.

---

## 9. Departments & Officers (`/api/departments`, `/api/officers`)

- `GET /api/departments` - List all active departments.
- `GET /api/departments/{id}` - Department details.
- `GET /api/departments/{id}/officers` - List of officers belonging to a department.
- `GET /api/officers` - List all officers across all departments.
- `GET /api/officers/{id}` - Specific officer profile.

---

## 10. System Settings & Demo Accelerator (`/api/settings`, `/api/dev/demo`)

### 10.1 System Settings
- `GET /api/settings` - All system configuration keys (SLA hours, reminder lead times, demo mode flag).
- `PUT /api/settings/{key}` - Update setting value (`ADMIN` only).

### 10.2 Dev Demo Accelerator
- `POST /api/dev/demo/run-cycle` - Manually triggers follow-up engine cycle:
  - Generates pre-deadline reminders
  - Transitions overdue complaints
  - Executes Level 1 & Level 2 statutory escalations
  - Recalculates health scores
- `POST /api/dev/demo/set-mode?enabled=true` - Toggles demo mode (treats SLA hours as minutes).
- `GET /api/dev/demo/status` - Current demo mode status.


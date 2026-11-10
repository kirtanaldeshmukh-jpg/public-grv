export type Role =
  | 'ROLE_CITIZEN'
  | 'ROLE_OFFICER'
  | 'ROLE_SUPERVISOR'
  | 'ROLE_ADMIN'
  | 'ROLE_DATA_ENTRY'
  | 'CITIZEN'
  | 'OFFICER'
  | 'SUPERVISOR'
  | 'ADMIN'
  | 'DATA_ENTRY';

export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';

export type ComplaintStatus =
  | 'NEW'
  | 'ASSIGNED'
  | 'TRIAGED'
  | 'IN_PROGRESS'
  | 'OVERDUE'
  | 'ESCALATED'
  | 'RESOLUTION_SUBMITTED'
  | 'REOPENED'
  | 'RESOLVED'
  | 'REJECTED'
  | 'SUBMITTED';

export type HealthBand = 'HEALTHY' | 'ATTENTION' | 'CRITICAL';

export interface HealthScoreDetail {
  score: number;
  band: HealthBand;
  reasons: string[];
}

export interface NextBestAction {
  action: string;
  title: string;
  reason: string;
  targetRole: string;
  urgency: string;
}

export interface CommunityIssueCluster {
  id: number;
  title: string;
  category: string;
  departmentName: string;
  latitude?: number;
  longitude?: number;
  locationAddress?: string;
  status: string;
  complaintCount: number;
  linkedTrackingIds: string[];
  createdAt?: string;
}

export interface OfficerRecommendedAction {
  complaintId: number;
  trackingId: string;
  title: string;
  action: string;
  actionTitle: string;
  reason: string;
  urgency: string;
  targetRole: string;
}

export interface OfficerRecommendedActionsGroup {
  officerId: number;
  officerName: string;
  departmentName: string;
  overdueCount: number;
  approachingDeadlineCount: number;
  pendingCitizenVerificationCount: number;
  totalActionableCount: number;
  actions: OfficerRecommendedAction[];
}

export interface Department {
  id: number;
  code: string;
  nameEn: string;
  nameKn: string;
  description?: string;
  defaultSlaDays: number;
  escalationHours: number;
  active: boolean;
  totalOfficers?: number;
  activeGrievances?: number;
  slaCompliance?: number;
}

export interface Officer {
  id: number;
  userId: number;
  fullName: string;
  employeeCode: string;
  designation: string;
  departmentId: number;
  departmentNameEn: string;
  phoneNumber?: string;
  email?: string;
  activeCases?: number;
  resolvedCases?: number;
  active: boolean;
}

export interface CitizenTimelineItem {
  id: number;
  action: string;
  fromStatus: string | null;
  toStatus: string | null;
  publicRemarks: string;
  timestamp: string;
}

export interface AttachmentItem {
  id: number;
  originalFilename: string;
  contentType: string;
  fileSize: number;
  createdAt: string;
  downloadUrl?: string;
}

export interface CitizenTrackingResponse {
  trackingId?: string;
  trackingNumber: string;
  title: string;
  description: string;
  category: string;
  departmentNameEn: string;
  departmentNameKn: string;
  status: ComplaintStatus;
  slaDueDate: string;
  followUpDeadline?: string;
  createdAt: string;
  escalationLevel: number;
  timeline: CitizenTimelineItem[];
  attachments?: AttachmentItem[];
  resolutionSummary?: string;
  resolutionSubmittedAt?: string;
  canConfirmResolution?: boolean;
  reopenCount?: number;
  communityIssue?: CommunityIssueCluster;
  healthScoreDetail?: HealthScoreDetail;
  nextBestAction?: NextBestAction;
}

export interface InternalAuditItem {
  id: number;
  complaintId: number;
  actorId: number | null;
  actorName: string;
  actorRole: string;
  action: string;
  fromStatus: string | null;
  toStatus: string | null;
  remarks: string;
  isInternal: boolean;
  createdAt: string;
}

export interface OfficerComplaintDetail {
  id: number;
  trackingId?: string;
  trackingNumber: string;
  citizenId: number;
  citizenName: string;
  citizenPhone: string;
  departmentId: number;
  departmentNameEn: string;
  assignedOfficerId: number | null;
  assignedOfficerName: string | null;
  title: string;
  description: string;
  category: string;
  priority: Priority;
  status: ComplaintStatus;
  source?: string;
  latitude: number | null;
  longitude: number | null;
  locationAddress: string | null;
  slaDueDate: string;
  followUpDeadline?: string;
  escalationLevel: number;
  isOverdue: boolean;
  healthScore?: number;
  aiSuggestedDepartmentId: number | null;
  aiSuggestedPriority: Priority | null;
  aiTriageConfirmed: boolean;
  createdAt: string;
  updatedAt: string;
  history: InternalAuditItem[];
  attachments?: AttachmentItem[];
  healthScoreDetail?: HealthScoreDetail;
  nextBestAction?: NextBestAction;
  communityIssue?: CommunityIssueCluster;
  reopenCount?: number;
  resolutionSummary?: string;
  resolutionSubmittedAt?: string;
  canConfirmResolution?: boolean;
}

export interface DashboardSummary {
  total: number;
  new: number;
  assigned: number;
  inProgress: number;
  pending: number;
  overdue: number;
  resolved: number;
}

export interface ReminderItem {
  id: number;
  complaintId: number;
  trackingId: string;
  officerId: number;
  officerName: string;
  departmentName: string;
  reminderType: string;
  nudgeCount: number;
  channel: string;
  message: string;
  sentAt: string;
  acknowledged: boolean;
  acknowledgedAt?: string;
  acknowledgedBy?: string;
}

export interface EscalationItem {
  id: number;
  complaintId: number;
  trackingId: string;
  departmentName: string;
  escalationLevel: number;
  triggerReason: string;
  escalatedFromRole: string;
  escalatedToRole: string;
  escalatedToOfficerName?: string;
  triggerTimestamp: string;
  acknowledged: boolean;
  acknowledgedAt?: string;
  notes?: string;
}

export interface NotificationItem {
  id: number;
  userId: number;
  complaintId?: number;
  trackingId?: string;
  title: string;
  message: string;
  channel: string;
  read: boolean;
  readAt?: string;
  createdAt: string;
}

export interface SystemSettingItem {
  settingKey: string;
  settingValue: string;
  description: string;
  updatedAt?: string;
}

export interface AuthUser {
  token: string;
  userId: number;
  username: string;
  fullName: string;
  email: string;
  role: Role;
  departmentId?: number;
  officerId?: number;
}

export interface PageResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
  empty: boolean;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface OfficerWorkload {
  officerId: number;
  officerName: string;
  employeeCode: string;
  departmentName: string;
  activeCount: number;
  overdueCount: number;
  resolvedCount: number;
}

export interface DepartmentOverdue {
  departmentId: number;
  departmentNameEn: string;
  departmentNameKn: string;
  departmentCode: string;
  overdueCount: number;
  totalCount: number;
  overduePercentage: number;
}

export interface AreaOverdue {
  areaName: string;
  latitude: number;
  longitude: number;
  complaintCount: number;
  overdueCount: number;
  criticalCount: number;
  radiusMeters: number;
}

export interface AnalyticsDashboardData {
  averageResolutionTimeHours: number;
  overduePercentage: number;
  pendingCount: number;
  escalationCount: number;
  reopenedCount: number;
  totalCount: number;
  resolvedCount: number;
  officerWorkload: OfficerWorkload[];
  overdueByDepartment: DepartmentOverdue[];
  overdueByArea: AreaOverdue[];
}

export interface TopPriorityComplaint {
  complaintId: number;
  trackingId: string;
  title: string;
  category: string;
  departmentName: string;
  priority: string;
  status: string;
  healthScore: number;
  healthBand: string;
  locationAddress: string;
  deadline: string;
  recommendedAction: string;
  actionKey: string;
}

export interface SupervisorBriefing {
  date: string;
  greetingEn: string;
  greetingKn: string;
  departmentNameEn: string;
  departmentNameKn: string;
  pendingCount: number;
  overdueCount: number;
  escalationCount: number;
  reopenedCount: number;
  approachingDeadlineCount: number;
  narrativeEn: string;
  narrativeKn: string;
  topPriorityComplaint: TopPriorityComplaint | null;
}

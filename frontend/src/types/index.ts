// API Response Types
export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data?: T;
  error?: string;
  timestamp?: string;
}

export interface Page<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: {
      sorted: boolean;
      empty: boolean;
      unsorted: boolean;
    };
  };
  totalPages: number;
  totalElements: number;
  last: boolean;
  first: boolean;
  size: number;
  number: number;
  numberOfElements: number;
  empty: boolean;
}

// Authentication
export interface AuthenticationRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
}

export interface AuthenticationResponse {
  token: string;
  userId: string;
  email: string;
  firstName: string;
  lastName: string;
}

// Tenant
export interface TenantRequest {
  name: string;
  email: string;
}

export interface TenantResponse {
  id: string;
  name: string;
  email: string;
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
}

export interface TenantDashboardResponse {
  totalCvs: number;
  totalMatches: number;
  totalCampaigns: number;
  totalEmailsSent: number;
  totalEmailsFailed: number;
  smtpConfigured: boolean;
  campaignStatusCounts: Record<string, number>;
}

// CV
export interface CVResponse {
  id: string;
  originalFilename: string;
  fileSizeBytes: number;
  mimeType: string;
  parsingStatus: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED';
  parsedAt?: string;
  uploadedAt: string;
  keywordCount?: number;
}

// Match Results
export interface ProfessorSummary {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  department: string;
  universityName: string;
  universityCountry: string;
}

export interface MatchResultResponse {
  id: string;
  professor: ProfessorSummary;
  matchScore: number;
  matchedKeywords: string;
  totalCvKeywords: number;
  totalProfessorKeywords: number;
  totalMatchedKeywords: number;
}

// Email Campaigns
export interface CreateCampaignRequest {
  cvId: string;
  name: string;
  subject: string;
  bodyTemplate: string;
  minMatchScore: number;
}

export interface EmailCampaignResponse {
  id: string;
  name: string;
  subject: string;
  minMatchScore: number;
  status: 'DRAFT' | 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED' | 'FAILED';
  totalRecipients: number;
  sentCount: number;
  failedCount: number;
  scheduledAt?: string;
  startedAt?: string;
  completedAt?: string;
  createdAt: string;
}

export interface EmailLogResponse {
  id: string;
  recipientEmail: string;
  subject: string;
  status: 'PENDING' | 'SENT' | 'FAILED' | 'BOUNCED';
  errorMessage?: string;
  retryCount: number;
  sentAt?: string;
  createdAt: string;
  professorId: string;
}

// SMTP Account
export interface SmtpAccountRequest {
  email: string;
  smtpHost: string;
  smtpPort: number;
  username: string;
  password: string;
  useTls?: boolean;
  useSsl?: boolean;
  fromName?: string;
}

export interface SmtpAccountResponse {
  id: string;
  email: string;
  smtpHost: string;
  smtpPort: number;
  username: string;
  useTls: boolean;
  useSsl: boolean;
  fromName?: string;
  status: 'ACTIVE' | 'INACTIVE' | 'FAILED';
}

// Incoming Email
export interface IncomingEmailResponse {
  from: string;
  subject: string;
  bodyPreview: string;
  receivedDate: string;
}

// ── Enums & Union Types ───────────────────────────────────────────────────────
export type UserRole = 'FOUNDER' | 'INVESTOR' | 'COFOUNDER' | 'ADMIN';
export type StartupStage = 'IDEA' | 'MVP' | 'EARLY_TRACTION' | 'SCALING';
export type InvestmentStatus =
  | 'PENDING'
  | 'APPROVED'
  | 'REJECTED'
  | 'COMPLETED'
  | 'PAYMENT_FAILED'
  | 'STARTUP_CLOSED';
export type PaymentStatus = 'PENDING' | 'INITIATED' | 'SUCCESS' | 'FAILED';
export type InvitationStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'CANCELLED';
export type TeamRole = 'CTO' | 'CPO' | 'MARKETING_HEAD' | 'ENGINEERING_LEAD';

// Legacy alias kept for compatibility
export type Role = UserRole;

// ── Normalized API Envelope ───────────────────────────────────────────────────
/** Canonical shape every data service exposes to components */
export interface ApiEnvelope<T> {
  success: boolean;
  data: T | null;
  error: string | null;
}

/** Raw backend wrapped response shape  { message, data } */
export interface ApiResponse<T> {
  message: string;
  data: T;
}

export interface PaginatedData<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface PaginatedApiResponse<T> {
  success: boolean;
  data: PaginatedData<T> | null;
  error: string | null;
}

export interface PaginationQuery {
  page?: number;
  size?: number;
  sort?: string;
}

// ── Auth ──────────────────────────────────────────────────────────────────────
export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
  role: UserRole;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  email: string;
  role: string;
  userId: number;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  email: string;
  pin: string;
  newPassword: string;
}

// ── User ──────────────────────────────────────────────────────────────────────
export interface UserResponse {
  userId: number;
  name: string | null;
  email: string;
  role: UserRole;
  skills: string | null;
  experience: string | null;
  bio: string | null;
  portfolioLinks: string | null;
}

export interface UserUpdateRequest {
  name?: string | null;
  skills?: string | null;
  experience?: string | null;
  bio?: string | null;
  portfolioLinks?: string | null;
}

// ── Startup ───────────────────────────────────────────────────────────────────
export interface StartupRequest {
  name: string;
  description: string;
  industry: string;
  problemStatement: string;
  solution: string;
  fundingGoal: number;
  stage: StartupStage;
}

export interface StartupResponse {
  id: number;
  name: string;
  description: string;
  industry: string;
  problemStatement: string;
  solution: string;
  fundingGoal: number;
  stage: StartupStage;
  founderId: number;
  createdAt: string;
}

// ── Investment ────────────────────────────────────────────────────────────────
export interface InvestmentRequest {
  startupId: number;
  amount: number;
}

export interface InvestmentStatusUpdate {
  status: 'APPROVED' | 'REJECTED' | 'COMPLETED';
}

export interface InvestmentResponse {
  id: number;
  startupId: number;
  investorId: number;
  amount: number;
  status: InvestmentStatus;
  createdAt: string;
}

// ── Team ──────────────────────────────────────────────────────────────────────
export interface InvitationRequest {
  startupId: number;
  invitedUserId: number;
  role: TeamRole;
}

export interface InvitationResponse {
  id: number;
  startupId: number;
  founderId: number;
  invitedUserId: number;
  role: TeamRole;
  status: InvitationStatus;
  createdAt: string;
  updatedAt: string | null;
}

export interface JoinTeamRequest {
  invitationId: number;
}

export interface TeamMemberResponse {
  id: number;
  startupId: number;
  userId: number;
  role: TeamRole;
  isActive: boolean;
  joinedAt: string;
  leftAt: string | null;
}

// ── Messaging ─────────────────────────────────────────────────────────────────
export interface MessageRequest {
  senderId: number;
  receiverId: number;
  content: string;
}

export interface MessageResponse {
  id: number;
  senderId: number;
  receiverId: number;
  content: string;
  createdAt: string;
}

/** Response shape from GET /messages/conversation/{u1}/{u2}/cursor */
export interface CursorPage<T> {
  content: T[];
  /** ID of the oldest message in this batch — pass as ?before= to load older history */
  nextCursor: number | null;
  /** ID of the newest message in this batch — pass as ?after= for catch-up */
  prevCursor: number | null;
}

// ── Notification ──────────────────────────────────────────────────────────────
export interface NotificationResponse {
  id: number;
  userId: number;
  type: string;
  message: string;
  read: boolean;
  createdAt: string;
}

// ── Payment ───────────────────────────────────────────────────────────────────
export interface CreateOrderRequest {
  investmentId: number;
}

export interface CreateOrderResponse {
  orderId: string;       // Razorpay order id
  amount: number;
  currency: string;
  investmentId: number;
}

export interface ConfirmPaymentRequest {
  razorpayOrderId: string;
  razorpayPaymentId: string;
  razorpaySignature: string;
}

export interface ConfirmPaymentResponse {
  status: string;
  investmentId: number;
}

export interface PaymentResponse {
  id: number;
  investmentId: number;
  investorId: number;
  startupId: number;
  founderId: number;
  amount: number;
  status: PaymentStatus;
  externalPaymentId: string | null;   // maps to razorpayPaymentId on backend
  failureReason: string | null;
  createdAt: string;
  updatedAt: string;
}

// ── Wallet ────────────────────────────────────────────────────────────────────
export interface WalletResponse {
  id: number;
  startupId: number;
  balance: number;
  createdAt: string;
  updatedAt: string;
}

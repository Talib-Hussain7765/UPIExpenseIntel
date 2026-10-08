export interface AuthResponse {
  tokenType: string;
  accessToken: string;
  expiresInSeconds: number;
  userId: string;
  email: string;
  fullName: string;
}

export interface CurrentUser {
  id: string;
  email: string;
  fullName: string;
  timezone: string;
  currency: string;
  monthlyReportEnabled: boolean;
  monthlyReportDay: number;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  fullName: string;
}

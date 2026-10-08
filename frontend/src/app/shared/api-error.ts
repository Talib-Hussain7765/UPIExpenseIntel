export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors: Record<string, string>;
}

export function apiErrorMessage(error: unknown): string {
  if (typeof error === 'object' && error !== null && 'error' in error) {
    const response = (error as { error?: Partial<ApiErrorResponse> }).error;
    if (response?.message) {
      return response.message;
    }
  }
  return 'Request failed';
}

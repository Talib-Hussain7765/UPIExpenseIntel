import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse, CurrentUser, LoginRequest, RegisterRequest } from './auth.models';

const ACCESS_TOKEN_KEY = 'upi_expense_intel_access_token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly currentUser = signal<CurrentUser | null>(null);

  constructor(private readonly http: HttpClient, private readonly router: Router) {}

  token(): string | null {
    return localStorage.getItem(ACCESS_TOKEN_KEY);
  }

  isAuthenticated(): boolean {
    return !!this.token();
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${environment.apiBaseUrl}/auth/login`, request).pipe(
      tap((response) => this.storeAuth(response))
    );
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${environment.apiBaseUrl}/auth/register`, request).pipe(
      tap((response) => this.storeAuth(response))
    );
  }

  loadCurrentUser(): Observable<CurrentUser> {
    return this.http.get<CurrentUser>(`${environment.apiBaseUrl}/auth/me`).pipe(
      tap((user) => this.currentUser.set(user))
    );
  }

  logout(): void {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    this.currentUser.set(null);
    this.router.navigateByUrl('/login');
  }

  expireSession(returnUrl = this.router.url): void {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    this.currentUser.set(null);
    this.router.navigate(['/login'], {
      queryParams: {
        sessionExpired: 'true',
        returnUrl: this.safeReturnUrl(returnUrl)
      }
    });
  }

  private storeAuth(response: AuthResponse): void {
    localStorage.setItem(ACCESS_TOKEN_KEY, response.accessToken);
    this.currentUser.set({
      id: response.userId,
      email: response.email,
      fullName: response.fullName,
      timezone: 'Asia/Kolkata',
      currency: 'INR',
      monthlyReportEnabled: false,
      monthlyReportDay: 1
    });
  }

  private safeReturnUrl(returnUrl: string): string {
    if (returnUrl.startsWith('/') && !returnUrl.startsWith('//') && !returnUrl.startsWith('/login')) {
      return returnUrl;
    }

    return '/dashboard';
  }
}

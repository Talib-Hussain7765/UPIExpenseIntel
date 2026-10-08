import { Component, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/auth/auth.service';
import { apiErrorMessage } from '../../../shared/api-error';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly infoMessage = signal<string | null>(null);
  readonly form = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]]
  });

  private readonly returnUrl: string;

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly authService: AuthService,
    private readonly router: Router,
    route: ActivatedRoute
  ) {
    const queryParams = route.snapshot.queryParamMap;
    this.returnUrl = this.safeReturnUrl(queryParams.get('returnUrl'));

    if (queryParams.get('sessionExpired') === 'true') {
      this.infoMessage.set('Your session expired. Please sign in again.');
    }
  }

  submit(): void {
    if (this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);
    this.authService.login(this.form.getRawValue()).subscribe({
      next: () => this.router.navigateByUrl(this.returnUrl),
      error: (error) => {
        this.errorMessage.set(apiErrorMessage(error));
        this.loading.set(false);
      }
    });
  }

  private safeReturnUrl(returnUrl: string | null): string {
    if (returnUrl?.startsWith('/') && !returnUrl.startsWith('//') && !returnUrl.startsWith('/login')) {
      return returnUrl;
    }

    return '/dashboard';
  }
}

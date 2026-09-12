import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-auth-callback',
  standalone: true,
  template: `
    <div class="text-center py-5">
      @if (error()) {
        <p class="text-danger">{{ error() }}</p>
        <button class="btn btn-accent" (click)="auth.login()">Try logging in again</button>
      } @else {
        <p class="text-muted">Signing you in…</p>
      }
    </div>
  `
})
export class AuthCallbackComponent implements OnInit {
  error = () => this._error;
  private _error: string | null = null;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    readonly auth: AuthService
  ) {}

  async ngOnInit(): Promise<void> {
    const params = this.route.snapshot.queryParamMap;
    const code = params.get('code');
    const state = params.get('state');
    const oauthError = params.get('error');

    if (oauthError) {
      this._error = `Keycloak returned an error: ${oauthError}`;
      return;
    }
    if (!code || !state) {
      this._error = 'Missing authorization code — this page should only be reached via the Keycloak redirect.';
      return;
    }

    try {
      await this.auth.handleCallback(code, state);
      this.router.navigateByUrl('/');
    } catch (e) {
      this._error = e instanceof Error ? e.message : 'Login failed.';
    }
  }
}

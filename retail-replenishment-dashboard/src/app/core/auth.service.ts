import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { generateCodeChallenge, generateCodeVerifier, generateState } from './pkce.util';

interface TokenResponse {
  access_token: string;
  id_token?: string;
  refresh_token?: string;
  expires_in: number;
  token_type: string;
}

const CODE_VERIFIER_KEY = 'rrd_pkce_code_verifier';
const STATE_KEY = 'rrd_pkce_state';
const ACCESS_TOKEN_KEY = 'rrd_access_token';
const ID_TOKEN_KEY = 'rrd_id_token';

/** Decodes the realm roles out of a JWT's payload for UI gating only (show/hide
 * an action button). This is NOT a security boundary — the backend enforces
 * every role check for real; a user could hand-edit sessionStorage and see a
 * button that then 403s. It exists purely so the UI doesn't invite an action
 * the API will reject, not to guard access to anything sensitive.
 */
function decodeRoles(accessToken: string): string[] {
  try {
    const payloadB64 = accessToken.split('.')[1];
    const json = atob(payloadB64.replace(/-/g, '+').replace(/_/g, '/'));
    const payload = JSON.parse(json);
    return payload?.realm_access?.roles ?? [];
  } catch {
    return [];
  }
}

/**
 * Authorization Code + PKCE against the retail-replenishment-dashboard
 * Keycloak client (public, no secret). sessionStorage is used for the
 * access/id tokens — acceptable for this POC's scope; a hardened build
 * would move to an in-memory token + silent-refresh iframe instead.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly isAuthenticated = signal(!!sessionStorage.getItem(ACCESS_TOKEN_KEY));
  readonly roles = signal<string[]>(
      sessionStorage.getItem(ACCESS_TOKEN_KEY) ? decodeRoles(sessionStorage.getItem(ACCESS_TOKEN_KEY)!) : []
  );

  constructor(private readonly http: HttpClient) {}

  getAccessToken(): string | null {
    return sessionStorage.getItem(ACCESS_TOKEN_KEY);
  }

  hasRole(role: string): boolean {
    return this.roles().includes(role);
  }

  async login(): Promise<void> {
    const verifier = generateCodeVerifier();
    const challenge = await generateCodeChallenge(verifier);
    const state = generateState();

    sessionStorage.setItem(CODE_VERIFIER_KEY, verifier);
    sessionStorage.setItem(STATE_KEY, state);

    const { issuer, clientId, redirectUri, scope } = environment.keycloak;
    const params = new URLSearchParams({
      response_type: 'code',
      client_id: clientId,
      redirect_uri: redirectUri,
      scope,
      state,
      code_challenge: challenge,
      code_challenge_method: 'S256'
    });

    window.location.href = `${issuer}/protocol/openid-connect/auth?${params.toString()}`;
  }

  async handleCallback(code: string, state: string): Promise<void> {
    const expectedState = sessionStorage.getItem(STATE_KEY);
    const verifier = sessionStorage.getItem(CODE_VERIFIER_KEY);
    if (!verifier || state !== expectedState) {
      throw new Error('PKCE state mismatch — possible CSRF or a stale/duplicate callback. Please log in again.');
    }

    const { issuer, clientId, redirectUri } = environment.keycloak;
    const body = new URLSearchParams({
      grant_type: 'authorization_code',
      client_id: clientId,
      redirect_uri: redirectUri,
      code,
      code_verifier: verifier
    });

    const response = await firstValueFrom(
        this.http.post<TokenResponse>(`${issuer}/protocol/openid-connect/token`, body.toString(), {
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
        })
    );

    sessionStorage.setItem(ACCESS_TOKEN_KEY, response.access_token);
    if (response.id_token) {
      sessionStorage.setItem(ID_TOKEN_KEY, response.id_token);
    }
    sessionStorage.removeItem(CODE_VERIFIER_KEY);
    sessionStorage.removeItem(STATE_KEY);
    this.isAuthenticated.set(true);
    this.roles.set(decodeRoles(response.access_token));
  }

  logout(): void {
    const idToken = sessionStorage.getItem(ID_TOKEN_KEY);
    sessionStorage.removeItem(ACCESS_TOKEN_KEY);
    sessionStorage.removeItem(ID_TOKEN_KEY);
    this.isAuthenticated.set(false);
    this.roles.set([]);

    const { issuer, clientId, redirectUri } = environment.keycloak;
    const params = new URLSearchParams({
      post_logout_redirect_uri: redirectUri.replace('/callback', ''),
      client_id: clientId
    });
    if (idToken) {
      params.set('id_token_hint', idToken);
    }
    window.location.href = `${issuer}/protocol/openid-connect/logout?${params.toString()}`;
  }
}
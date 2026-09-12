import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';

/**
 * Attaches the bearer token to requests aimed at this API — not to
 * Keycloak's own endpoints. Also handles the session-expiry case: access
 * tokens here live 15 minutes (see the realm's accessTokenLifespan), and
 * without this, an expired token produced a confusing generic "could not
 * load" message on every screen instead of sending the user back through
 * login. A 403 is deliberately NOT handled here — that means a valid
 * session with the wrong role, which each component should surface with
 * its own specific message, not a forced re-login.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  if (!req.url.startsWith(environment.apiBaseUrl)) {
    return next(req);
  }

  const token = auth.getAccessToken();
  const authedReq = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(authedReq).pipe(
      catchError((err: unknown) => {
        if (err instanceof HttpErrorResponse && err.status === 401) {
          auth.login();
        }
        return throwError(() => err);
      })
  );
};
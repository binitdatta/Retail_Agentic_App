import { inject } from '@angular/core';
import { CanActivateFn } from '@angular/router';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);

  if (auth.isAuthenticated()) {
    return true;
  }
  await auth.login();
  // login() navigates the whole browser away via window.location.href, so
  // we never actually need the guard to resolve — return false in case
  // that navigation hasn't taken effect yet on this tick.
  return false;
};
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8087/api',
  keycloak: {
    issuer: 'http://localhost:8080/realms/retail-replenishment',
    clientId: 'retail-replenishment-dashboard',
    redirectUri: 'http://localhost:4200/callback',
    // Space-separated, per the OIDC spec.
    scope: 'openid profile'
  }
};

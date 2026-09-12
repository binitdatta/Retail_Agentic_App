export const environment = {
  production: true,
  // Overridden per deployment — see README for the env-driven config pathway.
  apiBaseUrl: 'https://api.retail-replenishment.example.com/api',
  keycloak: {
    issuer: 'https://auth.example.com/realms/retail-replenishment',
    clientId: 'retail-replenishment-dashboard',
    redirectUri: 'https://dashboard.retail-replenishment.example.com/callback',
    scope: 'openid profile'
  }
};

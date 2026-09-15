import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/agent-run-list/agent-run-list.component').then((m) => m.AgentRunListComponent)
  },
  {
    path: 'runs/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/agent-run-detail/agent-run-detail.component').then((m) => m.AgentRunDetailComponent)
  },
  {
    path: 'runs/:id/http-trace',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/llm-http-trace/llm-http-trace.component').then((m) => m.LlmHttpTraceComponent)
  },
  {
    path: 'orders',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/orders-list/orders-list.component').then((m) => m.OrdersListComponent)
  },
  {
    path: 'orders/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/order-detail/order-detail.component').then((m) => m.OrderDetailComponent)
  },
  {
    path: 'escalations',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/escalations-list/escalations-list.component').then((m) => m.EscalationsListComponent)
  },
  {
    path: 'inventory',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/inventory-low-stock/inventory-low-stock.component').then((m) => m.InventoryLowStockComponent)
  },
  {
    path: 'training/demo-walkthrough',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/training/demo-walkthrough/demo-walkthrough.component').then((m) => m.TrainingDemoWalkthroughComponent)
  },
  {
    path: 'training/business-case',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/training/business-case/business-case.component').then((m) => m.TrainingBusinessCaseComponent)
  },
  {
    path: 'audit',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/audit-log/audit-log.component').then((m) => m.AuditLogComponent)
  },
  {
    path: 'training/architecture',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/training/architecture/architecture.component').then((m) => m.TrainingArchitectureComponent)
  },
  {
    path: 'training/mysql',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/training/mysql/mysql.component').then((m) => m.TrainingMysqlComponent)
  },
  {
    path: 'training/keycloak',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/training/keycloak/keycloak.component').then((m) => m.TrainingKeycloakComponent)
  },
  {
    path: 'training/spring-boot',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/training/spring-boot/spring-boot.component').then((m) => m.TrainingSpringBootComponent)
  },
  {
    path: 'training/angular-dashboard',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/training/angular-dashboard/angular-dashboard.component').then((m) => m.TrainingAngularDashboardComponent)
  },
  {
    path: 'training/python-agent-reference',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/training/python-agent-reference/python-agent-reference.component').then((m) => m.TrainingPythonAgentReferenceComponent)
  },
  {
    path: 'training/python-agent',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/training/python-agent/python-agent.component').then((m) => m.TrainingPythonAgentComponent)
  },
  {
    path: 'training/setup-guide',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/training/setup-guide/setup-guide.component').then((m) => m.TrainingSetupGuideComponent)
  },
  {
    path: 'callback',
    loadComponent: () =>
      import('./features/auth-callback/auth-callback.component').then((m) => m.AuthCallbackComponent)
  },
  { path: '**', redirectTo: '' }
];

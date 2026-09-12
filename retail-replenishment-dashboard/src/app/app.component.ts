import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';
import { ActorNameService } from './core/actor-name.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <nav class="navbar navbar-expand app-navbar px-3 mb-2">
      <a class="navbar-brand fw-semibold" routerLink="/">Retail Replenishment</a>
      @if (auth.isAuthenticated()) {
        <ul class="navbar-nav flex-row gap-3 ms-3 flex-wrap">
          <li class="nav-item">
            <a class="nav-link" routerLink="/" routerLinkActive="fw-bold" [routerLinkActiveOptions]="{exact: true}">Agent runs</a>
          </li>
          <li class="nav-item">
            <a class="nav-link" routerLink="/orders" routerLinkActive="fw-bold">Orders</a>
          </li>
          <li class="nav-item">
            <a class="nav-link" routerLink="/escalations" routerLinkActive="fw-bold">Escalations</a>
          </li>
          <li class="nav-item">
            <a class="nav-link" routerLink="/inventory" routerLinkActive="fw-bold">Inventory</a>
          </li>
          <li class="nav-item dropdown">
            <a class="nav-link dropdown-toggle" href="#" role="button" data-bs-toggle="dropdown" aria-expanded="false">
              Training
            </a>
            <ul class="dropdown-menu">
              <li><a class="dropdown-item" routerLink="/training/business-case">Business case</a></li>
              <li><a class="dropdown-item" routerLink="/training/demo-walkthrough">Demo run walkthrough</a></li>
              <li><hr class="dropdown-divider"></li>
              <li><a class="dropdown-item" routerLink="/training/architecture">1. Architecture</a></li>
              <li><a class="dropdown-item" routerLink="/training/mysql">2. MySQL 8</a></li>
              <li><a class="dropdown-item" routerLink="/training/keycloak">3. Keycloak</a></li>

              <li class="dropdown-submenu">
                <a class="dropdown-item dropdown-toggle" href="#" routerLink="/training/spring-boot">4. Spring Boot</a>
                <ul class="dropdown-menu">
                  <li><a class="dropdown-item" routerLink="/training/spring-boot" fragment="overview">Overview</a></li>
                  <li><a class="dropdown-item" routerLink="/training/spring-boot" fragment="entities">Entities &amp; repositories</a></li>
                  <li><a class="dropdown-item" routerLink="/training/spring-boot" fragment="security">Security</a></li>
                  <li><a class="dropdown-item" routerLink="/training/spring-boot" fragment="messaging">Messaging</a></li>
                </ul>
              </li>

              <li class="dropdown-submenu">
                <a class="dropdown-item dropdown-toggle" href="#" routerLink="/training/angular-dashboard">5. Angular dashboard</a>
                <ul class="dropdown-menu">
                  <li><a class="dropdown-item" routerLink="/training/angular-dashboard" fragment="overview">Overview</a></li>
                  <li><a class="dropdown-item" routerLink="/training/angular-dashboard" fragment="auth">Auth (PKCE)</a></li>
                  <li><a class="dropdown-item" routerLink="/training/angular-dashboard" fragment="hitl">HITL screens</a></li>
                </ul>
              </li>

              <li class="dropdown-submenu">
                <a class="dropdown-item dropdown-toggle" href="#" routerLink="/training/python-agent">6. Python agent</a>
                <ul class="dropdown-menu">
                  <li><a class="dropdown-item" routerLink="/training/python-agent" fragment="overview">Overview</a></li>
                  <li><a class="dropdown-item" routerLink="/training/python-agent" fragment="pipeline">LangGraph pipeline</a></li>
                  <li><a class="dropdown-item" routerLink="/training/python-agent" fragment="llm">LLM judgment calls</a></li>
                </ul>
              </li>

              <li><hr class="dropdown-divider"></li>
              <li><a class="dropdown-item fw-semibold" routerLink="/training/setup-guide">7. Setup guide — start here</a></li>
            </ul>
          </li>
        </ul>
      }
      <div class="ms-auto d-flex align-items-center gap-2">
        @if (auth.isAuthenticated()) {
          @if (auth.hasRole('SUPPLY_CHAIN_MANAGER')) {
            <span class="badge bg-light text-dark">Manager</span>
          } @else {
            <span class="badge bg-light text-dark">Viewer</span>
          }
          <input
            class="form-control form-control-sm"
            style="width: 160px;"
            placeholder="Acting as…"
            [value]="actorNameService.actorName()"
            (change)="actorNameService.setActorName($any($event.target).value)"
            title="Recorded as the approver/assignee on any action you take" />
          <button class="btn btn-sm btn-outline-light" (click)="auth.logout()">Log out</button>
        }
      </div>
    </nav>
    <main class="container pb-5">
      <router-outlet />
    </main>
  `
})
export class AppComponent {
  constructor(readonly auth: AuthService, readonly actorNameService: ActorNameService) {}
}

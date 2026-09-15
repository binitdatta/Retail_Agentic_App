import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { AgentRun } from '../../models/agent-run.model';

@Component({
  selector: 'app-agent-run-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './agent-run-list.component.html'
})
export class AgentRunListComponent implements OnInit {
  readonly runs = signal<AgentRun[]>([]);
  readonly loading = signal(true);
  readonly loadError = signal<string | null>(null);
  readonly resetting = signal(false);
  readonly resetMessage = signal<string | null>(null);
  readonly resetError = signal<string | null>(null);

  constructor(
      private readonly api: ApiService,
      private readonly router: Router,
      readonly auth: AuthService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.api.getRecentAgentRuns().subscribe({
      next: (runs) => {
        this.runs.set(runs);
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set('Could not load agent runs. Is the API reachable and are you logged in with a valid role?');
        this.loading.set(false);
      }
    });
  }

  canReset(): boolean {
    return this.auth.hasRole('SUPPLY_CHAIN_MANAGER');
  }

  resetDemo(): void {
    if (!confirm('Reset to the original demo scenario? This wipes all agent runs, orders, escalations, and delivery history — cannot be undone.')) {
      return;
    }
    this.resetting.set(true);
    this.resetMessage.set(null);
    this.resetError.set(null);
    this.api.resetDemoScenario().subscribe({
      next: (lowStock) => {
        this.resetting.set(false);
        this.resetMessage.set(`Reset complete — ${lowStock.length} low-stock item(s) restored.`);
        this.load();
      },
      error: (err) => {
        this.resetting.set(false);
        this.resetError.set(
            err?.status === 403
                ? (err?.error?.message ?? 'Reset is disabled in this environment or you lack the required role.')
                : `Reset failed: ${err?.error?.message ?? err?.message ?? 'unknown error'}`
        );
      }
    });
  }

  open(run: AgentRun): void {
    this.router.navigate(['/runs', run.agentRunId]);
  }

  statusBadgeClass(status: string): string {
    switch (status) {
      case 'COMPLETED': return 'bg-success';
      case 'RUNNING': return 'bg-primary';
      case 'FAILED': return 'bg-danger';
      case 'TIMED_OUT': return 'bg-warning text-dark';
      default: return 'bg-secondary';
    }
  }
}
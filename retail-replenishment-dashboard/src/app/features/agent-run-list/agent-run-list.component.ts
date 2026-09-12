import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ApiService } from '../../core/api.service';
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

  constructor(private readonly api: ApiService, private readonly router: Router) {}

  ngOnInit(): void {
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

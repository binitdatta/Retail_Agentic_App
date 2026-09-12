import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { AgentRun } from '../../models/agent-run.model';
import { DecisionLog } from '../../models/decision-log.model';
import { LlmCallLog } from '../../models/llm-call-log.model';
import { LlmCostRollup } from '../../models/llm-cost-rollup.model';

@Component({
  selector: 'app-agent-run-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './agent-run-detail.component.html'
})
export class AgentRunDetailComponent implements OnInit {
  readonly run = signal<AgentRun | null>(null);
  readonly decisions = signal<DecisionLog[]>([]);
  readonly llmCalls = signal<LlmCallLog[]>([]);
  readonly cost = signal<LlmCostRollup | null>(null);
  readonly loading = signal(true);
  readonly loadError = signal<string | null>(null);
  readonly expandedDecisionId = signal<number | null>(null);

  constructor(private readonly route: ActivatedRoute, private readonly api: ApiService) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    forkJoin({
      run: this.api.getAgentRun(id),
      decisions: this.api.getDecisions(id),
      llmCalls: this.api.getLlmCalls(id),
      cost: this.api.getLlmCost(id)
    }).subscribe({
      next: ({ run, decisions, llmCalls, cost }) => {
        this.run.set(run);
        this.decisions.set(decisions);
        this.llmCalls.set(llmCalls);
        this.cost.set(cost);
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set('Could not load this run. It may not exist, or you may not have a role permitted to view it.');
        this.loading.set(false);
      }
    });
  }

  toggleDecision(decisionId: number): void {
    this.expandedDecisionId.set(this.expandedDecisionId() === decisionId ? null : decisionId);
  }

  formatJson(value: unknown): string {
    return JSON.stringify(value, null, 2);
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

import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { DecisionLog } from '../../models/decision-log.model';
import { LlmCallLog } from '../../models/llm-call-log.model';
import { LlmCostRollup } from '../../models/llm-cost-rollup.model';

const STAGES = [
  '', 'DETECT_LOW_INVENTORY', 'FORECAST_DEMAND', 'CHECK_SUPPLIER_AVAILABILITY',
  'RECOMMEND_REPLENISHMENT', 'MONITOR_DELIVERY', 'ESCALATE_SHORTAGE'
];

@Component({
  selector: 'app-audit-log',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './audit-log.component.html'
})
export class AuditLogComponent implements OnInit {
  readonly cost = signal<LlmCostRollup | null>(null);

  readonly decisions = signal<DecisionLog[]>([]);
  readonly decisionsLoading = signal(true);
  readonly decisionsError = signal<string | null>(null);
  readonly stageFilter = signal<string>('');
  readonly stageOptions = STAGES;
  readonly expandedDecisionId = signal<number | null>(null);

  readonly llmCalls = signal<LlmCallLog[]>([]);
  readonly llmCallsLoading = signal(true);
  readonly llmCallsError = signal<string | null>(null);
  readonly providerFilter = signal<string>('');
  readonly successFilter = signal<string>(''); // '', 'true', 'false'

  constructor(private readonly api: ApiService) {}

  ngOnInit(): void {
    this.api.getGlobalLlmCost().subscribe({ next: (c) => this.cost.set(c), error: () => {} });
    this.loadDecisions();
    this.loadLlmCalls();
  }

  loadDecisions(): void {
    this.decisionsLoading.set(true);
    this.decisionsError.set(null);
    this.api.getAllDecisions(this.stageFilter() || undefined).subscribe({
      next: (d) => { this.decisions.set(d); this.decisionsLoading.set(false); },
      error: () => {
        this.decisionsError.set('Could not load decisions. Is the API reachable and are you logged in with a valid role?');
        this.decisionsLoading.set(false);
      }
    });
  }

  onStageFilterChange(value: string): void {
    this.stageFilter.set(value);
    this.loadDecisions();
  }

  toggleDecision(id: number): void {
    this.expandedDecisionId.set(this.expandedDecisionId() === id ? null : id);
  }

  pretty(value: unknown): string {
    return JSON.stringify(value, null, 2);
  }

  loadLlmCalls(): void {
    this.llmCallsLoading.set(true);
    this.llmCallsError.set(null);
    const success = this.successFilter() === '' ? undefined : this.successFilter() === 'true';
    this.api.getAllLlmCalls(this.providerFilter() || undefined, success).subscribe({
      next: (c) => { this.llmCalls.set(c); this.llmCallsLoading.set(false); },
      error: () => {
        this.llmCallsError.set('Could not load LLM calls. Is the API reachable and are you logged in with a valid role?');
        this.llmCallsLoading.set(false);
      }
    });
  }

  onProviderFilterChange(value: string): void {
    this.providerFilter.set(value);
    this.loadLlmCalls();
  }

  onSuccessFilterChange(value: string): void {
    this.successFilter.set(value);
    this.loadLlmCalls();
  }
}

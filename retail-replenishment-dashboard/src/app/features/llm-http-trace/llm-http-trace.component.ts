import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { LlmHttpTrace } from '../../models/llm-http-trace.model';

@Component({
  selector: 'app-llm-http-trace',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './llm-http-trace.component.html'
})
export class LlmHttpTraceComponent implements OnInit {
  readonly traces = signal<LlmHttpTrace[]>([]);
  readonly loading = signal(true);
  readonly loadError = signal<string | null>(null);
  readonly expandedTraceId = signal<number | null>(null);
  runId!: number;

  constructor(private readonly route: ActivatedRoute, private readonly api: ApiService) {}

  ngOnInit(): void {
    this.runId = Number(this.route.snapshot.paramMap.get('id'));
    this.api.getHttpTraces(this.runId).subscribe({
      next: (traces) => { this.traces.set(traces); this.loading.set(false); },
      error: () => {
        this.loadError.set('Could not load HTTP traces. Is the API reachable and are you logged in with a valid role?');
        this.loading.set(false);
      }
    });
  }

  toggle(traceId: number): void {
    this.expandedTraceId.set(this.expandedTraceId() === traceId ? null : traceId);
  }

  pretty(value: unknown): string {
    return JSON.stringify(value, null, 2);
  }

  statusBadgeClass(status: number | null): string {
    if (status == null) return 'bg-secondary';
    if (status >= 200 && status < 300) return 'bg-success';
    if (status >= 400) return 'bg-danger';
    return 'bg-warning text-dark';
  }
}

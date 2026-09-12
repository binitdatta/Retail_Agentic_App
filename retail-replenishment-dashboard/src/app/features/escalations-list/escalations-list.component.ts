import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { ActorNameService } from '../../core/actor-name.service';
import { Escalation } from '../../models/escalation.model';

const STATUS_OPTIONS = ['', 'OPEN', 'ACKNOWLEDGED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];

@Component({
    selector: 'app-escalations-list',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './escalations-list.component.html'
})
export class EscalationsListComponent implements OnInit {
    readonly escalations = signal<Escalation[]>([]);
    readonly loading = signal(true);
    readonly loadError = signal<string | null>(null);
    readonly actionError = signal<string | null>(null);
    readonly statusFilter = signal<string>('OPEN');
    readonly statusOptions = STATUS_OPTIONS;
    readonly busyId = signal<number | null>(null);
    readonly resolvingId = signal<number | null>(null);
    readonly resolutionDraft = signal('');

    constructor(
        private readonly api: ApiService,
        readonly auth: AuthService,
        readonly actorNameService: ActorNameService
    ) {}

    ngOnInit(): void {
        this.load();
    }

    load(): void {
        this.loading.set(true);
        this.loadError.set(null);
        this.api.getEscalations(undefined, this.statusFilter() || undefined).subscribe({
            next: (escalations) => {
                this.escalations.set(escalations);
                this.loading.set(false);
            },
            error: () => {
                this.loadError.set('Could not load escalations. Is the API reachable and are you logged in with a valid role?');
                this.loading.set(false);
            }
        });
    }

    onFilterChange(value: string): void {
        this.statusFilter.set(value);
        this.load();
    }

    canManage(): boolean {
        return this.auth.hasRole('SUPPLY_CHAIN_MANAGER');
    }

    acknowledge(escalation: Escalation): void {
        const actorName = this.actorNameService.actorName();
        if (!actorName) {
            this.actionError.set('Set your name in the top bar first — it records who took this action.');
            return;
        }
        this.actionError.set(null);
        this.busyId.set(escalation.escalationId);
        this.api.updateEscalationStatus(escalation.escalationId, 'ACKNOWLEDGED', actorName).subscribe({
            next: () => { this.busyId.set(null); this.load(); },
            error: (err) => { this.busyId.set(null); this.actionError.set(this.describeError(err)); }
        });
    }

    startResolve(escalation: Escalation): void {
        this.resolvingId.set(escalation.escalationId);
        this.resolutionDraft.set('');
    }

    cancelResolve(): void {
        this.resolvingId.set(null);
        this.resolutionDraft.set('');
    }

    confirmResolve(escalation: Escalation): void {
        const actorName = this.actorNameService.actorName();
        if (!actorName) {
            this.actionError.set('Set your name in the top bar first — it records who took this action.');
            return;
        }
        if (!this.resolutionDraft().trim()) {
            this.actionError.set('Add a short resolution note before resolving.');
            return;
        }
        this.actionError.set(null);
        this.busyId.set(escalation.escalationId);
        this.api.updateEscalationStatus(escalation.escalationId, 'RESOLVED', actorName, this.resolutionDraft()).subscribe({
            next: () => { this.busyId.set(null); this.resolvingId.set(null); this.load(); },
            error: (err) => { this.busyId.set(null); this.actionError.set(this.describeError(err)); }
        });
    }

    private describeError(err: any): string {
        return err?.status === 403
            ? 'Your role does not permit this action.'
            : `Action failed: ${err?.error?.message ?? err?.message ?? 'unknown error'}`;
    }

    severityBadgeClass(severity: string): string {
        switch (severity) {
            case 'CRITICAL': return 'bg-danger';
            case 'HIGH': return 'bg-warning text-dark';
            case 'MEDIUM': return 'bg-info text-dark';
            default: return 'bg-secondary';
        }
    }

    statusBadgeClass(status: string): string {
        switch (status) {
            case 'OPEN': return 'bg-danger';
            case 'ACKNOWLEDGED':
            case 'IN_PROGRESS': return 'bg-primary';
            case 'RESOLVED':
            case 'CLOSED': return 'bg-success';
            default: return 'bg-secondary';
        }
    }
}
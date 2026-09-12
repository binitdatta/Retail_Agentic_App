import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { ActorNameService } from '../../core/actor-name.service';
import { ReplenishmentOrder } from '../../models/replenishment-order.model';
import { DeliveryStatus } from '../../models/delivery-status.model';

@Component({
    selector: 'app-order-detail',
    standalone: true,
    imports: [CommonModule, RouterLink],
    templateUrl: './order-detail.component.html'
})
export class OrderDetailComponent implements OnInit {
    readonly order = signal<ReplenishmentOrder | null>(null);
    readonly shipments = signal<DeliveryStatus[]>([]);
    readonly loading = signal(true);
    readonly loadError = signal<string | null>(null);
    readonly actionError = signal<string | null>(null);
    readonly busy = signal(false);
    private orderId!: number;

    constructor(
        private readonly route: ActivatedRoute,
        private readonly api: ApiService,
        readonly auth: AuthService,
        readonly actorNameService: ActorNameService
    ) {}

    ngOnInit(): void {
        this.orderId = Number(this.route.snapshot.paramMap.get('id'));
        this.load();
    }

    load(): void {
        this.loading.set(true);
        this.loadError.set(null);
        this.api.getOrder(this.orderId).subscribe({
            next: (order) => {
                this.order.set(order);
                this.loading.set(false);
                // A RECOMMENDED order has no shipment yet — a 404 here is expected,
                // not an error, so it's handled separately from the order load.
                this.api.getDeliveryStatus(this.orderId).subscribe({
                    next: (shipments) => this.shipments.set(shipments),
                    error: () => this.shipments.set([])
                });
            },
            error: () => {
                this.loadError.set('Could not load this order. It may not exist, or you may not have a role permitted to view it.');
                this.loading.set(false);
            }
        });
    }

    canManage(): boolean {
        return this.auth.hasRole('SUPPLY_CHAIN_MANAGER');
    }

    approve(): void {
        this.runAction('APPROVED');
    }

    cancel(): void {
        if (!confirm('Cancel this order? This cannot be undone.')) return;
        this.runAction('CANCELLED');
    }

    private runAction(statusCode: string): void {
        const actorName = this.actorNameService.actorName();
        if (!actorName) {
            this.actionError.set('Set your name in the top bar first — it records who took this action.');
            return;
        }
        this.actionError.set(null);
        this.busy.set(true);
        this.api.updateOrderStatus(this.orderId, statusCode, actorName).subscribe({
            next: () => {
                this.busy.set(false);
                this.load();
            },
            error: (err) => {
                this.busy.set(false);
                this.actionError.set(
                    err?.status === 403
                        ? 'Your role does not permit this action.'
                        : `Action failed: ${err?.error?.message ?? err?.message ?? 'unknown error'}`
                );
            }
        });
    }

    statusBadgeClass(status: string): string {
        switch (status) {
            case 'DELIVERED': return 'bg-success';
            case 'RECOMMENDED': return 'bg-info text-dark';
            case 'APPROVED':
            case 'SENT_TO_SUPPLIER':
            case 'IN_TRANSIT': return 'bg-primary';
            case 'CANCELLED': return 'bg-secondary';
            case 'SHORTAGE_ESCALATED': return 'bg-danger';
            case 'DELAYED': return 'bg-warning text-dark';
            case 'EXCEPTION': return 'bg-danger';
            default: return 'bg-secondary';
        }
    }
}
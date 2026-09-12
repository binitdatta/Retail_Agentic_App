import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { ActorNameService } from '../../core/actor-name.service';
import { ReplenishmentOrder } from '../../models/replenishment-order.model';

const STATUS_OPTIONS = [
    '', 'RECOMMENDED', 'PENDING_APPROVAL', 'APPROVED', 'SENT_TO_SUPPLIER',
    'IN_TRANSIT', 'DELIVERED', 'CANCELLED', 'SHORTAGE_ESCALATED'
];

@Component({
    selector: 'app-orders-list',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './orders-list.component.html'
})
export class OrdersListComponent implements OnInit {
    readonly orders = signal<ReplenishmentOrder[]>([]);
    readonly loading = signal(true);
    readonly loadError = signal<string | null>(null);
    readonly actionError = signal<string | null>(null);
    readonly statusFilter = signal<string>('');
    readonly statusOptions = STATUS_OPTIONS;
    readonly busyOrderId = signal<number | null>(null);

    constructor(
        private readonly api: ApiService,
        private readonly router: Router,
        readonly auth: AuthService,
        readonly actorNameService: ActorNameService
    ) {}

    ngOnInit(): void {
        this.load();
    }

    load(): void {
        this.loading.set(true);
        this.loadError.set(null);
        this.api.getOrders(undefined, this.statusFilter() || undefined).subscribe({
            next: (orders) => {
                this.orders.set(orders);
                this.loading.set(false);
            },
            error: () => {
                this.loadError.set('Could not load orders. Is the API reachable and are you logged in with a valid role?');
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

    approve(order: ReplenishmentOrder, event: Event): void {
        event.stopPropagation();
        this.runAction(order.replenishmentOrderId, 'APPROVED');
    }

    cancel(order: ReplenishmentOrder, event: Event): void {
        event.stopPropagation();
        if (!confirm(`Cancel order ${order.orderNumber}? This cannot be undone.`)) return;
        this.runAction(order.replenishmentOrderId, 'CANCELLED');
    }

    private runAction(orderId: number, statusCode: string): void {
        const actorName = this.actorNameService.actorName();
        if (!actorName) {
            this.actionError.set('Set your name in the top bar first — it records who took this action.');
            return;
        }
        this.actionError.set(null);
        this.busyOrderId.set(orderId);
        this.api.updateOrderStatus(orderId, statusCode, actorName).subscribe({
            next: () => {
                this.busyOrderId.set(null);
                this.load();
            },
            error: (err) => {
                this.busyOrderId.set(null);
                this.actionError.set(
                    err?.status === 403
                        ? 'Your role does not permit this action.'
                        : `Action failed: ${err?.error?.message ?? err?.message ?? 'unknown error'}`
                );
            }
        });
    }

    open(order: ReplenishmentOrder): void {
        this.router.navigate(['/orders', order.replenishmentOrderId]);
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
            default: return 'bg-secondary';
        }
    }
}
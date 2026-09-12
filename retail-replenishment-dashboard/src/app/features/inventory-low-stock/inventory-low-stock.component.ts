import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../core/api.service';
import { LowStockItem } from '../../models/low-stock-item.model';

@Component({
    selector: 'app-inventory-low-stock',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './inventory-low-stock.component.html'
})
export class InventoryLowStockComponent implements OnInit {
    readonly items = signal<LowStockItem[]>([]);
    readonly loading = signal(true);
    readonly loadError = signal<string | null>(null);

    constructor(private readonly api: ApiService) {}

    ngOnInit(): void {
        this.api.getLowStock().subscribe({
            next: (items) => { this.items.set(items); this.loading.set(false); },
            error: () => {
                this.loadError.set('Could not load inventory. Is the API reachable and are you logged in with a valid role?');
                this.loading.set(false);
            }
        });
    }
}
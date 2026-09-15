import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { AgentRun } from '../models/agent-run.model';
import { DecisionLog } from '../models/decision-log.model';
import { LlmCallLog } from '../models/llm-call-log.model';
import { LlmCostRollup } from '../models/llm-cost-rollup.model';
import { LlmHttpTrace } from '../models/llm-http-trace.model';
import { ReplenishmentOrder } from '../models/replenishment-order.model';
import { DeliveryStatus } from '../models/delivery-status.model';
import { Escalation } from '../models/escalation.model';
import { LowStockItem } from '../models/low-stock-item.model';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly base = environment.apiBaseUrl;

  constructor(private readonly http: HttpClient) {}

  // -- agent runs / decisions / LLM calls (read-only dashboard) -----------

  getRecentAgentRuns(): Observable<AgentRun[]> {
    return this.http.get<AgentRun[]>(`${this.base}/agent-runs`);
  }

  getAgentRun(id: number): Observable<AgentRun> {
    return this.http.get<AgentRun>(`${this.base}/agent-runs/${id}`);
  }

  getDecisions(agentRunId: number): Observable<DecisionLog[]> {
    return this.http.get<DecisionLog[]>(`${this.base}/agent-runs/${agentRunId}/decisions`);
  }

  getLlmCalls(agentRunId: number): Observable<LlmCallLog[]> {
    return this.http.get<LlmCallLog[]>(`${this.base}/agent-runs/${agentRunId}/llm-calls`);
  }

  getLlmCost(agentRunId: number): Observable<LlmCostRollup> {
    return this.http.get<LlmCostRollup>(`${this.base}/agent-runs/${agentRunId}/llm-cost`);
  }

  getHttpTraces(agentRunId: number): Observable<LlmHttpTrace[]> {
    return this.http.get<LlmHttpTrace[]>(`${this.base}/agent-runs/${agentRunId}/http-traces`);
  }

  // Manager-only, and only works if the backend has app.demo-reset.enabled
  // set — see DemoResetService. Returns the post-reset low-stock list so
  // the caller can confirm it actually worked.
  resetDemoScenario(): Observable<LowStockItem[]> {
    return this.http.post<LowStockItem[]>(`${this.base}/admin/demo-reset`, {});
  }

  // -- global audit (across every run, not scoped to one) -------------------

  getAllDecisions(stageName?: string, storeId?: number, limit = 200): Observable<DecisionLog[]> {
    let params = new HttpParams().set('limit', limit);
    if (stageName) params = params.set('stageName', stageName);
    if (storeId != null) params = params.set('storeId', storeId);
    return this.http.get<DecisionLog[]>(`${this.base}/audit/decisions`, { params });
  }

  getAllLlmCalls(providerCode?: string, success?: boolean, limit = 200): Observable<LlmCallLog[]> {
    let params = new HttpParams().set('limit', limit);
    if (providerCode) params = params.set('providerCode', providerCode);
    if (success != null) params = params.set('success', success);
    return this.http.get<LlmCallLog[]>(`${this.base}/audit/llm-calls`, { params });
  }

  getGlobalLlmCost(): Observable<LlmCostRollup> {
    return this.http.get<LlmCostRollup>(`${this.base}/audit/llm-cost`);
  }

  // -- inventory (read-only) ------------------------------------------------

  getLowStock(storeId?: number): Observable<LowStockItem[]> {
    let params = new HttpParams();
    if (storeId != null) params = params.set('storeId', storeId);
    return this.http.get<LowStockItem[]>(`${this.base}/inventory/low-stock`, { params });
  }

  // -- replenishment orders (HITL: approve / cancel) -------------------------

  getOrders(storeId?: number, statusCode?: string): Observable<ReplenishmentOrder[]> {
    let params = new HttpParams();
    if (storeId != null) params = params.set('storeId', storeId);
    if (statusCode) params = params.set('statusCode', statusCode);
    return this.http.get<ReplenishmentOrder[]>(`${this.base}/replenishment-orders`, { params });
  }

  getOrder(id: number): Observable<ReplenishmentOrder> {
    return this.http.get<ReplenishmentOrder>(`${this.base}/replenishment-orders/${id}`);
  }

  updateOrderStatus(id: number, statusCode: string, actorName?: string): Observable<ReplenishmentOrder> {
    return this.http.patch<ReplenishmentOrder>(`${this.base}/replenishment-orders/${id}/status`, {
      statusCode,
      actorName: actorName || null
    });
  }

  getDeliveryStatus(orderId: number): Observable<DeliveryStatus[]> {
    return this.http.get<DeliveryStatus[]>(`${this.base}/replenishment-orders/${orderId}/delivery-status`);
  }

  // -- shortage escalations (HITL: acknowledge / resolve) ---------------------

  getEscalations(storeId?: number, statusCode?: string): Observable<Escalation[]> {
    let params = new HttpParams();
    if (storeId != null) params = params.set('storeId', storeId);
    if (statusCode) params = params.set('statusCode', statusCode);
    return this.http.get<Escalation[]>(`${this.base}/escalations`, { params });
  }

  updateEscalationStatus(
    id: number,
    statusCode: string,
    assignedTo?: string,
    resolutionNotes?: string
  ): Observable<Escalation> {
    return this.http.patch<Escalation>(`${this.base}/escalations/${id}/status`, {
      statusCode,
      assignedTo: assignedTo || null,
      resolutionNotes: resolutionNotes || null
    });
  }
}

export interface DecisionLog {
  decisionId: number;
  agentRunId: number;
  stageName: string;
  storeId: number | null;
  storeCode: string | null;
  productId: number | null;
  skuCode: string | null;
  inputSnapshot: unknown;
  outputDecision: unknown;
  llmModel: string | null;
  llmRationale: string | null;
  executedAt: string;
  durationMs: number | null;
}

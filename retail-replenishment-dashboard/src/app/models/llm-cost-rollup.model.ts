export interface LlmCostRollup {
  agentRunId: number;
  callCount: number;
  totalPromptTokens: number;
  totalCompletionTokens: number;
  totalEstimatedCostUsd: number;
  avgLatencyMs: number;
  errorCount: number;
}

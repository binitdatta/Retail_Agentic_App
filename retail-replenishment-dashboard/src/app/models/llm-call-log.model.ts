export interface LlmCallLog {
  llmCallId: number;
  agentRunId: number;
  decisionId: number | null;
  providerCode: string;
  modelName: string;
  promptTokens: number | null;
  completionTokens: number | null;
  totalTokens: number | null;
  estimatedCostUsd: number | null;
  latencyMs: number | null;
  success: boolean;
  requestedAt: string;
}

export interface AgentRun {
  agentRunId: number;
  runUuid: string;
  triggerType: string;
  triggerSource: string | null;
  storeId: number | null;
  storeCode: string | null;
  startedAt: string;
  completedAt: string | null;
  status: string;
  summary: string | null;
}

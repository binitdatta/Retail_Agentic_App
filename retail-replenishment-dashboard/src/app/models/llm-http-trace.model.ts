export interface LlmHttpTrace {
  httpTraceId: number;
  llmCallId: number;
  providerCode: string;
  modelName: string;
  httpMethod: string;
  requestUrl: string;
  requestHeaders: unknown;
  requestParams: unknown;
  requestBody: unknown;
  responseStatusCode: number | null;
  responseHeaders: unknown;
  responseBody: unknown;
  createdAt: string;
}

// API Design S2/S3: base path, bearer auth, and the standard error envelope.
// This is the ONLY place that talks to fetch() directly -- every feature module
// goes through here so the error-handling and auth-header logic isn't duplicated.

const BASE_URL = "/api/v1";

export interface ApiErrorBody {
  code: string;
  message: string;
  details?: Record<string, unknown>;
  traceId: string;
}

export class ApiError extends Error {
  code: string;
  status: number;
  details?: Record<string, unknown>;
  traceId: string;

  constructor(status: number, body: ApiErrorBody) {
    super(body.message);
    this.code = body.code;
    this.status = status;
    this.details = body.details;
    this.traceId = body.traceId;
  }
}

function getAccessToken(): string | null {
  return localStorage.getItem("nexus.accessToken");
}

export function setTokens(accessToken: string, refreshToken: string) {
  localStorage.setItem("nexus.accessToken", accessToken);
  localStorage.setItem("nexus.refreshToken", refreshToken);
}

export function clearTokens() {
  localStorage.removeItem("nexus.accessToken");
  localStorage.removeItem("nexus.refreshToken");
}

export function getRefreshToken(): string | null {
  return localStorage.getItem("nexus.refreshToken");
}

interface RequestOptions {
  method?: "GET" | "POST" | "PATCH" | "DELETE" | "PUT";
  body?: unknown;
  ifMatch?: string; // API Design S2.4: optimistic-lock version header
  skipAuth?: boolean;
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = { "Content-Type": "application/json" };

  if (!options.skipAuth) {
    const token = getAccessToken();
    if (token) headers["Authorization"] = `Bearer ${token}`;
  }
  if (options.ifMatch) headers["If-Match"] = `"${options.ifMatch}"`;

  const response = await fetch(`${BASE_URL}${path}`, {
    method: options.method ?? "GET",
    headers,
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
  });

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  const json = text ? JSON.parse(text) : undefined;

  if (!response.ok) {
    const errorBody: ApiErrorBody = json?.error ?? {
      code: "UNKNOWN_ERROR",
      message: `Request failed with status ${response.status}`,
      traceId: "",
    };
    throw new ApiError(response.status, errorBody);
  }

  return json as T;
}

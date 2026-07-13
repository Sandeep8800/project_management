import { apiRequest, setTokens, clearTokens, getRefreshToken } from "../../lib/apiClient";

export interface MeResponse {
  id: string;
  name: string;
  email: string;
  employeeId?: string;
  department?: string;
  defaultRole: string;
  status: string;
  platformAdmin: boolean;
  createdAt: string;
}

export interface MeProject {
  projectId: string;
  projectKey: string;
  projectName: string;
  role: string;
}

interface TokenResponse {
  accessToken: string;
  refreshToken: string;
}

export async function login(email: string, password: string): Promise<void> {
  const tokens = await apiRequest<TokenResponse>("/auth/login", {
    method: "POST",
    body: { email, password },
    skipAuth: true,
  });
  setTokens(tokens.accessToken, tokens.refreshToken);
}

export async function logout(): Promise<void> {
  const refreshToken = getRefreshToken();
  try {
    if (refreshToken) {
      await apiRequest("/auth/logout", { method: "POST", body: { refreshToken } });
    }
  } finally {
    clearTokens();
  }
}

export function fetchMe(): Promise<MeResponse> {
  return apiRequest<MeResponse>("/me");
}

export function fetchMyProjects(): Promise<MeProject[]> {
  return apiRequest<MeProject[]>("/me/projects");
}

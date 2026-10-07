import axios from 'axios';

export const AUTH_STORAGE_KEY = 'rentaltrack.auth';

export interface AuthSession {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  memberId: number;
  email: string;
  name: string;
  role: 'USER' | 'OPERATOR';
}

export async function login(
  email: string,
  password: string,
): Promise<AuthSession> {
  const response = await axios.post<AuthSession>('/api/v1/auth/login', {
    email,
    password,
  });

  return response.data;
}

export function loadSession(): AuthSession | null {
  const raw = localStorage.getItem(AUTH_STORAGE_KEY);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw) as AuthSession;
  } catch {
    localStorage.removeItem(AUTH_STORAGE_KEY);
    return null;
  }
}

export function saveSession(session: AuthSession) {
  localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session));
}

export function clearSession() {
  localStorage.removeItem(AUTH_STORAGE_KEY);
}

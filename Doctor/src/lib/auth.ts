export interface AuthUser {
  userId: number;
  email: string;
  firstName: string;
  lastName: string;
  hospitalId: number;
  roles: string[];
  doctorId?: number;
}

const TOKEN_KEY = "hc_access_token";
const USER_KEY = "hc_auth_user";

export function getAccessToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(TOKEN_KEY);
}

export function setAccessToken(token: string): void {
  if (typeof window === "undefined") return;
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearAccessToken(): void {
  if (typeof window === "undefined") return;
  localStorage.removeItem(TOKEN_KEY);
}

export function getAuthUser(): AuthUser | null {
  if (typeof window === "undefined") return null;
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

export function normalizeRole(role: string): string {
  const clean = role.trim().toUpperCase();
  return clean.startsWith("ROLE_") ? clean : `ROLE_${clean}`;
}

export function setAuthUser(user: AuthUser): void {
  if (typeof window === "undefined") return;
  const normalizedUser: AuthUser = {
    ...user,
    roles: (user.roles || []).map(normalizeRole),
  };
  localStorage.setItem(USER_KEY, JSON.stringify(normalizedUser));
}

export function clearAuthUser(): void {
  if (typeof window === "undefined") return;
  localStorage.removeItem(USER_KEY);
}

export function isAuthenticated(): boolean {
  return Boolean(getAccessToken());
}

export function logout(): void {
  const isPatientUser =
    typeof window !== "undefined" &&
    (window.location.pathname.startsWith("/patient") || isPatient());
  clearAccessToken();
  clearAuthUser();
  if (typeof window !== "undefined") {
    window.location.href = isPatientUser ? "/patient/login" : "/login";
  }
}

export function logoutPatient(): void {
  clearAccessToken();
  clearAuthUser();
  if (typeof window !== "undefined") {
    window.location.href = "/patient/login";
  }
}

export function hasRole(role: string): boolean {
  const user = getAuthUser();
  if (!user?.roles) return false;
  const target = normalizeRole(role);
  return user.roles.some((r) => normalizeRole(r) === target);
}

export function isReceptionist(): boolean {
  return hasRole("ROLE_RECEPTIONIST");
}

export function isDoctor(): boolean {
  return hasRole("ROLE_DOCTOR");
}

export function isOnlyReceptionist(): boolean {
  return isReceptionist() && !isDoctor();
}

export function isPatient(): boolean {
  return hasRole("ROLE_PATIENT");
}

export function isStaff(): boolean {
  const user = getAuthUser();
  if (!user?.roles || user.roles.length === 0) return false;
  return user.roles.some((r) => {
    const role = normalizeRole(r);
    return role !== "ROLE_PATIENT";
  });
}



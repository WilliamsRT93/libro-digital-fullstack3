// Helpers de sesion — SSR-safe para Next.js.
// sessionStorage solo existe en el navegador; los guards typeof window
// evitan errores durante el pre-render del servidor.

const TOKEN_KEY = "jwt";
const ROLES_KEY = "roles";
const USER_KEY  = "username";

function isBrowser() { return typeof window !== "undefined"; }

export function saveSession({ token, roles, username }) {
  if (!isBrowser()) return;
  sessionStorage.setItem(TOKEN_KEY, token);
  sessionStorage.setItem(ROLES_KEY, JSON.stringify(roles || []));
  sessionStorage.setItem(USER_KEY, username || "");
}

export function clearSession() {
  if (!isBrowser()) return;
  sessionStorage.removeItem(TOKEN_KEY);
  sessionStorage.removeItem(ROLES_KEY);
  sessionStorage.removeItem(USER_KEY);
}

export function getToken() {
  return isBrowser() ? sessionStorage.getItem(TOKEN_KEY) : null;
}

export function getRoles() {
  if (!isBrowser()) return [];
  try { return JSON.parse(sessionStorage.getItem(ROLES_KEY) || "[]"); }
  catch { return []; }
}

export function getUsername() {
  return isBrowser() ? (sessionStorage.getItem(USER_KEY) || "") : "";
}

export function isAuthenticated() {
  return !!getToken();
}

export function hasRole(role) {
  return getRoles().includes(role);
}

export function hasAnyRole(...roles) {
  const userRoles = getRoles();
  return roles.some((r) => userRoles.includes(r));
}

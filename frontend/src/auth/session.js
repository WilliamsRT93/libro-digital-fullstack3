// Helpers de sesion. Centraliza la lectura/escritura del JWT y los roles.
// Almacenamiento en sessionStorage: el token desaparece al cerrar la pestania.

const TOKEN_KEY = "jwt";
const ROLES_KEY = "roles";
const USER_KEY = "username";

export function saveSession({ token, roles, username }) {
  sessionStorage.setItem(TOKEN_KEY, token);
  sessionStorage.setItem(ROLES_KEY, JSON.stringify(roles || []));
  sessionStorage.setItem(USER_KEY, username || "");
}

export function clearSession() {
  sessionStorage.removeItem(TOKEN_KEY);
  sessionStorage.removeItem(ROLES_KEY);
  sessionStorage.removeItem(USER_KEY);
}

export function getToken() {
  return sessionStorage.getItem(TOKEN_KEY);
}

export function getRoles() {
  try {
    return JSON.parse(sessionStorage.getItem(ROLES_KEY) || "[]");
  } catch {
    return [];
  }
}

export function getUsername() {
  return sessionStorage.getItem(USER_KEY) || "";
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

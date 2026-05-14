import React from "react";
import { Navigate } from "react-router-dom";
import { isAuthenticated } from "../auth/session.js";

/**
 * Wrapper que protege rutas privadas.
 * Si el usuario no esta autenticado, lo redirige a la pagina de login.
 */
export default function ProtectedRoute({ children }) {
  if (!isAuthenticated()) {
    return <Navigate to="/" replace />;
  }
  return children;
}

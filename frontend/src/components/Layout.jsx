import React from "react";
import { useNavigate, NavLink } from "react-router-dom";
import { getUsername, getRoles, clearSession } from "../auth/session.js";

/**
 * Layout principal del area autenticada.
 * Provee el header con identidad del usuario, navegacion y logout.
 */
export default function Layout({ children }) {
  const navigate = useNavigate();
  const username = getUsername();
  const roles = getRoles();

  const onLogout = () => {
    clearSession();
    navigate("/", { replace: true });
  };

  const navStyle = ({ isActive }) => ({
    padding: "8px 14px",
    borderRadius: 6,
    color: isActive ? "#fff" : "rgba(255,255,255,0.85)",
    background: isActive ? "rgba(255,255,255,0.18)" : "transparent",
    textDecoration: "none",
    fontSize: 14,
    fontWeight: 500
  });

  return (
    <div style={{ minHeight: "100vh", display: "flex", flexDirection: "column" }}>
      <header
        style={{
          background: "var(--color-primary)",
          color: "#fff",
          padding: "14px 32px",
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          boxShadow: "var(--shadow-md)"
        }}
      >
        <div style={{ display: "flex", alignItems: "center", gap: 24 }}>
          <h1 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Libro de Clases Digital</h1>
          <nav style={{ display: "flex", gap: 8 }}>
            <NavLink to="/dashboard" end style={navStyle}>Inicio</NavLink>
            <NavLink to="/dashboard/asistencia" style={navStyle}>Asistencia</NavLink>
            <NavLink to="/dashboard/notas" style={navStyle}>Notas</NavLink>
            <NavLink to="/dashboard/reportes" style={navStyle}>Reportes</NavLink>
            {roles.includes("ADMIN") && (
              <NavLink to="/dashboard/usuarios" style={navStyle}>Usuarios</NavLink>
            )}
          </nav>
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
          <div style={{ textAlign: "right", lineHeight: 1.2 }}>
            <div style={{ fontWeight: 600, fontSize: 14 }}>{username}</div>
            <div style={{ fontSize: 11, opacity: 0.85 }}>{roles.join(" | ")}</div>
          </div>
          <button onClick={onLogout} className="outline" style={{ background: "rgba(255,255,255,0.1)", color: "#fff", border: "1px solid rgba(255,255,255,0.5)" }}>
            Cerrar sesion
          </button>
        </div>
      </header>

      <main style={{ flex: 1, padding: "32px", maxWidth: 1280, width: "100%", margin: "0 auto" }}>
        {children}
      </main>

      <footer style={{ textAlign: "center", padding: 16, fontSize: 12, color: "var(--color-text-muted)" }}>
        Plataforma Libro de Clases Digital - Microservicios
      </footer>
    </div>
  );
}

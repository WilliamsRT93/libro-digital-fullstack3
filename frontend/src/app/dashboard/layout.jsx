"use client";

import { useEffect, useState } from "react";
import { useRouter, usePathname } from "next/navigation";
import Link from "next/link";
import { isAuthenticated, getUsername, getRoles, clearSession } from "../../auth/session.js";

export default function DashboardLayout({ children }) {
  const router   = useRouter();
  const pathname = usePathname();
  const [ready, setReady] = useState(false);

  useEffect(() => {
    if (!isAuthenticated()) {
      router.replace("/");
    } else {
      setReady(true);
    }
  }, [router]);

  if (!ready) return null;

  const username = getUsername();
  const roles    = getRoles();

  const onLogout = () => {
    clearSession();
    router.replace("/");
  };

  const navLink = (href, label) => {
    const active = pathname === href;
    return (
      <Link key={href} href={href} style={{ padding: "8px 14px", borderRadius: 6, color: active ? "#fff" : "rgba(255,255,255,0.85)", background: active ? "rgba(255,255,255,0.18)" : "transparent", textDecoration: "none", fontSize: 14, fontWeight: 500 }}>
        {label}
      </Link>
    );
  };

  return (
    <div style={{ minHeight: "100vh", display: "flex", flexDirection: "column" }}>
      <header style={{ background: "var(--color-primary)", color: "#fff", padding: "14px 32px", display: "flex", alignItems: "center", justifyContent: "space-between", boxShadow: "var(--shadow-md)" }}>
        <div style={{ display: "flex", alignItems: "center", gap: 24 }}>
          <h1 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Libro de Clases Digital</h1>
          <nav style={{ display: "flex", gap: 8 }}>
            {navLink("/dashboard", "Inicio")}
            {navLink("/dashboard/asistencia", "Asistencia")}
            {navLink("/dashboard/notas", "Notas")}
            {navLink("/dashboard/reportes", "Reportes")}
            {roles.includes("ADMIN") && navLink("/dashboard/usuarios", "Usuarios")}
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

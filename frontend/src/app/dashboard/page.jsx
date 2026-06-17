"use client";

import Link from "next/link";
import { getUsername, getRoles } from "../../auth/session.js";

export default function HomePage() {
  const username = getUsername();
  const roles    = getRoles();

  const tiles = [
    { href: "/dashboard/asistencia", title: "Registrar Asistencia", desc: "Marca presencia, ausencia o atrasos del curso.", roles: ["DOCENTE", "ADMIN"] },
    { href: "/dashboard/notas",      title: "Ingresar Notas",        desc: "Captura calificaciones por asignatura.",      roles: ["DOCENTE", "ADMIN"] },
    { href: "/dashboard/reportes",   title: "Reportes PDF",          desc: "Descarga el reporte academico de un alumno.", roles: ["DOCENTE", "ADMIN", "APODERADO"] },
    { href: "/dashboard/usuarios",   title: "Gestion de Usuarios",   desc: "Crear docentes y apoderados, asignar roles.", roles: ["ADMIN"] },
  ];

  const visibles = tiles.filter((t) => t.roles.some((r) => roles.includes(r)));

  return (
    <div>
      <h2 style={{ marginTop: 0 }}>Bienvenido, {username}</h2>
      <p style={{ color: "var(--color-text-muted)", marginTop: -8 }}>Selecciona una accion para comenzar tu jornada.</p>

      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(260px, 1fr))", gap: 16, marginTop: 24 }}>
        {visibles.map((t) => (
          <Link key={t.href} href={t.href} style={{ textDecoration: "none", color: "inherit" }}>
            <div className="card" style={{ transition: "transform 0.15s ease, box-shadow 0.15s ease", height: "100%", cursor: "pointer" }}
                 onMouseEnter={(e) => { e.currentTarget.style.transform = "translateY(-2px)"; e.currentTarget.style.boxShadow = "var(--shadow-md)"; }}
                 onMouseLeave={(e) => { e.currentTarget.style.transform = "none"; e.currentTarget.style.boxShadow = "var(--shadow-sm)"; }}>
              <h3 className="card-title">{t.title}</h3>
              <p style={{ color: "var(--color-text-muted)", margin: 0, fontSize: 14 }}>{t.desc}</p>
            </div>
          </Link>
        ))}
      </div>
    </div>
  );
}

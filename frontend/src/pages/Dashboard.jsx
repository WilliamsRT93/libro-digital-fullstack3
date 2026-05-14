import React from "react";
import { Routes, Route, Navigate } from "react-router-dom";
import Layout from "../components/Layout.jsx";
import Home from "./Home.jsx";
import Asistencia from "./Asistencia.jsx";
import Notas from "./Notas.jsx";
import Reportes from "./Reportes.jsx";
import Usuarios from "./Usuarios.jsx";

/**
 * Dashboard principal con sub-rutas para cada modulo del sistema.
 */
export default function Dashboard() {
  return (
    <Layout>
      <Routes>
        <Route index element={<Home />} />
        <Route path="asistencia" element={<Asistencia />} />
        <Route path="notas" element={<Notas />} />
        <Route path="reportes" element={<Reportes />} />
        <Route path="usuarios" element={<Usuarios />} />
        <Route path="*" element={<Navigate to="" replace />} />
      </Routes>
    </Layout>
  );
}

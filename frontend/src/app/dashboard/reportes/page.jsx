"use client";

import { useState } from "react";
import client from "../../../api/client.js";

export default function ReportesPage() {
  const [alumnoId, setAlumnoId] = useState("");
  const [loading, setLoading]   = useState(false);
  const [feedback, setFeedback] = useState(null);

  const descargar = async (e) => {
    e.preventDefault();
    setFeedback(null);
    if (!alumnoId || Number(alumnoId) <= 0) { setFeedback({ type: "error", msg: "Indica un ID de alumno valido." }); return; }
    setLoading(true);
    try {
      const res = await client.get(`/notas/reporte-pdf?alumnoId=${alumnoId}`, { responseType: "blob" });
      const blob = new Blob([res.data], { type: "application/pdf" });
      const url  = URL.createObjectURL(blob);
      window.open(url, "_blank");
      setTimeout(() => URL.revokeObjectURL(url), 5000);
      setFeedback({ type: "success", msg: "Reporte generado correctamente." });
    } catch (ex) {
      const status = ex.response?.status;
      setFeedback({ type: "error", msg: status === 404 ? "No se encontraron notas para ese alumno." : "Error al generar el reporte PDF." });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h2 style={{ marginTop: 0 }}>Reportes Academicos</h2>
      <p style={{ color: "var(--color-text-muted)" }}>Genera el reporte PDF con todas las notas del alumno. Una copia se almacena automaticamente en S3.</p>

      {feedback && <div className={`alert ${feedback.type}`}>{feedback.msg}</div>}

      <div className="card" style={{ maxWidth: 480 }}>
        <form onSubmit={descargar}>
          <label htmlFor="rep-alumno">ID Alumno</label>
          <input id="rep-alumno" type="number" min="1" value={alumnoId} onChange={(e) => setAlumnoId(e.target.value)} required />
          <div style={{ marginTop: 16 }}>
            <button type="submit" disabled={loading}>{loading ? <><span className="spinner"></span> Generando...</> : "Descargar PDF"}</button>
          </div>
        </form>
      </div>
    </div>
  );
}

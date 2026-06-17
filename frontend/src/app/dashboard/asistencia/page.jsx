"use client";

import { useState } from "react";
import client from "../../../api/client.js";
import { hasAnyRole } from "../../../auth/session.js";

export default function AsistenciaPage() {
  const allowed = hasAnyRole("DOCENTE", "ADMIN");

  const [form, setForm] = useState({
    alumnoId: "", cursoId: "",
    fecha: new Date().toISOString().slice(0, 10),
    estado: "PRESENTE", observacion: "",
  });
  const [feedback, setFeedback] = useState(null);
  const [loading, setLoading]   = useState(false);
  const [historial, setHistorial] = useState([]);

  if (!allowed) return <div className="alert error">No tienes permisos para acceder a este modulo.</div>;

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const onSubmit = async (e) => {
    e.preventDefault();
    setFeedback(null);
    if (!form.alumnoId || !form.cursoId) { setFeedback({ type: "error", msg: "Debes indicar alumno y curso." }); return; }
    setLoading(true);
    try {
      const payload = { alumnoId: Number(form.alumnoId), cursoId: Number(form.cursoId), fecha: form.fecha, estado: form.estado, observacion: form.observacion || null };
      const { data } = await client.post("/asistencias", payload);
      setFeedback({ type: "success", msg: "Asistencia registrada correctamente." });
      setHistorial([{ ...payload, id: data.id || Date.now() }, ...historial]);
      setForm({ ...form, alumnoId: "", observacion: "" });
    } catch (ex) {
      const msg = ex.response?.data?.message || "Error al registrar la asistencia.";
      setFeedback({ type: "error", msg });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h2 style={{ marginTop: 0 }}>Registrar Asistencia</h2>
      <p style={{ color: "var(--color-text-muted)" }}>Las inasistencias generan automaticamente una notificacion al apoderado via Kafka.</p>

      {feedback && <div className={`alert ${feedback.type}`}>{feedback.msg}</div>}

      <div className="card" style={{ marginBottom: 24 }}>
        <form onSubmit={onSubmit}>
          <div className="row">
            <div><label htmlFor="alumnoId">ID Alumno</label><input id="alumnoId" name="alumnoId" type="number" min="1" value={form.alumnoId} onChange={onChange} required /></div>
            <div><label htmlFor="cursoId">ID Curso</label><input id="cursoId" name="cursoId" type="number" min="1" value={form.cursoId} onChange={onChange} required /></div>
            <div><label htmlFor="fecha">Fecha</label><input id="fecha" name="fecha" type="date" value={form.fecha} onChange={onChange} required /></div>
            <div>
              <label htmlFor="estado">Estado</label>
              <select id="estado" name="estado" value={form.estado} onChange={onChange}>
                <option value="PRESENTE">Presente</option>
                <option value="AUSENTE">Ausente</option>
                <option value="ATRASO">Atraso</option>
                <option value="JUSTIFICADO">Justificado</option>
              </select>
            </div>
          </div>
          <div style={{ marginTop: 14 }}>
            <label htmlFor="observacion">Observacion (opcional)</label>
            <input id="observacion" name="observacion" type="text" maxLength={250} value={form.observacion} onChange={onChange} placeholder="Comentario" />
          </div>
          <div style={{ marginTop: 18, display: "flex", gap: 8 }}>
            <button type="submit" disabled={loading}>{loading ? <><span className="spinner"></span> Registrando...</> : "Registrar"}</button>
            <button type="button" className="outline" onClick={() => setForm({ alumnoId: "", cursoId: "", fecha: new Date().toISOString().slice(0, 10), estado: "PRESENTE", observacion: "" })}>Limpiar</button>
          </div>
        </form>
      </div>

      {historial.length > 0 && (
        <div>
          <h3 style={{ fontSize: 16 }}>Historial reciente (sesion actual)</h3>
          <table>
            <thead><tr><th>Alumno</th><th>Curso</th><th>Fecha</th><th>Estado</th><th>Observacion</th></tr></thead>
            <tbody>
              {historial.map((h) => (
                <tr key={h.id}>
                  <td>{h.alumnoId}</td><td>{h.cursoId}</td><td>{h.fecha}</td>
                  <td><span className={`badge ${h.estado.toLowerCase()}`}>{h.estado}</span></td>
                  <td>{h.observacion || "-"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

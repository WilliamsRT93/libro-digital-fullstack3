"use client";

import { useState } from "react";
import client from "../../../api/client.js";
import { hasAnyRole } from "../../../auth/session.js";

export default function NotasPage() {
  const allowed = hasAnyRole("DOCENTE", "ADMIN");

  const [form, setForm] = useState({ alumnoId: "", cursoId: "", asignatura: "", valor: "", descripcion: "" });
  const [feedback, setFeedback]     = useState(null);
  const [loading, setLoading]       = useState(false);
  const [registradas, setRegistradas] = useState([]);

  if (!allowed) return <div className="alert error">No tienes permisos para acceder a este modulo.</div>;

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const onSubmit = async (e) => {
    e.preventDefault();
    setFeedback(null);
    const valor = parseFloat(form.valor);
    if (isNaN(valor) || valor < 1.0 || valor > 7.0) { setFeedback({ type: "error", msg: "La nota debe estar entre 1.0 y 7.0" }); return; }
    if (!form.alumnoId || !form.cursoId || !form.asignatura) { setFeedback({ type: "error", msg: "Completa los campos obligatorios." }); return; }
    setLoading(true);
    try {
      const payload = { alumnoId: Number(form.alumnoId), cursoId: Number(form.cursoId), asignatura: form.asignatura, valor, descripcion: form.descripcion || null };
      const { data } = await client.post("/notas", payload);
      setFeedback({ type: "success", msg: `Nota ${valor} registrada correctamente.` });
      setRegistradas([{ ...payload, id: data.id || Date.now() }, ...registradas]);
      setForm({ ...form, valor: "", descripcion: "" });
    } catch (ex) {
      const status = ex.response?.status;
      setFeedback({ type: "error", msg: status === 403 ? "No tienes permisos para registrar notas." : "Error al registrar la nota." });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h2 style={{ marginTop: 0 }}>Ingresar Notas</h2>
      <p style={{ color: "var(--color-text-muted)" }}>Escala de evaluacion 1.0 a 7.0.</p>

      {feedback && <div className={`alert ${feedback.type}`}>{feedback.msg}</div>}

      <div className="card" style={{ marginBottom: 24 }}>
        <form onSubmit={onSubmit}>
          <div className="row">
            <div><label>ID Alumno</label><input name="alumnoId" type="number" min="1" value={form.alumnoId} onChange={onChange} required /></div>
            <div><label>ID Curso</label><input name="cursoId" type="number" min="1" value={form.cursoId} onChange={onChange} required /></div>
            <div><label>Asignatura</label><input name="asignatura" type="text" maxLength={80} value={form.asignatura} onChange={onChange} placeholder="Matematica" required /></div>
            <div><label>Nota (1.0 - 7.0)</label><input name="valor" type="number" step="0.1" min="1.0" max="7.0" value={form.valor} onChange={onChange} required /></div>
          </div>
          <div style={{ marginTop: 14 }}>
            <label>Descripcion (opcional)</label>
            <input name="descripcion" type="text" maxLength={200} value={form.descripcion} onChange={onChange} placeholder="Prueba unidad 1" />
          </div>
          <div style={{ marginTop: 18 }}>
            <button type="submit" disabled={loading}>{loading ? <><span className="spinner"></span> Registrando...</> : "Registrar nota"}</button>
          </div>
        </form>
      </div>

      {registradas.length > 0 && (
        <div>
          <h3 style={{ fontSize: 16 }}>Notas registradas en esta sesion</h3>
          <table>
            <thead><tr><th>Alumno</th><th>Curso</th><th>Asignatura</th><th>Nota</th><th>Descripcion</th></tr></thead>
            <tbody>
              {registradas.map((n) => (
                <tr key={n.id}>
                  <td>{n.alumnoId}</td><td>{n.cursoId}</td><td>{n.asignatura}</td>
                  <td><strong style={{ color: n.valor >= 4 ? "var(--color-success)" : "var(--color-error)" }}>{n.valor.toFixed(1)}</strong></td>
                  <td>{n.descripcion || "-"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

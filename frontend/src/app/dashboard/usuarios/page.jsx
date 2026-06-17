"use client";

import { useEffect, useState } from "react";
import client from "../../../api/client.js";
import { hasRole } from "../../../auth/session.js";

export default function UsuariosPage() {
  const isAdmin = hasRole("ADMIN");

  const [usuarios, setUsuarios] = useState([]);
  const [feedback, setFeedback] = useState(null);
  const [loading, setLoading]   = useState(false);
  const [form, setForm] = useState({ username: "", password: "", fullName: "", rol: "DOCENTE" });

  useEffect(() => { if (isAdmin) cargar(); }, [isAdmin]);

  if (!isAdmin) return <div className="alert error">Solo el rol ADMIN puede gestionar usuarios.</div>;

  const cargar = async () => {
    try {
      const { data } = await client.get("/admin/users");
      setUsuarios(data || []);
    } catch { setFeedback({ type: "error", msg: "No se pudo obtener la lista de usuarios." }); }
  };

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const onSubmit = async (e) => {
    e.preventDefault();
    setFeedback(null);
    if (form.username.length < 3 || form.password.length < 6 || form.fullName.length < 3) {
      setFeedback({ type: "error", msg: "Completa todos los campos (password >= 6, otros >= 3)." }); return;
    }
    setLoading(true);
    try {
      await client.post("/admin/users", { username: form.username, password: form.password, fullName: form.fullName, roles: [form.rol] });
      setFeedback({ type: "success", msg: `Usuario ${form.username} creado con rol ${form.rol}.` });
      setForm({ username: "", password: "", fullName: "", rol: "DOCENTE" });
      cargar();
    } catch (ex) {
      setFeedback({ type: "error", msg: ex.response?.status === 409 ? "Ese usuario ya existe." : "Error al crear el usuario." });
    } finally {
      setLoading(false);
    }
  };

  const eliminar = async (id, username) => {
    if (!window.confirm(`Eliminar usuario ${username}?`)) return;
    try {
      await client.delete(`/admin/users/${id}`);
      setFeedback({ type: "success", msg: `Usuario ${username} eliminado.` });
      cargar();
    } catch { setFeedback({ type: "error", msg: "No se pudo eliminar el usuario." }); }
  };

  return (
    <div>
      <h2 style={{ marginTop: 0 }}>Gestion de Usuarios</h2>
      <p style={{ color: "var(--color-text-muted)" }}>Solo el administrador puede crear docentes y apoderados.</p>

      {feedback && <div className={`alert ${feedback.type}`}>{feedback.msg}</div>}

      <div className="card" style={{ marginBottom: 24 }}>
        <h3 className="card-title">Crear nuevo usuario</h3>
        <form onSubmit={onSubmit}>
          <div className="row">
            <div><label>Usuario</label><input name="username" value={form.username} onChange={onChange} required minLength={3} maxLength={80} /></div>
            <div><label>Nombre completo</label><input name="fullName" value={form.fullName} onChange={onChange} required minLength={3} maxLength={120} /></div>
            <div><label>Contrasenia (&gt;= 6)</label><input type="password" name="password" value={form.password} onChange={onChange} required minLength={6} /></div>
            <div>
              <label>Rol</label>
              <select name="rol" value={form.rol} onChange={onChange}>
                <option value="DOCENTE">DOCENTE</option>
                <option value="APODERADO">APODERADO</option>
                <option value="ADMIN">ADMIN</option>
              </select>
            </div>
          </div>
          <div style={{ marginTop: 18 }}>
            <button type="submit" disabled={loading}>{loading ? <><span className="spinner"></span> Creando...</> : "Crear usuario"}</button>
          </div>
        </form>
      </div>

      <h3 style={{ fontSize: 16 }}>Usuarios registrados ({usuarios.length})</h3>
      <table>
        <thead><tr><th>ID</th><th>Usuario</th><th>Nombre</th><th>Roles</th><th>Estado</th><th>Acciones</th></tr></thead>
        <tbody>
          {usuarios.map((u) => (
            <tr key={u.id}>
              <td>{u.id}</td>
              <td><strong>{u.username}</strong></td>
              <td>{u.fullName}</td>
              <td>{(u.roles || []).map(r => <span key={r} className="badge presente" style={{ marginRight: 4 }}>{r}</span>)}</td>
              <td>{u.enabled ? <span className="badge presente">activo</span> : <span className="badge ausente">deshabilitado</span>}</td>
              <td><button onClick={() => eliminar(u.id, u.username)} style={{ background: "var(--color-error)" }}>Eliminar</button></td>
            </tr>
          ))}
          {usuarios.length === 0 && <tr><td colSpan={6} style={{ textAlign: "center", color: "var(--color-text-muted)" }}>Sin usuarios.</td></tr>}
        </tbody>
      </table>
    </div>
  );
}

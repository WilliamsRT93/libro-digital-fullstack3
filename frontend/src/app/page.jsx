"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import client from "../api/client.js";
import { saveSession, isAuthenticated } from "../auth/session.js";

export default function LoginPage() {
  const router = useRouter();
  const [form, setForm] = useState({ username: "", password: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (isAuthenticated()) router.replace("/dashboard");
  }, [router]);

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const validate = () => {
    if (form.username.trim().length < 3) return "El usuario debe tener al menos 3 caracteres";
    if (form.password.length < 6) return "La contrasenia debe tener al menos 6 caracteres";
    return null;
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    setError("");
    const validationError = validate();
    if (validationError) { setError(validationError); return; }
    setLoading(true);
    try {
      const { data } = await client.post("/login", form);
      saveSession({ token: data.token, roles: data.roles, username: data.username });
      router.replace("/dashboard");
    } catch (ex) {
      const status = ex.response && ex.response.status;
      if (status === 401) setError("Credenciales invalidas. Verifica tu usuario y contrasenia.");
      else if (status === 503) setError("El API Gateway no esta disponible. Verifica que api-gateway este UP.");
      else if (status === 500) setError("Error interno en el servicio. Revisa los logs: docker compose logs ms-auth bff.");
      else if (status >= 500) setError("El servicio de autenticacion no esta disponible. Intenta nuevamente.");
      else if (!ex.response) setError("No se pudo conectar al BFF (puerto 8080). Verifica que el contenedor 'bff' este corriendo.");
      else setError(`No se pudo completar el inicio de sesion (HTTP ${status}).`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ minHeight: "100vh", display: "flex", alignItems: "center", justifyContent: "center", background: "linear-gradient(135deg, #1e40af 0%, #0f766e 100%)", padding: 16 }}>
      <div className="card" style={{ width: "100%", maxWidth: 420, boxShadow: "var(--shadow-md)" }}>
        <div style={{ textAlign: "center", marginBottom: 24 }}>
          <div style={{ width: 56, height: 56, borderRadius: 12, background: "var(--color-primary)", color: "#fff", display: "inline-flex", alignItems: "center", justifyContent: "center", fontSize: 24, fontWeight: 700, marginBottom: 12 }}>LC</div>
          <h1 style={{ margin: 0, fontSize: 22 }}>Libro de Clases Digital</h1>
          <p style={{ color: "var(--color-text-muted)", fontSize: 14, margin: "6px 0 0" }}>Ingresa con tus credenciales institucionales</p>
        </div>

        {error && <div className="alert error">{error}</div>}

        <form onSubmit={onSubmit} noValidate>
          <div style={{ marginBottom: 14 }}>
            <label htmlFor="username">Usuario</label>
            <input id="username" name="username" type="text" autoComplete="username" autoFocus value={form.username} onChange={onChange} placeholder="docente1 / admin1 / apoderado1" disabled={loading} />
          </div>
          <div style={{ marginBottom: 20 }}>
            <label htmlFor="password">Contrasenia</label>
            <input id="password" name="password" type="password" autoComplete="current-password" value={form.password} onChange={onChange} placeholder="********" disabled={loading} />
          </div>
          <button type="submit" disabled={loading} style={{ width: "100%" }}>
            {loading ? <><span className="spinner"></span> Ingresando...</> : "Ingresar"}
          </button>
        </form>

        <div style={{ marginTop: 16, padding: 12, background: "#f1f5f9", borderRadius: 8, fontSize: 12, color: "#475569" }}>
          <strong>Usuarios demo:</strong>
          <div>admin1 / Admin123!</div>
          <div>docente1 / Docente123!</div>
          <div>apoderado1 / Apoderado123!</div>
        </div>
      </div>
    </div>
  );
}

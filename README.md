# Plataforma Libro de Clases Digital

Sistema de microservicios para gestión académica escolar: autenticación, notas, asistencia,
mensajería y reportes PDF. Arquitectura event-driven con Kafka, segura con JWT y observable
con ELK + Prometheus/Grafana.

## Requisitos de entorno local

Antes de clonar y ejecutar el proyecto, asegúrate de tener instalado:

| Herramienta | Versión mínima | Notas |
|---|---|---|
| JDK | 21 (LTS) | `javac -version` debe mostrar `21.x`. Recomendado: Eclipse Temurin 21 |
| Maven | 3.8+ | Usar el Maven del sistema (`/usr/bin/mvn`), no el bundled de IntelliJ |
| Docker Desktop | 24+ | BuildKit activado por defecto en 24+ |
| Node.js | 18+ | Solo necesario si vas a desarrollar el frontend fuera de Docker |
| Git | 2.x | SSH configurado para el repositorio |

> **JDK 21 en Ubuntu/Debian:** `sudo apt install openjdk-21-jdk` y luego
> `sudo update-alternatives --config javac` para seleccionarlo como predeterminado.
> Si `javac -version` muestra JDK 17, las compilaciones con Lombok fallarán.

Archivos que debes crear localmente (no se incluyen en el repositorio):

```bash
cp .env.example .env          # variables de entorno del stack
./scripts/reset-keys.sh       # genera claves RSA en ms-auth/src/main/resources/keys/
```

## Stack Tecnológico

| Capa | Tecnología | Versión |
|------|------------|---------|
| Frontend | React + Vite + Axios | React 18 |
| BFF | Spring Boot WebFlux | 3.2.5 |
| API Gateway | Spring Cloud Gateway o Kong | SCG 4.1 / Kong 3.8 |
| Microservicios | Spring Boot + JPA (Hibernate) | 3.2.5 |
| Java Runtime | Eclipse Temurin LTS | 21.0.5 |
| Build tool | Maven | 3.9.9 |
| Base de Datos | PostgreSQL | 17 |
| Mensajería | Apache Kafka (Confluent) | 7.7.1 |
| Logs | ELK Stack | 8.15.3 |
| Métricas | Prometheus + Grafana | 2.55 / 11.3 |
| Almacenamiento | MinIO (S3 compatible) | RELEASE.2024-10-13 |
| Contenedores | Docker | 24+ |
| Orquestación | Kubernetes | 1.28+ |

## Arquitectura

```
                  +------------------+
                  |   React (3000)   |
                  +--------+---------+
                           |  HTTPS
                           v
                  +---------------------+
                  |  API Gateway (8081) |  <- Valida JWT (RS256), routing, rate-limit
                  |   (SCG o Kong)      |
                  +--------+------------+
                           |  HTTP
                           v
                  +------------------+
                  |   BFF (8080)     |  <- Composición y circuit breaker para la UI
                  +--------+---------+
                           |
   +----------------+------+------+----------------+
   |                |             |                |
   v                v             v                v
+---------+   +-----------+  +-----------+   +-------------+
| MS-Auth |   |MS-Academico|  |MS-Asist. |   |MS-Mensajeria|
|  8082   |   |    8083    |  |   8084   |   |    8085     |
+----+----+   +------+-----+  +-----+----+   +------+------+
     |               |              |               |
     v               v              v               v
  Postgres        Postgres       Postgres        Postgres
  (auth_db)      (acad_db)      (asist_db)      (msg_db)

         <----------- Kafka (9092) ----------->
              eventos: inasistencia, nota.creada,
              mensaje.enviado, notificacion.requerida

         <-- ELK Stack: Filebeat -> Logstash -> Elasticsearch -> Kibana -->
         <-- Prometheus / Grafana (métricas) -->
         <-- MinIO/S3 (reportes PDF) -->
```

## Microservicios y Puertos

| Servicio | Puerto | Base de Datos | Responsabilidad |
|----------|--------|---------------|-----------------|
| Frontend (React + Nginx) | 3000 | (sin BD) | UI |
| BFF | 8080 | (sin BD) | Composición para frontend |
| API Gateway (Spring Cloud Gateway) | 8081 | (sin BD) | Routing, JWT validation, rate-limit |
| API Gateway (Kong, alternativa) | 8000 (proxy) / 8001 (admin) | (sin BD) | Routing, JWT plugin |
| MS-Auth | 8082 | auth_db (5432) | Login, JWT, gestión de usuarios |
| MS-Académico | 8083 | acad_db (5433) | CRUD notas, generación PDF |
| MS-Asistencia | 8084 | asist_db (5434) | Registro asistencia, evento inasistencia |
| MS-Mensajería | 8085 | msg_db (5435) | Mensajes y notificaciones |
| Kafka | 9092 | (broker) | Bus de eventos |
| Elasticsearch | 9200 | (índice) | Storage de logs |
| Kibana | 5601 | (UI) | Visualización de logs |
| Prometheus | 9090 | (TSDB) | Métricas |
| Grafana | 3001 | (UI) | Dashboards |
| MinIO | 9000 / 9001 (UI) | (objetos) | Almacenamiento de PDFs |

## Roles RBAC

- ADMIN: gestión de usuarios y configuración global.
- DOCENTE: registra notas y asistencia, envía mensajes, consulta reportes.
- APODERADO: consulta notas y asistencia de pupilos, recibe notificaciones.

## Requisitos Previos

### Docker Desktop (obligatorio)

El proyecto se orquesta íntegramente con Docker Compose v2 (sintaxis `docker compose`,
con espacio, no `docker-compose` con guión que está deprecado). Necesitas
**Docker Desktop 24 o superior** corriendo antes de ejecutar cualquier comando del
proyecto.

| Sistema | Cómo instalar |
|---------|---------------|
| **Windows 10/11** | Descarga Docker Desktop desde https://www.docker.com/products/docker-desktop/. Habilita WSL 2 si te lo solicita. Al terminar, reinicia el equipo. |
| **macOS (Intel y Apple Silicon)** | Descarga el instalador `.dmg` desde la misma URL. Para Apple Silicon usa el build "Apple Chip". |
| **Linux Ubuntu/Debian** | `sudo apt update && sudo apt install -y docker.io docker-compose-plugin`. Agrega tu usuario al grupo `docker` (`sudo usermod -aG docker $USER`) y reabre la sesión. |

#### Verifica que Docker Desktop está funcionando

Antes de continuar, abre Docker Desktop y espera a que su ícono en la bandeja del
sistema indique "Engine running". Luego en una terminal nueva valida:

```bash
docker --version
# Esperado: Docker version 24.x.x o superior

docker compose version
# Esperado: Docker Compose version v2.x.x o superior

docker run --rm hello-world
# Debe imprimir: "Hello from Docker!"
```

Si alguno de los tres comandos falla:

- **Windows**: abre Docker Desktop manualmente desde el menú Inicio. Si pide actualización WSL, ejecuta en PowerShell con permisos de administrador: `wsl --update`.
- **macOS / Linux**: arranca el daemon (`open -a Docker` en macOS, `sudo systemctl start docker` en Linux).
- **Si dice "command not found"**: Docker Desktop no está instalado o no está en el PATH. Reinstala.

#### Recursos mínimos recomendados para Docker Desktop

| Recurso | Mínimo | Recomendado | Cómo ajustarlo |
|---------|--------|-------------|----------------|
| RAM | 6 GB | 8 GB | Settings -> Resources -> Memory |
| CPU | 2 vCPU | 4 vCPU | Settings -> Resources -> CPUs |
| Disco | 30 GB libres | 60 GB libres | Settings -> Resources -> Disk image size |

Si abres muchos servicios (toda la pila usa ~20 contenedores) y tienes menos de
6 GB asignados, los procesos Java se quedarán sin memoria y los contenedores
entrarán en estado `Restarting`.

### Otras herramientas

| Herramienta | Versión mínima | Para qué se usa | Verificación |
|-------------|----------------|-----------------|--------------|
| Git | 2.40+ | Clonar el repo | `git --version` |
| Java JDK | 21 LTS | Build local fuera de Docker (opcional) | `java --version` |
| Maven | 3.9+ | Build local fuera de Docker (opcional) | `mvn -version` |
| Node.js | 22 LTS | Frontend en modo dev (opcional) | `node --version` |
| kubectl | 1.28+ | Despliegue Kubernetes (opcional) | `kubectl version --client` |

Java, Maven y Node sólo son necesarios si quieres compilar fuera de Docker. La
ruta por contenedores no requiere ninguno de los tres en tu máquina local.

## Instalación y Ejecución

### Paso 1. Clonar el repositorio

```bash
git clone git@github.com:WilliamsRT93/libro-digital-fullstack3.git libro-clases
cd libro-clases
```

### Paso 2. Generar las claves RSA para el JWT

MS-Auth firma los tokens con RS256, por lo que necesita un par de claves PEM en
`ms-auth/src/main/resources/keys/`. Elige el método según tu sistema operativo.

#### En Linux, macOS o WSL (con OpenSSL nativo)

```bash
mkdir -p ms-auth/src/main/resources/keys
cd ms-auth/src/main/resources/keys

# 1. BORRAR claves anteriores (si existen) para garantizar regeneración limpia
rm -f *.pem

# 2. Generar la clave privada en formato PKCS8
openssl genpkey -algorithm RSA -out private_key.pem -pkeyopt rsa_keygen_bits:2048

# 3. Extraer la clave pública en formato X.509
openssl rsa -in private_key.pem -pubout -out public_key.pem

# 4. Verificar que la clave privada está bien formada
openssl rsa -in private_key.pem -check -noout
echo "OK: claves regeneradas"

cd -
```

#### En Windows con PowerShell (sin instalar OpenSSL)

Aprovechamos que Docker ya está instalado para el proyecto y usamos un
contenedor temporal con OpenSSL:

```powershell
# 1. Crear la carpeta destino si no existe
New-Item -ItemType Directory -Force -Path "ms-auth\src\main\resources\keys" | Out-Null

# 2. BORRAR claves anteriores (si existen) para garantizar regeneración limpia
Remove-Item ms-auth\src\main\resources\keys\*.pem -ErrorAction SilentlyContinue

# 3. Generar la clave privada (PKCS8)
docker run --rm -v "${PWD}\ms-auth\src\main\resources\keys:/keys" alpine/openssl `
  genpkey -algorithm RSA -out /keys/private_key.pem -pkeyopt rsa_keygen_bits:2048

# 4. Extraer la clave pública (X.509)
docker run --rm -v "${PWD}\ms-auth\src\main\resources\keys:/keys" alpine/openssl `
  rsa -in /keys/private_key.pem -pubout -out /keys/public_key.pem

# 5. Verificar que la clave privada es válida
docker run --rm -v "${PWD}\ms-auth\src\main\resources\keys:/keys" alpine/openssl `
  rsa -in /keys/private_key.pem -check -noout
Write-Host "OK: claves regeneradas" -ForegroundColor Green
```

Atajo: si quieres ejecutar todo lo anterior en un solo comando, usa el script
incluido `.\scripts\reset-keys.ps1` que ya hace borrado + regeneración +
verificación.

#### En Windows con Git Bash

Git for Windows trae OpenSSL incluido. Abre Git Bash (no PowerShell) y ejecuta
los mismos comandos del bloque Linux/macOS de más arriba.

#### En Windows con OpenSSL instalado nativo

```powershell
choco install openssl -y    # con Chocolatey
# o
scoop install openssl       # con Scoop
# o instalador gráfico desde https://slproweb.com/products/Win32OpenSSL.html
```

#### Verificación (todos los sistemas)

```bash
# En Linux/macOS/Git Bash:
ls ms-auth/src/main/resources/keys/

# En PowerShell:
Get-ChildItem ms-auth\src\main\resources\keys
```

Deben aparecer `private_key.pem` y `public_key.pem`.

### Paso 3. Construir las imágenes Docker

Compila y construye todas las imágenes en una sola pasada:

```bash
docker compose build
```

Esto puede demorar entre 5 y 20 minutos en la primera ejecución debido a la
descarga de dependencias Maven y npm. En builds posteriores Docker reutiliza
las capas y se reduce a 1-2 minutos por servicio modificado.

### Paso 4. Levantar la pila completa (arranque por etapas)

Para evitar race conditions (especialmente que los microservicios intenten
conectar a Kafka antes de que el broker esté listo), usa el script de
arranque por etapas incluido en el proyecto:

```bash
# Linux / macOS / Git Bash
bash scripts/start-staged.sh
```

```powershell
# Windows PowerShell
.\scripts\start-staged.ps1
```

Este script ejecuta el arranque en este orden:

1. **Bases de datos PostgreSQL** (postgres-auth, postgres-academico, postgres-asistencia, postgres-mensajeria)
2. **Zookeeper y Kafka** con esperas controladas (10s + 15s)
3. **Stack de observabilidad** (Elasticsearch, Logstash, Kibana, MinIO, Prometheus, Grafana)
4. **MS-Auth** primero (espera 30s y verifica health) para que publique el JWKS
5. **Resto de microservicios** (Académico, Asistencia, Mensajería), luego API Gateway, BFF y Frontend

Equivalente manual sin script:

```bash
# 1. Bases de datos primero
docker compose up -d postgres-auth postgres-academico postgres-asistencia postgres-mensajeria
sleep 8

# 2. Kafka y Zookeeper
docker compose up -d zookeeper
sleep 10
docker compose up -d kafka
sleep 15

# 3. Observabilidad
docker compose up -d elasticsearch logstash kibana minio prometheus grafana
sleep 10

# 4. MS-Auth (primero, espera a que publique JWKS)
docker compose up -d ms-auth
sleep 30

# 5. Resto de microservicios y capas de borde
docker compose up -d ms-academico ms-asistencia ms-mensajeria
sleep 15
docker compose up -d api-gateway
sleep 10
docker compose up -d bff frontend
```

Monitorea el progreso:

```bash
docker compose ps
docker compose logs -f ms-auth
```

Cuando veas en los logs `Started AuthApplication in X.X seconds`, el servicio
está listo. Presiona Ctrl+C para salir del seguimiento de logs.

### Paso 5. Verificar que todo esté saludable

#### Linux / macOS / Git Bash

```bash
for port in 8082 8083 8084 8085 8081 8080; do
  echo -n "Puerto $port: "
  curl -sf http://localhost:$port/actuator/health || echo "DOWN"
  echo
done
```

#### Windows PowerShell

`curl` en PowerShell es un alias de `Invoke-WebRequest`. Usa cualquiera:

```powershell
# Comprobar todos los puertos de una vez
@(8082, 8083, 8084, 8085, 8081, 8080) | ForEach-Object {
  $url = "http://localhost:$_/actuator/health"
  try {
    $r = Invoke-WebRequest $url -TimeoutSec 5 -ErrorAction Stop
    Write-Host "OK   $url -> $($r.Content)"
  } catch {
    Write-Host "FAIL $url -> $($_.Exception.Message)" -ForegroundColor Red
  }
}

# O usar curl.exe (el curl real, viene en Windows 10/11)
curl.exe http://localhost:8082/actuator/health
```

Todos deben retornar `{"status":"UP"}`.

### Paso 6. Acceder a las interfaces

| Servicio | URL | Credenciales |
|----------|-----|--------------|
| Frontend | http://localhost:3000 | (las que se creen en MS-Auth) |
| Kibana | http://localhost:5601 | (sin auth en local) |
| Grafana | http://localhost:3001 | admin / admin |
| Prometheus | http://localhost:9090 | (sin auth) |
| MinIO Console | http://localhost:9001 | minioadmin / minioadmin |

### Paso 7. Usuarios de demostración (precargados)

MS-Auth carga automáticamente tres usuarios de prueba al iniciarse por primera
vez (gracias a `data.sql` con BCrypt factor 12). No necesitas crear usuarios
manualmente para probar la aplicación.

| Usuario | Contraseña | Roles | Propósito |
|---------|------------|-------|-----------|
| `admin1` | `Admin123!` | ADMIN, DOCENTE | Acceso total al sistema |
| `docente1` | `Docente123!` | DOCENTE | Registro de notas y asistencia |
| `apoderado1` | `Apoderado123!` | APODERADO | Consulta de notas y reportes |

Los hashes BCrypt están en `ms-auth/src/main/resources/data.sql`. Para regenerar
con otras contraseñas, edita ese archivo y reinicia MS-Auth.

Si prefieres crear usuarios adicionales manualmente:

```bash
# Linux / macOS / WSL
docker exec -it postgres-auth psql -U auth_user -d auth_db
```

**Importante en Windows con Git Bash (MINGW64)**: el flag `-it` interactivo
no funciona bien sin TTY. Usa una de estas alternativas:

```bash
# Opción A: con winpty (Git Bash incluye winpty)
winpty docker exec -it postgres-auth psql -U auth_user -d auth_db

# Opción B: ejecutar consultas no interactivas con -c
docker exec -i postgres-auth psql -U auth_user -d auth_db -c "SELECT username, full_name FROM users;"

# Opción C: en PowerShell (TTY funciona normalmente)
docker exec -it postgres-auth psql -U auth_user -d auth_db
```

### Paso 8. Probar el flujo end-to-end

Usa las credenciales reales de `data.sql`. El flujo NO necesita `jq`
instalado: extraemos el token con `grep + sed` o con un parser PowerShell.

#### Linux / macOS / Git Bash

```bash
# 1. Login y extraer JWT (sin jq)
RESP=$(curl -s -X POST http://localhost:8080/bff/login \
  -H "Content-Type: application/json" \
  -d '{"username":"docente1","password":"Docente123!"}')
echo "$RESP"
TOKEN=$(echo "$RESP" | grep -oE '"token":"[^"]*"' | sed 's/"token":"//;s/"//')
echo "Token: ${TOKEN:0:60}..."

# 2. Registrar asistencia (genera evento Kafka si es AUSENTE)
curl -X POST http://localhost:8080/bff/asistencias \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"alumnoId":1,"cursoId":1,"fecha":"2026-04-29","estado":"AUSENTE"}'

# 3. Descargar reporte PDF
curl -X GET "http://localhost:8080/bff/notas/reporte-pdf?alumnoId=1" \
  -H "Authorization: Bearer $TOKEN" \
  --output reporte.pdf
```

#### Windows PowerShell (sin jq, parser nativo)

```powershell
# 1. Login y extraer JWT
$resp = Invoke-RestMethod -Uri http://localhost:8080/bff/login `
  -Method POST -ContentType "application/json" `
  -Body '{"username":"docente1","password":"Docente123!"}'
$token = $resp.token
Write-Host "Token: $($token.Substring(0,60))..."

# 2. Registrar asistencia
$body = @{ alumnoId=1; cursoId=1; fecha="2026-04-29"; estado="AUSENTE" } | ConvertTo-Json
Invoke-RestMethod -Uri http://localhost:8080/bff/asistencias `
  -Method POST -ContentType "application/json" `
  -Headers @{ Authorization = "Bearer $token" } -Body $body

# 3. Descargar reporte PDF
Invoke-WebRequest -Uri "http://localhost:8080/bff/notas/reporte-pdf?alumnoId=1" `
  -Headers @{ Authorization = "Bearer $token" } -OutFile reporte.pdf
```

> **Credenciales correctas** (las que carga automáticamente MS-Auth desde
> `data.sql`):
> `admin1 / Admin123!` - `docente1 / Docente123!` - `apoderado1 / Apoderado123!`

### Comandos útiles

```bash
docker compose stop                # detener preservando datos
docker compose down                # detener y eliminar contenedores (mantiene volúmenes)
docker compose down -v             # detener y eliminar TODO incluidos volúmenes
docker compose build ms-auth       # reconstruir un servicio
docker compose up -d ms-auth       # levantar/reiniciar un servicio
docker compose logs -f ms-auth     # logs en tiempo real
docker exec -it ms-auth sh         # shell dentro del contenedor
```

### Scripts de utilidad

La carpeta `scripts/` agrupa helpers para tareas frecuentes:

| Script | Plataforma | Propósito |
|--------|-----------|-----------|
| `start-staged.ps1` / `.sh` | Windows / Linux | Arranque por etapas con limpieza de redes |
| `reset-keys.ps1` / `.sh` | Windows / Linux | Borra y regenera las claves RSA del JWT |
| `fix-docker.ps1` | Windows PowerShell | Limpia caché corrupto de Docker y rearma |
| `fix-network.ps1` / `.sh` | Windows / Linux | Resuelve "network not found" del compose |
| `rebuild-microservicios.ps1` / `.sh` | Windows / Linux | Rebuild forzado de los 6 microservicios Java |
| `pull-base-images.ps1` / `.sh` | Windows / Linux | Pre-descarga las 13 imágenes base con reintentos |

Uso típico:

```powershell
# Regenerar claves del JWT
.\scripts\reset-keys.ps1

# Después, reconstruir MS-Auth
docker compose build ms-auth
docker compose up -d ms-auth
```

### Frontend React

Ver sección completa al final del documento.

## Errores Frecuentes

Esta sección recopila los problemas más comunes que verás al levantar el proyecto
por primera vez. Lee aquí antes de reportar bugs.

### 1. `Connection refused` al hacer curl al microservicio

**Síntoma**: `curl http://localhost:8082/actuator/health` retorna
`No es posible conectar con el servidor remoto` o `Failed to connect to localhost port 8082`.

**Causas y soluciones**:

| Causa probable | Diagnóstico | Solución |
|----------------|-------------|----------|
| Los contenedores no están levantados | `docker compose ps` muestra lista vacía | Ejecutar `docker compose up -d` |
| MS-Auth aún está arrancando | Estado `starting` en `docker compose ps` | Esperar 60 a 120 segundos |
| MS-Auth crasheó al iniciar | Estado `Exited` o `Restarting` | `docker compose logs ms-auth --tail 100` |
| Docker Desktop apagado | `docker --version` falla | Abrir Docker Desktop |
| Firewall bloqueando localhost | Otros puertos tampoco responden | Desactivar temporal o agregar excepción |

### 2. `version is obsolete` al ejecutar docker compose

**Síntoma**: `WARN[0000] /docker-compose.yml: the attribute version is obsolete`.

**Solución**: Ya está corregido en este proyecto. Si lo ves, es porque tienes
una versión vieja en caché. Refresca con:

```bash
docker compose down
git pull
docker compose up -d
```

### 3. `command not found: docker-compose` (con guión)

**Síntoma**: `bash: docker-compose: command not found`.

**Solución**: Docker Compose v1 (con guión) está deprecado. Usa siempre la
sintaxis nueva con espacio: **`docker compose`**. Viene incluida en Docker
Desktop 24+. Si tienes Docker viejo, actualiza Docker Desktop.

### 4. `error during connect: ... pipe/docker_engine` (Windows)

**Síntoma**: Mensajes como `open //./pipe/docker_engine: The system cannot find the file specified`.

**Causa**: Docker Desktop no está corriendo.

**Solución**:

1. Abre Docker Desktop desde el menú Inicio.
2. Espera a que la bandeja muestre "Engine running" (puede tardar 30 a 60 segundos).
3. Verifica con `docker run --rm hello-world`.

### 5. `Cannot start service ms-auth: keys not found` o `private_key.pem`

**Síntoma**: MS-Auth crashea con `FileNotFoundException` apuntando a `private_key.pem`.

**Causa**: No se ejecutó el Paso 2 (generar las claves RSA), o las claves se
generaron pero la imagen Docker se construyó antes y no las incluye.

**Solución**:

```bash
# 1. Generar las claves (ver Paso 2)
# 2. Reconstruir la imagen
docker compose build ms-auth
docker compose up -d ms-auth
```

### 6. Elasticsearch no inicia / sale `max virtual memory areas vm.max_map_count too low`

**Síntoma**: `elasticsearch` queda en estado `Exited` con código 78.

**Solución**:

- **Linux**: `sudo sysctl -w vm.max_map_count=262144`
- **Windows/macOS**: Editar `Settings -> Resources -> Advanced` en Docker Desktop
  y aumentar la memoria a 6 GB mínimo. WSL2 maneja `vm.max_map_count` automáticamente
  si tienes Docker Desktop reciente; si no, en PowerShell de admin: `wsl -d docker-desktop -u root sysctl -w vm.max_map_count=262144`.

### 7. `Port is already allocated`

**Síntoma**: `Bind for 0.0.0.0:5432 failed: port is already allocated`.

**Causa**: Otro Postgres (u otro servicio) está usando ese puerto en tu máquina.

**Solución**:

- Detener el servicio que ocupa el puerto, o
- Cambiar el mapping en `docker-compose.yml`. Por ejemplo, cambia
  `"5432:5432"` a `"15432:5432"` y la BD seguirá accesible en `localhost:15432`.

```bash
# Identificar quién usa el puerto en Linux/macOS:
lsof -i :5432

# En Windows PowerShell:
Get-NetTCPConnection -LocalPort 5432
```

### 8. Login retorna 401 siempre

**Síntoma**: Devuelve `{"code":"AUTH_001","message":"Invalid credentials"}` aunque
las credenciales son correctas.

**Causas**:

1. El hash BCrypt insertado en `auth_db` no corresponde a la contraseña.
2. No insertaste el rol en `user_roles` y el JWT viene sin claims útiles.

**Solución**: Genera un hash BCrypt válido:

```bash
docker run --rm openjdk:21-slim java -e "
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
System.out.println(new BCryptPasswordEncoder(12).encode(\"miClave123\"));
"
```

O bien, levanta una shell en MS-Auth y genera el hash con un endpoint helper.

### 9. `docker compose build` se queda colgado descargando dependencias

**Síntoma**: La etapa `mvn dependency:go-offline -B` parece no progresar.

**Causa**: Maven Central o el mirror configurado puede estar lento o caído.

**Solución**:

```bash
# Limpiar caché de Docker y reintentar
docker builder prune -f
docker compose build --no-cache ms-auth
```

### 10. `Manifest unknown` o `pull access denied` al hacer build

**Síntoma**: `docker compose build` falla con `manifest for image:tag not found`.

**Causa**: Docker no puede descargar una imagen base. Suele ser porque la versión
fijada quedó desactualizada o tu Docker Desktop tiene la versión 4.20 o anterior
que no soporta ciertos manifest schemas.

**Solución**:

1. Actualiza Docker Desktop a la versión más reciente.
2. Si persiste, edita el `Dockerfile` correspondiente y cambia el tag exacto
   por uno más genérico (por ejemplo `21-jre-alpine` en lugar de `21.0.5_11-jre-alpine`).

### 11. Frontend muestra "Network Error" al hacer login

**Síntoma**: La consola del navegador muestra error CORS o `Network Error`.

**Causa**: El BFF no está corriendo o el frontend no encuentra el proxy.

**Solución**:

```bash
docker compose ps bff       # debe estar Up
docker compose logs bff     # buscar errores
```

Si reinicias el BFF, espera 30 segundos antes de reintentar el login.

### 12. Kafka se reinicia constantemente

**Síntoma**: `kafka` aparece en estado `Restarting` cada 30 segundos.

**Por qué Zookeeper y Kafka deben trabajar en conjunto**

Kafka no es un sistema autónomo: necesita un coordinador externo que mantenga el estado
del clúster. Zookeeper cumple ese rol. Antes de que Kafka pueda recibir o entregar mensajes,
le pregunta a Zookeeper tres cosas fundamentales:

- **Quién soy en el clúster**: Zookeeper asigna y registra el `broker.id` de cada nodo Kafka.
  Sin ese registro, Kafka no sabe si es el broker primario o una réplica.
- **Cuáles topics y particiones existen**: los metadatos de topics (cuántas particiones,
  factor de replicación, offsets de líderes) se persisten en Zookeeper. Si Kafka arranca
  sin Zookeeper, no puede leer ni escribir esos metadatos y falla inmediatamente.
- **Elección de líder**: cuando un broker cae, Zookeeper coordina qué réplica asume el rol
  de líder de cada partición. Sin Zookeeper, el clúster no puede recuperarse de fallos.

La relación es de dependencia estricta: Kafka no tiene lógica interna para sustituir a
Zookeeper (esto cambia a partir de Kafka 3.3+ con el modo KRaft, pero la versión usada en
este proyecto — Confluent 7.7.1, basada en Kafka 3.7 — aún requiere Zookeeper por defecto).

**Por qué Zookeeper debe iniciar primero**

Al arrancar, Kafka intenta conectarse a Zookeeper en el puerto `2181` durante un tiempo
limitado (por defecto 6 segundos con hasta 3 reintentos). Si Zookeeper no responde en esa
ventana, Kafka lanza una excepción `SessionExpiredException` o `ConnectionLossException`
y el proceso termina. Docker lo detecta como un fallo y reinicia el contenedor, generando
el loop de `Restarting` que se ve en `docker compose ps`.

El orden correcto garantiza que cuando Kafka hace su primera conexión a `zookeeper:2181`,
el servidor ya está escuchando y puede responder al handshake de sesión ZAB (Zookeeper
Atomic Broadcast) sin timeout.

**Causa del error**: Zookeeper no terminó de levantar antes de Kafka, o falta memoria.

**Solución**:

```bash
docker compose down
docker compose up -d zookeeper
sleep 15
docker compose up -d kafka
sleep 20
docker compose up -d
```

### 13. WSL2 ocupa toda la RAM en Windows

**Síntoma**: Tu laptop se vuelve lenta tras varias horas con Docker Desktop abierto.

**Solución**: Crear `%UserProfile%\.wslconfig` con:

```ini
[wsl2]
memory=6GB
processors=4
swap=2GB
```

Cierra Docker Desktop, ejecuta `wsl --shutdown` en PowerShell y vuelve a abrir
Docker Desktop.

### 14. `vite` no se puede instalar / `EBADENGINE`

**Síntoma**: `npm install` en el frontend falla con error de engine de Node.

**Causa**: Tu Node local es muy antiguo.

**Solución**: Usa Node 22 LTS o ejecuta el frontend dentro de Docker
(`docker compose up -d frontend`) que ya trae la versión correcta.

### 15. `failed commit on ref ... unexpected commit digest`

**Síntoma**: `docker compose up` falla al descargar capas con mensajes como:

```
failed commit on ref "layer-sha256:abc...":
commit failed: unexpected commit digest sha256:xyz...,
expected sha256:abc...: failed precondition
```

**Causa**: una capa quedó corrupta en el caché local de Docker (corte de red,
disco lleno, bug del containerd snapshotter en Docker Desktop).

**Solución automática**:

```powershell
# Windows PowerShell desde la raíz del proyecto
.\scripts\fix-docker.ps1
```

**Solución manual paso a paso**:

```bash
# 1. Bajar todo
docker compose down -v

# 2. Limpiar caché de imágenes y builders
docker system prune -af --volumes
docker builder prune -af

# 3. Reiniciar Docker Desktop completamente
#    (Settings > Troubleshoot > Clean / Purge data, o reiniciar el servicio)

# 4. Pre-descargar imágenes una por una (no en paralelo)
docker pull postgres:17-alpine
docker pull confluentinc/cp-kafka:7.7.1
docker pull confluentinc/cp-zookeeper:7.7.1
docker pull docker.elastic.co/elasticsearch/elasticsearch:8.15.3
docker pull docker.elastic.co/logstash/logstash:8.15.3
docker pull docker.elastic.co/kibana/kibana:8.15.3
docker pull minio/minio:RELEASE.2024-10-13T13-34-11Z
docker pull prom/prometheus:v2.55.1
docker pull grafana/grafana:11.3.0

# 5. Reconstruir y levantar
docker compose build --no-cache
docker compose up -d
```

**Si el error persiste**: en Docker Desktop ve a Settings > General y
desactiva "Use containerd for pulling and storing images" (vuelve al
snapshotter clásico). Reinicia Docker Desktop y reintenta.

### 16. `npm install` falla con `ERESOLVE` en el frontend

**Síntoma**: durante el build del contenedor frontend aparece
`ERESOLVE could not resolve` o `peer dep` no compatible.

**Solución**: el `Dockerfile` ya usa `npm install --legacy-peer-deps` para
tolerar conflictos de peer dependencies. Si lo ejecutas localmente, usa el
mismo flag:

```bash
cd frontend
npm install --legacy-peer-deps
npm run dev
```

### 17. `docker exec -it` se cuelga en Git Bash (MINGW64)

**Síntoma**: al ejecutar `docker exec -it postgres-auth psql ...` la consola
queda esperando entrada que nunca llega.

**Causa**: Git Bash en Windows usa MinTTY que no es TTY real. El flag `-it`
no funciona como en Linux.

**Solución**: usar `winpty` (incluido en Git Bash) o evitar el modo interactivo:

```bash
# Solución 1: prefijo winpty
winpty docker exec -it postgres-auth psql -U auth_user -d auth_db

# Solución 2: ejecutar SQL directamente con -c (sin interactividad)
docker exec -i postgres-auth psql -U auth_user -d auth_db -c "SELECT * FROM users;"

# Solución 3: usar PowerShell en vez de Git Bash
# (en PowerShell el TTY funciona normalmente)
```

### 18. MS-Mensajería o API Gateway aparecen DOWN aunque levantaron

**Síntoma**: `curl http://localhost:8085/actuator/health` retorna 404 o no
responde, mismo en 8081.

**Causa probable**: el archivo `application.yml` del microservicio no expone
los endpoints de actuator. Spring Boot por defecto sólo expone `/actuator/info`.

**Solución**: ya está corregido en el proyecto, pero si modificaste algún yml,
asegúrate que tenga:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
```

Y reconstruye la imagen: `docker compose build ms-mensajeria api-gateway && docker compose up -d`.

### 19. Login del frontend muestra "El servicio de autenticación no está disponible"

**Síntoma**: en `http://localhost:3000` el login retorna ese mensaje.

**Causas posibles**:

| Causa | Verificación | Solución |
|-------|--------------|----------|
| API Gateway caído | `curl http://localhost:8081/actuator/health` falla | Ver error 18 |
| MS-Auth no terminó de iniciar | `docker compose logs ms-auth` | Esperar 30s más |
| BFF no encuentra Gateway | `docker compose logs bff` muestra `Connection refused` | `docker compose restart bff` |
| Credenciales incorrectas | Estado HTTP 401 | Usar `admin1 / Admin123!` |

El frontend ahora muestra mensajes diferenciados (401 / 503 / 500 / network)
para facilitar el diagnóstico.

### 20. `failed to set up container networking: network <id> not found`

**Síntoma**: al levantar la pila uno o más contenedores fallan con:

```
Error response from daemon: failed to set up container networking:
network 218b51804d31685e5efaf01f27fed965ed4295e4d7ae8045bd53455c112ada66 not found
```

**Causa**: la red `fullstack-3_libro-net` quedó huérfana (eliminada mientras
el compose tenía su ID en caché). Suele pasar cuando:

- Reinicias Docker Desktop con contenedores corriendo.
- Cambias entre proyectos diferentes que usan compose.
- Ejecutas `docker network prune` mientras la pila estaba arriba.

**Solución automática**:

```bash
# Linux / macOS / Git Bash
bash scripts/fix-network.sh
```

```powershell
# Windows PowerShell
.\scripts\fix-network.ps1
```

**Solución manual**:

```bash
docker compose down --remove-orphans
docker network prune -f
docker network rm fullstack-3_libro-net 2>/dev/null || true
docker compose up -d --force-recreate --remove-orphans
```

Si después de eso sigue fallando: cierra Docker Desktop completamente, borra
el caché (`%APPDATA%\Docker\` en Windows o `~/Library/Containers/com.docker.docker`
en macOS), reinicia Docker Desktop y vuelve a ejecutar `start-staged`.

> **Nota**: el script `start-staged.sh` / `.ps1` ahora hace esta limpieza
> automáticamente en su paso [0/6], por lo que rara vez verás este error
> usando el script.

### 21. Build del frontend falla con `rolldown` o `npm run build` exit code 1

**Síntoma**: el `docker compose build frontend` falla con un stacktrace de
`rolldown` o errores como:

```
at aggregateBindingErrorsIntoJsError ... shared/error-DL-e8-oE.mjs
at #build ... rolldown-build-DSxL8qiP.mjs
process "/bin/sh -c npm run build" did not complete successfully: exit code: 1
```

**Causa**: `package.json` quedó con `vite: ^8.0.10` (versión pre-release que
usa rolldown internamente y aún es inestable) o se arrastró un `package-lock.json`
local con dependencias rotas.

**Solución**:

1. **Bajar Vite a una versión estable**. En `frontend/package.json` cambia:

   ```json
   "devDependencies": {
     "@vitejs/plugin-react": "^4.3.4",
     "vite": "^5.4.10"
   }
   ```

2. **Asegurar que el contenedor no copia tu node_modules local**. El proyecto
   ya incluye `frontend/.dockerignore` que excluye `node_modules`, `dist`,
   `package-lock.json` y cachés de Vite/npm.

3. **Forzar legacy-peer-deps** vía `frontend/.npmrc`:

   ```
   legacy-peer-deps=true
   engine-strict=false
   ```

4. **Reconstruir sin caché**:

   ```bash
   docker compose build --no-cache frontend
   docker compose up -d frontend
   ```

5. Si construyes localmente (fuera de Docker):

   ```bash
   cd frontend
   rm -rf node_modules package-lock.json .vite dist
   npm install --legacy-peer-deps
   npm run build
   ```

### 22. `NoResourceFoundException: No static resource actuator/prometheus`

**Síntoma**: en los logs del microservicio aparece cada 15 segundos:

```
ERROR ... GlobalExceptionHandler ... Unhandled error
NoResourceFoundException: No static resource actuator/prometheus
```

Y Prometheus marca el target como `down`.

**Causa**: el endpoint `/actuator/prometheus` no se materializa sólo con
`spring-boot-starter-actuator`. Necesita la dependencia
`micrometer-registry-prometheus` que registra el endpoint y formatea las
métricas en formato OpenMetrics.

**Diagnóstico rápido**: si modificaste el `pom.xml` y el error persiste, lo
más probable es que el contenedor sigue corriendo la imagen antigua. Docker
reusa el caché cuando ve los mismos bytes en el `pom.xml`, así que un simple
`docker compose up -d` no rehace el build. Necesitas forzar el rebuild.

**Solución automática** (recomendada):

```bash
# Linux / macOS / Git Bash
bash scripts/rebuild-microservicios.sh
```

```powershell
# Windows PowerShell
.\scripts\rebuild-microservicios.ps1
```

Estos scripts:
1. Detienen los 6 microservicios Java.
2. Eliminan sus imágenes locales (`docker rmi -f`).
3. Reconstruyen sin caché (`--no-cache`).
4. Levantan los contenedores nuevos.
5. Verifican `/actuator/prometheus` en los 6 puertos.

**Solución manual**:

```bash
# Asegurar que el pom.xml tiene la dependencia
grep -A2 "micrometer-registry-prometheus" ms-auth/pom.xml

# Forzar rebuild sin caché
docker compose build --no-cache ms-auth ms-academico ms-asistencia ms-mensajeria api-gateway bff

# Recrear contenedores con la imagen nueva
docker compose up -d --force-recreate ms-auth ms-academico ms-asistencia ms-mensajeria api-gateway bff
```

Verificación después del rebuild:

```bash
curl http://localhost:8082/actuator/prometheus | head -3
# Esperado: líneas tipo "# HELP jvm_memory_used_bytes" o "jvm_memory_used_bytes{..."
```

> **Nota adicional**: el `GlobalExceptionHandler` de MS-Auth ahora maneja
> `NoResourceFoundException` con un log en nivel DEBUG (sin stack trace) en
> lugar de ERROR. Así mientras se hace el rebuild no llenas los logs de
> stack traces de 100+ líneas.

### 23. `TLS handshake timeout` al descargar imágenes Docker

**Síntoma**: el `docker compose build` falla con:

```
failed to do request: Head "https://registry-1.docker.io/v2/library/maven/manifests/...":
net/http: TLS handshake timeout
```

Y normalmente afecta a una imagen base como `maven:3.9.9-eclipse-temurin-21`,
mientras que otras (frontend) ya están en caché y siguen.

**Causas frecuentes**:

- Conexión a internet inestable o saturada.
- VPN corporativa que interfiere con Docker.
- DNS de Docker mal configurado.
- Docker Hub temporalmente lento (suele resolverse en minutos).
- Antivirus o firewall bloqueando tráfico TLS de Docker.

**Solución 1 - script con reintentos** (la más simple):

```bash
# Linux / macOS / Git Bash
bash scripts/pull-base-images.sh
docker compose build
```

```powershell
# Windows PowerShell
.\scripts\pull-base-images.ps1
docker compose build
```

El script descarga manualmente las 13 imágenes base con hasta 5 reintentos
cada una. Una vez en caché local, el build no necesita ir a Docker Hub.

**Solución 2 - configurar mirror de Docker Hub**

Si el problema persiste (red corporativa, país con latencia alta), edita
las **Settings -> Docker Engine** de Docker Desktop y agrega un mirror:

```json
{
  "registry-mirrors": [
    "https://mirror.gcr.io"
  ],
  "dns": ["8.8.8.8", "1.1.1.1"]
}
```

Aplica los cambios (Docker Desktop reinicia el daemon) y vuelve a intentar.

**Solución 3 - desactivar VPN temporalmente**

VPN corporativas suelen romper el TLS handshake con `registry-1.docker.io`.
Si tu empresa usa Cisco AnyConnect, GlobalProtect, OpenVPN, etc., apágala
sólo durante el `docker pull`/`docker compose build`. Después puedes
reactivarla.

**Solución 4 - reintentar simplemente**

Estos errores suelen ser transitorios. Volver a ejecutar `docker compose build`
30 a 60 segundos después funciona en muchos casos.

### 24. `Premature end of Content-Length` en build de Maven

**Síntoma**: durante `docker compose build` un microservicio Java falla con:

```
Could not transfer artifact org.rocksdb:rocksdbjni:jar:7.9.2
from/to central (https://repo.maven.apache.org/maven2):
Premature end of Content-Length delimited message body
(expected: 58,000,372; received: 30,433,856)
```

**Causa**: la conexión a Maven Central se cortó a mitad de la descarga del JAR
de `rocksdbjni` (58 MB, requerido por Spring Kafka). Es común con redes lentas,
saturadas, o con VPN corporativa.

**Soluciones**:

1. **Configuración robusta en Dockerfile** (ya aplicada en el proyecto). Todos
   los Dockerfile Java tienen ahora:
   - `MAVEN_OPTS` con timeouts largos (5 min connect, 15 min request) y
     hasta 10 reintentos por descarga.
   - Comando `mvn -B dependency:go-offline` ejecutado **3 veces seguidas** con
     `||` para que el build tolere fallos transitorios.
   - **BuildKit cache mount** `--mount=type=cache,target=/root/.m2` que
     persiste el repositorio Maven entre builds. Así `rocksdbjni` sólo se
     descarga una vez y nunca más.

2. **Habilitar BuildKit** (Docker Desktop 24+ ya lo tiene por defecto). Si no:

   ```powershell
   # Windows PowerShell
   $env:DOCKER_BUILDKIT=1
   docker compose build
   ```

   ```bash
   # Linux / macOS / Git Bash
   export DOCKER_BUILDKIT=1
   docker compose build
   ```

3. **Pre-descargar la dependencia problemática fuera de Docker** y montarla
   como volumen. Si tienes Maven local:

   ```bash
   mvn -f ms-auth/pom.xml dependency:get -Dartifact=org.rocksdb:rocksdbjni:7.9.2
   ```

4. **Usar mirror de Maven**. Crea `~/.m2/settings.xml` (en tu máquina) con un
   mirror más cercano, por ejemplo Aliyun (Asia) o cualquier mirror corporativo:

   ```xml
   <settings>
     <mirrors>
       <mirror>
         <id>maven-mirror</id>
         <name>Maven Central Mirror</name>
         <url>https://maven.aliyun.com/repository/public</url>
         <mirrorOf>central</mirrorOf>
       </mirror>
     </mirrors>
   </settings>
   ```

5. **Reintentar simplemente**. El cache mount hace que sólo lo que faltó se
   re-descargue. Tras una primera descarga exitosa parcial, el siguiente
   `docker compose build` retoma desde donde quedó.

### 25. Después de `docker compose down -v` perdí mis usuarios

**Síntoma**: Después de bajar la pila con `-v`, los usuarios creados desaparecieron.

**Causa**: La opción `-v` borra los volúmenes nominados, incluyendo los datos
de Postgres.

**Solución**: No es un error, es lo esperado. Para preservar datos usa sólo
`docker compose down` (sin `-v`). Para resetear el entorno desde cero, `down -v`
es el comando correcto.

## Pruebas Unitarias

El proyecto incluye **15 pruebas** distribuidas en 4 microservicios: 13 pruebas unitarias con
Mockito (sin levantar contexto Spring) y 2 pruebas de integración Kafka con broker embebido.
Todas usan JUnit 5 + AssertJ.

### Estructura de pruebas (estándar Maven)

Cada microservicio contiene sus pruebas **dentro** del propio módulo, siguiendo la convención
estándar de Maven para proyectos Spring Boot:

```
ms-auth/
└── src/
    ├── main/java/com/colegio/auth/
    │   ├── controller/      AuthController.java
    │   ├── service/         AuthService.java  UserAdminService.java
    │   ├── repository/      UserRepository.java
    │   └── security/        JwtService.java
    └── test/java/com/colegio/auth/
        └── service/
            ├── AuthServiceTest.java        ← Tests 1-3
            └── UserAdminServiceTest.java   ← Tests 4-8

ms-academico/
└── src/
    ├── main/java/com/colegio/academico/
    │   └── service/         NotaService.java
    └── test/java/com/colegio/academico/
        └── service/
            └── NotaServiceTest.java        ← Tests 9-11 (ver nota abajo*)

ms-asistencia/
└── src/
    ├── main/java/com/colegio/asistencia/
    │   └── service/         AsistenciaService.java
    └── test/java/com/colegio/asistencia/
        ├── service/
        │   └── AsistenciaServiceTest.java  ← Tests 12-13
        └── kafka/
            └── KafkaZookeeperIntegrationTest.java ← Tests 14-15
```

> *La carpeta `Pruebas Unitarias/` en la raíz del repositorio contiene **copias de referencia**
> de los mismos archivos de test. Las pruebas que ejecuta Maven son siempre las que están
> dentro de `src/test/` de cada microservicio, no las de esa carpeta.

> **Nota sobre el comando:** las pruebas del backend usan **Spring Boot + Maven**, no Node.js.
> El comando correcto es `mvn test` (no `npm test`, que corresponde al frontend React).

### Cobertura JaCoCo (resultados reales)

JaCoCo 0.8.11 está configurado en los `pom.xml` de `ms-auth`, `ms-academico` y `ms-asistencia`.
Ejecución verificada con JDK 21.0.11 + Maven 3.8.7:

| Microservicio | Clase | Líneas | Ramas | Estado |
|---|---|---|---|---|
| ms-auth | AuthService | **100%** | **100%** | ✓ PASA |
| ms-auth | UserAdminService | **75%** | **75%** | ✓ PASA |
| ms-academico | NotaService | **100%** | N/A | ✓ PASA |
| ms-asistencia | AsistenciaService | **100%** | **100%** | ✓ PASA |

**Cobertura global por microservicio** (línea, sobre el alcance medido por JaCoCo):

| Microservicio | Líneas | Ramas | Mínimo rúbrica (60%) |
|---|---|---|---|
| ms-auth | **85.3%** | 87.5% | ✓ PASA |
| ms-academico | **93.5%** | N/A (sin ramas) | ✓ PASA |
| ms-asistencia | **69.3%** | 50.0% | ✓ PASA |

**Alcance de la medición:** la estrategia de testing es unitaria sobre la capa de **Service**
(con Mockito, sin infraestructura — ver inventario abajo). `jacoco-maven-plugin` excluye de la
medición las clases de infraestructura que esa estrategia nunca tuvo intención de cubrir con
pruebas unitarias: `*Application`, `config/**`, `exception/**`, `controller/**` y `security/**`
en `ms-auth`; lo mismo en `ms-academico` más `S3StorageService` y `PdfReportService` (E/S real a
S3/iText, propia de pruebas de integración). Configuración en el `<plugin>` de `jacoco-maven-plugin`
de cada `pom.xml`.

Para generar el reporte HTML de cobertura:

```bash
mvn -f ms-auth/pom.xml test
# Reporte en: ms-auth/target/site/jacoco/index.html

mvn -f ms-academico/pom.xml test
# Reporte en: ms-academico/target/site/jacoco/index.html

mvn -f ms-asistencia/pom.xml test -Dtest=AsistenciaServiceTest -Dsurefire.failIfNoSpecifiedTests=false
# Reporte en: ms-asistencia/target/site/jacoco/index.html
```

**Requisito de entorno:** `javac -version` debe mostrar JDK 21. Si muestra JDK 17 o inferior:
```bash
sudo apt install -y openjdk-21-jdk
sudo update-alternatives --config javac  # seleccionar java-21
```

### Inventario de pruebas

| # | Microservicio | Clase | Descripción |
|---|---------------|-------|-------------|
| 1 | ms-auth | `AuthServiceTest` | Login exitoso retorna token y datos del usuario |
| 2 | ms-auth | `AuthServiceTest` | Password incorrecto lanza `BadCredentialsException` |
| 3 | ms-auth | `AuthServiceTest` | Usuario deshabilitado lanza `BadCredentialsException` sin verificar password |
| 4 | ms-auth | `UserAdminServiceTest` | Crear usuario nuevo persiste y retorna `UserResponse` correcto |
| 5 | ms-auth | `UserAdminServiceTest` | Username duplicado lanza `409 CONFLICT` antes de llegar a la BD |
| 6 | ms-auth | `UserAdminServiceTest` | Listar usuarios retorna lista completa mapeada a DTO |
| 7 | ms-auth | `UserAdminServiceTest` | Eliminar ID inexistente lanza `404 NOT FOUND` sin llamar a `deleteById` |
| 8 | ms-auth | `UserAdminServiceTest` | Actualizar roles persiste el nuevo conjunto de roles |
| 9 | ms-asistencia | `AsistenciaServiceTest` | Estado PRESENTE no publica ningún evento en Kafka |
| 10 | ms-asistencia | `AsistenciaServiceTest` | Estado AUSENTE publica `InasistenciaEvent` en el topic correcto |
| 11 | ms-academico | `NotaServiceTest` | Crear nota válida persiste entidad y retorna DTO correcto |
| 12 | ms-academico | `NotaServiceTest` | Consultar notas de alumno retorna lista ordenada |
| 13 | ms-academico | `NotaServiceTest` | Alumno sin notas retorna lista vacía (sin NullPointerException) |
| 14 | ms-asistencia | `KafkaZookeeperIntegrationTest` | Broker Kafka con Zookeeper embebido está activo y accesible |
| 15 | ms-asistencia | `KafkaZookeeperIntegrationTest` | Ciclo completo productor -> topic -> consumidor entrega mensaje intacto |

### Cómo ejecutar las pruebas

#### Todos los microservicios a la vez (desde la raíz del proyecto)

```bash
mvn -pl ms-auth,ms-academico,ms-asistencia test
```

#### Por microservicio individual

```bash
# Sólo MS-Auth (pruebas 1-8)
mvn -f ms-auth/pom.xml test

# Sólo MS-Académico (pruebas 11-13)
mvn -f ms-academico/pom.xml test

# Sólo MS-Asistencia (pruebas 9-10 + 14-15)
mvn -f ms-asistencia/pom.xml test
```

#### Sólo una clase específica

```bash
mvn -f ms-auth/pom.xml test -Dtest=AuthServiceTest
mvn -f ms-auth/pom.xml test -Dtest=UserAdminServiceTest
mvn -f ms-asistencia/pom.xml test -Dtest=AsistenciaServiceTest
mvn -f ms-asistencia/pom.xml test -Dtest=KafkaZookeeperIntegrationTest
mvn -f ms-academico/pom.xml test -Dtest=NotaServiceTest
```

#### Sólo un test específico

```bash
mvn -f ms-auth/pom.xml test -Dtest="AuthServiceTest#login_credencialesValidas_retornaTokenYDatosUsuario"
```

#### Dentro de Docker (sin Maven local)

```bash
docker run --rm \
  -v "$(pwd)/ms-auth:/project" \
  -w /project \
  maven:3.9.9-eclipse-temurin-21 \
  mvn test
```

### Resultado esperado

Una ejecución exitosa completa muestra al final de cada módulo:

```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.colegio.auth.service.AuthServiceTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.colegio.auth.service.UserAdminServiceTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] -------------------------------------------------------
[INFO] BUILD SUCCESS
```

Para MS-Asistencia, la prueba de integración Kafka tarda entre 5 y 15 segundos en levantar
el broker embebido; es normal ver logs de Kafka durante ese tiempo:

```
[INFO] Running com.colegio.asistencia.service.AsistenciaServiceTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.colegio.asistencia.kafka.KafkaZookeeperIntegrationTest
... (logs de EmbeddedKafka durante ~10s) ...
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Resumen consolidado al ejecutar los tres módulos juntos: **15 pruebas, 0 fallos, 0 errores**.

### Errores comunes en pruebas y soluciones

#### Error 1: `No Java compiler is provided in this environment`

```
[ERROR] Failed to execute goal ... compile ... No compiler is provided in this environment.
Perhaps you are running on a JRE rather than a JDK?
```

**Causa**: Maven no encuentra el JDK, sólo hay JRE instalado.

**Solución**:
```bash
# Verificar que es JDK (no JRE)
java -version       # debe mostrar "openjdk" o "temurin"
javac -version      # si falla, es JRE puro

# Instalar JDK 21 en Ubuntu/Debian
sudo apt install -y temurin-21-jdk

# En Windows: descargar desde https://adoptium.net y configurar JAVA_HOME
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21..."
```

#### Error 2: `Cannot find symbol` al compilar los tests

```
[ERROR] error: cannot find symbol
symbol:   class NotaRequest
location: class NotaServiceTest
```

**Causa**: el código del servicio tiene cambios que no reflejan los tests, o los tests
están fuera de la estructura de paquetes correcta.

**Solución**:
```bash
mvn -f ms-academico/pom.xml clean test
```

Si el error persiste, verificar que los tests están en
`src/test/java/com/colegio/<microservicio>/...` y no en otro directorio.

#### Error 3: `Mockito cannot mock this class`

```
[ERROR] org.mockito.exceptions.base.MockitoException:
Cannot mock/spy class com.colegio.auth.repository.UserRepository
```

**Causa**: versión incompatible de Mockito o byte-buddy por declarar Mockito manualmente
en el `pom.xml` además de `spring-boot-starter-test`.

**Solución**: No declarar Mockito manualmente; Spring Boot ya lo incluye en la versión
correcta vía `spring-boot-starter-test`. Verificar que no hay exclusiones accidentales:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

#### Error 4: `Address already in use` en `KafkaZookeeperIntegrationTest`

```
[ERROR] Caused by: org.apache.kafka.common.KafkaException:
Failed to create new KafkaAdminClient ... Address already in use: localhost:9092
```

**Causa**: el broker Kafka de Docker Compose está corriendo en `localhost:9092` y
`@EmbeddedKafka` intenta usar el mismo puerto.

**Solución**: detener el broker Kafka de Docker antes de correr la prueba:

```bash
docker compose stop kafka zookeeper
mvn -f ms-asistencia/pom.xml test -Dtest=KafkaZookeeperIntegrationTest
docker compose start kafka zookeeper
```

#### Error 5: `Test timeout` en `KafkaZookeeperIntegrationTest`

```
[ERROR] producirYConsumirEvento_flujoCicloCompleto_mensajeLlegaIntacto
Condition not fulfilled within 10000 milliseconds
```

**Causa**: el broker embebido tardó más de 10 segundos en levantar. Ocurre en máquinas
con poca RAM o en el primer build cuando los JARs de Kafka no están en caché.

**Solución**: ejecutar el test una segunda vez; la caché de la JVM lo resuelve:

```bash
mvn -f ms-asistencia/pom.xml test -Dtest=KafkaZookeeperIntegrationTest
mvn -f ms-asistencia/pom.xml test -Dtest=KafkaZookeeperIntegrationTest
```

#### Error 6: `UnnecessaryStubbingException` al correr todos los tests juntos

```
[ERROR] org.mockito.exceptions.misusing.UnnecessaryStubbingException:
Unnecessary stubbings detected ...
```

**Causa**: Mockito en modo estricto detecta un `when(...)` declarado en `@BeforeEach`
que no fue usado por todos los métodos de test.

**Solución**: mover el stub al test específico que lo necesita, o anotar la clase con
`@MockitoSettings` para relajar la estrictez sólo donde sea necesario:

```java
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class AuthServiceTest { ... }
```

## Frontend React

El frontend es una SPA con React 18 + Vite, ruteo con react-router-dom, sesión en
sessionStorage y un cliente axios centralizado. Se integra con el BFF a través del
prefijo `/bff` (proxy de Vite en desarrollo y proxy de Nginx en producción).

### Estructura del frontend

```
frontend/
├── package.json
├── vite.config.js
├── nginx.conf
├── Dockerfile
├── index.html
└── src/
    ├── main.jsx               # Bootstrap de la SPA y rutas
    ├── styles/global.css      # Sistema de diseño (variables, componentes base)
    ├── api/client.js          # Cliente axios con interceptores
    ├── auth/session.js        # Helpers de sesión JWT
    ├── components/
    │   ├── Layout.jsx         # Header con navegación y datos del usuario
    │   └── ProtectedRoute.jsx # HOC que protege rutas privadas
    └── pages/
        ├── Login.jsx          # Formulario de login con validaciones
        ├── Dashboard.jsx      # Contenedor con sub-rutas
        ├── Home.jsx           # Tiles de accesos rápidos por rol
        ├── Asistencia.jsx     # Registro de asistencia con historial local
        ├── Notas.jsx          # Captura de notas (escala 1.0 a 7.0)
        └── Reportes.jsx       # Descarga de PDF
```

### Modo desarrollo (con hot-reload)

Útil cuando estás iterando sobre la UI sin tener que reconstruir el contenedor.
Requiere Node.js 22 LTS instalado y el BFF corriendo (Docker o local).

```bash
cd frontend
npm install
npm run dev
# http://localhost:3000 con hot-reload
```

### Build de producción local

```bash
cd frontend
npm install
npm run build       # Genera frontend/dist
npm run preview     # Sirve en http://localhost:4173
```

### Build y despliegue dentro de Docker

```bash
docker compose up -d --build frontend
# Disponible en http://localhost:3000
```

### Roles soportados en la UI

| Módulo | Roles permitidos |
|--------|------------------|
| Asistencia | DOCENTE, ADMIN |
| Notas | DOCENTE, ADMIN |
| Reportes PDF | DOCENTE, ADMIN, APODERADO |
| Gestión de Usuarios | ADMIN |

### Gestión de usuarios (sólo ADMIN)

El usuario `admin1` puede crear nuevos docentes y apoderados desde el frontend
(`Dashboard -> Usuarios`) o por API. Los endpoints están protegidos con
`@PreAuthorize("hasRole('ADMIN')")` y validados en MS-Auth.

Ejemplo de creación vía API:

```bash
# 1. Login como admin
TOKEN=$(curl -s -X POST http://localhost:8080/bff/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin1","password":"Admin123!"}' \
  | grep -oE '"token":"[^"]*"' | sed 's/"token":"//;s/"//')

# 2. Crear un docente nuevo
curl -X POST http://localhost:8080/bff/admin/users \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
        "username":"docente2",
        "password":"Docente234!",
        "fullName":"Maria Rojas",
        "roles":["DOCENTE"]
      }'

# 3. Listar todos los usuarios
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/bff/admin/users

# 4. Eliminar un usuario por id
curl -X DELETE -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/bff/admin/users/4
```

Endpoints expuestos por MS-Auth para gestión (todos protegidos por ROLE_ADMIN):

| Método | Path | Propósito |
|--------|------|-----------|
| POST | `/admin/users` | Crear usuario con roles |
| GET | `/admin/users` | Listar todos los usuarios |
| PATCH | `/admin/users/{id}/roles` | Reemplazar roles de un usuario |
| PATCH | `/admin/users/{id}/estado?enabled=true` | Habilitar/deshabilitar usuario |
| DELETE | `/admin/users/{id}` | Eliminar usuario |

### Variante con Kong como API Gateway

```bash
docker compose down
docker compose --profile kong up -d
```

Antes hay que reemplazar `REEMPLAZAR_CON_EL_CONTENIDO_DE_public_key.pem` en
`infra/kong/kong.yml` por el contenido real de
`ms-auth/src/main/resources/keys/public_key.pem`.

### Despliegue en Kubernetes

#### 1. Construir y publicar todas las imágenes

##### Linux / macOS / Git Bash

```bash
docker compose build

# Tag y push de los 7 servicios al registry
for svc in ms-auth ms-academico ms-asistencia ms-mensajeria api-gateway bff frontend; do
  docker tag "fullstack-3-$svc:latest" "registry.local/$svc:1.0.0"
  docker push "registry.local/$svc:1.0.0"
done
```

##### Windows PowerShell

```powershell
docker compose build

@("ms-auth","ms-academico","ms-asistencia","ms-mensajeria",
  "api-gateway","bff","frontend") | ForEach-Object {
  docker tag "fullstack-3-$_`:latest" "registry.local/$_`:1.0.0"
  docker push "registry.local/$_`:1.0.0"
}
```

> Nota sobre el nombre de la imagen: Docker Compose nombra las imágenes
> construidas como `<carpeta-padre>-<servicio>:latest`. Si tu carpeta raíz
> no se llama `fullstack-3`, ajusta el prefijo. Verifica con `docker images`.

#### 2. Crear namespace, secrets y aplicar manifests

```bash
kubectl apply -f k8s/namespace.yaml

# Los Secrets con credenciales de Postgres ya están dentro de k8s/postgres.yaml
# (usando stringData). Para producción reemplázalos con un secret manager
# externo (Sealed Secrets, External Secrets, Vault, AWS SM, Azure KV).

kubectl apply -f k8s/postgres.yaml
kubectl apply -f k8s/kafka.yaml
kubectl apply -f k8s/ms-auth.yaml
kubectl apply -f k8s/ms-academico.yaml
kubectl apply -f k8s/ms-asistencia.yaml
kubectl apply -f k8s/ms-mensajeria.yaml
kubectl apply -f k8s/api-gateway.yaml
kubectl apply -f k8s/bff-frontend.yaml
```

O bien aplicar todo de una sola pasada:

```bash
kubectl apply -f k8s/
```

#### 3. Verificar despliegue

```bash
kubectl get pods -n libro-clases -w
kubectl get svc -n libro-clases
kubectl get ingress -n libro-clases
```

#### 4. Acceso local con Minikube

```bash
minikube tunnel
# agregar a /etc/hosts:  127.0.0.1  libro.colegio.local
# luego abrir https://libro.colegio.local
```

## Estructura del Repositorio

```
Fullstack 3/
├── README.md
├── ARCHITECTURE_DECISIONS.md
├── docker-compose.yml
├── api-gateway/
├── bff/
├── ms-auth/
├── ms-academico/
├── ms-asistencia/
├── ms-mensajeria/
├── frontend/
├── k8s/
└── infra/
    ├── kong/
    ├── logstash/
    └── prometheus/
```

## Estrategia Git

Se adopta Git Flow:

- `main`: releases estables, etiquetadas (vX.Y.Z).
- `develop`: integración continua de features.
- `feature/<ticket>-descripcion`: nuevas funcionalidades.
- `release/X.Y.Z`: estabilización previa al deploy a main.
- `hotfix/<ticket>`: correcciones críticas en producción.

Convención de commits: Conventional Commits (`feat:`, `fix:`, `docs:`, `chore:`, `refactor:`).

## Justificación Técnica

**Spring Boot + JPA**: estándar empresarial Java, reduce boilerplate, ecosistema maduro.

**Database-per-Service**: evita acoplamiento de esquema, permite escalar y versionar
cada microservicio de forma independiente (Bounded Context, DDD).

**JWT RS256**: firma asimétrica permite que el Gateway valide tokens sin compartir
el secreto. MS-Auth firma con clave privada; los demás validan con la pública.

**Kafka**: desacopla productores de consumidores, persiste eventos, permite reproducir.

**API Gateway + BFF**: el Gateway centraliza cross-cutting concerns. El BFF compone
respuestas para la UI. Se ofrecen dos implementaciones (SCG y Kong).

**Resilience4j**: circuit breaker, retry y bulkhead modernos con buena integración
en Micrometer.

**ELK + Prometheus/Grafana**: separación logs/métricas siguiendo mejores prácticas
de observabilidad.

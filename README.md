# Plataforma Libro de Clases Digital

Sistema de microservicios para gestion academica escolar: autenticacion, notas, asistencia,
mensajeria y reportes PDF. Arquitectura event-driven con Kafka, segura con JWT y observable
con ELK + Prometheus/Grafana.

## Stack Tecnologico

| Capa | Tecnologia | Version |
|------|------------|---------|
| Frontend | React + Vite + Axios | React 18 |
| BFF | Spring Boot WebFlux | 3.2.5 |
| API Gateway | Spring Cloud Gateway o Kong | SCG 4.1 / Kong 3.8 |
| Microservicios | Spring Boot + JPA (Hibernate) | 3.2.5 |
| Java Runtime | Eclipse Temurin LTS | 21.0.5 |
| Build tool | Maven | 3.9.9 |
| Base de Datos | PostgreSQL | 17 |
| Mensajeria | Apache Kafka (Confluent) | 7.7.1 |
| Logs | ELK Stack | 8.15.3 |
| Metricas | Prometheus + Grafana | 2.55 / 11.3 |
| Almacenamiento | MinIO (S3 compatible) | RELEASE.2024-10-13 |
| Contenedores | Docker | 24+ |
| Orquestacion | Kubernetes | 1.28+ |

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
                  |   BFF (8080)     |  <- Composicion y circuit breaker para la UI
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
         <-- Prometheus / Grafana (metricas) -->
         <-- MinIO/S3 (reportes PDF) -->
```

## Microservicios y Puertos

| Servicio | Puerto | Base de Datos | Responsabilidad |
|----------|--------|---------------|-----------------|
| Frontend (React + Nginx) | 3000 | (sin BD) | UI |
| BFF | 8080 | (sin BD) | Composicion para frontend |
| API Gateway (Spring Cloud Gateway) | 8081 | (sin BD) | Routing, JWT validation, rate-limit |
| API Gateway (Kong, alternativa) | 8000 (proxy) / 8001 (admin) | (sin BD) | Routing, JWT plugin |
| MS-Auth | 8082 | auth_db (5432) | Login, JWT, gestion de usuarios |
| MS-Academico | 8083 | acad_db (5433) | CRUD notas, generacion PDF |
| MS-Asistencia | 8084 | asist_db (5434) | Registro asistencia, evento inasistencia |
| MS-Mensajeria | 8085 | msg_db (5435) | Mensajes y notificaciones |
| Kafka | 9092 | (broker) | Bus de eventos |
| Elasticsearch | 9200 | (indice) | Storage de logs |
| Kibana | 5601 | (UI) | Visualizacion de logs |
| Prometheus | 9090 | (TSDB) | Metricas |
| Grafana | 3001 | (UI) | Dashboards |
| MinIO | 9000 / 9001 (UI) | (objetos) | Almacenamiento de PDFs |

## Roles RBAC

- ADMIN: gestion de usuarios y configuracion global.
- DOCENTE: registra notas y asistencia, envia mensajes, consulta reportes.
- APODERADO: consulta notas y asistencia de pupilos, recibe notificaciones.

## Requisitos Previos

### Docker Desktop (obligatorio)

El proyecto se orquesta integramente con Docker Compose v2 (sintaxis `docker compose`,
con espacio, no `docker-compose` con guion que esta deprecado). Necesitas
**Docker Desktop 24 o superior** corriendo antes de ejecutar cualquier comando del
proyecto.

| Sistema | Como instalar |
|---------|---------------|
| **Windows 10/11** | Descarga Docker Desktop desde https://www.docker.com/products/docker-desktop/. Habilita WSL 2 si te lo solicita. Al terminar, reinicia el equipo. |
| **macOS (Intel y Apple Silicon)** | Descarga el instalador `.dmg` desde la misma URL. Para Apple Silicon usa el build "Apple Chip". |
| **Linux Ubuntu/Debian** | `sudo apt update && sudo apt install -y docker.io docker-compose-plugin`. Agrega tu usuario al grupo `docker` (`sudo usermod -aG docker $USER`) y reabre la sesion. |

#### Verifica que Docker Desktop esta funcionando

Antes de continuar, abre Docker Desktop y espera a que su icono en la bandeja del
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

- **Windows**: abre Docker Desktop manualmente desde el menu Inicio. Si pide actualizacion WSL, ejecuta en PowerShell con permisos de administrador: `wsl --update`.
- **macOS / Linux**: arranca el daemon (`open -a Docker` en macOS, `sudo systemctl start docker` en Linux).
- **Si dice "command not found"**: Docker Desktop no esta instalado o no esta en el PATH. Reinstala.

#### Recursos minimos recomendados para Docker Desktop

| Recurso | Minimo | Recomendado | Como ajustarlo |
|---------|--------|-------------|----------------|
| RAM | 6 GB | 8 GB | Settings -> Resources -> Memory |
| CPU | 2 vCPU | 4 vCPU | Settings -> Resources -> CPUs |
| Disco | 30 GB libres | 60 GB libres | Settings -> Resources -> Disk image size |

Si abres muchos servicios (toda la pila usa ~20 contenedores) y tienes menos de
6 GB asignados, los procesos Java se quedaran sin memoria y los contenedores
entraran en estado `Restarting`.

### Otras herramientas

| Herramienta | Version minima | Para que se usa | Verificacion |
|-------------|----------------|-----------------|--------------|
| Git | 2.40+ | Clonar el repo | `git --version` |
| Java JDK | 21 LTS | Build local fuera de Docker (opcional) | `java --version` |
| Maven | 3.9+ | Build local fuera de Docker (opcional) | `mvn -version` |
| Node.js | 22 LTS | Frontend en modo dev (opcional) | `node --version` |
| kubectl | 1.28+ | Despliegue Kubernetes (opcional) | `kubectl version --client` |

Java, Maven y Node solo son necesarios si quieres compilar fuera de Docker. La
ruta por contenedores no requiere ninguno de los tres en tu maquina local.

## Instalacion y Ejecucion

### Paso 1. Clonar el repositorio

```bash
git clone git@github.com:WilliamsRT93/libro-digital-fullstack3.git libro-clases
cd libro-clases
```

### Paso 2. Generar las claves RSA para el JWT

MS-Auth firma los tokens con RS256, por lo que necesita un par de claves PEM en
`ms-auth/src/main/resources/keys/`. Elige el metodo segun tu sistema operativo.

#### En Linux, macOS o WSL (con OpenSSL nativo)

```bash
mkdir -p ms-auth/src/main/resources/keys
cd ms-auth/src/main/resources/keys

# 1. BORRAR claves anteriores (si existen) para garantizar regeneracion limpia
rm -f *.pem

# 2. Generar la clave privada en formato PKCS8
openssl genpkey -algorithm RSA -out private_key.pem -pkeyopt rsa_keygen_bits:2048

# 3. Extraer la clave publica en formato X.509
openssl rsa -in private_key.pem -pubout -out public_key.pem

# 4. Verificar que la clave privada esta bien formada
openssl rsa -in private_key.pem -check -noout
echo "OK: claves regeneradas"

cd -
```

#### En Windows con PowerShell (sin instalar OpenSSL)

Aprovechamos que Docker ya esta instalado para el proyecto y usamos un
contenedor temporal con OpenSSL:

```powershell
# 1. Crear la carpeta destino si no existe
New-Item -ItemType Directory -Force -Path "ms-auth\src\main\resources\keys" | Out-Null

# 2. BORRAR claves anteriores (si existen) para garantizar regeneracion limpia
Remove-Item ms-auth\src\main\resources\keys\*.pem -ErrorAction SilentlyContinue

# 3. Generar la clave privada (PKCS8)
docker run --rm -v "${PWD}\ms-auth\src\main\resources\keys:/keys" alpine/openssl `
  genpkey -algorithm RSA -out /keys/private_key.pem -pkeyopt rsa_keygen_bits:2048

# 4. Extraer la clave publica (X.509)
docker run --rm -v "${PWD}\ms-auth\src\main\resources\keys:/keys" alpine/openssl `
  rsa -in /keys/private_key.pem -pubout -out /keys/public_key.pem

# 5. Verificar que la clave privada es valida
docker run --rm -v "${PWD}\ms-auth\src\main\resources\keys:/keys" alpine/openssl `
  rsa -in /keys/private_key.pem -check -noout
Write-Host "OK: claves regeneradas" -ForegroundColor Green
```

Atajo: si quieres ejecutar todo lo anterior en un solo comando, usa el script
incluido `.\scripts\reset-keys.ps1` que ya hace borrado + regeneracion +
verificacion.

#### En Windows con Git Bash

Git for Windows trae OpenSSL incluido. Abre Git Bash (no PowerShell) y ejecuta
los mismos comandos del bloque Linux/macOS de mas arriba.

#### En Windows con OpenSSL instalado nativo

```powershell
choco install openssl -y    # con Chocolatey
# o
scoop install openssl       # con Scoop
# o instalador grafico desde https://slproweb.com/products/Win32OpenSSL.html
```

#### Verificacion (todos los sistemas)

```bash
# En Linux/macOS/Git Bash:
ls ms-auth/src/main/resources/keys/

# En PowerShell:
Get-ChildItem ms-auth\src\main\resources\keys
```

Deben aparecer `private_key.pem` y `public_key.pem`.

### Paso 3. Construir las imagenes Docker

Compila y construye todas las imagenes en una sola pasada:

```bash
docker compose build
```

Esto puede demorar entre 5 y 20 minutos en la primera ejecucion debido a la
descarga de dependencias Maven y npm. En builds posteriores Docker reutiliza
las capas y se reduce a 1-2 minutos por servicio modificado.

### Paso 4. Levantar la pila completa (arranque por etapas)

Para evitar race conditions (especialmente que los microservicios intenten
conectar a Kafka antes de que el broker este listo), usa el script de
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
5. **Resto de microservicios** (Academico, Asistencia, Mensajeria), luego API Gateway, BFF y Frontend

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
esta listo. Presiona Ctrl+C para salir del seguimiento de logs.

### Paso 5. Verificar que todo este saludable

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

### Paso 7. Usuarios de demostracion (precargados)

MS-Auth carga automaticamente tres usuarios de prueba al iniciarse por primera
vez (gracias a `data.sql` con BCrypt factor 12). No necesitas crear usuarios
manualmente para probar la aplicacion.

| Usuario | Contrasenia | Roles | Proposito |
|---------|-------------|-------|-----------|
| `admin1` | `Admin123!` | ADMIN, DOCENTE | Acceso total al sistema |
| `docente1` | `Docente123!` | DOCENTE | Registro de notas y asistencia |
| `apoderado1` | `Apoderado123!` | APODERADO | Consulta de notas y reportes |

Los hashes BCrypt estan en `ms-auth/src/main/resources/data.sql`. Para regenerar
con otras contrasenias, edita ese archivo y reinicia MS-Auth.

Si prefieres crear usuarios adicionales manualmente:

```bash
# Linux / macOS / WSL
docker exec -it postgres-auth psql -U auth_user -d auth_db
```

**Importante en Windows con Git Bash (MINGW64)**: el flag `-it` interactivo
no funciona bien sin TTY. Usa una de estas alternativas:

```bash
# Opcion A: con winpty (Git Bash incluye winpty)
winpty docker exec -it postgres-auth psql -U auth_user -d auth_db

# Opcion B: ejecutar consultas no interactivas con -c
docker exec -i postgres-auth psql -U auth_user -d auth_db -c "SELECT username, full_name FROM users;"

# Opcion C: en PowerShell (TTY funciona normalmente)
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

> **Credenciales correctas** (las que carga automaticamente MS-Auth desde
> `data.sql`):
> `admin1 / Admin123!` - `docente1 / Docente123!` - `apoderado1 / Apoderado123!`

### Comandos utiles

```bash
docker compose stop                # detener preservando datos
docker compose down                # detener y eliminar contenedores (mantiene volumenes)
docker compose down -v             # detener y eliminar TODO incluidos volumenes
docker compose build ms-auth       # reconstruir un servicio
docker compose up -d ms-auth       # levantar/reiniciar un servicio
docker compose logs -f ms-auth     # logs en tiempo real
docker exec -it ms-auth sh         # shell dentro del contenedor
```

### Scripts de utilidad

La carpeta `scripts/` agrupa helpers para tareas frecuentes:

| Script | Plataforma | Proposito |
|--------|-----------|-----------|
| `start-staged.ps1` / `.sh` | Windows / Linux | Arranque por etapas con limpieza de redes |
| `reset-keys.ps1` / `.sh` | Windows / Linux | Borra y regenera las claves RSA del JWT |
| `fix-docker.ps1` | Windows PowerShell | Limpia cache corrupto de Docker y rearma |
| `fix-network.ps1` / `.sh` | Windows / Linux | Resuelve "network not found" del compose |
| `rebuild-microservicios.ps1` / `.sh` | Windows / Linux | Rebuild forzado de los 6 microservicios Java |
| `pull-base-images.ps1` / `.sh` | Windows / Linux | Pre-descarga las 13 imagenes base con reintentos |

Uso tipico:

```powershell
# Regenerar claves del JWT
.\scripts\reset-keys.ps1

# Despues, reconstruir MS-Auth
docker compose build ms-auth
docker compose up -d ms-auth
```

### Frontend React

Ver seccion completa al final del documento.

## Errores Frecuentes

Esta seccion recopila los problemas mas comunes que veras al levantar el proyecto
por primera vez. Lee aqui antes de reportar bugs.

### 1. `Connection refused` al hacer curl al microservicio

**Sintoma**: `curl http://localhost:8082/actuator/health` retorna
`No es posible conectar con el servidor remoto` o `Failed to connect to localhost port 8082`.

**Causas y soluciones**:

| Causa probable | Diagnostico | Solucion |
|----------------|-------------|----------|
| Los contenedores no estan levantados | `docker compose ps` muestra lista vacia | Ejecutar `docker compose up -d` |
| MS-Auth aun esta arrancando | Estado `starting` en `docker compose ps` | Esperar 60 a 120 segundos |
| MS-Auth crasheo al iniciar | Estado `Exited` o `Restarting` | `docker compose logs ms-auth --tail 100` |
| Docker Desktop apagado | `docker --version` falla | Abrir Docker Desktop |
| Firewall bloqueando localhost | Otros puertos tampoco responden | Desactivar temporal o agregar excepcion |

### 2. `version is obsolete` al ejecutar docker compose

**Sintoma**: `WARN[0000] /docker-compose.yml: the attribute version is obsolete`.

**Solucion**: Ya esta corregido en este proyecto. Si lo ves, es porque tienes
una version vieja en cache. Refresca con:

```bash
docker compose down
git pull
docker compose up -d
```

### 3. `command not found: docker-compose` (con guion)

**Sintoma**: `bash: docker-compose: command not found`.

**Solucion**: Docker Compose v1 (con guion) esta deprecado. Usa siempre la
sintaxis nueva con espacio: **`docker compose`**. Viene incluida en Docker
Desktop 24+. Si tienes Docker viejo, actualiza Docker Desktop.

### 4. `error during connect: ... pipe/docker_engine` (Windows)

**Sintoma**: Mensajes como `open //./pipe/docker_engine: The system cannot find the file specified`.

**Causa**: Docker Desktop no esta corriendo.

**Solucion**:

1. Abre Docker Desktop desde el menu Inicio.
2. Espera a que la bandeja muestre "Engine running" (puede tardar 30 a 60 segundos).
3. Verifica con `docker run --rm hello-world`.

### 5. `Cannot start service ms-auth: keys not found` o `private_key.pem`

**Sintoma**: MS-Auth crashea con `FileNotFoundException` apuntando a `private_key.pem`.

**Causa**: No se ejecuto el Paso 2 (generar las claves RSA), o las claves se
generaron pero la imagen Docker se construyo antes y no las incluye.

**Solucion**:

```bash
# 1. Generar las claves (ver Paso 2)
# 2. Reconstruir la imagen
docker compose build ms-auth
docker compose up -d ms-auth
```

### 6. Elasticsearch no inicia / sale `max virtual memory areas vm.max_map_count too low`

**Sintoma**: `elasticsearch` queda en estado `Exited` con codigo 78.

**Solucion**:

- **Linux**: `sudo sysctl -w vm.max_map_count=262144`
- **Windows/macOS**: Editar `Settings -> Resources -> Advanced` en Docker Desktop
  y aumentar la memoria a 6 GB minimo. WSL2 maneja `vm.max_map_count` automaticamente
  si tienes Docker Desktop reciente; si no, en PowerShell de admin: `wsl -d docker-desktop -u root sysctl -w vm.max_map_count=262144`.

### 7. `Port is already allocated`

**Sintoma**: `Bind for 0.0.0.0:5432 failed: port is already allocated`.

**Causa**: Otro Postgres (u otro servicio) esta usando ese puerto en tu maquina.

**Solucion**:

- Detener el servicio que ocupa el puerto, o
- Cambiar el mapping en `docker-compose.yml`. Por ejemplo, cambia
  `"5432:5432"` a `"15432:5432"` y la BD seguira accesible en `localhost:15432`.

```bash
# Identificar quien usa el puerto en Linux/macOS:
lsof -i :5432

# En Windows PowerShell:
Get-NetTCPConnection -LocalPort 5432
```

### 8. Login retorna 401 siempre

**Sintoma**: Devuelve `{"code":"AUTH_001","message":"Invalid credentials"}` aunque
las credenciales son correctas.

**Causas**:

1. El hash BCrypt insertado en `auth_db` no corresponde a la contrasenia.
2. No insertaste el rol en `user_roles` y el JWT viene sin claims utiles.

**Solucion**: Genera un hash BCrypt valido:

```bash
docker run --rm openjdk:21-slim java -e "
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
System.out.println(new BCryptPasswordEncoder(12).encode(\"miClave123\"));
"
```

O bien, levanta una shell en MS-Auth y genera el hash con un endpoint helper.

### 9. `docker compose build` se queda colgado descargando dependencias

**Sintoma**: La etapa `mvn dependency:go-offline -B` parece no progresar.

**Causa**: Maven Central o el mirror configurado puede estar lento o caido.

**Solucion**:

```bash
# Limpiar cache de Docker y reintentar
docker builder prune -f
docker compose build --no-cache ms-auth
```

### 10. `Manifest unknown` o `pull access denied` al hacer build

**Sintoma**: `docker compose build` falla con `manifest for image:tag not found`.

**Causa**: Docker no puede descargar una imagen base. Suele ser porque la version
fijada quedo desactualizada o tu Docker Desktop tiene la version 4.20 o anterior
que no soporta ciertos manifest schemas.

**Solucion**:

1. Actualiza Docker Desktop a la version mas reciente.
2. Si persiste, edita el `Dockerfile` correspondiente y cambia el tag exacto
   por uno mas generico (por ejemplo `21-jre-alpine` en lugar de `21.0.5_11-jre-alpine`).

### 11. Frontend muestra "Network Error" al hacer login

**Sintoma**: La consola del navegador muestra error CORS o `Network Error`.

**Causa**: El BFF no esta corriendo o el frontend no encuentra el proxy.

**Solucion**:

```bash
docker compose ps bff       # debe estar Up
docker compose logs bff     # buscar errores
```

Si reinicias el BFF, espera 30 segundos antes de reintentar el login.

### 12. Kafka se reinicia constantemente

**Sintoma**: `kafka` aparece en estado `Restarting` cada 30 segundos.

**Causa**: Zookeeper no termino de levantar antes de Kafka, o falta memoria.

**Solucion**:

```bash
docker compose down
docker compose up -d zookeeper
sleep 15
docker compose up -d kafka
sleep 20
docker compose up -d
```

### 13. WSL2 ocupa toda la RAM en Windows

**Sintoma**: Tu laptop se vuelve lenta tras varias horas con Docker Desktop abierto.

**Solucion**: Crear `%UserProfile%\.wslconfig` con:

```ini
[wsl2]
memory=6GB
processors=4
swap=2GB
```

Cierra Docker Desktop, ejecuta `wsl --shutdown` en PowerShell y vuelve a abrir
Docker Desktop.

### 14. `vite` no se puede instalar / `EBADENGINE`

**Sintoma**: `npm install` en el frontend falla con error de engine de Node.

**Causa**: Tu Node local es muy antiguo.

**Solucion**: Usa Node 22 LTS o ejecuta el frontend dentro de Docker
(`docker compose up -d frontend`) que ya trae la version correcta.

### 15. `failed commit on ref ... unexpected commit digest`

**Sintoma**: `docker compose up` falla al descargar capas con mensajes como:

```
failed commit on ref "layer-sha256:abc...":
commit failed: unexpected commit digest sha256:xyz...,
expected sha256:abc...: failed precondition
```

**Causa**: una capa quedo corrupta en el cache local de Docker (corte de red,
disco lleno, bug del containerd snapshotter en Docker Desktop).

**Solucion automatica**:

```powershell
# Windows PowerShell desde la raiz del proyecto
.\scripts\fix-docker.ps1
```

**Solucion manual paso a paso**:

```bash
# 1. Bajar todo
docker compose down -v

# 2. Limpiar cache de imagenes y builders
docker system prune -af --volumes
docker builder prune -af

# 3. Reiniciar Docker Desktop completamente
#    (Settings > Troubleshoot > Clean / Purge data, o reiniciar el servicio)

# 4. Pre-descargar imagenes una por una (no en paralelo)
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
snapshotter clasico). Reinicia Docker Desktop y reintenta.

### 16. `npm install` falla con `ERESOLVE` en el frontend

**Sintoma**: durante el build del contenedor frontend aparece
`ERESOLVE could not resolve` o `peer dep` no compatible.

**Solucion**: el `Dockerfile` ya usa `npm install --legacy-peer-deps` para
tolerar conflictos de peer dependencies. Si lo ejecutas localmente, usa el
mismo flag:

```bash
cd frontend
npm install --legacy-peer-deps
npm run dev
```

### 17. `docker exec -it` se cuelga en Git Bash (MINGW64)

**Sintoma**: al ejecutar `docker exec -it postgres-auth psql ...` la consola
queda esperando entrada que nunca llega.

**Causa**: Git Bash en Windows usa MinTTY que no es TTY real. El flag `-it`
no funciona como en Linux.

**Solucion**: usar `winpty` (incluido en Git Bash) o evitar el modo interactivo:

```bash
# Solucion 1: prefijo winpty
winpty docker exec -it postgres-auth psql -U auth_user -d auth_db

# Solucion 2: ejecutar SQL directamente con -c (sin interactividad)
docker exec -i postgres-auth psql -U auth_user -d auth_db -c "SELECT * FROM users;"

# Solucion 3: usar PowerShell en vez de Git Bash
# (en PowerShell el TTY funciona normalmente)
```

### 18. MS-Mensajeria o API Gateway aparecen DOWN aunque levantaron

**Sintoma**: `curl http://localhost:8085/actuator/health` retorna 404 o no
responde, mismo en 8081.

**Causa probable**: el archivo `application.yml` del microservicio no expone
los endpoints de actuator. Spring Boot por defecto solo expone `/actuator/info`.

**Solucion**: ya esta corregido en el proyecto, pero si modificaste algun yml,
asegurate que tenga:

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

### 19. Login del frontend muestra "El servicio de autenticacion no esta disponible"

**Sintoma**: en `http://localhost:3000` el login retorna ese mensaje.

**Causas posibles**:

| Causa | Verificacion | Solucion |
|-------|--------------|----------|
| API Gateway caido | `curl http://localhost:8081/actuator/health` falla | Ver error 18 |
| MS-Auth no termino de iniciar | `docker compose logs ms-auth` | Esperar 30s mas |
| BFF no encuentra Gateway | `docker compose logs bff` muestra `Connection refused` | `docker compose restart bff` |
| Credenciales incorrectas | Estado HTTP 401 | Usar `admin1 / Admin123!` |

El frontend ahora muestra mensajes diferenciados (401 / 503 / 500 / network)
para facilitar el diagnostico.

### 20. `failed to set up container networking: network <id> not found`

**Sintoma**: al levantar la pila uno o mas contenedores fallan con:

```
Error response from daemon: failed to set up container networking:
network 218b51804d31685e5efaf01f27fed965ed4295e4d7ae8045bd53455c112ada66 not found
```

**Causa**: la red `fullstack-3_libro-net` quedo huerfana (eliminada mientras
el compose tenia su ID en cache). Suele pasar cuando:

- Reinicias Docker Desktop con contenedores corriendo.
- Cambias entre proyectos diferentes que usan compose.
- Ejecutas `docker network prune` mientras la pila estaba arriba.

**Solucion automatica**:

```bash
# Linux / macOS / Git Bash
bash scripts/fix-network.sh
```

```powershell
# Windows PowerShell
.\scripts\fix-network.ps1
```

**Solucion manual**:

```bash
docker compose down --remove-orphans
docker network prune -f
docker network rm fullstack-3_libro-net 2>/dev/null || true
docker compose up -d --force-recreate --remove-orphans
```

Si despues de eso sigue fallando: cierra Docker Desktop completamente, borra
el cache (`%APPDATA%\Docker\` en Windows o `~/Library/Containers/com.docker.docker`
en macOS), reinicia Docker Desktop y vuelve a ejecutar `start-staged`.

> **Nota**: el script `start-staged.sh` / `.ps1` ahora hace esta limpieza
> automaticamente en su paso [0/6], por lo que rara vez veras este error
> usando el script.

### 21. Build del frontend falla con `rolldown` o `npm run build` exit code 1

**Sintoma**: el `docker compose build frontend` falla con un stacktrace de
`rolldown` o errores como:

```
at aggregateBindingErrorsIntoJsError ... shared/error-DL-e8-oE.mjs
at #build ... rolldown-build-DSxL8qiP.mjs
process "/bin/sh -c npm run build" did not complete successfully: exit code: 1
```

**Causa**: `package.json` quedo con `vite: ^8.0.10` (version pre-release que
usa rolldown internamente y aun es inestable) o se arrastro un `package-lock.json`
local con dependencias rotas.

**Solucion**:

1. **Bajar Vite a una version estable**. En `frontend/package.json` cambia:

   ```json
   "devDependencies": {
     "@vitejs/plugin-react": "^4.3.4",
     "vite": "^5.4.10"
   }
   ```

2. **Asegurar que el contenedor no copia tu node_modules local**. El proyecto
   ya incluye `frontend/.dockerignore` que excluye `node_modules`, `dist`,
   `package-lock.json` y caches de Vite/npm.

3. **Forzar legacy-peer-deps** via `frontend/.npmrc`:

   ```
   legacy-peer-deps=true
   engine-strict=false
   ```

4. **Reconstruir sin cache**:

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

**Sintoma**: en los logs del microservicio aparece cada 15 segundos:

```
ERROR ... GlobalExceptionHandler ... Unhandled error
NoResourceFoundException: No static resource actuator/prometheus
```

Y Prometheus marca el target como `down`.

**Causa**: el endpoint `/actuator/prometheus` no se materializa solo con
`spring-boot-starter-actuator`. Necesita la dependencia
`micrometer-registry-prometheus` que registra el endpoint y formatea las
metricas en formato OpenMetrics.

**Diagnostico rapido**: si modificaste el `pom.xml` y el error persiste, lo
mas probable es que el contenedor sigue corriendo la imagen antigua. Docker
reusa el cache cuando ve los mismos bytes en el `pom.xml`, asi que un simple
`docker compose up -d` no rehace el build. Necesitas forzar el rebuild.

**Solucion automatica** (recomendada):

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
2. Eliminan sus imagenes locales (`docker rmi -f`).
3. Reconstruyen sin cache (`--no-cache`).
4. Levantan los contenedores nuevos.
5. Verifican `/actuator/prometheus` en los 6 puertos.

**Solucion manual**:

```bash
# Asegurar que el pom.xml tiene la dependencia
grep -A2 "micrometer-registry-prometheus" ms-auth/pom.xml

# Forzar rebuild sin cache
docker compose build --no-cache ms-auth ms-academico ms-asistencia ms-mensajeria api-gateway bff

# Recrear contenedores con la imagen nueva
docker compose up -d --force-recreate ms-auth ms-academico ms-asistencia ms-mensajeria api-gateway bff
```

Verificacion despues del rebuild:

```bash
curl http://localhost:8082/actuator/prometheus | head -3
# Esperado: lineas tipo "# HELP jvm_memory_used_bytes" o "jvm_memory_used_bytes{..."
```

> **Nota adicional**: el `GlobalExceptionHandler` de MS-Auth ahora maneja
> `NoResourceFoundException` con un log en nivel DEBUG (sin stack trace) en
> lugar de ERROR. Asi mientras se hace el rebuild no llenas los logs de
> stack traces de 100+ lineas.

### 23. `TLS handshake timeout` al descargar imagenes Docker

**Sintoma**: el `docker compose build` falla con:

```
failed to do request: Head "https://registry-1.docker.io/v2/library/maven/manifests/...":
net/http: TLS handshake timeout
```

Y normalmente afecta a una imagen base como `maven:3.9.9-eclipse-temurin-21`,
mientras que otras (frontend) ya estan en cache y siguen.

**Causas frecuentes**:

- Conexion a internet inestable o saturada.
- VPN corporativa que interfiere con Docker.
- DNS de Docker mal configurado.
- Docker Hub temporalmente lento (suele resolverse en minutos).
- Antivirus o firewall bloqueando trafico TLS de Docker.

**Solucion 1 - script con reintentos** (la mas simple):

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

El script descarga manualmente las 13 imagenes base con hasta 5 reintentos
cada una. Una vez en cache local, el build no necesita ir a Docker Hub.

**Solucion 2 - configurar mirror de Docker Hub**

Si el problema persiste (red corporativa, pais con latencia alta), edita
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

**Solucion 3 - desactivar VPN temporalmente**

VPN corporativas suelen romper el TLS handshake con `registry-1.docker.io`.
Si tu empresa usa Cisco AnyConnect, GlobalProtect, OpenVPN, etc., apagala
solo durante el `docker pull`/`docker compose build`. Despues puedes
reactivarla.

**Solucion 4 - reintentar simplemente**

Estos errores suelen ser transitorios. Volver a ejecutar `docker compose build`
30 a 60 segundos despues funciona en muchos casos.

### 24. `Premature end of Content-Length` en build de Maven

**Sintoma**: durante `docker compose build` un microservicio Java falla con:

```
Could not transfer artifact org.rocksdb:rocksdbjni:jar:7.9.2
from/to central (https://repo.maven.apache.org/maven2):
Premature end of Content-Length delimited message body
(expected: 58,000,372; received: 30,433,856)
```

**Causa**: la conexion a Maven Central se corto a mitad de la descarga del JAR
de `rocksdbjni` (58 MB, requerido por Spring Kafka). Es comun con redes lentas,
saturadas, o con VPN corporativa.

**Soluciones**:

1. **Configuracion robusta en Dockerfile** (ya aplicada en el proyecto). Todos
   los Dockerfile Java tienen ahora:
   - `MAVEN_OPTS` con timeouts largos (5 min connect, 15 min request) y
     hasta 10 reintentos por descarga.
   - Comando `mvn -B dependency:go-offline` ejecutado **3 veces seguidas** con
     `||` para que el build tolere fallos transitorios.
   - **BuildKit cache mount** `--mount=type=cache,target=/root/.m2` que
     persiste el repositorio Maven entre builds. Asi `rocksdbjni` solo se
     descarga una vez y nunca mas.

2. **Habilitar BuildKit** (Docker Desktop 24+ ya lo tiene por default). Si no:

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

3. **Pre-descargar la dependencia problematica fuera de Docker** y montarla
   como volumen. Si tienes Maven local:

   ```bash
   mvn -f ms-auth/pom.xml dependency:get -Dartifact=org.rocksdb:rocksdbjni:7.9.2
   ```

4. **Usar mirror de Maven**. Crea `~/.m2/settings.xml` (en tu maquina) con un
   mirror mas cercano, por ejemplo Aliyun (Asia) o cualquier mirror corporativo:

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

5. **Reintentar simplemente**. El cache mount hace que solo lo que falto se
   re-descargue. Tras una primera descarga exitosa parcial, el siguiente
   `docker compose build` retoma desde donde quedo.

### 25. Despues de `docker compose down -v` perdi mis usuarios

**Sintoma**: Despues de bajar la pila con `-v`, los usuarios creados desaparecieron.

**Causa**: La opcion `-v` borra los volumenes nominados, incluyendo los datos
de Postgres.

**Solucion**: No es un error, es lo esperado. Para preservar datos usa solo
`docker compose down` (sin `-v`). Para resetear el entorno desde cero, `down -v`
es el comando correcto.

## Pruebas Unitarias

El proyecto incluye **15 pruebas** distribuidas en 4 microservicios: 13 pruebas unitarias con
Mockito (sin levantar contexto Spring) y 2 pruebas de integracion Kafka con broker embebido.
Todas usan JUnit 5 + AssertJ.

### Inventario de pruebas

| # | Microservicio | Clase | Descripcion |
|---|---------------|-------|-------------|
| 1 | ms-auth | `AuthServiceTest` | Login exitoso retorna token y datos del usuario |
| 2 | ms-auth | `AuthServiceTest` | Password incorrecto lanza `BadCredentialsException` |
| 3 | ms-auth | `AuthServiceTest` | Usuario deshabilitado lanza `BadCredentialsException` sin verificar password |
| 4 | ms-auth | `UserAdminServiceTest` | Crear usuario nuevo persiste y retorna `UserResponse` correcto |
| 5 | ms-auth | `UserAdminServiceTest` | Username duplicado lanza `409 CONFLICT` antes de llegar a la BD |
| 6 | ms-auth | `UserAdminServiceTest` | Listar usuarios retorna lista completa mapeada a DTO |
| 7 | ms-auth | `UserAdminServiceTest` | Eliminar ID inexistente lanza `404 NOT FOUND` sin llamar a `deleteById` |
| 8 | ms-auth | `UserAdminServiceTest` | Actualizar roles persiste el nuevo conjunto de roles |
| 9 | ms-asistencia | `AsistenciaServiceTest` | Estado PRESENTE no publica ningun evento en Kafka |
| 10 | ms-asistencia | `AsistenciaServiceTest` | Estado AUSENTE publica `InasistenciaEvent` en el topic correcto |
| 11 | ms-academico | `NotaServiceTest` | Crear nota valida persiste entidad y retorna DTO correcto |
| 12 | ms-academico | `NotaServiceTest` | Consultar notas de alumno retorna lista ordenada |
| 13 | ms-academico | `NotaServiceTest` | Alumno sin notas retorna lista vacia (sin NullPointerException) |
| 14 | ms-asistencia | `KafkaZookeeperIntegrationTest` | Broker Kafka con Zookeeper embebido esta activo y accesible |
| 15 | ms-asistencia | `KafkaZookeeperIntegrationTest` | Ciclo completo productor -> topic -> consumidor entrega mensaje intacto |

### Como ejecutar las pruebas

#### Todos los microservicios a la vez (desde la raiz del proyecto)

```bash
mvn -pl ms-auth,ms-academico,ms-asistencia test
```

#### Por microservicio individual

```bash
# Solo MS-Auth (pruebas 1-8)
mvn -f ms-auth/pom.xml test

# Solo MS-Academico (pruebas 11-13)
mvn -f ms-academico/pom.xml test

# Solo MS-Asistencia (pruebas 9-10 + 14-15)
mvn -f ms-asistencia/pom.xml test
```

#### Solo una clase especifica

```bash
mvn -f ms-auth/pom.xml test -Dtest=AuthServiceTest
mvn -f ms-auth/pom.xml test -Dtest=UserAdminServiceTest
mvn -f ms-asistencia/pom.xml test -Dtest=AsistenciaServiceTest
mvn -f ms-asistencia/pom.xml test -Dtest=KafkaZookeeperIntegrationTest
mvn -f ms-academico/pom.xml test -Dtest=NotaServiceTest
```

#### Solo un test especifico

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

Una ejecucion exitosa completa muestra al final de cada modulo:

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

Para MS-Asistencia, la prueba de integracion Kafka tarda entre 5 y 15 segundos en levantar
el broker embebido; es normal ver logs de Kafka durante ese tiempo:

```
[INFO] Running com.colegio.asistencia.service.AsistenciaServiceTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.colegio.asistencia.kafka.KafkaZookeeperIntegrationTest
... (logs de EmbeddedKafka durante ~10s) ...
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Resumen consolidado al ejecutar los tres modulos juntos: **15 pruebas, 0 fallos, 0 errores**.

### Errores comunes en pruebas y soluciones

#### Error 1: `No Java compiler is provided in this environment`

```
[ERROR] Failed to execute goal ... compile ... No compiler is provided in this environment.
Perhaps you are running on a JRE rather than a JDK?
```

**Causa**: Maven no encuentra el JDK, solo hay JRE instalado.

**Solucion**:
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

**Causa**: el codigo del servicio tiene cambios que no reflejan los tests, o los tests
estan fuera de la estructura de paquetes correcta.

**Solucion**:
```bash
mvn -f ms-academico/pom.xml clean test
```

Si el error persiste, verificar que los tests estan en
`src/test/java/com/colegio/<microservicio>/...` y no en otro directorio.

#### Error 3: `Mockito cannot mock this class`

```
[ERROR] org.mockito.exceptions.base.MockitoException:
Cannot mock/spy class com.colegio.auth.repository.UserRepository
```

**Causa**: version incompatible de Mockito o byte-buddy por declarar Mockito manualmente
en el `pom.xml` ademas de `spring-boot-starter-test`.

**Solucion**: No declarar Mockito manualmente; Spring Boot ya lo incluye en la version
correcta via `spring-boot-starter-test`. Verificar que no hay exclusiones accidentales:

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

**Causa**: el broker Kafka de Docker Compose esta corriendo en `localhost:9092` y
`@EmbeddedKafka` intenta usar el mismo puerto.

**Solucion**: detener el broker Kafka de Docker antes de correr la prueba:

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

**Causa**: el broker embebido tardo mas de 10 segundos en levantar. Ocurre en maquinas
con poca RAM o en el primer build cuando los JARs de Kafka no estan en cache.

**Solucion**: ejecutar el test una segunda vez; la cache de la JVM lo resuelve:

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
que no fue usado por todos los metodos de test.

**Solucion**: mover el stub al test especifico que lo necesita, o anotar la clase con
`@MockitoSettings` para relajar la estrictez solo donde sea necesario:

```java
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class AuthServiceTest { ... }
```

## Frontend React

El frontend es una SPA con React 18 + Vite, ruteo con react-router-dom, sesion en
sessionStorage y un cliente axios centralizado. Se integra con el BFF a traves del
prefijo `/bff` (proxy de Vite en desarrollo y proxy de Nginx en produccion).

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
    ├── styles/global.css      # Sistema de diseno (variables, componentes base)
    ├── api/client.js          # Cliente axios con interceptores
    ├── auth/session.js        # Helpers de sesion JWT
    ├── components/
    │   ├── Layout.jsx         # Header con navegacion y datos del usuario
    │   └── ProtectedRoute.jsx # HOC que protege rutas privadas
    └── pages/
        ├── Login.jsx          # Formulario de login con validaciones
        ├── Dashboard.jsx      # Contenedor con sub-rutas
        ├── Home.jsx           # Tiles de accesos rapidos por rol
        ├── Asistencia.jsx     # Registro de asistencia con historial local
        ├── Notas.jsx          # Captura de notas (escala 1.0 a 7.0)
        └── Reportes.jsx       # Descarga de PDF
```

### Modo desarrollo (con hot-reload)

Util cuando estas iterando sobre la UI sin tener que reconstruir el contenedor.
Requiere Node.js 22 LTS instalado y el BFF corriendo (Docker o local).

```bash
cd frontend
npm install
npm run dev
# http://localhost:3000 con hot-reload
```

### Build de produccion local

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

| Modulo | Roles permitidos |
|--------|------------------|
| Asistencia | DOCENTE, ADMIN |
| Notas | DOCENTE, ADMIN |
| Reportes PDF | DOCENTE, ADMIN, APODERADO |
| Gestion de Usuarios | ADMIN |

### Gestion de usuarios (solo ADMIN)

El usuario `admin1` puede crear nuevos docentes y apoderados desde el frontend
(`Dashboard -> Usuarios`) o por API. Los endpoints estan protegidos con
`@PreAuthorize("hasRole('ADMIN')")` y validados en MS-Auth.

Ejemplo de creacion via API:

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

Endpoints expuestos por MS-Auth para gestion (todos protegidos por ROLE_ADMIN):

| Metodo | Path | Proposito |
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

#### 1. Construir y publicar todas las imagenes

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

> Nota sobre el nombre de la imagen: Docker Compose nombra las imagenes
> construidas como `<carpeta-padre>-<servicio>:latest`. Si tu carpeta raiz
> no se llama `fullstack-3`, ajusta el prefijo. Verifica con `docker images`.

#### 2. Crear namespace, secrets y aplicar manifests

```bash
kubectl apply -f k8s/namespace.yaml

# Los Secrets con credenciales de Postgres ya estan dentro de k8s/postgres.yaml
# (usando stringData). Para produccion reemplazalos con un secret manager
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
├── diagram-architecture.drawio.xml
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

- `main`: releases estables, taggeadas (vX.Y.Z).
- `develop`: integracion continua de features.
- `feature/<ticket>-descripcion`: nuevas funcionalidades.
- `release/X.Y.Z`: estabilizacion previa al deploy a main.
- `hotfix/<ticket>`: correcciones criticas en produccion.

Convencion de commits: Conventional Commits (`feat:`, `fix:`, `docs:`, `chore:`, `refactor:`).

## Justificacion Tecnica

**Spring Boot + JPA**: estandar empresarial Java, reduce boilerplate, ecosistema maduro.

**Database-per-Service**: evita acoplamiento de esquema, permite escalar y versionar
cada microservicio de forma independiente (Bounded Context, DDD).

**JWT RS256**: firma asimetrica permite que el Gateway valide tokens sin compartir
el secreto. MS-Auth firma con clave privada; los demas validan con la publica.

**Kafka**: desacopla productores de consumidores, persiste eventos, permite reproducir.

**API Gateway + BFF**: el Gateway centraliza cross-cutting concerns. El BFF compone
respuestas para la UI. Se ofrecen dos implementaciones (SCG y Kong).

**Resilience4j**: circuit breaker, retry y bulkhead modernos con buena integracion
en Micrometer.

**ELK + Prometheus/Grafana**: separacion logs/metricas siguiendo mejores practicas
de observabilidad.

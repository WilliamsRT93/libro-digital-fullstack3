-- =============================================================================
-- Seeds para MS-Auth - usuarios de demostracion
-- Se ejecuta automaticamente al iniciar MS-Auth despues que Hibernate crea las
-- tablas (gracias a spring.jpa.defer-datasource-initialization=true).
-- Idempotente: ON CONFLICT DO NOTHING evita duplicados en reinicios.
-- =============================================================================

-- Usuario administrador
-- Credenciales: admin1 / Admin123!
INSERT INTO users (username, password_hash, full_name, enabled, created_at)
VALUES ('admin1',
        '$2b$12$KN.i6isRGqAFRcKXUp3PYu5p7OF17ib5uEdLTr.B2aMFcp4bZ2pG.',
        'Administrador Demo', true, NOW())
ON CONFLICT (username) DO NOTHING;

-- Usuario docente
-- Credenciales: docente1 / Docente123!
INSERT INTO users (username, password_hash, full_name, enabled, created_at)
VALUES ('docente1',
        '$2b$12$rMalDL7T6O5TMzzFmlVaROS3ZIUlab7AYL1QJH93R50DckC2Ncr3e',
        'Docente Demo', true, NOW())
ON CONFLICT (username) DO NOTHING;

-- Usuario apoderado
-- Credenciales: apoderado1 / Apoderado123!
INSERT INTO users (username, password_hash, full_name, enabled, created_at)
VALUES ('apoderado1',
        '$2b$12$kpij0830pI3JQkyMhLQmteSUGrqRsj6.4VuqDjzqhRJhDnXOHh.IK',
        'Apoderado Demo', true, NOW())
ON CONFLICT (username) DO NOTHING;

-- Asignacion de roles (referenciados por sub-query para no depender de IDs fijos)
INSERT INTO user_roles (user_id, role)
SELECT id, 'ADMIN' FROM users WHERE username = 'admin1'
  AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = users.id AND ur.role = 'ADMIN');

INSERT INTO user_roles (user_id, role)
SELECT id, 'DOCENTE' FROM users WHERE username = 'admin1'
  AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = users.id AND ur.role = 'DOCENTE');

INSERT INTO user_roles (user_id, role)
SELECT id, 'DOCENTE' FROM users WHERE username = 'docente1'
  AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = users.id AND ur.role = 'DOCENTE');

INSERT INTO user_roles (user_id, role)
SELECT id, 'APODERADO' FROM users WHERE username = 'apoderado1'
  AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = users.id AND ur.role = 'APODERADO');

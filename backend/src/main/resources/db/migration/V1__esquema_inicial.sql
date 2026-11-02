-- =========================================================
-- V1: esquema inicial de BioHub (sale del diagrama de clases)
-- =========================================================

-- ---------- Seguridad y acceso ----------
CREATE TABLE rol (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre              VARCHAR(50) NOT NULL UNIQUE,
    nivel_autorizacion  VARCHAR(20) NOT NULL
        CHECK (nivel_autorizacion IN ('PUBLICO','INVESTIGADOR','AUDITOR','DIRECTOR','ALTO_RANGO','ADMINISTRADOR'))
);

CREATE TABLE permiso (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo       VARCHAR(50) NOT NULL UNIQUE,
    descripcion  VARCHAR(255)
);

CREATE TABLE rol_permiso (
    rol_id      UUID NOT NULL REFERENCES rol(id),
    permiso_id  UUID NOT NULL REFERENCES permiso(id),
    PRIMARY KEY (rol_id, permiso_id)
);

CREATE TABLE usuario (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre           VARCHAR(100) NOT NULL,
    correo           VARCHAR(150) NOT NULL UNIQUE,
    contrasena_hash  VARCHAR(100) NOT NULL,
    activo           BOOLEAN NOT NULL DEFAULT TRUE,
    rol_id           UUID NOT NULL REFERENCES rol(id)
);

CREATE TABLE investigador (
    usuario_id    UUID PRIMARY KEY REFERENCES usuario(id),
    especialidad  VARCHAR(100)
);

CREATE TABLE intento_acceso_no_autorizado (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fecha_hora          TIMESTAMPTZ NOT NULL DEFAULT now(),
    recurso_solicitado  VARCHAR(255) NOT NULL,
    ip_origen           VARCHAR(45),
    usuario_id          UUID NOT NULL REFERENCES usuario(id)
);

-- ---------- Taxonomía ----------
CREATE TABLE tipo_especie (
    id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre  VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE definicion_atributo (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo_especie_id  UUID NOT NULL REFERENCES tipo_especie(id),
    nombre           VARCHAR(100) NOT NULL,
    tipo_dato        VARCHAR(10) NOT NULL CHECK (tipo_dato IN ('NUMERO','TEXTO','FECHA','ARCHIVO')),
    obligatorio      BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (tipo_especie_id, nombre)
);

-- ---------- Hallazgos ----------
CREATE TABLE hallazgo (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fecha_registro   TIMESTAMPTZ NOT NULL DEFAULT now(),
    expedicion       VARCHAR(150),
    investigador_id  UUID NOT NULL REFERENCES investigador(usuario_id)
);

CREATE TABLE ficha_taxonomica (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hallazgo_id      UUID NOT NULL REFERENCES hallazgo(id),
    numero_version   INT  NOT NULL CHECK (numero_version >= 1),
    tipo_especie_id  UUID NOT NULL REFERENCES tipo_especie(id),
    reino            VARCHAR(100),
    filo             VARCHAR(100),
    clase            VARCHAR(100),
    orden            VARCHAR(100),
    familia          VARCHAR(100),
    genero           VARCHAR(100),
    especie          VARCHAR(100),
    fecha_version    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (hallazgo_id, numero_version)
);

CREATE TABLE valor_atributo (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ficha_id                UUID NOT NULL REFERENCES ficha_taxonomica(id),
    definicion_atributo_id  UUID NOT NULL REFERENCES definicion_atributo(id),
    valor                   TEXT NOT NULL,
    UNIQUE (ficha_id, definicion_atributo_id)
);

CREATE TABLE ubicacion (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hallazgo_id        UUID NOT NULL UNIQUE REFERENCES hallazgo(id),
    latitud            DOUBLE PRECISION NOT NULL CHECK (latitud BETWEEN -90 AND 90),
    longitud           DOUBLE PRECISION NOT NULL CHECK (longitud BETWEEN -180 AND 180),
    region             VARCHAR(100) NOT NULL,
    departamento       VARCHAR(100) NOT NULL,
    especie_protegida  BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE evidencia (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hallazgo_id  UUID NOT NULL REFERENCES hallazgo(id),
    tipo         VARCHAR(15) NOT NULL CHECK (tipo IN ('FOTOGRAFIA','MUESTRA','NOTA_CAMPO','AUDIO')),
    url_archivo  VARCHAR(255) NOT NULL,
    descripcion  VARCHAR(255),
    fecha_carga  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE registro_cambio (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fecha_hora           TIMESTAMPTZ NOT NULL DEFAULT now(),
    justificacion        TEXT NOT NULL CHECK (length(trim(justificacion)) > 0),
    autorizado_por       UUID NOT NULL REFERENCES usuario(id),
    version_anterior_id  UUID NOT NULL REFERENCES ficha_taxonomica(id),
    version_nueva_id     UUID NOT NULL UNIQUE REFERENCES ficha_taxonomica(id)
);

-- ---------- Equipos de campo ----------
CREATE TABLE equipo_campo (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo_identificacion  VARCHAR(50) NOT NULL UNIQUE,
    tipo                   VARCHAR(15) NOT NULL CHECK (tipo IN ('DRON','CAMARA_TRAMPA','HIDROFONO','SENSOR')),
    estado                 VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    investigador_id        UUID NOT NULL REFERENCES investigador(usuario_id)
);

-- ---------- Integración y reportes ----------
CREATE TABLE consumidor_externo (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organizacion       VARCHAR(150) NOT NULL,
    tipo               VARCHAR(30) NOT NULL,
    api_key            VARCHAR(100) NOT NULL UNIQUE,
    limite_por_minuto  INT NOT NULL DEFAULT 60 CHECK (limite_por_minuto BETWEEN 60 AND 1000)
);

CREATE TABLE reporte (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo              VARCHAR(50) NOT NULL,
    parametros        VARCHAR(255) NOT NULL,
    fecha_generacion  TIMESTAMPTZ,
    solicitado_por    UUID REFERENCES usuario(id)
);

CREATE TABLE reporte_presion (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporte_id            UUID NOT NULL REFERENCES reporte(id),
    departamento          VARCHAR(100) NOT NULL,
    total_hallazgos       INT NOT NULL DEFAULT 0,
    hallazgos_protegidos  INT NOT NULL DEFAULT 0,
    variacion_porcentual  NUMERIC(6,2),
    en_alerta             BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (reporte_id, departamento)
);
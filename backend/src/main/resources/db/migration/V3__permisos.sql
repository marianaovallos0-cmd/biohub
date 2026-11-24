-- ===== api-interna: lee y escribe todo... =====
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO biohub_interna;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO biohub_interna;

-- ...menos editar o borrar la historia y la auditoría (ASR-4 y ASR-5)
REVOKE UPDATE, DELETE ON ficha_taxonomica, valor_atributo,
                         registro_cambio, intento_acceso_no_autorizado
    FROM biohub_interna;

-- La tabla de control de Flyway no es de la aplicación
REVOKE ALL ON flyway_schema_history FROM biohub_interna;

-- ===== api-pública: solo la vista y los consumidores externos (ASR-1) =====
GRANT SELECT ON vista_publica_hallazgo TO biohub_publica;
GRANT SELECT ON consumidor_externo     TO biohub_publica;

-- ===== analítica: lee hallazgos sin coordenadas y escribe reportes (ASR-6) =====
GRANT SELECT ON hallazgo, ficha_taxonomica, tipo_especie TO biohub_analitica;
GRANT SELECT (id, hallazgo_id, region, departamento, especie_protegida)
    ON ubicacion TO biohub_analitica;
GRANT SELECT, INSERT, UPDATE ON reporte TO biohub_analitica;
GRANT SELECT, INSERT         ON reporte_presion TO biohub_analitica;
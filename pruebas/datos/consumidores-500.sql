-- =========================================================
-- HU-16: crea 500 consumidores externos para la prueba de carga.
-- SOLO para desarrollo. No se carga solo: se corre a mano antes de la prueba.
-- Las claves van de carga-hu16-001 a carga-hu16-500.
-- Nginx les da el límite por defecto de 60 peticiones por minuto (clientes.conf).
-- Si se corre dos veces, primero borra la carga anterior.
-- =========================================================
BEGIN;

DELETE FROM consumidor_externo WHERE api_key LIKE 'carga-hu16-%';

INSERT INTO consumidor_externo (organizacion, tipo, api_key, limite_por_minuto)
SELECT 'Consumidor de carga ' || i,
       CASE WHEN i % 2 = 0 THEN 'UNIVERSIDAD' ELSE 'ONG' END,
       'carga-hu16-' || lpad(i::text, 3, '0'),
       60
  FROM generate_series(1, 500) AS i;

COMMIT;

SELECT count(*) AS consumidores_de_carga FROM consumidor_externo WHERE api_key LIKE 'carga-hu16-%';

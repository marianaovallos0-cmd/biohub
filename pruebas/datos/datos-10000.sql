-- =========================================================
-- HU-11: carga 10.000 hallazgos de prueba para la prueba de carga.
-- SOLO para desarrollo. No se carga solo: se corre a mano antes de la prueba.
-- Llevan la expedición 'Carga HU-11'. También se borran los 'Prueba HU-11' que crean las pruebas k6.
-- Si se corre dos veces, primero borra la carga anterior.
-- =========================================================
BEGIN;

-- 1. Borrar una carga anterior (en orden, por las llaves foráneas)
DELETE FROM valor_atributo
 WHERE ficha_id IN (SELECT f.id FROM ficha_taxonomica f
                      JOIN hallazgo h ON h.id = f.hallazgo_id
                     WHERE h.expedicion IN ('Carga HU-11', 'Prueba HU-11'));
DELETE FROM ficha_taxonomica WHERE hallazgo_id IN (SELECT id FROM hallazgo WHERE expedicion IN ('Carga HU-11', 'Prueba HU-11'));
DELETE FROM ubicacion        WHERE hallazgo_id IN (SELECT id FROM hallazgo WHERE expedicion IN ('Carga HU-11', 'Prueba HU-11'));
DELETE FROM hallazgo         WHERE expedicion IN ('Carga HU-11', 'Prueba HU-11');

-- 2. Las 8 especies que se van a repetir, con su lugar
--    Tipo 1 = Anfibio, tipo 2 = Ave (los de R__datos_prueba.sql)
CREATE TEMP TABLE especies_carga ON COMMIT DROP AS
SELECT * FROM (VALUES
  (0, '30000000-0000-0000-0000-000000000001'::uuid, 'Amphibia', 'Anura',          'Dendrobatidae',   'Oophaga',    'histrionica', 'Pacífica', 'Chocó',      5.69, -76.66),
  (1, '30000000-0000-0000-0000-000000000001'::uuid, 'Amphibia', 'Anura',          'Bufonidae',       'Atelopus',   'nahumae',     'Caribe',   'Magdalena', 11.10, -74.01),
  (2, '30000000-0000-0000-0000-000000000001'::uuid, 'Amphibia', 'Anura',          'Phyllomedusidae', 'Agalychnis', 'callidryas',  'Pacífica', 'Valle',      3.88, -77.03),
  (3, '30000000-0000-0000-0000-000000000002'::uuid, 'Aves',     'Psittaciformes', 'Psittacidae',     'Ara',        'macao',       'Amazonía', 'Amazonas',  -4.21, -69.94),
  (4, '30000000-0000-0000-0000-000000000002'::uuid, 'Aves',     'Piciformes',     'Ramphastidae',    'Ramphastos', 'brevis',      'Pacífica', 'Chocó',      5.50, -76.80),
  (5, '30000000-0000-0000-0000-000000000002'::uuid, 'Aves',     'Cathartiformes', 'Cathartidae',     'Vultur',     'gryphus',     'Andina',   'Boyacá',     6.00, -72.30),
  (6, '30000000-0000-0000-0000-000000000002'::uuid, 'Aves',     'Passeriformes',  'Cotingidae',      'Rupicola',   'peruvianus',  'Andina',   'Antioquia',  6.25, -75.56),
  (7, '30000000-0000-0000-0000-000000000002'::uuid, 'Aves',     'Galliformes',    'Cracidae',        'Crax',       'alberti',     'Caribe',   'Bolívar',    8.10, -74.60)
) AS e(n, tipo, clase, orden, familia, genero, especie, region, departamento, lat, lon);

-- 3. Los 10.000 hallazgos: cada número i toma la especie i % 8
CREATE TEMP TABLE carga ON COMMIT DROP AS
SELECT gen_random_uuid() AS hallazgo_id, gen_random_uuid() AS ficha_id, i, e.*
  FROM generate_series(1, 10000) AS i
  JOIN especies_carga e ON e.n = i % 8;

INSERT INTO hallazgo (id, fecha_registro, expedicion, investigador_id)
SELECT hallazgo_id,
       now() - make_interval(mins => i),
       'Carga HU-11',
       CASE WHEN i % 2 = 0 THEN '20000000-0000-0000-0000-000000000001'
                           ELSE '20000000-0000-0000-0000-000000000002' END::uuid
  FROM carga;

INSERT INTO ficha_taxonomica (id, hallazgo_id, numero_version, tipo_especie_id,
                              reino, filo, clase, orden, familia, genero, especie)
SELECT ficha_id, hallazgo_id, 1, tipo, 'Animalia', 'Chordata', clase, orden, familia, genero, especie
  FROM carga;

-- El atributo obligatorio de cada tipo: temperatura del agua (anfibio) o ruta GPS (ave)
INSERT INTO valor_atributo (ficha_id, definicion_atributo_id, valor)
SELECT ficha_id,
       CASE WHEN clase = 'Amphibia' THEN '31000000-0000-0000-0000-000000000001'
                                    ELSE '31000000-0000-0000-0000-000000000004' END::uuid,
       CASE WHEN clase = 'Amphibia' THEN (18 + i % 10)::text
                                    ELSE 'rutas/carga-' || i || '.gpx' END
  FROM carga;

-- La ubicación varía un poco para que no sean todas el mismo punto
INSERT INTO ubicacion (hallazgo_id, latitud, longitud, region, departamento, especie_protegida)
SELECT hallazgo_id, lat + (i % 100) / 1000.0, lon + (i % 100) / 1000.0, region, departamento, i % 5 = 0
  FROM carga;

COMMIT;

-- 4. Comprobar
SELECT count(*) AS hallazgos_en_total FROM hallazgo;

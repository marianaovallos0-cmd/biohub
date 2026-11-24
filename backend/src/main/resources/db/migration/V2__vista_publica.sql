-- Lo único que api-pública puede leer de los hallazgos.
-- No tiene latitud ni longitud (ASR-1).
CREATE VIEW vista_publica_hallazgo AS
SELECT DISTINCT ON (h.id)
    h.id             AS hallazgo_id,
    h.fecha_registro,
    te.nombre        AS tipo_especie,
    f.reino, f.filo, f.clase, f.orden, f.familia, f.genero, f.especie,
    u.region,
    u.departamento
FROM hallazgo h
JOIN ficha_taxonomica f ON f.hallazgo_id = h.id
JOIN tipo_especie te    ON te.id = f.tipo_especie_id
LEFT JOIN ubicacion u   ON u.hallazgo_id = h.id
ORDER BY h.id, f.numero_version DESC;
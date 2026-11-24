-- HU-18: un solo reporte por tipo y mes. Si el director lo pide otra vez, se usa el que ya existe
ALTER TABLE reporte ADD CONSTRAINT reporte_tipo_periodo_unico UNIQUE (tipo, parametros);

-- La variación puede pasar de 9999 % (de 1 hallazgo a 150 es 14900 %) y NUMERIC(6,2) no alcanza
ALTER TABLE reporte_presion ALTER COLUMN variacion_porcentual TYPE NUMERIC(9,2);
CREATE DATABASE IF NOT EXISTS TAP;
USE TAP;

DROP TABLE IF EXISTS info_general_envio;
CREATE TABLE info_general_envio(
	gen_albaran VARCHAR(12) NOT NULL,
	gen_fecha_envio DATETIME ,
	gen_lugar_envio VARCHAR(40),
	gen_fecha_entrega DATETIME,
	gen_lugar_entrega VARCHAR(40),
    PRIMARY KEY (gen_albaran)
);

DROP TABLE IF EXISTS distancia_tiempo_envio;
CREATE TABLE distancia_tiempo_envio(
	env_albaran VARCHAR(12) NOT NULL,
	env_distancia DOUBLE ,
	env_tiempo DOUBLE,
    PRIMARY KEY (env_albaran)
);
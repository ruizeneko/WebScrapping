package edu.ehu.tap.ScrappingTool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Esta clase escribe en la BBDD la informacion de un objeto InformacionEnvio
 * @author Eneko Ruiz
 */
public class DatabaseTransfer {

    private final String dbUrl;
    private final String username;
    private final String password;
    private final String dbName;

    /**
     * Constructor
     * @param dbUrl Ubicacion de la BBDD (en nuestro caso, en nuestro local)
     * @param username Usuario
     * @param password Contraseña
     * @param tableName Nombre de la tabla
     */
    public DatabaseTransfer(String dbUrl, String username, String password, String tableName) {
        this.dbUrl = dbUrl;
        this.username = username;
        this.password = password;
        this.dbName = tableName;
    }

    /**
     * Metodo que inserta en la BBDD la informacion de un objeto InformacionEnvio, tras comprobar que todos su valores
     * son diferentes de NULL
     * @param infoEnvio Objeto InformacionEnvio con los datos del envio
     * @throws SQLException si hay algun problema durante la insercion (por ejemplo, una PRIMARY KEY VIOLATION) o durante
     * la conexion con la BBDD
     */
    public void connectionAndInsertion(InformacionEnvio infoEnvio) throws SQLException {
        if (infoEnvio.getFechaEnvio() != null && infoEnvio.getFechaEntrega() != null && infoEnvio.getLugarEntrega() != null
                && infoEnvio.getLugarEnvio() != null) {
            Connection connection = DriverManager.getConnection(dbUrl, username, password);
            java.sql.Timestamp envio = new java.sql.Timestamp(infoEnvio.getFechaEnvio().getTime());
            java.sql.Timestamp entrega = new java.sql.Timestamp(infoEnvio.getFechaEntrega().getTime());

            String query = " insert into " + dbName + "(gen_albaran, gen_fecha_envio, gen_lugar_envio, gen_fecha_entrega, gen_lugar_entrega)"
                    + " values(?, ?, ?, ?, ?);";

            PreparedStatement preparedStmt = connection.prepareStatement(query);
            // create the mysql insert preparedstatement
            preparedStmt.setString(1, infoEnvio.getNumeroAlbaran());
            preparedStmt.setTimestamp(2, envio);
            preparedStmt.setString(3, infoEnvio.getLugarEnvio());
            preparedStmt.setTimestamp(4, entrega);
            preparedStmt.setString(5, infoEnvio.getLugarEntrega());

            preparedStmt.execute();

        }
    }
}

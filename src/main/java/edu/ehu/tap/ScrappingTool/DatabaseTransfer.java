package edu.ehu.tap.ScrappingTool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author Eneko Ruiz
 */
public class DatabaseTransfer {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseTransfer.class);


    private final String dbUrl;
    private final String username;
    private final String password;
    private final String dbName;

    public DatabaseTransfer(String dbUrl, String username, String password, String dbName) {
        this.dbUrl = dbUrl;
        this.username = username;
        this.password = password;
        this.dbName = dbName;
    }

    public void connectionAndInsertion(InformacionEnvio infoEnvio) throws IllegalAccessException {
        try (Connection connection = DriverManager.getConnection(dbUrl, username, password)) {
            java.sql.Timestamp envio = new java.sql.Timestamp(infoEnvio.getFechaEnvio().getTime());
            java.sql.Timestamp entrega = new java.sql.Timestamp(infoEnvio.getFechaEntrega().getTime());


            String query = " insert into "+dbName+"(gen_albaran, gen_fecha_envio, gen_lugar_envio, gen_fecha_entrega, gen_lugar_entrega)"
                    + " values(?, ?, ?, ?, ?);";

            PreparedStatement preparedStmt = connection.prepareStatement(query);
            // create the mysql insert preparedstatement
            preparedStmt.setString(1, infoEnvio.getNumeroAlbaran());
            preparedStmt.setTimestamp(2, envio);
            preparedStmt.setString(3, infoEnvio.getLugarEnvio());
            preparedStmt.setTimestamp(4, entrega);
            preparedStmt.setString(5, infoEnvio.getLugarEntrega());

            preparedStmt.execute();

        } catch (Exception throwables) {
            throw new IllegalAccessException(throwables.toString());
        }
    }
}

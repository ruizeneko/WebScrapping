package edu.ehu.tap.ScrappingTool;

import java.io.*;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;

/**
 * Esta clase inserta en la table DISTANCIA_ENVIO_TIEMPO de MySQL la distancia entre el punto de envio y recogida,
 * asi como el tiempo consumido en el envio, tomando los datos de INFO_GENERAL_ENVIO
 * @author Eneko Ruiz
 */
public class FromCityToDistance {

    /**
     * Constructor
     *  @param loadFromDB Si populamos o no el set con los indices de la biblioteca
     * @throws SQLException Si no es posible acceder a la BBDD o la query esta mal realizada
     */
    public FromCityToDistance(boolean loadFromDB) throws SQLException {
        if (loadFromDB){
            columnToSet();
        }
    }
    /**
     * Set que contiene los albaranes para los que ya se ha calculado la distancia y el tiempo de envio
     */
    Set<String> albaranesYaUsados = new HashSet<>();

    /**
     * Metodo para insertar en la BBDD (tabla DISTANCIA_TIEMPO_ENVIO) el valor de la distacia entre el origen y entrega,
     * asi como el tiempo necesario para realizar el envio. Tambien se insertara el albaran correspondiente
     * @return Si el albaran se encuentra en la tabla DISTANCIA_TIEMPO_ENVIO o no
     * @throws SQLException si hay algun problema durante la insercion (por ejemplo, una PRIMARY KEY VIOLATION) o durante
     * la conexion con la BBDD
     * @throws IOException (proveniento de GeoEncoder, por instanciarlo) si al convertir el objeto Reader (donde
     *                      se recibe la respuesta del WS en forma de JSON) a String para parsearlo, este no puede ser leido
     * @throws JSONException (proveniento de GeoEncoder, por instanciarlo) si al convertir el String parseado a JSON,
     * este no tiene el formato adeacuado
     */
    public boolean getData() throws SQLException, IOException, JSONException {

        boolean alreadyInUse;
        Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/TAP", "root", "1512");
        String query = "SELECT gen_albaran FROM info_general_envio ORDER BY RAND() LIMIT 1";

        String albaran = "";

        Statement stm = connection.createStatement();
        ResultSet rs = stm.executeQuery(query);

        if (rs.next()) {
            albaran = rs.getString(1);
        }

        if (!albaranesYaUsados.contains(albaran) && !albaran.isEmpty()) {
            albaranesYaUsados.add(albaran);
            alreadyInUse = false;
        } else {
            alreadyInUse = true;
        }

        if (!alreadyInUse) {

            String queryBasedOnAlbaran = "SELECT * FROM info_general_envio where gen_albaran =" + albaran + ";";
            Double timeDistance = getTimeDistanceFromQuery(connection, queryBasedOnAlbaran);

            String cityOrigen = getOrigenDestinoEntrega(connection, queryBasedOnAlbaran)[0];
            String cityEntrega = getOrigenDestinoEntrega(connection, queryBasedOnAlbaran)[1];

            Double[] latLongOrigen = new GeoEncoder(cityOrigen, "ruizeneko97").encoderManager();
            Double [] latLongRecodiga = new GeoEncoder(cityEntrega, "ruizeneko97").encoderManager();

            double distanceMetrica = getDistanceFromLatLong(latLongOrigen, latLongRecodiga);


            String nwQuery = "insert into distancia_tiempo_envio(env_albaran, env_distancia, env_tiempo)"
                    + " values(?, ?, ?);";

            PreparedStatement preparedStmt = connection.prepareStatement(nwQuery);
            preparedStmt.setString(1, albaran);
            preparedStmt.setDouble(2, distanceMetrica);
            preparedStmt.setDouble(3,timeDistance);

            preparedStmt.execute();
        }
        connection.close();
        return alreadyInUse;
    }

    /**
     * A partir de las coordenadas entre un punto X e Y, devuelve la distancia entre ellos
     * @param coordinates1 Set de coordenadas de un lugar X (lat, long)
     * @param coordinates2 Set de coordenadas de un lugar Y (lat, long)
     * @return Distancia entre X e Y
     */
    private static Double getDistanceFromLatLong(Double[] coordinates1, Double[] coordinates2){
        Double lat1 = coordinates1[0];
        Double long1 = coordinates1[1];

        Double lat2 = coordinates2[0];
        Double long2 = coordinates2[1];

        return distance(lat1, long1, lat2, long2);
    }

    /**
     * A partir de un objeto Connection, calcula el tiempo que fue necesario en un determinado envio para recorrer la
     * distancia entre el origen y el destino
     * @param connection Objeto Connection que conecta con la BBDD de la que tomaremos las fechas (tabla INFO_GENERAL_ENVIO)
     * @param query Query a realizar para obtener las fechas
     * @return Tiempo (en horas) que tarda el envio de origen a destino
     * @throws SQLException Si se produce un error durante el acceso a la BBDD o el indice de la columan no es valido
     */
    private static Double getTimeDistanceFromQuery(Connection connection, String query) throws SQLException {
        Timestamp fechaOrigen = null;
        Timestamp fechaEntrega = null;

        Statement stm = connection.createStatement();
        ResultSet rs = stm.executeQuery(query);

        if (rs.next()) {
            fechaOrigen = rs.getTimestamp(2);
            fechaEntrega = rs.getTimestamp(4);
        }

        assert fechaEntrega != null;
        return (double) TimeUnit.MILLISECONDS.toHours(fechaEntrega.getTime() - fechaOrigen.getTime());
    }

    /**
     * A partir de un Objeto Connection, devuelve el origen y el destino de un determinado envio
     * @param connection Objeto Connection que conecta con la BBDD del que tomaremos el origen y el destino de la entrega (tabla INFO_GENERAL_ENVIO)
     * @param query Query a realizar para obtener el origen y el destino
     * @throws SQLException Si se produce un error durante el acceso a la BBDD o el indice de la columan no es valido
     */
    private static String[] getOrigenDestinoEntrega(Connection connection, String query) throws SQLException {
        String origen = null;
        String destino = null;

        Statement stm = connection.createStatement();
        ResultSet rs = stm.executeQuery(query);

        if (rs.next()) {
            origen = rs.getString(3);
            destino = rs.getString(5);
        }

        return new String[]{origen, destino};
    }

    /**
     * Calcula la distancia en kilometros entre X e Y, a partir de su coordenadas
     * @param lat1 Latitud del punto X
     * @param lon1 Longitud del punto X
     * @param lat2 Latitud del punto Y
     * @param lon2 Longitud del punto Y
     * @return Distancia entre X e Y
     */
    private static double distance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515 * 1.609344;
        return dist;
    }


    /**
     * Conversor de grados a radianes
     * @param deg Valor en grados
     * @return Valor en radianes
     */
    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    /**
     * Conversor de radianes a grados
     * @param rad Valor en radianes
     * @return Valor en grados
     */
    private static double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }

    /**
     * Carga en el set albaranesYaUsados los albaranes (Primary key de la BBDD) ya almacenados
     * @throws SQLException Si no es posible acceder a la BBDD o la query esta mal realizada
     */
    private void columnToSet() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/TAP", "root", "1512");
        String query = "SELECT env_albaran FROM distancia_tiempo_envio";

        Statement stm = connection.createStatement();
        ResultSet rs = stm.executeQuery(query);
        while (rs.next()) {
            albaranesYaUsados.add(rs.getString("env_albaran"));
        }
    }
}

/**
 * En este Main se populara la tabla DISTANCIA_TIEMPO_ENVIO añadiendo los n valores seleccionados en el loop. Si el valor
 * ya ha sido previamente generado (ya que se generan aleatoriamente), se restara 1 al contador del loop para que, una vez
 * terminado el loop, haya n entradas en la BBDD.
 */
class Mainer {
    public static void main(String[] args) throws SQLException, IOException, JSONException {
        FromCityToDistance fromCityToDistance = new FromCityToDistance(true);
        for (int i = 0; i < 1500; i++) {
            boolean alreadyInUse = fromCityToDistance.getData();
            if (alreadyInUse) {
                i--;
            }
        }
    }
}


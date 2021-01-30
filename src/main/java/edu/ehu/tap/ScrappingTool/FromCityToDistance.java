package edu.ehu.tap.ScrappingTool;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Eneko Ruiz
 */
public class FromCityToDistance {

    public static void getData(Connection connection, String albaran) throws SQLException, IOException {

        String query = "SELECT * FROM info_general_envio where gen_albaran =" + albaran + ";";
        Double timeDistance = getTimeDistanceFromQuery(connection, query);
        boolean found = !albaran.isEmpty();


        String cityOrigen = getOrigenEntrega(connection, query)[0];
        String cityEntrega = getOrigenEntrega(connection, query)[1];

            if (found) {
                String countryCodeOrigen = "es";
                String countryCodeDestino = "pt";

                String urlOrigen = "https://maps.googleapis.com/maps/api/geocode/json?region=" + countryCodeOrigen + "&address=" + cityOrigen +
                        
                urlOrigen = urlOrigen.replace(" ", "%20");

                String urlRecogida = "https://maps.googleapis.com/maps/api/geocode/json?region=" + countryCodeDestino + "&address=" + cityEntrega +
                       
                urlRecogida = urlRecogida.replace(" ", "%20");

                InputStream envioOrigen = new URL(urlOrigen).openStream();
                InputStream envioRecogida = new URL(urlRecogida).openStream();

                countryCodeOrigen = getCountry(envioOrigen, countryCodeOrigen);
                countryCodeDestino = getCountry(envioRecogida, countryCodeDestino);

                urlOrigen = "https://maps.googleapis.com/maps/api/geocode/json?region=" + countryCodeOrigen + "&address=" + cityOrigen +

                urlOrigen = urlOrigen.replace(" ", "%20");

                urlRecogida = "https://maps.googleapis.com/maps/api/geocode/json?region=" + countryCodeDestino + "&address=" + cityEntrega +

                urlRecogida = urlRecogida.replace(" ", "%20");

                envioOrigen = new URL(urlOrigen).openStream();
                envioRecogida = new URL(urlRecogida).openStream();

                Double[] latLongOrigen;
                Double [] latLongRecodiga;
                latLongOrigen = getLatLongFromInputStream(envioOrigen);
                latLongRecodiga = getLatLongFromInputStream(envioRecogida);

                double distanceMetrica = getDistanceFromLatLong(latLongOrigen, latLongRecodiga);


                String nwQuery = " insert into distancia_tiempo_envio(env_albaran, env_distancia, env_tiempo)"
                        + " values(?, ?, ?);";

                PreparedStatement preparedStmt = connection.prepareStatement(nwQuery);
                // create the mysql insert preparedstatement
                preparedStmt.setString(1, albaran);
                preparedStmt.setDouble(2, distanceMetrica);
                preparedStmt.setDouble(3,timeDistance);


                preparedStmt.execute();
            }
    }
        private static String readAll(Reader rd) throws IOException {
            StringBuilder sb = new StringBuilder();
            int cp;
            while ((cp = rd.read()) != -1) {
                sb.append((char) cp);
            }
            return sb.toString();
        }

        private static Double[] getLatLongFromInputStream(InputStream inputStream){
            double lat = 0.0;
            double lng = 0.0;
            try {
                BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                String jsonText = readAll(rd);
                JSONObject json = new JSONObject(jsonText);

                lat = (double) json.getJSONArray("results").getJSONObject(0).getJSONObject("geometry").getJSONObject("location").get("lat");
                lng = (double) json.getJSONArray("results").getJSONObject(0).getJSONObject("geometry").getJSONObject("location").get("lng");

            } catch (Exception e) {
                e.printStackTrace();
            }
            return new Double[]{lat, lng};
        }
    private static String getCountry(InputStream inputStream, String countryCode){
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String jsonText = readAll(rd);
            JSONObject json = new JSONObject(jsonText);
            int size = json.getJSONArray("results").length();
            if (size > 1){
                if (countryCode.equals("es")){
                    return "pt";
                } else {
                    return "es";
                }
            }


        } catch (Exception ignored){}
        return countryCode;
    }


        private static Double getDistanceFromLatLong(Double[] coordinates1, Double[] coordinates2){
            Double lat1 = coordinates1[0];
            Double long1 = coordinates1[1];

            Double lat2 = coordinates2[0];
            Double long2 = coordinates2[1];

            return distance(lat1, long1, lat2, long2);
        }

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

    private static String[] getOrigenEntrega(Connection connection, String query) throws SQLException {
        String origen = null;
        String entrega = null;

        Statement stm = connection.createStatement();
        ResultSet rs = stm.executeQuery(query);

        if (rs.next()) {
            origen = rs.getString(3);
            entrega = rs.getString(5);
        }

        return new String[]{origen, entrega};
    }

    private static double distance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515 * 1.609344;
        return dist;
    }


    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private static double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }
}

class Mainer{
    public static void main(String[] args) throws SQLException, IOException, JSONException {
        Set<String> albaranes = new HashSet<>();
        Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/TAP", "root", "1512");

        for(int i = 0; i < 100 ; i++) {
            String query = "SELECT gen_albaran FROM info_general_envio ORDER BY RAND() LIMIT 1";
            String albaran = "";

            Statement stm = connection.createStatement();
            ResultSet rs = stm.executeQuery(query);

            if (rs.next()) {
                albaran = rs.getString(1);
            }

            if (!albaranes.contains(albaran)) {
                albaranes.add(albaran);
            } else {
                i--;
            }
        }

        for (String albaran : albaranes) {
            FromCityToDistance.getData(connection, albaran);
        }
        connection.close();
    }
}

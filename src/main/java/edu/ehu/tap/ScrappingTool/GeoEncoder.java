package edu.ehu.tap.ScrappingTool;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * Nos devuelve las coordenadas de una ciudad tomadas de la API de Google o de GeoNames
 * @author Eneko Ruiz
 */

public class GeoEncoder {
    private final String placeName;
    private final String userName;

    /**
     * Este array sirve contiene que el ccTLD de los paises en los que reparte MRW, y se usara para comprobar la validez
     * de la respueta de la API
     */
    private final ArrayList<String> availableCountry = new ArrayList<>(){
        {
            add("ES");
            add("PT");
            add("AD");
            add("FR");
        }
    };

    /**
     * Constructor
     * @param placeName Nombre de la ciudad a buscar
     * @param userName Usuario de GeoNames (hay que ser usuario registrado
     */
    public GeoEncoder(String placeName, String userName) {
        this.placeName = placeName;
        this.userName = userName;
    }

    /**
     * Metodo para, a partir de la respuesta de las APIs, decide si utiliza la de GeoNames (por defecto a no ser que nos devuelva una
     * respuesta vacia o de un pais en el que no opera MRW) o la de Google y parsearla.
     * @return Coordenadas del lugar
     * @throws IOException Si al convertir el objeto BufferedReader (donde
     * se recibe la respuesta del WS en forma de JSON) a String para parsearlo, este no puede ser leido
     * @throws JSONException Si al convertir el String parseado a JSON,
     * este no tiene el formato adeacuado
     */
    public Double[] encoderManager() throws IOException, JSONException {
        double lat;
        double lng;

        // En este metodo, se ha decidido hacer dos sentencias condicionales IFs para aportar claridad al codigo
        if (geonamesEncoder().length() > 0
                && (!geonamesEncoder().isNull("address") || geonamesEncoder().length() > 0)
                && (!geonamesEncoder().getJSONObject("address").isNull("countryCode")
                || geonamesEncoder().getJSONObject("address").length() > 0)){

            String address = (String) geonamesEncoder().getJSONObject("address").get("countryCode");
            if (availableCountry.contains(address.toUpperCase())) {
                lng = Double.parseDouble((String) geonamesEncoder().getJSONObject("address").get("lng"));
                lat = Double.parseDouble((String) geonamesEncoder().getJSONObject("address").get("lat"));
            } else {
                lat = (Double) googleEncoder().getJSONArray("results").getJSONObject(0).getJSONObject("geometry").getJSONObject("location").get("lat");
                lng = (Double) googleEncoder().getJSONArray("results").getJSONObject(0).getJSONObject("geometry").getJSONObject("location").get("lng");
            }
        } else {
            lat = (Double) googleEncoder().getJSONArray("results").getJSONObject(0).getJSONObject("geometry").getJSONObject("location").get("lat");
            lng = (Double) googleEncoder().getJSONArray("results").getJSONObject(0).getJSONObject("geometry").getJSONObject("location").get("lng");
        }
        return new Double[]{lat, lng};
    }

    /**
     * Metodo para recibir la respuesta del API de GeoNames para una ciudad (formato JSON)
     * @return Objeto JSON con la respueta del request.
     * @throws IOException Si al convertir el objeto Reader (donde
     * se recibe la respuesta del WS en forma de JSON) a String para parsearlo, este no puede ser leido
     * @throws JSONException Si al convertir el String parseado a JSON,
     * este no tiene el formato adeacuado
     */
    private JSONObject geonamesEncoder() throws IOException, JSONException {
        String URL = "http://api.geonames.org/geoCodeAddressJSON?q=" + this.placeName + "&username=" + this.userName;
        URL = URL.replace(" ", "%20");

        InputStream inputStream = new URL(URL).openStream();
        BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String jsonText = readAll(rd);
        return new JSONObject(jsonText);
    }

    /**
     * Metodo para recibir la respuesta del API de Google para una ciudad (formato JSON).
     * @return Objeto JSON con la respueta del request.
     * @throws IOException Si al convertir el objeto Reader (donde
     * se recibe la respuesta del WS en forma de JSON) a String para parsearlo, este no puede ser leido
     * @throws JSONException Si al convertir el String parseado a JSON,
     * este no tiene el formato adeacuado
     */
    private JSONObject googleEncoder() throws IOException, JSONException {
        String URL =  getURLFroGoogleEncoder();
        InputStream inputStream = new URL(URL).openStream();

        BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String jsonText = readAll(rd);
        return new JSONObject(jsonText);
    }

    /**
     * Metodo para convertir la respuesta de un objeto Reader a un String
     * @param rd Objeto Reader con la respueta del request
     * @return String con la respueta
     * @throws IOException Si al convertir el objeto Reader (donde
     *  se recibe la respuesta del WS en forma de JSON) a String para parsearlo, este no puede ser leido
     */
    private String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    /**
     * Metodo (usado en la API de Google) para determinar si una ciudad pertenece a españa o portugal. Se ha comprobado
     * que las respuestas con un JSONArray tamaño superior a 1, estan buscando en el pais equivocado. Por lo tanto, si obtenemos
     * un JSONArray de tamaño superior a 1, cambiaremos el codigo del pais en el que estamos buscando.
     * @param inputStream InputStream con la respuesta de la request al API de Google
     * @param countryCode Codigo de pais utilizado
     * @return Codigo de pais correcto
     */
    private String getCountry(InputStream inputStream, String countryCode){
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

    /**
     * Construye la URL con el codigo de pais correcto (por defecto ES), usando el metodo getCountry
     * @return URL (en forma de String) correcta
     * @throws IOException
     *  Si al convertir el objeto Reader (donde
     *  se recibe la respuesta del WS en forma de JSON) a String para parsearlo, este no puede ser leido
     */
    private String getURLFroGoogleEncoder() throws IOException {
        String countryCode = "es";

        String URL =  "https://maps.googleapis.com/maps/api/geocode/json?region=" + countryCode + "&address=" + this.placeName +
                "&key=GOOGLE-API-KEY";
        URL = URL.replace(" ", "%20");

        InputStream inputStream = new URL(URL).openStream();

        countryCode = getCountry(inputStream, countryCode);

        URL =  "https://maps.googleapis.com/maps/api/geocode/json?region=" + countryCode + "&address=" + this.placeName +
                "&key=GOOGLE-API-KEY";
        URL = URL.replace(" ", "%20");

        return URL;
    }
}

package edu.ehu.tap.ScrappingTool;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


import java.io.IOException;

/**
 * Clase encargada de hacer scraping a una web (la URL se proporciona en el constructor).
 * @author Eneko Ruiz
 */

public class Scraper{

    private final String url;
    private final String userAgent;
    private final Proxy proxy;
    private final String numAlbaran;

    /**
     * Constructor
     * @param url URL sobre la que vamos a hacer scraping
     * @param userAgent User-agent para introducir a la cabecera de la llamada
     * @param proxy Servidor proxy que utilizaremos al usar la llamada
     * @param numAlbaran Numero de albaran sobre el que recibiremos informacion en la llamada
     */
    public Scraper(String url, String userAgent, Proxy proxy, String numAlbaran) {
        this.url = url;
        this.userAgent = userAgent;
        this.proxy = proxy;
        this.numAlbaran = numAlbaran;
    }

    /**
     * Metodo para hacer scrapping. Buscara en el codigo CSS de la respuesta las Strings entregado, recogido y recoger
     * (a traves de un REGEX) e introducira la informacion en un objeto InformacionEnvio
     * @return Objeto InformacionEnvio con la informacion del envio
     * @throws IOException Si JSOUP no puede acceder a la URL (esta mal formada, esta el servidor caido, etc)
     * @throws ParseException Si la String de la fecha no puede ser parseada (por ejemplo, no es una hora o no esta en
     * un formato reconocido por la clase Parser)
     */
    public InformacionEnvio scrappingTool() throws ParseException, IOException {

        // Es posible que tengas que cambiarme
        Proxy proxy = new Proxy(Proxy.Type.HTTP,
                new InetSocketAddress("51.158.123.35", 9999));

        final Document document = Jsoup.connect(url)
                .userAgent(userAgent)
                .proxy(proxy)
                .get();

        Elements tableRows = document.select("tbody > tr");

        String termEntrega = "entregado";

        String termRecogida = "recogido";
        String termRecogidaAlternativo = "recoger";

        String patternEntrega = "(?i)(?=.*" + String.join(")(?=.*", termEntrega.split("(?!^)")) + ")";
        String patternRecogida = "(?i)(?=.*" + String.join(")(?=.*", termRecogida.split("(?!^)")) + ")";
        String patternRecogidaAlternativa = "(?i)(?=.*" +
                String.join(")(?=.*", termRecogidaAlternativo.split("(?!^)")) + ")";

        Pattern regexEntrega = Pattern.compile(patternEntrega);
        Pattern regexRecogida = Pattern.compile(patternRecogida);
        Pattern regexRecogidaAlternativa = Pattern.compile(patternRecogidaAlternativa);

        String dateFormatString = "dd/MM/yyyy HH:mm";
        SimpleDateFormat dateFormat = new SimpleDateFormat(dateFormatString);

        InformacionEnvio infoEnvio = new InformacionEnvio(numAlbaran);

        for (Element tableRow : tableRows) {

            String status = tableRow.child(2).text();
            Matcher matchEntrega = regexEntrega.matcher(status);
            Matcher matchRecogida = regexRecogida.matcher(status);
            Matcher matchRecogidaAlternativa = regexRecogidaAlternativa.matcher(status);

            if (matchEntrega.find()) {
                String fecha = tableRow.child(0).text();
                String hora = tableRow.child(1).text();
                String date = fecha + " " + hora;
                String lugar = tableRow.child(3).text();
                Date myDate = dateFormat.parse(date);

                infoEnvio.setFechaEntrega(myDate);
                infoEnvio.setLugarEntrega(lugar);

            } else if (matchRecogida.find() || matchRecogidaAlternativa.find()) {
                String fecha = tableRow.child(0).text();
                String hora = tableRow.child(1).text();
                String date = fecha + " " + hora;
                String lugar = tableRow.child(3).text();

                infoEnvio.setFechaEnvio(dateFormat.parse(date));
                infoEnvio.setLugarEnvio(lugar);
            }
        }
        return infoEnvio;
    }
}

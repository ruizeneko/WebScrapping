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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author Eneko Ruiz
 */
public class Scrapper{

    // Base de la URL sobre la que vamos a hacer scrapping
    private final String url;
    private final String userAgent;
    private final Proxy proxy;
    private final String numAlbaran;

    public Scrapper(String url, String userAgent, Proxy proxy, String numAlbaran) {
        this.url = url;
        this.userAgent = userAgent;
        this.proxy = proxy;
        this.numAlbaran = numAlbaran;
    }

    public InformacionEnvio scrappingTool() throws IOException, ParseException {

        Proxy proxy = new Proxy(Proxy.Type.HTTP,
                new InetSocketAddress("142.4.203.248", 3128));

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

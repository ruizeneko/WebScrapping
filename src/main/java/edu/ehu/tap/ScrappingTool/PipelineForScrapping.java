package edu.ehu.tap.ScrappingTool;

import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Singleton para gestionar el proceso de scraping y de introduccion de la informacion en la BBDD. Extendera la clase
 * TimerTask para ejecutarla con una frecuencia determinada
 * @author Eneko Ruiz
 */
public class PipelineForScrapping extends TimerTask {

    /**
     * Set que contiene los albaranes screpeados.
     */
    private final Set<String> albaranesYaUsados = new HashSet<>();
    private final String FILENAME_USER_AGENT;
    private final String FILENAME_PROXY;
    private final String ALBARAN_SEED;
    private final String URL;
    static PipelineForScrapping pipeline;
    /**
     * Counter para parar la ejecucion cuando se han recorrido todos los albaranes posibles para la SEED dada.
     */
    int counter = 0;


    /**
     * Constructor privado
     * @param FILENAME_USER_AGENT Fichero que contiene una lista de cabeceras HTTP User-agent
     * @param FILENAME_PROXY Fichero que contiene una lista de servidores proxy
     * @param albaran_seed Seed del albaran a partir de la cual se realizara el scraping
     * @param url URL de la pagina de MRW
     * @param loadFromDB Si populamos o no el set con los indices de la biblioteca
     * @throws SQLException Si no es posible acceder a la BBDD o la query esta mal realizada
     */
    private PipelineForScrapping(String FILENAME_USER_AGENT, String FILENAME_PROXY, String albaran_seed, String url, boolean loadFromDB) throws SQLException {
        this.FILENAME_USER_AGENT = FILENAME_USER_AGENT;
        this.FILENAME_PROXY = FILENAME_PROXY;
        this.ALBARAN_SEED = albaran_seed;
        this.URL = url;

        if (loadFromDB){
            columnToSet();
        }
    }

    /**
     * Singleton para instanciar la clase
     * @param FILENAME_USER_AGENT Fichero que contiene una lista de cabeceras HTTP User-agent
     * @param FILENAME_PROXY Fichero que contiene una lista de servidores proxy
     * @param albaran_seed Seed del albaran a partir de la cual se realizara el scraping
     * @param url URL de la pagina de MRW
     * @param loadFromDB Si populamos o no el set con los indices de la biblioteca
     * @return Instancia del objeto PipelineForScrapping
     * @throws SQLException Si no se puede acceder a la BBDD
     */
    public static PipelineForScrapping getInstance(String FILENAME_USER_AGENT, String FILENAME_PROXY, String albaran_seed, String url, boolean loadFromDB) throws SQLException {
        if (pipeline == null) {
            pipeline = new PipelineForScrapping(FILENAME_USER_AGENT, FILENAME_PROXY, albaran_seed, url, loadFromDB);
        }
        return pipeline;
    }

    /**
     * Metodo que tenemos que reescribir al extender TimerTask. Esta se ejecutara con la frecuencia que se determine
     * cuando se instancie la clase. Con el try - catch, atendemos la excepcion, evitando que el proceso se pare si alguna
     * exception se lanza. El metodo toma un proxy aleatorio, un user-agent aleatorio y  un numero de albaran aleatorio
     * (construido a partir de la SEED) e introduce la informacion del albaran en la base de datos instanciando los objetos
     * Scraper primero y DatabaseTranser despues. Si el numero aleatorio recibido es "FIN", significara que se han generado
     * todas las combinaciones posibles y se detendra la ejecucion del programa.
     */
    @Override
    public void run() {
        String randomInt = randomIntGenerator();
        if (!randomInt.equals("FIN")) {
            try {
                Proxy proxy = getRandomProxy();
                Scraper scrappingTool = new Scraper(URL + ALBARAN_SEED + randomInt,
                        getRandomUseragent(), proxy,ALBARAN_SEED + randomInt);

                DatabaseTransfer dataTransfer = new DatabaseTransfer("jdbc:mysql://localhost:3306/TAP",
                        "root", "1512", "info_general_envio");

                dataTransfer.connectionAndInsertion(scrappingTool.scrappingTool());
            } catch (IOException | ParseException | SQLException e) {
                e.printStackTrace();
            }
        } else {
            System.exit(14);
        }

    }

    /**
     * Selecciona aleatoriamente un User-Agent de un fichero
     * @return Cabecera HTTP User-Agent
     */
    private String getRandomUseragent() {
        List<String> userAgents = new ArrayList<>();
        Random rand = new Random();
        try (BufferedReader br = new BufferedReader(new FileReader(this.FILENAME_USER_AGENT))) {
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                userAgents.add(sCurrentLine);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return userAgents.get(rand.nextInt(userAgents.size()));
    }

    /**
     * Selecciona aleatoriamente servidor proxy de un fichero y comprueba que funcione
     * @return Servidor proxy
     * @throws IOException si el servidor no funciona
     */
    private Proxy getRandomProxy() throws IOException {
        List<Proxy> proxies = new ArrayList<>();

        Random rand = new Random();

        FileInputStream file = new FileInputStream(FILENAME_PROXY);
        BufferedReader inputStreamReader = new BufferedReader(new InputStreamReader(file));

        while (inputStreamReader.ready()) {
            String line = inputStreamReader.readLine();
            String[] splittedLine = line.split(" ", 2);

            Proxy proxy = new Proxy(Proxy.Type.HTTP,
                    new InetSocketAddress(splittedLine[0], Integer.parseInt(splittedLine[1])));

            if (proxyChecker(proxy, splittedLine[0])) {
                proxies.add(proxy);
            }
        }
        return proxies.get(rand.nextInt(proxies.size()));
    }

    /**
     * Comprueba que el proxy suministrado funcione
     * @param proxy Proxy
     * @param proxyIP Direccion IP del proxy
     * @return true si funciona, false en caso contrario
     * @throws IOException si no es posible establecer conexion con la url
     */
    private boolean proxyChecker(Proxy proxy, String proxyIP) throws IOException {
        boolean works;
        try {
            works = true;
            java.net.URL url = new URL("http://google.com/");
            url.openConnection(proxy);
        } catch (IOException e) {
            works = false;
            notWorkingProxy(proxyIP);
        }
        return works;
    }

    /**
     * Se elimina el servidor proxy que no funciona del fichero que contiene los servidores proxy
     * @param proxyIP direccion IP del servidor proxy
     * @throws IOException si por algun motivo no se puede acceder al fichero, o una vez borrado, no se puede reescribir
     */
    private void notWorkingProxy(String proxyIP) throws IOException {
        File tempFile = new File("myTempFile.txt");

        BufferedReader reader = new BufferedReader(new FileReader(FILENAME_PROXY));
        BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

        String currentLine;

        while((currentLine = reader.readLine()) != null) {
            // trim newline when comparing with lineToRemove
            String trimmedLine = currentLine.trim();
            if(trimmedLine.equals(proxyIP)) continue;
            writer.write(currentLine + System.getProperty("line.separator"));
        }
        writer.close();
        reader.close();
        tempFile.renameTo(new File(FILENAME_PROXY));
    }

    /**
     * Genera un numero aleatorio de longitud 12 - longitud_SEED. Comprueba que no se haya generado previamente
     * y devuelve FIN si se han generado todas las combinaciones posibles.
     * @return Numero aleatorio de longitud 12 - longitud_SEED
     */
    private String randomIntGenerator() {
        String toReturn = "";
        String nineGenerator = StringUtils.repeat("9", 12 - ALBARAN_SEED.length());
        int randomNum = ThreadLocalRandom.current().nextInt(0, Integer.parseInt(nineGenerator) + 1);
        if (counter < maximumNumberOfAlbaranes()) {
            if (albaranesYaUsados.contains(ALBARAN_SEED + randomNum)) {
                randomIntGenerator();
            } else {
                String generatedVal = String.valueOf(randomNum);
                if (generatedVal.length() == 12 - ALBARAN_SEED.length()) {
                    counter++;
                    albaranesYaUsados.add(ALBARAN_SEED + randomNum);
                    toReturn = String.valueOf(randomNum);
                } else {
                    counter++;
                    toReturn = StringUtils.repeat("0", 12 - ALBARAN_SEED.length() - generatedVal.length()) +
                            randomNum;
                    albaranesYaUsados.add(ALBARAN_SEED + toReturn);
                }
            }
        } else {
            toReturn = "FIN";
        }
        return toReturn;
    }

    /**
     * Metodo para calcular el numero maximo de albaranes generables
     * @return Numero maximo de albaranes generables
     */
    private int maximumNumberOfAlbaranes(){
        int number_variable_digit = 12 - ALBARAN_SEED.length();
        return (int) Math.pow(10, number_variable_digit);
    }

    /**
     * Carga en el set albaranesYaUsados los albaranes (Primary key de la BBDD) ya almacenados
     * @throws SQLException Si no es posible acceder a la BBDD o la query esta mal realizada
     */
    private void columnToSet() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/TAP", "root", "1512");
        String query = "SELECT gen_albaran FROM info_general_envio";

        Statement stm = connection.createStatement();
        ResultSet rs = stm.executeQuery(query);
        while (rs.next()) {
            albaranesYaUsados.add(rs.getString("gen_albaran"));
        }
    }
}

/**
 * En este Main se instancia la clase PipelineForScrapping a traves
 * del metodo getInstance. Con el Timer y el objeto instanciado, se ejecuta
 * con una frecuencia, en nuestro caso, de entre 1s y 5s, el codigo contenido en "run". Con ello, se ira rellenando
 * la tabla INFO_GENERAL_ENVIO de la BBDD.
 */
class Main{
    static Timer timer = new Timer();

    public static void main(String[] args) throws SQLException {
        PipelineForScrapping pipeline;
        pipeline = PipelineForScrapping.getInstance(
                "C:\\tap\\ScrappingProject\\src\\main\\resources\\userAgents.txt",
                "C:\\tap\\ScrappingProject\\src\\main\\resources\\proxiesList.txt",
                "082032", "https://www.mrw.es/seguimiento_envios/MRW_historico_nacional.asp?enviament=", false);

        timer.schedule(pipeline, (long) (0 * 1e3),
                (long) (1e3 * ThreadLocalRandom.current().nextInt(1, 5 + 1)));
    }
}

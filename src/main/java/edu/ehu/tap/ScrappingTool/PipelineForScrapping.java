package edu.ehu.tap.ScrappingTool;

import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.net.*;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Eneko Ruiz
 */
public class PipelineForScrapping extends TimerTask {

    private final Set<String> albaranesYaUsados = new HashSet<>();
    private final String FILENAME_USER_AGENT;
    private final String FILENAME_PROXY;
    private final String ALBARAN_SEED;
    private final String URL;
    static PipelineForScrapping pipeline;

    private PipelineForScrapping(String FILENAME_USER_AGENT, String FILENAME_PROXY, String albaran_seed, String url) {
        this.FILENAME_USER_AGENT = FILENAME_USER_AGENT;
        this.FILENAME_PROXY = FILENAME_PROXY;
        this.ALBARAN_SEED = albaran_seed;
        this.URL = url;
    }

    public static PipelineForScrapping getInstance(String FILENAME_USER_AGENT, String FILENAME_PROXY, String albaran_seed, String url) {
        if (pipeline == null) {
            pipeline = new PipelineForScrapping(FILENAME_USER_AGENT, FILENAME_PROXY, albaran_seed, url);
        }
        return pipeline;
    }

    @Override
    public void run() {
        String randomInt = randomIntGenerator();
        if (!randomInt.equals("NO")) {
            try {
                Proxy proxy = getRandomProxy();
                Scrapper scrappingTool = new Scrapper(URL + ALBARAN_SEED + randomInt,
                        getRandomUseragent(), proxy,ALBARAN_SEED + randomInt);

                DatabaseTransfer dataTransfer = new DatabaseTransfer("jdbc:mysql://localhost:3306/TAP",
                        "root", "1512", "info_general_envio");

                dataTransfer.connectionAndInsertion(scrappingTool.scrappingTool());
            } catch (IOException | ParseException | IllegalAccessException e) {
                e.printStackTrace();
            }
        } else {
            System.exit(0);
        }

    }

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

    private boolean proxyChecker(Proxy proxy, String proxyIP) throws IOException {
        boolean works;
        try {
            works = true;
            java.net.URL url = new URL("http://java.sun.com/");
            url.openConnection(proxy);
        } catch (IOException e) {
            works = false;
            notWorkingProxy(proxyIP);
        }
        return works;
    }

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

    private String randomIntGenerator(){
        String nineGenerator = StringUtils.repeat("9", 12 - ALBARAN_SEED.length());
        int randomNum = ThreadLocalRandom.current().nextInt(0, Integer.parseInt(nineGenerator) + 1); //Chetar esto
        if (albaranesYaUsados.contains(ALBARAN_SEED + randomNum)){
            randomIntGenerator();
        } else {
            String generatedVal = String.valueOf(randomNum);
            if (generatedVal.length() == 12 - ALBARAN_SEED.length()) {
                albaranesYaUsados.add(ALBARAN_SEED + randomNum);
                return String.valueOf(randomNum);

            } else {
                String toReturn = StringUtils.repeat("0", 12 - ALBARAN_SEED.length() - generatedVal.length() ) +
                        randomNum;
                albaranesYaUsados.add(toReturn);
                return toReturn;
            }
        }
        return "NO";
    }
}

class Main{
    static Timer timer = new Timer();

    public static void main(String[] args){
        PipelineForScrapping pipeline;
        pipeline = PipelineForScrapping.getInstance(
                "C:\\tap\\ScrappingProject\\src\\main\\resources\\userAgents.txt",
                "C:\\tap\\ScrappingProject\\src\\main\\resources\\proxiesList.txt",
                "0100927", "https://www.mrw.es/seguimiento_envios/MRW_historico_nacional.asp?enviament=");

        timer.schedule(pipeline, (long) (0 * 1e3),
                (long) (1e3 * ThreadLocalRandom.current().nextInt(1, 2 + 1)));
    }
}

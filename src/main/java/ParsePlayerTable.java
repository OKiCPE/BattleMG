import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ParsePlayerTable implements Runnable {
    private static final Logger log = LogManager.getLogger(GetBattles.class);
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:50.0) Gecko/20100101 Firefox/50.0";
    private static final String BASE_URL = "http://warthunder.com/en/tournament/replay/page/1?Filter[game_mode][0]=";
    private String url;
    private Map<String, String> cookies = new HashMap<>();

    ParsePlayerTable(long battleId, GameMode gameMode) {
        StringBuilder urlBuilder = new StringBuilder(BASE_URL);
        switch (gameMode) {
            case Arcade:
                urlBuilder.append("arcade");
                break;
            case Realistic:
                urlBuilder.append("realistic");
                break;
            case Simulation:
                urlBuilder.append("simulation");
                break;
        }
        //   urlBuilder.append("#");
        urlBuilder.append(battleId);
        url = urlBuilder.toString();
    }


    void parce() {
        Document doc = null;
        Connection get = Jsoup.connect(url)
                .cookies(cookies)
                .userAgent(USER_AGENT)
                .header("Accept-Encoding", "gzip, deflate")
                .timeout(5000)
                .method(Connection.Method.GET);
        try {
            doc = get.execute().parse();
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.error(doc);
        throw new RuntimeException();
    }

    private void setCookies() {
        cookies.put("identity_sid", "4t8fvafmgv5i71m8q9ah9arla0");
        cookies.put("identity_token", "mbnzumnarru51gxxnn32jkmw8fqrzb7afdsdewkqkxptb7osw8xqt5od7fkpw1g5");
        cookies.put("identity_id", "47044885");
        cookies.put("identity_name", "id733");
    }

    @Override
    public void run() {
        setCookies();
        parce();

    }
}

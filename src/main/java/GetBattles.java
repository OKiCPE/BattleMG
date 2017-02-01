import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class GetBattles implements Runnable {
    private static final Logger log = LogManager.getLogger(GetBattles.class);
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:50.0) Gecko/20100101 Firefox/50.0";
    private static final String BASE_URL = "http://warthunder.com/en/tournament/replay/page/";
    private Map<String, String> cookies = new HashMap<>();
    private int pageNumber;
    private MyQueue myQueue;
    private ReentrantLock reentrantLock;
    TaskScheduler taskScheduler;
    static private String identity_sid;
    static private String identity_token;
    static private String identity_id;
    static private String identity_name;
    ReentrantLock restart;
    Condition restartCondition;
    boolean readyToRestart;

    static {
        Properties prop = GetProperties.get();
        identity_sid = prop.getProperty("identity_sid");
        identity_token = prop.getProperty("identity_token");
        identity_id = prop.getProperty("identity_id");
        identity_name = prop.getProperty("identity_name");
    }

    GetBattles(int pageNumber, TaskScheduler taskScheduler, ReentrantLock reentrantLock, ReentrantLock restart, Condition restartCondition, boolean readyToRestart) {
        this.taskScheduler = taskScheduler;
        this.pageNumber = pageNumber;
        this.reentrantLock = reentrantLock;
        this.restart = restart;
        this.restartCondition = restartCondition;
        this.readyToRestart = readyToRestart;
    }

    private void setCookies() {
        cookies.put("identity_sid", identity_sid);
        cookies.put("identity_token", identity_token);
        cookies.put("identity_id", identity_id);
        cookies.put("identity_name", identity_name);
    }

    private void open() throws IOException {
        StringBuilder url = new StringBuilder();
        url.append(BASE_URL);
        url.append(pageNumber);
        url.append("?Filter=&action=search");
        Connection get = Jsoup.connect(url.toString())
                .cookies(cookies)
                .userAgent(USER_AGENT)
                .header("Accept-Encoding", "gzip, deflate")
                .timeout(15000)
                .method(Connection.Method.GET);
        Document doc = get.execute().parse();
        Elements replays = doc.select("a[class=replay__item]");
        log.error("                     =============== PAGE " + pageNumber + " ================");
        if (pageNumber == 1) {
            restart.lock();
            restartCondition.signal();
            readyToRestart = true;
            restart.unlock();
        }
        for (Element replay : replays) {
            ReplayParser parser = new ReplayParser(replay, taskScheduler, reentrantLock);
            Thread thread = new Thread(parser);
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                log.error("Thread interrupted " + e);
            }
        }
    }

    @Override
    public void run() {
        setCookies();
        try {
            open();
        } catch (IOException e) {
            log.error("IOException at page " + pageNumber + " " + e);
        }
    }
}
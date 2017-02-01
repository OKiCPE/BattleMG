import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openqa.selenium.By;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

public class SingleTask implements Runnable {
    final private static String BASE_URL = "http://warthunder.com/en/tournament/replay/";
    private static final Logger log = LogManager.getLogger(SingleTask.class);
    static int taskNumber;
    private static ReentrantLock dbLock;
    private static DesiredCapabilities caps;
    static private String email;
    static private String pass;
    static DesiredCapabilities capabilities;
    static int[] wasRun;

    static {
        ArrayList<String> cliArgsCap = new ArrayList<>();
        cliArgsCap.add("--proxy=127.0.0.1:8800");
        caps = new DesiredCapabilities();
        caps.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, cliArgsCap);
        caps.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_SETTINGS_PREFIX + "loadImages", false);
        System.setProperty("phantomjs.binary.path", "phantomjs.exe");
        System.setProperty("webdriver.gecko.driver", "geckodriver.exe");
        System.setProperty("webdriver.chrome.driver", "chromedriver.exe");

        capabilities = DesiredCapabilities.chrome();
        Proxy proxy = new Proxy();
        proxy.setHttpProxy("127.0.0.1:8800");
        capabilities.setCapability("proxy", proxy);


        Properties prop = GetProperties.get();
        email = prop.getProperty("email");
        pass = prop.getProperty("pass");
    }

    //HtmlUnitDriver driver;
    Pattern pattern = Pattern.compile("\n");
    // HtmlUnitDriver driver;
    Thread thread;
    boolean emptyTask = false;
    private WebDriver driver;
    private ReentrantLock internal = new ReentrantLock();
    private Condition internalCondition = internal.newCondition();
    private boolean isBusy;
    private Battle battle;
    private boolean isReady;
    private boolean stop;
    private ReentrantLock reentrantLock;
    private CyclicBarrier cyclicBarrier = new CyclicBarrier(2);
    private CyclicBarrier externalStop;
    private ReentrantLock lock = new ReentrantLock();
    Condition condition = lock.newCondition();
    int currNumber;

    SingleTask(ReentrantLock dbLock, int l) {
        taskNumber++;
        reentrantLock = dbLock;
        currNumber = l;
        Thread thread = new Thread(this, "SingleTask-" + taskNumber);
        thread.start();

    }

    static void setRun(int number) {
        wasRun = new int[number];
        for (int i = 0; i < number; i++) {
            wasRun[i] = 0;
        }
    }

    boolean isBusy() {
        return isBusy;
    }

    void stop() {
        internal.lock();
        isBusy = true;
        stop = true;
        internalCondition.signalAll();
        internal.unlock();
    }

    void add(Battle battle) {
        internal.lock();
        isBusy = true;
        this.battle = battle;
        isReady = true;
        internalCondition.signal();
        internal.unlock();
    }

    private void process() {
        internal.lock();
        isBusy = false;
        isReady = false;
        //   System.out.println(battle.getBattleId());

        Session session = HibernateFactory.getSession();
        Transaction transaction = session.beginTransaction();


        String url = BASE_URL + battle.getBattleId();
        long start = System.currentTimeMillis();
        driver.navigate().to(url);
        long stop = System.currentTimeMillis();
        StringBuilder message = new StringBuilder();
        message.append("Loaded in ");
        String x = Float.toString((stop - start) / 1000.0f);
        message.append((stop - start) / 1000.0f);
        message.append("\t");
        message.append(driver.getCurrentUrl());
        message.append("\t");
        start = System.currentTimeMillis();
        String rawTeamA = "";
        List<WebElement> webElements = driver.findElements(By.className("team-players"));
        try {
            //rawTeamA = webElements.get(0).findElements(By.className("team-players")).get(0).getText();
            rawTeamA = webElements.get(0).getText();
            // System.out.println(rawTeamA);
        } catch (Exception e) {
            driver.navigate().to(url);
            webElements = driver.findElements(By.className("team-players"));
            try {
                rawTeamA = webElements.get(0).getText();
            } catch (Exception k) {
                log.error("part ");
            }
            log.error("Error parsing Team A " + x);
            log.error(url);
        }
        if (rawTeamA.isEmpty()) {
            driver.navigate().to(url);
            webElements = driver.findElements(By.className("team-players"));
            try {
                rawTeamA = webElements.get(0).getText();
            } catch (Exception k) {
                log.error("part ");
            }
        }

        //   start = System.currentTimeMillis();
        String[] teamA = new String[0];
        if (!rawTeamA.isEmpty()) {
            teamA = pattern.split(rawTeamA);
            if (teamA.length > 0) {
                for (String login : teamA) {
                    PlayersInBattle playersInBattle = new PlayersInBattle();
                    playersInBattle.setBattle(battle);
                    battle.setTeamA(teamA.length);

                    Player player = Player.getPlayer(login, session, reentrantLock);

                    playersInBattle.setPlayer(player);
                    playersInBattle.setTeam(0);
                    session.saveOrUpdate(playersInBattle);
                }
            }
        }
        message.append("Team A = ");
        message.append(teamA.length);
        String rawTeamB = "";
        try {
            // rawTeamB = driver.findElements(By.className("team-players")).get(1).getText();
            rawTeamB = webElements.get(1).getText();
        } catch (Exception e) {
            log.error("Error parsing Team B " + x);
            log.error(url);
        }
        String[] teamB = new String[0];
        if (!rawTeamB.isEmpty()) {
            teamB = pattern.split(rawTeamB);
            if (teamB.length > 0) {

                for (String login : teamB) {
                    PlayersInBattle playersInBattle = new PlayersInBattle();
                    playersInBattle.setBattle(battle);
                    battle.setTeamB(teamB.length);
                    Player player = Player.getPlayer(login, session, reentrantLock);
                    playersInBattle.setPlayer(player);
                    playersInBattle.setTeam(1);
                    session.saveOrUpdate(playersInBattle);
                }
            }
        }
        battle.setPlayers(teamA.length + teamB.length);
        message.append("\tTeam B = ");
        message.append(teamB.length);
        session.saveOrUpdate(battle);
        transaction.commit();
        session.close();
        stop = System.currentTimeMillis();
        message.append("\tprocessed in ");
        message.append((stop - start) / 1000.0f);
        log.error(message.toString());

        //  driver.close();

        wasRun[currNumber]++;
        if (wasRun[currNumber] >= 100) {
            wasRun[currNumber] = 0;
            log.error("browaser restart");
//            driver.quit();
            init();
        }

        internalCondition.signal();
        internal.unlock();
    }

    public void run() {
        init();
        while ((!stop) || (isReady)) {
            internal.lock();
            while (!isBusy) {
                try {
                    internalCondition.await();
                } catch (InterruptedException e) {
                    System.out.println("Error");
                }
            }
            if (isReady) {
                process();
            }
            internal.unlock();
        }
    }


    private void init() {
        driver = new PhantomJSDriver(caps);
//        System.out.println("reinit");
       /* DesiredCapabilities capabilities = DesiredCapabilities.chrome();

        HashMap<String, Object> images = new HashMap<String, Object>();
        images.put("images", 2);
        HashMap<String, Object> prefs = new HashMap<String, Object>();
        prefs.put("profile.default_content_setting_values", images);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--test-type --no-sandbox");
        options.addArguments("--enable-strict-powerful-feature-restrictions");
        options.setExperimentalOption("prefs", prefs);

        options.addArguments("--proxy-server=http://127.0.0.1:8800");
        capabilities.setCapability(ChromeOptions.CAPABILITY, options);

        driver = new ChromeDriver(capabilities);*/
        //   driver = new ChromeDriver();

        //driver = new HtmlUnitDriver();
        //driver = new FirefoxDriver();
//        driver = new HtmlUnitDriver();
//        driver.setJavascriptEnabled(true);
        //       driver.navigate().to(BASE_URL);
//        driver.get(BASE_URL);
        driver.navigate().to(BASE_URL);
        driver.findElement(By.id("email")).sendKeys(email);
        driver.findElement(By.id("password")).sendKeys(pass);
        driver.findElement(By.xpath("//input[@value='Authorization']")).submit();
        // driver.navigate().to(BASE_URL);
    }
}

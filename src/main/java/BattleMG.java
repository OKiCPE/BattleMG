import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class BattleMG {

    private static final Logger log = LogManager.getLogger(BattleMG.class);
    static ReentrantLock dbLock;

    static void magic(int playersThreads, int battlesThreads, int pageCount, ReentrantLock dbLock) {

        TaskScheduler taskScheduler = TaskScheduler.get(playersThreads, dbLock);
        ExecutorService service = Executors.newFixedThreadPool(battlesThreads);
        ReentrantLock restart = new ReentrantLock();
        Condition restartCondition = restart.newCondition();
        AtomicInteger ai = new AtomicInteger(0);
        boolean readyToRestart = false;

        for (int pp = 0; pp < 5_000_000; pp++) {
            for (int z = pageCount; z > 0; z--) {
                synchronized (ai) {
                    while (ai.get() > playersThreads) {
                        try {
                            ai.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    Thread thd = new Thread(new GetBattles(ai, z, taskScheduler, dbLock, restart, restartCondition, readyToRestart));
                    thd.start();
                    ai.incrementAndGet();
                }
            }
        }
        /*for (int p = 0; p < 100_000; p++) {
            for (int i = pageCount; i > 0; i--) {
                // log.error("added " + i);
                service.submit(new GetBattles(ai, i, taskScheduler, dbLock, restart, restartCondition, readyToRestart));
            }*/
           /* log.error("aa");
            System.out.println(readyToRestart);
            while (!readyToRestart) {
                log.error("before wait");
                restart.lock();
                log.error("aaaaaaa");
                try {
                    log.error("bbbbbb");
                    restartCondition.await();
                    System.out.println(readyToRestart);
                    log.error("cccccc");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("ddd");
                restart.unlock();
                System.out.println("eeeeee");
                //readyToRestart = false;
            }
            log.error("after wait");*/
        //readyToRestart = false;
      /*  }
        System.out.println("BEFORE");
        service.shutdown();
        System.out.println("AFTER");
        try {
            while (!service.awaitTermination(1, TimeUnit.SECONDS)) {
                log.info("Awaiting completion of threads.");
            }
        } catch (
                InterruptedException e) {
            log.error("Thread problem");
        }*/
        //taskScheduler.stop();
        //  HibernateFactory.close();
    }


    public static void main(String[] args) {
        System.getProperties().remove("sun.stdout.encoding");
        System.getProperties().remove("sun.stderr.encoding");
        int playersThreads = Integer.parseInt(args[0]);
        log.error("players threads count = " + playersThreads);
        int battlesThreads = Integer.parseInt(args[1]);
        log.error("battles threads count = " + battlesThreads);
        int pageCount = Integer.parseInt(args[2]);
        log.error("page to parse = " + pageCount);
        ReentrantLock reentrantLock = new ReentrantLock(true);
        magic(playersThreads, battlesThreads, pageCount, reentrantLock);
    }
}

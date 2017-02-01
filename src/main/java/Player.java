import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import javax.persistence.*;
import java.io.ObjectInput;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Entity
@Table(name = "players")
public class Player implements Serializable {
    private static final Logger log = LogManager.getLogger(Player.class);
    private static final long serialVersionUID = 1333929301315965245L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "MEDIUMINT UNSIGNED")
    private int Id;
    @Column(name = "login", unique = true, nullable = false)
    private String login;

    static Player getPlayer(String login, Session session1, ReentrantLock reentrantLock) {
        Player player;

        //   long start = System.currentTimeMillis();
        Session session = HibernateFactory.getSession();
        Transaction transaction = session.beginTransaction();
        Query query = session.createQuery("from Player where login = :login");

        query.setParameter("login", login);
        reentrantLock.lock();
        List list = query.getResultList();
        if (!list.isEmpty()) {
            player = (Player) query.getSingleResult();

        } else {
            player = new Player();
            player.setLogin(login);
            session.saveOrUpdate(player);


        }
        //session.close();
        // long stop = System.currentTimeMillis();
        // System.out.println("get player " + ((stop - start) / 1000.0f));
        reentrantLock.unlock();
        transaction.commit();
        session.close();

        return player;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }
}

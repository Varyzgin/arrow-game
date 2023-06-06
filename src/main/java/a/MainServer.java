package a;

import javafx.scene.shape.Circle;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainServer {
    int port = 3124;
    InetAddress ip = null;
    ExecutorService service = Executors.newCachedThreadPool();

    // список клиентов, подключенных к серверу, им будем отправлять текущее состояние системы
    ArrayList<ClientAtServer> allClients = new ArrayList<>();
    ArrayList<Integer> ids;
    ArrayList<String> nickNames;
    ArrayList<Integer> scores;
    ArrayList<Integer> shots;
    ArrayList<String> leaders;

    ArrayList<Integer> wins;
    SessionFactory sf = HibernateFactory.getSessionFactory();

    int num_of_clients = 0, num_of_ready_clients = 0, max_score = 0;

    double basic_arrow_pos = 150.0;
    Circle targetBig, targetSmall;
    // по сути сюда надо сложить все относительно мишеней
    int revOfTarBig = 1;
    int revOfTarSmall = 1;

    Thread t;

    public int moveTarget(int rev, Circle target, double frame) {
        double newY = target.getCenterY() + 2 * rev;
        if (newY + target.getRadius() > frame) rev = -1;
        if (newY - target.getRadius() < 0) rev = 1;
        target.setCenterY(newY);
        return rev;
    }
    // массовая рассылка ников клиентам
    void broadCast(Action action, ClientAtServer client){
        Message msg = null;
        if(action == Action.ON_CONNECT){
            // разберемся с количеством клиентов и присвоим id по номеру подключения
            num_of_clients++;
            client.id = num_of_clients;
            nickNames = new ArrayList<>();
            ArrayList<Double> arrows = new ArrayList<>();

            for(ClientAtServer allClients : allClients){
                nickNames.add(allClients.nickName);
                arrows.add(basic_arrow_pos * allClients.id);
            }

            msg = new Message(action, nickNames, arrows);
        }
        else if(action == Action.ON_READY){
            num_of_ready_clients++;
            System.out.println(num_of_ready_clients);
            if(num_of_clients == num_of_ready_clients){
                // поток, занимающийся движениями мишени
                if(t==null){
                    targetBig = new Circle();
                    targetBig.setCenterY(200);
                    targetBig.setRadius(50);

                    targetSmall = new Circle();
                    targetSmall.setCenterY(200);
                    targetSmall.setRadius(25);
                    t = new Thread(() -> {
                        while (true) {
                            try {
                                TimeUnit.MILLISECONDS.sleep(20);
                            } catch (InterruptedException e) {

                            }
                            revOfTarBig = moveTarget(revOfTarBig, targetBig, 800);
                            for (int i = 0; i < 2; i++) {
                                revOfTarSmall = moveTarget(revOfTarSmall, targetSmall, 800);
                            }
                        }
                    });
                    t.start();
                }
                ids = new ArrayList<>();
                scores = new ArrayList<>();
                shots = new ArrayList<>();
                for(ClientAtServer allClients : allClients){
                    ids.add(allClients.id);
                    scores.add(allClients.score);
                    shots.add(allClients.shots);
                }
                System.out.println(targetBig.getCenterY());
                msg = new Message(Action.GO, ids, scores, shots,
                        713.0, targetBig.getCenterY(), 50.0, revOfTarBig, 849.0, targetSmall.getCenterY(), 25.0, revOfTarSmall);
            }
            else msg = new Message(action, client.id);
        }
        else if (action == Action.ARROW){
            // прикол в том, что теперь все стреляется и двигается на сервере, клиенты
            // лишь будут воспроизводить графику по запросу сервера
            msg = new Message(action, targetBig.getCenterY(), revOfTarBig, targetSmall.getCenterY(), revOfTarSmall, client.id);
        }
        else if(action == Action.RESULT){
            if(client.score >= 3){
                // при чьем-то выигрыше необходимо обновить базу данных
                Session session = sf.openSession();
                // Запрос на получение объекта Leader по значению столбца nickname
                Query<Leaders> query = session.createQuery("FROM Leaders l WHERE l.nickname = ?0", Leaders.class);
                query.setParameter(0, client.nickName);
                Leaders leader = query.uniqueResult();

                Transaction tx = session.beginTransaction(); // создаем транзакцию
                if (leader != null) { // если нашли, то обновляем
                    leader.wins++; // Изменение значения другого поля в объекте Leader
                    session.update(leader); // Сохранение изменений в базе данных
                }
                else { // если не нашли, то добавляем
                    int m = Collections.max(ids); // посчитаем новый id, и пусть он будет на 1 больше чем максимальный
                    leader = new Leaders(m + 1, client.nickName, 1);
                    session.save(leader);
                }
                tx.commit();
                session.close();

                // обновим список лидеров с базы данных
                LeadersQuery();

                num_of_ready_clients = 0;
                msg = new Message(Action.WIN, client.id, client.nickName, client.shots, client.score);
                for(ClientAtServer clientAtServer : allClients){
                    clientAtServer.shots = 0;
                    clientAtServer.score = 0;
                }
                //t.interrupt();
                //t = null;
            }
            else {
                scores.set(client.id - 1, client.score);
                shots.set(client.id - 1, client.shots);
                msg = new Message(action, targetBig.getCenterY(), revOfTarBig, targetSmall.getCenterY(), revOfTarSmall, client.shots, client.score, client.id);
            }
        }

        for(ClientAtServer allClients : allClients) {
            allClients.send(msg);
        }
    }

    void LeadersQuery(){
        // заодно откроем сессию для общения с базой данных и подкачаем оттуда данные с победами
        leaders = new ArrayList<>();
        wins = new ArrayList<>();
        ids = new ArrayList<>();

        Session session = sf.openSession();
        Query<Leaders> query = session.createQuery("FROM Leaders ORDER BY wins DESC", Leaders.class);
        List<Leaders> list = query.list();
        //leaders.addAll(list);
        for (Leaders l : list){
            leaders.add(l.nickname);
            wins.add(l.wins);
            ids.add(l.id);
            System.out.println(l);
        }
        session.close();
    }
    private void SeverStart() {
        ServerSocket ss;
        try {
            //HibernateFactory.getSessionFactory(); // это мы проверяем, работает ли hibernate
            /*leaders = new ArrayList<>();
            wins = new ArrayList<>();
            ids = new ArrayList<>();
            Session session = sf.openSession();
            Query<Leaders> query = session.createQuery("FROM Leaders ORDER BY wins DESC", Leaders.class);
            List<Leaders> list = query.list();
            //leaders.addAll(list);
            for (Leaders l : list){
                leaders.add(l.nickname);
                wins.add(l.wins);
                ids.add(l.id);
                System.out.println(l);
            }
            Transaction tx = session.beginTransaction(); // создаем транзакцию
            int m = Collections.max(ids) + 1; // посчитаем новый id, и пусть он будет на 1 больше чем максимальный

            Leaders leader = new Leaders(m, "NewGuy", 1);
                session.save(leader);

            tx.commit();
            session.close();*/

            // запросим актуальный список лидеров с базы данных
            LeadersQuery();

            ip = InetAddress.getLocalHost();
            ss = new ServerSocket(port, 0, ip);
            System.out.println("Server started");

            while (num_of_clients < 4) {
                Socket cs;
                cs = ss.accept();

                ClientAtServer client = new ClientAtServer(cs, this);
                allClients.add(client);

                System.out.println("Client connected");
                service.submit(client);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) { new MainServer().SeverStart(); }
}

package server;

import client.multiplayer.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainServer {
    int port = 3124;
    InetAddress ip = null;
    ExecutorService service = Executors.newCachedThreadPool();

    // список клиентов, подключенных к серверу, им будем отправлять текущее состояние системы
    ArrayList<ClientAtServer> allClients = new ArrayList<>();
    int num_of_clients = 0, num_of_ready_clients = 0, max_score = 0;

    double basic_arrow_pos = 150.0;

    // по сути сюда надо сложить все относительно мишеней
    double yTarBig, yTarSmall;
    int revOfTarBig = 1;
    int revOfTarSmall = 1;



    // массовая рассылка ников клиентам
    void broadCast(Action action, ClientAtServer client){
        Message msg = null;
        if(action == Action.ON_CONNECT){

            ArrayList<String> nickNames = new ArrayList<>();
            ArrayList<Double> arrows = new ArrayList<>();

            for(ClientAtServer allClients : allClients){
                nickNames.add(allClients.nickName);
                arrows.add(basic_arrow_pos * allClients.id);
            }

            msg = new Message(action, nickNames, arrows);
        }
        else if(action == Action.ON_READY){
            num_of_ready_clients++;
            if(num_of_clients == num_of_ready_clients){
                // поток, занимающийся движениями мишени
                Thread t = new Thread(() -> {
                    Frame frame = new Frame();
                    Target targetBig = new Target(50, 50, 50);
                    Target targetSmall = new Target(150, 50, 25);

                    while (true) {
                        revOfTarBig = targetBig.move(revOfTarBig, frame);
                        for (int i = 0; i < 2; i++)
                            revOfTarSmall = targetSmall.move(revOfTarSmall, frame);
                        try {
                            TimeUnit.MILLISECONDS.sleep(20);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        // во время цикла меняются параметры объектов мишеней, поэтому, например,
                        // высоту, можно легко добыть с помощью targetBig.getY();
                        yTarBig = targetBig.getY();
                        yTarSmall = targetSmall.getY();
                    }
                });
                t.start();
                ArrayList<Integer> ids = new ArrayList<>();
                ArrayList<Integer> scores = new ArrayList<>();
                ArrayList<Integer> shots = new ArrayList<>();
                for(ClientAtServer allClients : allClients){
                    ids.add(allClients.id);
                    scores.add(allClients.score);
                    shots.add(allClients.shots);
                }
                msg = new Message(Action.GO, ids, scores, shots,
                        713.0, yTarBig, 50.0, revOfTarBig, 849.0, yTarSmall, 25.0, revOfTarSmall);
            }
            else msg = new Message(action, client.id);
        }
        else if (action == Action.ARROW){
            // прикол в том, что теперь все стреляется и двигается на сервере, клиенты
            // лишь будут воспроизводить графику по запросу сервера
            msg = new Message(action, yTarBig, revOfTarBig, yTarSmall, revOfTarSmall, client.id);
        }
        else if(action == Action.RESULT){
            msg = new Message(action, yTarBig, revOfTarBig, yTarSmall, revOfTarSmall, client.shots, client.score, client.id);
        }

        for(ClientAtServer allClients : allClients) {
            allClients.send(msg);
        }
    }


    private void SeverStart() {
        ServerSocket ss;
        try {
            ip = InetAddress.getLocalHost();
            ss = new ServerSocket(port, 0, ip);
            System.out.println("Server started");

            while (num_of_clients < 4) {
                Socket cs;
                cs = ss.accept();

                ClientAtServer client = new ClientAtServer(cs, this);
                allClients.add(client);
                // разберемся с количеством клиентов и присвоим id по номеру подключения
                num_of_clients++;
                client.id = num_of_clients;
                System.out.println("Client connected");
                service.submit(client);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) { new MainServer().SeverStart(); }
}

package client.multiplayer;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


public class HelloController {
    @FXML private AnchorPane globe;
    @FXML private TextField nickname_field;
    @FXML void initialize() {
    }




    int port = 3124;
    InetAddress ip = null;
    Socket ClientSocket;
    InputStream is;
    OutputStream os;
    DataInputStream dis;
    DataOutputStream dos;
    Gson gson = new Gson();


    // final, чтобы можно было использовать в потоке при обмене сообщениями
    final Circle targetBig = new Circle();
    final Circle targetSmall = new Circle();
    final Label nickNamesColumn = new Label();
    final Label scoreColumn = new Label();
    final Label shotsColumn = new Label();
    final Label readyColumn = new Label();
    ArrayList<Line> arrows = new ArrayList<>();
    ArrayList<String> localNickNames = new ArrayList<>();
    ArrayList<Integer> localScore = new ArrayList<>();
    ArrayList<Integer> localShots = new ArrayList<>();
    int online, id;

    int revOfTarBig = 1;
    int revOfTarSmall = 1;
    //Frame frame = new Frame();
    Thread arrowThread, connectThread, targetsThread;
    boolean pause = false;

    // отправка сообщения на сервер
    void send(Message msg){
        try {
            String s_msg = gson.toJson(msg);
            System.out.println(s_msg);
            dos.writeUTF(s_msg);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public int moveTarget(int rev, Circle target, double frame) {
        double newY = target.getCenterY() + 2 * rev;
        if (newY + target.getRadius() > frame) rev = -1;
        if (newY - target.getRadius() < 0) rev = 1;
        target.setCenterY(newY);
        return rev;
    }
    public void renewStats(){
        StringBuilder a = new StringBuilder(), b = new StringBuilder(), c = new StringBuilder();
        for(int i = 0; i < online; i++){
            a.append(localNickNames.get(i)).append('\n');
            b.append(localScore.get(i)).append('\n');
            c.append(localShots.get(i)).append('\n');
        }

        // я хз, но оно жалуется, что не final, создадим новую фигню
        final String d = a.toString(), e = b.toString(), f = c.toString();

        Platform.runLater(() -> {
            nickNamesColumn.setText(d);
            scoreColumn.setText(e);
            shotsColumn.setText(f);
        });
    }
    public void syncTargets(double y1, int rev1, double y2, int rev2){
        revOfTarBig = rev1;
        revOfTarSmall = rev2;
        targetBig.setCenterY(y1);
        targetSmall.setCenterY(y2);
    }
    public void moveArrow(Line arrow){
        arrowThread = new Thread(()->{
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(20);
                } catch (InterruptedException e) {
                    arrowThread.interrupt(); // если вдруг попали в тот момент, когда поток спит
                    // вызовется исключение и по ветке catch все-таки добьем этот поток
                }
                double NX = arrow.getLayoutX() + 20;
                Platform.runLater(() -> arrow.setLayoutX(NX));
            }
        });
        arrowThread.start();
    }
    @FXML protected void Connect(){
        if(nickname_field.getText() != null){
            try {
                ip = InetAddress.getLocalHost();
                ClientSocket = new Socket(ip, port);
                System.out.println("connected");
                os = ClientSocket.getOutputStream();
                dos = new DataOutputStream(os);

                globe.getChildren().add(targetBig);
                globe.getChildren().add(targetSmall);

                globe.getChildren().add(nickNamesColumn);
                globe.getChildren().add(scoreColumn);
                globe.getChildren().add(shotsColumn);
                globe.getChildren().add(readyColumn);

                readyColumn.setLayoutX(60);
                scoreColumn.setLayoutX(60);
                shotsColumn.setLayoutX(80);

                targetBig.setVisible(false);
                targetSmall.setVisible(false);
                targetBig.setFill(Color.GREEN);
                targetSmall.setFill(Color.ORANGE);

                // старт потока, отвечающего за общение с сервером
                connectThread = new Thread(()->{
                    try {
                        is = ClientSocket.getInputStream();
                        dis = new DataInputStream(is);

                        boolean firstStart = true;
                        // прием сообщений от сервера
                        while (true){
                            String str = dis.readUTF();
                            System.out.println(str);
                            Message msg = gson.fromJson(str, Message.class);
                            System.out.println(msg);

                            if(!pause) // 0 реакции, если на паузе (ни стрелы не будут пускаться,
                                // ни мишени двигаться, ни счета меняться)
                            // расставляем принятые данные
                                if(msg.action == Action.ON_CONNECT){
                                    online = msg.nickNames.size();

                                    // если в первый раз, то добавляем все стрелы до этого и вычисляем id
                                    if(firstStart) {
                                        for(int i = 0; i < online; i++){
                                            if(Objects.equals(msg.nickNames.get(i), nickname_field.getText()))
                                                id = i + 1;
                                        }
                                        for(int i = 0; i < id - 1; i++){
                                            Line arrow = new Line();
                                            arrow.setLayoutY(150 * (i + 1));
                                            arrow.setEndX(70);
                                            arrow.setLayoutX(14);
                                            arrows.add(arrow);
                                            final int ip = i; // опять выкрутасы
                                            Platform.runLater(() -> globe.getChildren().add(arrows.get(ip)));
                                        }
                                        firstStart = false;
                                    }

                                    // разбираемся с никами
                                    localNickNames = msg.nickNames;
                                    StringBuilder a = new StringBuilder();
                                    for(int i = 0; i < online; i++)
                                        a.append(localNickNames.get(i)).append('\n');
                                    final String b = a.toString();
                                    Platform.runLater(() -> nickNamesColumn.setText(b));

                                    // добавим стрелу
                                    Line arrow = new Line();
                                    arrow.setLayoutY(150 * (online));
                                    arrow.setEndX(70);
                                    arrow.setLayoutX(14);
                                    arrows.add(arrow);

                                    Platform.runLater(() -> globe.getChildren().add(arrow));
                                }
                                else if(msg.action == Action.ON_READY){
                                    // добавлять напротив ника статус готовности из msg.id
                                    int readyId = msg.id;
                                    StringBuilder a = new StringBuilder();
                                    for(int i = 0; i < online; i++)
                                        if(readyId == i + 1)
                                            a.append("ready");
                                        else
                                            a.append('\n');

                                    final String b = a.toString();
                                    Platform.runLater(() -> readyColumn.setText(b));
                                }
                                else if(msg.action == Action.GO){
                                    revOfTarBig = msg.rev1;
                                    revOfTarSmall = msg.rev2;
                                    targetBig.setCenterX(msg.x1);
                                    targetSmall.setCenterX(msg.x2);
                                    targetBig.setRadius(msg.r1);
                                    targetSmall.setRadius(msg.r2);
                                    targetBig.setVisible(true);
                                    targetSmall.setVisible(true);
                                    syncTargets(msg.y1, msg.rev1, msg.y2, msg.rev2);

                                    // старт потока, отвечающего за движение мишеней
                                    targetsThread = new Thread(()->{
                                        while (!Thread.currentThread().isInterrupted()) {
                                            try {
                                                TimeUnit.MILLISECONDS.sleep(20);
                                            } catch (InterruptedException e) {
                                                targetsThread.interrupt(); // если вдруг попали в тот момент, когда поток спит
                                                // вызовется исключение и по ветке catch все-таки добьем этот поток
                                            }
                                            revOfTarBig = moveTarget(revOfTarBig, targetBig, 800);
                                            for (int i = 0; i < 2; i++)
                                                revOfTarSmall = moveTarget(revOfTarSmall, targetSmall, 800);
                                        }
                                    });
                                    targetsThread.start();

                                    // разбираемся со счетами
                                    localShots = msg.shots;
                                    localScore = msg.scores;
                                    StringBuilder a = new StringBuilder(), b = new StringBuilder();
                                    for(int i = 0; i < online; i++){
                                        a.append(localShots.get(i)).append('\n');
                                        b.append(localScore.get(i)).append('\n');
                                    }
                                    final String c = a.toString(), d = b.toString();
                                    Platform.runLater(() -> {
                                        readyColumn.setText("");
                                        shotsColumn.setText(c);
                                        scoreColumn.setText(d);
                                    });
                                }
                                else if(msg.action == Action.ON_PAUSE){
                                    syncTargets(msg.y1, msg.rev1, msg.y2, msg.rev2);
                                    revOfTarBig = msg.rev1;
                                    revOfTarSmall = msg.rev2;
                                    // старт потока, отвечающего за движение мишеней
                                    targetsThread = new Thread(()->{
                                        while (!Thread.currentThread().isInterrupted()) {
                                            try {
                                                TimeUnit.MILLISECONDS.sleep(20);
                                            } catch (InterruptedException e) {
                                                targetsThread.interrupt(); // если вдруг попали в тот момент, когда поток спит
                                                // вызовется исключение и по ветке catch все-таки добьем этот поток
                                            }
                                            revOfTarBig = moveTarget(revOfTarBig, targetBig, 800);
                                            for (int i = 0; i < 2; i++) {
                                                revOfTarSmall = moveTarget(revOfTarSmall, targetSmall, 800);
                                            }
                                        }
                                    });
                                    targetsThread.start();
                                    // разбираемся со счетами
                                    localShots = msg.shots;
                                    localScore = msg.scores;
                                    StringBuilder a = new StringBuilder(), b = new StringBuilder();
                                    for(int i = 0; i < online; i++){
                                        a.append(localShots.get(i)).append('\n');
                                        b.append(localScore.get(i)).append('\n');
                                    }
                                    final String c = a.toString(), d = b.toString();
                                    Platform.runLater(() -> {
                                        targetBig.setVisible(true);
                                        targetSmall.setVisible(true);
                                        for (Line ar : arrows) ar.setVisible(true);
                                        shotsColumn.setText(c);
                                        scoreColumn.setText(d);
                                    });
                                }
                                else if(msg.action == Action.ARROW){
                                    syncTargets(msg.y1, msg.rev1, msg.y2, msg.rev2);
                                    moveArrow(arrows.get(msg.id - 1));
                                    // так как ArrayList начинается с 0, то из id всегда вычитаем 1
                                    // а далее увеличиваем количество выстрелов на 1
                                    localShots.set(msg.id - 1, localShots.get(msg.id - 1) + 1);
                                    renewStats();
                                }
                                else if(msg.action == Action.RESULT){
                                    syncTargets(msg.y1, msg.rev1, msg.y2, msg.rev2);
                                    arrowThread.interrupt();
                                    arrows.get(msg.id - 1).setLayoutX(14);
                                    localScore.set(msg.id - 1, msg.score);
                                    renewStats();
                                }
                                else if(msg.action == Action.WIN){
                                    arrowThread.interrupt();
                                    arrows.get(msg.id - 1).setLayoutX(14);
                                    renewStats();

                                    targetBig.setVisible(false);
                                    targetSmall.setVisible(false);
                                    targetsThread.interrupt();
                                    //targetsThread=null;

                                    Label win = new Label("Выиграл " + msg.nickName + "!");
                                    win.setLayoutX(300);
                                    win.setLayoutY(300);
                                    Platform.runLater(() -> globe.getChildren().add(win));
                                    try{
                                        TimeUnit.SECONDS.sleep(5);
                                    } catch (InterruptedException e) {
                                        throw new RuntimeException(e);
                                    }
                                    Platform.runLater(() -> globe.getChildren().remove(win));

                                    for(int i = 0; i < online; i++){
                                        localScore.set(i, 0);
                                        localShots.set(i, 0);
                                    }
                                    StringBuilder b = new StringBuilder(), c = new StringBuilder();
                                    for(int i = 0; i < online; i++){
                                        b.append(localScore.get(i)).append('\n');
                                        c.append(localShots.get(i)).append('\n');
                                    }
                                    // я хз, но оно жалуется, что не final, создадим новую фигню
                                    final String e = b.toString(), f = c.toString();
                                    Platform.runLater(() -> {
                                        scoreColumn.setText(e);
                                        shotsColumn.setText(f);
                                    });
                                }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                connectThread.start();
                Message msg = new Message(Action.CONNECT, nickname_field.getText());
                send(msg);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    @FXML protected void Ready() {
        Message msg;
        if(!pause) msg = new Message(Action.READY);
        else {
            msg = new Message(Action.PAUSE);
            pause = false;
        }
        send(msg);
    }
    @FXML protected void Pause(){
        pause = true;
        targetsThread.interrupt();
        Platform.runLater(() -> {
            targetBig.setVisible(false);
            targetSmall.setVisible(false);
            for (Line ar : arrows) ar.setVisible(false);
        });
    }
    @FXML protected void Shot(){
        Message msg = new Message(Action.SHOT);
        send(msg);
    }
}

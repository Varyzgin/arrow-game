package server;

import client.multiplayer.Action;
import client.multiplayer.Message;
import com.google.gson.Gson;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class ClientAtServer implements Runnable {
    public int id;
    public String nickName;
    public int score = 0;
    public int shots = 0;
    public Boolean online = false;

    Socket cs;
    MainServer ms;
    InputStream is;
    OutputStream os;
    DataInputStream dis;
    DataOutputStream dos;
    Gson gson = new Gson();

    // тут клиент только подключается
    public ClientAtServer(Socket cs, MainServer ms) {
        this.cs = cs;
        this.ms = ms;
        try {
            os = cs.getOutputStream();
            dos = new DataOutputStream(os);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    void send(Message msg){
        try {
            System.out.println(msg);
            String s_msg = gson.toJson(msg);

            System.out.println(s_msg);
            dos.writeUTF(s_msg);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    double xArrow;

    // подключение клиента
    @Override
    public void run() {
        try {
            is = cs.getInputStream();
            dis = new DataInputStream(is);
            System.out.println("Client thread started");

            // прием сообщений от сервера
            while(true) {
                String str = dis.readUTF();
                System.out.println(str);
                Message msg = gson.fromJson(str, Message.class);
                System.out.println(msg);
                // тут содержатся варианты интерпретаций сообщений
                if (msg.action == Action.CONNECT) {
                    // при приеме ника (заявки на подключение) устанавливаем ник
                    // на пользователя, отсылаем ник другим пользователям (вместе
                    // со счетом), создаем ему стрелу, отсылаем ее координаты
                    // другим пользователям
                    nickName = msg.nickName;
                    ms.broadCast(Action.ON_CONNECT, this);
                } else if (msg.action == Action.READY) {
                    ms.broadCast(Action.ON_READY, this);
                } else if (msg.action == Action.SHOT) {
                    ms.broadCast(Action.ARROW, this);
                    xArrow = 84;
                    double y = 150 * id; // расположение стрелы по вертикали
                    // вычисление попаданий, для этого создаем поток стрелы на сервере
                    shots++;
                    while (true) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(1);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        xArrow += 1;

                        if (xArrow == 713) // достиг большой мишени
                            if (y >= ms.yTarBig - 50 && y <= ms.yTarBig + 50) {
                                score++;
                                break;
                            }
                        if (xArrow == 849)  // достиг маленькой мишени
                            if (y >= ms.yTarSmall - 25 && y <= ms.yTarSmall + 25) {
                                score += 2;
                                break;
                            }
                        if (xArrow >= 900) // послыаем MICRO RES и ломаем цикл
                            break;
                    }
                    ms.broadCast(Action.RESULT, this);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

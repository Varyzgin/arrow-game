package a;

import java.util.ArrayList;

public class Message {
    public String nickName;
    public Boolean ready;
    public Boolean shot;
    public ArrayList<Integer> ids;
    public ArrayList<String> nickNames;
    public ArrayList<Integer> scores;
    public ArrayList<Integer> shots;
    public ArrayList<Integer> wins;
    public ArrayList<Double> arrows; // положение стрел
    public double x1;
    public double y1;
    public double r1;
    public int rev1;
    public double x2;
    public double y2;
    public double r2;
    public int rev2;
    public int score;
    public int id;
    public int sho;
    public Action action;

    // CONNECT
    public Message(Action action, String nickName) {
        this.action = action;
        this.nickName = nickName;
    }

    // READY or SHOT or simple message
    public Message(Action action) {
        this.action = action;
    }



    // ON_CONNECT
    public Message(Action action, ArrayList<String> nickNames, ArrayList<Double> arrows) {
        this.action = action;
        this.nickNames = nickNames;
        this.arrows = arrows;
    }
    // ON_READY
    public Message(Action action, Integer id) {
        this.action = action;
        this.id = id;
    }
    // GO
    public Message(Action action, ArrayList<Integer> ids,
                   ArrayList<Integer> scores,
                   ArrayList<Integer> shots,
                   double x1, double y1, double r1, int rev1,
                   double x2, double y2, double r2, int rev2) {
        this.action = action;
        this.ids = ids;
        this.scores = scores;
        this.shots = shots;
        this.x1 = x1;
        this.y1 = y1;
        this.r1 = r1;
        this.rev1 = rev1;
        this.x2 = x2;
        this.y2 = y2;
        this.r2 = r2;
        this.rev2 = rev2;
    }
    // ON_PAUSE
    public Message(Action action, ArrayList<Integer> ids, ArrayList<Integer> scores,
                   ArrayList<Integer> shots, double y1, int rev1, double y2, int rev2) {
        this.action = action;
        this.ids = ids;
        this.scores = scores;
        this.shots = shots;
        this.y1 = y1;
        this.rev1 = rev1;
        this.y2 = y2;
        this.rev2 = rev2;
    }
    // ARROW
    public Message(Action action, double y1, int rev1, double y2, int rev2, int id) {
        this.action = action;
        this.y1 = y1;
        this.rev1 = rev1;
        this.y2 = y2;
        this.rev2 = rev2;
        this.id = id;
    }
    // RESULT
    public Message(Action action, double y1, int rev1, double y2, int rev2, int shots, int score, int id) {
        this.action = action;
        this.y1 = y1;
        this.rev1 = rev1;
        this.y2 = y2;
        this.rev2 = rev2;
        this.sho = shots;
        this.score = score;
        this.id = id;
    }
    // WIN
    public Message(Action action, int id, String nickName, int sho, int score) {
        this.action = action;
        this.id = id;
        this.nickName = nickName;
        this.sho = sho;
        this.score = score;
    }
    // ON_LEADERS
    public Message(Action action, ArrayList<String> nickNames, ArrayList<Integer> wins, int kostul) {
        this.action = action;
        this.nickNames = nickNames;
        this.wins = wins;
    }

    @Override
    public String toString() {
        String str = "Message{" + "action=" + this.action;
        if(this.action == Action.CONNECT)
            str = str + ", nickName='" + nickName + '\'';

        else if(this.action == Action.ON_CONNECT) str = str +
                ", nickNames=" + nickNames +
                ", arrows=" + arrows;
        else if(this.action == Action.ON_READY) str = str + ", id=" + id;
        else if(this.action == Action.GO) str = str +
                ", ids=" + ids +
                ", scores=" + scores +
                ", shots=" + shots +
                ", x1=" + x1 + ", y1=" + y1 + ", r1=" + r1 +
                ", x2=" + x2 + ", y2=" + y2 + ", r2=" + r2 +
                ", rev1=" + rev1 + ", rev2=" + rev2;
        else if(this.action == Action.ON_PAUSE) str = str +
                ", ids=" + ids +
                ", scores=" + scores +
                ", shots=" + shots +
                ", y1=" + y1 + ", rev1=" + rev1 +
                ", y2=" + y2 + ", rev2=" + rev2;
        else if(this.action == Action.ARROW) str = str +
                ", y1=" + y1 + ", rev1=" + rev1 +
                ", y2=" + y2 + ", rev2=" + rev2 +
                ", id=" + id;
        else if(this.action == Action.RESULT) str = str +
                ", y1=" + y1 + ", rev1=" + rev1 +
                ", y2=" + y2 + ", rev2=" + rev2 +
                ", sho=" + sho + ", score=" + score +
                ", id=" + id;
        else if(this.action == Action.WIN) str = str +
                ", id=" + id + ", nickName=" + nickName +
                ", sho=" + sho + ", score=" + score;
        else if(this.action == Action.ON_LEADERS) str = str +
                ", nickNames=" + nickNames +
                ", wins=" + wins;
        str += '}';
        return str;
    }
}
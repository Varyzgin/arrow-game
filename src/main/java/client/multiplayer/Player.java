package client.multiplayer;

public class Player {
    int id;
    String nickName;
    Integer points;
    Integer shots;

    public Player(String nickName, Integer points, Integer shots) {
        this.nickName = nickName;
        this.points = points;
        this.shots = shots;
    }

    public int getid() { return id; }

    public String getNik() { return nickName; }

    public void setid(int id) { this.id = id; }

    public void setNik(String nik) { this.nickName = nik; }

    @Override
    public String toString() {
        return "Player{" +
                "id=" + id +
                ", nickName='" + nickName + '\'' +
                ", points=" + points +
                ", shots=" + shots +
                '}';
    }
}

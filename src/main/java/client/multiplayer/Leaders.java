package client.multiplayer;

public class Leaders {
    int id;
    String nickName;
    Integer wins;

    public Leaders(int id, String nickName, Integer wins) {
        this.id = id;
        this.nickName = nickName;
        this.wins = wins;
    }

    public String toString() {
        return "Leaders{id=" + this.id
                + ", nickName='" + this.nickName
                + "', wins=" + this.wins +
                "}";
    }
}

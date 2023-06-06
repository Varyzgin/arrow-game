package a;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Leaders {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    int id;
    public String nickname;
    public Integer wins;
    public Leaders() {

    }
    public Leaders(int id, String nickname, Integer wins) {
        this.id = id;
        this.nickname = nickname;
        this.wins = wins;
    }

    public String toString() {
        return "Leaders{id=" + this.id
                + ", nickname='" + this.nickname
                + "', wins=" + this.wins +
                "}";
    }
}

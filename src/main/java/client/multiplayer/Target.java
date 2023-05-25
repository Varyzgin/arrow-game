package client.multiplayer;

public class Target {
    double x = 0, y = 0, r = 10;
    public Target(double x, double y, double r){
        this.x = x;
        this.y = y;
        this.r = r;
    }
    public double getY() { return y; }
    public double getR() { return r; }

    public void setY(double y) {
        this.y = y;
    }

    public String toStringAll() {
        return "Target{" +
                "x=" + x +
                ", y=" + y +
                ", r=" + r +
                '}';
    }
    public String toStringY() {
        return "Target{" +
                ", y=" + y +
                '}';
    }
    public int move(int rev, Frame frame) {
        double newY = this.y + 2 * rev;
        if (newY + this.r > frame.getHeight()) rev = -1;
        if (newY - this.r < 0) rev = 1;
        this.y = newY;
        return rev;
    }
}

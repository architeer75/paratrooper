package pe.kr.architeer.entity;

/** 대공포탄 */
public final class Shot {
    public double x, y, vx, vy;

    /** 탄 위치가 (x, y, w, h) 사각형 안에 있는지 */
    public boolean in(double x, double y, double w, double h) {
        return this.x >= x && this.x < x + w && this.y >= y && this.y < y + h;
    }
}

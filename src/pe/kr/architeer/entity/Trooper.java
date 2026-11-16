package pe.kr.architeer.entity;

import java.util.List;

/** 공수부대원: 자유 낙하 → 낙하산 → 착지, 또는 낙하산을 잃고 추락 */
public final class Trooper {
    public double x, y, vy;           // vy: 낙하산을 잃고 추락할 때의 속도
    public int col, openY;
    public boolean chute, chuteLost, landed;
    public List<double[]> path;       // 침투 애니메이션용 경유점
}

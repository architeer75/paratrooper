package pe.kr.architeer.entity;

/** 공수부대원을 떨어뜨리는 헬기 */
public final class Heli {
    public double x;
    public int y, dir, drops, lastCol = -1;     // dir: +1 오른쪽, -1 왼쪽 / lastCol: 마지막으로 투하를 검사한 열
}

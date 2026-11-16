package pe.kr.architeer.entity;

/** 추락사한 대원. 충돌 판정 없이 쓰러지는 애니메이션만 보여 주고 사라진다. */
public final class Corpse {
    public double x;
    public int floor, dir, age;       // floor: 몸이 닿은 바닥 y, dir: 쓰러지는 방향
}

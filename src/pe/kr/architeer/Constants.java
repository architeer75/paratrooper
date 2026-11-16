package pe.kr.architeer;

import java.awt.Color;

/** 게임 전역 상수: 화면·지형 좌표, CGA 팔레트, 속도·난이도 값 (틱 = 1/60초) */
final class Constants {
    private Constants() {}

    // ───────────────────────── 화면/지형 상수 ─────────────────────────
    static final int W = 320, H = 200, SCALE = 3;
    static final int GROUND = 186;                       // 지면 y (발이 닿는 높이)
    static final int BASE_X = 144, BASE_W = 32, BASE_H = 16;
    static final int BASE_TOP = GROUND - BASE_H;         // 포대 윗면 y
    static final int PIVOT_X = 160, PIVOT_Y = BASE_TOP - 8;
    static final int BARREL_LEN = 16;
    static final int COL_W = 8, COLS = W / COL_W;        // 낙하 열(8px 격자)
    static final int BASE_COL_L = BASE_X / COL_W;        // 18
    static final int BASE_COL_R = (BASE_X + BASE_W) / COL_W - 1; // 21
    static final int TR_W = 5, TR_H = 8;                 // 공수부대원 크기
    static final int GY = GROUND - TR_H;                 // 지면에 선 대원의 y

    // CGA 팔레트 1 (고휘도)
    static final Color BLACK = Color.BLACK;
    static final Color CYAN = new Color(0x55FFFF);
    static final Color MAGENTA = new Color(0xFF55FF);
    static final Color WHITE = Color.WHITE;
    static final Color[] PALETTE = {CYAN, MAGENTA, WHITE};

    // ───────────────────────── 속도/난이도 상수 (틱 = 1/60초) ─────────────────────────
    static final double TURN_SPEED = Math.toRadians(2.0);
    static final double MAX_ANGLE = Math.toRadians(86);
    static final double SHOT_SPEED = 3.0;
    static final int FIRE_COOLDOWN = 12;
    static final double HELI_SPEED = 1.2, JET_SPEED = 2.4, BOMB_SPEED = 1.4;
    static final double FALL_FREE = 1.3, FALL_CHUTE = 0.45, FALL_DEAD = 2.4;
    static final double FALL_ACCEL = 0.03;               // 낙하산을 잃은 뒤 FALL_DEAD까지 가속
    static final int SHOT_SUBSTEPS = 4;                  // 빠른 표적을 뚫고 지나가지 않도록 나눠서 이동
    static final double WALK_SPEED = 0.6;
    static final int HELI_PHASE_TICKS = 1500;            // 헬기 웨이브 길이(약 25초)
    static final double JET_WAVE_MULT = 2.5;             // 제트기 웨이브 대수 = (2 + wave) × 이 값
    static final int DEAD_TICKS = 240;                   // 시체가 남아 있는 시간(4초), 마지막 40틱은 깜빡이며 사라짐
}

package pe.kr.architeer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import pe.kr.architeer.entity.*;
import pe.kr.architeer.sound.Sound;

import static pe.kr.architeer.Constants.*;

/** 게임 상태와 규칙: 웨이브 진행, 이동, 충돌, 점수, 침투. 화면 그리기와 입력 장치는 알지 못한다. */
final class Game {
    final Random rnd = new Random();

    final List<Heli> helis = new ArrayList<Heli>();
    final List<Jet> jets = new ArrayList<Jet>();
    final List<Bomb> bombs = new ArrayList<Bomb>();
    final List<Shot> shots = new ArrayList<Shot>();
    final List<Bit> bits = new ArrayList<Bit>();
    final List<Trooper> troopers = new ArrayList<Trooper>();
    final List<Trooper> walkers = new ArrayList<Trooper>();
    final List<Corpse> corpses = new ArrayList<Corpse>();
    final int[] stack = new int[COLS];                   // 열별 착지 인원

    State state = State.TITLE;
    boolean keyLeft, keyRight, keyFire, paused;
    double angle;                                        // 0 = 수직 위, +는 오른쪽
    int score, hiScore, fireCooldown, ticks, dyingTimer;
    int wave, phase, phaseT, spawnT, jetsLeft;           // phase: 0 헬기, 1 휴식, 2 제트기, 3 휴식
    int pendingSide;                                     // 4명이 착지한 쪽 (-1 왼쪽, +1 오른쪽, 0 없음)
    int walkerIdx;

    void newGame() {
        helis.clear(); jets.clear(); bombs.clear(); shots.clear(); bits.clear();
        troopers.clear(); walkers.clear(); corpses.clear();
        Arrays.fill(stack, 0);
        angle = 0; score = 0; fireCooldown = 0;
        wave = 0; phase = 0; phaseT = 0; spawnT = 60; jetsLeft = 0;
        pendingSide = 0; walkerIdx = 0; paused = false;
        state = State.PLAY;
    }

    void tick() {
        ticks++;
        if (paused || state == State.TITLE) return;

        boolean alive = state == State.PLAY || state == State.INVASION;
        if (alive) {
            updateTurret();
            if (state == State.PLAY && pendingSide == 0) updateWaves();
        }
        updateHelis();
        updateJets();
        updateBombs();
        updateTroopers();
        updateCorpses();
        updateShots();
        updateBits();

        if (state == State.PLAY && pendingSide != 0 && skyClear()) beginInvasion(pendingSide);
        if (state == State.INVASION) updateInvasion();
        if (state == State.DYING && --dyingTimer <= 0) state = State.OVER;
    }

    void updateTurret() {
        if (keyLeft) angle -= TURN_SPEED;
        if (keyRight) angle += TURN_SPEED;
        angle = Math.max(-MAX_ANGLE, Math.min(MAX_ANGLE, angle));
        if (fireCooldown > 0) fireCooldown--;
        if (keyFire && fireCooldown == 0) {
            Shot s = new Shot();
            s.vx = Math.sin(angle) * SHOT_SPEED;
            s.vy = -Math.cos(angle) * SHOT_SPEED;
            s.x = PIVOT_X + Math.sin(angle) * BARREL_LEN;
            s.y = PIVOT_Y - Math.cos(angle) * BARREL_LEN;
            shots.add(s);
            fireCooldown = FIRE_COOLDOWN;
            if (score > 0) score--;                      // 발사 1회당 -1점 (0 미만 불가)
            Sound.play(Sound.FLAK, Math.sin(angle) * 0.4);
        }
    }

    // 헬기 웨이브(점점 낮은 고도) → 휴식 → 제트기 웨이브 → 휴식 → 다음 웨이브
    void updateWaves() {
        switch (phase) {
            case 0:
                phaseT++;
                if (--spawnT <= 0) {
                    int lanes = 1 + Math.min(3, phaseT * 4 / HELI_PHASE_TICKS);
                    int lane = rnd.nextInt(lanes);
                    Heli h = new Heli();
                    h.dir = lane % 2 == 0 ? 1 : -1;      // 같은 고도는 같은 방향 → 겹침 방지
                    h.x = h.dir > 0 ? -24 : W;
                    h.y = 6 + lane * 13;
                    helis.add(h);
                    spawnT = Math.max(25, 70 - wave * 8) + rnd.nextInt(40);
                }
                if (phaseT >= HELI_PHASE_TICKS) { phase = 1; phaseT = 0; }
                break;
            case 2:
                if (jetsLeft > 0 && --spawnT <= 0) {
                    Jet j = new Jet();
                    j.dir = rnd.nextBoolean() ? 1 : -1;
                    j.x = j.dir > 0 ? -24 : W;
                    j.y = 8 + rnd.nextInt(3) * 10;
                    jets.add(j);
                    jetsLeft--;
                    spawnT = 80;
                }
                if (jetsLeft == 0) { phase = 3; phaseT = 0; }
                break;
            default:                                     // 1, 3: 하늘이 비면 잠시 휴식
                if (helis.isEmpty() && jets.isEmpty() && bombs.isEmpty() && ++phaseT >= 100) {
                    if (phase == 1) { phase = 2; jetsLeft = (int) Math.round((2 + wave) * JET_WAVE_MULT); spawnT = 0; }
                    else { phase = 0; wave++; spawnT = 30; }
                    phaseT = 0;
                }
        }
    }

    void updateHelis() {
        for (Iterator<Heli> it = helis.iterator(); it.hasNext(); ) {
            Heli h = it.next();
            h.x += h.dir * HELI_SPEED;
            if (h.x < -30 || h.x > W + 6) { it.remove(); continue; }
            double cx = h.x + 12;
            int col = (int) Math.floor(cx / COL_W);
            if (col != h.lastCol && col >= 1 && col < COLS - 1) {
                h.lastCol = col;
                boolean overBase = col >= BASE_COL_L && col <= BASE_COL_R;
                double p = Math.min(0.10, 0.035 + wave * 0.01) * (overBase ? 0.3 : 1.0);
                if (state == State.PLAY && pendingSide == 0 && h.drops < 3 && rnd.nextDouble() < p) {
                    Trooper t = new Trooper();
                    t.col = col;
                    t.x = col * COL_W + 2;
                    t.y = h.y + 10;
                    t.openY = 60 + rnd.nextInt(55);
                    troopers.add(t);
                    h.drops++;
                }
            }
        }
    }

    void updateJets() {
        for (Iterator<Jet> it = jets.iterator(); it.hasNext(); ) {
            Jet j = it.next();
            j.x += j.dir * JET_SPEED;
            if (j.x < -30 || j.x > W + 6) { it.remove(); continue; }
            double cx = j.x + 12;
            if (!j.dropped && Math.abs(cx - PIVOT_X) < 110 && (state == State.PLAY || state == State.INVASION)) {
                j.dropped = true;
                Bomb b = new Bomb();
                b.x = cx; b.y = j.y + 8;
                double dx = PIVOT_X - b.x, dy = PIVOT_Y - b.y, d = Math.hypot(dx, dy);
                b.vx = dx / d * BOMB_SPEED; b.vy = dy / d * BOMB_SPEED;   // 포탑을 정조준
                bombs.add(b);
                Sound.play(Sound.WHISTLE, pan(cx));
            }
        }
    }

    void updateBombs() {
        for (Iterator<Bomb> it = bombs.iterator(); it.hasNext(); ) {
            Bomb b = it.next();
            b.x += b.vx; b.y += b.vy;
            if (Math.hypot(b.x - PIVOT_X, b.y - PIVOT_Y) < 6 || b.y > BASE_TOP) {
                it.remove();
                destroyTurret();
            }
        }
    }

    void updateTroopers() {
        for (Iterator<Trooper> it = troopers.iterator(); it.hasNext(); ) {
            Trooper t = it.next();
            if (t.landed) continue;
            if (!t.chute && !t.chuteLost && t.y >= t.openY) t.chute = true;
            if (t.chuteLost) t.vy = Math.min(FALL_DEAD, t.vy + FALL_ACCEL);
            t.y += t.chuteLost ? t.vy : t.chute ? FALL_CHUTE : FALL_FREE;
            double feet = t.y + TR_H;

            if (t.col >= BASE_COL_L && t.col <= BASE_COL_R) {            // 포대 바로 위
                if (feet >= BASE_TOP) {
                    it.remove();
                    if (t.chuteLost) fallDeath(t.x, BASE_TOP);
                    else destroyTurret();                                // 포대에 직접 착지 → 게임 오버
                }
                continue;
            }
            int limit = GROUND - stack[t.col] * TR_H;
            if (feet < limit) continue;

            if (t.chuteLost) {                                           // 낙하산 잃고 추락: 밑에 있던 대원도 사망
                it.remove();
                Trooper top = topOfColumn(t.col);
                if (top != null) {                                       // 깔린 대원도 함께 나자빠지고, 떨어진 대원은 그 위에 눕는다
                    fallDeath(top.x, limit);
                    limit -= Sprites.DEAD_A.getHeight();
                    top.landed = false; top.y = -1000; top.chuteLost = true; top.col = -1; stack[t.col]--;
                }
                fallDeath(t.x, limit);
                continue;
            }
            t.landed = true; t.chute = false;
            t.y = limit - TR_H;
            stack[t.col]++;
            Sound.play(Sound.THUD, pan(t.x));
            if (state != State.PLAY) continue;

            // 포대 바로 옆 열에 3명이 쌓이면 맨 위 대원이 포대로 뛰어오른다
            if ((t.col == BASE_COL_L - 1 || t.col == BASE_COL_R + 1) && stack[t.col] >= 3) {
                int side = t.col < BASE_COL_L ? -1 : 1;
                t.path = new ArrayList<double[]>();
                t.path.add(new double[]{mirror(150, side), GY - 16});
                walkers.clear(); walkers.add(t); walkerIdx = 0;
                state = State.INVASION;
            } else {
                int left = 0, right = 0;
                for (int c = 0; c < BASE_COL_L; c++) left += stack[c];
                for (int c = BASE_COL_R + 1; c < COLS; c++) right += stack[c];
                if (left >= 4) pendingSide = -1; else if (right >= 4) pendingSide = 1;
            }
        }
        // 추락에 깔려 제거 표시된 대원 정리
        for (Iterator<Trooper> it = troopers.iterator(); it.hasNext(); ) if (it.next().col == -1) it.remove();
    }

    void fallDeath(double x, int floor) {
        Corpse c = new Corpse();
        c.x = x; c.floor = floor; c.dir = rnd.nextBoolean() ? 1 : -1;
        corpses.add(c);
        for (int i = 0; i < 12; i++) {                                   // 충격으로 튀는 피(자홍)와 흙먼지(청록)
            Bit b = new Bit();
            b.x = x + 2; b.y = floor - 2;
            b.vx = (rnd.nextDouble() - 0.5) * 2.4;
            b.vy = -0.4 - rnd.nextDouble() * 1.2;
            b.color = i < 8 ? MAGENTA : CYAN;
            b.life = 15 + rnd.nextInt(20);
            bits.add(b);
        }
        Sound.play(Sound.SPLAT, pan(x));
    }

    void updateCorpses() {
        for (Iterator<Corpse> it = corpses.iterator(); it.hasNext(); ) if (++it.next().age >= DEAD_TICKS) it.remove();
    }

    Trooper topOfColumn(int col) {
        Trooper top = null;
        for (Trooper t : troopers) if (t.landed && t.col == col && (top == null || t.y < top.y)) top = t;
        return top;
    }

    void updateShots() {
        for (Iterator<Shot> it = shots.iterator(); it.hasNext(); ) {
            Shot s = it.next();
            for (int k = 0; k < SHOT_SUBSTEPS; k++) {
                s.x += s.vx / SHOT_SUBSTEPS; s.y += s.vy / SHOT_SUBSTEPS;
                if (s.x < 0 || s.x >= W || s.y < 0 || hitSomething(s)) { it.remove(); break; }
            }
        }
    }

    boolean hitSomething(Shot s) {
        for (Iterator<Bomb> it = bombs.iterator(); it.hasNext(); ) {
            Bomb b = it.next();
            if (Math.abs(s.x - b.x) <= 3.5 && Math.abs(s.y - b.y) <= 3.5) {
                it.remove(); score += 25; explode(b.x, b.y, 10, false); return true;
            }
        }
        for (Iterator<Heli> it = helis.iterator(); it.hasNext(); ) {
            Heli h = it.next();
            if (s.in(h.x, h.y, 24, 9)) { it.remove(); score += 5; explode(h.x + 12, h.y + 4, 22, false); return true; }
        }
        for (Iterator<Jet> it = jets.iterator(); it.hasNext(); ) {
            Jet j = it.next();
            if (s.in(j.x, j.y, 24, 7)) { it.remove(); score += 5; explode(j.x + 12, j.y + 3, 22, false); return true; }
        }
        for (Iterator<Trooper> it = troopers.iterator(); it.hasNext(); ) {
            Trooper t = it.next();
            if (t.landed) continue;
            int pad = t.chuteLost ? 2 : 0;                               // 추락 중인 대원은 판정을 넉넉하게
            if (s.in(t.x - 1 - pad, t.y - pad, TR_W + 2 + 2 * pad, TR_H + 2 * pad)) {   // 몸통 명중
                it.remove(); score += 2; explode(t.x + 2, t.y + 4, 8, false); return true;
            }
            if (t.chute && s.in(t.x - 3, t.y - 8, 11, 8)) {             // 낙하산 명중 → 추락
                t.chute = false; t.chuteLost = true; t.vy = FALL_CHUTE; score += 2;
                explode(t.x + 2, t.y - 5, 6, false); return true;
            }
        }
        return false;
    }

    void updateBits() {
        for (Iterator<Bit> it = bits.iterator(); it.hasNext(); ) {
            Bit b = it.next();
            b.x += b.vx; b.y += b.vy; b.vy += 0.05;
            if (--b.life <= 0 || b.y >= GROUND - 1) it.remove();
        }
    }

    void explode(double x, double y, int n, boolean big) {
        for (int i = 0; i < n; i++) {
            Bit b = new Bit();
            double a = rnd.nextDouble() * Math.PI * 2, v = (big ? 0.5 : 0.3) + rnd.nextDouble() * (big ? 2.5 : 1.4);
            b.x = x; b.y = y; b.vx = Math.cos(a) * v; b.vy = Math.sin(a) * v - (big ? 1.2 : 0.3);
            b.color = PALETTE[rnd.nextInt(3)];
            b.life = 30 + rnd.nextInt(big ? 90 : 40);
            bits.add(b);
        }
        Sound.play(big ? Sound.BLAST_BIG : n >= 20 ? Sound.BLAST_AIR : Sound.POP, pan(x));
    }

    static double pan(double x) { return (x / W * 2 - 1) * 0.8; }

    boolean skyClear() {
        if (!helis.isEmpty() || !jets.isEmpty() || !bombs.isEmpty()) return false;
        for (Trooper t : troopers) if (!t.landed) return false;
        return true;
    }

    // side 기준(왼쪽 좌표계)의 x를 좌/우 대칭 변환
    static double mirror(double x, int side) { return side < 0 ? x : W - TR_W - x; }

    // 한쪽에 4명 착지 + 하늘이 비면: 인간 피라미드를 쌓아 포대로 올라간다
    void beginInvasion(final int side) {
        List<Trooper> cand = new ArrayList<Trooper>();
        for (Trooper t : troopers)
            if (t.landed && (side < 0 ? t.col < BASE_COL_L : t.col > BASE_COL_R)) cand.add(t);
        Collections.sort(cand, new Comparator<Trooper>() {
            @Override public int compare(Trooper a, Trooper b) {
                return Double.compare(Math.abs(a.x - PIVOT_X), Math.abs(b.x - PIVOT_X));
            }
        });
        double[][][] routes = {
                {{138, GY}},                                                     // 1번: 포대 벽에 붙어 선다
                {{132, GY}, {132, GY - 8}, {138, GY - 8}},                       // 2번: 1번 위로
                {{132, GY}},                                                     // 3번: 1번 옆에 선다
                {{126, GY}, {126, GY - 8}, {132, GY - 8}, {132, GY - 16},        // 4번: 3번 → 2번을 딛고
                        {138, GY - 16}, {150, GY - 16}}                          //      포대 위 포탑으로
        };
        walkers.clear();
        for (int i = 0; i < 4 && i < cand.size(); i++) {
            Trooper t = cand.get(i);
            t.y = GY;                                                            // 쌓여 있던 대원은 지면으로 내린다
            t.path = new ArrayList<double[]>();
            for (double[] p : routes[i]) t.path.add(new double[]{mirror(p[0], side), p[1]});
            walkers.add(t);
        }
        walkerIdx = 0;
        state = State.INVASION;
    }

    void updateInvasion() {
        if (walkerIdx >= walkers.size()) { destroyTurret(); return; }
        Trooper t = walkers.get(walkerIdx);
        if (t.path.isEmpty()) { walkerIdx++; return; }
        double[] p = t.path.get(0);
        double dx = p[0] - t.x, dy = p[1] - t.y, d = Math.hypot(dx, dy);
        if (d <= WALK_SPEED) { t.x = p[0]; t.y = p[1]; t.path.remove(0); }
        else { t.x += dx / d * WALK_SPEED; t.y += dy / d * WALK_SPEED; }
        if (ticks % 12 == 0) Sound.play(Sound.STEP, pan(t.x));
    }

    void destroyTurret() {
        if (state != State.PLAY && state != State.INVASION) return;
        state = State.DYING;
        dyingTimer = 150;
        hiScore = Math.max(hiScore, score);
        explode(PIVOT_X, PIVOT_Y, 90, true);
    }
}

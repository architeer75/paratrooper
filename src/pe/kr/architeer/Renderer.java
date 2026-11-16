package pe.kr.architeer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import pe.kr.architeer.entity.*;

import static pe.kr.architeer.Constants.*;
import static pe.kr.architeer.Sprites.*;

/** 게임 상태를 320x200 CGA 화면에 그린다. */
final class Renderer {
    final Game game;
    final Font font = new Font(Font.MONOSPACED, Font.BOLD, 10);

    Renderer(Game game) { this.game = game; }

    void render(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setColor(BLACK);
        g.fillRect(0, 0, W, H);
        g.setFont(font);

        // 지면, 포대
        g.setColor(CYAN);
        g.fillRect(0, GROUND, W, 1);
        g.setColor(WHITE);
        g.fillRect(BASE_X, BASE_TOP, BASE_W, BASE_H);

        // 포탑
        if (game.state != State.DYING && game.state != State.OVER) {
            g.setColor(CYAN);
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
            g.drawLine(PIVOT_X, PIVOT_Y, (int) Math.round(PIVOT_X + Math.sin(game.angle) * BARREL_LEN),
                    (int) Math.round(PIVOT_Y - Math.cos(game.angle) * BARREL_LEN));
            g.setStroke(new BasicStroke(1f));
            g.setColor(MAGENTA);
            g.fillOval(PIVOT_X - 8, PIVOT_Y - 6, 16, 12);
            g.fillRect(PIVOT_X - 8, PIVOT_Y, 16, BASE_TOP - PIVOT_Y);
            g.setColor(CYAN);
            g.fillRect(PIVOT_X - 1, PIVOT_Y - 1, 3, 3);
        }

        boolean rotor = (game.ticks / 3) % 2 == 0;
        for (Heli h : game.helis)
            g.drawImage(h.dir > 0 ? (rotor ? HELI_A : HELI_B) : (rotor ? HELI_A_L : HELI_B_L), (int) h.x, h.y, null);
        for (Jet j : game.jets) g.drawImage(j.dir > 0 ? JET : JET_L, (int) j.x, j.y, null);
        for (Bomb b : game.bombs) g.drawImage(BOMB, (int) b.x - 2, (int) b.y - 2, null);

        for (Corpse c : game.corpses) drawCorpse(g, c);
        for (Trooper t : game.troopers) {
            boolean walking = t.path != null && !t.path.isEmpty() && game.state == State.INVASION
                    && game.walkerIdx < game.walkers.size() && game.walkers.get(game.walkerIdx) == t;
            g.drawImage(walking && (game.ticks / 6) % 2 == 0 ? TROOPER_B : TROOPER_A, (int) Math.round(t.x), (int) Math.round(t.y), null);
            if (t.chute) g.drawImage(CHUTE, (int) t.x - 3, (int) t.y - 8, null);
        }

        g.setColor(WHITE);
        for (Shot s : game.shots) g.fillRect((int) s.x, (int) s.y, 2, 2);
        for (Bit b : game.bits) { g.setColor(b.color); g.fillRect((int) b.x, (int) b.y, 2, 2); }

        // 점수
        g.setColor(CYAN);
        g.drawString("SCORE: " + game.score, 8, 197);
        String hi = "HI-SCORE: " + game.hiScore;
        g.drawString(hi, W - 8 - g.getFontMetrics().stringWidth(hi), 197);

        if (game.state == State.TITLE) {
            center(g, "P A R A T R O O P E R", 60, MAGENTA);
            center(g, "LEFT / RIGHT : ROTATE GUN", 90, WHITE);
            center(g, "UP / SPACE : FIRE", 102, WHITE);
            center(g, "P : PAUSE   S : SOUND   ESC : QUIT", 114, WHITE);
            if ((game.ticks / 30) % 2 == 0) center(g, "PRESS ENTER TO START", 140, CYAN);
        } else if (game.state == State.OVER) {
            center(g, "GAME OVER", 80, MAGENTA);
            if ((game.ticks / 30) % 2 == 0) center(g, "PRESS ENTER TO PLAY AGAIN", 100, WHITE);
        } else if (game.paused) {
            center(g, "PAUSED", 90, WHITE);
        }
    }

    void center(Graphics2D g, String s, int y, Color c) {
        g.setColor(c);
        g.drawString(s, (W - g.getFontMetrics().stringWidth(s)) / 2, y);
    }

    void drawCorpse(Graphics2D g, Corpse c) {
        int a = c.age;
        if (a >= DEAD_TICKS - 40 && (a / 4) % 2 == 0) return;
        boolean r = c.dir > 0;
        BufferedImage img;
        if (a < 6) img = DEAD_SQUASH;                                    // 쿵: 다리가 꺾이며 주저앉음
        else if (a < 12) img = r ? DEAD_TILT : DEAD_TILT_L;              // 옆으로 넘어감
        else if (a >= 70) img = r ? DEAD_LIMP : DEAD_LIMP_L;             // 꼴까닥: 들려 있던 팔이 툭 떨어짐
        else {                                                           // 점점 약해지는 경련
            boolean twitch = (a >= 28 && a < 32) || (a >= 44 && a < 47) || (a >= 58 && a < 60);
            img = twitch ? (r ? DEAD_B : DEAD_B_L) : (r ? DEAD_A : DEAD_A_L);
        }
        int w = img.getWidth();
        int x = (int) Math.round(c.x) + (r ? (w > TR_W ? -1 : 0) : TR_W - w + (w > TR_W ? 1 : 0));
        int y = c.floor - img.getHeight() - (a >= 12 && a < 15 ? 1 : 0);     // 쓰러지며 한 번 튕김
        g.drawImage(img, x, y, null);
    }
}

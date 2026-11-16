package pe.kr.architeer;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static pe.kr.architeer.Constants.*;

/** 문자열로 그린 픽셀 스프라이트 ('c'=청록, 'm'=자홍, 'w'=흰색). 왼쪽을 향한 버전은 좌우 반전해 만든다. */
final class Sprites {
    private Sprites() {}

    static final BufferedImage HELI_A = sprite(
            "....wwwwwwwwwwwwwwwww...",
            "............w...........",
            "c........cccccccc.......",
            "cc.....cccccccmmmcc.....",
            "cccccccccccccmmmmmcc....",
            "cc.....cccccccccccccc...",
            "c.......ccccccccccc.....",
            ".........c.....c........",
            ".......wwwwwwwwwwww.....");
    static final BufferedImage HELI_B = sprite(
            "..........wwwww.........",
            "............w...........",
            "c........cccccccc.......",
            "cc.....cccccccmmmcc.....",
            "cccccccccccccmmmmmcc....",
            "cc.....cccccccccccccc...",
            "c.......ccccccccccc.....",
            ".........c.....c........",
            ".......wwwwwwwwwwww.....");
    static final BufferedImage JET = sprite(
            "cc......................",
            "ccc.....cccc............",
            "ccccccccccccccccccc.....",
            "cccccccccccccccccmmmcc..",
            "cccccccccccccccccccccccc",
            "......ccccccc...........",
            ".....cccccc.............");
    static final BufferedImage TROOPER_A = sprite(
            ".www.",
            ".www.",
            "..c..",
            "ccccc",
            "..c..",
            "..c..",
            ".c.c.",
            ".c.c.");
    static final BufferedImage TROOPER_B = sprite(
            ".www.",
            ".www.",
            "..c..",
            "ccccc",
            "..c..",
            "..c..",
            ".c.c.",
            "c...c");
    static final BufferedImage CHUTE = sprite(
            "...mmmmm...",
            ".mmmmmmmmm.",
            "mmmmmmmmmmm",
            "mmmmmmmmmmm",
            "w....w....w",
            ".w...w...w.",
            "..w..w..w..",
            "...w.w.w...");
    // 추락사: 찌그러짐 → 옆으로 기울어짐 → 나자빠짐(팔다리 경련) → 축 늘어짐. 오른쪽으로 쓰러지는 기준
    static final BufferedImage DEAD_SQUASH = sprite(
            ".www.",
            ".www.",
            "ccccc",
            ".c.c.",
            "c...c");
    static final BufferedImage DEAD_TILT = sprite(
            "....ww",
            "..c.ww",
            "...c..",
            "..c.c.",
            ".c....",
            "c.c...",
            "c..c..");
    static final BufferedImage DEAD_A = sprite(
            "....c...",
            "c...c.ww",
            ".cccccww",
            "c...c.ww");
    static final BufferedImage DEAD_B = sprite(
            "c...c...",
            ".c..c.ww",
            "..ccccww",
            "....c.ww");
    static final BufferedImage DEAD_LIMP = sprite(
            "........",
            "c.....ww",
            ".cccccww",
            "c..cc.ww");
    static final BufferedImage BOMB = sprite(
            ".ww.",
            "wwww",
            "wwww",
            ".ww.");
    static final BufferedImage HELI_A_L = flip(HELI_A), HELI_B_L = flip(HELI_B), JET_L = flip(JET);
    static final BufferedImage DEAD_TILT_L = flip(DEAD_TILT), DEAD_A_L = flip(DEAD_A), DEAD_B_L = flip(DEAD_B),
            DEAD_LIMP_L = flip(DEAD_LIMP);

    static BufferedImage sprite(String... rows) {
        int w = 0;
        for (String r : rows) w = Math.max(w, r.length());
        BufferedImage img = new BufferedImage(w, rows.length, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < rows.length; y++) {
            for (int x = 0; x < rows[y].length(); x++) {
                char c = rows[y].charAt(x);
                Color col = c == 'c' ? CYAN : c == 'm' ? MAGENTA : c == 'w' ? WHITE : null;
                if (col != null) img.setRGB(x, y, col.getRGB());
            }
        }
        return img;
    }

    static BufferedImage flip(BufferedImage src) {
        BufferedImage img = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++)
            for (int x = 0; x < src.getWidth(); x++)
                img.setRGB(src.getWidth() - 1 - x, y, src.getRGB(x, y));
        return img;
    }
}

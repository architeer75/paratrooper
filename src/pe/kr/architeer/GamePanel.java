package pe.kr.architeer;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

import pe.kr.architeer.sound.Sound;

import static pe.kr.architeer.Constants.*;

/** 키 입력을 게임에 전달하고, 타이머마다 60Hz 고정 스텝으로 게임을 진행시켜 화면을 확대해 그린다. */
@SuppressWarnings("serial")
final class GamePanel extends JPanel implements ActionListener {
    final Game game = new Game();
    final Renderer renderer = new Renderer(game);
    final BufferedImage screen = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
    long lastNanos;
    double acc;

    GamePanel() {
        setPreferredSize(new Dimension(W * SCALE, H * SCALE));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) { key(e.getKeyCode(), true); }
            @Override public void keyReleased(KeyEvent e) { key(e.getKeyCode(), false); }
        });
    }

    void key(int code, boolean down) {
        switch (code) {
            case KeyEvent.VK_LEFT: game.keyLeft = down; break;
            case KeyEvent.VK_RIGHT: game.keyRight = down; break;
            case KeyEvent.VK_UP:
            case KeyEvent.VK_SPACE: game.keyFire = down; break;
            case KeyEvent.VK_ENTER:
                if (down && (game.state == State.TITLE || game.state == State.OVER)) game.newGame();
                break;
            case KeyEvent.VK_P: if (down && game.state != State.TITLE) game.paused = !game.paused; break;
            case KeyEvent.VK_S: if (down) Sound.enabled = !Sound.enabled; break;
            case KeyEvent.VK_ESCAPE: if (down) System.exit(0); break;
            default:
        }
    }

    // ───────────────────────── 메인 루프 (60Hz 고정 스텝) ─────────────────────────
    @Override public void actionPerformed(ActionEvent e) {
        long now = System.nanoTime();
        if (lastNanos == 0) lastNanos = now;
        acc += (now - lastNanos) / 1e9;
        lastNanos = now;
        int steps = 0;
        while (acc >= 1 / 60.0 && steps++ < 5) { game.tick(); acc -= 1 / 60.0; }
        if (acc > 0.1) acc = 0;
        repaint();
    }

    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D sg = screen.createGraphics();
        renderer.render(sg);
        sg.dispose();
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        double sc = Math.min(getWidth() / (double) W, getHeight() / (double) H);
        int dw = (int) (W * sc), dh = (int) (H * sc);
        g.drawImage(screen, (getWidth() - dw) / 2, (getHeight() - dh) / 2, dw, dh, null);
    }
}

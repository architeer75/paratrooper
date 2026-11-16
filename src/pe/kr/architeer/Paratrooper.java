package pe.kr.architeer;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import pe.kr.architeer.sound.Sound;

/**
 * Paratrooper (1982, DOS) 스타일 게임 (Java 8+).
 *
 *   컴파일: javac -d out $(find src -name "*.java")
 *   실행  : java -cp out pe.kr.architeer.Paratrooper
 *
 *   조작  : ←/→ 포신 회전, ↑ 또는 Space 발사, Enter 시작/재시작,
 *           P 일시정지, S 사운드 on/off, Esc 종료
 *
 * 내부 해상도는 CGA 320x200 4색(검정/청록/자홍/흰색)이며 창 크기에 맞춰 정수배가 아닌
 * 비율로도 최근접 보간(nearest neighbor)으로 확대한다.
 *
 *   Constants  전역 상수          Sprites   픽셀 스프라이트
 *   Game       상태와 게임 규칙    Renderer  화면 그리기
 *   GamePanel  입력과 메인 루프    State     게임 진행 상태
 *   entity     Heli, Jet, Bomb, Shot, Trooper, Corpse, Bit
 *   sound      Sound(효과음 합성과 믹서), Voice
 */
public final class Paratrooper {
    private Paratrooper() {}

    public static void main(String[] args) {
        Sound.init();
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                JFrame f = new JFrame("Paratrooper");
                GamePanel panel = new GamePanel();
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                f.add(panel);
                f.pack();
                f.setLocationRelativeTo(null);
                f.setVisible(true);
                panel.requestFocusInWindow();
                new Timer(8, panel).start();
            }
        });
    }
}

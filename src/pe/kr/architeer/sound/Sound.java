package pe.kr.architeer.sound;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

/** 효과음: 시작 시 절차적으로 합성해 두고, 전용 스레드의 믹서가 여러 소리를 동시에 섞어 재생한다. */
public final class Sound {
    private Sound() {}

    public static final int FLAK = 0, BLAST_AIR = 1, BLAST_BIG = 2, POP = 3, WHISTLE = 4, THUD = 5, STEP = 6, SPLAT = 7;
    static final float RATE = 44100f;
    static final int FRAMES = 256, MAX_VOICES = 32;
    static final double MASTER = 0.9;
    public static volatile boolean enabled = true;
    static volatile boolean ready;

    static final float[][][] bank = new float[8][][];
    static final ConcurrentLinkedQueue<Voice> pending = new ConcurrentLinkedQueue<Voice>();
    static final Random rnd = new Random();


    /** 효과음을 합성한 뒤 믹서 스레드를 돌린다. 오디오 장치가 없으면 조용히 비활성화. */
    public static void init() {
        Thread t = new Thread(new Runnable() {
            @Override public void run() { mixLoop(); }
        }, "snd-mixer");
        t.setDaemon(true);
        t.start();
    }

    /** pan: -1(왼쪽) ~ 1(오른쪽). 변형 중 하나를 골라 음높이를 살짝 흔들어 재생한다. */
    public static void play(int id, double pan) {
        if (!enabled || !ready) return;
        float[][] v = bank[id];
        Voice s = new Voice();
        s.d = v[rnd.nextInt(v.length)];
        s.step = 0.94 + rnd.nextDouble() * 0.12;
        double a = (Math.max(-1, Math.min(1, pan)) + 1) * Math.PI / 4;      // 등전력 패닝
        s.gl = (float) Math.cos(a);
        s.gr = (float) Math.sin(a);
        pending.add(s);
    }

    static void mixLoop() {
        SourceDataLine line;
        try {
            build();
            AudioFormat fmt = new AudioFormat(RATE, 16, 2, true, false);
            line = AudioSystem.getSourceDataLine(fmt);
            line.open(fmt, FRAMES * 4 * 4);                                  // 약 23ms 버퍼
            line.start();
        } catch (Throwable ex) { enabled = false; return; }
        ready = true;
        List<Voice> voices = new ArrayList<Voice>();
        float[] mix = new float[FRAMES * 2];
        byte[] out = new byte[FRAMES * 4];
        try {
            while (true) {
                for (Voice v; (v = pending.poll()) != null; ) {
                    if (voices.size() >= MAX_VOICES) voices.remove(0);
                    voices.add(v);
                }
                if (!enabled) voices.clear();
                Arrays.fill(mix, 0f);
                for (Iterator<Voice> it = voices.iterator(); it.hasNext(); ) {
                    Voice v = it.next();
                    int n = v.d.length - 1;
                    for (int i = 0; i < FRAMES && v.pos < n; i++, v.pos += v.step) {
                        int k = (int) v.pos;
                        float x = v.d[k] + (v.d[k + 1] - v.d[k]) * (float) (v.pos - k);
                        mix[2 * i] += x * v.gl;
                        mix[2 * i + 1] += x * v.gr;
                    }
                    if (v.pos >= n) it.remove();
                }
                for (int i = 0; i < mix.length; i++) {
                    int s = (int) (Math.tanh(mix[i] * MASTER) * 32000);       // 소프트 클리핑
                    out[2 * i] = (byte) s;
                    out[2 * i + 1] = (byte) (s >> 8);
                }
                line.write(out, 0, out.length);
            }
        } catch (Throwable ex) {
            enabled = false;
        }
    }

    static void build() {
        bank[FLAK] = new float[4][];
        bank[BLAST_AIR] = new float[4][];
        bank[BLAST_BIG] = new float[2][];
        bank[POP] = new float[4][];
        bank[WHISTLE] = new float[2][];
        bank[THUD] = new float[4][];
        bank[STEP] = new float[4][];
        bank[SPLAT] = new float[4][];
        for (int id = 0; id < bank.length; id++)
            for (int v = 0; v < bank[id].length; v++) {
                Random r = new Random(id * 1000 + v);
                float[] s;
                switch (id) {
                    case FLAK:      s = norm(flak(r), 0.55); break;
                    case BLAST_AIR: s = norm(echo(blast(r, 1.8, 1.0, 2), 0.12, 0.3), 0.9); break;
                    case BLAST_BIG: s = norm(echo(blast(r, 3.6, 2.2, 5), 0.17, 0.35), 1.0); break;
                    case POP:       s = norm(blast(r, 0.45, 0.3, 0), 0.45); break;
                    case WHISTLE:   s = norm(whistle(r), 0.08); break;
                    case THUD:      s = norm(thud(r, 0.16, 90, 0.04), 0.3); break;
                    case SPLAT:     s = norm(splat(r), 0.6); break;
                    default:        s = norm(thud(r, 0.07, 150, 0.012), 0.15); break;
                }
                bank[id][v] = s;
            }
    }

    // 대공포 발사: 총구 파열음(고역 노이즈) + 포구 폭압(저음 스윕) + 화염 노이즈 + 잔향 + 노리쇠 금속음
    static float[] flak(Random r) {
        float[] s = buf(0.45);
        double lp1 = 0, lp2 = 0, ph = 0, prev = 0, clank = 0.055 + r.nextDouble() * 0.02;
        for (int i = 0; i < s.length; i++) {
            double t = i / RATE, n = r.nextDouble() * 2 - 1;
            double crack = (n - prev) * Math.exp(-t / 0.004);
            prev = n;
            lp1 += lp(1400) * (n - lp1);
            lp2 += lp(350) * (n - lp2);
            ph += 2 * Math.PI * (40 + 120 * Math.exp(-t / 0.025)) / RATE;
            double tc = t - clank, metal = tc < 0 ? 0 : Math.exp(-tc / 0.012)
                    * (Math.sin(2 * Math.PI * 1870 * tc) + 0.7 * Math.sin(2 * Math.PI * 2710 * tc)
                    + 0.5 * Math.sin(2 * Math.PI * 4130 * tc));
            s[i] = (float) (crack * 0.6 + lp1 * env(t, 0.0005, 0.035) * 2.5 + Math.sin(ph) * env(t, 0.001, 0.07) * 0.9
                    + lp2 * Math.exp(-t / 0.16) * 1.0 + metal * 0.08);
        }
        return s;
    }

    // 폭발: 고역이 빠르게 닫히는 폭음 + 초저음 붐 + 브라운 노이즈 럼블 + 유폭 + 파편 튀는 소리
    static float[] blast(Random r, double len, double size, int bursts) {
        float[] s = buf(len);
        double[] at = new double[bursts], amp = new double[bursts];
        for (int k = 0; k < bursts; k++) { at[k] = 0.08 + r.nextDouble() * len * 0.35; amp[k] = 0.4 + r.nextDouble() * 0.6; }
        double lp1 = 0, lp2 = 0, br = 0, ph = 0, crk = 0, prevCrk = 0;
        for (int i = 0; i < s.length; i++) {
            double t = i / RATE, n = r.nextDouble() * 2 - 1;
            lp1 += lp(150 + 5000 * Math.exp(-t / (0.06 * size))) * (n - lp1);
            lp2 += lp(2500) * (n - lp2);
            br = br * 0.998 + n * 0.03;
            ph += 2 * Math.PI * (28 + 60 * Math.exp(-t / (0.08 * size))) / RATE;
            double sec = 0;
            for (int k = 0; k < bursts; k++) {
                double tk = t - at[k];
                if (tk > 0) sec += lp2 * amp[k] * Math.min(1, tk / 0.002) * Math.exp(-tk / 0.05);
            }
            crk *= 0.8;
            if (r.nextDouble() < 0.004 * Math.exp(-t / (0.5 * size))) crk += (r.nextDouble() * 2 - 1) * 0.6;
            double debris = crk - prevCrk;
            prevCrk = crk;
            s[i] = (float) (lp1 * env(t, 0.002, 0.18 * size) * 3 + Math.sin(ph) * env(t, 0.004, 0.22 * size) * 0.8
                    + br * env(t, 0.03, 0.45 * size) + sec * 1.5 + debris * 0.5);
        }
        return s;
    }

    // 투하된 폭탄의 낙하 휘파람: 고음에서 저음으로 떨어진다
    static float[] whistle(Random r) {
        float[] s = buf(0.9);
        double ph = 0, f0 = 2000 + r.nextDouble() * 400;
        for (int i = 0; i < s.length; i++) {
            double t = i / RATE, u = t / 0.9;
            ph += 2 * Math.PI * f0 * Math.pow(0.35, u) * (1 + 0.004 * Math.sin(2 * Math.PI * 6 * t)) / RATE;
            s[i] = (float) ((Math.sin(ph) + (r.nextDouble() * 2 - 1) * 0.08) * Math.sqrt(Math.sin(Math.PI * u)));
        }
        return s;
    }

    // 착지/발걸음: 짧은 저음 + 둔탁한 노이즈
    static float[] thud(Random r, double len, double hz, double decay) {
        float[] s = buf(len);
        double l = 0, ph = 0;
        for (int i = 0; i < s.length; i++) {
            double t = i / RATE, n = r.nextDouble() * 2 - 1;
            l += lp(700) * (n - l);
            ph += 2 * Math.PI * hz * (0.6 + 0.4 * Math.exp(-t / decay)) / RATE;
            s[i] = (float) (Math.sin(ph) * env(t, 0.001, decay) + l * Math.exp(-t / (decay * 0.5)) * 2);
        }
        return s;
    }

    // 추락사: 묵직하게 바닥을 치는 소리 + 뼈가 부러지는 듯한 짧은 파열음 몇 개
    static float[] splat(Random r) {
        float[] s = thud(r, 0.3, 75, 0.07);
        double[] at = {0.004, 0.012 + r.nextDouble() * 0.01, 0.03 + r.nextDouble() * 0.02};
        double prev = 0;
        for (int i = 0; i < s.length; i++) {
            double t = i / RATE, n = r.nextDouble() * 2 - 1, c = 0;
            for (int k = 0; k < at.length; k++) if (t > at[k]) c += Math.exp(-(t - at[k]) / 0.003) / (k + 1);
            s[i] += (float) ((n - prev) * c * 0.8);
            prev = n;
        }
        return s;
    }

    static float[] buf(double sec) { return new float[(int) (RATE * sec)]; }

    static double env(double t, double attack, double decay) {
        return t < attack ? t / attack : Math.exp(-(t - attack) / decay);
    }

    /** 1극 저역통과 필터 계수 */
    static double lp(double hz) { return 1 - Math.exp(-2 * Math.PI * hz / RATE); }

    /** 피드백 딜레이: 야외 지형에 반사되는 메아리 */
    static float[] echo(float[] s, double delay, double fb) {
        int d = (int) (delay * RATE);
        for (int i = d; i < s.length; i++) s[i] += s[i - d] * (float) fb;
        return s;
    }

    /** 최댓값을 peak로 맞추고 끝을 페이드아웃해 클릭음을 없앤다 */
    static float[] norm(float[] s, double peak) {
        float m = 1e-6f;
        for (float x : s) m = Math.max(m, Math.abs(x));
        int fade = (int) (RATE * 0.03);
        for (int i = 0; i < s.length; i++) {
            double g = peak / m;
            if (i > s.length - fade) g *= (s.length - i) / (double) fade;
            s[i] *= (float) g;
        }
        return s;
    }
}

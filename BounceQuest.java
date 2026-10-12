import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

/**
 * Bounce Quest - a small Java 2D platform game inspired by classic mobile bounce games.
 * Controls: A/D or arrows to move, Space/W/Up to jump, R to restart, Enter to start.
 */
public class BounceQuest extends JPanel implements ActionListener, KeyListener {
    private static final int W = 960, H = 600;
    private final javax.swing.Timer timer = new javax.swing.Timer(16, this);
    private enum State { MENU, PLAYING, DEAD, WON }
    private State state = State.MENU;

    private double px, py, vx, vy;
    private final int ballR = 17;
    private boolean left, right, jumpHeld, grounded;
    private double cameraX = 0;
    private int score = 0;
    private long deadAt;
    private final int worldWidth = 3200;
    private final java.util.List<Rectangle> platforms = new ArrayList<>();
    private final java.util.List<Rectangle> spikes = new ArrayList<>();
    private final java.util.List<Coin> coins = new ArrayList<>();
    private Rectangle goal;

    private static class Coin {
        double x, y; boolean taken = false;
        Coin(double x, double y) { this.x=x; this.y=y; }
    }

    public BounceQuest() {
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        setDoubleBuffered(true);
        addKeyListener(this);
        buildLevel();
        resetPlayer();
        timer.start();
    }

    private void buildLevel() {
        platforms.clear(); spikes.clear(); coins.clear();
        // Main ground is broken into safe islands and gaps.
        platforms.add(new Rectangle(0, 500, 470, 100));
        platforms.add(new Rectangle(540, 465, 260, 135));
        platforms.add(new Rectangle(880, 500, 360, 100));
        platforms.add(new Rectangle(1320, 450, 240, 150));
        platforms.add(new Rectangle(1640, 500, 330, 100));
        platforms.add(new Rectangle(2040, 455, 280, 145));
        platforms.add(new Rectangle(2390, 500, 340, 100));
        platforms.add(new Rectangle(2790, 455, 410, 145));
        // Floating platforms.
        platforms.add(new Rectangle(250, 390, 120, 18));
        platforms.add(new Rectangle(620, 340, 110, 18));
        platforms.add(new Rectangle(970, 385, 130, 18));
        platforms.add(new Rectangle(1450, 330, 110, 18));
        platforms.add(new Rectangle(1770, 390, 120, 18));
        platforms.add(new Rectangle(2130, 330, 120, 18));
        platforms.add(new Rectangle(2500, 380, 110, 18));
        platforms.add(new Rectangle(2880, 330, 120, 18));
        // Spikes on ground islands.
        addSpikeStrip(350, 482, 3); addSpikeStrip(690, 447, 2); addSpikeStrip(1080, 482, 3);
        addSpikeStrip(1430, 432, 2); addSpikeStrip(1810, 482, 3); addSpikeStrip(2150, 437, 2);
        addSpikeStrip(2510, 482, 3); addSpikeStrip(2950, 437, 2);
        // Coin trails.
        double[][] c = {{280,350},{335,350},{650,300},{700,300},{760,420},{1010,345},{1060,345},
                {1455,285},{1510,285},{1800,350},{1855,350},{2150,285},{2205,285},{2530,340},{2585,340},{2910,290},{2965,290}};
        for (double[] a : c) coins.add(new Coin(a[0],a[1]));
        goal = new Rectangle(3125, 375, 42, 80);
    }

    private void addSpikeStrip(int x, int y, int count) {
        for (int i=0;i<count;i++) spikes.add(new Rectangle(x+i*24, y, 22, 18));
    }

    private void resetPlayer() {
        px = 75; py = 455; vx = 0; vy = 0; cameraX = 0; score = 0;
        for (Coin c : coins) c.taken = false;
        state = State.PLAYING;
    }

    @Override public void actionPerformed(ActionEvent e) {
        if (state == State.PLAYING) updateGame();
        if (state == State.DEAD && System.currentTimeMillis()-deadAt > 900) resetPlayer();
        repaint();
    }

    private void updateGame() {
        double accel = 0.62;
        if (left) vx -= accel;
        if (right) vx += accel;
        if (!left && !right) vx *= 0.82;
        vx = Math.max(-5.2, Math.min(5.2, vx));
        vy += 0.48;
        vy = Math.min(vy, 12);

        double oldY = py;
        px += vx;
        if (px < ballR) { px = ballR; vx = 0; }
        if (px > worldWidth-ballR) { px = worldWidth-ballR; vx = 0; }
        py += vy;
        grounded = false;

        Rectangle ball = ballRect();
        for (Rectangle p : platforms) {
            if (ball.intersects(p)) {
                if (vy >= 0 && oldY + ballR <= p.y + 7) {
                    py = p.y-ballR; vy = 0; grounded = true;
                } else if (vy < 0 && oldY-ballR >= p.y+p.height-5) {
                    py = p.y+p.height+ballR; vy = 0;
                } else if (px < p.x) { px = p.x-ballR; vx = 0; }
                else if (px > p.x+p.width) { px = p.x+p.width+ballR; vx = 0; }
            }
        }
        if (py > H+250) { die(); return; }
        ball = ballRect();
        for (Rectangle s : spikes) {
            Rectangle expanded = new Rectangle(s.x+3, s.y+5, s.width-6, s.height-5);
            if (ball.intersects(expanded)) { die(); return; }
        }
        for (Coin c : coins) {
            if (!c.taken && ballRect().intersects(new Rectangle((int)c.x-10,(int)c.y-10,20,20))) {
                c.taken = true; score += 10;
            }
        }
        if (ballRect().intersects(goal)) state = State.WON;
        cameraX = Math.max(0, Math.min(worldWidth-W, px-W*0.38));
    }

    private Rectangle ballRect() { return new Rectangle((int)(px-ballR), (int)(py-ballR), ballR*2, ballR*2); }
    private void die() { state = State.DEAD; deadAt = System.currentTimeMillis(); }

    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D)g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawBackground(g);
        if (state == State.MENU) { drawMenu(g); g.dispose(); return; }
        g.translate(-cameraX, 0);
        drawWorld(g);
        drawPlayer(g);
        g.translate(cameraX, 0);
        drawHUD(g);
        if (state == State.DEAD) drawOverlay(g, "WHOOPS!", "Back to the start in a moment...");
        if (state == State.WON) drawOverlay(g, "LEVEL COMPLETE!", "Coins: " + score + "     Press R to play again");
        g.dispose();
    }

    private void drawBackground(Graphics2D g) {
        GradientPaint sky = new GradientPaint(0,0,new Color(39,166,236),0,H,new Color(190,239,255));
        g.setPaint(sky); g.fillRect(0,0,W,H);
        // Puffy clouds.
        g.setColor(new Color(255,255,255,220));
        for (int i=0;i<7;i++) {
            int x=(int)((i*190-cameraX*0.22)%(W+180)); if(x< -100)x+=W+180;
            int y=55+(i%3)*48;
            g.fillOval(x,y,65,28); g.fillOval(x+20,y-13,42,38); g.fillOval(x+42,y,54,27);
        }
        // Distant hills.
        g.setColor(new Color(75,195,135));
        for (int i=0;i<8;i++) {
            int x=(int)(i*170-cameraX*0.35);
            g.fillOval(x-40,425+(i%2)*20,230,180);
        }
        g.setColor(new Color(42,166,105));
        for (int i=0;i<9;i++) {
            int x=(int)(i*160-cameraX*0.55);
            g.fillOval(x-25,465+(i%3)*9,210,130);
        }
    }

    private void drawWorld(Graphics2D g) {
        // Platforms with grassy tops.
        for (Rectangle p : platforms) {
            g.setColor(new Color(125,82,48)); g.fillRoundRect(p.x,p.y,p.width,p.height,8,8);
            g.setColor(new Color(57,190,73)); g.fillRoundRect(p.x,p.y,p.width,12,8,8);
            g.setColor(new Color(34,145,60)); g.fillRect(p.x,p.y+9,p.width,4);
            g.setColor(new Color(255,255,255,30));
            for(int x=p.x+18;x<p.x+p.width;x+=36) g.drawLine(x,p.y+24,x+10,p.y+39);
        }
        // Spikes.
        for (Rectangle s : spikes) {
            Polygon tri = new Polygon(new int[]{s.x,s.x+s.width/2,s.x+s.width},new int[]{s.y+s.height,s.y,s.y+s.height},3);
            g.setColor(new Color(225,235,240)); g.fillPolygon(tri);
            g.setColor(new Color(115,135,150)); g.drawPolygon(tri);
        }
        // Coins.
        for (Coin c : coins) if(!c.taken) {
            g.setColor(new Color(255,190,20)); g.fillOval((int)c.x-9,(int)c.y-9,18,18);
            g.setColor(new Color(255,239,105)); g.fillOval((int)c.x-5,(int)c.y-6,7,12);
            g.setColor(new Color(190,112,0)); g.drawOval((int)c.x-9,(int)c.y-9,18,18);
        }
        // Finish flag.
        g.setColor(new Color(90,65,45)); g.fillRoundRect(goal.x+5,goal.y,6,90,4,4);
        Polygon flag = new Polygon(new int[]{goal.x+11,goal.x+48,goal.x+11},new int[]{goal.y+3,goal.y+16,goal.y+29},3);
        g.setColor(new Color(255,74,85)); g.fillPolygon(flag);
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif",Font.BOLD,11)); g.drawString("GO!",goal.x+14,goal.y+18);
    }

    private void drawPlayer(Graphics2D g) {
        int x=(int)px, y=(int)py;
        // Shadow.
        g.setColor(new Color(0,0,0,35)); g.fillOval(x-20,y+13,40,9);
        GradientPaint ballPaint = new GradientPaint(x-12,y-15,new Color(255,110,93),x+12,y+15,new Color(210,32,45));
        g.setPaint(ballPaint); g.fillOval(x-ballR,y-ballR,ballR*2,ballR*2);
        g.setColor(new Color(255,190,165)); g.fillOval(x-10,y-12,10,7);
        g.setColor(new Color(120,15,30)); g.drawOval(x-ballR,y-ballR,ballR*2,ballR*2);
        // Eyes follow direction.
        int eyeShift = vx > 0.4 ? 3 : vx < -0.4 ? -2 : 0;
        g.setColor(Color.WHITE); g.fillOval(x-7+eyeShift,y-5,8,10); g.fillOval(x+3+eyeShift,y-5,8,10);
        g.setColor(new Color(35,35,45)); g.fillOval(x-4+eyeShift,y-2,4,5); g.fillOval(x+6+eyeShift,y-2,4,5);
        g.setColor(new Color(120,15,30)); g.drawArc(x-7,y+1,16,10,200,140);
    }

    private void drawHUD(Graphics2D g) {
        g.setColor(new Color(0,0,0,65)); g.fillRoundRect(18,16,220,50,18,18);
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif",Font.BOLD,18));
        g.drawString("★  " + score, 34, 47);
        g.setFont(new Font("SansSerif",Font.PLAIN,12)); g.drawString("A/D or ←/→ move   SPACE jump", 270, 34);
        g.drawString("R restart", 270, 51);
        // Progress bar.
        g.setColor(new Color(0,0,0,60)); g.fillRoundRect(W-205,22,180,13,10,10);
        g.setColor(new Color(255,255,255,180)); g.fillRoundRect(W-203,24,(int)(176*Math.min(1,px/worldWidth)),9,8,8);
    }

    private void drawMenu(Graphics2D g) {
        g.setColor(new Color(0,0,0,90)); g.fillRoundRect(185,105,590,390,34,34);
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif",Font.BOLD,48));
        drawCentered(g,"BOUNCE QUEST",175);
        g.setFont(new Font("SansSerif",Font.PLAIN,19)); drawCentered(g,"A tiny platform adventure",220);
        // Big mascot ball.
        int x=W/2,y=310;
        g.setPaint(new GradientPaint(x-30,y-35,new Color(255,128,104),x+30,y+35,new Color(210,32,45)));
        g.fillOval(x-38,y-38,76,76); g.setColor(Color.WHITE); g.fillOval(x-19,y-15,15,19); g.fillOval(x+5,y-15,15,19);
        g.setColor(new Color(35,35,45)); g.fillOval(x-14,y-8,7,9); g.fillOval(x+10,y-8,7,9);
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif",Font.BOLD,21)); drawCentered(g,"PRESS ENTER TO PLAY",395);
        g.setFont(new Font("SansSerif",Font.PLAIN,16)); drawCentered(g,"Move: A / D or arrow keys     Jump: SPACE / W / ↑",432);
        drawCentered(g,"Collect coins. Dodge spikes. Find the flag!",461);
    }

    private void drawOverlay(Graphics2D g, String title, String subtitle) {
        g.setColor(new Color(7,20,35,175)); g.fillRect(0,0,W,H);
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif",Font.BOLD,42)); drawCentered(g,title, H/2-20);
        g.setFont(new Font("SansSerif",Font.PLAIN,19)); drawCentered(g,subtitle,H/2+25);
    }
    private void drawCentered(Graphics2D g,String s,int y) { FontMetrics fm=g.getFontMetrics(); g.drawString(s,(W-fm.stringWidth(s))/2,y); }

    @Override public void keyPressed(KeyEvent e) {
        int k=e.getKeyCode();
        if (state == State.MENU && (k==KeyEvent.VK_ENTER || k==KeyEvent.VK_SPACE)) { resetPlayer(); return; }
        if (k==KeyEvent.VK_A || k==KeyEvent.VK_LEFT) left=true;
        if (k==KeyEvent.VK_D || k==KeyEvent.VK_RIGHT) right=true;
        if (k==KeyEvent.VK_R) { resetPlayer(); return; }
        if ((k==KeyEvent.VK_SPACE || k==KeyEvent.VK_W || k==KeyEvent.VK_UP) && !jumpHeld) {
            jumpHeld=true;
            if (state==State.PLAYING && grounded) { vy=-10.5; grounded=false; }
            else if (state==State.WON) resetPlayer();
        }
    }
    @Override public void keyReleased(KeyEvent e) {
        int k=e.getKeyCode();
        if (k==KeyEvent.VK_A || k==KeyEvent.VK_LEFT) left=false;
        if (k==KeyEvent.VK_D || k==KeyEvent.VK_RIGHT) right=false;
        if (k==KeyEvent.VK_SPACE || k==KeyEvent.VK_W || k==KeyEvent.VK_UP) jumpHeld=false;
    }
    @Override public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Bounce Quest | Java 2D");
            BounceQuest game = new BounceQuest();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(game); frame.pack(); frame.setResizable(false);
            frame.setLocationRelativeTo(null); frame.setVisible(true); game.requestFocusInWindow();
        });
    }
}

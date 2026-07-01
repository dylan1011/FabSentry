package com.fabsim.console;

import com.fabsim.domain.ToolState;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public final class WaferPanel extends JPanel {

    private volatile ToolState current = ToolState.IDLE;
    private boolean darkMode = true;
    private int litCount = 0;
    private int totalDies = 0;
    private final List<Point> dies = new ArrayList<>();
    private Timer fillTimer;
    private static final int GRID = 12;

    public WaferPanel() {
        setOpaque(false);
        setPreferredSize(new Dimension(260, 260));
        buildDies();
        fillTimer = new Timer(90, e -> {
            if (current == ToolState.PROCESSING && litCount < totalDies) {
                litCount++;
                repaint();
            } else {
                fillTimer.stop();
            }
        });
    }

    public void setDarkMode(boolean dark) {
        this.darkMode = dark;
        repaint();
    }

    public void setCurrent(ToolState state) {
        this.current = state;
        if (state == ToolState.PROCESSING) {
            litCount = 0;
            if (!fillTimer.isRunning()) fillTimer.start();
        } else if (state == ToolState.COMPLETE) {
            litCount = totalDies;
        } else if (state == ToolState.IDLE || state == ToolState.SETUP) {
            litCount = 0;
        }
        repaint();
    }

    private void buildDies() {
        dies.clear();
        double center = (GRID - 1) / 2.0;
        double radius = GRID / 2.0;
        for (int gy = 0; gy < GRID; gy++) {
            for (int gx = 0; gx < GRID; gx++) {
                double dx = gx - center;
                double dy = gy - center;
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist <= radius - 0.6) {
                    dies.add(new Point(gx, gy));
                }
            }
        }
        totalDies = dies.size();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int size = Math.min(getWidth(), getHeight()) - 16;
        int ox = (getWidth() - size) / 2;
        int oy = (getHeight() - size) / 2;

        g2.setColor(darkMode ? new Color(26, 29, 34) : new Color(240, 242, 246));
        g2.fillOval(ox, oy, size, size);
        g2.setColor(darkMode ? new Color(90, 94, 104) : new Color(190, 194, 202));
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawOval(ox, oy, size, size);

        int flat = size / 5;
        g2.drawLine(ox + size / 2 - flat / 2, oy + size - 2, ox + size / 2 + flat / 2, oy + size - 2);

        double cell = size / (double) GRID;
        int gap = 2;
        Color litColor = new Color(47, 168, 79);
        Color emptyColor = darkMode ? new Color(40, 43, 50) : new Color(214, 217, 224);

        for (int i = 0; i < dies.size(); i++) {
            Point d = dies.get(i);
            int x = (int) (ox + d.x * cell) + gap;
            int y = (int) (oy + d.y * cell) + gap;
            int dieSize = (int) cell - gap * 2;
            g2.setColor(i < litCount ? litColor : emptyColor);
            g2.fillRoundRect(x, y, dieSize, dieSize, 2, 2);
        }

        g2.dispose();
    }
}

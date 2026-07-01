package com.fabsim.console;

import com.fabsim.domain.ToolState;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.util.LinkedHashMap;
import java.util.Map;

public final class StateDiagramPanel extends JPanel {

    private volatile ToolState current = ToolState.IDLE;
    private final Map<ToolState, Point> nodes = new LinkedHashMap<>();
    private float glow = 0f;
    private Timer glowTimer;
    private boolean darkMode = true;

    public StateDiagramPanel() {
        setPreferredSize(new Dimension(380, 380));
        setOpaque(false);
        nodes.put(ToolState.IDLE, new Point(190, 60));
        nodes.put(ToolState.SETUP, new Point(310, 150));
        nodes.put(ToolState.PROCESSING, new Point(270, 290));
        nodes.put(ToolState.PAUSED, new Point(110, 290));
        nodes.put(ToolState.COMPLETE, new Point(70, 150));
        nodes.put(ToolState.DOWN, new Point(190, 200));

        glowTimer = new Timer(24, e -> {
            glow -= 0.03f;
            if (glow <= 0f) {
                glow = 0f;
                glowTimer.stop();
            }
            repaint();
        });
    }

    public void setDarkMode(boolean dark) {
        this.darkMode = dark;
        repaint();
    }

    public void setCurrent(ToolState state) {
        this.current = state;
        this.glow = 1f;
        if (!glowTimer.isRunning()) glowTimer.start();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawEdge(g2, ToolState.IDLE, ToolState.SETUP);
        drawEdge(g2, ToolState.SETUP, ToolState.PROCESSING);
        drawEdge(g2, ToolState.PROCESSING, ToolState.PAUSED);
        drawEdge(g2, ToolState.PROCESSING, ToolState.COMPLETE);
        drawEdge(g2, ToolState.COMPLETE, ToolState.IDLE);
        drawEdge(g2, ToolState.PAUSED, ToolState.PROCESSING);
        drawEdge(g2, ToolState.PROCESSING, ToolState.DOWN);
        drawEdge(g2, ToolState.DOWN, ToolState.IDLE);

        for (Map.Entry<ToolState, Point> e : nodes.entrySet()) {
            drawNode(g2, e.getKey(), e.getValue());
        }
    }

    private void drawEdge(Graphics2D g2, ToolState from, ToolState to) {
        Point a = nodes.get(from);
        Point b = nodes.get(to);
        g2.setColor(darkMode ? new Color(78, 82, 92) : new Color(210, 213, 220));
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(new Line2D.Double(a.x, a.y, b.x, b.y));
    }

    private void drawNode(Graphics2D g2, ToolState state, Point p) {
        int r = 36;
        boolean active = state == current;

        if (active && glow > 0f) {
            int gr = (int) (r + 10 * glow);
            Color base = colorFor(state);
            g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), (int) (60 * glow)));
            g2.fillOval(p.x - gr, p.y - gr, gr * 2, gr * 2);
        }

        Color inactiveFill = darkMode ? new Color(26, 29, 34) : new Color(238, 240, 245);
        Color fill = active ? colorFor(state) : inactiveFill;
        g2.setColor(fill);
        g2.fillOval(p.x - r, p.y - r, r * 2, r * 2);

        Color inactiveStroke = darkMode ? new Color(96, 100, 112) : new Color(200, 203, 210);
        g2.setColor(active ? Color.WHITE : inactiveStroke);
        g2.setStroke(new BasicStroke(active ? 2.5f : 1.2f));
        g2.drawOval(p.x - r, p.y - r, r * 2, r * 2);

        Color inactiveText = darkMode ? new Color(170, 174, 184) : new Color(90, 94, 104);
        g2.setColor(active ? Color.WHITE : inactiveText);
        g2.setFont(getFont().deriveFont(active ? Font.BOLD : Font.PLAIN, 10.5f));
        FontMetrics fm = g2.getFontMetrics();
        String label = state.name();
        g2.drawString(label, p.x - fm.stringWidth(label) / 2, p.y + fm.getAscent() / 2 - 1);
    }

    private Color colorFor(ToolState state) {
        switch (state) {
            case PROCESSING: return new Color(47, 168, 79);
            case PAUSED:     return new Color(224, 164, 35);
            case DOWN:       return new Color(214, 69, 69);
            case COMPLETE:   return new Color(47, 127, 224);
            case SETUP:      return new Color(124, 92, 214);
            default:         return new Color(107, 114, 128);
        }
    }
}

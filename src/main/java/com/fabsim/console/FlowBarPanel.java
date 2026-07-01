package com.fabsim.console;

import com.fabsim.domain.ToolState;

import javax.swing.*;
import java.awt.*;

public final class FlowBarPanel extends JPanel {

    private volatile ToolState current = ToolState.IDLE;
    private boolean darkMode = true;

    private static final ToolState[] FLOW = {
        ToolState.IDLE, ToolState.SETUP, ToolState.PROCESSING, ToolState.COMPLETE
    };

    public FlowBarPanel() {
        setOpaque(false);
        setPreferredSize(new Dimension(380, 70));
    }

    public void setDarkMode(boolean dark) {
        this.darkMode = dark;
        repaint();
    }

    public void setCurrent(ToolState state) {
        this.current = state;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int n = FLOW.length;
        int pad = 12;
        int gap = 12;
        int w = getWidth();
        int h = getHeight();
        int boxW = (w - pad * 2 - gap * (n - 1)) / n;
        int boxH = 40;
        int y = (h - boxH) / 2;

        int activeIndex = indexOf(current);

        for (int i = 0; i < n; i++) {
            int x = pad + i * (boxW + gap);

            if (i < n - 1) {
                g2.setColor(darkMode ? new Color(70, 74, 84) : new Color(210, 213, 220));
                g2.setStroke(new BasicStroke(2f));
                int ly = y + boxH / 2;
                g2.drawLine(x + boxW, ly, x + boxW + gap, ly);
            }

            boolean isActive = (i == activeIndex);
            boolean isPast = (activeIndex >= 0 && i < activeIndex);

            Color fill;
            if (isActive) {
                fill = colorFor(FLOW[i]);
            } else if (isPast) {
                Color c = colorFor(FLOW[i]);
                fill = new Color(c.getRed(), c.getGreen(), c.getBlue(), 90);
            } else {
                fill = darkMode ? new Color(30, 33, 39) : new Color(236, 238, 243);
            }
            g2.setColor(fill);
            g2.fillRoundRect(x, y, boxW, boxH, 10, 10);

            Color textColor;
            if (isActive) {
                textColor = Color.WHITE;
            } else if (isPast) {
                textColor = darkMode ? new Color(210, 213, 220) : new Color(80, 84, 94);
            } else {
                textColor = darkMode ? new Color(150, 154, 164) : new Color(140, 144, 154);
            }
            g2.setColor(textColor);
            g2.setFont(getFont().deriveFont(isActive ? Font.BOLD : Font.PLAIN, 11f));
            FontMetrics fm = g2.getFontMetrics();
            String label = FLOW[i].name();
            int tx = x + (boxW - fm.stringWidth(label)) / 2;
            int ty = y + (boxH - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(label, tx, ty);
        }

        g2.dispose();
    }

    private int indexOf(ToolState s) {
        for (int i = 0; i < FLOW.length; i++) {
            if (FLOW[i] == s) return i;
        }
        return -1;
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

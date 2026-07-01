package com.fabsim.console;

import javax.swing.*;
import java.awt.*;

public final class CardPanel extends JPanel {

    private boolean darkMode = true;

    public CardPanel(LayoutManager layout) {
        super(layout);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
    }

    public void setDarkMode(boolean dark) {
        this.darkMode = dark;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color bg = darkMode ? new Color(22, 24, 29) : new Color(248, 249, 251);
        Color border = darkMode ? new Color(42, 45, 52) : new Color(224, 227, 233);
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
        g2.setColor(border);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
        g2.dispose();
        super.paintComponent(g);
    }
}

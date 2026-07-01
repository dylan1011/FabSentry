package com.fabsim.console;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public final class RoundButton extends JButton {

    private static final Color ACCENT = new Color(47, 127, 224);
    private boolean hover = false;
    private boolean darkMode = true;

    public RoundButton(String text) {
        super(text);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setFont(getFont().deriveFont(Font.PLAIN, 13f));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(180, 40));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hover = isEnabled(); repaint(); }
            @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
        });
    }

    public void setDarkMode(boolean dark) {
        this.darkMode = dark;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        int arc = 12;

        Color bg;
        if (!isEnabled()) {
            bg = darkMode ? new Color(24, 26, 31) : new Color(226, 228, 234);
        } else if (hover) {
            bg = ACCENT;
        } else {
            bg = darkMode ? new Color(30, 33, 39) : new Color(238, 240, 245);
        }
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, w, h, arc, arc);

        Color textColor;
        if (!isEnabled()) {
            textColor = darkMode ? new Color(90, 94, 104) : new Color(170, 174, 184);
        } else if (hover) {
            textColor = Color.WHITE;
        } else {
            textColor = darkMode ? new Color(220, 223, 230) : new Color(60, 64, 74);
        }
        g2.setColor(textColor);
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics(getFont());
        int tx = (w - fm.stringWidth(getText())) / 2;
        int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(getText(), tx, ty);
        g2.dispose();
    }
}

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.*;

/**
 * A small set of custom-painted Swing components so the app matches the v0.3 design
 * without any external look-and-feel library: rounded panels, pill buttons, line icons and a step bar.
 */
final class Ui {
    // ---------- colors ----------
    static final Color INK = new Color(30, 38, 56);
    static final Color MUTED = new Color(104, 112, 133);
    static final Color LINE = new Color(215, 220, 228);
    static final Color BG = new Color(246, 247, 249);
    static final Color WHITE = Color.WHITE;
    static final Color BLUE = new Color(59, 91, 204);
    static final Color BLUE_SOFT = new Color(234, 240, 255);
    static final Color BLUE_PALE = new Color(244, 247, 255);
    static final Color GREEN = new Color(31, 157, 116);
    static final Color GREEN_SOFT = new Color(233, 246, 240);
    static final Color GREEN_TEXT = new Color(20, 104, 77);
    static final Color AMBER = new Color(183, 121, 31);
    static final Color AMBER_SOFT = new Color(253, 246, 232);
    static final Color AMBER_TEXT = new Color(138, 90, 18);
    static final Color PURPLE = new Color(106, 79, 196);
    static final Color PURPLE_SOFT = new Color(240, 236, 251);
    static final Color ORANGE = new Color(168, 101, 12);
    static final Color ORANGE_SOFT = new Color(252, 243, 230);
    static final Color RED = new Color(180, 71, 90);
    static final Color ACADEMIC = BLUE, FINANCIAL = GREEN, SOCIAL = ORANGE;

    private Ui() { }

    static Color statusColor(FitStatus s) {
        switch (s) {
            case MATCH: return GREEN;
            case MISMATCH: return RED;
            case NEEDS_VERIFICATION: return AMBER;
            case INFO: return new Color(74, 111, 165);
            default: return new Color(138, 147, 163);
        }
    }

    // ---------- fonts ----------
    static Font sans(int style, float size) { return new Font("Helvetica Neue", style, 1).deriveFont(style, size); }
    static Font serif(float size) { return new Font("Georgia", Font.BOLD, 1).deriveFont(Font.BOLD, size); }

    static JLabel text(String s, float size, int style, Color color) {
        JLabel l = new JLabel(s);
        l.setFont(sans(style, size));
        l.setForeground(color);
        return l;
    }

    static JLabel title(String s, float size) {
        JLabel l = new JLabel(s);
        l.setFont(serif(size));
        l.setForeground(INK);
        return l;
    }

    /** Wrapping text at a fixed width. Text is escaped; use html() for markup. */
    static JLabel wrap(String s, int width, float size, Color color) {
        return html(escape(s), width, size, color);
    }

    /**
     * Swing's HTML renderer draws CSS "px" about a third larger than screen pixels,
     * so widths are scaled down here to fit the layout exactly.
     */
    static int css(int screenPixels) { return Math.round(screenPixels * 0.75f); }

    static JLabel html(String markup, int width, float size, Color color) {
        JLabel l = new JLabel("<html><div style='width:" + css(width) + "px'>" + markup + "</div></html>");
        l.setFont(sans(Font.PLAIN, size));
        l.setForeground(color);
        return l;
    }

    static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    static String hex(Color c) { return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue()); }

    static <T extends JComponent> T left(T c) { c.setAlignmentX(Component.LEFT_ALIGNMENT); return c; }

    static JPanel row(int gap, JComponent... parts) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, gap, 0));
        p.setOpaque(false);
        for (JComponent c : parts) p.add(c);
        return p;
    }

    static JPanel column(int gap) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        return p;
    }

    static void antialias(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    // ---------- rounded panel ----------
    static class Card extends JPanel {
        Color fill, border;
        int arc = 18;
        float borderWidth = 1f;

        Card(LayoutManager layout, Color fill, Color border) {
            super(layout);
            this.fill = fill; this.border = border;
            setOpaque(false);
            setBorder(new EmptyBorder(14, 16, 14, 16));
        }

        void style(Color fill, Color border, float width) { this.fill = fill; this.border = border; this.borderWidth = width; repaint(); }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            antialias(g);
            float o = borderWidth / 2f;
            RoundRectangle2D r = new RoundRectangle2D.Float(o, o, getWidth() - borderWidth, getHeight() - borderWidth, arc, arc);
            if (fill != null) { g.setColor(fill); g.fill(r); }
            if (border != null) { g.setColor(border); g.setStroke(new BasicStroke(borderWidth)); g.draw(r); }
            g.dispose();
            super.paintComponent(g0);
        }
    }

    // ---------- pill (chip) toggle ----------
    static class Pill extends JToggleButton {
        Pill(String text) { this(text, true); }
        Pill(String text, boolean toggles) {
            super(text);
            setFont(sans(Font.PLAIN, 13f));
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(6, 13, 6, 13));
            if (!toggles) setModel(new DefaultButtonModel()); // display-only chip
            addChangeListener(e -> setFont(sans(isSelected() ? Font.BOLD : Font.PLAIN, 13f)));
        }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            antialias(g);
            boolean on = isSelected();
            RoundRectangle2D r = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
            g.setColor(on ? BLUE_SOFT : WHITE); g.fill(r);
            g.setColor(on ? BLUE : new Color(196, 203, 216)); g.setStroke(new BasicStroke(on ? 1.4f : 1f)); g.draw(r);
            g.dispose();
            setForeground(on ? new Color(36, 57, 154) : INK);
            super.paintComponent(g0);
        }
    }

    // ---------- buttons ----------
    static class Button extends JButton {
        final boolean primary;
        Color accent = BLUE;
        Button(String text, boolean primary) {
            super(text);
            this.primary = primary;
            setFont(sans(Font.BOLD, 13.5f));
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(8, 16, 8, 16));
            setForeground(primary ? WHITE : INK);
        }
        Button small() { setFont(sans(Font.BOLD, 12.5f)); setBorder(new EmptyBorder(5, 11, 5, 11)); return this; }
        Button accent(Color c) { accent = c; if (!primary) setForeground(c); return this; }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            antialias(g);
            RoundRectangle2D r = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 14, 14);
            boolean hover = getModel().isRollover();
            if (primary) {
                g.setColor(isEnabled() ? (hover ? accent.darker() : accent) : new Color(170, 180, 205));
                g.fill(r);
            } else {
                g.setColor(hover ? BLUE_PALE : WHITE); g.fill(r);
                g.setColor(accent == BLUE ? new Color(196, 203, 216) : accent); g.draw(r);
            }
            g.dispose();
            super.paintComponent(g0);
        }
    }

    /** A text link such as "Change direction" or "Show more". */
    static JButton link(String text) {
        JButton b = new JButton(text);
        b.setFont(sans(Font.BOLD, 13f));
        b.setForeground(BLUE);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(2, 4, 2, 4));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ---------- line icons drawn with Java2D (24 x 24 grid) ----------
    static class LineIcon implements Icon {
        final String kind; final Color color; final int size;
        LineIcon(String kind, Color color, int size) { this.kind = kind; this.color = color; this.size = size; }
        public int getIconWidth() { return size; }
        public int getIconHeight() { return size; }
        public void paintIcon(Component c, Graphics g0, int x, int y) {
            Graphics2D g = (Graphics2D) g0.create();
            antialias(g);
            g.translate(x, y);
            g.scale(size / 24.0, size / 24.0);
            g.setColor(color);
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            draw(g, kind);
            g.dispose();
        }
        private static void draw(Graphics2D g, String kind) {
            Path2D p = new Path2D.Float();
            switch (kind) {
                case "cap":
                    p.moveTo(3, 10); p.lineTo(12, 5); p.lineTo(21, 10); p.lineTo(12, 15); p.closePath();
                    p.moveTo(7, 12.5); p.lineTo(7, 16.5); p.quadTo(12, 19.5, 17, 16.5); p.lineTo(17, 12.5);
                    break;
                case "pin":
                    p.moveTo(12, 21); p.curveTo(5, 14.5, 5, 11, 5, 9); p.curveTo(5, 5, 8, 2, 12, 2);
                    p.curveTo(16, 2, 19, 5, 19, 9); p.curveTo(19, 11, 19, 14.5, 12, 21);
                    g.draw(new Ellipse2D.Float(9.5f, 6.5f, 5, 5));
                    break;
                case "bank":
                    p.moveTo(3, 9); p.lineTo(12, 4); p.lineTo(21, 9);
                    for (float x : new float[]{5, 9.5f, 14.5f, 19}) { p.moveTo(x, 10); p.lineTo(x, 18); }
                    p.moveTo(3, 20); p.lineTo(21, 20);
                    break;
                case "compass":
                    g.draw(new Ellipse2D.Float(3, 3, 18, 18));
                    p.moveTo(15.5, 8.5); p.lineTo(13.5, 13.5); p.lineTo(8.5, 15.5); p.lineTo(10.5, 10.5); p.closePath();
                    break;
                case "heart": case "heartFull":
                    p.moveTo(12, 20); p.curveTo(5, 15.5, 4, 12, 4, 9.5); p.curveTo(4, 7, 6, 5, 8.3, 5);
                    p.curveTo(10, 5, 11.3, 6, 12, 7.3); p.curveTo(12.7, 6, 14, 5, 15.7, 5);
                    p.curveTo(18, 5, 20, 7, 20, 9.5); p.curveTo(20, 12, 19, 15.5, 12, 20);
                    if (kind.equals("heartFull")) g.fill(p);
                    break;
                case "lock":
                    g.draw(new RoundRectangle2D.Float(5, 11, 14, 9, 3, 3));
                    p.moveTo(8, 11); p.lineTo(8, 8); p.curveTo(8, 4, 16, 4, 16, 8); p.lineTo(16, 11);
                    break;
                case "help":
                    g.draw(new Ellipse2D.Float(3, 3, 18, 18));
                    p.moveTo(9.5, 9.5); p.curveTo(9.5, 6.5, 14.5, 6.5, 14.5, 9.5); p.curveTo(14.5, 11.5, 12, 11.5, 12, 13.5);
                    g.fill(new Ellipse2D.Float(11.3f, 16.3f, 1.4f, 1.4f));
                    break;
                case "search":
                    g.draw(new Ellipse2D.Float(5, 5, 12, 12));
                    p.moveTo(15.5, 15.5); p.lineTo(20, 20);
                    break;
                case "dollar":
                    g.draw(new Ellipse2D.Float(3, 3, 18, 18));
                    p.moveTo(14.5, 9.3); p.curveTo(13.8, 8, 12.8, 7.8, 12, 7.8); p.curveTo(10.4, 7.8, 9.5, 8.6, 9.5, 9.8);
                    p.curveTo(9.5, 12.2, 14.5, 11.4, 14.5, 14); p.curveTo(14.5, 15.3, 13.4, 16.2, 12, 16.2);
                    p.curveTo(10.8, 16.2, 9.9, 15.6, 9.4, 14.7); p.moveTo(12, 6.3); p.lineTo(12, 17.7);
                    break;
                case "people":
                    g.draw(new Ellipse2D.Float(6, 6, 6, 6));
                    g.draw(new Ellipse2D.Float(14, 7.5f, 5, 5));
                    p.moveTo(3.5, 19); p.curveTo(4, 16, 6.5, 14.5, 9, 14.5); p.curveTo(11.5, 14.5, 14, 16, 14.5, 19);
                    p.moveTo(14.5, 15); p.curveTo(17, 14.5, 19.5, 15.5, 20.5, 18.5);
                    break;
                case "book":
                    p.moveTo(4, 5); p.lineTo(10, 5); p.curveTo(11, 5, 12, 6, 12, 7); p.lineTo(12, 19); p.curveTo(12, 18, 11, 17, 10, 17); p.lineTo(4, 17); p.closePath();
                    p.moveTo(20, 5); p.lineTo(14, 5); p.curveTo(13, 5, 12, 6, 12, 7); p.moveTo(12, 19); p.curveTo(12, 18, 13, 17, 14, 17); p.lineTo(20, 17); p.lineTo(20, 5);
                    break;
                case "check":
                    p.moveTo(5, 12.5); p.lineTo(9.5, 17); p.lineTo(19, 7.5);
                    break;
                case "list":
                    for (float y : new float[]{6, 12, 18}) { p.moveTo(9, y); p.lineTo(20, y); p.moveTo(4, y); p.lineTo(5, y + 1); p.lineTo(7, y - 1); }
                    break;
                case "chat":
                    p.moveTo(4, 5); p.lineTo(20, 5); p.lineTo(20, 16); p.lineTo(9, 16); p.lineTo(4, 20); p.closePath();
                    break;
                case "link":
                    p.moveTo(10, 14); p.curveTo(11.5, 15.5, 14, 15.5, 15.7, 14); p.lineTo(18.7, 11); p.curveTo(20.5, 9.2, 20.5, 6.8, 18.7, 5.3);
                    p.curveTo(17, 3.6, 14.6, 3.6, 13, 5.3); p.lineTo(12, 6.3);
                    p.moveTo(14, 10); p.curveTo(12.5, 8.5, 10, 8.5, 8.3, 10); p.lineTo(5.3, 13); p.curveTo(3.5, 14.8, 3.5, 17.2, 5.3, 18.7);
                    p.curveTo(7, 20.4, 9.4, 20.4, 11, 18.7); p.lineTo(12, 17.7);
                    break;
                case "arrow":
                    p.moveTo(5, 12); p.lineTo(19, 12); p.moveTo(13, 6); p.lineTo(19, 12); p.lineTo(13, 18);
                    break;
                case "back":
                    p.moveTo(19, 12); p.lineTo(5, 12); p.moveTo(11, 6); p.lineTo(5, 12); p.lineTo(11, 18);
                    break;
                case "chevron":
                    p.moveTo(9, 5); p.lineTo(16, 12); p.lineTo(9, 19);
                    break;
                case "down":
                    p.moveTo(6, 9); p.lineTo(12, 15); p.lineTo(18, 9);
                    break;
                default:
                    g.draw(new Ellipse2D.Float(4, 4, 16, 16));
            }
            g.draw(p);
        }
    }

    static Icon icon(String kind, Color color, int size) { return new LineIcon(kind, color, size); }

    /** A colored circle with a line icon in the middle. */
    static JComponent bubble(String kind, Color color, Color bg, int size) {
        JComponent c = new JComponent() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                antialias(g);
                g.setColor(bg);
                g.fill(new Ellipse2D.Float(0, 0, size, size));
                int s = size / 2;
                new LineIcon(kind, color, s).paintIcon(this, g, (size - s) / 2, (size - s) / 2);
                g.dispose();
            }
        };
        Dimension d = new Dimension(size, size);
        c.setPreferredSize(d); c.setMinimumSize(d); c.setMaximumSize(d);
        return c;
    }

    /** A small colored dot followed by text, for statuses. */
    static JLabel status(FitStatus s, String text, float size) {
        JLabel l = new JLabel("<html><span style='color:" + hex(statusColor(s)) + "'>&#9679;</span> " + escape(text) + "</html>");
        l.setFont(sans(Font.BOLD, size));
        l.setForeground(INK);
        return l;
    }

    /** A rounded, tinted note (green "why" or amber "check"). */
    static JComponent note(String text, Color bg, Color fg, int width, float size) {
        Card c = new Card(new BorderLayout(), bg, null);
        c.arc = 12;
        c.setBorder(new EmptyBorder(6, 9, 6, 9));
        c.add(wrap(text, width, size, fg));
        return c;
    }

    /** Small grey tag such as "Public" or "Two-year". */
    static JComponent tag(String text) {
        Card c = new Card(new BorderLayout(), new Color(238, 241, 246), null);
        c.arc = 20;
        c.setBorder(new EmptyBorder(2, 9, 2, 9));
        JLabel l = text(text, 11.5f, Font.BOLD, new Color(74, 84, 104));
        c.add(l);
        return c;
    }

    // ---------- step bar ----------
    static class Stepper extends JComponent {
        private final String[] labels;
        private int active;
        Stepper(String... labels) { this.labels = labels; setPreferredSize(new Dimension(labels.length * 142, 54)); }
        void setActive(int i) { active = i; repaint(); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            antialias(g);
            int n = labels.length, w = n > 5 ? 140 : 155, x0 = (getWidth() - w * n) / 2 + w / 2;
            for (int i = 0; i < n - 1; i++) {
                g.setColor(i < active ? new Color(143, 163, 230) : new Color(227, 231, 238));
                g.fillRect(x0 + i * w + 14, 13, w - 28, 2);
            }
            for (int i = 0; i < n; i++) {
                int cx = x0 + i * w;
                g.setColor(i == active ? BLUE : i < active ? new Color(143, 163, 230) : new Color(227, 231, 238));
                g.fill(new Ellipse2D.Float(cx - 13, 1, 26, 26));
                g.setFont(sans(Font.BOLD, 12.5f));
                if (i < active) {
                    new LineIcon("check", WHITE, 16).paintIcon(this, g, cx - 8, 6);
                } else {
                    g.setColor(i == active ? WHITE : MUTED);
                    String s = String.valueOf(i + 1);
                    g.drawString(s, cx - g.getFontMetrics().stringWidth(s) / 2, 19);
                }
                g.setFont(sans(i == active ? Font.BOLD : Font.PLAIN, 12f));
                g.setColor(i == active ? INK : new Color(138, 147, 163));
                g.drawString(labels[i], cx - g.getFontMetrics().stringWidth(labels[i]) / 2, 46);
            }
            g.dispose();
        }
    }

    /** A panel that always matches the width of its scroll pane, so content wraps instead of overflowing. */
    static class WidthPanel extends JPanel implements Scrollable {
        WidthPanel(LayoutManager l) { super(l); setOpaque(false); }
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 18; }
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 200; }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }

    /** A scroll pane without borders and with smooth scrolling. */
    static JScrollPane scroll(JComponent c) {
        JScrollPane s = new JScrollPane(c);
        s.setBorder(null);
        s.setOpaque(false);
        s.getViewport().setOpaque(false);
        s.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        s.getVerticalScrollBar().setUnitIncrement(18);
        s.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE); // repaint fully; avoids smearing with see-through panels
        return s;
    }
}

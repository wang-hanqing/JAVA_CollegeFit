import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

/** Shared colors and small helpers so every screen looks the same as the proposal. */
final class Ui {
    static final Color INK = new Color(30, 38, 56);
    static final Color MUTED = new Color(104, 112, 133);
    static final Color LINE = new Color(215, 220, 228);
    static final Color BG = new Color(246, 247, 249);
    static final Color ACADEMIC = new Color(59, 91, 204);
    static final Color FINANCIAL = new Color(31, 157, 116);
    static final Color SOCIAL = new Color(168, 101, 12);

    private Ui() { }

    static Color statusColor(FitStatus s) {
        switch (s) {
            case MATCH: return new Color(31, 157, 116);
            case MISMATCH: return new Color(180, 71, 90);
            case NEEDS_VERIFICATION: return new Color(183, 121, 31);
            default: return new Color(138, 147, 163);
        }
    }

    static JLabel title(String text, float size) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(Font.SERIF, Font.BOLD, Math.round(size)));
        l.setForeground(INK);
        return l;
    }

    static JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(MUTED);
        return l;
    }

    static Border card() {
        return new CompoundBorder(new LineBorder(LINE, 1, true), new EmptyBorder(14, 16, 14, 16));
    }

    /** A colored dot followed by text, used for statuses. */
    static JLabel dot(FitStatus status, String text) {
        JLabel l = new JLabel("<html><span style='color:" + hex(statusColor(status)) + "'>&#9679;</span> "
            + escape(text) + "</html>");
        return l;
    }

    static String hex(Color c) { return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue()); }

    static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Wraps long text inside a JLabel at a fixed width. */
    static JLabel wrap(String text, int width) {
        return new JLabel("<html><div style='width:" + width + "px'>" + escape(text) + "</div></html>");
    }
}

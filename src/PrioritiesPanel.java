import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Step 3: "What matters most to you?" Up to three priorities, in the order she clicks them. Can be skipped. */
class PrioritiesPanel extends JPanel {
    private final CollegeFitSwingApp app;
    private final List<StudentProfile.Priority> chosen = new ArrayList<>();
    private final Map<StudentProfile.Priority, Ui.Card> cards = new EnumMap<>(StudentProfile.Priority.class);
    private final Map<StudentProfile.Priority, RankBadge> badges = new EnumMap<>(StudentProfile.Priority.class);
    private final JLabel orderText = Ui.text("", 14.5f, Font.PLAIN, Ui.INK);
    private final JLabel topic = Ui.text("", 13f, Font.PLAIN, Ui.MUTED);
    private StudentProfile profile;

    PrioritiesPanel(CollegeFitSwingApp app) {
        super(new BorderLayout());
        this.app = app;
        setOpaque(false);
        JPanel content = Ui.column(0);
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JPanel t = Ui.column(0);
        t.add(Ui.left(Ui.title(Messages.get("pri.title"), 28)));
        t.add(Ui.left(Ui.text(Messages.get("pri.subtitle"), 14.5f, Font.PLAIN, Ui.MUTED)));
        head.add(t, BorderLayout.CENTER);
        head.add(topic, BorderLayout.EAST);
        content.add(Ui.left(head));
        content.add(Box.createVerticalStrut(16));

        JPanel grid = new JPanel(new GridLayout(3, 2, 14, 14));
        grid.setOpaque(false);
        grid.add(card(StudentProfile.Priority.COSTS, "dollar", Ui.GREEN, Ui.GREEN_SOFT));
        grid.add(card(StudentProfile.Priority.GRADUATION, "book", Ui.PURPLE, Ui.PURPLE_SOFT));
        grid.add(card(StudentProfile.Priority.ADMISSION, "check", Ui.BLUE, Ui.BLUE_SOFT));
        grid.add(card(StudentProfile.Priority.STUDY, "cap", Ui.BLUE, Ui.BLUE_SOFT));
        grid.add(card(StudentProfile.Priority.CAMPUS, "people", Ui.ORANGE, Ui.ORANGE_SOFT));
        Ui.Card how = new Ui.Card(new BorderLayout(), new Color(251, 251, 252), Ui.LINE);
        how.setBorder(new EmptyBorder(16, 20, 16, 20));
        how.add(Ui.html("<b>" + Ui.escape(Messages.get("pri.how.title")) + "</b><br><span style='color:#687085'>"
            + Ui.escape(Messages.get("pri.how")) + "</span>", 440, 13f, Ui.INK));
        grid.add(how);
        content.add(Ui.left(grid));
        content.add(Box.createVerticalStrut(14));

        Ui.Card order = new Ui.Card(new BorderLayout(), Ui.BLUE_PALE, new Color(214, 222, 247));
        order.setBorder(new EmptyBorder(12, 20, 12, 20));
        order.add(orderText, BorderLayout.CENTER);
        JButton clear = Ui.link(Messages.get("pri.clear"));
        clear.addActionListener(e -> { chosen.clear(); refresh(); });
        order.add(clear, BorderLayout.EAST);
        content.add(Ui.left(order));

        Ui.Button back = new Ui.Button(Messages.get("nav.back"), false);
        back.addActionListener(e -> app.changeDirection());
        Ui.Button skip = new Ui.Button(Messages.get("pri.skip"), false);
        skip.addActionListener(e -> go(List.of()));
        Ui.Button next = new Ui.Button(Messages.get("pri.next"), true);
        next.setIcon(Ui.icon("arrow", Ui.WHITE, 16));
        next.setHorizontalTextPosition(SwingConstants.LEFT);
        next.addActionListener(e -> go(chosen));
        add(CollegeFitSwingApp.page(content, Ui.text(Messages.get("pri.footnote"), 12.5f, Font.PLAIN, Ui.MUTED), back, skip, next));
    }

    private void go(List<StudentProfile.Priority> list) {
        StudentProfile p = profile.withPrograms(profile.getPrograms());
        p.setPriorities(list);
        app.showOptions(p);
    }

    void load(StudentProfile p) {
        profile = p;
        chosen.clear();
        chosen.addAll(p.getPriorities());
        topic.setText(Messages.get("pri.topic", OptionsPanel.topic(p), OptionsPanel.place(p)));
        refresh();
    }

    private JComponent card(StudentProfile.Priority p, String icon, Color color, Color bg) {
        Ui.Card c = new Ui.Card(new BorderLayout(14, 0), Ui.WHITE, Ui.LINE);
        c.setBorder(new EmptyBorder(14, 18, 14, 18));
        c.add(Ui.bubble(icon, color, bg, 46), BorderLayout.WEST);
        JPanel col = Ui.column(0);
        JPanel titleRow = Ui.row(6, Ui.text(p.toString(), 16f, Font.BOLD, Ui.INK));
        JButton help = Ui.link("?");
        help.setToolTipText(Messages.get("pri.basis." + p.name()));
        help.addActionListener(e -> JOptionPane.showMessageDialog(this, Ui.wrap(Messages.get("pri.basis." + p.name()), 380, 13f, Ui.INK),
            p.toString(), JOptionPane.INFORMATION_MESSAGE));
        titleRow.add(help);
        col.add(Ui.left(titleRow));
        col.add(Ui.left(Ui.wrap(Messages.get("pri.desc." + p.name()), 380, 13.5f, Ui.INK)));
        JPanel mid = new JPanel(new GridBagLayout());
        mid.setOpaque(false);
        mid.add(col);
        c.add(mid, BorderLayout.CENTER);
        RankBadge badge = new RankBadge();
        badges.put(p, badge);
        JPanel right = new JPanel(new GridBagLayout());
        right.setOpaque(false);
        right.add(badge);
        c.add(right, BorderLayout.EAST);
        c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        c.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (chosen.contains(p)) chosen.remove(p);
                else if (chosen.size() < StudentProfile.MAX_PRIORITIES) chosen.add(p);
                else JOptionPane.showMessageDialog(PrioritiesPanel.this, Messages.get("error.priorities"));
                refresh();
            }
        });
        cards.put(p, c);
        return c;
    }

    private void refresh() {
        for (StudentProfile.Priority p : StudentProfile.Priority.values()) {
            int rank = chosen.indexOf(p) + 1;
            cards.get(p).style(rank > 0 ? Ui.BLUE_PALE : Ui.WHITE, rank > 0 ? Ui.BLUE : Ui.LINE, rank > 0 ? 2f : 1f);
            badges.get(p).setRank(rank);
        }
        if (chosen.isEmpty()) {
            orderText.setText(Messages.get("pri.order.none"));
        } else {
            List<String> parts = new ArrayList<>();
            for (int i = 0; i < chosen.size(); i++) parts.add((i + 1) + ". " + chosen.get(i));
            orderText.setText("<html><b>" + Ui.escape(Messages.get("pri.order")) + "</b> &nbsp;" + Ui.escape(String.join("  \u2192  ", parts)) + "</html>");
        }
    }

    /** Empty circle, or a blue circle with #1, #2 or #3. */
    static class RankBadge extends JComponent {
        private int rank;
        RankBadge() { setPreferredSize(new Dimension(36, 36)); }
        void setRank(int r) { rank = r; repaint(); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Ui.antialias(g);
            if (rank == 0) {
                g.setColor(new Color(196, 203, 216));
                g.setStroke(new BasicStroke(2f));
                g.draw(new Ellipse2D.Float(2, 2, 32, 32));
            } else {
                g.setColor(Ui.BLUE);
                g.fill(new Ellipse2D.Float(1, 1, 34, 34));
                g.setColor(Ui.WHITE);
                g.setFont(Ui.sans(Font.BOLD, 14f));
                String s = "#" + rank;
                g.drawString(s, 18 - g.getFontMetrics().stringWidth(s) / 2, 23);
            }
            g.dispose();
        }
    }
}

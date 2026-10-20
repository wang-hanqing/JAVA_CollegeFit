import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Step 3: a small set of starting options, two from each college type.
 * The title says what is being explored, the order rule is shown, and each group can show more.
 */
class OptionsPanel extends JPanel {
    static final int FIRST = 2, MORE = 4;
    private final CollegeFitSwingApp app;
    private final JLabel heading = Ui.title("", 26);
    private final JPanel directionChips = Ui.row(8);
    private final JLabel countText = Ui.text("", 12.5f, Font.PLAIN, Ui.MUTED);
    private final JComboBox<RecommendationEngine.Order> order = new JComboBox<>(RecommendationEngine.Order.values());
    private final JPanel columns = new JPanel(new GridLayout(1, 3, 16, 0));
    private final JLabel footnote = Ui.text("", 12f, Font.PLAIN, Ui.MUTED);
    private final Map<College.Type, Integer> shown = new EnumMap<>(College.Type.class);
    private StudentProfile profile;
    private List<CollegeRecommendation> ranked = new ArrayList<>();

    OptionsPanel(CollegeFitSwingApp app) {
        super(new BorderLayout());
        this.app = app;
        setOpaque(false);
        columns.setOpaque(false);
        order.addActionListener(e -> { if (profile != null) render(); });

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JPanel left = Ui.column(0);
        left.add(Ui.left(heading));
        left.add(Box.createVerticalStrut(8));
        left.add(Ui.left(directionChips));
        head.add(left, BorderLayout.CENTER);
        JPanel right = Ui.column(0);
        countText.setAlignmentX(RIGHT_ALIGNMENT);
        right.add(countText);
        right.add(Box.createVerticalStrut(6));
        JPanel o = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        o.setOpaque(false);
        o.add(Ui.text(Messages.get("options.order"), 12.5f, Font.PLAIN, Ui.MUTED));
        o.add(order);
        o.setAlignmentX(RIGHT_ALIGNMENT);
        right.add(o);
        head.add(right, BorderLayout.EAST);

        JPanel content = Ui.column(0);
        content.add(Ui.left(head));
        content.add(Box.createVerticalStrut(14));
        content.add(Ui.left(columns));

        Ui.Button mine = new Ui.Button(Messages.get("options.toMine"), true);
        mine.setIcon(Ui.icon("arrow", Ui.WHITE, 16));
        mine.setHorizontalTextPosition(SwingConstants.LEFT);
        mine.addActionListener(e -> app.showMyColleges());
        add(CollegeFitSwingApp.page(content, footnote, mine));
    }

    void show(StudentProfile p) {
        profile = p;
        ranked = app.engine.recommend(p, app.colleges);
        for (College.Type t : College.Type.values()) shown.put(t, FIRST);
        order.setSelectedItem(RecommendationEngine.Order.RECENT_DEGREES);
        heading.setText(Messages.get("options.title", topic(p), place(p)));
        directionChips.removeAll();
        directionChips.add(Ui.text(Messages.get("options.direction"), 13f, Font.PLAIN, Ui.MUTED));
        for (String chip : chips(p)) directionChips.add(new Ui.Pill(chip, false));
        JButton change = Ui.link(Messages.get("options.change"));
        change.addActionListener(e -> app.changeDirection());
        directionChips.add(change);
        footnote.setText(Messages.get(p.getIncomeBracket().isKnown() ? "options.footnote.bracket" : "options.footnote.all",
            p.getIncomeBracket().toString()));
        render();
    }

    /** Redraw after saving or removing a college elsewhere. */
    void refreshSaved() { if (profile != null) render(); }

    private void render() {
        RecommendationEngine.Order o = (RecommendationEngine.Order) order.getSelectedItem();
        ranked.sort(RecommendationEngine.comparator(o, profile));
        Map<College.Type, List<CollegeRecommendation>> groups = RecommendationEngine.groups(ranked, profile.getPathway());
        countText.setText("<html><div style='text-align:right'>" + Ui.escape(Messages.get("options.count", Messages.number(ranked.size()), place(profile)))
            + "<br>" + Ui.escape(Messages.get("options.showing")) + "</div></html>");
        columns.removeAll();
        for (College.Type t : College.Type.values()) columns.add(column(t, groups.get(t)));
        columns.revalidate();
        columns.repaint();
    }

    private JComponent column(College.Type t, List<CollegeRecommendation> recs) {
        JPanel col = Ui.column(0);
        col.setAlignmentY(TOP_ALIGNMENT);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(Ui.text(Messages.get("group." + t.name()) + "  (" + recs.size() + ")", 15f, Font.BOLD, Ui.INK), BorderLayout.WEST);
        int n = Math.min(shown.get(t), recs.size());
        if (n < recs.size()) {
            JButton more = Ui.link(Messages.get("options.more"));
            more.addActionListener(e -> { shown.put(t, shown.get(t) + MORE); render(); });
            top.add(more, BorderLayout.EAST);
        }
        col.add(Ui.left(top));
        col.add(Ui.left(Ui.text(Messages.get("group." + t.name() + ".sub"), 12f, Font.PLAIN, Ui.MUTED)));
        col.add(Box.createVerticalStrut(8));
        if (recs.isEmpty()) col.add(Ui.left(Ui.wrap(Messages.get("options.none"), 300, 12.5f, Ui.MUTED)));
        for (int i = 0; i < n; i++) {
            col.add(Ui.left(new OptionCard(app, profile, recs.get(i), this::render)));
            col.add(Box.createVerticalStrut(12));
        }
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(col, BorderLayout.NORTH); // keeps cards at the top of each column
        return wrap;
    }

    // ---------- the "direction" words shown on the page ----------
    static String topic(StudentProfile p) {
        if (!p.hasPrograms()) return Messages.get("options.topic.all");
        List<String> names = new ArrayList<>();
        for (Program pr : p.getPrograms()) names.add(pr.isFamily() ? CipFamilies.name(pr.cipCode) : pr.shortName());
        if (names.size() > 2) return names.get(0) + ", " + names.get(1) + " " + Messages.get("options.topic.more", names.size() - 2);
        return String.join(" " + Messages.get("and") + " ", names);
    }

    static String place(StudentProfile p) {
        if (p.getLocationScope() != StudentProfile.LocationScope.PREFERRED_STATE) return Messages.get("options.anywhere");
        List<String> names = new ArrayList<>();
        for (String s : p.getPreferredStates()) names.add(StateNames.name(s));
        return Messages.get("options.in", String.join(", ", names));
    }

    static List<String> chips(StudentProfile p) {
        List<String> out = new ArrayList<>();
        for (Program pr : p.getPrograms()) out.add(pr.shortName());
        out.add(p.getPathway() == StudentProfile.Pathway.NOT_SURE ? Messages.get("options.anyLevel") : p.getPathway().toString());
        for (String s : p.getPreferredStates()) out.add(StateNames.name(s));
        for (StudentProfile.CampusSetting c : p.getSettings()) out.add(c.toString());
        for (StudentProfile.CollegeSize c : p.getSizes()) out.add(Messages.get("CollegeSize.short." + c.name()));
        return out;
    }
}

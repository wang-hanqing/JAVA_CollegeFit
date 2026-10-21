import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Step 4: Explore. Two colleges from each type to start, ordered by the student's priorities.
 * Keep adds a college to Compare; Not for me sets it aside with a reason and can be undone.
 * A suggestion appears when the same reason is used twice, and changes nothing until she accepts.
 */
class OptionsPanel extends JPanel {
    static final int FIRST = 2, MORE = 4, TIP_AFTER = 2;
    private final CollegeFitSwingApp app;
    private final JLabel heading = Ui.title("", 26);
    private final JPanel directionChips = Ui.row(8);
    private final JLabel orderLine = Ui.text("", 13f, Font.PLAIN, Ui.INK);
    private final JLabel countText = Ui.text("", 12.5f, Font.PLAIN, Ui.MUTED);
    private final JPanel tip = new JPanel(new BorderLayout());
    private final JPanel columns = new JPanel(new GridLayout(1, 3, 16, 0));
    private final JLabel footnote = Ui.text("", 12f, Font.PLAIN, Ui.MUTED);
    private final Map<College.Type, Integer> shown = new EnumMap<>(College.Type.class);
    private final Set<College.Type> openNotForMe = EnumSet.noneOf(College.Type.class);
    private StudentProfile profile;
    private List<CollegeRecommendation> ranked = new ArrayList<>();

    OptionsPanel(CollegeFitSwingApp app) {
        super(new BorderLayout());
        this.app = app;
        setOpaque(false);
        columns.setOpaque(false);
        tip.setOpaque(false);

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JPanel left = Ui.column(0);
        left.add(Ui.left(heading));
        left.add(Box.createVerticalStrut(6));
        left.add(Ui.left(directionChips));
        left.add(Box.createVerticalStrut(6));
        JButton change = Ui.link(Messages.get("explore.changePriorities"));
        change.addActionListener(e -> app.showPriorities(profile));
        left.add(Ui.left(Ui.row(4, orderLine, change)));
        head.add(left, BorderLayout.CENTER);
        countText.setVerticalAlignment(SwingConstants.TOP);
        head.add(countText, BorderLayout.EAST);

        JPanel content = Ui.column(0);
        content.add(Ui.left(head));
        content.add(Box.createVerticalStrut(10));
        content.add(Ui.left(tip));
        content.add(Box.createVerticalStrut(10));
        content.add(Ui.left(columns));

        Ui.Button compare = new Ui.Button(Messages.get("explore.toCompare"), true);
        compare.setIcon(Ui.icon("arrow", Ui.WHITE, 16));
        compare.setHorizontalTextPosition(SwingConstants.LEFT);
        compare.addActionListener(e -> app.showCompare());
        add(CollegeFitSwingApp.page(content, footnote, compare));
    }

    void show(StudentProfile p) {
        profile = p;
        ranked = app.engine.recommend(p, app.colleges);
        for (College.Type t : College.Type.values()) shown.put(t, FIRST);
        openNotForMe.clear();
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

    void refresh() { if (profile != null) render(); }

    private void render() {
        // order line: what decides the order, in her words
        if (profile.hasPriorities()) {
            List<String> parts = new ArrayList<>();
            for (int i = 0; i < profile.getPriorities().size(); i++)
                parts.add("<b>" + (i + 1) + ". " + Ui.escape(Messages.get("order.short." + profile.getPriorities().get(i).name())) + "</b>");
            orderLine.setText("<html>" + Ui.escape(Messages.get("explore.orderedBy")) + " " + String.join(", " + Messages.get("then") + " ", parts) + ".</html>");
        } else {
            orderLine.setText(Messages.get("explore.orderedDefault"));
        }
        countText.setText("<html><div style='text-align:right'>" + Ui.escape(Messages.get("explore.counts", app.shortlist.size(), app.notForMe.size()))
            + "<br>" + Ui.escape(Messages.get("options.count", Messages.number(ranked.size()), place(profile))) + "</div></html>");
        renderTip();
        Map<College.Type, List<CollegeRecommendation>> groups = RecommendationEngine.groups(ranked, profile.getPathway());
        columns.removeAll();
        for (College.Type t : College.Type.values()) columns.add(column(t, groups.get(t)));
        columns.revalidate();
        columns.repaint();
    }

    /** "You marked 2 colleges 'Too far from home'. Want to change your states?" Only acts when she clicks. */
    private void renderTip() {
        tip.removeAll();
        for (Map.Entry<String, Integer> e : app.reasonCounts().entrySet()) {
            String reason = e.getKey();
            if (e.getValue() < TIP_AFTER || reason.equals("OTHER") || app.dismissedTips.contains(reason)) continue;
            Ui.Card card = new Ui.Card(new BorderLayout(12, 0), Ui.BLUE_PALE, new Color(214, 222, 247));
            card.setBorder(new EmptyBorder(8, 16, 8, 16));
            card.add(Ui.html(Ui.escape(Messages.get("tip.text", e.getValue(), Messages.get("reason." + reason)))
                + " " + Ui.escape(Messages.get("tip.ask." + reason)), 700, 13f, Ui.INK), BorderLayout.CENTER);
            Ui.Button notNow = new Ui.Button(Messages.get("tip.notNow"), false).small();
            notNow.addActionListener(x -> { app.dismissedTips.add(reason); render(); });
            Ui.Button yes = new Ui.Button(Messages.get("tip.do." + reason), true).small();
            yes.addActionListener(x -> act(reason));
            card.add(Ui.row(8, notNow, yes), BorderLayout.EAST);
            tip.add(card, BorderLayout.CENTER);
            break; // one suggestion at a time
        }
        tip.revalidate();
    }

    private void act(String reason) {
        app.dismissedTips.add(reason);
        switch (reason) {
            case "COST": app.reorder(first(StudentProfile.Priority.COSTS)); break;
            case "ADMISSION": app.reorder(first(StudentProfile.Priority.ADMISSION)); break;
            default: app.changeDirection(); // states, programs or campus choices
        }
    }

    /** Puts one priority first and keeps her other choices after it (up to three). */
    private List<StudentProfile.Priority> first(StudentProfile.Priority p) {
        List<StudentProfile.Priority> out = new ArrayList<>(List.of(p));
        for (StudentProfile.Priority q : profile.getPriorities()) if (q != p && out.size() < 3) out.add(q);
        return out;
    }

    private JComponent column(College.Type t, List<CollegeRecommendation> recs) {
        List<CollegeRecommendation> active = new ArrayList<>(), setAside = new ArrayList<>();
        for (CollegeRecommendation r : recs) (app.notForMe.containsKey(r.college) ? setAside : active).add(r);
        JPanel col = Ui.column(0);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(Ui.text(Messages.get("group." + t.name()) + "  (" + active.size() + ")", 15f, Font.BOLD, Ui.INK), BorderLayout.WEST);
        int n = Math.min(shown.get(t), active.size());
        if (n < active.size()) {
            JButton more = Ui.link(Messages.get("options.more"));
            more.addActionListener(e -> { shown.put(t, shown.get(t) + MORE); render(); });
            top.add(more, BorderLayout.EAST);
        }
        col.add(Ui.left(top));
        col.add(Ui.left(Ui.text(Messages.get("group." + t.name() + ".sub"), 12f, Font.PLAIN, Ui.MUTED)));
        col.add(Box.createVerticalStrut(8));
        if (active.isEmpty()) col.add(Ui.left(Ui.wrap(Messages.get("options.none"), 300, 12.5f, Ui.MUTED)));
        for (int i = 0; i < n; i++) {
            col.add(Ui.left(new OptionCard(app, profile, active.get(i), this::render)));
            col.add(Box.createVerticalStrut(12));
        }
        if (!setAside.isEmpty()) col.add(Ui.left(notForMeBox(t, setAside)));
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(col, BorderLayout.NORTH);
        return wrap;
    }

    /** Folded list of colleges set aside, each with its reason, Undo and Edit reason. */
    private JComponent notForMeBox(College.Type t, List<CollegeRecommendation> setAside) {
        Ui.Card box = new Ui.Card(new BorderLayout(), new Color(251, 251, 252), Ui.LINE);
        box.setBorder(new EmptyBorder(8, 12, 8, 12));
        JPanel col = Ui.column(0);
        boolean open = openNotForMe.contains(t);
        JButton head = Ui.link(Messages.get("not.section", setAside.size()) + (open ? "  \u25B4" : "  \u25BE"));
        head.setForeground(Ui.INK);
        head.addActionListener(e -> { if (!openNotForMe.remove(t)) openNotForMe.add(t); render(); });
        col.add(Ui.left(head));
        if (open) {
            for (CollegeRecommendation r : setAside) {
                College c = r.college;
                String reason = app.notForMe.get(c);
                String why = reason.isEmpty() ? Messages.get("not.noReason") : Messages.get("reason." + reason);
                col.add(Box.createVerticalStrut(6));
                col.add(Ui.left(Ui.html("<b>" + Ui.escape(c.name) + "</b><br><span style='color:#687085'>"
                    + Ui.escape(Messages.get("not.reasonLine", why)) + "</span>", 300, 12f, Ui.INK)));
                JButton undo = Ui.link(Messages.get("not.undo"));
                undo.addActionListener(e -> { app.undoNotForMe(c); render(); });
                JButton edit = Ui.link(Messages.get("not.edit"));
                edit.addActionListener(e -> {
                    String nr = OptionCard.askReason(this, c.name, reason);
                    if (nr != null) { app.notForMe(c, nr); render(); }
                });
                col.add(Ui.left(Ui.row(4, undo, edit)));
            }
        }
        box.add(col);
        return box;
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

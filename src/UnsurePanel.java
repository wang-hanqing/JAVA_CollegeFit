import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

/**
 * Step 2 for "I'm not sure yet", in three short screens:
 *   1. What do you enjoy doing?  six O*NET career interest types, pick up to 2 (or "I'm not sure yet")
 *   2. Fields linked to what you enjoy  (or, if not sure, eight common fields)
 *   3. Two ways to start college  (two-year / four-year), then Priorities.
 * Every interest, career and count on these screens comes from the data; the eight common fields are a curated design choice.
 */
class UnsurePanel extends JPanel {
    /** Common starting fields for newcomer students (2-digit CIP families). */
    static final String[] FIELDS = {"52", "51", "11", "50", "24", "26", "42", "45"};
    static final String LIBERAL_ARTS = "24";
    static final int MAX_INTERESTS = 2, FIELDS_SHOWN = 4;

    enum Stage { INTEREST, FIELDS, PATHWAY }

    private final CollegeFitSwingApp app;
    private final JComboBox<String> liveIn;
    private final Ui.Pill inState = new Ui.Pill(Messages.get("unsure.inState")), anywhere = new Ui.Pill(Messages.get("unsure.anywhere"));
    private final Map<StudentProfile.Pathway, Ui.Pill> pathway = new LinkedHashMap<>();
    private final PathwayExplainer explainer;
    private final JPanel content = Ui.column(0);
    private final JLabel summary = Ui.text("", 12.5f, Font.PLAIN, Ui.MUTED);
    private final Ui.Button next;

    private Stage stage = Stage.INTEREST;
    private final Set<InterestLinks.Interest> interests = EnumSet.noneOf(InterestLinks.Interest.class);
    private boolean notSure = false;   // "I'm not sure yet" on the interest screen -> common fields
    private boolean showAll = false;
    private String warning = null;
    private final Set<String> chosen = new LinkedHashSet<>();

    UnsurePanel(CollegeFitSwingApp app) {
        super(new BorderLayout());
        this.app = app;
        setOpaque(false);
        String[] items = new String[app.states.size() + 1];
        items[0] = Messages.get("form.notSure");
        for (int i = 0; i < app.states.size(); i++) items[i + 1] = app.states.get(i);
        liveIn = new JComboBox<>(items);
        liveIn.setSelectedItem("NY");
        explainer = new PathwayExplainer(app.colleges);
        ButtonGroup scope = new ButtonGroup();
        scope.add(inState); scope.add(anywhere);
        inState.setSelected(true);
        pathway.put(StudentProfile.Pathway.NOT_SURE, new Ui.Pill(Messages.get("unsure.both")));
        pathway.put(StudentProfile.Pathway.ASSOCIATE, new Ui.Pill(Messages.get("unsure.two")));
        pathway.put(StudentProfile.Pathway.BACHELOR, new Ui.Pill(Messages.get("unsure.four")));
        ButtonGroup pg = new ButtonGroup();
        for (Ui.Pill p : pathway.values()) { pg.add(p); p.addActionListener(e -> updateSummary()); }
        pathway.get(StudentProfile.Pathway.NOT_SURE).setSelected(true);
        liveIn.addActionListener(e -> render());
        inState.addActionListener(e -> render());
        anywhere.addActionListener(e -> render());

        Ui.Button back = new Ui.Button(Messages.get("nav.back"), false);
        back.addActionListener(e -> back());
        next = new Ui.Button("", true);
        next.setIcon(Ui.icon("arrow", Ui.WHITE, 16));
        next.setHorizontalTextPosition(SwingConstants.LEFT);
        next.addActionListener(e -> next());
        add(CollegeFitSwingApp.page(content, summary, back, next));
        render();
    }

    // ---------- navigation inside Step 2 ----------
    private void back() {
        warning = null;
        if (stage == Stage.INTEREST) { app.showStart(); return; }
        stage = stage == Stage.PATHWAY ? Stage.FIELDS : Stage.INTEREST;
        render();
    }

    private void next() {
        warning = null;
        if (stage == Stage.INTEREST) {
            if (interests.isEmpty() || app.interestLinks.isEmpty()) notSure = true;
            showAll = false;
            stage = Stage.FIELDS;
            render();
        } else if (stage == Stage.FIELDS) {
            stage = Stage.PATHWAY;
            render();
        } else {
            try { app.showPriorities(buildProfile()); } catch (IllegalArgumentException ex) { app.error(ex); }
        }
    }

    private boolean commonFields() { return notSure || interests.isEmpty() || app.interestLinks.isEmpty(); }

    private Set<String> scopeStates() {
        if (liveIn.getSelectedIndex() <= 0 || anywhere.isSelected()) return Set.of();
        return Set.of((String) liveIn.getSelectedItem());
    }

    private String where() {
        Set<String> states = scopeStates();
        return states.isEmpty() ? Messages.get("pathway.us") : StateNames.name(states.iterator().next());
    }

    // ---------- screens ----------
    private void render() {
        content.removeAll();
        if (stage == Stage.INTEREST) renderInterests();
        else if (stage == Stage.FIELDS) { if (commonFields()) renderCommonFields(); else renderLinkedFields(); }
        else renderPathway();
        next.setText(Messages.get(stage == Stage.INTEREST ? "unsure.next.fields"
            : stage == Stage.FIELDS ? "unsure.next.continue" : "dir.next"));
        content.revalidate();
        content.repaint();
        updateSummary();
    }

    private void header(String title, String subtitle) {
        content.add(Ui.left(Ui.title(title, 28)));
        content.add(Box.createVerticalStrut(4));
        content.add(Ui.left(Ui.wrap(subtitle, 900, 14.5f, Ui.MUTED)));
        content.add(Box.createVerticalStrut(12));
    }

    private void placeRow() {
        content.add(Ui.left(Ui.row(10, Ui.text(Messages.get("unsure.liveIn"), 14f, Font.BOLD, Ui.INK), liveIn,
            (JComponent) Box.createHorizontalStrut(12),
            Ui.text(Messages.get("unsure.explore"), 14f, Font.BOLD, Ui.INK), inState, anywhere)));
        content.add(Box.createVerticalStrut(16));
    }

    private void footnote(String text) {
        content.add(Box.createVerticalStrut(10));
        content.add(Ui.left(Ui.wrap(text, 900, 12f, Ui.MUTED)));
    }

    /** Screen 1: six interest cards plus "I'm not sure yet". */
    private void renderInterests() {
        header(Messages.get("unsure.interest.title"), Messages.get("unsure.interest.subtitle"));
        placeRow();
        JPanel grid = new JPanel(new GridLayout(2, 3, 14, 14));
        grid.setOpaque(false);
        for (InterestLinks.Interest t : InterestLinks.Interest.values()) grid.add(interestCard(t));
        content.add(Ui.left(grid));
        content.add(Box.createVerticalStrut(14));
        content.add(Ui.left(notSureCard()));
        if (warning != null) {
            content.add(Box.createVerticalStrut(10));
            content.add(Ui.left(Ui.note(warning, Ui.AMBER_SOFT, Ui.AMBER_TEXT, 600, 13f)));
        }
        footnote(Messages.get("unsure.interest.source"));
    }

    private JComponent interestCard(InterestLinks.Interest t) {
        boolean on = interests.contains(t);
        Ui.Card c = new Ui.Card(new BorderLayout(), on ? Ui.BLUE_PALE : Ui.WHITE, on ? Ui.BLUE : Ui.LINE);
        c.borderWidth = on ? 2f : 1f;
        c.setBorder(new EmptyBorder(14, 16, 14, 16));
        JPanel col = Ui.column(0);
        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);
        JLabel name = Ui.wrap(Messages.get("interest." + t), 300, 15.5f, Ui.INK);
        name.setFont(Ui.sans(Font.BOLD, 15.5f));
        top.add(name, BorderLayout.CENTER);
        top.add(new JLabel(on ? Ui.icon("check", Ui.BLUE, 18) : Ui.icon("none", Ui.LINE, 0)), BorderLayout.EAST);
        col.add(Ui.left(top));
        col.add(Box.createVerticalStrut(8));
        col.add(Ui.left(Ui.text(Messages.get("unsure.interest.example"), 11.5f, Font.BOLD, Ui.MUTED)));
        col.add(Box.createVerticalStrut(3));
        col.add(Ui.left(Ui.wrap(Messages.get("interest." + t + ".ex1"), 320, 13f, Ui.INK)));
        col.add(Ui.left(Ui.wrap(Messages.get("interest." + t + ".ex2"), 320, 13f, Ui.INK)));
        c.add(col, BorderLayout.NORTH);
        clickable(c, () -> {
            warning = null;
            if (interests.contains(t)) interests.remove(t);
            else if (interests.size() >= MAX_INTERESTS) warning = Messages.get("unsure.interest.max");
            else { interests.add(t); notSure = false; }
            render();
        });
        return c;
    }

    private JComponent notSureCard() {
        Ui.Card c = new Ui.Card(new BorderLayout(), notSure ? Ui.BLUE_PALE : Ui.BG, notSure ? Ui.BLUE : new Color(169, 177, 194));
        c.borderWidth = notSure ? 2f : 1f;
        c.setBorder(new EmptyBorder(12, 16, 12, 16));
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        row.setOpaque(false);
        row.add(Ui.text(Messages.get("unsure.interest.notSure"), 14.5f, Font.BOLD, Ui.INK));
        row.add(Ui.text(Messages.get("unsure.interest.notSure.text"), 13f, Font.PLAIN, Ui.MUTED));
        c.add(row, BorderLayout.CENTER);
        if (notSure) c.add(new JLabel(Ui.icon("check", Ui.BLUE, 18)), BorderLayout.EAST);
        clickable(c, () -> {
            warning = null;
            notSure = !notSure;
            if (notSure) interests.clear();
            render();
        });
        return c;
    }

    /** Screen 2a: fields linked to the chosen interests. */
    private void renderLinkedFields() {
        List<String> names = new ArrayList<>();
        for (InterestLinks.Interest t : interests) names.add("\u201C" + Messages.get("interest." + t) + "\u201D");
        header(Messages.get("unsure.fields.title"), Messages.get("unsure.fields.subtitle", String.join(" and ", names)));
        placeRow();
        Set<String> states = scopeStates();
        String where = where();
        List<InterestLinks.FieldMatch> fields = app.interestLinks.fields(interests, app.colleges, states);
        if (fields.isEmpty()) {
            content.add(Ui.left(Ui.note(Messages.get("unsure.fields.empty", where), Ui.AMBER_SOFT, Ui.AMBER_TEXT, 700, 13f)));
        }
        int shown = showAll ? fields.size() : Math.min(FIELDS_SHOWN, fields.size());
        for (int i = 0; i < shown; i++) {
            content.add(Ui.left(fieldMatchCard(fields.get(i), where)));
            content.add(Box.createVerticalStrut(12));
        }
        if (fields.size() > FIELDS_SHOWN) {
            JButton more = Ui.link(showAll ? Messages.get("unsure.fields.showFewer") : Messages.get("unsure.fields.showAll", fields.size()));
            more.addActionListener(e -> { showAll = !showAll; render(); });
            content.add(Ui.left(more));
            content.add(Box.createVerticalStrut(12));
        }
        int liberal = RecommendationEngine.fieldCount(app.colleges, states, LIBERAL_ARTS);
        if (liberal > 0) content.add(Ui.left(liberalArtsCard(liberal, where)));
        footnote(Messages.get("unsure.fields.source", where));
    }

    private JComponent fieldMatchCard(InterestLinks.FieldMatch f, String where) {
        boolean on = chosen.contains(f.family);
        Ui.Card c = new Ui.Card(new BorderLayout(0, 12), on ? Ui.BLUE_PALE : Ui.WHITE, on ? Ui.BLUE : Ui.LINE);
        c.borderWidth = on ? 2f : 1f;
        c.setBorder(new EmptyBorder(16, 18, 16, 18));
        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);
        JPanel titleCol = Ui.column(0);
        titleCol.add(Ui.left(Ui.text(CipFamilies.name(f.family), 17f, Font.BOLD, Ui.INK)));
        titleCol.add(Box.createVerticalStrut(3));
        titleCol.add(Ui.left(Ui.text(Messages.get("unsure.fields.meta", Messages.number(f.colleges), where,
            Messages.number(f.linkedCareers)), 12.5f, Font.BOLD, Ui.BLUE)));
        top.add(titleCol, BorderLayout.CENTER);
        top.add(new JLabel(on ? Ui.icon("check", Ui.BLUE, 20) : Ui.icon("none", Ui.LINE, 0)), BorderLayout.EAST);
        c.add(top, BorderLayout.NORTH);

        JPanel body = new JPanel(new GridLayout(1, 2, 28, 0));
        body.setOpaque(false);
        JPanel careers = Ui.column(0);
        careers.add(Ui.left(Ui.text(Messages.get("unsure.fields.careers"), 11.5f, Font.BOLD, Ui.MUTED)));
        for (InterestLinks.CareerLink k : f.careers) {
            careers.add(Box.createVerticalStrut(7));
            JLabel t = Ui.wrap(k.title, 470, 13.5f, Ui.INK);
            t.setFont(Ui.sans(Font.BOLD, 13.5f));
            careers.add(Ui.left(t));
            careers.add(Ui.left(Ui.text(Messages.get("jobZone." + k.jobZone), 12f, Font.PLAIN, new Color(74, 84, 112))));
        }
        JPanel programs = Ui.column(0);
        programs.add(Ui.left(Ui.text(Messages.get("unsure.fields.programs", where), 11.5f, Font.BOLD, Ui.MUTED)));
        for (String[] p : f.programs) {
            programs.add(Box.createVerticalStrut(7));
            JPanel line = new JPanel(new BorderLayout(12, 0));
            line.setOpaque(false);
            line.add(Ui.wrap(p[0], 430, 13.5f, Ui.INK), BorderLayout.CENTER);
            line.add(Ui.text(Messages.get("unsure.fields.programCount", Messages.number(Integer.parseInt(p[1]))), 12.5f, Font.PLAIN, Ui.MUTED), BorderLayout.EAST);
            programs.add(Ui.left(line));
        }
        careers.setAlignmentY(Component.TOP_ALIGNMENT);
        JPanel cw = new JPanel(new BorderLayout()); cw.setOpaque(false); cw.add(careers, BorderLayout.NORTH);
        JPanel pw = new JPanel(new BorderLayout()); pw.setOpaque(false); pw.add(programs, BorderLayout.NORTH);
        body.add(cw);
        body.add(pw);
        c.add(body, BorderLayout.CENTER);
        clickable(c, () -> toggle(f.family));
        return c;
    }

    private JComponent liberalArtsCard(int count, String where) {
        boolean on = chosen.contains(LIBERAL_ARTS);
        Ui.Card c = new Ui.Card(new BorderLayout(8, 0), on ? Ui.BLUE_PALE : Ui.BG, on ? Ui.BLUE : new Color(169, 177, 194));
        c.borderWidth = on ? 2f : 1f;
        c.setBorder(new EmptyBorder(14, 18, 14, 18));
        JPanel col = Ui.column(0);
        col.add(Ui.left(Ui.text(Messages.get("unsure.fields.liberal", CipFamilies.name(LIBERAL_ARTS)), 15.5f, Font.BOLD, Ui.INK)));
        col.add(Box.createVerticalStrut(3));
        col.add(Ui.left(Ui.text(Messages.get("unsure.fields.liberal.text", Messages.number(count), where), 12.5f, Font.PLAIN, Ui.MUTED)));
        c.add(col, BorderLayout.CENTER);
        c.add(new JLabel(on ? Ui.icon("check", Ui.BLUE, 20) : Ui.icon("none", Ui.LINE, 0)), BorderLayout.EAST);
        clickable(c, () -> toggle(LIBERAL_ARTS));
        return c;
    }

    /** Screen 2b: eight common fields, for students who are not sure what they enjoy. */
    private void renderCommonFields() {
        header(Messages.get("unsure.common.title"), Messages.get("unsure.common.subtitle"));
        placeRow();
        Set<String> states = scopeStates();
        String where = where();
        JPanel grid = new JPanel(new GridLayout(2, 4, 12, 12));
        grid.setOpaque(false);
        for (String f : FIELDS) grid.add(fieldCard(f, RecommendationEngine.fieldCount(app.colleges, states, f), where));
        content.add(Ui.left(grid));
        footnote(Messages.get("unsure.countNote", where));
    }

    private JComponent fieldCard(String family, int count, String where) {
        boolean on = chosen.contains(family);
        Ui.Card c = new Ui.Card(new BorderLayout(), on ? Ui.BLUE_PALE : Ui.WHITE, on ? Ui.BLUE : Ui.LINE);
        c.borderWidth = on ? 2f : 1f;
        c.setBorder(new EmptyBorder(12, 14, 12, 14));
        JPanel col = Ui.column(0);
        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);
        JLabel name = Ui.wrap(CipFamilies.name(family), 150, 14f, Ui.INK);
        name.setFont(Ui.sans(Font.BOLD, 14f));
        top.add(name, BorderLayout.CENTER);
        top.add(new JLabel(on ? Ui.icon("check", Ui.BLUE, 18) : Ui.icon("none", Ui.LINE, 0)), BorderLayout.EAST);
        col.add(Ui.left(top));
        col.add(Box.createVerticalStrut(4));
        col.add(Ui.left(Ui.wrap(Messages.get("field." + family), 190, 12f, Ui.MUTED)));
        col.add(Box.createVerticalStrut(6));
        col.add(Ui.left(Ui.text(Messages.get("unsure.fieldCount", Messages.number(count), where), 12f, Font.BOLD, Ui.BLUE)));
        c.add(col);
        clickable(c, () -> toggle(family));
        return c;
    }

    /** Screen 3: two-year and four-year colleges. */
    private void renderPathway() {
        header(Messages.get("unsure.pathway.title"), Messages.get("unsure.pathway.subtitle"));
        explainer.update(scopeStates());
        JPanel pathRow = new JPanel(new BorderLayout(14, 0));
        pathRow.setOpaque(false);
        pathRow.add(explainer, BorderLayout.CENTER);
        JPanel pills = Ui.column(0);
        for (Ui.Pill p : pathway.values()) { pills.add(p); pills.add(Box.createVerticalStrut(6)); }
        pathRow.add(pills, BorderLayout.EAST);
        content.add(Ui.left(pathRow));
    }

    // ---------- helpers ----------
    private void toggle(String family) {
        if (!chosen.remove(family)) chosen.add(family);
        render();
    }

    private static void clickable(JComponent c, Runnable action) {
        c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        c.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { action.run(); }
        });
    }

    private void updateSummary() {
        if (stage == Stage.INTEREST) {
            List<String> names = new ArrayList<>();
            for (InterestLinks.Interest t : interests) names.add(Messages.get("interest." + t));
            if (notSure) names.add(Messages.get("unsure.interest.notSure"));
            summary.setText(Messages.get("unsure.interest.summary", names.isEmpty() ? Messages.get("unsure.interest.none") : String.join("; ", names)));
            return;
        }
        List<String> names = new ArrayList<>();
        for (String f : chosen) names.add(CipFamilies.name(f));
        String fields = names.isEmpty() ? Messages.get("unsure.noFields") : String.join(", ", names);
        String types = "";
        for (Map.Entry<StudentProfile.Pathway, Ui.Pill> e : pathway.entrySet()) if (e.getValue().isSelected()) types = e.getValue().getText();
        summary.setText(Messages.get("unsure.summary", fields, types));
    }

    /** Coming back to Step 2 starts at the interest screen; earlier choices are kept. */
    void load(StudentProfile p) {
        if (p.hasHomeState()) liveIn.setSelectedItem(p.getHomeState());
        stage = Stage.INTEREST;
        warning = null;
        render();
    }

    void reset() {
        chosen.clear();
        interests.clear();
        notSure = false;
        showAll = false;
        warning = null;
        stage = Stage.INTEREST;
        liveIn.setSelectedItem("NY");
        inState.setSelected(true);
        pathway.get(StudentProfile.Pathway.NOT_SURE).setSelected(true);
        render();
    }

    StudentProfile buildProfile() {
        StudentProfile s = new StudentProfile();
        List<Program> fields = new ArrayList<>();
        for (String f : chosen) fields.add(Program.family(f, 0));
        s.setPrograms(fields);
        for (Map.Entry<StudentProfile.Pathway, Ui.Pill> e : pathway.entrySet()) if (e.getValue().isSelected()) s.setPathway(e.getKey());
        if (liveIn.getSelectedIndex() > 0) s.setHomeState((String) liveIn.getSelectedItem());
        Set<String> states = scopeStates();
        if (states.isEmpty()) s.setLocationScope(StudentProfile.LocationScope.ANY_STATE);
        else s.setPreferredStates(states);
        return s;
    }
}

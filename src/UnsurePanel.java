import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

/**
 * Step 2 for "I'm not sure yet": learn the two ways to start college, then pick fields that sound interesting.
 * Field counts come from the data; the list of eight common fields is a curated design choice.
 */
class UnsurePanel extends JPanel {
    /** Common starting fields for newcomer students (2-digit CIP families). */
    static final String[] FIELDS = {"52", "51", "11", "50", "24", "26", "42", "45"};

    private final CollegeFitSwingApp app;
    private final JComboBox<String> liveIn;
    private final Ui.Pill inState = new Ui.Pill(Messages.get("unsure.inState")), anywhere = new Ui.Pill(Messages.get("unsure.anywhere"));
    private final Map<StudentProfile.Pathway, Ui.Pill> pathway = new LinkedHashMap<>();
    private final PathwayExplainer explainer;
    private final JPanel fieldGrid = new JPanel(new GridLayout(2, 4, 12, 12));
    private final Set<String> chosen = new LinkedHashSet<>();
    private final JLabel summary = Ui.text("", 12.5f, Font.PLAIN, Ui.MUTED);
    private final JLabel countNote = Ui.text("", 12f, Font.PLAIN, Ui.MUTED);

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
        liveIn.addActionListener(e -> refreshCounts());
        inState.addActionListener(e -> refreshCounts());
        anywhere.addActionListener(e -> refreshCounts());
        fieldGrid.setOpaque(false);

        JPanel content = Ui.column(0);
        content.add(Ui.left(Ui.title(Messages.get("unsure.title"), 28)));
        content.add(Box.createVerticalStrut(4));
        content.add(Ui.left(Ui.text(Messages.get("unsure.subtitle"), 14.5f, Font.PLAIN, Ui.MUTED)));
        content.add(Box.createVerticalStrut(12));
        content.add(Ui.left(Ui.row(10, Ui.text(Messages.get("unsure.liveIn"), 14f, Font.BOLD, Ui.INK), liveIn,
            (JComponent) Box.createHorizontalStrut(12),
            Ui.text(Messages.get("unsure.explore"), 14f, Font.BOLD, Ui.INK), inState, anywhere)));
        content.add(section("unsure.step1"));
        JPanel pathRow = new JPanel(new BorderLayout(14, 0));
        pathRow.setOpaque(false);
        pathRow.add(explainer, BorderLayout.CENTER);
        JPanel pills = Ui.column(0);
        for (Ui.Pill p : pathway.values()) { pills.add(p); pills.add(Box.createVerticalStrut(6)); }
        pathRow.add(pills, BorderLayout.EAST);
        content.add(Ui.left(pathRow));
        content.add(section("unsure.step2"));
        content.add(Ui.left(fieldGrid));
        content.add(Box.createVerticalStrut(8));
        content.add(Ui.left(countNote));

        Ui.Button back = new Ui.Button(Messages.get("nav.back"), false);
        back.addActionListener(e -> app.showStart());
        Ui.Button next = new Ui.Button(Messages.get("dir.next"), true);
        next.setIcon(Ui.icon("arrow", Ui.WHITE, 16));
        next.setHorizontalTextPosition(SwingConstants.LEFT);
        next.addActionListener(e -> {
            try { app.showOptions(buildProfile()); } catch (IllegalArgumentException ex) { app.error(ex); }
        });
        add(CollegeFitSwingApp.page(content, summary, back, next));
        refreshCounts();
    }

    private static JComponent section(String key) {
        JLabel l = Ui.text(Messages.get(key), 15f, Font.BOLD, Ui.INK);
        l.setBorder(new EmptyBorder(16, 0, 8, 0));
        return Ui.left(l);
    }

    private Set<String> scopeStates() {
        if (liveIn.getSelectedIndex() <= 0 || anywhere.isSelected()) return Set.of();
        return Set.of((String) liveIn.getSelectedItem());
    }

    private void refreshCounts() {
        Set<String> states = scopeStates();
        explainer.update(states);
        String where = states.isEmpty() ? Messages.get("pathway.us") : StateNames.name(states.iterator().next());
        fieldGrid.removeAll();
        for (String f : FIELDS) fieldGrid.add(fieldCard(f, RecommendationEngine.fieldCount(app.colleges, states, f), where));
        countNote.setText(Messages.get("unsure.countNote", where));
        fieldGrid.revalidate();
        fieldGrid.repaint();
        updateSummary();
    }

    private JComponent fieldCard(String family, int count, String where) {
        boolean on = chosen.contains(family);
        Ui.Card c = new Ui.Card(new BorderLayout(), on ? Ui.BLUE_PALE : Ui.WHITE, on ? Ui.BLUE : Ui.LINE);
        c.borderWidth = on ? 2f : 1f;
        c.setBorder(new EmptyBorder(12, 14, 12, 14));
        JPanel col = Ui.column(0);
        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);
        top.add(Ui.wrap(CipFamilies.name(family), 150, 14f, Ui.INK), BorderLayout.CENTER);
        ((JLabel) top.getComponent(0)).setFont(Ui.sans(Font.BOLD, 14f));
        top.add(new JLabel(on ? Ui.icon("check", Ui.BLUE, 18) : Ui.icon("none", Ui.LINE, 0)), BorderLayout.EAST);
        col.add(Ui.left(top));
        col.add(Box.createVerticalStrut(4));
        col.add(Ui.left(Ui.wrap(Messages.get("field." + family), 190, 12f, Ui.MUTED)));
        col.add(Box.createVerticalStrut(6));
        col.add(Ui.left(Ui.text(Messages.get("unsure.fieldCount", Messages.number(count), where), 12f, Font.BOLD, Ui.BLUE)));
        c.add(col);
        c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        c.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (!chosen.remove(family)) chosen.add(family);
                refreshCounts();
            }
        });
        return c;
    }

    private void updateSummary() {
        List<String> names = new ArrayList<>();
        for (String f : chosen) names.add(CipFamilies.name(f));
        String fields = names.isEmpty() ? Messages.get("unsure.noFields") : String.join(", ", names);
        String types = "";
        for (Map.Entry<StudentProfile.Pathway, Ui.Pill> e : pathway.entrySet()) if (e.getValue().isSelected()) types = e.getValue().getText();
        summary.setText(Messages.get("unsure.summary", fields, types));
    }

    void load(StudentProfile p) {
        if (p.hasHomeState()) liveIn.setSelectedItem(p.getHomeState());
        refreshCounts();
    }

    void reset() {
        chosen.clear();
        liveIn.setSelectedItem("NY");
        inState.setSelected(true);
        pathway.get(StudentProfile.Pathway.NOT_SURE).setSelected(true);
        refreshCounts();
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

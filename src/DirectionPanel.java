import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

/**
 * Step 2 for students who know something: a subject, a place or a degree pathway.
 * Only study goals and campus preferences are shown; scores and costs are folded away as optional.
 */
class DirectionPanel extends JPanel {
    private final CollegeFitSwingApp app;

    // Study goals
    private final JTextField search = new JTextField(22);
    private final DefaultListModel<Program> matches = new DefaultListModel<>();
    private final JList<Program> matchList = new JList<>(matches);
    private final List<Program> chosenPrograms = new ArrayList<>();
    private final JPanel programChips = Ui.row(6);
    private final Map<StudentProfile.Pathway, Ui.Pill> pathway = new EnumMap<>(StudentProfile.Pathway.class);

    // Where and what kind of campus
    private final Map<StudentProfile.LocationScope, Ui.Pill> where = new EnumMap<>(StudentProfile.LocationScope.class);
    private final Set<String> chosenStates = new TreeSet<>();
    private final JPanel stateChips = Ui.row(6);
    private final JComboBox<String> stateToAdd;
    private final Map<StudentProfile.CampusSetting, Ui.Pill> settings = new EnumMap<>(StudentProfile.CampusSetting.class);
    private final Map<StudentProfile.CollegeSize, Ui.Pill> sizes = new EnumMap<>(StudentProfile.CollegeSize.class);

    private final ExtraInfoPanel extras;
    private final JPanel layoutArea = Ui.column(0);
    private final JLabel heading = Ui.title("", 28);
    private final JLabel subheading = Ui.text("", 14.5f, Font.PLAIN, Ui.MUTED);
    private final PathwayExplainer explainer;
    private CollegeFitSwingApp.Start start = CollegeFitSwingApp.Start.SUBJECT;
    private Map<String, Integer> counts = new HashMap<>();
    private Set<String> countStates = Set.of();

    DirectionPanel(CollegeFitSwingApp app) {
        super(new BorderLayout());
        this.app = app;
        setOpaque(false);
        stateToAdd = new JComboBox<>(app.states.toArray(new String[0]));
        stateToAdd.setSelectedItem("NY");
        extras = new ExtraInfoPanel(app.states);
        explainer = new PathwayExplainer(app.colleges);

        for (StudentProfile.Pathway p : StudentProfile.Pathway.values()) pathway.put(p, new Ui.Pill(p.toString()));
        group(pathway.values());
        for (StudentProfile.LocationScope s : StudentProfile.LocationScope.values()) where.put(s, new Ui.Pill(s.toString()));
        group(where.values());
        for (StudentProfile.CampusSetting c : StudentProfile.CampusSetting.values())
            if (c != StudentProfile.CampusSetting.NOT_SURE) settings.put(c, new Ui.Pill(c.toString()));
        for (StudentProfile.CollegeSize c : StudentProfile.CollegeSize.values()) {
            Ui.Pill pill = new Ui.Pill(Messages.get("card.size." + c.name()));
            pill.setToolTipText(c.toString()); // e.g. "Small (under 2,000)"
            sizes.put(c, pill);
        }
        for (Ui.Pill p : where.values()) p.addActionListener(e -> updateStates());

        matchList.setVisibleRowCount(4);
        // Counts follow the chosen states; with no states chosen they are national
        matchList.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean sel, boolean focus) {
                Program p = (Program) value;
                Integer n = counts.get(p.cipCode);
                String where = countStates.isEmpty() ? Messages.get("dir.count.us", Messages.number(n == null ? 0 : n))
                    : n == null ? Messages.get("dir.count.none", countPlace()) : Messages.get("dir.count.state", Messages.number(n), countPlace());
                return super.getListCellRendererComponent(list, p.fullName + "   (" + where + ")", index, sel, focus);
            }
        });
        matchList.setFont(Ui.sans(Font.PLAIN, 13.5f));
        matchList.setFixedCellHeight(28);
        matchList.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                Program p = matchList.getSelectedValue();
                if (p != null) { addProgram(p); search.setText(""); }
            }
        });
        search.setFont(Ui.sans(Font.PLAIN, 14f));
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
        });
        filter();

        JPanel content = Ui.column(0);
        content.add(Ui.left(heading));
        content.add(Box.createVerticalStrut(4));
        content.add(Ui.left(subheading));
        content.add(Box.createVerticalStrut(14));
        content.add(Ui.left(layoutArea));

        Ui.Button back = new Ui.Button(Messages.get("nav.back"), false);
        back.addActionListener(e -> app.showStart());
        Ui.Button next = new Ui.Button(Messages.get("dir.next"), true);
        next.setIcon(Ui.icon("arrow", Ui.WHITE, 16));
        next.setHorizontalTextPosition(SwingConstants.LEFT);
        next.addActionListener(e -> {
            try { app.showPriorities(buildProfile()); } catch (IllegalArgumentException ex) { app.error(ex); }
        });
        add(CollegeFitSwingApp.page(content, CollegeFitSwingApp.privacyNote("dir.privacy"), back, next));
        reset();
    }

    private static void group(Collection<Ui.Pill> pills) {
        ButtonGroup g = new ButtonGroup();
        for (Ui.Pill p : pills) g.add(p);
    }

    /** Arranges the page for the chosen starting point and fills in earlier answers. */
    void load(CollegeFitSwingApp.Start s, StudentProfile profile) {
        start = s;
        heading.setText(Messages.get("dir.title." + s.name()));
        subheading.setText(Messages.get("dir.subtitle"));
        chosenPrograms.clear();
        chosenPrograms.addAll(profile.getPrograms());
        pathway.get(profile.getPathway()).setSelected(true);
        where.get(profile.getLocationScope()).setSelected(true);
        chosenStates.clear();
        chosenStates.addAll(profile.getPreferredStates());
        for (Map.Entry<StudentProfile.CampusSetting, Ui.Pill> e : settings.entrySet()) e.getValue().setSelected(profile.getSettings().contains(e.getKey()));
        for (Map.Entry<StudentProfile.CollegeSize, Ui.Pill> e : sizes.entrySet()) e.getValue().setSelected(profile.getSizes().contains(e.getKey()));
        extras.load(profile);
        redrawPrograms();
        updateStates();

        layoutArea.removeAll();
        JPanel two = new JPanel(new GridLayout(1, 2, 18, 0));
        two.setOpaque(false);
        if (s == CollegeFitSwingApp.Start.PLACE) { two.add(placeCard()); two.add(goalsCard()); }
        else { two.add(goalsCard()); two.add(placeCard()); }
        if (s == CollegeFitSwingApp.Start.PATHWAY) {
            explainer.update(Set.of());
            layoutArea.add(Ui.left(explainer));
            layoutArea.add(Box.createVerticalStrut(14));
        }
        layoutArea.add(Ui.left(two));
        layoutArea.add(Box.createVerticalStrut(14));
        JPanel bottom = new JPanel(new GridLayout(1, 2, 18, 0));
        bottom.setOpaque(false);
        bottom.add(extrasCard());
        bottom.add(howCard());
        layoutArea.add(Ui.left(bottom));
        layoutArea.revalidate();
        layoutArea.repaint();
    }

    void reset() {
        chosenPrograms.clear();
        chosenStates.clear();
        StudentProfile empty = new StudentProfile();
        load(start, empty);
    }

    private JComponent goalsCard() {
        Ui.Card c = new Ui.Card(new BorderLayout(), Ui.WHITE, Ui.LINE);
        c.setBorder(new EmptyBorder(16, 20, 16, 20));
        JPanel col = Ui.column(0);
        col.add(Ui.left(cardTitle("cap", Ui.BLUE, Ui.BLUE_SOFT, Messages.get("dir.goals"))));
        col.add(label("dir.programs", "dir.programs.hint"));
        JPanel searchRow = Ui.row(8, new JLabel(Ui.icon("search", Ui.MUTED, 16)), search);
        col.add(Ui.left(searchRow));
        JScrollPane list = new JScrollPane(matchList);
        list.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        list.setPreferredSize(new Dimension(400, 116));
        list.setMaximumSize(new Dimension(460, 116));
        col.add(Box.createVerticalStrut(6));
        col.add(Ui.left(list));
        col.add(Box.createVerticalStrut(6));
        col.add(Ui.left(programChips));
        col.add(label("dir.degree", null));
        col.add(Ui.left(Ui.row(6, pathway.values().toArray(new JComponent[0]))));
        col.add(Box.createVerticalStrut(6));
        col.add(Ui.left(Ui.wrap(Messages.get("dir.degree.hint"), 400, 12.5f, Ui.MUTED)));
        c.add(col);
        return c;
    }

    private JComponent placeCard() {
        Ui.Card c = new Ui.Card(new BorderLayout(), Ui.WHITE, Ui.LINE);
        c.setBorder(new EmptyBorder(16, 20, 16, 20));
        JPanel col = Ui.column(0);
        col.add(Ui.left(cardTitle("pin", Ui.GREEN, Ui.GREEN_SOFT, Messages.get("dir.place"))));
        col.add(label("dir.where", null));
        col.add(Ui.left(Ui.row(6, where.values().toArray(new JComponent[0]))));
        col.add(Box.createVerticalStrut(8));
        Ui.Button add = new Ui.Button(Messages.get("dir.addState"), false).small();
        add.addActionListener(e -> {
            chosenStates.add((String) stateToAdd.getSelectedItem());
            where.get(StudentProfile.LocationScope.PREFERRED_STATE).setSelected(true);
            updateStates();
        });
        col.add(Ui.left(Ui.row(6, stateToAdd, add)));
        col.add(Box.createVerticalStrut(6));
        col.add(Ui.left(stateChips));
        col.add(label("dir.setting", "dir.optional"));
        col.add(Ui.left(Ui.row(6, settings.values().toArray(new JComponent[0]))));
        col.add(label("dir.size", "dir.optional"));
        col.add(Ui.left(Ui.row(6, sizes.values().toArray(new JComponent[0]))));
        c.add(col);
        return c;
    }

    private JComponent extrasCard() {
        Ui.Card c = new Ui.Card(new BorderLayout(), new Color(251, 251, 252), Ui.LINE);
        c.setBorder(new EmptyBorder(12, 20, 12, 20));
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JPanel t = Ui.column(0);
        t.add(Ui.left(Ui.html("<b>" + Ui.escape(Messages.get("dir.extras")) + "</b> <span style='color:#687085'>"
            + Ui.escape(Messages.get("dir.optionalParen")) + "</span>", 400, 14f, Ui.INK)));
        t.add(Ui.left(Ui.wrap(Messages.get("dir.extras.hint"), 400, 12.5f, Ui.MUTED)));
        head.add(t, BorderLayout.CENTER);
        JLabel chevron = new JLabel(Ui.icon("down", Ui.MUTED, 18));
        head.add(chevron, BorderLayout.EAST);
        head.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        extras.setVisible(false);
        extras.setBorder(new EmptyBorder(10, 0, 0, 0));
        head.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { extras.setVisible(!extras.isVisible()); c.revalidate(); }
        });
        c.add(head, BorderLayout.NORTH);
        c.add(extras, BorderLayout.CENTER);
        return c;
    }

    private JComponent howCard() {
        Ui.Card c = new Ui.Card(new BorderLayout(), Ui.BLUE_PALE, new Color(214, 222, 247));
        c.setBorder(new EmptyBorder(12, 18, 12, 18));
        c.add(Ui.html("<b>" + Ui.escape(Messages.get("how.title")) + "</b><br>" + Ui.escape(Messages.get("how.program"))
            + "<br>" + Ui.escape(Messages.get("how.place")) + "<br>" + Ui.escape(Messages.get("how.extras")), 420, 12.5f, Ui.INK));
        return c;
    }

    static JComponent cardTitle(String icon, Color color, Color bg, String text) {
        JPanel p = Ui.row(12, Ui.bubble(icon, color, bg, 40), Ui.text(text, 17f, Font.BOLD, Ui.INK));
        return p;
    }

    private static JComponent label(String key, String hintKey) {
        String hint = hintKey == null ? "" : " <span style='color:#687085;font-weight:normal'>" + Ui.escape(Messages.get(hintKey)) + "</span>";
        JLabel l = Ui.html("<b>" + Ui.escape(Messages.get(key)) + "</b>" + hint, 460, 14f, Ui.INK);
        l.setBorder(new EmptyBorder(14, 0, 8, 0));
        return Ui.left(l);
    }

    private void filter() {
        String q = search.getText().trim().toLowerCase();
        matches.clear();
        int n = 0;
        for (Program p : app.programs) {
            if (!q.isEmpty() && !p.fullName.toLowerCase().contains(q)) continue;
            matches.addElement(p);
            if (++n == 40) break; // a short list is easier to scan
        }
    }

    private void addProgram(Program p) {
        if (!chosenPrograms.contains(p)) chosenPrograms.add(p);
        redrawPrograms();
    }

    private void redrawPrograms() {
        programChips.removeAll();
        for (Program p : new ArrayList<>(chosenPrograms)) {
            Ui.Pill chip = new Ui.Pill(p.shortName() + "   \u00d7");
            chip.setSelected(true);
            chip.addActionListener(e -> { chosenPrograms.remove(p); redrawPrograms(); });
            programChips.add(chip);
        }
        if (chosenPrograms.isEmpty()) programChips.add(Ui.text(Messages.get("dir.programs.none"), 12.5f, Font.PLAIN, Ui.MUTED));
        programChips.revalidate();
        programChips.repaint();
    }

    private String countPlace() {
        return countStates.size() == 1 ? StateNames.name(countStates.iterator().next()) : Messages.get("dir.count.yourStates");
    }

    /** Recounts colleges per program for the chosen states (national when no state is chosen). */
    private void refreshCounts() {
        boolean specific = where.get(StudentProfile.LocationScope.PREFERRED_STATE).isSelected();
        Set<String> st = specific ? new TreeSet<>(chosenStates) : Set.of();
        if (!st.equals(countStates) || counts.isEmpty()) {
            countStates = st;
            counts = RecommendationEngine.programCounts(app.colleges, st);
            matchList.repaint();
        }
    }

    private void updateStates() {
        boolean specific = where.get(StudentProfile.LocationScope.PREFERRED_STATE).isSelected();
        refreshCounts();
        stateChips.removeAll();
        if (specific) {
            for (String s : new ArrayList<>(chosenStates)) {
                Ui.Pill chip = new Ui.Pill(StateNames.name(s) + "   \u00d7");
                chip.setSelected(true);
                chip.addActionListener(e -> { chosenStates.remove(s); updateStates(); });
                stateChips.add(chip);
            }
            if (chosenStates.isEmpty()) stateChips.add(Ui.text(Messages.get("dir.states.none"), 12.5f, Font.PLAIN, Ui.MUTED));
        }
        stateChips.revalidate();
        stateChips.repaint();
    }

    /** Builds the profile from this page; keeps optional answers from the folded section. */
    StudentProfile buildProfile() {
        StudentProfile s = new StudentProfile();
        s.setPrograms(chosenPrograms);
        for (Map.Entry<StudentProfile.Pathway, Ui.Pill> e : pathway.entrySet()) if (e.getValue().isSelected()) s.setPathway(e.getKey());
        StudentProfile.LocationScope scope = StudentProfile.LocationScope.NOT_SURE;
        for (Map.Entry<StudentProfile.LocationScope, Ui.Pill> e : where.entrySet()) if (e.getValue().isSelected()) scope = e.getKey();
        if (scope == StudentProfile.LocationScope.PREFERRED_STATE) s.setPreferredStates(chosenStates);
        else s.setLocationScope(scope);
        List<StudentProfile.CampusSetting> st = new ArrayList<>();
        for (Map.Entry<StudentProfile.CampusSetting, Ui.Pill> e : settings.entrySet()) if (e.getValue().isSelected()) st.add(e.getKey());
        s.setSettings(st);
        List<StudentProfile.CollegeSize> sz = new ArrayList<>();
        for (Map.Entry<StudentProfile.CollegeSize, Ui.Pill> e : sizes.entrySet()) if (e.getValue().isSelected()) sz.add(e.getKey());
        s.setSizes(sz);
        extras.apply(s);
        return s;
    }
}

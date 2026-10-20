import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Results page: her answers and next steps on the left, college cards on the right. */
public class ResultsPanel extends JPanel {
    static final int MAX_MATCHES = 12, MAX_ELSEWHERE = 4;
    private final JLabel count = Ui.muted("");
    private final JLabel sortExplain = Ui.muted("");
    private final JPanel side = new ScrollablePanel();
    private final JPanel sections = new ScrollablePanel();
    private final JComboBox<RecommendationEngine.SortOrder> sort = new JComboBox<>(RecommendationEngine.SortOrder.values());
    private final Shortlist shortlist = new Shortlist(); // kept for the session until Start over
    private final RecommendationEngine engine = new RecommendationEngine();
    private final JButton myList = new JButton();
    private final JCheckBox onlyMatching = new JCheckBox(Messages.get("results.filter"));
    private final JComboBox<Object> focus = new JComboBox<>();
    private final JPanel focusBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    private StudentProfile fullProfile, student;      // fullProfile keeps every program; student may be focused on one
    private List<College> baseColleges = new ArrayList<>();
    private List<CollegeRecommendation> ranked = new ArrayList<>(), elsewhere = new ArrayList<>();
    private boolean updatingFocus = false;

    public ResultsPanel(Runnable edit, Runnable startOver) {
        super(new BorderLayout(14, 12));
        setBorder(new EmptyBorder(18, 22, 14, 22));
        setBackground(Ui.BG);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);
        for (JComponent c : new JComponent[]{Ui.title(Messages.get("results.title"), 24), count, sortExplain}) {
            c.setAlignmentX(LEFT_ALIGNMENT);
            text.add(c);
            text.add(Box.createVerticalStrut(4));
        }
        top.add(text, BorderLayout.CENTER);

        JButton back = new JButton("\u2190 " + Messages.get("results.edit"));
        back.addActionListener(e -> edit.run());
        JButton restart = new JButton(Messages.get("results.startOver"));
        restart.setToolTipText(Messages.get("results.startOver.tip"));
        restart.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(this, Messages.get("results.startOver.confirm"),
                Messages.get("results.startOver"), JOptionPane.OK_CANCEL_OPTION);
            if (ok == JOptionPane.OK_OPTION) { shortlist.clear(); student = null; fullProfile = null; listChanged(); startOver.run(); }
        });
        myList.addActionListener(e -> new MyListDialog(this, shortlist, fullProfile, engine, this::listEdited).setVisible(true));
        listChanged();
        JPanel controls = new JPanel(new GridLayout(3, 1, 0, 6));
        controls.setOpaque(false);
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        row1.setOpaque(false);
        row1.add(myList); row1.add(back); row1.add(restart);
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        row2.setOpaque(false);
        row2.add(new JLabel(Messages.get("sort.label"))); row2.add(sort);
        focusBox.setOpaque(false);
        focusBox.add(new JLabel(Messages.get("results.focus"))); focusBox.add(focus);
        row2.add(focusBox);
        onlyMatching.setOpaque(false);
        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        row3.setOpaque(false);
        row3.add(onlyMatching);
        controls.add(row1); controls.add(row2); controls.add(row3);
        onlyMatching.addActionListener(e -> { if (student != null) render(); });
        focus.addActionListener(e -> { if (!updatingFocus && fullProfile != null) refocus(); });
        top.add(controls, BorderLayout.EAST);
        sort.addActionListener(e -> { if (student != null) render(); });
        add(top, BorderLayout.NORTH);

        side.setBackground(Color.WHITE);
        side.setBorder(Ui.card());
        JScrollPane sideScroll = new JScrollPane(side);
        sideScroll.setPreferredSize(new Dimension(310, 100));
        sideScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sideScroll.setBorder(null);
        add(sideScroll, BorderLayout.WEST);

        sections.setOpaque(false);
        JScrollPane scroll = new JScrollPane(sections);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(22);
        add(scroll, BorderLayout.CENTER);

        add(Ui.muted(Messages.get("results.legend") + "   " + Messages.get("results.source")), BorderLayout.SOUTH);
    }

    public void display(StudentProfile s, List<CollegeRecommendation> ranked, List<CollegeRecommendation> elsewhere) {
        this.fullProfile = s;
        this.student = s;
        this.ranked = new ArrayList<>(ranked);
        this.elsewhere = elsewhere;
        baseColleges = new ArrayList<>();
        for (CollegeRecommendation r : ranked) baseColleges.add(r.college);
        sort.setSelectedItem(RecommendationEngine.SortOrder.BEST_FIT);
        onlyMatching.setSelected(false);
        updatingFocus = true; // fill the Focus menu without triggering a re-rank
        focus.removeAllItems();
        focus.addItem(Messages.get("results.focus.all"));
        for (Program p : s.getPrograms()) focus.addItem(p);
        updatingFocus = false;
        focusBox.setVisible(s.getPrograms().size() > 1);
        showSide(s);
        render();
    }

    /** "Focus on one interest": re-evaluates the same colleges for one chosen program, without editing the form. */
    private void refocus() {
        Object choice = focus.getSelectedItem();
        student = choice instanceof Program ? fullProfile.withPrograms(List.of((Program) choice)) : fullProfile;
        ranked = engine.recommendAll(student, baseColleges);
        ranked.sort(RecommendationEngine.comparator(RecommendationEngine.SortOrder.BEST_FIT, student));
        elsewhere = engine.worthALookElsewhere(student, ranked, MAX_ELSEWHERE);
        render();
    }

    /** Re-sorts with the chosen order and redraws the cards. */
    private void render() {
        RecommendationEngine.SortOrder order = (RecommendationEngine.SortOrder) sort.getSelectedItem();
        ranked.sort(RecommendationEngine.comparator(order, student));
        sortExplain.setText("<html><div style='width:520px'>" + Ui.escape(Messages.get("sort.explain." + order.name())) + "</div></html>");
        sections.removeAll();

        // Social preferences shape the order; they only filter when the student asks for it.
        boolean filter = onlyMatching.isSelected();
        List<CollegeRecommendation> matches = new ArrayList<>();
        for (CollegeRecommendation r : ranked) {
            if (matches.size() == MAX_MATCHES) break;
            if (r.hasProgramEvidence() && (!filter || r.social.status != FitStatus.MISMATCH)) matches.add(r);
        }
        count.setText(Messages.get("results.count", ranked.size(), matches.size()));
        addSection(Messages.get(filter ? "results.matches.filtered" : "results.matches"), null, matches);
        if (!elsewhere.isEmpty())
            addSection(Messages.get("results.elsewhere", String.join(", ", student.getPreferredStates())),
                Messages.get("results.elsewhere.note"), elsewhere);
        sections.revalidate();
        sections.repaint();
    }

    private void addSection(String title, String note, List<CollegeRecommendation> recs) {
        JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        head.setOpaque(false);
        JLabel t = new JLabel(title + (note == null ? "" : "   "));
        t.setFont(Ui.font(t, Font.BOLD, 15f));
        head.add(t);
        if (note != null) head.add(Ui.muted(note));
        head.setAlignmentX(LEFT_ALIGNMENT);
        sections.add(head);
        sections.add(Box.createVerticalStrut(8));

        JPanel grid = new JPanel(new GridLayout(0, 2, 12, 12));
        grid.setOpaque(false);
        grid.setAlignmentX(LEFT_ALIGNMENT);
        if (recs.isEmpty()) grid.add(Ui.muted(Messages.get("results.none")));
        for (CollegeRecommendation r : recs)
            grid.add(new CollegeCardPanel(r, () -> CollegeDetailsPanel.show(this, student, r), shortlist, this::listChanged));
        sections.add(grid);
        sections.add(Box.createVerticalStrut(18));
    }

    /** A card's Save button changed: update the count only, so the page does not jump. */
    private void listChanged() { myList.setText(Messages.get("list.button", shortlist.size())); }

    /** A college was removed in My list: update the count and redraw cards so their Save buttons match. */
    private void listEdited() { listChanged(); render(); }

    private void showSide(StudentProfile s) {
        side.removeAll();
        side.add(heading(Messages.get("results.answers")));
        answer("form.pathway", s.getPathway().toString());
        List<String> programs = new ArrayList<>();
        for (Program p : s.getPrograms()) programs.add(p.shortName());
        answer("form.program", programs.isEmpty() ? Messages.get("program.notSure") : String.join(", ", programs));
        answer("form.sat", s.getSatStatus() == StudentProfile.SatStatus.PROVIDED
            ? s.getSatReadingWriting() + " / " + s.getSatMath() : s.getSatStatus().toString());
        answer("form.where", s.getLocationScope() == StudentProfile.LocationScope.PREFERRED_STATE
            ? String.join(", ", s.getPreferredStates()) : s.getLocationScope().toString());
        answer("form.setting", joinOrNotSure(s.getSettings()));
        answer("form.size", joinOrNotSure(s.getSizes()));
        answer("form.home", s.hasHomeState() ? s.getHomeState() : Messages.get("form.notSure"));
        answer("form.budget", s.getBudgetStatus() == StudentProfile.ChoiceStatus.KNOWN
            ? Messages.money(s.getAnnualBudget()) : s.getBudgetStatus().toString());
        answer("form.income", s.getIncomeBracket().toString());
        answer("form.aid", s.getAidEligibility().toString());

        side.add(Box.createVerticalStrut(14));
        side.add(heading(Messages.get("results.steps")));
        for (String step : new NextStepGenerator().profileSteps(s)) {
            JCheckBox cb = new JCheckBox("<html><div style='width:185px'>" + Ui.escape(step) + "</div></html>");
            cb.setOpaque(false);
            cb.setAlignmentX(LEFT_ALIGNMENT);
            side.add(cb);
        }
        side.revalidate();
        side.repaint();
    }

    private static String joinOrNotSure(java.util.Collection<?> values) {
        if (values.isEmpty()) return Messages.get("form.notSure");
        List<String> out = new ArrayList<>();
        for (Object v : values) out.add(v.toString());
        return String.join(", ", out);
    }

    private JLabel heading(String text) {
        JLabel l = Ui.title(text, 17);
        l.setAlignmentX(LEFT_ALIGNMENT);
        l.setBorder(new EmptyBorder(0, 0, 6, 0));
        return l;
    }

    private void answer(String key, String value) {
        JLabel l = new JLabel("<html><div style='width:200px'><span style='color:#687085'>" + Ui.escape(Messages.get(key))
            + "</span><br><b>" + Ui.escape(value) + "</b></div></html>");
        l.setAlignmentX(LEFT_ALIGNMENT);
        l.setBorder(new EmptyBorder(0, 0, 6, 0));
        side.add(l);
    }
}

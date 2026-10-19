import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Results page: her answers and next steps on the left, college cards on the right. */
public class ResultsPanel extends JPanel {
    static final int MAX_MATCHES = 12, MAX_ELSEWHERE = 4;
    private final JLabel count = Ui.muted("");
    private final JPanel side = new ScrollablePanel();
    private final JPanel sections = new ScrollablePanel();
    private final JComboBox<RecommendationEngine.SortOrder> sort = new JComboBox<>(RecommendationEngine.SortOrder.values());
    private final JLabel sortExplain = Ui.muted("");
    private StudentProfile student;
    private final Shortlist shortlist = new Shortlist(); // kept for the whole session, even after editing answers
    private final RecommendationEngine engine = new RecommendationEngine();
    private final JButton myList = new JButton();
    private List<CollegeRecommendation> ranked = new ArrayList<>(), elsewhere = new ArrayList<>();

    public ResultsPanel(Runnable edit) {
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
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controls.setOpaque(false);
        myList.addActionListener(e -> new MyListDialog(this, shortlist, student, engine, this::listEdited).setVisible(true));
        listChanged();
        controls.add(myList);
        controls.add(new JLabel(Messages.get("sort.label")));
        controls.add(sort);
        controls.add(back);
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

        JLabel legend = Ui.muted(Messages.get("results.legend") + "   " + Messages.get("results.source"));
        add(legend, BorderLayout.SOUTH);
    }

    public void display(StudentProfile s, List<CollegeRecommendation> ranked, List<CollegeRecommendation> elsewhere) {
        this.student = s;
        this.ranked = new ArrayList<>(ranked);
        this.elsewhere = elsewhere;
        sort.setSelectedItem(RecommendationEngine.SortOrder.BEST_FIT);
        showSide(s);
        render();
    }

    /** Re-sorts with the chosen order and redraws the cards. */
    private void render() {
        StudentProfile s = student;
        RecommendationEngine.SortOrder order = (RecommendationEngine.SortOrder) sort.getSelectedItem();
        ranked.sort(RecommendationEngine.comparator(order, s));
        sortExplain.setText("<html><div style='width:540px'>" + Ui.escape(Messages.get("sort.explain." + order.name())) + "</div></html>");
        sections.removeAll();

        // Section 1: colleges that award her program and do not conflict with her preferences.
        List<CollegeRecommendation> matches = new ArrayList<>();
        for (CollegeRecommendation r : ranked) {
            if (matches.size() == MAX_MATCHES) break;
            boolean programOk = s.getProgram().isNotSure() || r.programPriority == 0;
            if (programOk && r.social.status != FitStatus.MISMATCH) matches.add(r);
        }
        count.setText(Messages.get("results.count", ranked.size(), matches.size()));
        addSection(Messages.get("results.matches"), null, matches, s);

        // Section 2: discovery beyond the state she knows.
        if (!elsewhere.isEmpty())
            addSection(Messages.get("results.elsewhere", s.getPreferredState()), Messages.get("results.elsewhere.note"), elsewhere, s);

        sections.revalidate();
        sections.repaint();
    }

    private void addSection(String title, String note, List<CollegeRecommendation> recs, StudentProfile s) {
        JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        head.setOpaque(false);
        JLabel t = new JLabel(title + (note == null ? "" : "   "));
        t.setFont(t.getFont().deriveFont(Font.BOLD, 15f));
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
            grid.add(new CollegeCardPanel(r, () -> CollegeDetailsPanel.show(this, s, r), shortlist, this::listChanged));
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
        answer("form.pathway", s.getPathway());
        answer("form.program", s.getProgram().isNotSure() ? s.getProgram() : s.getProgram().shortName());
        answer("form.sat", s.getSatStatus() == StudentProfile.SatStatus.PROVIDED
            ? s.getSatReadingWriting() + " / " + s.getSatMath() : s.getSatStatus());
        answer("form.home", s.hasHomeState() ? s.getHomeState() : Messages.get("form.notSure"));
        answer("form.where", s.getLocationScope() == StudentProfile.LocationScope.PREFERRED_STATE
            ? s.getPreferredState() : s.getLocationScope());
        answer("form.setting", s.getCampusSetting());
        answer("form.size", s.getCollegeSize());
        answer("form.budget", s.getBudgetStatus() == StudentProfile.ChoiceStatus.KNOWN
            ? Messages.money(s.getAnnualBudget()) : s.getBudgetStatus());
        answer("form.income", s.getIncomeBracket());
        answer("form.aid", s.getAidEligibility());

        side.add(Box.createVerticalStrut(14));
        side.add(heading(Messages.get("results.steps")));
        for (String step : new NextStepGenerator().profileSteps(s)) {
            JCheckBox cb = new JCheckBox("<html><div style='width:200px'>" + Ui.escape(step) + "</div></html>");
            cb.setOpaque(false);
            cb.setAlignmentX(LEFT_ALIGNMENT);
            side.add(cb);
        }
        side.revalidate();
        side.repaint();
    }

    private JLabel heading(String text) {
        JLabel l = Ui.title(text, 17);
        l.setAlignmentX(LEFT_ALIGNMENT);
        l.setBorder(new EmptyBorder(0, 0, 6, 0));
        return l;
    }

    private void answer(String key, Object value) {
        JLabel l = new JLabel("<html><span style='color:#687085'>" + Ui.escape(Messages.get(key)) + "</span><br><b>"
            + Ui.escape(String.valueOf(value)) + "</b></html>");
        l.setAlignmentX(LEFT_ALIGNMENT);
        l.setBorder(new EmptyBorder(0, 0, 6, 0));
        side.add(l);
    }
}

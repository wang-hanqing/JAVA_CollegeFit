import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

/** Step 5: up to three saved colleges side by side, what to do next, and questions to ask. */
class MyCollegesPanel extends JPanel {
    private final CollegeFitSwingApp app;
    private final JPanel body = new JPanel(new BorderLayout());

    MyCollegesPanel(CollegeFitSwingApp app) {
        super(new BorderLayout());
        this.app = app;
        setOpaque(false);
        body.setOpaque(false);
        add(body);
    }

    void refresh() {
        app.compare.retainAll(app.shortlist.colleges());
        for (College c : app.shortlist.colleges()) if (app.compare.size() < 3) app.compare.add(c);
        StudentProfile s = app.profile;
        JPanel content = Ui.column(0);
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JPanel t = Ui.column(0);
        t.add(Ui.left(Ui.title(Messages.get("mine.title"), 28)));
        t.add(Ui.left(Ui.text(Messages.get("mine.subtitle"), 14.5f, Font.PLAIN, Ui.MUTED)));
        head.add(t, BorderLayout.CENTER);
        content.add(Ui.left(head));
        content.add(Box.createVerticalStrut(10));

        List<College> saved = app.shortlist.colleges();
        if (saved.isEmpty()) {
            content.add(Ui.left(Ui.wrap(Messages.get("mine.empty"), 600, 14f, Ui.MUTED)));
        } else {
            JPanel strip = Ui.row(6, Ui.text(Messages.get("mine.pick"), 13f, Font.BOLD, Ui.INK));
            for (College c : saved) {
                Ui.Pill p = new Ui.Pill(c.name);
                p.setSelected(app.compare.contains(c));
                p.addActionListener(e -> {
                    if (p.isSelected() && app.compare.size() >= 3) { p.setSelected(false); JOptionPane.showMessageDialog(this, Messages.get("compare.full")); return; }
                    if (p.isSelected()) app.compare.add(c); else app.compare.remove(c);
                    refresh();
                });
                strip.add(p);
            }
            content.add(Ui.left(strip));
            content.add(Box.createVerticalStrut(12));
            List<CollegeRecommendation> recs = app.engine.recommendAll(s, new ArrayList<>(app.compare));
            JComponent[] cards = new JComponent[3];
            for (int i = 0; i < 3; i++) cards[i] = i < recs.size() ? card(recs.get(i), s) : placeholder();
            content.add(Ui.left(DetailsPanel.grid(3, cards)));
            content.add(Box.createVerticalStrut(14));

            Set<String> todo = new LinkedHashSet<>();
            NextStepGenerator g = new NextStepGenerator();
            for (CollegeRecommendation r : recs) todo.addAll(g.collegeSteps(s, r));
            todo.addAll(g.profileSteps(s));
            JPanel steps = Ui.column(0);
            steps.add(Ui.left(DirectionPanel.cardTitle("list", Ui.BLUE, Ui.BLUE_SOFT, Messages.get("mine.todo"))));
            steps.add(Box.createVerticalStrut(6));
            for (String x : todo) {
                JCheckBox cb = new JCheckBox("<html><div style='width:" + Ui.css(470) + "px'>" + Ui.escape(x) + "</div></html>");
                cb.setOpaque(false);
                cb.setFont(Ui.sans(Font.PLAIN, 13f));
                steps.add(Ui.left(cb));
            }
            String topic = s.hasPrograms() ? OptionsPanel.topic(s) : Messages.get("mine.q.programFallback");
            JPanel qs = Ui.column(0);
            qs.add(Ui.left(DirectionPanel.cardTitle("chat", Ui.AMBER, Ui.AMBER_SOFT, Messages.get("mine.questions"))));
            qs.add(Box.createVerticalStrut(6));
            for (int i = 1; i <= 4; i++) qs.add(Ui.left(Ui.wrap("\u2022  " + Messages.get("mine.q" + i, topic), 420, 13f, Ui.INK)));
            Ui.Card a = new Ui.Card(new BorderLayout(), Ui.WHITE, Ui.LINE); a.add(steps);
            Ui.Card b = new Ui.Card(new BorderLayout(), Ui.WHITE, Ui.LINE); b.add(qs);
            content.add(Ui.left(DetailsPanel.grid(2, a, b)));
        }

        Ui.Button back = new Ui.Button(Messages.get("mine.back"), false);
        back.addActionListener(e -> app.backToOptions());
        Ui.Button over = new Ui.Button(Messages.get("startOver"), false);
        over.addActionListener(e -> app.startOver());
        Ui.Button export = new Ui.Button(Messages.get("mine.exportAll"), true);
        export.setEnabled(!saved.isEmpty());
        export.addActionListener(e -> export());
        body.removeAll();
        body.add(CollegeFitSwingApp.page(content, CollegeFitSwingApp.privacyNote("mine.privacy"), back, over, export));
        body.revalidate();
        body.repaint();
    }

    private JComponent card(CollegeRecommendation r, StudentProfile s) {
        College c = r.college;
        Ui.Card card = new Ui.Card(new BorderLayout(), Ui.WHITE, Ui.LINE);
        JPanel col = Ui.column(0);
        JLabel name = Ui.wrap(c.name, 300, 16f, Ui.INK);
        name.setFont(Ui.sans(Font.BOLD, 16f));
        col.add(Ui.left(name));
        col.add(Ui.left(Ui.row(6, Ui.text(c.city + ", " + c.state, 12f, Font.PLAIN, Ui.MUTED),
            Ui.tag(Messages.get(c.isPublic() ? "type.public" : "type.private")), Ui.tag(Messages.get(c.isTwoYear() ? "tag.two" : "tag.four")))));
        col.add(Box.createVerticalStrut(8));
        boolean oos = c.isPublic() && s.hasHomeState() && !s.getHomeState().equals(c.state);
        Integer avg = oos ? null : s.getIncomeBracket().isKnown() ? c.netPrice(s.getIncomeBracket()) : c.netPriceAll;
        String avgLabel = s.getIncomeBracket().isKnown() ? Messages.get("mine.avg.bracket", s.getIncomeBracket()) : Messages.get("mine.avg.all");
        String compl = c.completionRate == null ? Messages.get("notReported")
            : Messages.get("mine.completion", Math.round(c.completionRate * 100) + "%", c.completionYears);
        col.add(Ui.left(row(Messages.get("lens.program"), r.programLine(s))));
        col.add(Ui.left(row(Messages.get("tile.admission"), r.academic.label)));
        col.add(Ui.left(row(Messages.get("tile.cost"), Messages.money(oos ? c.outOfStateCostEstimate() : c.costOfAttendance))));
        col.add(Ui.left(row(avgLabel, Messages.money(avg))));
        col.add(Ui.left(row(Messages.get("tile.completion"), compl)));
        String why = r.cardWhy();
        if (!why.isEmpty()) { col.add(Box.createVerticalStrut(8)); col.add(Ui.left(Ui.note(why, Ui.GREEN_SOFT, Ui.GREEN_TEXT, 290, 12f))); }
        col.add(Box.createVerticalStrut(9));
        Ui.Button details = new Ui.Button(Messages.get("card.details"), false).small();
        details.addActionListener(e -> app.showDetails(c));
        JButton remove = Ui.link(Messages.get("list.remove"));
        remove.addActionListener(e -> { app.toggleSaved(c); refresh(); });
        col.add(Ui.left(Ui.row(8, details, remove)));
        card.add(col, BorderLayout.NORTH);
        return card;
    }

    private static JComponent row(String label, String value) {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setOpaque(false);
        p.setBorder(new javax.swing.border.CompoundBorder(new javax.swing.border.MatteBorder(0, 0, 1, 0, new Color(238, 240, 244)), new EmptyBorder(5, 0, 5, 0)));
        p.add(Ui.wrap(label, 140, 12.5f, Ui.MUTED), BorderLayout.WEST);
        JLabel v = Ui.html("<div style='text-align:right'><b>" + Ui.escape(value) + "</b></div>", 150, 12.5f, Ui.INK);
        p.add(v, BorderLayout.EAST);
        return p;
    }

    private JComponent placeholder() {
        Ui.Card c = new Ui.Card(new BorderLayout(), new Color(251, 251, 252), Ui.LINE);
        c.add(Ui.wrap(Messages.get("mine.placeholder"), 280, 13f, Ui.MUTED));
        return c;
    }

    private void export() {
        int ok = JOptionPane.showConfirmDialog(this, Ui.wrap(Messages.get("list.privacy.confirm"), 360, 13f, Ui.INK),
            Messages.get("mine.export"), JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(System.getProperty("user.home"), "CollegeFit_my_list_" + LocalDate.now() + ".csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = chooser.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".csv")) f = new File(f.getParentFile(), f.getName() + ".csv");
        try {
            app.shortlist.exportCsv(f.toPath(), app.profile, app.engine);
            JOptionPane.showMessageDialog(this, Messages.get("list.exported", f.getAbsolutePath()));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, Messages.get("list.exportFailed") + "\n" + ex.getMessage());
        }
    }
}

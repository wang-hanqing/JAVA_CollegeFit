import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

/** Step 6: her colleges with her own reasons, what to do next, questions to ask, export and Start over. */
class NextStepsPanel extends JPanel {
    private final CollegeFitSwingApp app;
    private final JPanel body = new JPanel(new BorderLayout());

    NextStepsPanel(CollegeFitSwingApp app) {
        super(new BorderLayout());
        this.app = app;
        setOpaque(false);
        body.setOpaque(false);
        add(body);
    }

    void refresh() {
        StudentProfile s = app.profile;
        JPanel content = Ui.column(0);
        content.add(Ui.left(Ui.title(Messages.get("next.title"), 28)));
        content.add(Ui.left(Ui.text(Messages.get("next.subtitle"), 14f, Font.PLAIN, Ui.MUTED)));
        content.add(Box.createVerticalStrut(12));

        List<College> list = new ArrayList<>(app.compare.isEmpty() ? app.shortlist.colleges() : app.compare);
        if (list.isEmpty()) {
            content.add(Ui.left(Ui.wrap(Messages.get("cmp.none"), 700, 14f, Ui.MUTED)));
        } else {
            JPanel cards = new JPanel(new GridLayout(1, 3, 12, 0));
            cards.setOpaque(false);
            for (College c : list) {
                Set<String> reasons = app.keepReasons.getOrDefault(c, Set.of());
                List<String> names = new ArrayList<>();
                for (String k : reasons) names.add(Messages.get("keepReason." + k));
                Ui.Card card = new Ui.Card(new BorderLayout(), Ui.WHITE, Ui.LINE);
                JPanel col = Ui.column(0);
                JLabel n = Ui.wrap(c.name, 300, 15f, Ui.INK);
                n.setFont(Ui.sans(Font.BOLD, 15f));
                col.add(Ui.left(n));
                col.add(Ui.left(Ui.text(c.city + ", " + c.state + ". " + Messages.get("group." + c.type().name()), 12f, Font.PLAIN, Ui.MUTED)));
                col.add(Box.createVerticalStrut(8));
                col.add(Ui.left(Ui.html("<b>" + Ui.escape(Messages.get("next.myReasons")) + "</b> "
                    + Ui.escape(names.isEmpty() ? Messages.get("next.noReasons") : String.join(", ", names)), 300, 12.5f, Ui.INK)));
                JButton d = Ui.link(Messages.get("card.detailsShort"));
                d.addActionListener(e -> app.showDetails(c, 4));
                col.add(Ui.left(d));
                card.add(col, BorderLayout.NORTH);
                cards.add(card);
            }
            for (int i = list.size(); i < 3; i++) { JPanel empty = new JPanel(); empty.setOpaque(false); cards.add(empty); }
            content.add(Ui.left(cards));
            content.add(Box.createVerticalStrut(14));

            Set<String> todo = new LinkedHashSet<>();
            NextStepGenerator g = new NextStepGenerator();
            for (CollegeRecommendation r : app.engine.recommendAll(s, list)) todo.addAll(g.collegeSteps(s, r));
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

        Ui.Button back = new Ui.Button(Messages.get("next.back"), false);
        back.addActionListener(e -> app.showCompare());
        Ui.Button over = new Ui.Button(Messages.get("startOver"), false);
        over.addActionListener(e -> app.startOver());
        Ui.Button export = new Ui.Button(Messages.get("mine.exportAll"), true);
        export.setEnabled(app.shortlist.size() > 0);
        export.addActionListener(e -> export());
        body.removeAll();
        body.add(CollegeFitSwingApp.page(content, CollegeFitSwingApp.privacyNote("mine.privacy"), back, over, export));
        body.revalidate();
        body.repaint();
    }

    private void export() {
        int ok = JOptionPane.showConfirmDialog(this, Ui.wrap(Messages.get("list.privacy.confirm"), 360, 13f, Ui.INK),
            Messages.get("mine.exportAll"), JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;
        if (CollegeFitSwingApp.WEB) { // browser: the page's "Download my list" button saves this file to her device
            try {
                app.shortlist.exportCsv(java.nio.file.Paths.get(CollegeFitSwingApp.WEB_EXPORT), app.profile, app.engine, app.keepReasons);
                JOptionPane.showMessageDialog(this, Ui.wrap(Messages.get("web.exported"), 380, 13f, Ui.INK));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, Messages.get("list.exportFailed") + "\n" + ex.getMessage());
            }
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(System.getProperty("user.home"), "CollegeFit_my_list_" + LocalDate.now() + ".csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = chooser.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".csv")) f = new File(f.getParentFile(), f.getName() + ".csv");
        try {
            app.shortlist.exportCsv(f.toPath(), app.profile, app.engine, app.keepReasons);
            JOptionPane.showMessageDialog(this, Messages.get("list.exported", f.getAbsolutePath()));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, Messages.get("list.exportFailed") + "\n" + ex.getMessage());
        }
    }
}

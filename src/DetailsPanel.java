import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.net.URI;
import java.util.List;

/** Step 4: one college. Overview tiles, three lens boxes, why and what to verify, then detail tabs and links. */
class DetailsPanel extends JPanel {
    private final CollegeFitSwingApp app;
    private final JPanel body = new JPanel(new BorderLayout());
    private College college;
    private String tab = "overview";

    DetailsPanel(CollegeFitSwingApp app) {
        super(new BorderLayout());
        this.app = app;
        setOpaque(false);
        body.setOpaque(false);
        add(body);
    }

    void show(College c) { college = c; tab = "overview"; render(); }

    private void render() {
        CollegeRecommendation r = app.engine.evaluate(app.profile, college);
        StudentProfile s = app.profile;
        JPanel content = Ui.column(0);

        JButton back = Ui.link("\u2190 " + Messages.get(app.detailsFromCompare() ? "details.backCompare" : "details.back"));
        back.addActionListener(e -> app.closeDetails());
        content.add(Ui.left(back));
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JPanel names = Ui.column(0);
        names.add(Ui.left(Ui.title(college.name, 28)));
        names.add(Ui.left(Ui.text(college.city + ", " + college.state + ". " + Messages.get("details.kind." + college.type().name()),
            14f, Font.PLAIN, Ui.MUTED)));
        head.add(names, BorderLayout.CENTER);
        boolean kept = app.isKept(college);
        Ui.Button keep = new Ui.Button(kept ? "\u2713 " + Messages.get("keep.kept") : Messages.get("keep.keep"), !kept);
        if (kept) keep.accent(Ui.GREEN);
        keep.addActionListener(e -> { if (app.isKept(college)) app.unkeep(college); else app.keep(college); render(); });
        boolean aside = app.notForMe.containsKey(college);
        Ui.Button no = new Ui.Button(Messages.get(aside ? "not.undo" : "keep.not"), false);
        no.addActionListener(e -> {
            if (app.notForMe.containsKey(college)) app.undoNotForMe(college);
            else { String reason = OptionCard.askReason(this, college.name, ""); if (reason != null) app.notForMe(college, reason); }
            render();
        });
        head.add(Ui.row(10, no, keep), BorderLayout.EAST);
        content.add(Ui.left(head));
        content.add(Box.createVerticalStrut(10));
        content.add(Ui.left(tabs()));
        content.add(Box.createVerticalStrut(12));

        switch (tab) {
            case "program":
                content.add(Ui.left(grid(2, academicsBox(r, 0, 1), academicsBox(r, 2, 3))));
                break;
            case "costs":
                content.add(Ui.left(grid(2, factBox("dollar", Ui.FINANCIAL, Ui.GREEN_SOFT, Messages.get("lens.financial"), r.financial, npcButton()),
                    whyVerify(r, s, false))));
                break;
            case "place":
                content.add(Ui.left(grid(2, factBox("people", Ui.SOCIAL, Ui.ORANGE_SOFT, Messages.get("lens.social"), r.social, null),
                    Ui.note(Messages.get("details.place.note"), Ui.BLUE_PALE, Ui.INK, 420, 12.5f))));
                break;
            default:
                content.add(Ui.left(tiles(r, s)));
                content.add(Box.createVerticalStrut(12));
                content.add(Ui.left(grid(3,
                    academicsBox(r, 0, 3),
                    factBox("dollar", Ui.FINANCIAL, Ui.GREEN_SOFT, Messages.get("lens.financial"), r.financial, npcButton()),
                    factBox("people", Ui.SOCIAL, Ui.ORANGE_SOFT, Messages.get("lens.social"), r.social, null))));
                content.add(Box.createVerticalStrut(12));
                if (r.nursingBachelor) { content.add(Ui.left(Ui.note(Messages.get("prog.next.license"), Ui.AMBER_SOFT, Ui.AMBER_TEXT, 900, 12.5f))); content.add(Box.createVerticalStrut(10)); }
                content.add(Ui.left(whyVerify(r, s, true)));
        }

        Ui.Button site = new Ui.Button(Messages.get("details.site"), false).small();
        site.setIcon(Ui.icon("link", Ui.INK, 14));
        site.addActionListener(e -> open(college.schoolUrl));
        Ui.Button copy = new Ui.Button(Messages.get("details.copy"), false).small();
        copy.addActionListener(e -> {
            String t = college.schoolUrl + (blank(college.netPriceCalculatorUrl) ? "" : "\n" + college.netPriceCalculatorUrl);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(t), null);
            copy.setText(Messages.get("details.copied"));
        });
        Ui.Button extra = new Ui.Button(Messages.get("details.addInfo"), false).small();
        extra.addActionListener(e -> editExtras());
        body.removeAll();
        body.add(CollegeFitSwingApp.page(content, Ui.text(Messages.get("details.disclaimer"), 12f, Font.PLAIN, Ui.MUTED), extra, site, copy));
        body.revalidate();
        body.repaint();
    }

    private JComponent tabs() {
        String[][] t = {{"overview", "tab.overview"}, {"program", "tab.program"}, {"costs", "tab.costs"}, {"place", "tab.place"}};
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 0));
        bar.setOpaque(false);
        bar.setBorder(new MatteBorder(0, 0, 1, 0, Ui.LINE));
        for (String[] x : t) {
            JButton b = Ui.link(Messages.get(x[1]));
            boolean on = x[0].equals(tab);
            b.setForeground(on ? Ui.INK : Ui.MUTED);
            b.setFont(Ui.sans(on ? Font.BOLD : Font.PLAIN, 14f));
            b.setBorder(on ? new javax.swing.border.CompoundBorder(new MatteBorder(0, 0, 3, 0, Ui.BLUE), new EmptyBorder(6, 0, 5, 0))
                : new EmptyBorder(6, 0, 8, 0));
            b.addActionListener(e -> { tab = x[0]; render(); });
            bar.add(b);
        }
        bar.setMaximumSize(new Dimension(4000, 40));
        return bar;
    }

    private JComponent tiles(CollegeRecommendation r, StudentProfile s) {
        College c = r.college;
        String degreesLabel = s.hasPrograms() ? Messages.get("tile.degrees") : Messages.get("tile.degreesAll");
        boolean open = Boolean.TRUE.equals(c.openAdmission);
        String admission = open ? Messages.get("ac.open")
            : c.admissionRate != null && c.admissionRate > 0 ? Math.round(c.admissionRate * 100) + "%" : Messages.get("notReported");
        String admissionNote = Messages.get(open ? "tile.admission.open" : "tile.admission.note");
        String deg = r.recentDegrees > 0 ? Messages.number(r.recentDegrees) : Messages.get("notReported");
        boolean oos = c.isPublic() && s.hasHomeState() && !s.getHomeState().equals(c.state);
        Integer cost = oos ? c.outOfStateCostEstimate() : c.costOfAttendance;
        String compl = c.completionRate == null ? Messages.get("notReported") : Math.round(c.completionRate * 100) + "%";
        String complNote = c.completionYears == 0 ? "" : Messages.get("tile.completion.within", c.completionYears);
        String size = c.undergraduates == null ? Messages.get("notReported") : Messages.number(c.undergraduates);
        return grid(5,
            tile("cap", Ui.BLUE, new Color(238, 242, 252), degreesLabel, deg, r.recentDegrees > 0 ? r.levelText() : ""),
            tile("check", Ui.BLUE, new Color(238, 242, 252), Messages.get(open ? "tile.admission" : "tile.admissionRate"), admission, admissionNote),
            tile("dollar", Ui.GREEN, Ui.GREEN_SOFT, Messages.get("tile.cost"), Messages.money(cost),
                Messages.get(oos ? "tile.cost.oos" : c.isPublic() ? "tile.cost.public" : "tile.cost.private")),
            tile("book", Ui.PURPLE, Ui.PURPLE_SOFT, Messages.get("tile.completion"), compl, complNote),
            tile("people", Ui.ORANGE, Ui.ORANGE_SOFT, Messages.get("tile.size"), size, Messages.get("tile.size.note")));
    }

    private static JComponent tile(String icon, Color color, Color bg, String label, String value, String note) {
        Ui.Card c = new Ui.Card(new BorderLayout(10, 0), bg, null);
        c.setBorder(new EmptyBorder(10, 12, 10, 12));
        c.add(Ui.bubble(icon, color, Ui.WHITE, 36), BorderLayout.WEST);
        JPanel col = Ui.column(0);
        col.add(Ui.left(Ui.text(label, 11.5f, Font.PLAIN, Ui.MUTED)));
        col.add(Ui.left(Ui.text(value, 15.5f, Font.BOLD, Ui.INK)));
        if (!note.isEmpty()) col.add(Ui.left(Ui.text(note, 11f, Font.PLAIN, Ui.MUTED)));
        c.add(col, BorderLayout.CENTER);
        return c;
    }

    /** Program facts shown at the top of the academic box on the overview tab. */
    private static List<Fact> programRows(CollegeRecommendation r) { return r.program.facts; }

    /**
     * "Academics and programs" in four parts: Program information, Admissions, Student outcomes,
     * Academic resources. Shows parts first..last (0 to 3). Descriptive only; none of this ranks colleges.
     */
    private JComponent academicsBox(CollegeRecommendation r, int first, int last) {
        College c = r.college;
        Ui.Card box = new Ui.Card(new BorderLayout(), Ui.WHITE, Ui.LINE);
        box.setBorder(new EmptyBorder(12, 16, 12, 16));
        JPanel col = Ui.column(0);
        col.add(Ui.left(Ui.row(10, Ui.bubble("cap", Ui.ACADEMIC, Ui.BLUE_SOFT, 32), Ui.text(Messages.get("details.academics"), 15f, Font.BOLD, Ui.ACADEMIC))));
        JLabel status = Ui.status(r.academic.status, r.academic.label, 12.5f);
        status.setBorder(new EmptyBorder(4, 42, 2, 0));
        col.add(Ui.left(status));
        for (int part = first; part <= last; part++) {
            col.add(Ui.left(sectionTitle("details.part." + part)));
            switch (part) {
                case 0:
                    for (Fact f : r.program.facts) col.add(Ui.left(factRow(f)));
                    break;
                case 1:
                    for (Fact f : r.academic.facts) col.add(Ui.left(factRow(f)));
                    break;
                case 2:
                    col.add(Ui.left(factRow(new Fact(Messages.get("out.completion"),
                        c.completionRate == null ? Messages.get("notReported") : Math.round(c.completionRate * 100) + "%",
                        c.completionRate == null ? "" : Messages.get("out.completion.note", c.completionYears), null))));
                    col.add(Ui.left(factRow(new Fact(Messages.get("out.transfer"),
                        c.transferRate == null ? Messages.get("notReported") : Math.round(c.transferRate * 100) + "%",
                        c.transferRate == null ? "" : Messages.get(c.isTwoYear() ? "out.transfer.note.two" : "out.transfer.note.four", c.transferYears), null))));
                    break;
                default:
                    col.add(Ui.left(factRow(new Fact(Messages.get("res.ratio"),
                        c.studentFacultyRatio == null ? Messages.get("notReported") : Messages.get("res.ratio.value", c.studentFacultyRatio),
                        Messages.get("res.ratio.note"), null))));
                    col.add(Box.createVerticalStrut(6));
                    col.add(Ui.left(Ui.note(Messages.get("res.explore"), Ui.BLUE_PALE, Ui.INK, 300, 12f)));
            }
        }
        box.add(col, BorderLayout.NORTH);
        return box;
    }

    private static JComponent sectionTitle(String key) {
        JLabel l = Ui.text(Messages.get(key).toUpperCase(), 11f, Font.BOLD, Ui.MUTED);
        l.setBorder(new EmptyBorder(12, 0, 2, 0));
        return l;
    }

    private JComponent factBox(String icon, Color color, Color bg, String title, EvaluationResult r, Object extra) {
        Ui.Card box = new Ui.Card(new BorderLayout(), Ui.WHITE, Ui.LINE);
        box.setBorder(new EmptyBorder(12, 16, 12, 16));
        JPanel col = Ui.column(0);
        col.add(Ui.left(Ui.row(10, Ui.bubble(icon, color, bg, 32), Ui.text(title, 15f, Font.BOLD, color))));
        JLabel status = Ui.status(r.status, r.label, 12.5f);
        status.setBorder(new EmptyBorder(4, 42, 2, 0));
        col.add(Ui.left(status));
        col.add(Box.createVerticalStrut(4));
        if (extra instanceof List) for (Object f : (List<?>) extra) col.add(Ui.left(factRow((Fact) f)));
        for (Fact f : r.facts) col.add(Ui.left(factRow(f)));
        if (extra instanceof JComponent) { col.add(Box.createVerticalStrut(8)); col.add(Ui.left((JComponent) extra)); }
        box.add(col, BorderLayout.NORTH);
        return box;
    }

    /** One fact: a small grey label, then the value in bold with an optional note. Stacked so nothing overlaps. */
    private static JComponent factRow(Fact f) {
        String mark = f.match == null ? "" : f.match ? "<span style='color:#1f9d74'>&#10003;</span> " : "<span style='color:#b4475a'>&#10007;</span> ";
        String note = f.note.isEmpty() ? "" : "<br><span style='color:#687085'>" + Ui.escape(f.note) + "</span>";
        JLabel l = Ui.html("<span style='color:#687085'>" + Ui.escape(f.label) + "</span><br>" + mark + "<b>" + Ui.escape(f.value) + "</b>" + note,
            300, 12.5f, Ui.INK);
        l.setBorder(new javax.swing.border.CompoundBorder(new MatteBorder(0, 0, 1, 0, new Color(238, 240, 244)), new EmptyBorder(6, 0, 6, 0)));
        return l;
    }

    private JComponent whyVerify(CollegeRecommendation r, StudentProfile s, boolean wide) {
        StringBuilder why = new StringBuilder("<b style='font-size:13px'>" + Ui.escape(Messages.get("details.why")) + "</b><br>");
        List<String> reasons = r.reasons();
        if (reasons.isEmpty()) why.append(Ui.escape(Messages.get("details.why.none")));
        for (String x : reasons) why.append("&#10003; ").append(Ui.escape(x)).append("<br>");
        StringBuilder check = new StringBuilder("<b style='font-size:13px'>" + Ui.escape(Messages.get("details.verify")) + "</b><br>");
        for (String x : new NextStepGenerator().collegeSteps(s, r)) check.append("&#9744; ").append(Ui.escape(x)).append("<br>");
        int w = wide ? 470 : 380;
        Ui.Card a = new Ui.Card(new BorderLayout(), Ui.GREEN_SOFT, new Color(207, 234, 220));
        a.add(Ui.html(why.toString(), w, 12.5f, Ui.GREEN_TEXT));
        Ui.Card b = new Ui.Card(new BorderLayout(), Ui.AMBER_SOFT, new Color(243, 226, 191));
        b.add(Ui.html(check.toString(), w, 12.5f, Ui.AMBER_TEXT));
        if (!wide) { JPanel col = Ui.column(0); col.add(Ui.left(a)); col.add(Box.createVerticalStrut(10)); col.add(Ui.left(b)); return col; }
        return grid(2, a, b);
    }

    private JComponent npcButton() {
        if (blank(college.netPriceCalculatorUrl))
            return Ui.note(Messages.get("fin.next.noNpc"), Ui.AMBER_SOFT, Ui.AMBER_TEXT, 260, 12f);
        Ui.Button b = new Ui.Button(Messages.get("details.npc"), false).accent(Ui.GREEN);
        b.addActionListener(e -> open(college.netPriceCalculatorUrl));
        return b;
    }

    private void editExtras() {
        ExtraInfoPanel p = new ExtraInfoPanel(app.states);
        p.load(app.profile);
        int ok = JOptionPane.showConfirmDialog(this, p, Messages.get("details.addInfo"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;
        try {
            StudentProfile copy = app.profile.withPrograms(app.profile.getPrograms());
            p.apply(copy);
            app.updateExtras(copy);
            render();
        } catch (IllegalArgumentException ex) { app.error(ex); }
    }

    private void open(String url) {
        if (CollegeFitSwingApp.WEB) { CollegeFitSwingApp.showLink(this, url); return; } // browser tab: copy the link
        try {
            URI uri = URI.create(url.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme())) throw new IllegalArgumentException();
            Desktop.getDesktop().browse(uri);
        } catch (Exception ex) {
            CollegeFitSwingApp.showLink(this, url);
        }
    }

    private static boolean blank(String s) { return s == null || s.isBlank(); }

    static JPanel grid(int columns, JComponent... parts) {
        JPanel g = new JPanel(new GridLayout(1, columns, 12, 0));
        g.setOpaque(false);
        for (JComponent p : parts) {
            JPanel top = new JPanel(new BorderLayout());
            top.setOpaque(false);
            top.add(p, BorderLayout.NORTH); // boxes keep their natural height
            g.add(top);
        }
        return g;
    }
}

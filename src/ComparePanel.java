import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Step 5: Compare and decide. Up to three kept colleges side by side, rows in the order of her priorities,
 * markers only where a difference is large and measured the same way, her own reasons, and a short trade-off summary.
 */
class ComparePanel extends JPanel {
    static final String[] KEEP_REASONS = {"COSTS", "STUDY", "GRADUATION", "ADMISSION", "CAMPUS", "OTHER"};
    private final CollegeFitSwingApp app;
    private final JPanel body = new JPanel(new BorderLayout());

    ComparePanel(CollegeFitSwingApp app) {
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
        content.add(Ui.left(Ui.title(Messages.get("cmp.title"), 28)));
        content.add(Ui.left(Ui.text(Messages.get("cmp.subtitle"), 14f, Font.PLAIN, Ui.MUTED)));
        content.add(Box.createVerticalStrut(10));

        List<College> kept = app.shortlist.colleges();
        if (kept.size() < 2) {
            content.add(Ui.left(Ui.wrap(Messages.get(kept.isEmpty() ? "cmp.none" : "cmp.one"), 700, 14f, Ui.MUTED)));
        } else {
            JPanel strip = Ui.row(6, Ui.text(Messages.get("mine.pick"), 13f, Font.BOLD, Ui.INK));
            for (College c : kept) {
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
            content.add(Box.createVerticalStrut(10));
            List<CollegeRecommendation> recs = app.engine.recommendAll(s, new ArrayList<>(app.compare));
            Comparison cmp = new Comparison(s, recs);
            if (cmp.differentDegrees()) {
                content.add(Ui.left(Ui.note(Messages.get("cmp.degreeBanner"), Ui.AMBER_SOFT, Ui.AMBER_TEXT, 1000, 13f)));
                content.add(Box.createVerticalStrut(10));
            }
            content.add(Ui.left(table(s, recs, cmp)));
            content.add(Box.createVerticalStrut(12));
            JPanel bottom = new JPanel(new GridLayout(1, 2, 12, 0));
            bottom.setOpaque(false);
            Ui.Card sum = new Ui.Card(new BorderLayout(), Ui.BLUE_PALE, new Color(214, 222, 247));
            StringBuilder sb = new StringBuilder("<b style='font-size:13px'>" + Ui.escape(Messages.get("cmp.tradeoffs")) + "</b>");
            List<String> lines = cmp.summary();
            for (int i = 0; i < lines.size(); i++)
                sb.append("<br>").append(i == lines.size() - 1 ? "<span style='color:#687085'>" + Ui.escape(lines.get(i)) + "</span>" : "&#8226; " + Ui.escape(lines.get(i)));
            sum.add(Ui.html(sb.toString(), 540, 12.5f, Ui.INK));
            Ui.Card glance = new Ui.Card(new BorderLayout(), Ui.WHITE, Ui.LINE);
            glance.add(Ui.html("<b style='font-size:13px'>" + Ui.escape(Messages.get("cmp.glance")) + "</b><br>" + glance(s, recs)
                + "<br><span style='color:#687085'>" + Ui.escape(Messages.get("cmp.glance.note")) + "</span>", 440, 12.5f, Ui.INK));
            bottom.add(top(sum));
            bottom.add(top(glance));
            content.add(Ui.left(bottom));
        }

        Ui.Button back = new Ui.Button(Messages.get("cmp.back"), false);
        back.addActionListener(e -> app.backToOptions());
        Ui.Button next = new Ui.Button(Messages.get("cmp.next"), true);
        next.setIcon(Ui.icon("arrow", Ui.WHITE, 16));
        next.setHorizontalTextPosition(SwingConstants.LEFT);
        next.addActionListener(e -> app.showNextSteps());
        body.removeAll();
        body.add(CollegeFitSwingApp.page(content, Ui.text(Messages.get("cmp.footer"), 12.5f, Font.PLAIN, Ui.MUTED), back, next));
        body.revalidate();
        body.repaint();
    }

    private JComponent table(StudentProfile s, List<CollegeRecommendation> recs, Comparison cmp) {
        Ui.Card card = new Ui.Card(new GridBagLayout(), Ui.WHITE, Ui.LINE);
        card.setBorder(new EmptyBorder(6, 6, 6, 6));
        int row = 0;
        cell(card, row, 0, new JLabel(""), false);
        for (int i = 0; i < recs.size(); i++) {
            College c = recs.get(i).college;
            JPanel h = Ui.column(0);
            JLabel n = Ui.wrap(c.name, 270, 14.5f, Ui.INK);
            n.setFont(Ui.sans(Font.BOLD, 14.5f));
            h.add(Ui.left(n));
            h.add(Ui.left(Ui.text(c.city + ", " + c.state + ". " + Messages.get("group." + c.type().name()), 11.5f, Font.PLAIN, Ui.MUTED)));
            JButton d = Ui.link(Messages.get("card.detailsShort"));
            d.addActionListener(e -> app.showDetails(c, 4));
            h.add(Ui.left(d));
            cell(card, row, i + 1, h, false);
        }
        row++;
        for (Comparison.Row r : Comparison.rowOrder(s)) {
            int rank = rankOf(s, r);
            String tag = rank > 0 ? "<span style='background:#3b5bcc;color:#ffffff'>&nbsp;#" + rank + "&nbsp;</span>&nbsp; " : "";
            cell(card, row, 0, Ui.html(tag + "<b>" + Ui.escape(Messages.get("cmp.row." + r.name())) + "</b><br><span style='color:#687085'>"
                + Ui.escape(Messages.get("cmp.row." + r.name() + ".note")) + "</span>", 200, 12.5f, Ui.INK), true);
            for (int i = 0; i < recs.size(); i++) {
                CollegeRecommendation rec = recs.get(i);
                String marker = cmp.marker(rec, r);
                String chip = marker == null ? "" : "<br><span style='background:" + (warning(marker) ? "#fdf6e8;color:#8a5a12" : "#e9f6f0;color:#14684d")
                    + "'>&nbsp;" + Ui.escape(marker) + "&nbsp;</span>";
                cell(card, row, i + 1, Ui.html(value(s, rec, r) + chip, 270, 13f, Ui.INK), true);
            }
            row++;
        }
        // Her own reasons
        cell(card, row, 0, Ui.html("<b>" + Ui.escape(Messages.get("cmp.row.REASONS")) + "</b><br><span style='color:#687085'>"
            + Ui.escape(Messages.get("cmp.row.REASONS.note")) + "</span>", 200, 12.5f, Ui.INK), true);
        for (int i = 0; i < recs.size(); i++) {
            College c = recs.get(i).college;
            Set<String> mine = app.keepReasons.computeIfAbsent(c, k -> new LinkedHashSet<>());
            JPanel pills = new JPanel(new GridLayout(0, 2, 4, 4));
            pills.setOpaque(false);
            for (String k : KEEP_REASONS) {
                Ui.Pill p = new Ui.Pill(Messages.get("keepReason." + k));
                p.setFont(Ui.sans(Font.PLAIN, 11.5f));
                p.setSelected(mine.contains(k));
                p.addActionListener(e -> { if (p.isSelected()) mine.add(k); else mine.remove(k); });
                pills.add(p);
            }
            cell(card, row, i + 1, pills, true);
        }
        return card;
    }

    private static boolean warning(String marker) {
        return marker.equals(Messages.get("cmp.mark.notComparable")) || marker.equals(Messages.get("cmp.mark.differentPath"))
            || marker.startsWith(Messages.get("cmp.mark.measurePrefix"));
    }

    private static int rankOf(StudentProfile s, Comparison.Row r) {
        for (int i = 0; i < s.getPriorities().size(); i++) if (s.getPriorities().get(i).name().equals(r.name())) return i + 1;
        return 0;
    }

    private static String value(StudentProfile s, CollegeRecommendation r, Comparison.Row row) {
        College c = r.college;
        switch (row) {
            case DEGREE: return "<b>" + Ui.escape(cap(Comparison.degreeText(s, r))) + "</b>";
            case COSTS: {
                Integer v = RecommendationEngine.comparableCost(s, c);
                String pub = Ui.escape(Messages.get("cmp.costBefore", Messages.money(c.costOfAttendance)));
                return (v == null ? Ui.escape(Messages.get("notReported")) : "<b>" + Messages.money(v) + " / yr</b>") + "<br><span style='color:#687085'>" + pub + "</span>";
            }
            case GRADUATION:
                return c.completionRate == null ? Ui.escape(Messages.get("notReported"))
                    : "<b>" + Math.round(c.completionRate * 100) + "%</b> " + Ui.escape(Messages.get("card.completion.measure", c.completionYears));
            case ADMISSION:
                return "<b>" + Ui.escape(Boolean.TRUE.equals(c.openAdmission) ? Messages.get("ac.open")
                    : c.admissionRate != null && c.admissionRate > 0 ? Messages.get("cmp.admission.rate", Math.round(c.admissionRate * 100) + "%")
                    : Messages.get("notReported")) + "</b>";
            case STUDY: return Ui.escape(r.programLine(s));
            default: return r.socialHtml();
        }
    }

    private static String cap(String s) { return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1); }

    /** Plain description of the list: college types, degree levels and admission policies (not her chances). */
    private static String glance(StudentProfile s, List<CollegeRecommendation> recs) {
        Map<String, Integer> types = new LinkedHashMap<>(), degrees = new LinkedHashMap<>();
        int open = 0, rated = 0;
        for (CollegeRecommendation r : recs) {
            types.merge(Messages.get("group." + r.college.type().name()).toLowerCase(), 1, Integer::sum);
            degrees.merge(Comparison.degreeText(s, r), 1, Integer::sum);
            if (Boolean.TRUE.equals(r.college.openAdmission)) open++; else if (r.college.admissionRate != null) rated++;
        }
        return Ui.escape(Messages.get("cmp.glance.types", join(types))) + "<br>" + Ui.escape(Messages.get("cmp.glance.degrees", join(degrees)))
            + "<br>" + Ui.escape(Messages.get("cmp.glance.admission", open, rated));
    }

    private static String join(Map<String, Integer> m) {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, Integer> e : m.entrySet()) out.add(e.getValue() + " " + e.getKey());
        return String.join(", ", out);
    }

    private static void cell(JPanel grid, int row, int col, JComponent c, boolean line) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = col; g.gridy = row; g.anchor = GridBagConstraints.NORTHWEST; g.fill = GridBagConstraints.BOTH;
        g.weightx = col == 0 ? 0 : 1;
        c.setBorder(new javax.swing.border.CompoundBorder(line ? new MatteBorder(1, 0, 0, 0, new Color(238, 240, 244)) : new EmptyBorder(0, 0, 0, 0),
            new EmptyBorder(8, 10, 8, 10)));
        grid.add(c, g);
    }

    private static JComponent top(JComponent c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(c, BorderLayout.NORTH);
        return p;
    }
}

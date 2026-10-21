import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * One college on the Explore page. Kept short on purpose: the program, the student's top two priorities,
 * and Keep / Not for me. Everything else is on the details page.
 */
class OptionCard extends Ui.Card {
    static final String[] REASONS = {"COST", "FAR", "ADMISSION", "PROGRAM", "SIZE", "OTHER"};

    OptionCard(CollegeFitSwingApp app, StudentProfile s, CollegeRecommendation r, Runnable changed) {
        super(new BorderLayout(), Ui.WHITE, Ui.LINE);
        College c = r.college;
        boolean kept = app.isKept(c);
        if (kept) style(Ui.WHITE, Ui.GREEN, 2f);
        setBorder(new EmptyBorder(12, 14, 12, 14));
        JPanel col = Ui.column(0);
        JLabel name = Ui.wrap(c.name, 330, 15f, Ui.INK);
        name.setFont(Ui.sans(Font.BOLD, 15f));
        col.add(Ui.left(name));
        col.add(Box.createVerticalStrut(3));
        col.add(Ui.left(Ui.row(6, Ui.text(c.city + ", " + c.state, 12f, Font.PLAIN, Ui.MUTED),
            Ui.tag(Messages.get(c.isTwoYear() ? "tag.two" : "tag.four")))));
        col.add(Box.createVerticalStrut(8));
        List<StudentProfile.Priority> shown = topTwo(s);
        if (!shown.contains(StudentProfile.Priority.STUDY))
            col.add(Ui.left(line("", Messages.get("lens.program"), Ui.INK, Ui.escape(r.programLine(s)))));
        for (int i = 0; i < shown.size(); i++) {
            String tag = s.hasPriorities() ? "#" + (i + 1) : "";
            col.add(Ui.left(priorityLine(shown.get(i), tag, s, r)));
        }
        col.add(Box.createVerticalStrut(9));

        Ui.Button keep = new Ui.Button(kept ? "\u2713 " + Messages.get("keep.kept") : Messages.get("keep.keep"), false).small();
        if (kept) keep.accent(Ui.GREEN);
        keep.addActionListener(e -> { if (app.isKept(c)) app.unkeep(c); else app.keep(c); changed.run(); });
        Ui.Button no = new Ui.Button(Messages.get("keep.not"), false).small();
        no.setForeground(Ui.MUTED);
        no.addActionListener(e -> {
            String reason = askReason(this, c.name, "");
            if (reason != null) { app.notForMe(c, reason); changed.run(); }
        });
        JButton details = Ui.link(Messages.get("card.detailsShort"));
        details.addActionListener(e -> app.showDetails(c, 3));
        JPanel buttons = new JPanel(new BorderLayout());
        buttons.setOpaque(false);
        buttons.add(Ui.row(8, keep, no), BorderLayout.WEST);
        buttons.add(details, BorderLayout.EAST);
        col.add(Ui.left(buttons));
        add(col);
    }

    /** Her first two priorities; without priorities, cost and admission give a quick picture. */
    static List<StudentProfile.Priority> topTwo(StudentProfile s) {
        if (!s.hasPriorities()) return List.of(StudentProfile.Priority.COSTS, StudentProfile.Priority.ADMISSION);
        return s.getPriorities().subList(0, Math.min(2, s.getPriorities().size()));
    }

    static JComponent priorityLine(StudentProfile.Priority p, String tag, StudentProfile s, CollegeRecommendation r) {
        College c = r.college;
        String value;
        switch (p) {
            case COSTS: {
                Integer cost = RecommendationEngine.comparableCost(s, c);
                boolean oos = c.isPublic() && s.hasHomeState() && !s.getHomeState().equals(c.state);
                value = cost != null ? "<b>" + Messages.money(cost) + "/yr*</b>"
                    : Ui.escape(Messages.get(oos ? "card.cost.oosNotComparable" : "card.cost.notReported"));
                break;
            }
            case GRADUATION:
                value = c.completionRate == null ? Ui.escape(Messages.get("notReported"))
                    : "<b>" + Math.round(c.completionRate * 100) + "%</b> " + Ui.escape(Messages.get("card.completion.measure", c.completionYears));
                break;
            case ADMISSION:
                value = Ui.escape(Boolean.TRUE.equals(c.openAdmission) ? Messages.get("ac.open")
                    : c.admissionRate != null && c.admissionRate > 0 ? Messages.get("card.admission.rate", Math.round(c.admissionRate * 100) + "%")
                    : Messages.get("notReported"));
                break;
            case STUDY:
                value = Ui.escape(r.programLine(s));
                break;
            default:
                value = r.socialHtml();
        }
        Color color = p == StudentProfile.Priority.COSTS ? Ui.FINANCIAL : p == StudentProfile.Priority.GRADUATION ? Ui.PURPLE
            : p == StudentProfile.Priority.CAMPUS ? Ui.SOCIAL : Ui.ACADEMIC;
        return line(tag, Messages.get("card.label." + p.name()), color, value);
    }

    static JComponent line(String tag, String label, Color color, String html) {
        String t = tag.isEmpty() ? "" : "<span style='background:#3b5bcc;color:#ffffff'>&nbsp;" + tag + "&nbsp;</span>&nbsp; ";
        JLabel l = new JLabel("<html><div style='width:" + Ui.css(335) + "px'>" + t + "<b style='color:" + Ui.hex(color) + "'>"
            + Ui.escape(label) + "</b>&nbsp;&nbsp;" + html + "</div></html>");
        l.setFont(Ui.sans(Font.PLAIN, 12.5f));
        l.setForeground(Ui.INK);
        l.setBorder(new EmptyBorder(2, 0, 2, 0));
        return l;
    }

    /** Asks for an optional reason. Returns the reason key ("" for none), or null if she cancels. */
    static String askReason(Component parent, String college, String current) {
        JPanel p = Ui.column(0);
        p.add(Ui.left(Ui.text(Messages.get("not.question"), 13f, Font.PLAIN, Ui.MUTED)));
        p.add(Box.createVerticalStrut(8));
        ButtonGroup g = new ButtonGroup();
        Ui.Pill[] pills = new Ui.Pill[REASONS.length];
        for (int i = 0; i < REASONS.length; i++) {
            pills[i] = new Ui.Pill(Messages.get("reason." + REASONS[i]));
            pills[i].setSelected(REASONS[i].equals(current));
            g.add(pills[i]);
            p.add(Ui.left(pills[i]));
            p.add(Box.createVerticalStrut(5));
        }
        String[] options = {Messages.get("not.save"), Messages.get("not.cancel")};
        int ok = JOptionPane.showOptionDialog(parent, p, Messages.get("not.title", college), JOptionPane.DEFAULT_OPTION,
            JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (ok != 0) return null;
        for (int i = 0; i < REASONS.length; i++) if (pills[i].isSelected()) return REASONS[i];
        return "";
    }
}

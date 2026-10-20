import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** One starting option: four data lines, a data-based reason, and Details / Save buttons. */
class OptionCard extends Ui.Card {
    OptionCard(CollegeFitSwingApp app, StudentProfile s, CollegeRecommendation r, Runnable onSave) {
        super(new BorderLayout(), Ui.WHITE, Ui.LINE);
        setBorder(new EmptyBorder(12, 14, 12, 14));
        College c = r.college;
        JPanel col = Ui.column(0);
        col.add(Ui.left(Ui.wrap(c.name, 330, 15f, Ui.INK)));
        ((JLabel) col.getComponent(0)).setFont(Ui.sans(Font.BOLD, 15f));
        col.add(Box.createVerticalStrut(3));
        col.add(Ui.left(Ui.row(6, Ui.text(c.city + ", " + c.state, 12f, Font.PLAIN, Ui.MUTED),
            Ui.tag(Messages.get(c.isPublic() ? "type.public" : "type.private")))));
        col.add(Box.createVerticalStrut(8));
        col.add(Ui.left(line(Messages.get("lens.program"), Ui.INK, Ui.escape(r.programLine(s)))));
        String aca = "<span style='color:" + (r.academic.status == FitStatus.NEEDS_VERIFICATION ? "#b7791f" : "#1e2638") + "'>"
            + Ui.escape(r.academic.label) + "</span>";
        col.add(Ui.left(line(Messages.get("lens.academic"), Ui.ACADEMIC, aca)));
        col.add(Ui.left(line(Messages.get("lens.financial"), Ui.FINANCIAL, Ui.escape(r.financialLine(s)))));
        col.add(Ui.left(line(Messages.get("lens.social"), Ui.SOCIAL, r.socialHtml())));
        if (r.nursingBachelor) { col.add(Box.createVerticalStrut(6)); col.add(Ui.left(Ui.note(Messages.get("prog.next.license"), Ui.AMBER_SOFT, Ui.AMBER_TEXT, 325, 11.5f))); }
        col.add(Box.createVerticalStrut(9));
        Ui.Button details = new Ui.Button(Messages.get("card.details"), false).small();
        details.addActionListener(e -> app.showDetails(c));
        boolean saved = app.shortlist.contains(c);
        Ui.Button save = new Ui.Button(Messages.get(saved ? "list.saved" : "list.save"), false).small();
        save.setIcon(Ui.icon(saved ? "heartFull" : "heart", saved ? Ui.RED : Ui.INK, 14));
        save.addActionListener(e -> { app.toggleSaved(c); onSave.run(); });
        col.add(Ui.left(Ui.row(8, details, save)));
        add(col);
    }

    private static JComponent line(String lens, Color color, String html) {
        JLabel l = new JLabel("<html><div style='width:" + Ui.css(335) + "px'><b style='color:" + Ui.hex(color) + "'>" + Ui.escape(lens)
            + "</b>&nbsp;&nbsp;" + html + "</div></html>");
        l.setFont(Ui.sans(Font.PLAIN, 12.5f));
        l.setForeground(Ui.INK);
        l.setBorder(new EmptyBorder(2, 0, 2, 0));
        return l;
    }
}

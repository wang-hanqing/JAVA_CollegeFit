import javax.swing.*;
import java.awt.*;

/** One result card: name, program evidence, a status for each lens, Save and details buttons. */
public class CollegeCardPanel extends JPanel {
    public CollegeCardPanel(CollegeRecommendation rec, Runnable showDetails, Shortlist list, Runnable onListChange) {
        super(new BorderLayout(8, 10));
        setBackground(Color.WHITE);
        setBorder(Ui.card());

        JPanel top = new JPanel(new GridLayout(0, 1, 0, 2));
        top.setOpaque(false);
        JLabel name = new JLabel("<html><div style='width:250px'><b>" + Ui.escape(rec.college.name) + "</b></div></html>");
        name.setFont(Ui.font(name, Font.PLAIN, 16f));
        top.add(name);
        top.add(Ui.muted(rec.college.city + ", " + rec.college.state + "   " + Messages.get(rec.college.isPublic() ? "type.public" : "type.private")));
        JToggleButton save = new JToggleButton();
        save.setSelected(list.contains(rec.college));
        save.setText(Messages.get(save.isSelected() ? "list.saved" : "list.save"));
        save.addActionListener(e -> {
            if (save.isSelected()) list.add(rec.college); else list.remove(rec.college);
            save.setText(Messages.get(save.isSelected() ? "list.saved" : "list.save"));
            onListChange.run();
        });
        JPanel head = new JPanel(new BorderLayout(8, 0));
        head.setOpaque(false);
        head.add(top, BorderLayout.CENTER);
        JPanel saveBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        saveBox.setOpaque(false);
        saveBox.add(save);
        head.add(saveBox, BorderLayout.EAST);
        add(head, BorderLayout.NORTH);

        JPanel rows = new JPanel(new GridLayout(0, 1, 0, 6));
        rows.setOpaque(false);
        rows.add(line(Messages.get("lens.program"), Ui.INK, rec.program));
        rows.add(line(Messages.get("lens.academic"), Ui.ACADEMIC, rec.academic));
        rows.add(line(Messages.get("lens.financial"), Ui.FINANCIAL, rec.financial));
        rows.add(line(Messages.get("lens.social"), Ui.SOCIAL, rec.social));
        add(rows, BorderLayout.CENTER);

        JButton details = new JButton(Messages.get("card.details") + "  \u2192");
        details.addActionListener(e -> showDetails.run());
        add(details, BorderLayout.SOUTH);
    }

    private static JPanel line(String lens, Color color, EvaluationResult r) {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setOpaque(false);
        JLabel l = new JLabel(lens);
        l.setForeground(color);
        l.setFont(Ui.font(l, Font.BOLD, 13f));
        p.add(l, BorderLayout.WEST);
        p.add(Ui.dot(r.status, r.label), BorderLayout.EAST);
        return p;
    }
}

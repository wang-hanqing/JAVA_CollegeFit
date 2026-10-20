import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.List;

/** Side-by-side view of 2 or 3 saved colleges: program, academic, financial, social and what to verify. */
public class CompareDialog extends JDialog {
    public CompareDialog(Component parent, StudentProfile s, List<CollegeRecommendation> recs) {
        super(SwingUtilities.getWindowAncestor(parent), Messages.get("compare.title"), ModalityType.APPLICATION_MODAL);
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setBackground(Color.WHITE);
        grid.setBorder(new EmptyBorder(16, 18, 16, 18));
        NextStepGenerator steps = new NextStepGenerator();

        int col = 1;
        for (CollegeRecommendation r : recs) cell(grid, 0, col++, "<b style='font-size:14px'>" + Ui.escape(r.college.name) + "</b>", null);

        String[] rows = { "compare.place", "lens.program", "lens.academic", "lens.financial", "lens.social", "compare.verify" };
        Color[] colors = { Ui.MUTED, Ui.INK, Ui.ACADEMIC, Ui.FINANCIAL, Ui.SOCIAL, Ui.MUTED };
        for (int i = 0; i < rows.length; i++) {
            JLabel head = new JLabel(Messages.get(rows[i]));
            head.setForeground(colors[i]);
            head.setFont(Ui.font(head, Font.BOLD, 14f));
            GridBagConstraints g = constraints(i + 1, 0);
            grid.add(head, g);
            col = 1;
            for (CollegeRecommendation r : recs) {
                String html;
                switch (i) {
                    case 0: html = Ui.escape(r.college.city + ", " + r.college.state) + "<br>"
                        + Messages.get(r.college.isPublic() ? "type.public" : "type.private"); break;
                    case 1: html = lens(r.program); break;
                    case 2: html = lens(r.academic); break;
                    case 3: html = lens(r.financial); break;
                    case 4: html = lens(r.social); break;
                    default:
                        StringBuilder sb = new StringBuilder();
                        for (String step : steps.collegeSteps(s, r)) sb.append("&#9744; ").append(Ui.escape(step)).append("<br>");
                        html = "<span style='font-size:11px'>" + sb + "</span>";
                }
                cell(grid, i + 1, col++, html, null);
            }
        }
        JScrollPane scroll = new JScrollPane(grid);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        setContentPane(scroll);
        setSize(Math.min(1180, 260 + 300 * recs.size()), 720);
        setLocationRelativeTo(parent);
    }

    /** Status label in color, then the first three facts. */
    private static String lens(EvaluationResult r) {
        StringBuilder sb = new StringBuilder("<b style='color:" + Ui.hex(Ui.statusColor(r.status)) + "'>&#9679; "
            + Ui.escape(r.label) + "</b>");
        int n = 0;
        for (Fact f : r.facts) {
            if (n++ == 4) break;
            sb.append("<br><span style='color:#687085'>").append(Ui.escape(f.label)).append(":</span> ").append(Ui.escape(f.value));
        }
        return sb.toString();
    }

    private static void cell(JPanel grid, int row, int col, String html, Color bg) {
        JLabel l = new JLabel("<html><div style='width:250px'>" + html + "</div></html>");
        l.setVerticalAlignment(SwingConstants.TOP);
        l.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, Ui.LINE), new EmptyBorder(8, 6, 8, 6)));
        grid.add(l, constraints(row, col));
    }

    private static GridBagConstraints constraints(int row, int col) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = col; g.gridy = row; g.anchor = GridBagConstraints.NORTHWEST;
        g.fill = GridBagConstraints.BOTH; g.insets = new Insets(0, 0, 0, 10);
        return g;
    }
}

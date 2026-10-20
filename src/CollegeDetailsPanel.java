import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.net.URI;
import java.util.List;

/**
 * Details page, following the design document:
 * a header, "Why explore" and "What to verify" boxes, then itemized Program, Academic, Financial
 * and Social sections, and the official links.
 */
public class CollegeDetailsPanel extends JPanel {
    private static final int TEXT_WIDTH = 500;

    public CollegeDetailsPanel(StudentProfile student, CollegeRecommendation rec) {
        super(new BorderLayout());
        College c = rec.college;
        JPanel body = new ScrollablePanel();
        body.setBorder(new EmptyBorder(18, 22, 18, 22));
        body.setBackground(Ui.BG);

        // Header
        body.add(left(Ui.title(c.name, 24)));
        String size = c.undergraduates == null ? "" : ", " + Messages.get("details.undergrads", Messages.number(c.undergraduates));
        JLabel sub = Ui.muted(c.city + ", " + c.state + "   " + Messages.get(c.isPublic() ? "type.public" : "type.private") + size);
        sub.setFont(Ui.font(sub, Font.PLAIN, 14f));
        body.add(left(sub));
        body.add(Box.createVerticalStrut(14));

        // Why explore + What to verify
        JPanel summary = new JPanel(new GridLayout(1, 2, 12, 0));
        summary.setOpaque(false);
        summary.setAlignmentX(LEFT_ALIGNMENT);
        JPanel why = box(Messages.get("details.why"), Ui.GOOD_BG);
        List<String> reasons = rec.reasons();
        if (reasons.isEmpty()) why.add(left(text(Messages.get("details.why.none"), 250, Ui.MUTED, 13f)));
        for (String r : reasons) why.add(left(text("\u2713  " + r, 250, Ui.INK, 13.5f)));
        JPanel verify = box(Messages.get("details.verify"), Ui.CHECK_BG);
        for (String step : new NextStepGenerator().collegeSteps(student, rec)) {
            JCheckBox cb = new JCheckBox("<html><div style='width:230px'>" + Ui.escape(step) + "</div></html>");
            cb.setFont(Ui.font(cb, Font.PLAIN, 13f));
            cb.setOpaque(false);
            verify.add(left(cb));
        }
        summary.add(why);
        summary.add(verify);
        body.add(summary);
        body.add(Box.createVerticalStrut(14));

        // Itemized lens sections
        body.add(section(Messages.get("lens.program"), Ui.INK, rec.program));
        body.add(section(Messages.get("lens.academic"), Ui.ACADEMIC, rec.academic));
        body.add(section(Messages.get("lens.financial"), Ui.FINANCIAL, rec.financial));
        body.add(section(Messages.get("lens.social"), Ui.SOCIAL, rec.social));

        // Links
        body.add(Box.createVerticalStrut(6));
        body.add(left(links(c)));
        body.add(Box.createVerticalStrut(8));
        body.add(left(text(Messages.get("details.disclaimer"), TEXT_WIDTH, Ui.MUTED, 12f)));

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        add(scroll, BorderLayout.CENTER);
    }

    /** A lens card: colored title with its status, then one row per fact. */
    private static JPanel section(String lens, Color color, EvaluationResult r) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(new CompoundBorder(new EmptyBorder(0, 0, 10, 0), new LineBorder(Ui.LINE, 1, true)),
            new EmptyBorder(12, 14, 10, 14)));
        card.setAlignmentX(LEFT_ALIGNMENT);

        JPanel title = new JPanel(new BorderLayout());
        title.setOpaque(false);
        JLabel name = new JLabel(lens);
        name.setForeground(color);
        name.setFont(Ui.font(name, Font.BOLD, 16f));
        title.add(name, BorderLayout.WEST);
        JLabel status = Ui.dot(r.status, r.label);
        status.setFont(Ui.font(status, Font.BOLD, 13.5f));
        title.add(status, BorderLayout.EAST);
        title.setAlignmentX(LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(8));

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        grid.setAlignmentX(LEFT_ALIGNMENT);
        int row = 0;
        for (Fact f : r.facts) {
            GridBagConstraints g = new GridBagConstraints();
            g.gridy = row++; g.anchor = GridBagConstraints.NORTHWEST; g.insets = new Insets(4, 0, 4, 12);
            g.gridx = 0;
            JLabel label = new JLabel("<html><div style='width:170px'>" + Ui.escape(f.label) + "</div></html>");
            label.setForeground(Ui.MUTED);
            label.setFont(Ui.font(label, Font.PLAIN, 13f));
            grid.add(label, g);
            g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
            String mark = f.match == null ? "" : f.match
                ? "<span style='color:#1f9d74'>&#10003;</span> " : "<span style='color:#b4475a'>&#10007;</span> ";
            String note = f.note.isEmpty() ? "" : "<br><span style='color:#687085;font-size:11px'>" + Ui.escape(f.note) + "</span>";
            JLabel value = new JLabel("<html><div style='width:340px'>" + mark + "<b>" + Ui.escape(f.value) + "</b>" + note + "</div></html>");
            value.setFont(Ui.font(value, Font.PLAIN, 13.5f));
            grid.add(value, g);
        }
        card.add(grid);
        return card;
    }

    private static JPanel box(String title, Color bg) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(bg);
        p.setBorder(new EmptyBorder(12, 14, 12, 14));
        JLabel t = new JLabel(title);
        t.setFont(Ui.font(t, Font.BOLD, 15f));
        p.add(left(t));
        p.add(Box.createVerticalStrut(6));
        return p;
    }

    private JPanel links(College c) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setOpaque(false);
        p.add(linkButton(Messages.get("details.openSite"), c.schoolUrl));
        if (c.netPriceCalculatorUrl == null || c.netPriceCalculatorUrl.isBlank()) {
            p.add(Ui.muted(Messages.get("details.noNpc")));
        } else {
            p.add(linkButton(Messages.get("details.openNpc"), c.netPriceCalculatorUrl));
        }
        JButton copy = new JButton(Messages.get("details.copy"));
        copy.addActionListener(e -> {
            String text = c.schoolUrl + (c.netPriceCalculatorUrl == null || c.netPriceCalculatorUrl.isBlank() ? "" : "\n" + c.netPriceCalculatorUrl);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
            copy.setText(Messages.get("details.copied"));
        });
        p.add(copy);
        return p;
    }

    private JButton linkButton(String text, String url) {
        JButton b = new JButton(text);
        b.setEnabled(url != null && !url.isBlank());
        b.addActionListener(e -> {
            try {
                URI uri = URI.create(url.trim());
                if (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme()))
                    throw new IllegalArgumentException("Only web links are supported.");
                Desktop.getDesktop().browse(uri);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, Messages.get("details.openFailed") + "\n" + url, text, JOptionPane.WARNING_MESSAGE);
            }
        });
        return b;
    }

    private static JLabel text(String s, int width, Color color, float size) {
        JLabel l = new JLabel("<html><div style='width:" + width + "px'>" + Ui.escape(s) + "</div></html>");
        l.setForeground(color);
        l.setFont(Ui.font(l, Font.PLAIN, size));
        return l;
    }

    private static <T extends JComponent> T left(T c) { c.setAlignmentX(LEFT_ALIGNMENT); return c; }

    public static void show(Component parent, StudentProfile student, CollegeRecommendation rec) {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(parent), Messages.get("details.title"),
            Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setContentPane(new CollegeDetailsPanel(student, rec));
        dlg.setSize(780, 820);
        dlg.setLocationRelativeTo(parent);
        dlg.setVisible(true);
    }
}

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.net.URI;

/** Details dialog: the reason behind each status, what to check next, and official links. */
public class CollegeDetailsPanel extends JPanel {
    public CollegeDetailsPanel(StudentProfile student, CollegeRecommendation rec) {
        super(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(16, 18, 16, 18));
        JPanel head = new JPanel(new GridLayout(0, 1, 0, 2));
        head.add(Ui.title(rec.college.name, 22));
        head.add(Ui.muted(rec.college.city + ", " + rec.college.state));
        add(head, BorderLayout.NORTH);

        JPanel body = new ScrollablePanel();
        block(body, Messages.get("lens.program"), Ui.INK, rec.program);
        block(body, Messages.get("lens.academic"), Ui.ACADEMIC, rec.academic);
        block(body, Messages.get("lens.financial"), Ui.FINANCIAL, rec.financial);
        block(body, Messages.get("lens.social"), Ui.SOCIAL, rec.social);

        JLabel sub = new JLabel(Messages.get("details.checkNext"));
        sub.setFont(sub.getFont().deriveFont(Font.BOLD, 16f));
        sub.setAlignmentX(LEFT_ALIGNMENT);
        body.add(Box.createVerticalStrut(10));
        body.add(sub);
        for (String step : new NextStepGenerator().collegeSteps(student, rec)) {
            JCheckBox cb = new JCheckBox("<html><div style='width:540px'>" + Ui.escape(step) + "</div></html>");
            cb.setAlignmentX(LEFT_ALIGNMENT);
            body.add(cb);
        }
        link(body, Messages.get("details.website"), rec.college.schoolUrl);
        link(body, Messages.get("details.npc"), rec.college.netPriceCalculatorUrl);
        JLabel disclaimer = Ui.muted(Messages.get("details.disclaimer"));
        disclaimer.setAlignmentX(LEFT_ALIGNMENT);
        body.add(Box.createVerticalStrut(8));
        body.add(disclaimer);

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    private static void block(JPanel panel, String lens, Color color, EvaluationResult r) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setBorder(new EmptyBorder(8, 0, 8, 0));
        JPanel title = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JLabel name = new JLabel(lens + "   ");
        name.setForeground(color);
        name.setFont(name.getFont().deriveFont(Font.BOLD));
        title.add(name);
        title.add(Ui.dot(r.status, r.label + "  (" + Messages.get("status." + r.status.name()) + ")"));
        p.add(title, BorderLayout.NORTH);
        p.add(Ui.wrap(r.explanation, 560), BorderLayout.CENTER);
        panel.add(p);
    }

    private void link(JPanel panel, String title, String url) {
        if (url == null || url.isBlank()) return;
        JPanel section = new JPanel(new BorderLayout(0, 4));
        section.setBorder(new EmptyBorder(10, 0, 4, 0));
        section.setAlignmentX(LEFT_ALIGNMENT);
        JLabel heading = new JLabel(title);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD));
        section.add(heading, BorderLayout.NORTH);
        JTextField address = new JTextField(url);
        address.setEditable(false);
        section.add(address, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton open = new JButton(Messages.get("details.open"));
        JButton copy = new JButton(Messages.get("details.copy"));
        open.addActionListener(e -> {
            try {
                URI uri = URI.create(url.trim());
                if (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme()))
                    throw new IllegalArgumentException("Only web links are supported.");
                Desktop.getDesktop().browse(uri);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, Messages.get("details.openFailed") + "\n" + ex.getMessage(),
                    title, JOptionPane.WARNING_MESSAGE);
            }
        });
        copy.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(url), null);
            copy.setText(Messages.get("details.copied"));
        });
        actions.add(open);
        actions.add(copy);
        section.add(actions, BorderLayout.SOUTH);
        panel.add(section);
    }

    public static void show(Component parent, StudentProfile student, CollegeRecommendation rec) {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(parent), Messages.get("details.title"),
            Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setContentPane(new CollegeDetailsPanel(student, rec));
        dlg.setSize(720, 700);
        dlg.setLocationRelativeTo(parent);
        dlg.setVisible(true);
    }
}

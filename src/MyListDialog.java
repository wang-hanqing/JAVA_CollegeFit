import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.util.List;

/** Shows the saved colleges with their current statuses, and exports them to CSV. */
public class MyListDialog extends JDialog {
    private final Shortlist list;
    private final StudentProfile student;
    private final RecommendationEngine engine;
    private final Runnable onChange;
    private final JPanel rows = new ScrollablePanel();
    private final java.util.Map<College, JCheckBox> compareBoxes = new java.util.LinkedHashMap<>();

    public MyListDialog(Component parent, Shortlist list, StudentProfile student, RecommendationEngine engine, Runnable onChange) {
        super(SwingUtilities.getWindowAncestor(parent), Messages.get("list.title"), ModalityType.APPLICATION_MODAL);
        this.list = list; this.student = student; this.engine = engine; this.onChange = onChange;
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(new EmptyBorder(16, 18, 16, 18));
        content.add(Ui.title(Messages.get("list.title"), 22), BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(rows);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        content.add(scroll, BorderLayout.CENTER);

        JButton export = new JButton(Messages.get("list.export"));
        export.addActionListener(e -> export());
        JButton compare = new JButton(Messages.get("list.compare"));
        compare.addActionListener(e -> compare());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(compare);
        buttons.add(export);
        JPanel footer = new JPanel(new BorderLayout());
        footer.add(Ui.muted(Messages.get("list.privacy")), BorderLayout.WEST);
        footer.add(buttons, BorderLayout.EAST);
        content.add(footer, BorderLayout.SOUTH);
        setContentPane(content);
        refresh();
        setSize(760, 560);
        setLocationRelativeTo(parent);
    }

    private void refresh() {
        rows.removeAll();
        compareBoxes.clear();
        List<CollegeRecommendation> recs = list.evaluate(student, engine);
        if (recs.isEmpty()) rows.add(Ui.muted(Messages.get("list.empty")));
        for (CollegeRecommendation r : recs) {
            JPanel row = new JPanel(new BorderLayout(10, 4));
            row.setBorder(new EmptyBorder(8, 4, 8, 4));
            row.setAlignmentX(LEFT_ALIGNMENT);
            row.add(new JLabel("<html><b>" + Ui.escape(r.college.name) + "</b> &nbsp;" + Ui.escape(r.college.city + ", " + r.college.state)
                + "<br>" + Ui.escape(r.program.label) + " &nbsp;|&nbsp; " + Ui.escape(r.academic.label)
                + " &nbsp;|&nbsp; " + Ui.escape(r.financial.label) + " &nbsp;|&nbsp; " + Ui.escape(r.social.label) + "</html>"),
                BorderLayout.CENTER);
            JCheckBox pick = new JCheckBox(Messages.get("list.compare.pick"));
            compareBoxes.put(r.college, pick);
            row.add(pick, BorderLayout.WEST);
            JButton remove = new JButton(Messages.get("list.remove"));
            remove.addActionListener(e -> { list.remove(r.college); onChange.run(); refresh(); });
            row.add(remove, BorderLayout.EAST);
            rows.add(row);
            rows.add(new JSeparator());
        }
        rows.revalidate();
        rows.repaint();
    }

    /** Opens the side-by-side view for the 2 or 3 colleges she ticked. */
    private void compare() {
        java.util.List<College> picked = new java.util.ArrayList<>();
        for (java.util.Map.Entry<College, JCheckBox> e : compareBoxes.entrySet()) if (e.getValue().isSelected()) picked.add(e.getKey());
        if (picked.size() < 2 || picked.size() > 3) {
            JOptionPane.showMessageDialog(this, Messages.get("list.compare.need"));
            return;
        }
        new CompareDialog(this, student, engine.recommendAll(student, picked)).setVisible(true);
    }

    private void export() {
        if (list.size() == 0) {
            JOptionPane.showMessageDialog(this, Messages.get("list.empty"));
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this, "<html><div style='width:360px'>" + Ui.escape(Messages.get("list.privacy.confirm"))
            + "</div></html>", Messages.get("list.export"), JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(System.getProperty("user.home"), "CollegeFit_my_list_" + LocalDate.now() + ".csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) file = new File(file.getParentFile(), file.getName() + ".csv");
        try {
            list.exportCsv(file.toPath(), student, engine);
            JOptionPane.showMessageDialog(this, Messages.get("list.exported", file.getAbsolutePath()));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, Messages.get("list.exportFailed") + "\n" + ex.getMessage(),
                Messages.get("list.title"), JOptionPane.WARNING_MESSAGE);
        }
    }
}

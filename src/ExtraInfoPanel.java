import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Optional answers that only explain colleges (never filter them): SAT, home state, budget, income, FAFSA.
 * Used folded on the direction page and in the "Add scores or cost details" dialog.
 * Score and budget boxes get their own line and appear only when needed, so they are never squeezed out of view.
 */
class ExtraInfoPanel extends JPanel {
    private final JComboBox<StudentProfile.SatStatus> satStatus = new JComboBox<>(StudentProfile.SatStatus.values());
    private final JTextField satReading = new JTextField(4), satMath = new JTextField(4);
    private final JPanel satRow;
    private final JComboBox<String> homeState;
    private final JComboBox<StudentProfile.ChoiceStatus> budgetStatus = new JComboBox<>(StudentProfile.ChoiceStatus.values());
    private final JTextField budget = new JTextField(8);
    private final JPanel budgetRow;
    private final JComboBox<StudentProfile.IncomeBracket> income = new JComboBox<>(StudentProfile.IncomeBracket.values());
    private final JComboBox<StudentProfile.AidEligibility> aid = new JComboBox<>(StudentProfile.AidEligibility.values());

    ExtraInfoPanel(List<String> states) {
        super(new GridBagLayout());
        setOpaque(false);
        String[] items = new String[states.size() + 1];
        items[0] = Messages.get("form.notSure");
        for (int i = 0; i < states.size(); i++) items[i + 1] = states.get(i);
        homeState = new JComboBox<>(items);
        // one line per section, so both boxes stay visible in a narrow panel
        satRow = Ui.column(0);
        satRow.add(Ui.left(Ui.row(6, Ui.text(Messages.get("form.satReading"), 12.5f, Font.PLAIN, Ui.MUTED), satReading)));
        satRow.add(Box.createVerticalStrut(4));
        satRow.add(Ui.left(Ui.row(6, Ui.text(Messages.get("form.satMath"), 12.5f, Font.PLAIN, Ui.MUTED), satMath)));
        budgetRow = Ui.row(6, Ui.text(Messages.get("form.budgetAmount"), 12.5f, Font.PLAIN, Ui.MUTED), budget);
        int r = 0;
        add(r++, "extra.sat", satStatus);
        add(r++, null, satRow);
        add(r++, "extra.home", homeState);
        add(r++, "extra.budget", budgetStatus);
        add(r++, null, budgetRow);
        add(r++, "extra.income", income);
        add(r++, "extra.aid", aid);
        satStatus.addActionListener(e -> update());
        budgetStatus.addActionListener(e -> update());
        update();
    }

    private void add(int row, String key, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row; c.gridx = 0; c.anchor = GridBagConstraints.WEST; c.insets = new Insets(4, 0, 4, 14);
        if (key != null) add(Ui.text(Messages.get(key), 13f, Font.BOLD, Ui.INK), c);
        c.gridx = 1; c.weightx = 1;
        add(field, c);
    }

    private void update() {
        satRow.setVisible(satStatus.getSelectedItem() == StudentProfile.SatStatus.PROVIDED);
        budgetRow.setVisible(budgetStatus.getSelectedItem() == StudentProfile.ChoiceStatus.KNOWN);
        revalidate();
        repaint();
    }

    /** Shows the profile's current answers. */
    void load(StudentProfile s) {
        satStatus.setSelectedItem(s.getSatStatus());
        satReading.setText(s.getSatReadingWriting() == null ? "" : String.valueOf(s.getSatReadingWriting()));
        satMath.setText(s.getSatMath() == null ? "" : String.valueOf(s.getSatMath()));
        homeState.setSelectedItem(s.hasHomeState() ? s.getHomeState() : Messages.get("form.notSure"));
        budgetStatus.setSelectedItem(s.getBudgetStatus());
        budget.setText(s.getAnnualBudget() == null ? "" : String.valueOf(s.getAnnualBudget()));
        income.setSelectedItem(s.getIncomeBracket());
        aid.setSelectedItem(s.getAidEligibility());
        update();
    }

    /** Writes these answers into the profile. Throws IllegalArgumentException with a specific message. */
    void apply(StudentProfile s) {
        StudentProfile.SatStatus st = (StudentProfile.SatStatus) satStatus.getSelectedItem();
        if (st == StudentProfile.SatStatus.PROVIDED)
            s.setSatScores(whole(satReading.getText(), "error.sat.missing"), whole(satMath.getText(), "error.sat.missing"));
        else s.setSatStatus(st);
        s.setHomeState(homeState.getSelectedIndex() > 0 ? (String) homeState.getSelectedItem() : null);
        StudentProfile.ChoiceStatus b = (StudentProfile.ChoiceStatus) budgetStatus.getSelectedItem();
        if (b == StudentProfile.ChoiceStatus.KNOWN) s.setAnnualBudget(whole(budget.getText().replace(",", "").replace("$", ""), "error.budget.missing"));
        else s.setBudgetStatus(b);
        s.setIncomeBracket((StudentProfile.IncomeBracket) income.getSelectedItem());
        s.setAidEligibility((StudentProfile.AidEligibility) aid.getSelectedItem());
    }

    private static int whole(String text, String errorKey) {
        try { return Integer.parseInt(text.trim()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException(Messages.get(errorKey)); }
    }
}

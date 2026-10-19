import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** The guided questions. Every question starts at "Not sure". */
public class StudentFormPanel extends JPanel {
    private final JComboBox<StudentProfile.Pathway> pathway = new JComboBox<>(StudentProfile.Pathway.values());
    private final List<Program> allPrograms;
    private final DefaultComboBoxModel<Program> programModel = new DefaultComboBoxModel<>();
    private final JComboBox<Program> program = new JComboBox<>(programModel);
    private final JTextField programSearch = new JTextField(12);
    private final JComboBox<StudentProfile.SatStatus> satStatus = new JComboBox<>(StudentProfile.SatStatus.values());
    private final JTextField satReading = new JTextField(5), satMath = new JTextField(5);
    private final JComboBox<String> homeState;
    private final JComboBox<StudentProfile.LocationScope> where = new JComboBox<>(StudentProfile.LocationScope.values());
    private final JComboBox<String> preferredState;
    private final JComboBox<StudentProfile.CampusSetting> setting = new JComboBox<>(StudentProfile.CampusSetting.values());
    private final JComboBox<StudentProfile.CollegeSize> size = new JComboBox<>(StudentProfile.CollegeSize.values());
    private final JComboBox<StudentProfile.ChoiceStatus> budgetStatus = new JComboBox<>(StudentProfile.ChoiceStatus.values());
    private final JTextField budget = new JTextField(8);
    private final JComboBox<StudentProfile.IncomeBracket> income = new JComboBox<>(StudentProfile.IncomeBracket.values());
    private final JComboBox<StudentProfile.AidEligibility> aid = new JComboBox<>(StudentProfile.AidEligibility.values());

    public StudentFormPanel(Runnable onExplore, List<String> states, List<Program> programs) {
        super(new BorderLayout(14, 14));
        setBorder(new EmptyBorder(22, 28, 18, 28));
        setBackground(Ui.BG);

        String[] stateItems = new String[states.size() + 1];
        stateItems[0] = Messages.get("form.notSure");
        for (int i = 0; i < states.size(); i++) stateItems[i + 1] = states.get(i);
        homeState = new JComboBox<>(stateItems);
        allPrograms = new ArrayList<>();
        allPrograms.add(Program.NOT_SURE);
        allPrograms.addAll(programs);
        filterPrograms("");
        program.setPrototypeDisplayValue(new Program("0000", "Teacher Education and Professional Development, Specific Levels", 9999));
        programSearch.setToolTipText(Messages.get("form.programHint"));
        // Typing in the search box narrows the program list
        programSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filterPrograms(programSearch.getText()); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filterPrograms(programSearch.getText()); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterPrograms(programSearch.getText()); }
        });
        preferredState = new JComboBox<>(states.toArray(new String[0]));

        JPanel header = new JPanel(new GridLayout(0, 1, 0, 4));
        header.setOpaque(false);
        header.add(Ui.title(Messages.get("form.title"), 26));
        header.add(Ui.muted(Messages.get("form.subtitle")));
        add(header, BorderLayout.NORTH);

        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(Color.WHITE);
        content.setBorder(Ui.card());
        int r = 0;
        row(content, r++, "form.pathway", pathway);
        row(content, r++, "form.program", inline(programSearch, program));
        row(content, r++, "form.sat", inline(satStatus, Messages.get("form.satReading"), satReading, Messages.get("form.satMath"), satMath));
        row(content, r++, "form.home", homeState);
        row(content, r++, "form.where", inline(where, Messages.get("form.preferred"), preferredState));
        row(content, r++, "form.setting", setting);
        row(content, r++, "form.size", size);
        row(content, r++, "form.budget", inline(budgetStatus, Messages.get("form.budgetAmount"), budget));
        row(content, r++, "form.income", income);
        row(content, r++, "form.aid", aid);
        GridBagConstraints filler = new GridBagConstraints();
        filler.gridy = r; filler.weighty = 1; // pushes the questions to the top
        content.add(Box.createGlue(), filler);
        add(new JScrollPane(content), BorderLayout.CENTER);

        JButton example = new JButton(Messages.get("form.example"));
        example.addActionListener(e -> loadExample());
        JButton explore = new JButton(Messages.get("form.explore") + "  \u2192");
        explore.addActionListener(e -> onExplore.run());
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(example, BorderLayout.WEST);
        footer.add(explore, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);

        satStatus.addActionListener(e -> updateEnabled());
        budgetStatus.addActionListener(e -> updateEnabled());
        where.addActionListener(e -> updateEnabled());
        preferredState.setSelectedItem("NY");
        updateEnabled();
    }

    /** Keeps "Not sure yet" plus every program whose name contains the search text. */
    private void filterPrograms(String text) {
        Program selected = (Program) program.getSelectedItem();
        String q = text.trim().toLowerCase();
        programModel.removeAllElements();
        for (Program p : allPrograms)
            if (p.isNotSure() || q.isEmpty() || p.fullName.toLowerCase().contains(q)) programModel.addElement(p);
        boolean keep = selected != null && programModel.getIndexOf(selected) >= 0 && !(selected.isNotSure() && !q.isEmpty());
        if (keep) program.setSelectedItem(selected);
        else if (!q.isEmpty() && programModel.getSize() > 1) program.setSelectedIndex(1); // first match
        else program.setSelectedIndex(0);
    }

    private static JPanel inline(JComponent first, Object... rest) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setOpaque(false);
        p.add(first);
        for (Object o : rest) p.add(o instanceof String ? Ui.muted((String) o) : (JComponent) o);
        return p;
    }

    private static void row(JPanel panel, int r, String key, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = r; c.insets = new Insets(8, 8, 8, 8); c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        JLabel label = new JLabel(Messages.get(key));
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        panel.add(label, c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.NONE;
        panel.add(field, c);
    }

    private void updateEnabled() {
        boolean scores = satStatus.getSelectedItem() == StudentProfile.SatStatus.PROVIDED;
        satReading.setEnabled(scores);
        satMath.setEnabled(scores);
        budget.setEnabled(budgetStatus.getSelectedItem() == StudentProfile.ChoiceStatus.KNOWN);
        preferredState.setEnabled(where.getSelectedItem() == StudentProfile.LocationScope.PREFERRED_STATE);
    }

    /** Fills the form with the fictional student from the proposal. */
    private void loadExample() {
        pathway.setSelectedItem(StudentProfile.Pathway.NOT_SURE);
        programSearch.setText("");
        for (Program p : allPrograms) if ("5138".equals(p.cipCode)) program.setSelectedItem(p);
        satStatus.setSelectedItem(StudentProfile.SatStatus.NOT_YET);
        homeState.setSelectedItem("NY");
        where.setSelectedItem(StudentProfile.LocationScope.PREFERRED_STATE);
        preferredState.setSelectedItem("NY");
        setting.setSelectedItem(StudentProfile.CampusSetting.URBAN);
        size.setSelectedItem(StudentProfile.CollegeSize.NOT_SURE);
        budgetStatus.setSelectedItem(StudentProfile.ChoiceStatus.NOT_SURE);
        income.setSelectedItem(StudentProfile.IncomeBracket.UNDER_30K);
        aid.setSelectedItem(StudentProfile.AidEligibility.NOT_SURE);
        updateEnabled();
    }

    /** Builds a profile from the form. Throws NumberFormatException or IllegalArgumentException on bad input. */
    public StudentProfile getProfile() {
        StudentProfile s = new StudentProfile();
        s.setPathway((StudentProfile.Pathway) pathway.getSelectedItem());
        s.setProgram((Program) program.getSelectedItem());
        StudentProfile.SatStatus st = (StudentProfile.SatStatus) satStatus.getSelectedItem();
        if (st == StudentProfile.SatStatus.PROVIDED)
            s.setSatScores(Integer.parseInt(satReading.getText().trim()), Integer.parseInt(satMath.getText().trim()));
        else s.setSatStatus(st);
        if (homeState.getSelectedIndex() > 0) s.setHomeState((String) homeState.getSelectedItem());
        StudentProfile.LocationScope scope = (StudentProfile.LocationScope) where.getSelectedItem();
        if (scope == StudentProfile.LocationScope.PREFERRED_STATE) s.setPreferredState((String) preferredState.getSelectedItem());
        else s.setLocationScope(scope);
        s.setCampusSetting((StudentProfile.CampusSetting) setting.getSelectedItem());
        s.setCollegeSize((StudentProfile.CollegeSize) size.getSelectedItem());
        StudentProfile.ChoiceStatus b = (StudentProfile.ChoiceStatus) budgetStatus.getSelectedItem();
        if (b == StudentProfile.ChoiceStatus.KNOWN) s.setAnnualBudget(Integer.parseInt(budget.getText().trim().replace(",", "")));
        else s.setBudgetStatus(b);
        s.setIncomeBracket((StudentProfile.IncomeBracket) income.getSelectedItem());
        s.setAidEligibility((StudentProfile.AidEligibility) aid.getSelectedItem());
        return s;
    }
}

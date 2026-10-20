import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.*;
import java.util.List;

/** The guided questions. Every question starts at "Not sure". Several answers can be multi-select. */
public class StudentFormPanel extends JPanel {
    private final JComboBox<StudentProfile.Pathway> pathway = new JComboBox<>(StudentProfile.Pathway.values());

    // Programs: search, pick, add as many as she likes (single programs or whole fields)
    private final List<Program> allPrograms;
    private final DefaultComboBoxModel<Program> programModel = new DefaultComboBoxModel<>();
    private final JComboBox<Program> program = new JComboBox<>(programModel);
    private final JTextField programSearch = new JTextField(10);
    private final JButton addProgram = new JButton(Messages.get("form.add"));
    private final ChipList<Program> programChips;

    private final JComboBox<StudentProfile.SatStatus> satStatus = new JComboBox<>(StudentProfile.SatStatus.values());
    private final JTextField satReading = new JTextField(5), satMath = new JTextField(5);
    private final JComboBox<String> homeState;

    // Location: scope plus one or more states
    private final JComboBox<StudentProfile.LocationScope> where = new JComboBox<>(StudentProfile.LocationScope.values());
    private final JComboBox<String> stateToAdd;
    private final JButton addState = new JButton(Messages.get("form.add"));
    private final ChipList<String> stateChips;

    private final Map<StudentProfile.CampusSetting, JCheckBox> settingBoxes = new EnumMap<>(StudentProfile.CampusSetting.class);
    private final Map<StudentProfile.CollegeSize, JCheckBox> sizeBoxes = new EnumMap<>(StudentProfile.CollegeSize.class);

    private final JComboBox<StudentProfile.ChoiceStatus> budgetStatus = new JComboBox<>(StudentProfile.ChoiceStatus.values());
    private final JTextField budget = new JTextField(8);
    private final JComboBox<StudentProfile.IncomeBracket> income = new JComboBox<>(StudentProfile.IncomeBracket.values());
    private final JComboBox<StudentProfile.AidEligibility> aid = new JComboBox<>(StudentProfile.AidEligibility.values());

    public StudentFormPanel(Runnable onExplore, List<String> states, List<Program> programs) {
        super(new BorderLayout(14, 14));
        setBorder(new EmptyBorder(22, 28, 18, 28));
        setBackground(Ui.BG);

        String[] homeItems = new String[states.size() + 1];
        homeItems[0] = Messages.get("form.notSure");
        for (int i = 0; i < states.size(); i++) homeItems[i + 1] = states.get(i);
        homeState = new JComboBox<>(homeItems);
        stateToAdd = new JComboBox<>(states.toArray(new String[0]));
        stateToAdd.setSelectedItem("NY");

        allPrograms = new ArrayList<>(programs);
        programChips = new ChipList<>(Program::shortName, this::updateEnabled);
        stateChips = new ChipList<>(s -> s, this::updateEnabled);
        filterPrograms("");
        program.setPrototypeDisplayValue(new Program("0000", "Teacher Education and Professional Development, Specific", 9999));
        programSearch.setToolTipText(Messages.get("form.programHint"));
        programSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterPrograms(programSearch.getText()); }
            public void removeUpdate(DocumentEvent e) { filterPrograms(programSearch.getText()); }
            public void changedUpdate(DocumentEvent e) { filterPrograms(programSearch.getText()); }
        });
        addProgram.addActionListener(e -> programChips.add((Program) program.getSelectedItem()));
        addState.addActionListener(e -> stateChips.add((String) stateToAdd.getSelectedItem()));

        for (StudentProfile.CampusSetting c : StudentProfile.CampusSetting.values())
            if (c != StudentProfile.CampusSetting.NOT_SURE) settingBoxes.put(c, checkbox(c.toString()));
        for (StudentProfile.CollegeSize c : StudentProfile.CollegeSize.values()) sizeBoxes.put(c, checkbox(c.toString()));

        JPanel header = new JPanel(new GridLayout(0, 1, 0, 4));
        header.setOpaque(false);
        header.add(Ui.title(Messages.get("form.title"), 26));
        header.add(Ui.muted(Messages.get("form.subtitle")));
        add(header, BorderLayout.NORTH);

        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(Color.WHITE);
        content.setBorder(Ui.card());
        int r = 0;
        section(content, r++, "form.section.goals");
        row(content, r++, "form.pathway", null, inline(pathway));
        row(content, r++, "form.program", "form.program.hint", stack(
            inline(Ui.muted(Messages.get("form.search")), programSearch, program, addProgram), programChips));
        row(content, r++, "form.sat", null, inline(satStatus, Ui.muted(Messages.get("form.satReading")), satReading,
            Ui.muted(Messages.get("form.satMath")), satMath));
        section(content, r++, "form.section.social");
        row(content, r++, "form.where", "form.where.hint", stack(inline(where, stateToAdd, addState), stateChips));
        row(content, r++, "form.setting", "form.multi.hint", inline(settingBoxes.values().toArray(new JComponent[0])));
        row(content, r++, "form.size", "form.multi.hint", inline(sizeBoxes.values().toArray(new JComponent[0])));
        section(content, r++, "form.section.financial");
        row(content, r++, "form.home", "form.home.hint", inline(homeState));
        row(content, r++, "form.budget", null, inline(budgetStatus, Ui.muted(Messages.get("form.budgetAmount")), budget));
        row(content, r++, "form.income", "form.income.hint", inline(income));
        row(content, r++, "form.aid", null, inline(aid));
        GridBagConstraints filler = new GridBagConstraints();
        filler.gridy = r; filler.weighty = 1;
        content.add(Box.createGlue(), filler);
        JScrollPane scroll = new JScrollPane(content);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        JButton example = new JButton(Messages.get("form.example"));
        example.addActionListener(e -> loadExample());
        JButton explore = new JButton(Messages.get("form.explore") + "  \u2192");
        explore.setFont(Ui.font(explore, Font.BOLD, 14f));
        explore.addActionListener(e -> onExplore.run());
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(example, BorderLayout.WEST);
        footer.add(explore, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);

        satStatus.addActionListener(e -> updateEnabled());
        budgetStatus.addActionListener(e -> updateEnabled());
        where.addActionListener(e -> updateEnabled());
        updateEnabled();
    }

    private static JCheckBox checkbox(String text) {
        JCheckBox b = new JCheckBox(text);
        b.setOpaque(false);
        return b;
    }

    private void filterPrograms(String text) {
        String q = text.trim().toLowerCase();
        programModel.removeAllElements();
        for (Program p : allPrograms)
            if (q.isEmpty() || p.fullName.toLowerCase().contains(q)) programModel.addElement(p);
        if (programModel.getSize() > 0) program.setSelectedIndex(0);
        revalidate();
    }

    private static JPanel inline(JComponent... parts) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setOpaque(false);
        for (JComponent c : parts) p.add(c);
        return p;
    }

    private static JPanel stack(JComponent... parts) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        for (JComponent c : parts) { c.setAlignmentX(LEFT_ALIGNMENT); p.add(c); }
        // chips line up with the controls above them
        if (parts.length > 1) ((JComponent) parts[1]).setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
        return p;
    }

    private static void section(JPanel panel, int r, String key) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = r; c.gridx = 0; c.gridwidth = 2; c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(r == 0 ? 2 : 16, 8, 2, 8);
        JLabel l = Ui.title(Messages.get(key), 17);
        panel.add(l, c);
    }

    private static void row(JPanel panel, int r, String key, String hintKey, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = r; c.insets = new Insets(7, 8, 7, 8); c.anchor = GridBagConstraints.NORTHWEST;
        c.gridx = 0;
        String hint = hintKey == null ? "" : "<br><span style='color:#687085;font-weight:normal'>" + Ui.escape(Messages.get(hintKey)) + "</span>";
        JLabel label = new JLabel("<html><div style='width:230px'><b>" + Ui.escape(Messages.get(key)) + "</b>" + hint + "</div></html>");
        panel.add(label, c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.NONE;
        panel.add(field, c);
    }

    private void updateEnabled() {
        boolean scores = satStatus.getSelectedItem() == StudentProfile.SatStatus.PROVIDED;
        satReading.setEnabled(scores);
        satMath.setEnabled(scores);
        budget.setEnabled(budgetStatus.getSelectedItem() == StudentProfile.ChoiceStatus.KNOWN);
        boolean specific = where.getSelectedItem() == StudentProfile.LocationScope.PREFERRED_STATE;
        stateToAdd.setEnabled(specific);
        addState.setEnabled(specific);
        stateChips.setVisible(specific);
    }

    /** Puts every question back to "Not sure" (used by Start over). */
    public void reset() {
        pathway.setSelectedItem(StudentProfile.Pathway.NOT_SURE);
        programSearch.setText("");
        programChips.clear();
        satStatus.setSelectedItem(StudentProfile.SatStatus.NOT_SURE);
        satReading.setText(""); satMath.setText("");
        homeState.setSelectedIndex(0);
        where.setSelectedItem(StudentProfile.LocationScope.NOT_SURE);
        stateChips.clear();
        for (JCheckBox b : settingBoxes.values()) b.setSelected(false);
        for (JCheckBox b : sizeBoxes.values()) b.setSelected(false);
        budgetStatus.setSelectedItem(StudentProfile.ChoiceStatus.NOT_SURE);
        budget.setText("");
        income.setSelectedItem(StudentProfile.IncomeBracket.NOT_SURE);
        aid.setSelectedItem(StudentProfile.AidEligibility.NOT_SURE);
        updateEnabled();
    }

    /** Fills the form with the fictional student from the proposal. */
    private void loadExample() {
        reset();
        for (Program p : allPrograms) if ("5138".equals(p.cipCode)) programChips.add(p);
        satStatus.setSelectedItem(StudentProfile.SatStatus.NOT_YET);
        homeState.setSelectedItem("NY");
        where.setSelectedItem(StudentProfile.LocationScope.PREFERRED_STATE);
        stateChips.add("NY");
        settingBoxes.get(StudentProfile.CampusSetting.URBAN).setSelected(true);
        income.setSelectedItem(StudentProfile.IncomeBracket.UNDER_30K);
        updateEnabled();
    }

    /** Builds a profile from the form. Throws NumberFormatException or IllegalArgumentException on bad input. */
    public StudentProfile getProfile() {
        StudentProfile s = new StudentProfile();
        s.setPathway((StudentProfile.Pathway) pathway.getSelectedItem());
        s.setPrograms(programChips.items());
        StudentProfile.SatStatus st = (StudentProfile.SatStatus) satStatus.getSelectedItem();
        if (st == StudentProfile.SatStatus.PROVIDED)
            s.setSatScores(Integer.parseInt(satReading.getText().trim()), Integer.parseInt(satMath.getText().trim()));
        else s.setSatStatus(st);
        if (homeState.getSelectedIndex() > 0) s.setHomeState((String) homeState.getSelectedItem());
        StudentProfile.LocationScope scope = (StudentProfile.LocationScope) where.getSelectedItem();
        if (scope == StudentProfile.LocationScope.PREFERRED_STATE) s.setPreferredStates(stateChips.items());
        else s.setLocationScope(scope);
        List<StudentProfile.CampusSetting> settings = new ArrayList<>();
        for (Map.Entry<StudentProfile.CampusSetting, JCheckBox> e : settingBoxes.entrySet()) if (e.getValue().isSelected()) settings.add(e.getKey());
        s.setSettings(settings);
        List<StudentProfile.CollegeSize> sizes = new ArrayList<>();
        for (Map.Entry<StudentProfile.CollegeSize, JCheckBox> e : sizeBoxes.entrySet()) if (e.getValue().isSelected()) sizes.add(e.getKey());
        s.setSizes(sizes);
        StudentProfile.ChoiceStatus b = (StudentProfile.ChoiceStatus) budgetStatus.getSelectedItem();
        if (b == StudentProfile.ChoiceStatus.KNOWN) s.setAnnualBudget(Integer.parseInt(budget.getText().trim().replace(",", "")));
        else s.setBudgetStatus(b);
        s.setIncomeBracket((StudentProfile.IncomeBracket) income.getSelectedItem());
        s.setAidEligibility((StudentProfile.AidEligibility) aid.getSelectedItem());
        return s;
    }
}

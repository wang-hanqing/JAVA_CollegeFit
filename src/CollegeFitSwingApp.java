import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

/**
 * CollegeFit v0.4: guided college decision-making in six steps.
 * 1 Get started -> 2 Your direction -> 3 Priorities -> 4 Explore -> 5 Compare and decide -> 6 Next steps.
 * College details open from Explore or Compare and return there.
 *
 *   java -cp out CollegeFitSwingApp            all 2,989 colleges (run from the CollegeFit root folder)
 *   java -cp out CollegeFitSwingApp --sample   30-college test sample
 *   java -jar CollegeFit.jar                   shared version: data is packed inside the jar
 */
public class CollegeFitSwingApp extends JFrame {
    /** The four ways to begin, from the first page. */
    public enum Start { SUBJECT, PLACE, PATHWAY, NOT_SURE }

    // Shared state for every page
    final List<College> colleges;
    final List<Program> programs;
    final List<String> states;
    final RecommendationEngine engine = new RecommendationEngine();
    final Shortlist shortlist = new Shortlist();                 // Kept colleges
    final Set<College> compare = new LinkedHashSet<>();          // up to 3 kept colleges shown side by side
    final Map<College, String> notForMe = new LinkedHashMap<>(); // college -> reason key ("" if none); can be undone
    final Map<College, Set<String>> keepReasons = new HashMap<>(); // the student's own reasons for keeping a college
    final Set<String> dismissedTips = new HashSet<>();           // suggestion banners she said "Not now" to
    StudentProfile profile = new StudentProfile();
    Start start = Start.SUBJECT;

    private final CardLayout cards = new CardLayout();
    private final JPanel pages = new JPanel(cards);
    private final Ui.Stepper stepper = new Ui.Stepper(Messages.get("step.1"), Messages.get("step.2"),
        Messages.get("step.3"), Messages.get("step.4"), Messages.get("step.5"), Messages.get("step.6"));
    private final Ui.Button myColleges = new Ui.Button("", false);

    private final StartPanel startPanel;
    private final DirectionPanel directionPanel;
    private final UnsurePanel unsurePanel;
    private final PrioritiesPanel prioritiesPanel;
    private final OptionsPanel optionsPanel;
    private final DetailsPanel detailsPanel;
    private final ComparePanel comparePanel;
    private final NextStepsPanel nextStepsPanel;
    private int detailsFrom = 3; // step that opened the details page

    public CollegeFitSwingApp(List<College> colleges, List<Program> programs) {
        super(Messages.get("app.title"));
        this.colleges = colleges;
        this.programs = programs;
        Set<String> st = new TreeSet<>();
        for (College c : colleges) st.add(c.state);
        this.states = new ArrayList<>(st);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Ui.BG);
        root.add(topBar(), BorderLayout.NORTH);

        // Opaque backgrounds everywhere avoid black areas when pages switch on macOS
        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(Ui.BG);
        JPanel stepRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        stepRow.setBackground(Ui.BG);
        stepRow.add(stepper);
        body.add(stepRow, BorderLayout.NORTH);
        pages.setBackground(Ui.BG);
        body.add(pages, BorderLayout.CENTER);
        root.add(body, BorderLayout.CENTER);
        setContentPane(root);

        startPanel = new StartPanel(this);
        directionPanel = new DirectionPanel(this);
        unsurePanel = new UnsurePanel(this);
        prioritiesPanel = new PrioritiesPanel(this);
        optionsPanel = new OptionsPanel(this);
        detailsPanel = new DetailsPanel(this);
        comparePanel = new ComparePanel(this);
        nextStepsPanel = new NextStepsPanel(this);
        pages.add(startPanel, "start");
        pages.add(directionPanel, "direction");
        pages.add(unsurePanel, "unsure");
        pages.add(prioritiesPanel, "priorities");
        pages.add(optionsPanel, "options");
        pages.add(detailsPanel, "details");
        pages.add(comparePanel, "compare");
        pages.add(nextStepsPanel, "next");

        listChanged();
        showStart();
        setSize(1260, 880);
        setMinimumSize(new Dimension(1100, 760));
        setLocationRelativeTo(null);
    }

    private JComponent topBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Ui.WHITE);
        bar.setBorder(new javax.swing.border.CompoundBorder(new MatteBorder(0, 0, 1, 0, Ui.LINE), new EmptyBorder(10, 28, 10, 28)));
        JLabel brand = new JLabel("<html><span style='color:#1e2638'>College</span><span style='color:#3b5bcc'>Fit</span></html>");
        brand.setFont(Ui.serif(24f));
        brand.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        brand.setToolTipText(Messages.get("nav.home.tip"));
        brand.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { showStart(); }
        });
        bar.add(brand, BorderLayout.WEST);
        Ui.Button home = new Ui.Button(Messages.get("nav.home"), false);
        home.setToolTipText(Messages.get("nav.home.tip"));
        home.addActionListener(e -> showStart());
        myColleges.setIcon(Ui.icon("heart", Ui.INK, 16));
        myColleges.addActionListener(e -> showCompare());
        Ui.Button help = new Ui.Button(Messages.get("nav.help"), false);
        help.setIcon(Ui.icon("help", Ui.INK, 16));
        help.addActionListener(e -> JOptionPane.showMessageDialog(this,
            Ui.html(Ui.escape(Messages.get("help.text")) + "<br><br><b>" + Ui.escape(Messages.get("help.data.title")) + "</b><br>"
                + Ui.escape(Messages.get("help.data")).replace("\n", "<br>"), 460, 13f, Ui.INK),
            Messages.get("nav.help"), JOptionPane.INFORMATION_MESSAGE));
        bar.add(Ui.row(10, home, myColleges, help), BorderLayout.EAST);
        return bar;
    }

    // ---------- navigation ----------
    void showStart() { stepper.setActive(0); cards.show(pages, "start"); }

    void showDirection(Start s) {
        start = s;
        stepper.setActive(1);
        if (s == Start.NOT_SURE) { unsurePanel.load(profile); cards.show(pages, "unsure"); }
        else { directionPanel.load(s, profile); cards.show(pages, "direction"); }
    }

    /** Back to the direction page the student used last. */
    void changeDirection() { showDirection(start); }

    /** Step 3: priorities, after the direction page. */
    void showPriorities(StudentProfile p) {
        if (!p.hasPriorities()) p.setPriorities(profile.getPriorities()); // keep earlier choices when coming back
        profile = p;
        stepper.setActive(2);
        prioritiesPanel.load(p);
        cards.show(pages, "priorities");
    }

    /** Step 4: explore, ordered by the profile's priorities. */
    void showOptions(StudentProfile p) {
        profile = p;
        stepper.setActive(3);
        optionsPanel.show(p);
        cards.show(pages, "options");
    }

    /** Back to Explore without recomputing the direction. */
    void backToOptions() { stepper.setActive(3); optionsPanel.refresh(); cards.show(pages, "options"); }

    /** Changes priorities from a suggestion and re-orders Explore. */
    void reorder(List<StudentProfile.Priority> priorities) {
        StudentProfile p = profile.withPrograms(profile.getPrograms());
        p.setPriorities(priorities);
        showOptions(p);
    }

    /** Details are a helper page: opened from Explore (4) or Compare (5), and Back returns there. */
    void showDetails(College c, int fromStep) {
        detailsFrom = fromStep;
        stepper.setActive(fromStep);
        detailsPanel.show(c);
        cards.show(pages, "details");
    }

    boolean detailsFromCompare() { return detailsFrom == 4; }

    void closeDetails() { if (detailsFrom == 4) showCompare(); else backToOptions(); }

    void showCompare() {
        stepper.setActive(4);
        comparePanel.refresh();
        cards.show(pages, "compare");
    }

    void showNextSteps() {
        stepper.setActive(5);
        nextStepsPanel.refresh();
        cards.show(pages, "next");
    }

    /** Home keeps answers and saved colleges; Start over clears them. */
    void startOver() {
        int ok = JOptionPane.showConfirmDialog(this, Messages.get("startOver.confirm"), Messages.get("startOver"), JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;
        shortlist.clear();
        compare.clear();
        notForMe.clear();
        keepReasons.clear();
        dismissedTips.clear();
        profile = new StudentProfile();
        directionPanel.reset();
        unsurePanel.reset();
        listChanged();
        showStart();
    }

    // ---------- Keep and Not for me ----------
    boolean isKept(College c) { return shortlist.contains(c); }

    void keep(College c) {
        notForMe.remove(c);
        if (!shortlist.contains(c)) { shortlist.add(c); if (compare.size() < 3) compare.add(c); }
        listChanged();
    }

    void unkeep(College c) {
        shortlist.remove(c);
        compare.remove(c);
        keepReasons.remove(c);
        listChanged();
    }

    /** Marks a college "Not for me" with an optional reason. It is not deleted and can be undone. */
    void notForMe(College c, String reason) {
        unkeep(c);
        notForMe.put(c, reason == null ? "" : reason);
    }

    void undoNotForMe(College c) { notForMe.remove(c); }

    /** How many colleges were set aside for each reason. */
    Map<String, Integer> reasonCounts() {
        Map<String, Integer> out = new HashMap<>();
        for (String r : notForMe.values()) if (!r.isEmpty()) out.merge(r, 1, Integer::sum);
        return out;
    }

    void addToCompare(College c) {
        keep(c);
        if (!compare.contains(c)) {
            if (compare.size() == 3) {
                JOptionPane.showMessageDialog(this, Messages.get("compare.full"));
                return;
            }
            compare.add(c);
        }
        listChanged();
    }

    void listChanged() { myColleges.setText(Messages.get("nav.mine", shortlist.size())); }

    /** Re-evaluates with new optional answers (scores, costs) without changing the direction. */
    void updateExtras(StudentProfile p) { profile = p; }

    void error(Exception ex) {
        String msg = ex instanceof NumberFormatException ? Messages.get("error.number") : ex.getMessage();
        JOptionPane.showMessageDialog(this, msg, Messages.get("error.title"), JOptionPane.WARNING_MESSAGE);
    }

    // ---------- shared page frame ----------
    /** A page with scrollable content and a white footer bar. */
    static JPanel page(JComponent content, JComponent footLeft, JComponent... footRight) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JPanel padded = new Ui.WidthPanel(new BorderLayout());
        padded.setBorder(new EmptyBorder(4, 40, 12, 40));
        padded.add(content, BorderLayout.CENTER);
        p.add(Ui.scroll(padded), BorderLayout.CENTER);
        JPanel foot = new JPanel(new BorderLayout());
        foot.setBackground(Ui.WHITE);
        foot.setBorder(new javax.swing.border.CompoundBorder(new MatteBorder(1, 0, 0, 0, Ui.LINE), new EmptyBorder(10, 28, 10, 28)));
        if (footLeft != null) foot.add(footLeft, BorderLayout.WEST);
        foot.add(Ui.row(10, footRight), BorderLayout.EAST);
        p.add(foot, BorderLayout.SOUTH);
        return p;
    }

    static JComponent privacyNote(String key) {
        JLabel l = Ui.text(Messages.get(key), 12.5f, Font.PLAIN, Ui.MUTED);
        l.setIcon(Ui.icon("lock", Ui.MUTED, 15));
        l.setIconTextGap(8);
        return l;
    }

    public static void main(String[] args) {
        String suffix = args.length > 0 && args[0].equals("--sample") ? "_sample" : "";
        Path dir = Paths.get("data", "processed");
        Path collegesFile = dir.resolve("colleges" + suffix + ".csv");
        boolean onDisk = Files.exists(collegesFile);
        // Use files on disk while developing; otherwise use the copy packed inside CollegeFit.jar.
        CollegeDataLoader.Source colleges = onDisk ? CollegeDataLoader.file(collegesFile)
            : CollegeDataLoader.resource("/data/processed/colleges" + suffix + ".csv");
        CollegeDataLoader.Source programs = onDisk ? CollegeDataLoader.file(dir.resolve("programs" + suffix + ".csv"))
            : CollegeDataLoader.resource("/data/processed/programs" + suffix + ".csv");
        try {
            List<College> list = CollegeDataLoader.load(colleges, programs);
            List<Program> programList = CollegeDataLoader.loadProgramList(programs, list);
            System.out.println("Loaded " + list.size() + " colleges and " + programList.size() + " programs and fields"
                + (onDisk ? " from " + dir.toAbsolutePath() : " from the app"));
            SwingUtilities.invokeLater(() -> new CollegeFitSwingApp(list, programList).setVisible(true));
        } catch (java.io.IOException ex) {
            String msg = "Could not load data: " + ex.getMessage();
            System.err.println(msg);
            if (!GraphicsEnvironment.isHeadless()) JOptionPane.showMessageDialog(null, msg, "CollegeFit", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
}

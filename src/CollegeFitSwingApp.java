import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

/**
 * CollegeFit v0.3: a five-step journey.
 * 1 Get started -> 2 Your direction -> 3 See options -> 4 Explore details -> 5 Next steps.
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
    final Shortlist shortlist = new Shortlist();
    final Set<College> compare = new LinkedHashSet<>(); // up to 3 saved colleges picked for "My 3 colleges"
    StudentProfile profile = new StudentProfile();
    Start start = Start.SUBJECT;

    private final CardLayout cards = new CardLayout();
    private final JPanel pages = new JPanel(cards);
    private final Ui.Stepper stepper = new Ui.Stepper(Messages.get("step.1"), Messages.get("step.2"),
        Messages.get("step.3"), Messages.get("step.4"), Messages.get("step.5"));
    private final Ui.Button myColleges = new Ui.Button("", false);

    private final StartPanel startPanel;
    private final DirectionPanel directionPanel;
    private final UnsurePanel unsurePanel;
    private final OptionsPanel optionsPanel;
    private final DetailsPanel detailsPanel;
    private final MyCollegesPanel myCollegesPanel;

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

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        JPanel stepRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        stepRow.setOpaque(false);
        stepRow.add(stepper);
        body.add(stepRow, BorderLayout.NORTH);
        pages.setOpaque(false);
        body.add(pages, BorderLayout.CENTER);
        root.add(body, BorderLayout.CENTER);
        setContentPane(root);

        startPanel = new StartPanel(this);
        directionPanel = new DirectionPanel(this);
        unsurePanel = new UnsurePanel(this);
        optionsPanel = new OptionsPanel(this);
        detailsPanel = new DetailsPanel(this);
        myCollegesPanel = new MyCollegesPanel(this);
        pages.add(startPanel, "start");
        pages.add(directionPanel, "direction");
        pages.add(unsurePanel, "unsure");
        pages.add(optionsPanel, "options");
        pages.add(detailsPanel, "details");
        pages.add(myCollegesPanel, "mine");

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
        bar.add(brand, BorderLayout.WEST);
        myColleges.setIcon(Ui.icon("heart", Ui.INK, 16));
        myColleges.addActionListener(e -> showMyColleges());
        Ui.Button help = new Ui.Button(Messages.get("nav.help"), false);
        help.setIcon(Ui.icon("help", Ui.INK, 16));
        help.addActionListener(e -> JOptionPane.showMessageDialog(this,
            Ui.wrap(Messages.get("help.text"), 420, 13f, Ui.INK), Messages.get("nav.help"), JOptionPane.INFORMATION_MESSAGE));
        bar.add(Ui.row(10, myColleges, help), BorderLayout.EAST);
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

    void showOptions(StudentProfile p) {
        profile = p;
        stepper.setActive(2);
        optionsPanel.show(p);
        cards.show(pages, "options");
    }

    void backToOptions() { stepper.setActive(2); optionsPanel.refreshSaved(); cards.show(pages, "options"); }

    void showDetails(College c) {
        stepper.setActive(3);
        detailsPanel.show(c);
        cards.show(pages, "details");
    }

    void showMyColleges() {
        stepper.setActive(4);
        myCollegesPanel.refresh();
        cards.show(pages, "mine");
    }

    /** Clears answers, results and the saved list (for shared computers). */
    void startOver() {
        int ok = JOptionPane.showConfirmDialog(this, Messages.get("startOver.confirm"), Messages.get("startOver"), JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;
        shortlist.clear();
        compare.clear();
        profile = new StudentProfile();
        directionPanel.reset();
        unsurePanel.reset();
        listChanged();
        showStart();
    }

    // ---------- saved colleges ----------
    void toggleSaved(College c) {
        if (shortlist.contains(c)) { shortlist.remove(c); compare.remove(c); }
        else { shortlist.add(c); if (compare.size() < 3) compare.add(c); }
        listChanged();
    }

    void addToCompare(College c) {
        if (!shortlist.contains(c)) shortlist.add(c);
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

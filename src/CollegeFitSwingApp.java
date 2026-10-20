import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

/**
 * Entry point.
 *   java -cp out CollegeFitSwingApp            all 2,989 colleges (run from the CollegeFit root folder)
 *   java -cp out CollegeFitSwingApp --sample   30-college test sample
 *   java -jar CollegeFit.jar                   shared version: data is packed inside the jar
 */
public class CollegeFitSwingApp extends JFrame {
    private final List<College> colleges;
    private final RecommendationEngine engine = new RecommendationEngine();
    private final CardLayout pages = new CardLayout();
    private final JPanel center = new JPanel(pages);
    private final StudentFormPanel form;
    private final ResultsPanel results;

    public CollegeFitSwingApp(List<College> colleges, List<Program> programs) {
        super(Messages.get("app.title"));
        this.colleges = colleges;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel brand = new JPanel(new BorderLayout());
        brand.setBorder(new EmptyBorder(12, 22, 12, 22));
        brand.setBackground(Color.WHITE);
        brand.add(Ui.title(Messages.get("app.title"), 26), BorderLayout.WEST);
        brand.add(Ui.muted(Messages.get("app.tagline")), BorderLayout.EAST);
        add(brand, BorderLayout.NORTH);

        Set<String> states = new TreeSet<>();
        for (College c : colleges) states.add(c.state);
        form = new StudentFormPanel(this::search, new ArrayList<>(states), programs);
        results = new ResultsPanel(() -> pages.show(center, "form"), () -> { form.reset(); pages.show(center, "form"); });
        center.add(form, "form");
        center.add(results, "results");
        add(center, BorderLayout.CENTER);

        JLabel foot = Ui.muted(Messages.get("app.footer"));
        foot.setBorder(new EmptyBorder(6, 22, 8, 22));
        add(foot, BorderLayout.SOUTH);
        setSize(1240, 840);
        setMinimumSize(new Dimension(980, 640));
        setLocationRelativeTo(null);
    }

    private void search() {
        try {
            StudentProfile s = form.getProfile();
            List<CollegeRecommendation> ranked = engine.recommend(s, colleges);
            results.display(s, ranked, engine.worthALookElsewhere(s, ranked, ResultsPanel.MAX_ELSEWHERE));
            pages.show(center, "results");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, Messages.get("error.number"), Messages.get("error.title"), JOptionPane.WARNING_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), Messages.get("error.title"), JOptionPane.WARNING_MESSAGE);
        }
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
            System.out.println("Loaded " + list.size() + " colleges and " + programList.size() + " programs"
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

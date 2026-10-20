import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EnumMap;
import java.util.Map;

/** Step 1: choose one of four starting points. No scores, income or budget are asked here. */
class StartPanel extends JPanel {
    private final Map<CollegeFitSwingApp.Start, Ui.Card> options = new EnumMap<>(CollegeFitSwingApp.Start.class);
    private CollegeFitSwingApp.Start chosen = CollegeFitSwingApp.Start.SUBJECT;

    StartPanel(CollegeFitSwingApp app) {
        super(new BorderLayout());
        setOpaque(false);
        JPanel content = Ui.column(0);
        JLabel h = Ui.title(Messages.get("start.title"), 34);
        h.setAlignmentX(CENTER_ALIGNMENT);
        JLabel sub = Ui.text(Messages.get("start.subtitle"), 15f, Font.PLAIN, Ui.MUTED);
        sub.setAlignmentX(CENTER_ALIGNMENT);
        content.add(Box.createVerticalStrut(18));
        content.add(h);
        content.add(Box.createVerticalStrut(8));
        content.add(sub);
        content.add(Box.createVerticalStrut(30));

        JPanel grid = new JPanel(new GridLayout(2, 2, 18, 18));
        grid.setOpaque(false);
        grid.setBorder(new EmptyBorder(0, 60, 0, 60));
        grid.add(option(app, CollegeFitSwingApp.Start.SUBJECT, "cap", Ui.BLUE, Ui.BLUE_SOFT));
        grid.add(option(app, CollegeFitSwingApp.Start.PLACE, "pin", Ui.GREEN, Ui.GREEN_SOFT));
        grid.add(option(app, CollegeFitSwingApp.Start.PATHWAY, "bank", Ui.PURPLE, Ui.PURPLE_SOFT));
        grid.add(option(app, CollegeFitSwingApp.Start.NOT_SURE, "compass", Ui.AMBER, Ui.AMBER_SOFT));
        grid.setMaximumSize(new Dimension(1000, 290));
        content.add(grid);
        highlight();

        Ui.Button go = new Ui.Button(Messages.get("start.continue"), true);
        go.setIcon(Ui.icon("arrow", Ui.WHITE, 16));
        go.setHorizontalTextPosition(SwingConstants.LEFT);
        go.addActionListener(e -> app.showDirection(chosen));
        add(CollegeFitSwingApp.page(content, CollegeFitSwingApp.privacyNote("start.privacy"), go));
    }

    private Ui.Card option(CollegeFitSwingApp app, CollegeFitSwingApp.Start s, String icon, Color color, Color bg) {
        Ui.Card c = new Ui.Card(new BorderLayout(18, 0), Ui.WHITE, Ui.LINE);
        c.setBorder(new EmptyBorder(20, 22, 20, 22));
        c.add(Ui.bubble(icon, color, bg, 58), BorderLayout.WEST);
        JPanel text = Ui.column(0);
        JLabel t = Ui.wrap(Messages.get("start." + s.name()), 270, 18f, Ui.INK);
        t.setFont(Ui.sans(Font.BOLD, 18f));
        text.add(Ui.left(t));
        text.add(Box.createVerticalStrut(6));
        text.add(Ui.left(Ui.wrap(Messages.get("start." + s.name() + ".desc"), 270, 13.5f, Ui.MUTED)));
        JPanel middle = new JPanel(new GridBagLayout()); // centers the text vertically
        middle.setOpaque(false);
        middle.add(text);
        c.add(middle, BorderLayout.CENTER);
        c.add(new JLabel(Ui.icon("chevron", Ui.MUTED, 20)), BorderLayout.EAST);
        c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        c.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                chosen = s;
                highlight();
                if (e.getClickCount() >= 2) app.showDirection(s); // double-click goes straight on
            }
        });
        options.put(s, c);
        return c;
    }

    private void highlight() {
        for (Map.Entry<CollegeFitSwingApp.Start, Ui.Card> e : options.entrySet()) {
            boolean on = e.getKey() == chosen;
            e.getValue().style(on ? Ui.BLUE_PALE : Ui.WHITE, on ? Ui.BLUE : Ui.LINE, on ? 2f : 1f);
        }
    }
}

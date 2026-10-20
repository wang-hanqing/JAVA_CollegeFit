import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Explains two-year and four-year colleges with counts and median published costs from the data. */
class PathwayExplainer extends JPanel {
    private final List<College> colleges;
    private final JPanel cards = new JPanel(new GridLayout(1, 2, 14, 0));

    PathwayExplainer(List<College> colleges) {
        super(new BorderLayout());
        this.colleges = colleges;
        setOpaque(false);
        cards.setOpaque(false);
        add(cards, BorderLayout.CENTER);
    }

    void update(Set<String> states) {
        Map<RecommendationEngine.PathwayKind, int[]> t = RecommendationEngine.pathwaySummary(colleges, states);
        String where = states.size() == 1 ? StateNames.name(states.iterator().next()) : Messages.get("pathway.us");
        int[] two = t.get(RecommendationEngine.PathwayKind.TWO_YEAR), pub = t.get(RecommendationEngine.PathwayKind.FOUR_YEAR_PUBLIC),
            priv = t.get(RecommendationEngine.PathwayKind.FOUR_YEAR_PRIVATE);
        cards.removeAll();
        cards.add(card("bank", Ui.PURPLE, Ui.PURPLE_SOFT, Messages.get("pathway.two"), Messages.get("pathway.two.tag"),
            Messages.get("pathway.two.text", Messages.number(two[0]), where, Messages.money(two[1]))));
        cards.add(card("cap", Ui.BLUE, Ui.BLUE_SOFT, Messages.get("pathway.four"), Messages.get("pathway.four.tag"),
            Messages.get("pathway.four.text", Messages.number(pub[0]), Messages.money(pub[1]), Messages.number(priv[0]), Messages.money(priv[1]), where)));
        cards.revalidate();
        cards.repaint();
    }

    private static JComponent card(String icon, Color color, Color bg, String title, String tag, String text) {
        Ui.Card c = new Ui.Card(new BorderLayout(14, 0), Ui.WHITE, Ui.LINE);
        c.setBorder(new EmptyBorder(14, 18, 14, 18));
        c.add(Ui.bubble(icon, color, bg, 44), BorderLayout.WEST);
        JPanel col = Ui.column(0);
        col.add(Ui.left(Ui.row(8, Ui.text(title, 15f, Font.BOLD, Ui.INK), Ui.tag(tag))));
        col.add(Box.createVerticalStrut(4));
        col.add(Ui.left(Ui.wrap(text, 330, 12.5f, Ui.MUTED)));
        c.add(col, BorderLayout.CENTER);
        JPanel top = new JPanel(new BorderLayout()); // keeps the card at its natural height
        top.setOpaque(false);
        top.add(c, BorderLayout.NORTH);
        return top;
    }
}

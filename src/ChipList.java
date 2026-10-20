import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Shows chosen items as removable chips, e.g. "Registered Nursing  x", with "+N more" and Clear all. */
class ChipList<T> extends JPanel {
    static final int MAX_VISIBLE = 6;
    private final List<T> items = new ArrayList<>();
    private final Runnable onChange;
    private final Function<T, String> text;

    static final int MAX_WIDTH = 720; // chips wrap onto new lines instead of widening the form

    ChipList(Function<T, String> text, Runnable onChange) {
        super(new FlowLayout(FlowLayout.LEFT, 6, 4));
        this.text = text; this.onChange = onChange;
        setOpaque(false);
    }

    List<T> items() { return new ArrayList<>(items); }

    /** Adds an item if it is new; returns false if it was already chosen. */
    boolean add(T item) {
        if (item == null || items.contains(item)) return false;
        items.add(item);
        redraw();
        return true;
    }

    /** Adds several items at once, skipping ones already chosen. */
    int addAll(List<T> more) {
        int added = 0;
        for (T item : more) if (item != null && !items.contains(item)) { items.add(item); added++; }
        redraw();
        return added;
    }

    void clear() { items.clear(); redraw(); }

    /** Preferred size with wrapping: lays the chips out in rows no wider than MAX_WIDTH. */
    @Override public Dimension getPreferredSize() {
        FlowLayout fl = (FlowLayout) getLayout();
        int rowWidth = 0, rowHeight = 0, height = fl.getVgap(), widest = 0;
        for (Component c : getComponents()) {
            Dimension d = c.getPreferredSize();
            if (rowWidth > 0 && rowWidth + fl.getHgap() + d.width > MAX_WIDTH) {
                height += rowHeight + fl.getVgap();
                rowWidth = 0; rowHeight = 0;
            }
            rowWidth += (rowWidth > 0 ? fl.getHgap() : fl.getHgap()) + d.width;
            rowHeight = Math.max(rowHeight, d.height);
            widest = Math.max(widest, rowWidth);
        }
        height += rowHeight + fl.getVgap();
        return new Dimension(Math.min(MAX_WIDTH, widest + fl.getHgap()), getComponentCount() == 0 ? 0 : height);
    }

    @Override public Dimension getMaximumSize() { return getPreferredSize(); }

    private void redraw() {
        removeAll();
        int shown = 0;
        for (T item : new ArrayList<>(items)) {
            if (shown++ == MAX_VISIBLE) break;
            String label = text.apply(item);
            if (label.length() > 28) label = label.substring(0, 26) + "...";
            JButton chip = new JButton(label + "  \u00d7");
            chip.setToolTipText(text.apply(item));
            chip.addActionListener(e -> { items.remove(item); redraw(); });
            add(chip);
        }
        if (items.size() > MAX_VISIBLE) {
            JLabel more = Ui.muted(Messages.get("form.more", items.size() - MAX_VISIBLE));
            StringBuilder tip = new StringBuilder("<html>");
            for (T item : items) tip.append(Ui.escape(text.apply(item))).append("<br>");
            more.setToolTipText(tip.append("</html>").toString());
            add(more);
        }
        if (items.size() > 1) {
            JButton clear = new JButton(Messages.get("form.clearAll"));
            clear.addActionListener(e -> clear());
            add(clear);
        }
        revalidate();
        repaint();
        onChange.run();
    }
}

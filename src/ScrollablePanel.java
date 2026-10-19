import javax.swing.*;
import java.awt.*;

/** A panel that always fits the width of its scroll pane, so long text wraps instead of scrolling sideways. */
class ScrollablePanel extends JPanel implements Scrollable {
    ScrollablePanel() { setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); }
    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 20; }
    @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 200; }
    @Override public boolean getScrollableTracksViewportWidth() { return true; }
    @Override public boolean getScrollableTracksViewportHeight() { return false; }
}

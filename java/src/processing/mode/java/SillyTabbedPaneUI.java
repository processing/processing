package processing.mode.java;


import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;

public class SillyTabbedPaneUI extends BasicTabbedPaneUI {
  static final int HIGH = 30;

  // controls the ugly extra amount at the top
  static private final Insets NO_INSETS = new Insets(0, 0, 0, 0);

//	private Font selectedFont;
//	private FontMetrics selectedMetrics;

//	/**
//	 * The color to use to fill in the background
//	 */
//	private Color fillColor;


	static public ComponentUI createUI(JComponent c) {
		return new SillyTabbedPaneUI();
	}


	protected void installDefaults() {
		super.installDefaults();

		tabAreaInsets.left = 4;
		selectedTabPadInsets = new Insets(0, 0, 0, 0);
		tabInsets = selectedTabPadInsets;

//		tabPane.setBackground(Color.CYAN);
//		Color background = tabPane.getBackground();
//		fillColor = background.darker();

//		selectedFont = tabPane.getFont().deriveFont(Font.BOLD);
//		selectedMetrics = tabPane.getFontMetrics(selectedFont);
	}


	public int getTabRunCount(JTabbedPane pane) {
		return 1;
	}


	protected Insets getContentBorderInsets(int tabPlacement) {
		return NO_INSETS;
	}


	protected int calculateTabHeight(int tabPlacement, int tabIndex, int fontHeight) {
	  return HIGH;
//		int vHeight = fontHeight;
//		if (vHeight % 2 > 0) {
//			vHeight++;
//		}
//		return vHeight;
	}


	protected int calculateTabWidth(int tabPlacement, int tabIndex, FontMetrics metrics) {
		return super.calculateTabWidth(tabPlacement, tabIndex, metrics) + metrics.getHeight();
	}


	protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
	                                  int x, int y, int w, int h, boolean isSelected) {
	  g.setColor(isSelected ? Color.RED : Color.ORANGE);
	  g.fillRect(x, y, w, h);
	  /*
		Polygon shape = new Polygon();

		shape.addPoint(x, y + h);
		shape.addPoint(x, y);
		shape.addPoint(x + w - (h / 2), y);

		if (isSelected || (tabIndex == (rects.length - 1)))
		{
			shape.addPoint(x + w + (h / 2), y + h);
		}
		else
		{
			shape.addPoint(x + w, y + (h / 2));
			shape.addPoint(x + w, y + h);
		}

		g.setColor(tabPane.getBackground());
		g.fillPolygon(shape);
		*/
	}


	/** Paint the border of an individual tab */
	protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex,
	                              int x, int y, int w, int h, boolean isSelected) {
	}


	protected void paintContentBorderTopEdge(Graphics g, int tabPlacement, int selectedIndex,
	                                         int x, int y, int w, int h) {
	  /*
		Rectangle selectedRect = selectedIndex < 0 ? null : getTabBounds(selectedIndex, calcRect);

		selectedRect.width = selectedRect.width + (selectedRect.height / 2) - 1;

		g.setColor(Color.BLACK);

		g.drawLine(x, y, selectedRect.x, y);
		g.drawLine(selectedRect.x + selectedRect.width + 1, y, x + w, y);

		g.setColor(Color.WHITE);

		g.drawLine(x, y + 1, selectedRect.x, y + 1);
		g.drawLine(selectedRect.x + 1, y + 1, selectedRect.x + 1, y);
		g.drawLine(selectedRect.x + selectedRect.width + 2, y + 1, x + w, y + 1);

		g.setColor(shadow);
		g.drawLine(selectedRect.x + selectedRect.width, y, selectedRect.x + selectedRect.width + 1, y + 1);
		*/
	}


	protected void paintContentBorderRightEdge(Graphics g, int tabPlacement, int selectedIndex,
	                                           int x, int y, int w, int h) {
	}


	protected void paintContentBorderLeftEdge(Graphics g, int tabPlacement, int selectedIndex,
	                                          int x, int y, int w, int h) {
	}


	protected void paintContentBorderBottomEdge(Graphics g, int tabPlacement, int selectedIndex,
	                                            int x, int y, int w, int h) {
	}


	protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects,
	                                   int tabIndex, Rectangle iconRect,
	                                   Rectangle textRect, boolean isSelected) {
	}


	protected void paintTabArea(Graphics g, int tabPlacement, int selectedIndex) {
		int tw = tabPane.getBounds().width;

//		g.setColor(fillColor);
		g.setColor(Color.YELLOW);
		g.fillRect(0, 0, tw, rects[0].height + 3);

		super.paintTabArea(g, tabPlacement, selectedIndex);
	}


	/*
	protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics, int tabIndex, String title, Rectangle textRect, boolean isSelected)
	{
		if (isSelected)
		{
			int vDifference = (int)(selectedMetrics.getStringBounds(title,g).getWidth()) - textRect.width;
			textRect.x -= (vDifference / 2);
			super.paintText(g, tabPlacement, selectedFont, selectedMetrics, tabIndex, title, textRect, isSelected);
		}
		else
		{
			super.paintText(g, tabPlacement, font, metrics, tabIndex, title, textRect, isSelected);
		}
	}
	*/

	protected int getTabLabelShiftY(int tabPlacement, int tabIndex, boolean isSelected)
	{
		return 0;
	}
}

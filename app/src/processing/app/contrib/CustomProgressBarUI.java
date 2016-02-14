package processing.app.contrib;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.plaf.basic.BasicProgressBarUI;

import processing.app.ui.Toolkit;

public class CustomProgressBarUI extends BasicProgressBarUI {
  
  // The width of the border
  int iStrokWidth = 0;
  Color backgroundColor;
  Color foregroundColor;
  Color stringColor;
  
  public CustomProgressBarUI() {
    super();
  }
  

  public CustomProgressBarUI(Color backgroundColor, Color foregroundColor,
                             Color stringColor) {
    super();
    this.backgroundColor = backgroundColor;
    this.foregroundColor = foregroundColor;
    this.stringColor = stringColor;
  }


  @Override
  public void paint(Graphics g, JComponent c) {
    Graphics2D g2d = (Graphics2D) g.create();
    if (backgroundColor != null) {
      g2d.setPaint(backgroundColor);
      g2d.fillRect(0 + iStrokWidth, 0 + iStrokWidth, progressBar.getWidth()
        - iStrokWidth, progressBar.getHeight() - iStrokWidth);
    }
    if (progressBar.isIndeterminate()) {
      paintIndeterminate(g, c);
    } else {
      paintDeterminate(g, c);
    }
  }
  @Override
  public void paintDeterminate(Graphics g, JComponent c) {

      progressBar.setFont(Toolkit.getSansFont(14, Font.PLAIN));
      progressBar.setForeground(foregroundColor);
      Graphics2D g2d = (Graphics2D) g.create();
      Insets b = progressBar.getInsets();
      int barRectWidth  = progressBar.getWidth()  - b.right - b.left;
      int barRectHeight = progressBar.getHeight() - b.top - b.bottom;
      if (barRectWidth <= 0 || barRectHeight <= 0) {
        return;
      }

      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      g2d.setStroke(new BasicStroke(iStrokWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      g2d.setColor(progressBar.getBackground());
      g2d.setBackground(progressBar.getBackground());

      int width = progressBar.getWidth();
      int height = progressBar.getHeight();
      
      // Painting a border
      RoundRectangle2D outline = new RoundRectangle2D.Double((iStrokWidth / 2), (iStrokWidth / 2),
              width - iStrokWidth, height - iStrokWidth,
              0, 0);

      g2d.draw(outline);

      int iInnerHeight = height - (iStrokWidth * 4);
      int iInnerWidth = width - (iStrokWidth * 4);

      double dProgress = progressBar.getPercentComplete();
      if (dProgress < 0) {
          dProgress = 0;
      } else if (dProgress > 1) {
          dProgress = 1;
      }

      iInnerWidth = (int) Math.round(iInnerWidth * dProgress);


      // To create a gradient in the progressbar 
//      int x = iStrokWidth * 2;
//      int y = iStrokWidth * 2;
//      Point2D start = new Point2D.Double(x, y);
//      Point2D end = new Point2D.Double(x, y + iInnerHeight);
      
//      float[] dist = {0.0f, 0.25f, 1.0f};
//      Color[] colors = {progressBar.getBackground(), progressBar.getBackground().brighter(), progressBar.getBackground().darker()};
//      LinearGradientPaint p = new LinearGradientPaint(start, end, dist, colors);
//      g2d.setPaint(p);

      g2d.setPaint(foregroundColor);
      RoundRectangle2D fill = new RoundRectangle2D.Double(iStrokWidth * 2, iStrokWidth * 2,
              iInnerWidth, iInnerHeight, 0, 0);

      g2d.fill(fill);
      
      // Deal with possible text painting
      if (progressBar.isStringPainted()) {
        paintString(g, b.left, b.top, barRectWidth, barRectHeight, 0, b);
      }

      g2d.dispose();
  }

  @Override
  protected Color getSelectionBackground() {
    return stringColor;
  }
  @Override
  protected Color getSelectionForeground() {
    return stringColor;
  }
}

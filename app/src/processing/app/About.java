package processing.app;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


public class About extends Window {
  Image image;
  int width, height;


  public About(Frame frame) {
    super(frame);

    if (Toolkit.highResDisplay()) {
      image = Toolkit.getLibImage("about-2x.jpg"); //$NON-NLS-1$
      width = image.getWidth(null) / 2;
      height = image.getHeight(null) / 2;
    } else {
      image = Toolkit.getLibImage("about.jpg"); //$NON-NLS-1$
      width = image.getWidth(null);
      height = image.getHeight(null);
    }

    addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
        dispose();
      }
    });

    Dimension screen = Toolkit.getScreenSize();
    setBounds((screen.width-width)/2, (screen.height-height)/2, width, height);
    setVisible(true);
  }


  public void paint(Graphics g) {
    g.drawImage(image, 0, 0, width, height, null);

    Graphics2D g2 = (Graphics2D) g;
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

    g.setFont(new Font("SansSerif", Font.PLAIN, 10)); //$NON-NLS-1$
    g.setColor(Color.white);
    g.drawString(Base.getVersionName(), 90, 29);
  }
}
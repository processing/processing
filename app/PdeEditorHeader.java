import java.awt.*;
import java.awt.event.*;


    /*
    PdeEditorLabel sketchLabel = new PdeEditorLabel(1);
    Color sketchBgColor = new Color(51, 51, 51);
    Color sketchPrimaryTextColor = Color.white;
    Color sketchSecondaryTextColor = new Color(153, 153, 153);
    sketchLabel.setForeground(sketchPrimaryTextColor);
    sketchLabel.setBackground(sketchBgColor);
    rightPanel.add("North", sketchLabel);
    */

public class PdeEditorHeader extends Panel /* implements ActionListener*/ {
  static final String PROJECT_TITLER = "sketch";
  static final String USER_TITLER = "user";

  //static final Color primaryColor = Color.white;
  //static final Color secondaryColor = new Color(153, 153, 153);
  //static final Color backgroundColor = new Color(51, 51, 51);

  static Color primaryColor; // = Color.white;
  static Color secondaryColor; // = new Color(153, 153, 153);
  static Color backgroundColor; // = new Color(51, 51, 51);

  PdeEditor editor;

  String project;
  int projectLeft;
  int projectRight;
  int projectTitleLeft;

  String user;
  int userLeft;
  int userRight;
  int userTitleLeft;

  Font font;
  FontMetrics metrics;
  int fontAscent;

  Image offscreen;
  int sizeW, sizeH;
  int imageW, imageH;


  public PdeEditorHeader(PdeEditor editor, String project, String user) {
    this.editor = editor;
    this.project = project;
    this.user = user;

    if (primaryColor == null) {
      backgroundColor = PdeBase.getColor("editor.header.bgcolor", 
					   new Color(51, 51, 51));
      primaryColor = PdeBase.getColor("editor.header.fgcolor.primary", 
					new Color(255, 255, 255));
      secondaryColor = PdeBase.getColor("editor.header.fgcolor.secondary", 
					  new Color(153, 153, 153));
    }
  }


  public void setProject(String project) {
    this.project = project;
    projectLeft = 0;
  }

  public void setUser(String user) {
    this.user = user;
    userLeft = 0;
  }


  public void update() {
    paint(this.getGraphics());
  }

  public void update(Graphics g) {
    paint(g);
  }

  public void paint(Graphics screen) {
    if (screen == null) return;

    Dimension size = getSize();
    if ((size.width != sizeW) || (size.height != sizeH)) {
      // component has been resized

      if ((size.width > imageW) || (size.height > imageH)) {
	// nix the image and recreate, it's too small
	offscreen = null;

      } else {
	// who cares, just resize
	sizeW = size.width; 
	sizeH = size.height;
	userLeft = 0; // reset
      }
    }

    if (offscreen == null) {
      sizeW = size.width;
      sizeH = size.height;
      userLeft = 0; // reset
      imageW = sizeW;
      imageH = sizeH;
      offscreen = createImage(imageW, imageH);
    }

    Graphics g = offscreen.getGraphics();
    if (font == null) {
      //font = new Font("SansSerif", Font.PLAIN, 12);
      font = PdeBase.getFont("editor.header.font",
			       new Font("SansSerif", Font.PLAIN, 12));
      g.setFont(font);
      metrics = g.getFontMetrics();
      fontAscent = metrics.getAscent();
    }

    if (projectLeft == 0) {
      projectTitleLeft = PdeEditor.INSET_SIZE;
      projectLeft = projectTitleLeft + 
	metrics.stringWidth(PROJECT_TITLER) + PdeEditor.INSET_SIZE;
    }

    if (userLeft == 0) {
      userLeft = sizeW - 20 - metrics.stringWidth(user);
      userTitleLeft = userLeft - PdeEditor.INSET_SIZE - 
	metrics.stringWidth(USER_TITLER);
    }

    int baseline = (sizeH + fontAscent) / 2;

    g.setColor(backgroundColor);
    g.fillRect(0, 0, imageW, imageH);

    g.setColor(secondaryColor);
    g.drawString(PROJECT_TITLER, projectTitleLeft, baseline);
    g.drawString(USER_TITLER, userTitleLeft, baseline);

    g.setColor(primaryColor);
    g.drawString(project, projectLeft, baseline);
    g.drawString(user, userLeft, baseline);

    //g.setColor(fgColor[mode]);
    //g.drawString(message, PdeEditor.INSET_SIZE, (sizeH + fontAscent) / 2);

    screen.drawImage(offscreen, 0, 0, null);
  }


  /*
  protected void setButtonBounds() {
    int top = (sizeH - BUTTON_HEIGHT) / 2;
    int noLeft = sizeW - PdeEditor.INSET_SIZE - BUTTON_WIDTH;
    int yesLeft = noLeft - PdeEditor.INSET_SIZE - BUTTON_WIDTH;

    noButton.setBounds(noLeft, top, BUTTON_WIDTH, BUTTON_HEIGHT);
    yesButton.setBounds(yesLeft, top, BUTTON_WIDTH, BUTTON_HEIGHT);
  }
  */


  public Dimension getPreferredSize() {
    return new Dimension(300, PdeEditor.GRID_SIZE);
  }


  /*
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == noButton) {
      System.out.println("clicked no");

    } else if (e.getSource() == yesButton) {
      System.out.println("clicked yes");
    }
  }
  */
}


  /*
  Color noticeBgColor = new Color(102, 102, 102);
  Color noticeFgColor = new Color(255, 255, 255);

  Color errorBgColor = new Color(102, 26, 0);
  Color errorFgColor = new Color(255, 255, 255);

  Color promptBgColor = new Color(204, 153, 0);
  Color promptFgColor = new COlor(0, 0, 0);
  */

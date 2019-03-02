/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyirght (c) 2012-19 The Processing Foundation
  Copyright (c) 2004-12 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, version 2.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.app.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import processing.app.Base;
import processing.app.Language;
import processing.app.Mode;


/**
 * Run/Stop button plus Mode selection
 */
abstract public class EditorToolbar extends JPanel implements KeyListener {
  // haven't decided how to handle this/how to make public/consistency
  // for components/does it live in theme.txt
  static final int HIGH = Toolkit.zoom(53);
  // horizontal gap between buttons
  static final int GAP = Toolkit.zoom(9);
  // corner radius on the mode selector
  static final int RADIUS = Toolkit.zoom(3);

  protected Editor editor;
  protected Base base;
  protected Mode mode;

  protected EditorButton runButton;
  protected EditorButton stopButton;

  protected EditorButton rolloverButton;
  protected JLabel rolloverLabel;

  protected Box box;

  protected Image gradient;


  public EditorToolbar(Editor editor) {
    this.editor = editor;
    base = editor.getBase();
    mode = editor.getMode();

    gradient = mode.makeGradient("toolbar", Toolkit.zoom(400), HIGH);

    rebuild();
  }


  public void rebuild() {
    removeAll();  // remove previous components, if any
    List<EditorButton> buttons = createButtons();

    box = Box.createHorizontalBox();
    box.add(Box.createHorizontalStrut(Editor.LEFT_GUTTER));

    rolloverLabel = new JLabel();
    rolloverLabel.setFont(mode.getFont("toolbar.rollover.font"));
    rolloverLabel.setForeground(mode.getColor("toolbar.rollover.color"));

    for (EditorButton button : buttons) {
      box.add(button);
      box.add(Box.createHorizontalStrut(GAP));
//      registerButton(button);
    }
//    // remove the last gap
//    box.remove(box.getComponentCount() - 1);

//    box.add(Box.createHorizontalStrut(LABEL_GAP));
    box.add(rolloverLabel);
//    currentButton = runButton;

//    runButton.setRolloverLabel(label);
//    stopButton.setRolloverLabel(label);

    box.add(Box.createHorizontalGlue());
    addModeButtons(box, rolloverLabel);
//    Component items = createModeButtons();
//    if (items != null) {
//      box.add(items);
//    }
    ModeSelector ms = new ModeSelector();
    box.add(ms);
    box.add(Box.createHorizontalStrut(Editor.RIGHT_GUTTER));

    setLayout(new BorderLayout());
    add(box, BorderLayout.CENTER);
  }


//  public void registerButton(EditorButton button) {
    //button.setRolloverLabel(rolloverLabel);
    //editor.getTextArea().addKeyListener(button);
//  }


//  public void setReverse(EditorButton button) {
//    button.setGradient(reverseGradient);
//  }


//  public void setText(String text) {
//    label.setText(text);
//  }


  public void paintComponent(Graphics g) {
    Dimension size = getSize();
    g.drawImage(gradient, 0, 0, size.width, size.height, this);
  }


  public List<EditorButton> createButtons() {
    runButton = new EditorButton(this,
                                 "/lib/toolbar/run",
                                 Language.text("toolbar.run"),
                                 Language.text("toolbar.present")) {
      @Override
      public void actionPerformed(ActionEvent e) {
        handleRun(e.getModifiers());
      }
    };

    stopButton = new EditorButton(this,
                                  "/lib/toolbar/stop",
                                  Language.text("toolbar.stop")) {
      @Override
      public void actionPerformed(ActionEvent e) {
        handleStop();
      }
    };
    return new ArrayList<>(Arrays.asList(runButton, stopButton));
  }


  public void addModeButtons(Box box, JLabel label) {
  }


  public void addGap(Box box) {
    box.add(Box.createHorizontalStrut(GAP));
  }


//  public Component createModeSelector() {
//    return new ModeSelector();
//  }


//  protected void swapButton(EditorButton replacement) {
//    if (currentButton != replacement) {
//      box.remove(currentButton);
//      box.add(replacement, 1);  // has to go after the strut
//      box.revalidate();
//      box.repaint();  // may be needed
//      currentButton = replacement;
//    }
//  }


  public void activateRun() {
    runButton.setSelected(true);
    repaint();
  }


  public void deactivateRun() {
    runButton.setSelected(false);
    repaint();
  }


  public void activateStop() {
    stopButton.setSelected(true);
    repaint();
  }


  public void deactivateStop() {
    stopButton.setSelected(false);
    repaint();
  }


  abstract public void handleRun(int modifiers);


  abstract public void handleStop();


  void setRollover(EditorButton button, InputEvent e) {
    rolloverButton = button;
//    if (rolloverButton != null) {
    updateRollover(e);
//    } else {
//      rolloverLabel.setText("");
//    }
  }


  void updateRollover(InputEvent e) {
    if (rolloverButton != null) {
      rolloverLabel.setText(rolloverButton.getRolloverText(e));
    } else {
      rolloverLabel.setText("");
    }
  }


  @Override
  public void keyTyped(KeyEvent e) { }


  @Override
  public void keyReleased(KeyEvent e) {
    updateRollover(e);
  }


  @Override
  public void keyPressed(KeyEvent e) {
    updateRollover(e);
  }


  public Dimension getPreferredSize() {
    return new Dimension(super.getPreferredSize().width, HIGH);
  }


  public Dimension getMinimumSize() {
    return new Dimension(super.getMinimumSize().width, HIGH);
  }


  public Dimension getMaximumSize() {
    return new Dimension(super.getMaximumSize().width, HIGH);
  }


  class ModeSelector extends JPanel {
    Image offscreen;
    int width, height;

    String title;
    Font titleFont;
    Color titleColor;
    int titleAscent;
    int titleWidth;

    final int MODE_GAP_WIDTH = Toolkit.zoom(13);
    final int ARROW_GAP_WIDTH = Toolkit.zoom(6);
    final int ARROW_WIDTH = Toolkit.zoom(6);
    final int ARROW_TOP = Toolkit.zoom(12);
    final int ARROW_BOTTOM = Toolkit.zoom(18);

    int[] triangleX = new int[3];
    int[] triangleY = new int[] { ARROW_TOP, ARROW_TOP, ARROW_BOTTOM };

//    Image background;
    Color backgroundColor;
    Color outlineColor;


    @SuppressWarnings("deprecation")
    public ModeSelector() {
      title = mode.getTitle(); //.toUpperCase();
      titleFont = mode.getFont("mode.title.font");
      titleColor = mode.getColor("mode.title.color");

      // getGraphics() is null and no offscreen yet
      titleWidth = getToolkit().getFontMetrics(titleFont).stringWidth(title);

      addMouseListener(new MouseAdapter() {
        public void mousePressed(MouseEvent event) {
          JPopupMenu popup = editor.getModePopup();
          popup.show(ModeSelector.this, event.getX(), event.getY());
        }
      });

      //background = mode.getGradient("reversed", 100, EditorButton.DIM);
      backgroundColor = mode.getColor("mode.background.color");
      outlineColor = mode.getColor("mode.outline.color");
    }

    @Override
    public void paintComponent(Graphics screen) {
//      Toolkit.debugOpacity(this);

      Dimension size = getSize();
      width = 0;
      if (width != size.width || height != size.height) {
        offscreen = Toolkit.offscreenGraphics(this, size.width, size.height);
        width = size.width;
        height = size.height;
      }

      Graphics g = offscreen.getGraphics();
      Graphics2D g2 = Toolkit.prepareGraphics(g);
      //Toolkit.clearGraphics(g, width, height);
//      g.clearRect(0, 0, width, height);
//      g.setColor(Color.GREEN);
//      g.fillRect(0, 0, width, height);

      g.setFont(titleFont);
      if (titleAscent == 0) {
        titleAscent = (int) Toolkit.getAscent(g); //metrics.getAscent();
      }
      FontMetrics metrics = g.getFontMetrics();
      titleWidth = metrics.stringWidth(title);

      // clear the background
      g.setColor(backgroundColor);
      g.fillRect(0, 0, width, height);

      // draw the outline for this feller
      g.setColor(outlineColor);
      //Toolkit.dpiStroke(g2);
      g2.draw(Toolkit.createRoundRect(1, 1, width-1, height-1,
                                      RADIUS, RADIUS, RADIUS, RADIUS));

      g.setColor(titleColor);
      g.drawString(title, MODE_GAP_WIDTH, (height + titleAscent) / 2);

      int x = MODE_GAP_WIDTH + titleWidth + ARROW_GAP_WIDTH;
      triangleX[0] = x;
      triangleX[1] = x + ARROW_WIDTH;
      triangleX[2] = x + ARROW_WIDTH/2;
      g.fillPolygon(triangleX, triangleY, 3);

      screen.drawImage(offscreen, 0, 0, width, height, this);
    }

    @Override
    public Dimension getPreferredSize() {
      return new Dimension(MODE_GAP_WIDTH + titleWidth +
                           ARROW_GAP_WIDTH + ARROW_WIDTH + MODE_GAP_WIDTH,
                           EditorButton.DIM);
    }

    @Override
    public Dimension getMinimumSize() {
      return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
      return getPreferredSize();
    }
  }
}
/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2013-15 The Processing Foundation
  Copyright (c) 2010-13 Ben Fry and Casey Reas

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License version 2
  as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.mode.java;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;

import processing.app.Editor;
import processing.app.EditorButton;
import processing.app.EditorToolbar;
import processing.app.Language;


public class JavaToolbar extends EditorToolbar {
  JavaEditor jeditor;

//  boolean debug;  // true if this is the expanded debug feller
  EditorButton stepButton;
  EditorButton continueButton;


//  public JavaToolbar(Editor editor, boolean debug) {
  public JavaToolbar(Editor editor) {
    super(editor);
//    this.debug = debug;
    jeditor = (JavaEditor) editor;
  }


  @Override
  public List<EditorButton> createButtons() {
    // jeditor not ready yet because this is called by super()
    final boolean debug = ((JavaEditor) editor).isDebuggerEnabled();
//    System.out.println("creating buttons in JavaToolbar, debug:" + debug);
    List<EditorButton> outgoing = new ArrayList<>();

    final String runText = debug ?
      Language.text("toolbar.debug") : Language.text("toolbar.run");
    runButton = new EditorButton(mode,
                                 "/lib/toolbar/run",
                                 runText,
                                 Language.text("toolbar.present")) {
      @Override
      public void actionPerformed(ActionEvent e) {
        handleRun(e.getModifiers());
      }
    };
    outgoing.add(runButton);

    if (debug) {
      stepButton = new EditorButton(mode,
                                    "/lib/toolbar/step",
                                    Language.text("menu.debug.step"),
                                    Language.text("menu.debug.step_into"),
                                    Language.text("menu.debug.step_out")) {
        @Override
        public void actionPerformed(ActionEvent e) {
          final int mask = KeyEvent.SHIFT_DOWN_MASK | KeyEvent.ALT_DOWN_MASK;
          jeditor.handleStep(e.getModifiers() & mask);
        }
      };
      outgoing.add(stepButton);

      continueButton = new EditorButton(mode,
                                        "/lib/toolbar/continue",
                                        Language.text("menu.debug.continue")) {
        @Override
        public void actionPerformed(ActionEvent e) {
          jeditor.handleContinue();
        }
      };
      outgoing.add(continueButton);
    }

    stopButton = new EditorButton(mode,
                                  "/lib/toolbar/stop",
                                  Language.text("toolbar.stop")) {
      @Override
      public void actionPerformed(ActionEvent e) {
        handleStop();
      }
    };
    outgoing.add(stopButton);

    return outgoing;
  }


  public void addModeButtons(Box box) {
    EditorButton debugButton = new EditorButton(mode, "/lib/toolbar/debug",
                                                Language.text("toolbar.debug")) {

      @Override
      public void actionPerformed(ActionEvent e) {
        jeditor.toggleDebug();
      }
    };
    debugButton.setReverse();
    box.add(debugButton);
    addGap(box);
  }


  @Override
  public void handleRun(int modifiers) {
    boolean shift = (modifiers & InputEvent.SHIFT_MASK) != 0;
    if (shift) {
      jeditor.handlePresent();
    } else {
      jeditor.handleRun();
    }
  }


  @Override
  public void handleStop() {
    jeditor.handleStop();
  }


  public void activateContinue() {
    continueButton.setSelected(true);
    repaint();
  }


  protected void deactivateContinue() {
    continueButton.setSelected(false);
    repaint();
  }


  protected void activateStep() {
    stepButton.setSelected(true);
    repaint();
  }


  protected void deactivateStep() {
    stepButton.setSelected(false);
    repaint();
  }
}


/*
public class JavaToolbar extends EditorToolbar {
  static protected final int RUN    = 0;
  static protected final int STOP   = 1;


  public JavaToolbar(Editor editor, Base base) {
    super(editor, base);
  }


  public void init() {
    Image[][] images = loadImages();
    for (int i = 0; i < 2; i++) {
      addButton(getTitle(i, false), getTitle(i, true), images[i], false);
    }
  }


  static public String getTitle(int index, boolean shift) {
    switch (index) {
    case RUN: return !shift ? Language.text("toolbar.run") : Language.text("toolbar.present");
    case STOP: return Language.text("toolbar.stop");
    }
    return null;
  }


  public void handlePressed(MouseEvent e, int sel) {
    boolean shift = e.isShiftDown();
    JavaEditor jeditor = (JavaEditor) editor;

    switch (sel) {
    case RUN:
      if (shift) {
        jeditor.handlePresent();
      } else {
        jeditor.handleRun();
      }
      break;

    case STOP:
      jeditor.handleStop();
      break;
    }
  }
}
*/
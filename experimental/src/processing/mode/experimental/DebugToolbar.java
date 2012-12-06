/*
 * Copyright (C) 2012 Martin Leopold <m@martinleopold.com>
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package processing.mode.experimental;

import java.awt.Image;
import java.awt.event.MouseEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import processing.app.Base;
import processing.app.Editor;
import processing.mode.java.JavaToolbar;

/**
 * Custom toolbar for the editor window. Preserves original button numbers
 * ({@link JavaToolbar#RUN}, {@link JavaToolbar#STOP}, {@link JavaToolbar#NEW},
 * {@link JavaToolbar#OPEN}, {@link JavaToolbar#SAVE}, {@link JavaToolbar#EXPORT})
 * which can be used e.g. in {@link #activate} and
 * {@link #deactivate}.
 *
 * @author Martin Leopold <m@martinleopold.com>
 */
public class DebugToolbar extends JavaToolbar {
  // preserve original button id's, but re-define so they are accessible 
  // (they are used by DebugEditor, so they want to be public)

  static protected final int RUN    = 100; // change this, to be able to get it's name via getTitle()
  static protected final int DEBUG = JavaToolbar.RUN;

  static protected final int CONTINUE = 101;
  static protected final int STEP = 102;
  static protected final int TOGGLE_BREAKPOINT = 103;
  static protected final int TOGGLE_VAR_INSPECTOR = 104;

  static protected final int STOP   = JavaToolbar.STOP;

  static protected final int NEW    = JavaToolbar.NEW;
  static protected final int OPEN   = JavaToolbar.OPEN;
  static protected final int SAVE   = JavaToolbar.SAVE;
  static protected final int EXPORT = JavaToolbar.EXPORT;


  // the sequence of button ids. (this maps button position = index to button ids)
  static protected final int[] buttonSequence = {
    DEBUG, CONTINUE, STEP, STOP, TOGGLE_BREAKPOINT, TOGGLE_VAR_INSPECTOR, 
    NEW, OPEN, SAVE, EXPORT
  }; 

  
  public DebugToolbar(Editor editor, Base base) {
    super(editor, base);
  }

  
  /**
   * Initialize buttons. Loads images and adds the buttons to the toolbar.
   */
  @Override
  public void init() {
    Image[][] images = loadImages();
    for (int idx = 0; idx < buttonSequence.length; idx++) {
      int id = buttonId(idx);
      addButton(getTitle(id, false), getTitle(id, true), images[idx], id == NEW || id == TOGGLE_BREAKPOINT);
    }
  }

  
  /**
   * Get the title for a toolbar button. Displayed in the toolbar when
   * hovering over a button.
   * @param id id of the toolbar button
   * @param shift true if shift is pressed
   * @return the title
   */
  public static String getTitle(int id, boolean shift) {
    switch (id) {
    case DebugToolbar.RUN:
      return JavaToolbar.getTitle(JavaToolbar.RUN, shift);
    case STOP:
      return JavaToolbar.getTitle(JavaToolbar.STOP, shift);
    case NEW:
      return JavaToolbar.getTitle(JavaToolbar.NEW, shift);
    case OPEN:
      return JavaToolbar.getTitle(JavaToolbar.OPEN, shift);
    case SAVE:
      return JavaToolbar.getTitle(JavaToolbar.SAVE, shift);
    case EXPORT:
      return JavaToolbar.getTitle(JavaToolbar.EXPORT, shift);
    case DEBUG:
      if (shift) {
        return "Run";
      } else {
        return "Debug";
      }
    case CONTINUE:
      return "Continue";
    case TOGGLE_BREAKPOINT:
      return "Toggle Breakpoint";
    case STEP:
      if (shift) {
        return "Step Into";
      } else {
        return "Step";
      }
    case TOGGLE_VAR_INSPECTOR:
      return "Variable Inspector";
    }
    return null;
  }

  
  /**
   * Event handler called when a toolbar button is clicked.
   * @param e the mouse event
   * @param idx index (i.e. position) of the toolbar button clicked
   */
  @Override
  public void handlePressed(MouseEvent e, int idx) {
    boolean shift = e.isShiftDown();
    DebugEditor deditor = (DebugEditor) editor;
    int id = buttonId(idx); // convert index/position to button id

    switch (id) {
//            case DebugToolbar.RUN:
//                super.handlePressed(e, JavaToolbar.RUN);
//                break;
    case STOP:
      Logger.getLogger(DebugToolbar.class.getName()).log(Level.INFO, "Invoked 'Stop' toolbar button");
      super.handlePressed(e, JavaToolbar.STOP);
      break;
    case NEW:
      Logger.getLogger(DebugToolbar.class.getName()).log(Level.INFO, "Invoked 'New' toolbar button");
      super.handlePressed(e, JavaToolbar.NEW);
      break;
    case OPEN:
      Logger.getLogger(DebugToolbar.class.getName()).log(Level.INFO, "Invoked 'Open' toolbar button");
      super.handlePressed(e, JavaToolbar.OPEN);
      break;
    case SAVE:
      Logger.getLogger(DebugToolbar.class.getName()).log(Level.INFO, "Invoked 'Save' toolbar button");
      super.handlePressed(e, JavaToolbar.SAVE);
      break;
    case EXPORT:
      Logger.getLogger(DebugToolbar.class.getName()).log(Level.INFO, "Invoked 'Export' toolbar button");
      super.handlePressed(e, JavaToolbar.EXPORT);
      break;
    case DEBUG:
      if (shift) {
        Logger.getLogger(DebugToolbar.class.getName()).log(Level.INFO, "Invoked 'Run' toolbar button");
        deditor.handleRun();
      } else {
        Logger.getLogger(DebugToolbar.class.getName()).log(Level.INFO, "Invoked 'Debug' toolbar button");
        deditor.dbg.startDebug();
      }
      break;
    case CONTINUE:
      Logger.getLogger(DebugToolbar.class.getName()).log(Level.INFO, "Invoked 'Continue' toolbar button");
      deditor.dbg.continueDebug();
      break;
    case TOGGLE_BREAKPOINT:
      Logger.getLogger(DebugToolbar.class.getName()).log(Level.INFO, "Invoked 'Toggle Breakpoint' toolbar button");
      deditor.dbg.toggleBreakpoint();
      break;
    case STEP:
      if (shift) {
        Logger.getLogger(DebugToolbar.class.getName()).log(Level.INFO, "Invoked 'Step Into' toolbar button");
        deditor.dbg.stepInto();
      } else {
        Logger.getLogger(DebugToolbar.class.getName()).log(Level.INFO, "Invoked 'Step' toolbar button");
        deditor.dbg.stepOver();
      }
      break;
//            case STEP_INTO:
//                deditor.dbg.stepInto();
//                break;
//            case STEP_OUT:
//                deditor.dbg.stepOut();
//                break;
    case TOGGLE_VAR_INSPECTOR:
      Logger.getLogger(DebugToolbar.class.getName()).log(Level.INFO, "Invoked 'Variable Inspector' toolbar button");
      deditor.toggleVariableInspector();
      break;
    }
  }

  
  /**
   * Activate (light up) a button.
   * @param id the button id
   */
  @Override
  public void activate(int id) {
    //System.out.println("activate button idx: " + buttonIndex(id));
    super.activate(buttonIndex(id));
  }

  
  /**
   * Set a button to be inactive.
   * @param id the button id
   */
  @Override
  public void deactivate(int id) {
    //System.out.println("deactivate button idx: " + buttonIndex(id));
    super.deactivate(buttonIndex(id));
  }

  
  /**
   * Get button position (index) from it's id.
   * @param buttonId the button id
   * ({@link #RUN}, {@link #DEBUG}, {@link #CONTINUE}), {@link #STEP}, ...)
   * @return the button index
   */
  protected int buttonIndex(int buttonId) {
    for (int i = 0; i < buttonSequence.length; i++) {
      if (buttonSequence[i] == buttonId) {
        return i;
      }
    }
    return -1;
  }

  
  /**
   * Get the button id from its position (index).
   * @param buttonIdx the button index
   * @return the button id
   * ({@link #RUN}, {@link #DEBUG}, {@link #CONTINUE}), {@link #STEP}, ...)
   */
  protected int buttonId(int buttonIdx) {
    return buttonSequence[buttonIdx];
  }
}

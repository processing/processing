/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2010-11 Ben Fry and Casey Reas

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

import java.awt.Image;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;

import processing.app.Base;
import processing.app.Editor;
import processing.app.EditorToolbar;


public class JavaToolbar extends EditorToolbar {
  /** Rollover titles for each button. */
//  static final String title[] = {
//    "Run", "Stop", "New", "Open", "Save", "Export"
//  };
  
  /** Titles for each button when the shift key is pressed. */ 
//  static final String titleShift[] = {
//    "Present", "Stop", "New Editor Window", "Open in Another Window", "Save", "Export to Application"
//  };

  static protected final int RUN    = 0;
  static protected final int STOP   = 1;

  static protected final int NEW    = 2;
  static protected final int OPEN   = 3;
  static protected final int SAVE   = 4;
  static protected final int EXPORT = 5;

//  JPopupMenu popup;
//  JMenu menu;

  
  public JavaToolbar(Editor editor, Base base) {
    super(editor, base);
  }
  
  
  public void init() {
    Image[][] images = loadImages();
    for (int i = 0; i < 6; i++) {
      addButton(getTitle(i, false), getTitle(i, true), images[i], i == NEW);
    }
  }
  
  
  static public String getTitle(int index, boolean shift) {
    switch (index) {
    case RUN:    return !shift ? "Run" : "Present";
    case STOP:   return "Stop";
    case NEW:    return !shift ? "New" : "New Editor Window";
    case OPEN:   return !shift ? "Open" : "Open in Another Window";
    case SAVE:   return "Save";
    case EXPORT: return !shift ? "Export Application" : "Export Applet";
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

    case OPEN:
//      popup = menu.getPopupMenu();
      // TODO I think we need a longer chain of accessors here.
      JPopupMenu popup = editor.getMode().getToolbarMenu().getPopupMenu();
      popup.show(this, e.getX(), e.getY());
      break;

    case NEW:
      if (shift) {
        base.handleNew();
      } else {
        base.handleNewReplace();
      }
      break;

    case SAVE:
      jeditor.handleSaveRequest(false);
      break;

    case EXPORT:
      if (shift) {
        jeditor.handleExportApplication();
      } else {
        jeditor.handleExportApplet();
      }
      break;
    }
  }
}
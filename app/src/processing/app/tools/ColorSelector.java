/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2006-14 Ben Fry and Casey Reas

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

package processing.app.tools;

import processing.app.*;

import java.awt.Color;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;


/**
 * Color selector tool for the Tools menu.
 * <p/>
 * Using the keyboard shortcuts, you can copy/paste the values for the
 * colors and paste them into your program. We didn't do any sort of
 * auto-insert of colorMode() or fill() or stroke() code cuz we couldn't
 * decide on a good way to do this.. your contributions welcome).
 */
public class ColorSelector implements Tool {

  /**
   * Only create one instance, otherwise we'll have dozens of animation
   * threads going if you open/close a lot of editor windows.
   */
  private static volatile ColorChooser selector;

  private Editor editor;

  
  public String getMenuTitle() {
    return Language.text("menu.tools.color_selector");
  }


  public void init(Editor editor) {
    this.editor = editor;
  }


  public void run() {
    if (selector == null) {
      synchronized(ColorSelector.class) {
        if (selector == null) {
          selector = new ColorChooser(editor, false, Color.WHITE,
                                      Language.text("menu.edit.copy"),
                                      new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
              Clipboard clipboard = Toolkit.getSystemClipboard();
              clipboard.setContents(new StringSelection(selector.getHexColor()), null);
            }
          });
        }
      }
    }
    selector.show();
  }
}

/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-09 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.app;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;

import javax.swing.*;

import processing.app.syntax.*;
import processing.core.*;


/**
 * 
 */
public class LibraryManager {

  JFrame dialog;

  JLabel uriLabel;
  JTextField libraryUri;
  JButton installButton;
  
  // the calling editor, so updates can be applied

  Editor editor;

  public LibraryManager() {

    // setup dialog for the prefs

    //dialog = new JDialog(editor, "Preferences", true);
    dialog = new JFrame("Library Manager");
    dialog.setResizable(true);
    
    Base.setIcon(dialog);

    uriLabel = new JLabel("Library URI:");
    libraryUri = new JTextField(40);
    installButton = new JButton("Install");
    
    ActionListener installLibAction = new ActionListener() {
      
      public void actionPerformed(ActionEvent arg) {
        try {
          URI uri = new URI(libraryUri.getText());
          System.out.println("Installing library: " + uri);
        } catch (URISyntaxException e) {
          System.err.println("Malformed URI");
        }
        libraryUri.setText("");
      }
    };
    libraryUri.addActionListener(installLibAction);
    installButton.addActionListener(installLibAction);
    
    Container pane = dialog.getContentPane();
    BoxLayout boxLayout = new BoxLayout(pane, BoxLayout.Y_AXIS);
    pane.setLayout(boxLayout);
    
    Box horizontal = Box.createHorizontalBox();
    horizontal.add(Box.createVerticalStrut(2));
    horizontal.add(uriLabel);
    uriLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
    horizontal.add(Box.createVerticalStrut(5));
    
    horizontal.add(libraryUri);
    horizontal.add(installButton);
    
    pane.add(horizontal);
    
    dialog.pack();
    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    dialog.setLocation((screen.width - dialog.getWidth()) / 2,
                      (screen.height - dialog.getHeight()) / 2);
  }
  
  protected void showFrame(Editor editor) {
    this.editor = editor;
    dialog.setVisible(true);
  }
  
}

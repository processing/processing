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
          URL url = new URL(libraryUri.getText());
          System.out.println("Installing library: " + url);
          installLibrary(url);
        } catch (MalformedURLException e) {
          System.err.println("Malformed URL");
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
  
  protected void installLibrary(URL url) {
    try {
      String[] paths = url.getFile().split("/");
      if (paths.length != 0) {
        String fileName = paths[paths.length - 1];
        File tmpFolder = Base.createTempFolder(fileName.split("\\.")[0], "library");
        
        File libFile = new File(tmpFolder, fileName);
        libFile.setWritable(true);
      
        downloadFile(url, libFile);
      }
    } catch (IOException e) {
      Base.showError("Trouble creating temporary folder",
                     "Could not create a place to store libraries being downloaded.\n" +
                     "That's gonna prevent us from continuing.", e);
    }
  }
  
  /**
   * Returns true if the file was successfully downloaded, false otherwise
   */
  protected boolean downloadFile(URL source, File dest) {
    try {
      URLConnection urlConn = source.openConnection();
      urlConn.setConnectTimeout(1000);
      urlConn.setReadTimeout(1000);
      
      // String expectedType1 = "application/x-zip-compressed";
      // String expectedType2 = "application/zip";
      // String type = urlConn.getContentType();
      // if (expectedType1.equals(type) || expectedType2.equals(type)) {
      // }

      int fileSize = urlConn.getContentLength();
      InputStream in = urlConn.getInputStream();
      FileOutputStream out = new FileOutputStream(dest);
      
      byte[] b = new byte[256];
      int bytesDownloaded = 0, len;
      while ((len = in.read(b)) != -1) {
        int progress = (int) (100.0 * bytesDownloaded / fileSize );
        // System.out.println("Downloaded " + progress + "%");
        out.write(b, 0, len);
        bytesDownloaded += len;
      }
      out.close();
      // System.out.println("Done!");
    
      return true;
      
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    
    return false;
  }

  protected void showFrame(Editor editor) {
    this.editor = editor;
    dialog.setVisible(true);
  }
  
}

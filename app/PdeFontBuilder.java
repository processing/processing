/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdeFontBuilder - gui interface to font creation heaven/hell
  Part of the Processing project - http://Proce55ing.net

  Copyright (c) 2001-03 Massachusetts Institute of Technology

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

import java.awt.*;
import java.awt.event.*;
import java.io.*;
//import java.net.*;
//import java.text.*;
//import java.util.*;
//import java.util.zip.*;

import javax.swing.*;
import javax.swing.event.*;


public class PdeFontBuilder extends JFrame {
  File targetFolder;

  //JComboBox fontSelector;
  JList fontSelector;
  JComboBox styleSelector;
  JTextField sizeSelector;

  static final String styles[] = {
    "Plain", "Bold", "Italic", "Bold Italic" 
  };

  String fontName;


  // font.deriveFont(float size)

  public PdeFontBuilder(File targetFolder) {
    super("Create Font");

    this.targetFolder = targetFolder;

    Container pain = getContentPane();
    pain.setLayout(new BoxLayout(pain, BoxLayout.Y_AXIS));

    String labelText = 
      "Use this tool to create bitmap fonts for your program.\n" +
      "Select a font and size, and click 'OK' to generate a font\n" + 
      "and add it to the data folder of the current sketch.\n" +
      "The recommended size for 3D applications is 48 points.";

    //JLabel label = new JLabel(labelText);
    JTextArea textarea = new JTextArea(labelText);
    textarea.setFont(new Font("Dialog", Font.PLAIN, 12));
    pain.add(textarea);
    //pain.add(label);

    JPanel panel = new JPanel();
    //panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

    GraphicsEnvironment ge = 
      GraphicsEnvironment.getLocalGraphicsEnvironment();

    Font fonts[] = ge.getAllFonts();
    String families[] = ge.getAvailableFontFamilyNames();

    // don't care about families starting with . or #
    // also ignore dialog, dialoginput, monospaced, serif, sansserif

    /*
    for (int i = 0; i < fonts.length; i++) {
      //System.out.println(fonts[i]);
      if (fonts[i].getFontName().indexOf(fonts[i].getFamily()) != 0) {
        System.out.println(fonts[i]);
      }
    }
    */

    //fontSelector = new JComboBox();
    fontSelector = new JList(families); 
    JScrollPane fontScroller = new JScrollPane(fontSelector);
    panel.add(fontScroller);
    //fontSelector.setFont(new Font("SansSerif", Font.PLAIN, 10));
    /*
    fontSelector.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          fontName = e.getActionCommand();
        }
      });
    */
    //String list[] = Toolkit.getDefaultToolkit().getFontList();
    //for (int i = 0; i < list.length; i++) {
    /*
    for (int i = 0; i < families.length; i++) {
      //fontSelector.addItem(list[i]);
      fontSelector.addItem(families[i]);
    }
    panel.add(fontSelector);
    */

    styleSelector = new JComboBox();
    for (int i = 0; i < styles.length; i++) {
      styleSelector.addItem(styles[i]);
    }
    styleSelector.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          String command = e.getActionCommand();
          int style = Font.PLAIN;
          if (command.indexOf("Bold") != -1) { 
            style |= Font.BOLD;
          }
          if (command.indexOf("Italic") != -1) {
            style |= Font.ITALIC;
          }
        }
      });
    panel.add(styleSelector);

    sizeSelector = new JTextField("48");
    panel.add(sizeSelector);

    pain.add(panel);

    JPanel buttons = new JPanel();
    JButton cancelButton = new JButton("Cancel");
    JButton okButton = new JButton("OK");
    buttons.add(cancelButton);
    buttons.add(okButton);
    pain.add(buttons);

    pack();
    show();
  }
}

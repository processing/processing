/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdePreferences - controls user preferences
  Part of the Processing project - http://Proce55ing.net

  Copyright (c) 2001-03 
  Ben Fry, Massachusetts Institute of Technology and 
  Casey Reas, Interaction Design Institute Ivrea

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
import java.net.*;
import java.util.*;
import java.util.zip.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.*;

/*
#ifndef RXTX
import javax.comm.*;
#else
import gnu.io.*;
#endif
*/


/*
  need to bring all the prefs into here
  PdeEditor with its sketch.properties
  and PdeBase with pde.properties

  on first run:
  processing.properties is created in user.home
  it contains the contents of 
  pde.properties + pde_platform.properties
  and then begins writing additional sketch.properties stuff
 */
public class PdePreferences extends JComponent {

  // gui variables

  static final int BIG = 13;
  static final int BETWEEN = 13;
  static final int SMALL = 6;

  JFrame frame;
  int wide, high;

  JCheckBox newSketchPrompt;
  JTextField sketchbookLocation;


  // data model

  Properties properties;


  public PdePreferences() {

    // load preferences

    properties = new Properties();
    try {
      //properties.load(new FileInputStream("lib/pde.properties"));
      //#URL where = getClass().getResource("PdeBase.class");
      //System.err.println(where);
      //System.getProperties().list(System.err);
      //System.err.println("userdir = " + System.getProperty("user.dir"));

      if (PdeBase.platform == PdeBase.MACOSX) {
        //String pkg = "Proce55ing.app/Contents/Resources/Java/";
        //properties.load(new FileInputStream(pkg + "pde.properties"));
        //properties.load(new FileInputStream(pkg + "pde.properties_macosx"));
        properties.load(new FileInputStream("lib/pde.properties"));
        properties.load(new FileInputStream("lib/pde_macosx.properties"));

      } else if (PdeBase.platform == PdeBase.MACOS9) {
        properties.load(new FileInputStream("lib/pde.properties"));
        properties.load(new FileInputStream("lib/pde_macos9.properties"));

      } else {  
        // under win95, current dir not set properly
        // so using a relative url like "lib/" won't work
        properties.load(getClass().getResource("pde.properties").openStream());
        String platformProps = "pde_" + platforms[platform] + ".properties";
        properties.load(getClass().getResource(platformProps).openStream());
      }
      //properties.list(System.out);

    } catch (Exception e) {
      System.err.println("Error reading pde.properties");
      e.printStackTrace();
      //System.exit(1);
    }


    // setup frame for the prefs

    frame = new JFrame("Preferences");
    frame.setResizable(false);

    Container pain = this;
    //Container pain = frame.getContentPane();
    pain.setLayout(null);

    int top = BIG;
    int left = BIG;
    int right = 0;

    JLabel label;
    JButton button;
    JComboBox combo;
    Dimension d, d2, d3;
    int h, v, vmax;


    // [ ] Prompt for name and folder when creating new sketch

    newSketchPrompt = 
      new JCheckBox("Prompt for name when creating a new sketch");
    pain.add(newSketchPrompt);
    d = newSketchPrompt.getPreferredSize();
    newSketchPrompt.setBounds(left, top, d.width, d.height);
    right = Math.max(right, left + d.width);
    top += d.height + BETWEEN;


    // Sketchbook location: [...............................]  [ Browse ]

    label = new JLabel("Sketchbook location:");
    pain.add(label);
    d = label.getPreferredSize();

    sketchbookLocation = new JTextField(18);
    pain.add(sketchbookLocation);
    d2 = sketchbookLocation.getPreferredSize();

    button = new JButton("Browse");
    pain.add(button);
    d3 = button.getPreferredSize();

    // take max height of all components to vertically align em
    vmax = Math.max(Math.max(d.height, d2.height), d3.height);
    label.setBounds(left, top + (vmax-d.height)/2, 
                    d.width, d.height);
    h = left + d.width + BETWEEN;
    sketchbookLocation.setBounds(h, top + (vmax-d2.height)/2, 
                                 d2.width, d2.height);
    h += d2.width + BETWEEN;
    button.setBounds(h, top + (vmax-d3.height)/2, 
                     d3.width, d3.height);

    right = Math.max(right, h + d3.width + BIG);
    top += vmax + BETWEEN;


    // Default serial port:  [ COM1 + ]

    /*
    label = new JLabel("Default serial port:");
    pain.add(label);
    d = label.getPreferredSize();

    Vector list = buildPortList();
    combo = new JComboBox(list);
    pain.add(combo);
    d2 = combo.getPreferredSize();

    if (list.size() == 0) {
      label.setEnabled(false);
      combo.setEnabled(false);

    } else {
      String defaultName = PdeBase.get("serial.port", "unspecified");
      combo.setSelectedItem(defaultName);
    }

    vmax = Math.max(d.height, d2.height);
    label.setBounds(left, top + (vmax-d.height)/2, 
                    d.width, d.height);
    h = left + d.width + BETWEEN;
    combo.setBounds(h, top + (vmax-d2.height)/2, 
                    d2.width, d2.height);    
    right = Math.max(right, h + d2.width + BIG);
    top += vmax + BETWEEN;
    */


    // [ ] Use external editor


    // 

    String blather = 
      "More preferences are in the 'lib' folder inside text files\n" +
      "named pde.properties and pde_" + 
      PdeBase.platforms[PdeBase.platform] + ".properties";

    JTextArea textarea = new JTextArea(blather);
    textarea.setBorder(new EmptyBorder(SMALL, SMALL, SMALL, SMALL));
    textarea.setFont(new Font("Dialog", Font.PLAIN, 12));
    pain.add(textarea);

    //pain.add(label);
    d = textarea.getPreferredSize();
    textarea.setBounds(left, top, d.width, d.height);
    top += d.height + BETWEEN;

    //

    wide = right + BIG;
    high = top + BIG;
    setSize(wide, high);

    Container content = frame.getContentPane();
    content.setLayout(new BorderLayout());
    content.add(this, BorderLayout.CENTER);

    frame.pack();

    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    frame.setLocation((screen.width - wide) / 2,
                      (screen.height - high) / 2);

    //

    frame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          handleQuit();
        }
      });
    //frame.show();
  }


  public void showFrame() {
    frame.show();
  }


  // open the last-used sketch, etc
  public void apply() {
  }


  public void init() {
    // load the last program that was in use

    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    int windowX = -1, windowY = 0, windowW = 0, windowH = 0;

    Properties skprops = new Properties();
    try {
      if (PdeBase.platform == PdeBase.MACOSX) {
        //String pkg = "Proce55ing.app/Contents/Resources/Java/";
        //skprops.load(new FileInputStream(pkg + "sketch.properties"));
        skprops.load(new FileInputStream("lib/sketch.properties"));

      } else if (PdeBase.platform == PdeBase.MACOS9) {
        skprops.load(new FileInputStream("lib/sketch.properties"));

      } else {
        skprops.load(getClass().getResource("sketch.properties").openStream());
      }

      windowX = Integer.parseInt(skprops.getProperty("window.x", "-1"));
      windowY = Integer.parseInt(skprops.getProperty("window.y", "-1"));
      windowW = Integer.parseInt(skprops.getProperty("window.w", "-1"));
      windowH = Integer.parseInt(skprops.getProperty("window.h", "-1"));

      // if screen size has changed, the window coordinates no longer
      // make sense, so don't use them unless they're identical
      int screenW = Integer.parseInt(skprops.getProperty("screen.w", "-1"));
      int screenH = Integer.parseInt(skprops.getProperty("screen.h", "-1"));

      if ((screen.width != screenW) || (screen.height != screenH)) {
        // probably not valid for this machine, so invalidate sizing
        windowX = -1;
      }

      String name = skprops.getProperty("sketch.name");
      String path = skprops.getProperty("sketch.directory");
      String user = skprops.getProperty("user.name");

      String what = path + File.separator + name + ".pde";

      if (windowX != -1) {
        String dividerLocation = 
          skprops.getProperty("editor.divider.location");
        if (dividerLocation != null) {
          splitPane.setDividerLocation(Integer.parseInt(dividerLocation));
        }
      }

      if (new File(what).exists()) {
        userName = user;
        skOpen(path, name);

      } else {
        userName = "default";
        skNew();
      }

      String serialPort = skprops.getProperty("serial.port");
      if (serialPort != null) {
        PdeBase.properties.put("serial.port", serialPort);
      }

      boolean ee = new Boolean(skprops.getProperty("editor.external", "false")).booleanValue();
      setExternalEditor(ee);

    } catch (Exception e) { 
      // this exception doesn't matter, it's just the normal course of things
      // the app reaches here when no sketch.properties file exists
      //e.printStackTrace();

      // indicator that this is the first time this feller has used p5
      PdeBase.firstTime = true;

      // even if folder for 'default' user doesn't exist, or
      // sketchbook itself is missing, mkdirs() will make it happy
      userName = "default";

      // doesn't exist, not available, make my own
      skNew();
    }

    if (windowX == -1) {
      //System.out.println("using defaults for window size");
      windowW = PdeBase.getInteger("window.width", 500);
      windowH = PdeBase.getInteger("window.height", 500);
      windowX = (screen.width - windowW) / 2;
      windowY = (screen.height - windowH) / 2;
    }
    //PdeBase.frame.setBounds(windowX, windowY, windowW, windowH);
    base.setBounds(windowX, windowY, windowW, windowH);
    //rebuildSketchbookMenu(PdeBase.sketchbookMenu);
  }

  /*
    externalEditorItem = new CheckboxMenuItem("Use External Editor");
    externalEditorItem.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        //System.out.println(e);
        if (e.getStateChange() == ItemEvent.SELECTED) {
          editor.setExternalEditor(true);
        } else {
          editor.setExternalEditor(false);
        }
      }
    });
    menu.add(externalEditorItem);
  */


  public Dimension getPreferredSize() {
    return new Dimension(wide, high);
  }


  /*
  class SerialMenuListener implements ItemListener {
    //public SerialMenuListener() { }

    public void itemStateChanged(ItemEvent e) {
      int count = serialMenu.getItemCount();
      for (int i = 0; i < count; i++) {
        ((CheckboxMenuItem)serialMenu.getItem(i)).setState(false);
      }
      CheckboxMenuItem item = (CheckboxMenuItem)e.getSource();
      item.setState(true);
      String name = item.getLabel();
      //System.out.println(item.getLabel());
      PdeBase.properties.put("serial.port", name);
      //System.out.println("set to " + get("serial.port"));
    }
  }
  */


  /*
  protected Vector buildPortList() {
    // get list of names for serial ports
    // have the default port checked (if present)
    Vector list = new Vector();

    //SerialMenuListener listener = new SerialMenuListener();
    boolean problem = false;

    // if this is failing, it may be because
    // lib/javax.comm.properties is missing.
    // java is weird about how it searches for java.comm.properties
    // so it tends to be very fragile. i.e. quotes in the CLASSPATH
    // environment variable will hose things.
    try {
      //System.out.println("building port list");
      Enumeration portList = CommPortIdentifier.getPortIdentifiers();
      while (portList.hasMoreElements()) {
        CommPortIdentifier portId = 
          (CommPortIdentifier) portList.nextElement();
        //System.out.println(portId);

        if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
          //if (portId.getName().equals(port)) {
          String name = portId.getName();
          //CheckboxMenuItem mi = 
          //new CheckboxMenuItem(name, name.equals(defaultName));

          //mi.addItemListener(listener);
          //serialMenu.add(mi);
          list.addElement(name);
        }
      }
    } catch (UnsatisfiedLinkError e) {
      e.printStackTrace();
      problem = true;

    } catch (Exception e) {
      System.out.println("exception building serial menu");
      e.printStackTrace();
    }

    //if (serialMenu.getItemCount() == 0) {
      //System.out.println("dimming serial menu");
    //serialMenu.setEnabled(false);
    //}

    // only warn them if this is the first time
    if (problem && PdeBase.firstTime) {
      JOptionPane.showMessageDialog(this, //frame,
                                    "Serial port support not installed.\n" +
                                    "Check the readme for instructions\n" +
                                    "if you need to use the serial port.    ",
                                    "Serial Port Warning",
                                    JOptionPane.WARNING_MESSAGE);
    }
    return list;
  }
  */
}

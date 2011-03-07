package processing.mode.java;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.*;

import javax.swing.*;
import javax.swing.border.*;

import processing.app.*;


public class JavaEditor extends Editor {
  JavaMode jmode;
  // TODO this needs prefs to be applied when necessary
  PdeKeyListener listener;

  
  protected JavaEditor(Base base, String path, int[] location, Mode mode) {
    super(base, path, location, mode);    
    
    // hopefully these are no longer needed w/ swing
    // (har har har.. that was wishful thinking)
    listener = new PdeKeyListener(this, textarea);

    jmode = (JavaMode) mode;
  }

  
  public EditorToolbar createToolbar() {
    return new JavaToolbar(this, base);
  }
  
  
  public Formatter createFormatter() {
    return new AutoFormat();
  }
  
  
  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  
  public JMenu buildFileMenu() {
    String appletTitle = JavaToolbar.getTitle(JavaToolbar.EXPORT, false);
    JMenuItem exportApplet = Base.newJMenuItem(appletTitle, 'E');
    exportApplet.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        handleExportApplet();
      }
    });

    String appTitle = JavaToolbar.getTitle(JavaToolbar.EXPORT, true);
    JMenuItem exportApplication = Base.newJMenuItemShift(appTitle, 'E');
    exportApplication.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        handleExportApplication();
      }
    });
    return buildFileMenu(new JMenuItem[] { exportApplet, exportApplication });
  }
  
  
  public JMenu buildSketchMenu() {
    JMenuItem runItem = Base.newJMenuItem(JavaToolbar.getTitle(JavaToolbar.RUN, false), 'R');
    runItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleRun();
        }
      });

    JMenuItem presentItem = Base.newJMenuItemShift(JavaToolbar.getTitle(JavaToolbar.RUN, true), 'R');
    presentItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handlePresent();
        }
      });

    JMenuItem stopItem = new JMenuItem(JavaToolbar.getTitle(JavaToolbar.STOP, false));
    stopItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleStop();
        }
      });
    return buildSketchMenu(new JMenuItem[] { runItem, presentItem, stopItem });
  }
  

  public JMenu buildHelpMenu() {
    // To deal with a Mac OS X 10.5 bug, add an extra space after the name
    // so that the OS doesn't try to insert its slow help menu.
    JMenu menu = new JMenu("Help ");
    JMenuItem item;

    item = new JMenuItem("Getting Started");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Base.openURL("http://processing.org/learning/gettingstarted/");
        }
      });
    menu.add(item);

    item = new JMenuItem("Environment");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          showReference("environment" + File.separator + "index.html");
        }
      });
    menu.add(item);

    item = new JMenuItem("Troubleshooting");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Base.openURL("http://wiki.processing.org/w/Troubleshooting");
        }
      });
    menu.add(item);

    item = new JMenuItem("Reference");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          showReference("index.html");
        }
      });
    menu.add(item);

    item = Base.newJMenuItemShift("Find in Reference", 'F');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (textarea.isSelectionActive()) {
            handleFindReference();
          }
        }
      });
    menu.add(item);

    item = new JMenuItem("Frequently Asked Questions");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Base.openURL("http://wiki.processing.org/w/FAQ");
        }
      });
    menu.add(item);

    item = new JMenuItem("Visit Processing.org");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Base.openURL("http://processing.org/");
        }
      });
    menu.add(item);

    // macosx already has its own about menu
    if (!Base.isMacOS()) {
      menu.addSeparator();
      item = new JMenuItem("About Processing");
      item.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            base.handleAbout();
          }
        });
      menu.add(item);
    }

    return menu;
  }    
  

  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  
  public String getCommentPrefix() {
    return "//";
  }
  
  
  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Called by Sketch &rarr; Export.
   * Handles calling the export() function on sketch, and
   * queues all the gui status stuff that comes along with it.
   * <p/>
   * Made synchronized to (hopefully) avoid problems of people
   * hitting export twice, quickly, and horking things up.
   */
  public void handleExportApplet() {
    if (handleExportCheckModified()) {
      toolbar.activate(JavaToolbar.EXPORT);
      try {
        boolean success = jmode.handleExportApplet(sketch);
        if (success) {
          File appletFolder = new File(sketch.getFolder(), "applet");
          Base.openFolder(appletFolder);
          statusNotice("Done exporting.");
        } else {
          // error message will already be visible
        }

      } catch (Exception e) {
        statusError(e);
      }
      toolbar.deactivate(JavaToolbar.EXPORT);
    }
  }
  
  
//  public void handleExportApplication() {
//    toolbar.activate(Toolbar.EXPORT);
//    try {
//      jmode.handleExportApplication();
//    } catch (Exception e) {
//      statusError(e);
//    }
//    toolbar.deactivate(Toolbar.EXPORT);
//  }  
  

  /**
   * Handler for Sketch &rarr; Export Application
   */
  public void handleExportApplication() {
    toolbar.activate(JavaToolbar.EXPORT);
    
    if (handleExportCheckModified()) {
      statusNotice("Exporting application...");
      try {
        if (exportApplicationPrompt()) {
          Base.openFolder(sketch.getFolder());
          statusNotice("Done exporting.");
        } else {
          // error message will already be visible
          // or there was no error, in which case it was canceled.
        }
      } catch (Exception e) {
        statusNotice("Error during export.");
        e.printStackTrace();
      }
    }
    toolbar.deactivate(JavaToolbar.EXPORT);
  }
  
  
  protected boolean exportApplicationPrompt() throws IOException, SketchException {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.add(Box.createVerticalStrut(6));

    //Box panel = Box.createVerticalBox();

    //Box labelBox = Box.createHorizontalBox();
//    String msg = "<html>Click Export to Application to create a standalone, " +
//      "double-clickable application for the selected plaforms.";

//    String msg = "Export to Application creates a standalone, \n" +
//      "double-clickable application for the selected plaforms.";
    String line1 = "Export to Application creates double-clickable,";
    String line2 = "standalone applications for the selected plaforms.";
    JLabel label1 = new JLabel(line1, SwingConstants.CENTER);
    JLabel label2 = new JLabel(line2, SwingConstants.CENTER);
    label1.setAlignmentX(Component.LEFT_ALIGNMENT);
    label2.setAlignmentX(Component.LEFT_ALIGNMENT);
//    label1.setAlignmentX();
//    label2.setAlignmentX(0);
    panel.add(label1);
    panel.add(label2);
    int wide = label2.getPreferredSize().width;
    panel.add(Box.createVerticalStrut(12));

    final JCheckBox windowsButton = new JCheckBox("Windows");
    //windowsButton.setMnemonic(KeyEvent.VK_W);
    windowsButton.setSelected(Preferences.getBoolean("export.application.platform.windows"));
    windowsButton.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Preferences.setBoolean("export.application.platform.windows", windowsButton.isSelected());
      }
    });

    final JCheckBox macosxButton = new JCheckBox("Mac OS X");
    //macosxButton.setMnemonic(KeyEvent.VK_M);
    macosxButton.setSelected(Preferences.getBoolean("export.application.platform.macosx"));
    macosxButton.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Preferences.setBoolean("export.application.platform.macosx", macosxButton.isSelected());
      }
    });

    final JCheckBox linuxButton = new JCheckBox("Linux");
    //linuxButton.setMnemonic(KeyEvent.VK_L);
    linuxButton.setSelected(Preferences.getBoolean("export.application.platform.linux"));
    linuxButton.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Preferences.setBoolean("export.application.platform.linux", linuxButton.isSelected());
      }
    });

    JPanel platformPanel = new JPanel();
    //platformPanel.setLayout(new BoxLayout(platformPanel, BoxLayout.X_AXIS));
    platformPanel.add(windowsButton);
    platformPanel.add(Box.createHorizontalStrut(6));
    platformPanel.add(macosxButton);
    platformPanel.add(Box.createHorizontalStrut(6));
    platformPanel.add(linuxButton);
    platformPanel.setBorder(new TitledBorder("Platforms"));
    //Dimension goodIdea = new Dimension(wide, platformPanel.getPreferredSize().height);
    //platformPanel.setMaximumSize(goodIdea);
    wide = Math.max(wide, platformPanel.getPreferredSize().width);
    platformPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    panel.add(platformPanel);

//  Box indentPanel = Box.createHorizontalBox();
//  indentPanel.add(Box.createHorizontalStrut(new JCheckBox().getPreferredSize().width));
    final JCheckBox showStopButton = new JCheckBox("Show a Stop button");
    //showStopButton.setMnemonic(KeyEvent.VK_S);
    showStopButton.setSelected(Preferences.getBoolean("export.application.stop"));
    showStopButton.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Preferences.setBoolean("export.application.stop", showStopButton.isSelected());
      }
    });
    showStopButton.setEnabled(Preferences.getBoolean("export.application.fullscreen"));
    showStopButton.setBorder(new EmptyBorder(3, 13, 6, 13));
//  indentPanel.add(showStopButton);
//  indentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

    final JCheckBox fullScreenButton = new JCheckBox("Full Screen (Present mode)");
    //fullscreenButton.setMnemonic(KeyEvent.VK_F);
    fullScreenButton.setSelected(Preferences.getBoolean("export.application.fullscreen"));
    fullScreenButton.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        boolean sal = fullScreenButton.isSelected();
        Preferences.setBoolean("export.application.fullscreen", sal);
        showStopButton.setEnabled(sal);
      }
    });
    fullScreenButton.setBorder(new EmptyBorder(3, 13, 3, 13));

    JPanel optionPanel = new JPanel();
    optionPanel.setLayout(new BoxLayout(optionPanel, BoxLayout.Y_AXIS));
    optionPanel.add(fullScreenButton);
    optionPanel.add(showStopButton);
//    optionPanel.add(indentPanel);
    optionPanel.setBorder(new TitledBorder("Options"));
    wide = Math.max(wide, platformPanel.getPreferredSize().width);
    //goodIdea = new Dimension(wide, optionPanel.getPreferredSize().height);
    optionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    //optionPanel.setMaximumSize(goodIdea);
    panel.add(optionPanel);

    Dimension good;
    //label1, label2, platformPanel, optionPanel
    good = new Dimension(wide, label1.getPreferredSize().height);
    label1.setMaximumSize(good);
    good = new Dimension(wide, label2.getPreferredSize().height);
    label2.setMaximumSize(good);
    good = new Dimension(wide, platformPanel.getPreferredSize().height);
    platformPanel.setMaximumSize(good);
    good = new Dimension(wide, optionPanel.getPreferredSize().height);
    optionPanel.setMaximumSize(good);

//    JPanel actionPanel = new JPanel();
//    optionPanel.setLayout(new BoxLayout(optionPanel, BoxLayout.X_AXIS));
//    optionPanel.add(Box.createHorizontalGlue());

//    final JDialog frame = new JDialog(editor, "Export to Application");

//    JButton cancelButton = new JButton("Cancel");
//    cancelButton.addActionListener(new ActionListener() {
//      public void actionPerformed(ActionEvent e) {
//        frame.dispose();
//        return false;
//      }
//    });

    // Add the buttons in platform-specific order
//    if (PApplet.platform == PConstants.MACOSX) {
//      optionPanel.add(cancelButton);
//      optionPanel.add(exportButton);
//    } else {
//      optionPanel.add(exportButton);
//      optionPanel.add(cancelButton);
//    }
    String[] options = { "Export", "Cancel" };
    final JOptionPane optionPane = new JOptionPane(panel,
                                                   JOptionPane.PLAIN_MESSAGE,
                                                   //JOptionPane.QUESTION_MESSAGE,
                                                   JOptionPane.YES_NO_OPTION,
                                                   null,
                                                   options,
                                                   options[0]);

    final JDialog dialog = new JDialog(this, "Export Options", true);
    dialog.setContentPane(optionPane);

    optionPane.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent e) {
        String prop = e.getPropertyName();

        if (dialog.isVisible() &&
            (e.getSource() == optionPane) &&
            (prop.equals(JOptionPane.VALUE_PROPERTY))) {
          //If you were going to check something
          //before closing the window, you'd do
          //it here.
          dialog.setVisible(false);
        }
      }
    });
    dialog.pack();
    dialog.setResizable(false);

    Rectangle bounds = getBounds();
    dialog.setLocation(bounds.x + (bounds.width - dialog.getSize().width) / 2,
                       bounds.y + (bounds.height - dialog.getSize().height) / 2);
    dialog.setVisible(true);

    Object value = optionPane.getValue();
    if (value.equals(options[0])) {
      return jmode.handleExportApplication(sketch);
    } else if (value.equals(options[1]) || value.equals(new Integer(-1))) {
      // closed window by hitting Cancel or ESC
      statusNotice("Export to Application canceled.");
    }
    return false;
  }


  /**
   * Checks to see if the sketch has been modified, and if so,
   * asks the user to save the sketch or cancel the export.
   * This prevents issues where an incomplete version of the sketch
   * would be exported, and is a fix for
   * <A HREF="http://dev.processing.org/bugs/show_bug.cgi?id=157">Bug 157</A>
   */
  protected boolean handleExportCheckModified() {
    if (sketch.isModified()) {
      Object[] options = { "OK", "Cancel" };
      int result = JOptionPane.showOptionDialog(this,
                                                "Save changes before export?",
                                                "Save",
                                                JOptionPane.OK_CANCEL_OPTION,
                                                JOptionPane.QUESTION_MESSAGE,
                                                null,
                                                options,
                                                options[0]);

      if (result == JOptionPane.OK_OPTION) {
        handleSaveRequest(true);

      } else {
        // why it's not CANCEL_OPTION is beyond me (at least on the mac)
        // but f-- it.. let's get this shite done..
        //} else if (result == JOptionPane.CANCEL_OPTION) {
        statusNotice("Export canceled, changes must first be saved.");
        //toolbar.clear();
        return false;
      }
    }
    return true;
  }


  public void handleRun() {
    toolbar.activate(JavaToolbar.RUN);

    new Thread(new Runnable() {
      public void run() {
        prepareRun();
        try {
          jmode.handleRun(sketch, JavaEditor.this);
        } catch (Exception e) {
          statusError(e);
        }
      }
    }).start();
  }
  
  
  public void handlePresent() {
    toolbar.activate(JavaToolbar.RUN);

    new Thread(new Runnable() {
      public void run() {
        prepareRun();
        try {
          jmode.handlePresent(sketch, JavaEditor.this);
        } catch (Exception e) {
          statusError(e);
        }
      }
    }).start();
  }


  public void handleStop() {
    toolbar.activate(JavaToolbar.STOP);

    try {
      jmode.handleStop();
    } catch (Exception e) {
      statusError(e);
    }

    toolbar.deactivate(JavaToolbar.RUN);
    toolbar.deactivate(JavaToolbar.STOP);

    // focus the PDE again after quitting presentation mode [toxi 030903]
    toFront();
  }
  
  
  public void handleSave() {
    toolbar.activate(JavaToolbar.SAVE);
    handleStop();
    super.handleSave();
    toolbar.deactivate(JavaToolbar.SAVE);
  }
  
  
  public boolean handleSaveAs() {
    toolbar.activate(JavaToolbar.SAVE);
    handleStop();
    boolean result = super.handleSaveAs();
    toolbar.deactivate(JavaToolbar.SAVE);
    return result;
  }
  
  
  /**
   * Add import statements to the current tab for all of packages inside
   * the specified jar file.
   */
  public void handleImportLibrary(String jarPath) {
    // make sure the user didn't hide the sketch folder
    sketch.ensureExistence();

    // import statements into the main sketch file (code[0])
    // if the current code is a .java file, insert into current
    //if (current.flavor == PDE) {
    if (mode.isDefaultExtension(sketch.getCurrentCode())) {
      sketch.setCurrentCode(0);
    }
    
    // could also scan the text in the file to see if each import
    // statement is already in there, but if the user has the import
    // commented out, then this will be a problem.
    String[] list = Base.packageListFromClassPath(jarPath);
    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < list.length; i++) {
      buffer.append("import ");
      buffer.append(list[i]);
      buffer.append(".*;\n");
    }
    buffer.append('\n');
    buffer.append(getText());
    setText(buffer.toString());
    setSelection(0, 0);  // scroll to start
    sketch.setModified(true);
  }
  
  
  public void statusError(String what) {
    super.statusError(what);
//    new Exception("deactivating RUN").printStackTrace();
    toolbar.deactivate(JavaToolbar.RUN);
  }

  
  /**
   * Deactivate the Run button. This is called by Runner to notify that the
   * sketch has stopped running, usually in response to an error (or maybe
   * the sketch completing and exiting?) Tools should not call this function.
   * To initiate a "stop" action, call handleStop() instead.
   */
  public void deactivateRun() {
    toolbar.deactivate(JavaToolbar.RUN);
  }


  public void deactivateExport() {
    toolbar.deactivate(JavaToolbar.EXPORT);
  }
  
  
  public void internalCloseRunner() {
    jmode.handleStop();
  }
}
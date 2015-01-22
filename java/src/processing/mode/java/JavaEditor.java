package processing.mode.java;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import processing.app.*;
import processing.app.Toolkit;
import processing.app.contrib.ToolContribution;
import processing.app.syntax.JEditTextArea;
import processing.app.syntax.PdeTextAreaDefaults;
import processing.mode.java.runner.Runner;


public class JavaEditor extends Editor {
  JavaMode jmode;

  // TODO this needs prefs to be applied when necessary
//  PdeKeyListener listener;

  // Runner associated with this editor window
  private Runner runtime;


  protected JavaEditor(Base base, String path, EditorState state, Mode mode) {
    super(base, path, state, mode);

//    // hopefully these are no longer needed w/ swing
//    // (har har har.. that was wishful thinking)
//    listener = new PdeKeyListener(this, textarea);

    jmode = (JavaMode) mode;
  }
  
  
  protected JEditTextArea createTextArea() {
    return new JEditTextArea(new PdeTextAreaDefaults(mode)) {
       // Forwards key events directly to the input handler. This is slightly 
       // faster than using a KeyListener because some Swing overhead is avoided.
      PdeKeyListener editorListener = new PdeKeyListener(JavaEditor.this, this);

      // Moved out of JEditTextArea for 3.0a6 to remove dependency on Java Mode
      public void processKeyEvent(KeyEvent evt) {
        // this had to be added in Processing 007X, because the menu key
        // events weren't making it up to the frame.
        super.processKeyEvent(evt);

        if (inputHandler != null) {
          switch (evt.getID()) {  
          case KeyEvent.KEY_TYPED:  
            if ((editorListener == null) || !editorListener.keyTyped(evt)) {
              inputHandler.keyTyped(evt);
            }
            break;
          case KeyEvent.KEY_PRESSED:
            if ((editorListener == null) || !editorListener.keyPressed(evt)) {
              inputHandler.keyPressed(evt);
            }
            break;
          case KeyEvent.KEY_RELEASED:
            inputHandler.keyReleased(evt);
            break;
          }
        }
      }
    };
  }


  public EditorToolbar createToolbar() {
    return new JavaToolbar(this, base);
  }


  public Formatter createFormatter() {
    return new AutoFormat();
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public JMenu buildFileMenu() {
    //String appTitle = JavaToolbar.getTitle(JavaToolbar.EXPORT, false);
    String appTitle = Language.text("toolbar.export_application");
    JMenuItem exportApplication = Toolkit.newJMenuItemShift(appTitle, 'E');
    exportApplication.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        handleExportApplication();
      }
    });
//    String appletTitle = JavaToolbar.getTitle(JavaToolbar.EXPORT, true);
//    JMenuItem exportApplet = Base.newJMenuItemShift(appletTitle, 'E');
//    exportApplet.addActionListener(new ActionListener() {
//      public void actionPerformed(ActionEvent e) {
//        handleExportApplet();
//      }
//    });

//    return buildFileMenu(new JMenuItem[] { exportApplication, exportApplet });
    return buildFileMenu(new JMenuItem[] { exportApplication });
  }


  public JMenu buildSketchMenu() {
    JMenuItem runItem = Toolkit.newJMenuItem(JavaToolbar.getTitle(JavaToolbar.RUN, false), 'R');
    runItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        handleRun();
      }
    });

    JMenuItem presentItem = Toolkit.newJMenuItemShift(JavaToolbar.getTitle(JavaToolbar.RUN, true), 'R');
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
    JMenu menu = new JMenu(Language.text("menu.help"));
    JMenuItem item;

    // macosx already has its own about menu
    if (!Base.isMacOS()) {
      item = new JMenuItem(Language.text("menu.help.about"));
      item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          new About(JavaEditor.this);
        }
      });
      menu.add(item);
    }

    item = new JMenuItem(Language.text("menu.help.environment"));
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        showReference("environment" + File.separator + "index.html");
      }
    });
    menu.add(item);

    item = new JMenuItem(Language.text("menu.help.reference"));
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        showReference("index.html");
      }
    });
    menu.add(item);

    item = Toolkit.newJMenuItemShift(Language.text("menu.help.find_in_reference"), 'F');
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (textarea.isSelectionActive()) {
          handleFindReference();
        }
      }
    });
    menu.add(item);
    
    menu.addSeparator();
    
    final JMenu libRefSubmenu = new JMenu(Language.text("menu.help.libraries_reference"));
    // Populate only when sub-menu is opened, to avoid having spurious menu 
    // options if a library is deleted, or a missing menu option if a library is added
    libRefSubmenu.addMenuListener(new MenuListener() {
      
      @Override
      public void menuSelected(MenuEvent e) {
        boolean isCoreLibMenuItemAdded = false;
        boolean isContribLibMenuItemAdded = false;
        
        // Adding this in case references are included in a core library,
        // or other core libraries are incuded in future
        isCoreLibMenuItemAdded = addLibReferencesToSubMenu(mode.coreLibraries, libRefSubmenu);
        
        if (isCoreLibMenuItemAdded && !mode.contribLibraries.isEmpty())
          libRefSubmenu.addSeparator();
        
        isContribLibMenuItemAdded = addLibReferencesToSubMenu(mode.contribLibraries, libRefSubmenu);
        
        if (!isContribLibMenuItemAdded && !isCoreLibMenuItemAdded) {
          JMenuItem emptyMenuItem = new JMenuItem(Language.text("menu.help.empty"));
          emptyMenuItem.setEnabled(false);
          emptyMenuItem.setFocusable(false);
          emptyMenuItem.setFocusPainted(false);
          libRefSubmenu.add(emptyMenuItem);
        }
        else if (!isContribLibMenuItemAdded && !mode.coreLibraries.isEmpty()) {
          //re-populate the menu to get rid of terminal separator
          libRefSubmenu.removeAll();
          addLibReferencesToSubMenu(mode.coreLibraries, libRefSubmenu);
        }
      }
      
      @Override
      public void menuDeselected(MenuEvent e) {
        libRefSubmenu.removeAll();
      }
      
      @Override
      public void menuCanceled(MenuEvent e) {
        menuDeselected(e);
      }
    });
    menu.add(libRefSubmenu);
    
    final JMenu toolRefSubmenu = new JMenu(Language.text("menu.help.tools_reference"));
    // Populate only when sub-menu is opened, to avoid having spurious menu 
    // options if a tool is deleted, or a missing menu option if a library is added
    toolRefSubmenu.addMenuListener(new MenuListener() {
      
      @Override
      public void menuSelected(MenuEvent e) {
        boolean isCoreToolMenuItemAdded = false;
        boolean isContribToolMenuItemAdded = false;
        
        // Adding this in in case a reference folder is added for MovieMaker, or in case
        // other core tools are introduced later
        isCoreToolMenuItemAdded = addToolReferencesToSubMenu(getCoreTools(), toolRefSubmenu);
        
        if (isCoreToolMenuItemAdded && !contribTools.isEmpty())
          toolRefSubmenu.addSeparator();
        
        isContribToolMenuItemAdded = addToolReferencesToSubMenu(contribTools, toolRefSubmenu);
        
        if (!isContribToolMenuItemAdded && !isCoreToolMenuItemAdded) {
          toolRefSubmenu.removeAll(); // in case a separator was added
          final JMenuItem emptyMenuItem = new JMenuItem(Language.text("menu.help.empty"));
          emptyMenuItem.setEnabled(false);
          emptyMenuItem.setBorderPainted(false);
          emptyMenuItem.setFocusable(false);
          emptyMenuItem.setFocusPainted(false);
          toolRefSubmenu.add(emptyMenuItem);
        }
        else if (!isContribToolMenuItemAdded && !contribTools.isEmpty()) {
          // re-populate the menu to get rid of terminal separator
          toolRefSubmenu.removeAll();
          addToolReferencesToSubMenu(getCoreTools(), toolRefSubmenu);
        }
      }
      
      @Override
      public void menuDeselected(MenuEvent e) {
        toolRefSubmenu.removeAll();
      }
      
      @Override
      public void menuCanceled(MenuEvent e) {
        menuDeselected(e);
      }
    });
    menu.add(toolRefSubmenu);

    menu.addSeparator();
    item = new JMenuItem(Language.text("menu.help.online"));
    item.setEnabled(false);
    menu.add(item);

    item = new JMenuItem(Language.text("menu.help.getting_started"));
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Base.openURL(Language.text("menu.help.getting_started.url"));
      }
    });
    menu.add(item);

    item = new JMenuItem(Language.text("menu.help.troubleshooting"));
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Base.openURL(Language.text("menu.help.troubleshooting.url"));
      }
    });
    menu.add(item);

    item = new JMenuItem(Language.text("menu.help.faq"));
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Base.openURL(Language.text("menu.help.faq.url"));
      }
    });
    menu.add(item);
    
    item = new JMenuItem(Language.text("menu.help.foundation"));
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Base.openURL(Language.text("menu.help.foundation.url"));
      }
    });
    menu.add(item);

    item = new JMenuItem(Language.text("menu.help.visit"));
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Base.openURL(Language.text("menu.help.visit.url"));
      }
    });
    menu.add(item);

    return menu;
  }


  //. . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Populates the JMenu with JMenuItems, one for each Library that has a
   * reference accompanying it. The JMenuItems open the index.htm/index.html
   * file of the reference in the user's default browser, or the readme.txt in
   * the user's default text editor.
   * 
   * @param libsList
   *          A list of the Libraries to be added
   * @param subMenu
   *          The JMenu to which the JMenuItems corresponding to the Libraries
   *          are to be added
   * @return true if and only if any JMenuItems were added; false otherwise
   */
  private boolean addLibReferencesToSubMenu(ArrayList<Library> libsList, JMenu subMenu) {
    boolean isItemAdded = false;
    Iterator<Library> iter = libsList.iterator();
    while (iter.hasNext()) {
      final Library libContrib = iter.next();
      if (libContrib.hasReference()) {
        JMenuItem libRefItem = new JMenuItem(libContrib.getName());
        libRefItem.addActionListener(new ActionListener() {

          @Override
          public void actionPerformed(ActionEvent arg0) {
            showReferenceFile(libContrib.getReferenceIndexFile());
          }
        });
        subMenu.add(libRefItem);
        isItemAdded = true;
      }
    }
    return isItemAdded;
  }


  /**
   * 
   * Populates the JMenu with JMenuItems, one for each Tool that has a reference
   * accompanying it. The JMenuItems open the index.htm/index.html file of the
   * reference in the user's default browser, or the readme.txt in the user's
   * default text editor.
   * 
   * @param toolsList
   *          A list of Tools to be added
   * @param subMenu
   *          The JMenu to which the JMenuItems corresponding to the Tools are
   *          to be added
   * @return true if and only if any JMenuItems were added; false otherwise
   */
  private boolean addToolReferencesToSubMenu(ArrayList<ToolContribution> toolsList, JMenu subMenu) {
    boolean isItemAdded = false;
    Iterator<ToolContribution> iter = toolsList.iterator();
    while (iter.hasNext()) {
      final ToolContribution toolContrib = iter.next();
      final File toolRef = new File(toolContrib.getFolder(), "reference/index.html");
      if (toolRef.exists()) {
        JMenuItem libRefItem = new JMenuItem(toolContrib.getName());
        libRefItem.addActionListener(new ActionListener() {

          @Override
          public void actionPerformed(ActionEvent arg0) {
            showReferenceFile(toolRef);
          }
        });
        subMenu.add(libRefItem);
        isItemAdded = true;
      }
    }
    return isItemAdded;
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
  /*
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
  */


  /**
   * Handler for Sketch &rarr; Export Application
   */
  public void handleExportApplication() {
//    toolbar.activate(JavaToolbar.EXPORT);

    if (handleExportCheckModified()) {
      statusNotice(Language.text("export.notice.exporting"));
      try {
        if (exportApplicationPrompt()) {
          Base.openFolder(sketch.getFolder());
          statusNotice(Language.text("export.notice.exporting.done"));
        } else {
          // error message will already be visible
          // or there was no error, in which case it was canceled.
        }
      } catch (Exception e) {
        statusNotice(Language.text("export.notice.exporting.error"));
        e.printStackTrace();
      }
    }
//    toolbar.deactivate(JavaToolbar.EXPORT);
  }

  
//  JPanel presentColorPanel;
//  JTextField presentColorPanel;
  
  protected boolean exportApplicationPrompt() throws IOException, SketchException {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.add(Box.createVerticalStrut(6));

//    Box panel = Box.createVerticalBox();
//    Box labelBox = Box.createHorizontalBox();
//    String msg = "<html>Click Export to Application to create a standalone, " +
//      "double-clickable application for the selected plaforms.";
//    String msg = "Export to Application creates a standalone, \n" +
//      "double-clickable application for the selected plaforms.";
    String line1 = Language.text("export.description.line1");
    String line2 = Language.text("export.description.line2");
    //String line2 = "standalone application for the current plaform.";
    JLabel label1 = new JLabel(line1, SwingConstants.CENTER);
    JLabel label2 = new JLabel(line2, SwingConstants.CENTER);
    label1.setAlignmentX(Component.LEFT_ALIGNMENT);
    label2.setAlignmentX(Component.LEFT_ALIGNMENT);
    panel.add(label1);
    panel.add(label2);
    // The longer line is different between Windows and OS X.
//    int wide = Math.max(label1.getPreferredSize().width,
//                        label2.getPreferredSize().width);
    panel.add(Box.createVerticalStrut(12));

    final JCheckBox windowsButton = new JCheckBox("Windows");
    //windowsButton.setMnemonic(KeyEvent.VK_W);
    windowsButton.setSelected(Preferences.getBoolean("export.application.platform.windows"));
    windowsButton.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Preferences.setBoolean("export.application.platform.windows", windowsButton.isSelected());
      }
    });

    // Only possible to export OS X applications on OS X
    if (!Base.isMacOS()) {
      // Make sure they don't have a previous 'true' setting for this
      Preferences.setBoolean("export.application.platform.macosx", false);
    }
    final JCheckBox macosxButton = new JCheckBox("Mac OS X");
    macosxButton.setSelected(Preferences.getBoolean("export.application.platform.macosx"));
    macosxButton.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Preferences.setBoolean("export.application.platform.macosx", macosxButton.isSelected());
      }
    });
    if (!Base.isMacOS()) {
      macosxButton.setEnabled(false);
      macosxButton.setToolTipText(Language.text("export.tooltip.macosx"));
    }

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
    platformPanel.setBorder(new TitledBorder(Language.text("export.platforms")));
    //Dimension goodIdea = new Dimension(wide, platformPanel.getPreferredSize().height);
    //platformPanel.setMaximumSize(goodIdea);
//    wide = Math.max(wide, platformPanel.getPreferredSize().width);
    platformPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    panel.add(platformPanel);
    int divWidth = platformPanel.getPreferredSize().width;
    
    //int indent = new JCheckBox().getPreferredSize().width;
    int indent = 0;
    
    final JCheckBox showStopButton = new JCheckBox(Language.text("export.options.show_stop_button"));
    showStopButton.setSelected(Preferences.getBoolean("export.application.stop"));
    showStopButton.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Preferences.setBoolean("export.application.stop", showStopButton.isSelected());
      }
    });
    showStopButton.setEnabled(Preferences.getBoolean("export.application.fullscreen"));
    showStopButton.setBorder(new EmptyBorder(3, 13 + indent, 6, 13));

    final JCheckBox fullScreenButton = new JCheckBox(Language.text("export.options.fullscreen"));
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

    JPanel presentPanel = new JPanel();
    presentPanel.setLayout(new BoxLayout(presentPanel, BoxLayout.Y_AXIS));
    Box fullScreenBox = Box.createHorizontalBox();
    fullScreenBox.add(fullScreenButton);
    
    /*
    //run.present.stop.color
//    presentColorPanel = new JTextField();
//    presentColorPanel.setFocusable(false);
//    presentColorPanel.setEnabled(false);
    presentColorPanel = new JPanel() {
      public void paintComponent(Graphics g) {
        g.setColor(Preferences.getColor("run.present.bgcolor"));
        Dimension size = getSize();
        g.fillRect(0, 0, size.width, size.height);
      }
    };
    presentColorPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
//    presentColorPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
    presentColorPanel.setMaximumSize(new Dimension(30, 20));
    fullScreenBox.add(presentColorPanel);
    */
    fullScreenBox.add(new ColorPreference("run.present.bgcolor"));
    //presentPanel.add(fullScreenButton);
    fullScreenBox.add(Box.createHorizontalStrut(10));
    fullScreenBox.add(Box.createHorizontalGlue());

    presentPanel.add(fullScreenBox);
    
//    presentColorPanel.addMouseListener(new MouseAdapter() {
//      public void mousePressed(MouseEvent e) {
//        new ColorListener("run.present.bgcolor");
//      }
//    });

    Box showStopBox = Box.createHorizontalBox();
    showStopBox.add(showStopButton);
    showStopBox.add(new ColorPreference("run.present.stop.color"));
    showStopBox.add(Box.createHorizontalStrut(10));
    showStopBox.add(Box.createHorizontalGlue());
    presentPanel.add(showStopBox);
    
    //presentPanel.add(showStopButton);
//    presentPanel.add(Box.createHorizontalStrut(10));
//    presentPanel.add(Box.createHorizontalGlue());
    presentPanel.setBorder(new TitledBorder(Language.text("export.full_screen")));
//    wide = Math.max(wide, platformPanel.getPreferredSize().width);
    presentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    panel.add(presentPanel);

//    Dimension good;
//    good = new Dimension(wide, label1.getPreferredSize().height);
//    label1.setMaximumSize(good);
//    good = new Dimension(wide, label2.getPreferredSize().height);
//    label2.setMaximumSize(good);
//    good = new Dimension(wide, presentPanel.getPreferredSize().height);
    
    //
    
    JPanel embedPanel = new JPanel();
    embedPanel.setLayout(new BoxLayout(embedPanel, BoxLayout.Y_AXIS));
    
    String platformName = null;
    if (Base.isMacOS()) {
      platformName = "Mac OS X";
    } else if (Base.isWindows()) {
      platformName = "Windows (" + Base.getNativeBits() + "-bit)";
    } else if (Base.isLinux()) {
      platformName = "Linux (" + Base.getNativeBits() + "-bit)";
    }
    
    boolean embed = Preferences.getBoolean("export.application.embed_java");
    final String embedWarning =
      "<html><div width=\"" + divWidth + "\"><font size=\"2\">" + 
//      "<html><body><font size=2>" +
      "Embedding Java will make the " + platformName + " application " +
      "larger, but it will be far more likely to work. " +
      "Users on other platforms will need to <a href=\"\">install Java 7</a>.";
    final String nopeWarning = 
      "<html><div width=\"" + divWidth + "\"><font size=\"2\">" + 
//      "<html><body><font size=2>" +
      "Users on all platforms will have to install the latest " +
      "version of Java 7 from <a href=\"\">http://java.com/download</a>. " +
      "<br/>&nbsp;";
      //"from <a href=\"http://java.com/download\">java.com/download</a>.";
    final JLabel warningLabel = new JLabel(embed ? embedWarning : nopeWarning);
    warningLabel.addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent event) {
        Base.openURL("http://java.com/download");
      }
    });
    warningLabel.setBorder(new EmptyBorder(3, 13 + indent, 3, 13));

    final JCheckBox embedJavaButton = 
      new JCheckBox(Language.text("export.embed_java.for") + " " + platformName);
    embedJavaButton.setSelected(embed);
    embedJavaButton.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        boolean selected = embedJavaButton.isSelected();
        Preferences.setBoolean("export.application.embed_java", selected);
        if (selected) {
          warningLabel.setText(embedWarning);
        } else {
          warningLabel.setText(nopeWarning);
        }
      }
    });
    embedJavaButton.setBorder(new EmptyBorder(3, 13, 3, 13));
    
    embedPanel.add(embedJavaButton);
    embedPanel.add(warningLabel);
    embedPanel.setBorder(new TitledBorder(Language.text("export.embed_java")));
    panel.add(embedPanel);
    
    //
    
    if (Base.isMacOS()) {
      JPanel signPanel = new JPanel();
      signPanel.setLayout(new BoxLayout(signPanel, BoxLayout.Y_AXIS));
      signPanel.setBorder(new TitledBorder(Language.text("export.code_signing")));
      
      // gatekeeper: http://support.apple.com/kb/ht5290
      // for developers: https://developer.apple.com/developer-id/
      String thePain =
        //"<html><body><font size=2>" +
        "In recent versions of OS X, Apple has introduced the \u201CGatekeeper\u201D system, " + 
        "which makes it more difficult to run applications like those exported from Processing. ";
      
      if (new File("/usr/bin/codesign_allocate").exists()) {
        thePain +=  
          "This application will be \u201Cself-signed\u201D which means that Finder may report that the " +
          "application is from an \u201Cunidentified developer\u201D. If the application will not " + 
          "run, try right-clicking the app and selecting Open from the pop-up menu. Or you can visit " +
          "System Preferences \u2192 Security & Privacy and select Allow apps downloaded from: anywhere. ";          
      } else {
        thePain += 
          "Gatekeeper requires applications to be \u201Csigned\u201D, or they will be reported as damaged. " +
          "To prevent this message, install Xcode (and the Command Line Tools) from the App Store, or visit " + 
          "System Preferences \u2192 Security & Privacy and select Allow apps downloaded from: anywhere. ";
      }
      thePain += 
        "To avoid the messages entirely, manually code sign your app. " +
        "For more information: <a href=\"\">https://developer.apple.com/developer-id/</a>";
      
      // xattr -d com.apple.quarantine thesketch.app
      
      //signPanel.add(new JLabel(thePain));
      //JEditorPane area = new JEditorPane("text/html", thePain);
      //JTextPane area = new JEditorPane("text/html", thePain);
      
//      JTextArea area = new JTextArea(thePain);
//      area.setBackground(null);
//      area.setFont(new Font("Dialog", Font.PLAIN, 10));
//      area.setLineWrap(true);
//      area.setWrapStyleWord(true);
      // Are you f-king serious, Java API developers?
      JLabel area = new JLabel("<html><div width=\"" + divWidth + "\"><font size=\"2\">" + thePain + "</div></html>");
      
      area.setBorder(new EmptyBorder(3, 13, 3, 13));
//      area.setPreferredSize(new Dimension(embedPanel.getPreferredSize().width, 100));
//      area.setPreferredSize(new Dimension(300, 200));
      signPanel.add(area);
//      signPanel.add(Box.createHorizontalGlue());
      signPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
      
      area.addMouseListener(new MouseAdapter() {
        public void mousePressed(MouseEvent event) {
          Base.openURL("https://developer.apple.com/developer-id/");
        }
      });
      
      panel.add(signPanel);
    }
    //System.out.println(panel.getPreferredSize());
//    panel.setMinimumSize(new Dimension(316, 461));
//    panel.setPreferredSize(new Dimension(316, 461));
//    panel.setMaximumSize(new Dimension(316, 461));
    
    //

    String[] options = { Language.text("prompt.export"), Language.text("prompt.cancel") };

    final JOptionPane optionPane = new JOptionPane(panel,
                                                   JOptionPane.PLAIN_MESSAGE,
                                                   JOptionPane.YES_NO_OPTION,
                                                   null,
                                                   options,
                                                   options[0]);


    final JDialog dialog = new JDialog(this, Language.text("export"), true);
    dialog.setContentPane(optionPane);
//    System.out.println(optionPane.getLayout());
    
    optionPane.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent e) {
        String prop = e.getPropertyName();

        if (dialog.isVisible() &&
            (e.getSource() == optionPane) &&
            (prop.equals(JOptionPane.VALUE_PROPERTY))) {
          // If you were going to check something before  
          // closing the window, you'd do it here.
          dialog.setVisible(false);
        }
      }
    });
    dialog.pack();
//    System.out.println("after pack: " + panel.getPreferredSize());
//    dialog.setSize(optionPane.getPreferredSize());
    dialog.setResizable(false);
    
    // Center the window in the middle of the editor
    Rectangle bounds = getBounds();
    dialog.setLocation(bounds.x + (bounds.width - dialog.getSize().width) / 2,
                       bounds.y + (bounds.height - dialog.getSize().height) / 2);
    dialog.setVisible(true);
    
    //System.out.println(panel.getSize());

    Object value = optionPane.getValue();
    if (value.equals(options[0])) {
      return jmode.handleExportApplication(sketch);
    } else if (value.equals(options[1]) || value.equals(Integer.valueOf(-1))) {
      // closed window by hitting Cancel or ESC
      statusNotice(Language.text("export.notice.exporting.cancel"));
    }
    return false;
  }

  /*
  Color bgcolor = Preferences.getColor("run.present.bgcolor");
  final ColorChooser c = new ColorChooser(JavaEditor.this, true, bgcolor, 
                                          "Select", new ActionListener() {
    
    @Override
    public void actionPerformed(ActionEvent e) {
      Preferences.setColor("run.present.bgcolor", c.getColor());
    }
  });
  */

  /*
  class ColorListener implements ActionListener {
    ColorChooser chooser;
    String prefName;
    
    public ColorListener(String prefName) {
      this.prefName = prefName;
      Color color = Preferences.getColor(prefName);
      chooser = new ColorChooser(JavaEditor.this, true, color, "Select", this);
      chooser.show();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      Color color = chooser.getColor();
      Preferences.setColor(prefName, color);
//      presentColorPanel.setBackground(color);
      presentColorPanel.repaint();
      chooser.hide();
    }    
  }
  */
  
  class ColorPreference extends JPanel implements ActionListener {
    ColorChooser chooser;
    String prefName;
    
    public ColorPreference(String pref) {
      prefName = pref;
      
      setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
      setPreferredSize(new Dimension(30, 20));
      setMaximumSize(new Dimension(30, 20));
      
      addMouseListener(new MouseAdapter() {
        public void mouseReleased(MouseEvent e) {
          Color color = Preferences.getColor(prefName);
          chooser = new ColorChooser(JavaEditor.this, true, color, Language.text("color_chooser.select"), ColorPreference.this);
          chooser.show();
        }
      });
    }
    
    public void paintComponent(Graphics g) {
      g.setColor(Preferences.getColor(prefName));
      Dimension size = getSize();
      g.fillRect(0, 0, size.width, size.height);
    }

    public void actionPerformed(ActionEvent e) {
      Color color = chooser.getColor();
      Preferences.setColor(prefName, color);
      //presentColorPanel.repaint();
      repaint();
      chooser.hide();
    }
  }
  
  
//  protected void selectColor(String prefName) {
//    Color color = Preferences.getColor(prefName);
//    final ColorChooser chooser = new ColorChooser(JavaEditor.this, true, color, 
//                                            "Select", new ActionListener() {
//      
//      @Override
//      public void actionPerformed(ActionEvent e) {
//        Preferences.setColor(prefName, c.getColor());
//      }
//    });
//  }

  
  /**
   * Checks to see if the sketch has been modified, and if so,
   * asks the user to save the sketch or cancel the export.
   * This prevents issues where an incomplete version of the sketch
   * would be exported, and is a fix for
   * <A HREF="http://dev.processing.org/bugs/show_bug.cgi?id=157">Bug 157</A>
   */
  protected boolean handleExportCheckModified() {
    if (sketch.isModified()) {
      Object[] options = { Language.text("prompt.ok"), Language.text("prompt.cancel") };
      int result = JOptionPane.showOptionDialog(this,
                                                Language.text("export.unsaved_changes"),
                                                Language.text("menu.file.save"),
                                                JOptionPane.OK_CANCEL_OPTION,
                                                JOptionPane.QUESTION_MESSAGE,
                                                null,
                                                options,
                                                options[0]);

      if (result == JOptionPane.OK_OPTION) {
        handleSave(true);

      } else {
        // why it's not CANCEL_OPTION is beyond me (at least on the mac)
        // but f-- it.. let's get this shite done..
        //} else if (result == JOptionPane.CANCEL_OPTION) {
        statusNotice(Language.text("export.notice.cancel.unsaved_changes"));
        //toolbar.clear();
        return false;
      }
    }
    return true;
  }


  public void handleRun() {
    new Thread(new Runnable() {
      public void run() {
        prepareRun();
        try {
          toolbar.activate(JavaToolbar.RUN);
          runtime = jmode.handleRun(sketch, JavaEditor.this);
//          System.out.println("runtime now " + runtime);
        } catch (Exception e) {
          statusError(e);
        }
      }
    }).start();
  }


  public void handlePresent() {
    new Thread(new Runnable() {
      public void run() {
        prepareRun();
        try {
          toolbar.activate(JavaToolbar.RUN);
          runtime = jmode.handlePresent(sketch, JavaEditor.this);
        } catch (Exception e) {
          statusError(e);
        }
      }
    }).start();
  }


  public void handleStop() {
    toolbar.activate(JavaToolbar.STOP);

    try {
      //jmode.handleStop();
      if (runtime != null) {
        runtime.close();  // kills the window
        runtime = null;
//      } else {
//        System.out.println("runtime is null");
      }
    } catch (Exception e) {
      statusError(e);
    }

    toolbar.deactivate(JavaToolbar.RUN);
    toolbar.deactivate(JavaToolbar.STOP);

    // focus the PDE again after quitting presentation mode [toxi 030903]
    toFront();
  }


  public void handleSave() {
//    toolbar.activate(JavaToolbar.SAVE);
    super.handleSave(true);
//    toolbar.deactivate(JavaToolbar.SAVE);
  }


  public boolean handleSaveAs() {
//    toolbar.activate(JavaToolbar.SAVE);
    boolean result = super.handleSaveAs();
//    toolbar.deactivate(JavaToolbar.SAVE);
    return result;
  }


  /**
   * Add import statements to the current tab for all of packages inside
   * the specified jar file.
   */
  public void handleImportLibrary(String libraryName) {

    // make sure the user didn't hide the sketch folder
    sketch.ensureExistence();

    // import statements into the main sketch file (code[0])
    // if the current code is a .java file, insert into current
    //if (current.flavor == PDE) {
    if (mode.isDefaultExtension(sketch.getCurrentCode())) {
      sketch.setCurrentCode(0);
    }
    
    Library lib = mode.findLibraryByName(libraryName);
    if (lib == null) {
      statusError("Unable to locate library: "+libraryName);
      return;
    }

    // could also scan the text in the file to see if each import
    // statement is already in there, but if the user has the import
    // commented out, then this will be a problem.
    String[] list = lib.getSpecifiedImports(); // ask the library for its imports 
    if (list == null) {
      
      // Default to old behavior and load each package in the primary jar
      list = Base.packageListFromClassPath(lib.getJarPath());
    }

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < list.length; i++) {
      sb.append("import ");
      sb.append(list[i]);
      sb.append(".*;\n");
    }
    sb.append('\n');
    sb.append(getText());
    setText(sb.toString());
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


//  public void deactivateExport() {
//    toolbar.deactivate(JavaToolbar.EXPORT);
//  }


  public void internalCloseRunner() {
    //jmode.handleStop();
    handleStop();
  }
}

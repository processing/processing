/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdeEditor - main editor panel for the processing development environment
  Part of the Processing project - http://processing.org

  Except where noted, code is written by Ben Fry and
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

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.*;

import com.oroinc.text.regex.*;

import com.apple.mrj.*;


public class PdeEditor extends JFrame
implements MRJAboutHandler, MRJQuitHandler, MRJPrefsHandler
{
  // yeah
  static final String WINDOW_TITLE = "Processing";

  // p5 icon for the window
  Image icon;

  // otherwise, if the window is resized with the message label
  // set to blank, it's preferredSize() will be fukered
  static final String EMPTY =
    "                                                                     " +
    "                                                                     " +
    "                                                                     ";

  static final int HANDLE_NEW  = 1;
  static final int HANDLE_OPEN = 2;
  static final int HANDLE_QUIT = 3;
  int checkModifiedMode;
  String handleOpenPath;
  boolean handleNewShift;
  boolean handleNewLibrary;
  //String handleSaveAsPath;
  //String openingName;

  PdeEditorButtons buttons;
  PdeEditorHeader header;
  PdeEditorStatus status;
  PdeEditorConsole console;

  JSplitPane splitPane;
  JPanel consolePanel;

  // currently opened program
  public PdeSketch sketch;

  public JEditTextArea textarea;
  PdeEditorListener listener;

  // runtime information and window placement
  Point appletLocation;
  Point presentLocation;
  Window presentationWindow;
  RunButtonWatcher watcher;
  PdeRuntime runtime;

  //boolean externalRuntime;
  //String externalPaths;
  //File externalCode;

  JMenuItem exportAppItem;
  JMenuItem saveMenuItem;
  JMenuItem saveAsMenuItem;
  //JMenuItem beautifyMenuItem;

  //

  boolean running;
  boolean presenting;

  // undo fellers
  JMenuItem undoItem, redoItem;
  protected UndoAction undoAction;
  protected RedoAction redoAction;
  static public UndoManager undo = new UndoManager(); // editor needs this guy

  //

  //PdeHistory history;  // TODO re-enable history
  PdeSketchbook sketchbook;
  PdePreferences preferences;
  PdeEditorFind find;

  //static Properties keywords; // keyword -> reference html lookup


  public PdeEditor() {
    super(WINDOW_TITLE + " - " + PdeBase.VERSION);
    // this is needed by just about everything else
    preferences = new PdePreferences();

    // #@$*(@#$ apple.. always gotta think different
    MRJApplicationUtils.registerAboutHandler(this);
    MRJApplicationUtils.registerPrefsHandler(this);
    MRJApplicationUtils.registerQuitHandler(this);

    // set the window icon
    try {
      icon = PdeBase.getImage("icon.gif", this);
      setIconImage(icon);
    } catch (Exception e) { } // fail silently, no big whup


    // add listener to handle window close box hit event
    addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          handleQuit();
        }
      });

    PdeKeywords keywords = new PdeKeywords();
    // TODO re-enable history
    //history = new PdeHistory(this);
    sketchbook = new PdeSketchbook(this);

    JMenuBar menubar = new JMenuBar();
    menubar.add(buildFileMenu());
    menubar.add(buildEditMenu());
    menubar.add(buildSketchMenu());
    menubar.add(buildToolsMenu());
    // what platform has their help menu way on the right?
    //if ((PdeBase.platform == PdeBase.WINDOWS) ||
    //menubar.add(Box.createHorizontalGlue());
    menubar.add(buildHelpMenu());

    setJMenuBar(menubar);

    // doesn't matter when this is created, just make it happen at some point
    find = new PdeEditorFind(PdeEditor.this);

    Container pain = getContentPane();
    pain.setLayout(new BorderLayout());

    buttons = new PdeEditorButtons(this);
    pain.add("West", buttons);

    JPanel rightPanel = new JPanel();
    rightPanel.setLayout(new BorderLayout());

    header = new PdeEditorHeader(this);
    rightPanel.add(header, BorderLayout.NORTH);

    textarea = new JEditTextArea(new PdeTextAreaDefaults());
    textarea.setRightClickPopup(new TextAreaPopup());
    textarea.setTokenMarker(new PdeKeywords());

    textarea.setHorizontalOffset(4);
    //textarea.setBorder(new EmptyBorder(0, 20, 0, 0));
    //textarea.setBackground(Color.white);

    // assemble console panel, consisting of status area and the console itself
    consolePanel = new JPanel();
    //System.out.println(consolePanel.getInsets());
    consolePanel.setLayout(new BorderLayout());

    status = new PdeEditorStatus(this);
    consolePanel.add(status, BorderLayout.NORTH);

    console = new PdeEditorConsole(this);
    consolePanel.add(console, BorderLayout.CENTER);

    splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                               textarea, consolePanel);

    splitPane.setOneTouchExpandable(true);
    // repaint child panes while resizing
    splitPane.setContinuousLayout(true);
    // if window increases in size, give all of increase to textarea (top pane)
    splitPane.setResizeWeight(1D);

    // to fix ugliness.. normally macosx java 1.3 puts an
    // ugly white border around this object, so turn it off.
    if (PdeBase.platform == PdeBase.MACOSX) {
      splitPane.setBorder(null);
    }

    // the default size on windows is too small and kinda ugly
    int dividerSize = PdePreferences.getInteger("editor.divider.size");
    if (dividerSize != 0) {
      splitPane.setDividerSize(dividerSize);
    }

    rightPanel.add(splitPane, BorderLayout.CENTER);

    pain.add("Center", rightPanel);

    // hopefully these are no longer needed w/ swing
    // (har har har.. that was wishful thinking)
    listener = new PdeEditorListener(this, textarea);
    textarea.pdeEditorListener = listener;

    // set the undo stuff for this feller
    Document document = textarea.getDocument();
    document.addUndoableEditListener(new PdeUndoableEditListener());

    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    if ((PdeBase.platform == PdeBase.MACOSX) ||
        (PdeBase.platform == PdeBase.MACOS9)) {
      presentationWindow = new Frame();

      // mrj is still (with version 2.2.x) a piece of shit,
      // and doesn't return valid insets for frames
      //presentationWindow.pack(); // make a peer so insets are valid
      //Insets insets = presentationWindow.getInsets();
      // the extra +20 is because the resize boxes intrude
      Insets insets = new Insets(21, 5, 5 + 20, 5);

      presentationWindow.setBounds(-insets.left, -insets.top,
                                   screen.width + insets.left + insets.right,
                                   screen.height + insets.top + insets.bottom);
    } else {
      presentationWindow = new Frame();
      //((Frame)presentationWindow).setUndecorated(true);
      try {
        Method undecoratedMethod =
          Frame.class.getMethod("setUndecorated",
                                new Class[] { Boolean.TYPE });
        undecoratedMethod.invoke(presentationWindow,
                                 new Object[] { Boolean.TRUE });
      } catch (Exception e) { }
      //} catch (NoSuchMethodException e) { }
      //} catch (NoSuchMethodError e) { }

      presentationWindow.setBounds(0, 0, screen.width, screen.height);
    }

    Label label = new Label("stop");
    label.addMouseListener(new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
          setVisible(true);
          doClose();
        }});

    Dimension labelSize = new Dimension(60, 20);
    presentationWindow.setLayout(null);
    presentationWindow.add(label);
    label.setBounds(5, screen.height - 5 - labelSize.height,
                    labelSize.width, labelSize.height);

    Color presentationBgColor =
      PdePreferences.getColor("run.present.bgcolor");
    presentationWindow.setBackground(presentationBgColor);

    textarea.addFocusListener(new FocusAdapter() {
        public void focusGained(FocusEvent e) {
          if (presenting == true) {
            try {
              presentationWindow.toFront();
              runtime.applet.requestFocus();
            } catch (Exception ex) { }
          }
        }
      });

    this.addFocusListener(new FocusAdapter() {
        public void focusGained(FocusEvent e) {
          if (presenting == true) {
            try {
              presentationWindow.toFront();
              runtime.applet.requestFocus();
            } catch (Exception ex) { }
          }
        }
      });

    // moved from the PdeRuntime window to the main presentation window
    // [toxi 030903]
    presentationWindow.addKeyListener(new KeyAdapter() {
        public void keyPressed(KeyEvent e) {
          //System.out.println("window got " + e);
          if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            runtime.stop();
            doClose();
          } else {
            // pass on the event to the applet [toxi 030903]
            runtime.applet.keyPressed(e);
          }
        }
      });
  }


  /**
   * Hack for #@#)$(* Mac OS X.
   */
  public Dimension getMinimumSize() {
    return new Dimension(500, 500);
  }


  // ...................................................................


  /**
   * Post-constructor setup for the editor area. Loads the last
   * sketch that was used (if any), and restores other Editor settings.
   * The complement to "storePreferences", this is called when the
   * application is first launched.
   */
  public void restorePreferences() {
    // figure out window placement

    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    boolean windowPositionInvalid = false;

    if (PdePreferences.get("last.screen.height") != null) {
      // if screen size has changed, the window coordinates no longer
      // make sense, so don't use them unless they're identical
      int screenW = PdePreferences.getInteger("last.screen.width");
      int screenH = PdePreferences.getInteger("last.screen.height");

      if ((screen.width != screenW) || (screen.height != screenH)) {
        windowPositionInvalid = true;
      }
    } else {
      windowPositionInvalid = true;
    }

    if (windowPositionInvalid) {
      //System.out.println("using default size");
      int windowH = PdePreferences.getInteger("default.window.height");
      int windowW = PdePreferences.getInteger("default.window.width");
      setBounds((screen.width - windowW) / 2,
                (screen.height - windowH) / 2,
                windowW, windowH);
      // this will be invalid as well, so grab the new value
      PdePreferences.setInteger("last.divider.location",
                                splitPane.getDividerLocation());
    } else {
      setBounds(PdePreferences.getInteger("last.window.x"),
                PdePreferences.getInteger("last.window.y"),
                PdePreferences.getInteger("last.window.width"),
                PdePreferences.getInteger("last.window.height"));
    }


    // last sketch that was in use

    //String sketchName = PdePreferences.get("last.sketch.name");
    String sketchPath = PdePreferences.get("last.sketch.path");
    //PdeSketch sketchTemp = new PdeSketch(sketchPath);

    if ((sketchPath != null) && (new File(sketchPath)).exists()) {
      // don't check modified because nothing is open yet
      handleOpen2(sketchPath);

    } else {
      handleNew2(true);
    }


    // location for the console/editor area divider

    int location = PdePreferences.getInteger("last.divider.location");
    splitPane.setDividerLocation(location);


    // read the preferences that are settable in the preferences window

    applyPreferences();
  }


  /**
   * Read and apply new values from the preferences, either because
   * the app is just starting up, or the user just finished messing
   * with things in the Preferences window.
   */
  public void applyPreferences() {

    // apply the setting for 'use external editor'
    boolean external = PdePreferences.getBoolean("editor.external");

    textarea.setEditable(!external);
    saveMenuItem.setEnabled(!external);
    saveAsMenuItem.setEnabled(!external);
    //beautifyMenuItem.setEnabled(!external);

    TextAreaPainter painter = textarea.getPainter();
    if (external) {
      // disable line highlight and turn off the caret when disabling
      Color color = PdePreferences.getColor("editor.external.bgcolor");
      painter.setBackground(color);
      painter.lineHighlight = false;
      textarea.setCaretVisible(false);

    } else {
      Color color = PdePreferences.getColor("editor.bgcolor");
      painter.setBackground(color);
      painter.lineHighlight =
        PdePreferences.getBoolean("editor.linehighlight");
      textarea.setCaretVisible(true);
    }

    // in case tab expansion stuff has changed
    listener.applyPreferences();

    // in case moved to a new location
    sketchbook.rebuildMenus();
  }


  /**
   * Store preferences about the editor's current state.
   * Called when the application is quitting.
   */
  public void storePreferences() {
    //System.out.println("storing preferences");

    // window location information
    Rectangle bounds = getBounds();
    PdePreferences.setInteger("last.window.x", bounds.x);
    PdePreferences.setInteger("last.window.y", bounds.y);
    PdePreferences.setInteger("last.window.width", bounds.width);
    PdePreferences.setInteger("last.window.height", bounds.height);

    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    PdePreferences.setInteger("last.screen.width", screen.width);
    PdePreferences.setInteger("last.screen.height", screen.height);

    // last sketch that was in use
    //PdePreferences.set("last.sketch.name", sketchName);
    //PdePreferences.set("last.sketch.name", sketch.name);
    PdePreferences.set("last.sketch.path", sketch.getMainFilePath());

    // location for the console/editor area divider
    int location = splitPane.getDividerLocation();
    PdePreferences.setInteger("last.divider.location", location);
  }


  // ...................................................................


  protected JMenu buildFileMenu() {
    JMenuItem item;
    JMenu menu = new JMenu("File");

    if (!PdePreferences.getBoolean("export.library")) {
      item = newJMenuItem("New", 'N');
      item.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            handleNew(false);
          }
        });
      menu.add(item);

    } else {
      item = newJMenuItem("New Sketch", 'N');
      item.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            handleNew(false);
          }
        });
      menu.add(item);

      item = new JMenuItem("New Library");
      item.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            handleNewLibrary();
          }
        });
      menu.add(item);
    }

    /*
    item = newJMenuItem("New code", 'N', true);
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleNewCode();
        }
      });
    menu.add(item);
    */

    /*
    item = newJMenuItem("Open", 'O');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleOpen(null);
        }
      });
    menu.add(item);
    menu.add(sketchbook.rebuildMenu());
    menu.add(sketchbook.getExamplesMenu());
    */
    menu.add(sketchbook.getOpenMenu());

    saveMenuItem = newJMenuItem("Save", 'S');
    saveMenuItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleSave();
        }
      });
    menu.add(saveMenuItem);

    saveAsMenuItem = newJMenuItem("Save As...", 'S', true);
    saveAsMenuItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleSaveAs();
        }
      });
    menu.add(saveAsMenuItem);

    item = newJMenuItem("Export", 'E');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleExport();
        }
      });
    menu.add(item);

    exportAppItem = newJMenuItem("Export Application", 'E', true);
    exportAppItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleExportApp();
        }
      });
    menu.add(exportAppItem);

    menu.addSeparator();

    item = newJMenuItem("Page Setup", 'P', true);
    item.setEnabled(false);
    menu.add(item);

    item = newJMenuItem("Print", 'P');
    item.setEnabled(false);
    menu.add(item);

    // macosx already has its own preferences and quit menu
    if (PdeBase.platform != PdeBase.MACOSX) {
      menu.addSeparator();

      item = new JMenuItem("Preferences");
      item.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            handlePrefs();
          }
        });
      menu.add(item);

      menu.addSeparator();

      item = newJMenuItem("Quit", 'Q');
      item.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            handleQuit();
          }
        });
      menu.add(item);
    }
    return menu;
  }


  protected JMenu buildSketchMenu() {
    JMenuItem item;
    JMenu menu = new JMenu("Sketch");

    item = newJMenuItem("Run", 'R');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleRun(false);
        }
      });
    menu.add(item);

    item = newJMenuItem("Present", 'R', true);
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleRun(true);
        }
      });
    menu.add(item);

    //menu.add(newJMenuItem("Stop", 'T'));
    menu.add(new JMenuItem("Stop"));
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleStop();
        }
      });
    menu.addSeparator();

    //

    item = new JMenuItem("Add File...");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          sketch.addFile();
        }
      });
    menu.add(item);

    menu.add(sketchbook.getImportMenu());

    if ((PdeBase.platform == PdeBase.WINDOWS) ||
        (PdeBase.platform == PdeBase.MACOSX)) {
      // no way to do an 'open in file browser' on other platforms
      // since there isn't any sort of standard
      item = new JMenuItem("Show Sketch Folder");
      item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          //PdeBase.openFolder(sketchDir);
          PdeBase.openFolder(sketch.folder);
        }
      });
      menu.add(item);
    }

    // TODO re-enable history
    //history.attachMenu(menu);
    return menu;
  }


  protected JMenu buildToolsMenu() {
    JMenuItem item;
    JMenu menu = new JMenu("Tools");

    item = new JMenuItem("Auto Format");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleBeautify();
        }
      });
    menu.add(item);

    item = new JMenuItem("Create Font...");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          new PdeFontBuilder().show(sketch.dataFolder);
        }
      });
    menu.add(item);

    item = new JMenuItem("Archive Sketch");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          //new PdeFontBuilder().show(sketch.dataFolder);
          Archiver archiver = new Archiver();
          archiver.setup(PdeEditor.this);
          archiver.show();
        }
      });
    menu.add(item);

    return menu;
  }


  protected JMenu buildHelpMenu() {
    JMenu menu = new JMenu("Help");
    JMenuItem item;

    item = new JMenuItem("Environment");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          PdeBase.openURL(System.getProperty("user.dir") + File.separator +
                          "reference" + File.separator + "environment" +
                          File.separator + "index.html");
        }
      });
    menu.add(item);

    item = new JMenuItem("Reference");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          PdeBase.openURL(System.getProperty("user.dir") + File.separator +
                          "reference" + File.separator + "index.html");
        }
      });
    menu.add(item);

    item = newJMenuItem("Find in Reference", 'F', true);
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (textarea.isSelectionActive()) {
            String text = textarea.getSelectedText();
            if (text.length() == 0) {
              message("First select a word to find in the reference.");

            } else {
              String referenceFile = PdeKeywords.getReference(text);
              if (referenceFile == null) {
                message("No reference available for \"" + text + "\"");
              } else {
                PdeBase.showReference(referenceFile);
              }
            }
          }
        }
      });
    menu.add(item);

    item = newJMenuItem("Visit Processing.org", '5');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          PdeBase.openURL("http://processing.org/");
        }
      });
    menu.add(item);

    // macosx already has its own about menu
    if (PdeBase.platform != PdeBase.MACOSX) {
      menu.addSeparator();
      item = new JMenuItem("About Processing");
      item.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            handleAbout();
          }
        });
      menu.add(item);
    }

    return menu;
  }


  public JMenu buildEditMenu() {
    JMenu menu = new JMenu("Edit");
    JMenuItem item;

    undoItem = newJMenuItem("Undo", 'Z');
    undoItem.addActionListener(undoAction = new UndoAction());
    menu.add(undoItem);

    redoItem = newJMenuItem("Redo", 'Y');
    redoItem.addActionListener(redoAction = new RedoAction());
    menu.add(redoItem);

    menu.addSeparator();

    // TODO "cut" and "copy" should really only be enabled
    // if some text is currently selected
    item = newJMenuItem("Cut", 'X');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          textarea.cut();
          sketch.setModified();
        }
      });
    menu.add(item);

    item = newJMenuItem("Copy", 'C');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          textarea.copy();
        }
      });
    menu.add(item);

    item = newJMenuItem("Paste", 'V');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          textarea.paste();
          sketch.setModified();
        }
      });
    menu.add(item);

    item = newJMenuItem("Select All", 'A');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          textarea.selectAll();
        }
      });
    menu.add(item);

    menu.addSeparator();

    item = newJMenuItem("Find...", 'F');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          find.show();
        }
      });
    menu.add(item);

    item = newJMenuItem("Find Next", 'G');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          // TODO find next should only be enabled after a
          // search has actually taken place
          find.find(true);
        }
      });
    menu.add(item);

    return menu;
  }


  /**
   * Convenience method for the antidote to overthought
   * swing api mess for setting accelerators.
   */
  static public JMenuItem newJMenuItem(String title, int what) {
    return newJMenuItem(title, what, false);
  }


  /**
   * A software engineer, somewhere, needs to have his abstraction
   * taken away. I hear they jail people in third world countries for
   * writing the sort of crappy api that would require a four line
   * helpher function to *set the command key* for a menu item.
   */
  static public JMenuItem newJMenuItem(String title,
                                       int what, boolean shift) {
    JMenuItem menuItem = new JMenuItem(title);
    int modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    if (shift) modifiers |= ActionEvent.SHIFT_MASK;
    menuItem.setAccelerator(KeyStroke.getKeyStroke(what, modifiers));
    return menuItem;
  }


  // ...................................................................


  // This one listens for edits that can be undone.
  protected class PdeUndoableEditListener implements UndoableEditListener {
    public void undoableEditHappened(UndoableEditEvent e) {
      // Remember the edit and update the menus.
      undo.addEdit(e.getEdit());
      undoAction.updateUndoState();
      redoAction.updateRedoState();
    }
  }


  class UndoAction extends AbstractAction {
    public UndoAction() {
      super("Undo");
      this.setEnabled(false);
    }

    public void actionPerformed(ActionEvent e) {
      try {
        undo.undo();
      } catch (CannotUndoException ex) {
        //System.out.println("Unable to undo: " + ex);
        //ex.printStackTrace();
      }
      updateUndoState();
      redoAction.updateRedoState();
    }

    protected void updateUndoState() {
      if (undo.canUndo()) {
        this.setEnabled(true);
        undoItem.setEnabled(true);
        putValue(Action.NAME, undo.getUndoPresentationName());
      } else {
        this.setEnabled(false);
        undoItem.setEnabled(false);
        putValue(Action.NAME, "Undo");
      }
    }
  }


  class RedoAction extends AbstractAction {
    public RedoAction() {
      super("Redo");
      this.setEnabled(false);
    }

    public void actionPerformed(ActionEvent e) {
      try {
        undo.redo();
      } catch (CannotRedoException ex) {
        //System.out.println("Unable to redo: " + ex);
        //ex.printStackTrace();
      }
      updateRedoState();
      undoAction.updateUndoState();
    }

    protected void updateRedoState() {
      if (undo.canRedo()) {
        this.setEnabled(true);
        redoItem.setEnabled(true);
        putValue(Action.NAME, undo.getRedoPresentationName());
      } else {
        this.setEnabled(false);
        redoItem.setEnabled(false);
        putValue(Action.NAME, "Redo");
      }
    }
  }


  // ...................................................................


  // interfaces for MRJ Handlers, but naming is fine
  // so used internally for everything else

  public void handleAbout() {
    //System.out.println("the about box will now be shown");
    final Image image = PdeBase.getImage("about.jpg", this);
    int w = image.getWidth(this);
    int h = image.getHeight(this);
    final Window window = new Window(this) {
        public void paint(Graphics g) {
          g.drawImage(image, 0, 0, null);

          /*
            // does nothing..
          Graphics2D g2 = (Graphics2D) g;
          g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                              RenderingHints.VALUE_ANTIALIAS_OFF);
          */

          g.setFont(new Font("SansSerif", Font.PLAIN, 11));
          g.setColor(Color.white);
          g.drawString(PdeBase.VERSION, 50, 30);
        }
      };
    window.addMouseListener(new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
          window.dispose();
        }
      });
    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    window.setBounds((screen.width-w)/2, (screen.height-h)/2, w, h);
    window.show();
  }


  /**
   * Show the (already created on app init) preferences window.
   */
  public void handlePrefs() {
    // since this can't actually block, it'll hide
    // the editor window while the prefs are open
    preferences.showFrame(this);
    // and then call applyPreferences if 'ok' is hit
    // and then unhide

    // may need to rebuild sketch and other menus
    //applyPreferences();

    // next have editor do its thing
    //editor.appyPreferences();
  }


  // ...................................................................


  /**
   * Get the contents of the current buffer. Used by the PdeSketch class.
   */
  public String getText() {
    return textarea.getText();
  }


  /**
   * Called by PdeEditorHeader when the tab is changed
   * (or a new set of files are opened).
   * @param discardUndo true if undo info to this point should be ignored
   */
  public void setText(String what, boolean discardUndo) {
    textarea.setText(what);

    if (discardUndo) undo.discardAllEdits();

    textarea.select(0, 0);    // move to the beginning of the document
    textarea.requestFocus();  // get the caret blinking
  }


  public void handleRun(boolean present) {
    doClose();
    running = true;
    buttons.run();

    // do this for the terminal window / dos prompt / etc
    for (int i = 0; i < 10; i++) System.out.println();

    // clear the console on each run, unless the user doesn't want to
    //if (PdeBase.getBoolean("console.auto_clear", true)) {
    //if (PdePreferences.getBoolean("console.auto_clear", true)) {
    if (PdePreferences.getBoolean("console.auto_clear")) {
      console.clear();
    }

    presenting = present;
    if (presenting) {
      // wipe everything out with a bulbous screen-covering window
      presentationWindow.show();
      presentationWindow.toFront();
    }

    try {
      if (!sketch.handleRun()) return;

      runtime = new PdeRuntime(sketch, this);
      runtime.start(presenting ? presentLocation : appletLocation);
      watcher = new RunButtonWatcher();

    } catch (PdeException e) {
      error(e);

    } catch (Exception e) {
      e.printStackTrace();
    }
    //sketch.cleanup();  // where does this go?
  }


  class RunButtonWatcher implements Runnable {
    Thread thread;

    public RunButtonWatcher() {
      thread = new Thread(this, "run button watcher");
      thread.setPriority(Thread.MIN_PRIORITY);
      thread.start();
    }

    public void run() {
      while (Thread.currentThread() == thread) {
        if (runtime == null) {
          stop();

        } else {
          if (runtime.applet != null) {
            if (runtime.applet.finished) {
              stop();
            }
            //buttons.running(!runtime.applet.finished);

          } else if (runtime.process != null) {
            //buttons.running(true);  // ??

          } else {
            stop();
          }
        }
        try {
          Thread.sleep(250);
        } catch (InterruptedException e) { }
        //System.out.println("still inside runner thread");
      }
    }

    public void stop() {
      buttons.running(false);
      thread = null;
    }
  }


  public void handleStop() {  // called by menu or buttons
    if (presenting) {
      doClose();
    } else {
      doStop();
    }
  }


  /**
   * Stop the applet but don't kill its window.
   */
  public void doStop() {
    if (runtime != null) runtime.stop();
    if (watcher != null) watcher.stop();
    message(EMPTY);

    // the buttons are sometimes still null during the constructor
    // is this still true? are people still hitting this error?
    /*if (buttons != null)*/ buttons.clear();

    running = false;
  }


  /**
   * Stop the applet and kill its window. When running in presentation
   * mode, this will always be called instead of doStop().
   */
  public void doClose() {
    if (presenting) {
      presentationWindow.hide();

    } else {
      try {
        // the window will also be null the process was running
        // externally. so don't even try setting if window is null
        // since PdeRuntime will set the appletLocation when an
        // external process is in use.
        if (runtime.window != null) {
          appletLocation = runtime.window.getLocation();
        }
      } catch (NullPointerException e) { }
    }

    //if (running) doStop();
    doStop();  // need to stop if runtime error

    try {
      if (runtime != null) {
        runtime.close();  // kills the window
        runtime = null; // will this help?
      }
    } catch (Exception e) { }
    //buttons.clear();  // done by doStop

    sketch.cleanup();

    // [toxi 030903]
    // focus the PDE again after quitting presentation mode
    toFront();
  }


  /**
   * Check to see if there have been changes. If so, prompt user
   * whether or not to save first. If the user cancels, just ignore.
   * Otherwise, one of the other methods will handle calling
   * checkModified2() which will get on with business.
   */
  protected void checkModified(int checkModifiedMode) {
    this.checkModifiedMode = checkModifiedMode;

    if (!sketch.modified) {
      checkModified2();
      return;
    }

    String prompt = "Save changes to " + sketch.name + "?  ";

    if (checkModifiedMode != HANDLE_QUIT) {
      // if the user is not quitting, then use the nicer
      // dialog that's actually inside the p5 window.
      status.prompt(prompt);

    } else {
      // if the user selected quit, then this has to be done with
      // a JOptionPane instead of internally in the editor.
      // TODO this is actually just a bug to be fixed.

      // macosx java kills the app even though cancel might get hit
      // so the cancel button is (temporarily) left off
      // this may be treated differently in macosx java 1.4,
      // but 1.4 isn't currently stable enough to use.

      // turns out windows has the same problem (sometimes)
      // disable cancel for now until a fix can be found.

      Object[] options = { "Yes", "No" };
      int result = JOptionPane.showOptionDialog(this,
                                                prompt,
                                                "Quit",
                                                JOptionPane.YES_NO_OPTION,
                                                JOptionPane.QUESTION_MESSAGE,
                                                null,
                                                options,
                                                options[0]);

      if (result == JOptionPane.YES_OPTION) {
        handleSave();
        checkModified2();

      } else if (result == JOptionPane.NO_OPTION) {
        checkModified2();  // though this may just quit

      } else if (result == JOptionPane.CANCEL_OPTION) {
        // ignored
      }
    }
  }


  /**
   * Called by PdeEditorStatus to complete the job.
   */
  public void checkModified2() {
    switch (checkModifiedMode) {
      case HANDLE_NEW:  handleNew2(false); break;
      case HANDLE_OPEN: handleOpen2(handleOpenPath); break;
      case HANDLE_QUIT: handleQuit2(); break;
    }
    checkModifiedMode = 0;
  }


  /**
   * New was called (by buttons or by menu), first check modified
   * and if things work out ok, handleNew2() will be called.
   *
   * If shift is pressed when clicking the toolbar button, then
   * force the opposite behavior from sketchbook.prompt's setting
   */
  public void handleNew(boolean shift) {
    doStop();
    handleNewShift = shift;
    handleNewLibrary = false;
    checkModified(HANDLE_NEW);
  }


  /**
   * User selected "New Library", this will act just like handleNew
   * but internally set a flag that the new guy is a library,
   * meaning that a "library" subfolder will be added.
   */
  public void handleNewLibrary() {
    doStop();
    handleNewShift = false;
    handleNewLibrary = true;
    checkModified(HANDLE_NEW);
  }


  /**
   * Does all the plumbing to create a new project
   * then calls handleOpen to load it up.
   * @param startup true if the app is starting (auto-create a sketch)
   */
  protected void handleNew2(boolean startup) {
    try {
      String pdePath =
        sketchbook.handleNew(startup, handleNewShift, handleNewLibrary);
      if (pdePath != null) handleOpen2(pdePath);

    } catch (IOException e) {
      // not sure why this would happen, but since there's no way to
      // recover (outside of creating another new setkch, which might
      // just cause more trouble), then they've gotta quit.
      PdeBase.showError("Problem creating a new sketch",
                        "An error occurred while creating\n" +
                        "a new sketch. Processing must now quit.", e);
    }
  }


  /**
   * Open a sketch given the full path to the .pde file.
   * Pass in 'null' to prompt the user for the name of the sketch.
   */
  public void handleOpen(String path) {
    if (path == null) {  // "open..." selected from the menu
      path = sketchbook.handleOpen();
      if (path == null) return;
    }
    doStop();
    handleOpenPath = path;
    checkModified(HANDLE_OPEN);
  }


  /**
   * Second stage of open, occurs after having checked to
   * see if the modifications (if any) to the previous sketch
   * need to be saved.
   */
  protected void handleOpen2(String path) {
    try {
      // check to make sure that this .pde file is
      // in a folder of the same name
      File file = new File(path);
      File parentFile = new File(file.getParent());
      String parentName = parentFile.getName();
      String pdeName = parentName + ".pde";
      File altFile = new File(file.getParent(), pdeName);

      //System.out.println("path = " + file.getParent());
      //System.out.println("name = " + file.getName());
      //System.out.println("pname = " + parentName);

      if (pdeName.equals(file.getName())) {
        // no beef with this guy

      } else if (altFile.exists()) {
        // user selected a .java from the same sketch,
        // but open the .pde instead
        path = altFile.getAbsolutePath();
        //System.out.println("found alt file in same folder");

      } else if (!path.endsWith(".pde")) {
        PdeBase.showWarning("Bad file selected",
                            "Processing can only open its own sketches\n" +
                            "and other files ending in .pde", null);
        return;

      } else {
        String properParent =
          file.getName().substring(0, file.getName().length() - 4);

        Object[] options = { "OK", "Cancel" };
        String prompt =
          "The file \"" + file.getName() + "\" needs to be inside\n" +
          "a sketch folder named \"" + properParent + "\".\n" +
          "Create this folder, move the file, and continue?";

        int result = JOptionPane.showOptionDialog(this,
                                                  prompt,
                                                  "Moving",
                                                  JOptionPane.YES_NO_OPTION,
                                                  JOptionPane.QUESTION_MESSAGE,
                                                  null,
                                                  options,
                                                  options[0]);

        if (result == JOptionPane.YES_OPTION) {
          // create properly named folder
          File properFolder = new File(file.getParent(), properParent);
          if (properFolder.exists()) {
            PdeBase.showWarning("Error",
                                "A folder named \"" + properParent + "\" " +
                                "already exists. Can't open sketch.", null);
            return;
          }
          if (!properFolder.mkdirs()) {
            throw new IOException("Couldn't create sketch folder");
          }
          // copy the sketch inside
          File properPdeFile = new File(properFolder, file.getName());
          File origPdeFile = new File(path);
          PdeBase.copyFile(origPdeFile, properPdeFile);

          // remove the original file, so user doesn't get confused
          origPdeFile.delete();

          // update with the new path
          path = properPdeFile.getAbsolutePath();

        } else if (result == JOptionPane.NO_OPTION) {
          return;
        }
      }

      sketch = new PdeSketch(this, path);
      // TODO re-enable this once export application works
      exportAppItem.setEnabled(false && !sketch.isLibrary());
      buttons.disableRun(sketch.isLibrary());
      header.rebuild();
      if (PdePreferences.getBoolean("console.auto_clear")) {
        console.clear();
      }

    } catch (Exception e) {
      error(e);
    }
  }


  // there is no handleSave1 since there's never a need to prompt
  public void handleSave() {
    message("Saving...");
    try {
      if (sketch.save()) {
        message("Done Saving.");
      } else {
        message(EMPTY);
      }
      // rebuild sketch menu in case a save-as was forced
      sketchbook.rebuildMenus();

    } catch (Exception e) {
      // show the error as a message in the window
      error(e);

      // zero out the current action,
      // so that checkModified2 will just do nothing
      checkModifiedMode = 0;
      // this is used when another operation calls a save
    }
    buttons.clear();
  }


  public void handleSaveAs() {
    doStop();

    message("Saving...");
    try {
      if (sketch.saveAs()) {
        message("Done Saving.");
        sketchbook.rebuildMenus();
      } else {
        message("Save Cancelled.");
      }

    } catch (Exception e) {
      // show the error as a message in the window
      error(e);
    }
    buttons.clear();
  }


  /**
   * Handles calling the export() function on sketch, and
   * queues all the gui status stuff that comes along with it.
   *
   * Made synchronized to (hopefully) avoid problems of people
   * hitting export twice, quickly, and horking things up.
   */
  synchronized public void handleExport() {
    String what = sketch.isLibrary() ? "Applet" : "Library";
    message("Exporting " + what + "...");
    try {
      boolean success = sketch.isLibrary() ?
        sketch.exportLibrary() : sketch.exportApplet();
      if (success) {
        message("Done exporting.");
      } else {
        // error message will already be visible
      }
    } catch (Exception e) {
      message("Error during export.");
      e.printStackTrace();
    }
    buttons.clear();
  }


  synchronized public void handleExportApp() {
    message("Exporting application...");
    try {
      if (sketch.exportApplication()) {
        message("Done exporting.");
      } else {
        // error message will already be visible
      }
    } catch (Exception e) {
      message("Error during export.");
      e.printStackTrace();
    }
    buttons.clear();
  }


  /**
   * Quit, but first ask user if it's ok. Also store preferences
   * to disk just in case they want to quit. Final exit() happens
   * in PdeEditor since it has the callback from PdeEditorStatus.
   */
  public void handleQuit() {
    // stop isn't sufficient with external vm & quit
    // instead use doClose() which will kill the external vm
    //doStop();
    doClose();

    //if (!checkModified()) return;
    checkModified(HANDLE_QUIT);
    //System.out.println("exiting doquit");
  }


  /**
   * Actually do the quit action.
   */
  protected void handleQuit2() {
    storePreferences();
    preferences.save();

    sketchbook.clean();

    //System.out.println("exiting here");
    System.exit(0);
  }


  // an improved algorithm that would still avoid a full state machine
  // 1. build an array of strings for the lines
  // 2. first remove everything between /* and */ (relentless)
  // 3. next remove anything inside two sets of " "
  //    but not if escaped with a \
  //    these can't extend beyond a line, so that works well
  //    (this will save from "http://blahblah" showing up as a comment)
  // 4. remove from // to the end of a line everywhere
  // 5. run through remaining text to do indents
  //    using hokey brace-counting algorithm
  // 6. also add indents for switch statements
  //    case blah: { }  (colons at end of line isn't a good way)
  //    maybe /case \w+\:/
  public void handleBeautify() {
    String prog = textarea.getText();

    // TODO re-enable history
    //history.record(prog, PdeHistory.BEAUTIFY);

    int tabSize = PdePreferences.getInteger("editor.tabs.size");

    char program[] = prog.toCharArray();
    StringBuffer buffer = new StringBuffer();
    boolean gotBlankLine = false;
    int index = 0;
    int level = 0;

    while (index != program.length) {
      int begin = index;
      while ((program[index] != '\n') &&
             (program[index] != '\r')) {
        index++;
        if (program.length == index)
          break;
      }
      int end = index;
      if (index != program.length) {
        if ((index+1 != program.length) &&
            // treat \r\n from windows as one line
            (program[index] == '\r') &&
            (program[index+1] == '\n')) {
          index += 2;
        } else {
          index++;
        }
      } // otherwise don't increment

      String line = new String(program, begin, end-begin);
      line = line.trim();

      if (line.length() == 0) {
        if (!gotBlankLine) {
          // let first blank line through
          buffer.append('\n');
          gotBlankLine = true;
        }
      } else {
        //System.out.println(level);
        int idx = -1;
        String myline = line.substring(0);
        while (myline.lastIndexOf('}') != idx) {
          idx = myline.indexOf('}');
          myline = myline.substring(idx+1);
          level--;
        }
        //for (int i = 0; i < level*2; i++) {
        // TODO i've since forgotten how i made this work (maybe it's even
        //      a bug) but for now, level is incrementing/decrementing in
        //      steps of two. in the interest of getting a release out,
        //      i'm just gonna roll with that since this function will prolly
        //      be replaced entirely and there are other things to worry about.
        for (int i = 0; i < tabSize * level / 2; i++) {
          buffer.append(' ');
        }
        buffer.append(line);
        buffer.append('\n');
        //if (line.charAt(0) == '{') {
        //level++;
        //}
        idx = -1;
        myline = line.substring(0);
        while (myline.lastIndexOf('{') != idx) {
          idx = myline.indexOf('{');
          myline = myline.substring(idx+1);
          level++;
        }
        gotBlankLine = false;
      }
    }

    // save current (rough) selection point
    int selectionEnd = textarea.getSelectionEnd();

    // replace with new bootiful text
    setText(buffer.toString(), false);

    // make sure the caret would be past the end of the text
    if (buffer.length() < selectionEnd - 1) {
      selectionEnd = buffer.length() - 1;
    }

    // at least in the neighborhood
    textarea.select(selectionEnd, selectionEnd);

    //setSketchModified(true);
    //sketch.setCurrentModified(true);
    sketch.setModified();
    buttons.clear();
  }


  // TODO iron out bugs with this code under
  //      different platforms, especially macintosh
  public void highlightLine(int lnum) {
    if (lnum < 0) {
      textarea.select(0, 0);
      return;
    }
    //System.out.println(lnum);
    String s = textarea.getText();
    int len = s.length();
    int st = -1;
    int ii = 0;
    int end = -1;
    int lc = 0;
    if (lnum == 0) st = 0;
    for (int i = 0; i < len; i++) {
      ii++;
      //if ((s.charAt(i) == '\n') || (s.charAt(i) == '\r')) {
      boolean newline = false;
      if (s.charAt(i) == '\r') {
        if ((i != len-1) && (s.charAt(i+1) == '\n')) {
          i++; //ii--;
        }
        lc++;
        newline = true;
      } else if (s.charAt(i) == '\n') {
        lc++;
        newline = true;
      }
      if (newline) {
        if (lc == lnum)
          //st = i+1;
          st = ii;
        else if (lc == lnum+1) {
          //end = i;
          end = ii;
          break;
        }
      }
    }
    if (end == -1) end = len;

    // sometimes KJC claims that the line it found an error in is
    // the last line in the file + 1.  Just highlight the last line
    // in this case. [dmose]
    if (st == -1) st = len;

    textarea.select(st, end);
  }


  // ...................................................................


  public void error(Exception e) {
    status.error(e.getMessage());
    e.printStackTrace();
  }


  public void error(PdeException e) {
    if (e.file >= 0) sketch.setCurrent(e.file);
    if (e.line >= 0) highlightLine(e.line);

    status.error(e.getMessage());
    buttons.clearRun();
  }


  /*
  public void finished() {
    running = false;
    buttons.clearRun();
    message("Done.");
  }
  */


  public void message(String msg) {
    status.notice(msg);
  }


  /*
  public void messageClear(String msg) {
    status.unnotice(msg);
  }
  */


  // ...................................................................


  /**
   * Returns the edit popup menu.
   */
  class TextAreaPopup extends JPopupMenu {
    //protected ReferenceKeys referenceItems = new ReferenceKeys();
    String currentDir = System.getProperty("user.dir");
    String referenceFile = null;

    JMenuItem cutItem, copyItem;
    JMenuItem referenceItem;


    public TextAreaPopup() {
      JMenuItem item;

      cutItem = new JMenuItem("Cut");
      cutItem.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            textarea.cut();
          }
      });
      this.add(cutItem);

      copyItem = new JMenuItem("Copy");
      copyItem.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            textarea.copy();
          }
        });
      this.add(copyItem);

      item = new JMenuItem("Paste");
      item.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            textarea.paste();
          }
        });
      this.add(item);

      item = new JMenuItem("Select All");
      item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          textarea.selectAll();
        }
      });
      this.add(item);

      this.addSeparator();

      referenceItem = new JMenuItem("Find in Reference");
      referenceItem.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            PdeBase.showReference(referenceFile);
          }
        });
      this.add(referenceItem);
    }

    // if no text is selected, disable copy and cut menu items
    public void show(Component component, int x, int y) {
      if (textarea.isSelectionActive()) {
        cutItem.setEnabled(true);
        copyItem.setEnabled(true);

        referenceFile = PdeKeywords.getReference(textarea.getSelectedText());
        if (referenceFile != null) {
          referenceItem.setEnabled(true);
        }
      } else {
        cutItem.setEnabled(false);
        copyItem.setEnabled(false);
        referenceItem.setEnabled(false);
      }
      super.show(component, x, y);
    }
  }
}


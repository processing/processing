/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdeEditor - main editor panel for the processing ide
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
import java.text.*;
import java.util.*;
import java.util.zip.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.*;

import com.oroinc.text.regex.*;

#ifdef MACOS
import com.apple.mrj.*;
#endif


public class PdeEditor extends JFrame
#ifdef MACOS
  implements MRJAboutHandler, MRJQuitHandler, MRJPrefsHandler
#endif 
{
  // yeah
  static final String WINDOW_TITLE = "Processing";

  // p5 icon for the window
  Image icon;

  // otherwise, if the window is resized with the message label
  // set to blank, it's preferredSize() will be fukered
  static final String EMPTY = "                                                                                                                                                             ";

  static final int SK_NEW  = 1;
  static final int SK_OPEN = 2;
  static final int DO_OPEN = 3;
  static final int DO_QUIT = 4;
  int checking;
  String openingPath; 
  String openingName;

  PdeEditorListener listener;

  PdeEditorButtons buttons;
  PdeEditorHeader header;
  PdeEditorStatus status;
  PdeEditorConsole console;

  JSplitPane splitPane;
  JPanel consolePanel;

  JEditTextArea textarea;

  // currently opened program
  String sketchName; // name of the file (w/o pde if a sketch)
  File sketchFile;   // the .pde file itself
  File sketchDir;    // if a sketchbook project, the parent dir
  boolean sketchModified;

  Point appletLocation; //= new Point(0, 0);
  Point presentLocation; // = new Point(0, 0);

  Window presentationWindow;

  RunButtonWatcher watcher;

  PdeRuntime runtime;
  boolean externalRuntime;
  String externalPaths;
  File externalCode;

  JMenuItem saveMenuItem;
  JMenuItem saveAsMenuItem;
  JMenuItem beautifyMenuItem;

  JMenu exportMenu;

  // 

  boolean running;
  boolean presenting;
  boolean renaming;

  //PdeBase base;

  //PrintStream leechErr;
  PdeMessageStream messageStream;

  // location for lib/build, contents for which will be emptied
  String tempBuildPath;

  static final String TEMP_CLASS = "Temporary";

  // undo fellers
  JMenuItem undoItem, redoItem;

  protected UndoAction undoAction;
  protected RedoAction redoAction;
  static public UndoManager undo = new UndoManager(); // editor needs this guy

  // 

  PdeHistory history;
  PdeSketchbook sketchbook;
  PdePreferences preferences;
  PdeEditorFind find;

  //static Properties keywords; // keyword -> reference html lookup


  public PdeEditor() {
    // this is needed by just about everything else
    preferences = new PdePreferences();


#ifdef MACOS
      // #@$*(@#$ apple.. always gotta think different
      MRJApplicationUtils.registerAboutHandler(this);
      MRJApplicationUtils.registerPrefsHandler(this);
      MRJApplicationUtils.registerQuitHandler(this);
#endif

    // set the window icon

    try {
      //icon = Toolkit.getDefaultToolkit().getImage("lib/icon.gif");
      icon = PdeBase.getImage("icon.gif", this);
      setIconImage(icon);
    } catch (Exception e) { } // fail silently, no big whup


    // add listener to handle window close box hit event

    addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          handleQuit();
        }
      });
    //frame.addWindowListener(windowListener);
    //this.addWindowListener(windowListener);


    PdeKeywords keywords = new PdeKeywords();
    history = new PdeHistory(this);
    sketchbook = new PdeSketchbook(this);

    JMenuBar menubar = new JMenuBar();
    menubar.add(buildFileMenu());
    menubar.add(buildEditMenu());
    // what platform has their help menu way on the right?
    //if ((PdeBase.platform == PdeBase.WINDOWS) || 
    //menubar.add(Box.createHorizontalGlue());
    menubar.add(buildHelpMenu());

    setJMenuBar(menubar);

    Container pain = getContentPane();
    pain.setLayout(new BorderLayout());

    /*
    Panel leftPanel = new Panel();
    leftPanel.setLayout(new BorderLayout());

    // set bgcolor of buttons here, b/c also used for empty component
    buttons = new PdeEditorButtons(this);
    Color buttonBgColor = PdePreferences.getColor("editor.buttons.bgcolor");
    buttons.setBackground(buttonBgColor);
    leftPanel.add("North", buttons);
    Label dummy = new Label();
    dummy.setBackground(buttonBgColor);
    leftPanel.add("Center", dummy);

    pain.add("West", leftPanel);
    */
    pain.add("West", new PdeEditorButtons(this));

    JPanel rightPanel = new JPanel();
    rightPanel.setLayout(new BorderLayout());

    header = new PdeEditorHeader(this);
    rightPanel.add(header, BorderLayout.NORTH);

    textarea = new JEditTextArea();
    textarea.setRightClickPopup(new TextAreaPopup());
    textarea.setTokenMarker(new PdeKeywords());

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
    // (that was wishful thinking, they still are, until we switch to jedit)
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
#ifdef JDK14
      ((Frame)presentationWindow).setUndecorated(true);
#endif
      presentationWindow.setBounds(0, 0, screen.width, screen.height);
    }

    Label label = new Label("stop");
    label.addMouseListener(new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
          //System.out.println("got stop");
          //doStop();
          setVisible(true);
          doClose();

          //#ifdef JDK13
          // move editor to front in case it was hidden
          //PdeBase.frame.setState(Frame.NORMAL);
          //base.setState(Frame.NORMAL);
          //#endif
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

    // can this happen here?
    restorePreferences();
  }


  // hack for #@#)$(* macosx
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

    String sketchName = PdePreferences.get("last.sketch.name");
    String sketchDir = PdePreferences.get("last.sketch.path");

    if (sketchName != null) {
      if (new File(sketchDir + File.separator + sketchName).exists()) {
        skOpen(sketchDir, sketchName);

      } else {
        skNew();
      }
    }


    // location for the console/editor area divider

    int location = PdePreferences.getInteger("last.divider.location");
    splitPane.setDividerLocation(location);


    // read the preferences that are settable in the preferences window

    applyPreferences();
  }


  /**
   * Apply changes to preferences that come from changes 
   * by the user in the preferences window.
   */
  public void applyPreferences() {
    // apply the setting for 'use external editor' 

    boolean external = PdePreferences.getBoolean("editor.external");

    listener.setExternalEditor(external);
    textarea.setEditable(!external);
    saveMenuItem.setEnabled(!external);
    saveAsMenuItem.setEnabled(!external);
    beautifyMenuItem.setEnabled(!external);

    TextAreaPainter painter = textarea.getPainter();
    if (external) {
      // disable line highlight and turn off the caret when disabling
      Color color = PdePreferences.getColor("editor.program.bgcolor.external");
      painter.setBackground(color);
      painter.lineHighlight = false;
      textarea.setCaretVisible(false);

    } else {
      Color color = PdePreferences.getColor("editor.program.bgcolor");
      painter.setBackground(color);
      painter.lineHighlight = 
        PdePreferences.getBoolean("editor.program.linehighlight");
      textarea.setCaretVisible(true);
    }


    // in case library option has been enabled or disabled

    buildExportMenu();


    // in case moved to a new location

    //rebuildSketchbookMenu(sketchbookMenu);
    sketchbook.rebuildMenu();
  }


  /**
   * Store preferences about the editor's current state.
   * Called when the application is quitting.
   */
  public void storePreferences() {
    // window location information
    Rectangle bounds = getBounds();
    PdePreferences.setInteger("last.window.x", bounds.x);
    PdePreferences.setInteger("last.window.y", bounds.y);
    PdePreferences.setInteger("last.window.width", bounds.width);
    PdePreferences.setInteger("last.window.height", bounds.height);

    // last sketch that was in use
    PdePreferences.set("last.sketch.name", sketchName);
    PdePreferences.set("last.sketch.path", sketchDir.getAbsolutePath());

    // location for the console/editor area divider
    int location = splitPane.getDividerLocation();    
    PdePreferences.setInteger("last.divider.location", location);
  }


  // ...................................................................


  protected JMenu buildFileMenu() {
    JMenuItem item;
    JMenu menu = new JMenu("File");

    item = newMenuItem("New", 'N');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          skNew();
        }
      });
    menu.add(item);

    //sketchbookMenu = new JMenu("Open");
    //menu.add(sketchbookMenu);
    menu.add(sketchbook.rebuildMenu());

    saveMenuItem = newMenuItem("Save", 'S');
    saveMenuItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          doSave();
        }
      });
    menu.add(saveMenuItem);

    saveAsMenuItem = newMenuItem("Save as...", 'S', true);
    saveAsMenuItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          skSaveAs(false);
        }
      });
    menu.add(saveAsMenuItem);

    item = new JMenuItem("Rename...");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          skSaveAs(true);
        }
      });
    menu.add(item);

    //menu.addSeparator();

    exportMenu = buildExportMenu();
    menu.add(exportMenu);

    menu.addSeparator();

    item = newMenuItem("Page Setup", 'P', true);
    item.setEnabled(false);
    menu.add(item);

    item = newMenuItem("Print", 'P');
    item.setEnabled(false);
    menu.add(item);

    menu.addSeparator();

    // macosx already has its own preferences and quit menu
    if (PdeBase.platform != PdeBase.MACOSX) {
      item = new JMenuItem("Preferences");
      item.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            handlePrefs();
          } 
        });
      menu.add(item);

      menu.addSeparator();

      item = newMenuItem("Quit", 'Q');
      item.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            handleQuit();
          } 
        });
      menu.add(item);
    }
    //menu.addActionListener(this);
    //menubar.add(menu);
    return menu;
  }


  protected JMenu buildExportMenu() {
    if (exportMenu == null) {
      exportMenu = new JMenu("Export");
    } else {
      exportMenu.removeAll();
    }
    JMenuItem item;

    item = newMenuItem("Applet", 'E');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          skExport();
        }
      });
    exportMenu.add(item);

    item = newMenuItem("Application", 'E', true);
    item.setEnabled(false);
    exportMenu.add(item);

    if (PdePreferences.getBoolean("export.library")) {
      item = new JMenuItem("Library");
      item.setEnabled(false);
      exportMenu.add(item);
    }
    return exportMenu;
  }


  protected JMenu buildSketchMenu() {
    JMenuItem item;
    JMenu menu = new JMenu("Sketch");

    item = newMenuItem("Run", 'R');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          doRun(false);
        }
      });
    menu.add(item);

    item = newMenuItem("Present", 'R', true);
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          doRun(true);
        }
      });
    menu.add(item);

    menu.add(newMenuItem("Stop", 'T'));
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleStop();
        }
      });
    menu.addSeparator();

    item = new JMenuItem("Add file...");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          addFile();
        }
      });
    menu.add(item);

    item = new JMenuItem("Create font...");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          new PdeFontBuilder(new File(sketchDir, "data"));
        }
      });
    menu.add(item);

    if ((PdeBase.platform == PdeBase.WINDOWS) || 
        (PdeBase.platform == PdeBase.MACOSX)) {
      // no way to do an 'open in file browser' on other platforms
      // since there isn't any sort of standard
      item = new JMenuItem("Show sketch folder");
      item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          PdeBase.openFolder(sketchDir);
        }
      });
      menu.add(item);
    }

    history.attachMenu(menu);

    //menu.addActionListener(this);
    //menubar.add(menu);  // add the sketch menu
    return menu;
  }


  protected JMenu buildHelpMenu() {
    JMenu menu = new JMenu("Help");
    JMenuItem item;

    item = new JMenuItem("Help");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          PdeBase.openURL(System.getProperty("user.dir") + 
                          File.separator + "reference" + 
                          File.separator + "environment" +
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

    item = new JMenuItem("Proce55ing.net", '5');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          PdeBase.openURL("http://Proce55ing.net/");
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
    }

    //menu.addActionListener(this);
    //menubar.setHelpMenu(menu);
    return menu;
  }


  public JMenu buildEditMenu() {
    JMenu menu = new JMenu("Edit");
    JMenuItem item; 

    undoItem = newMenuItem("Undo", 'Z');
    undoItem.addActionListener(undoAction = new UndoAction());
    menu.add(undoItem);

    redoItem = newMenuItem("Redo", 'Y');
    redoItem.addActionListener(redoAction = new RedoAction());
    menu.add(redoItem);

    menu.addSeparator();

    // "cut" and "copy" should really only be enabled 
    // if some text is currently selected
    item = newMenuItem("Cut", 'X');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          textarea.cut();
        }
      });
    menu.add(item);

    item = newMenuItem("Copy", 'C');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          textarea.copy();
        }
      });
    menu.add(item);

    item = newMenuItem("Paste", 'V');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          textarea.paste();
        }
      });
    menu.add(item);

    item = newMenuItem("Select All", 'A');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          textarea.selectAll();
        }
      });
    menu.add(item);

    beautifyMenuItem = newMenuItem("Beautify", 'B');
    beautifyMenuItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          doBeautify();
        }
      });
    menu.add(beautifyMenuItem);

    menu.addSeparator();

    item = newMenuItem("Find...", 'F');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          //find();
          if (find == null) { 
            find = new PdeEditorFind(PdeEditor.this);
          } else {
            find.show();
          }
        }
      });
    menu.add(item);

    item = newMenuItem("Find Next", 'G');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          //findNext();
          if (find != null) find.find();
        }
      });
    menu.add(item);

    item = newMenuItem("Find in Reference", 'F', true);
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

    return menu;
  }


  // antidote for overthought swing api mess for setting accelerators

  static public JMenuItem newMenuItem(String title, char what) {
    return newMenuItem(title, what, false);
  }

  static public JMenuItem newMenuItem(String title, char what, boolean shift) {
    JMenuItem menuItem = new JMenuItem(title);
    menuItem.setAccelerator(KeyStroke.getKeyStroke(what, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | (shift ? ActionEvent.SHIFT_MASK : 0)));
    return menuItem;
  }


  // This one listens for edits that can be undone.
  protected class PdeUndoableEditListener implements UndoableEditListener {
    public void undoableEditHappened(UndoableEditEvent e) {
      //Remember the edit and update the menus.
      undo.addEdit(e.getEdit());
      undoAction.updateUndoState();
      redoAction.updateRedoState();
      //System.out.println("setting sketch to modified");
      //if (!editor.sketchModified) editor.setSketchModified(true);
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
    // make sure this blocks until finished
    preferences.showFrame();

    // may need to rebuild sketch and other menus
    applyPreferences();

    // next have editor do its thing
    //editor.appyPreferences();
  }


  /** 
   * Quit, but first ask user if it's ok. Also store preferences
   * to disk just in case they want to quit. Final exit() happens 
   * in PdeEditor since it has the callback from PdeEditorStatus.
   */
  public void handleQuit() {
    // check to see if the person actually wants to quit
    //editor.doQuit();
    doQuit();
  }


  // ...................................................................


  protected void changeText(String what, boolean emptyUndo) {
    textarea.setText(what);

    // TODO need to wipe out the undo state here
    if (emptyUndo) undo.discardAllEdits();

    textarea.select(0, 0);    // move to the beginning of the document
    textarea.requestFocus();  // get the caret blinking
  }


  // in an advanced program, the returned classname could be different,
  // which is why the className is set based on the return value.
  // @param exporting if set, then code is cleaner, 
  //                  but line numbers won't line up properly.
  //                  also modifies which imports (1.1 only) are included.
  // @return null if compilation failed, className if not
  //
  protected String build(String program, String className,
                         String buildPath, boolean exporting) 
    throws PdeException, Exception {

    // true if this should extend BApplet instead of BAppletGL
    //boolean extendsNormal = base.normalItem.getState();

    externalRuntime = false;
    externalPaths = null;

    externalCode = new File(sketchDir, "code");
    if (externalCode.exists()) {
      externalRuntime = true;
      externalPaths = PdeCompiler.includeFolder(externalCode);

    } else {
      externalCode = null;
    }

    // add the includes from the external code dir
    //
    String imports[] = null;
    if (externalCode != null) {
      //String includePaths = PdeCompiler.includeFolder(externalCode);
      //imports = PdeCompiler.magicImports(includePaths);
      imports = PdeCompiler.magicImports(externalPaths);
      /*
      StringBuffer buffer = new StringBuffer();
      for (int i = 0; i < imports.length; i++) {
        buffer.append("import ");
        buffer.append(imports[i]);
        buffer.append(".*; ");
      }
      */
      //buffer.append(program);
      //program = buffer.toString();
    }
    //System.out.println("imports is " + imports);

    PdePreprocessor preprocessor = null;
    preprocessor = new PdePreprocessor(program, buildPath);
    try {
      className = 
        preprocessor.writeJava(className, imports, false);

    } catch (antlr.RecognitionException re) {
      // this even returns a column
      throw new PdeException(re.getMessage(), 
                             re.getLine() - 1, re.getColumn());

    } catch (antlr.TokenStreamRecognitionException tsre) {
      // while this seems to store line and column internally,
      // there doesn't seem to be a method to grab it.. 
      // so instead it's done using a regexp

      PatternMatcher matcher = new Perl5Matcher();
      PatternCompiler compiler = new Perl5Compiler();
      // line 3:1: unexpected char: 0xA0
      String mess = "^line (\\d+):(\\d+):\\s";
      Pattern pattern = compiler.compile(mess);

      PatternMatcherInput input = 
        new PatternMatcherInput(tsre.toString());
      if (matcher.contains(input, pattern)) {
        MatchResult result = matcher.getMatch();

        int line = Integer.parseInt(result.group(1).toString());
        int column = Integer.parseInt(result.group(2).toString());
        throw new PdeException(tsre.getMessage(), line-1, column);

      } else {
        throw new PdeException(tsre.toString());
      }

    } catch (PdeException pe) {
      throw pe;

    } catch (Exception ex) {
      System.err.println("Uncaught exception type:" + ex.getClass());
      ex.printStackTrace();
      throw new PdeException(ex.toString());
    }

    if (PdePreprocessor.programType == PdePreprocessor.ADVANCED) {
      externalRuntime = true; // we in advanced mode now, boy
    }

    // compile the program
    //
    PdeCompiler compiler = 
      new PdeCompiler(buildPath, className, externalCode, this);
    // macos9 now officially broken.. see PdeCompilerJavac
    //PdeCompiler compiler = 
    //  ((PdeBase.platform == PdeBase.MACOS9) ? 
    //   new PdeCompilerJavac(buildPath, className, this) :
    //   new PdeCompiler(buildPath, className, this));

    // run the compiler, and funnel errors to the leechErr
    // which is a wrapped around 
    // (this will catch and parse errors during compilation
    // the messageStream will call message() for 'compiler')
    messageStream = new PdeMessageStream(compiler);
    //PrintStream leechErr = new PrintStream(messageStream);
    //boolean result = compiler.compileJava(leechErr);
    //return compiler.compileJava(leechErr);
    boolean success = 
      compiler.compileJava(new PrintStream(messageStream));

    return success ? className : null;
  }


  /**
   * Preprocess, Compile, and Run the current code.
   *
   * There are three main parts to this process:
   *
   *   (0. if not java, then use another 'engine'.. i.e. python)
   *
   *    1. do the p5 language preprocessing
   *       this creates a working .java file in a specific location
   *       better yet, just takes a chunk of java code and returns a 
   *       new/better string editor can take care of saving this to a 
   *       file location
   *
   *    2. compile the code from that location
   *       catching errors along the way
   *       currently done with kjc, but would be nice to use jikes
   *       placing it in a ready classpath, or .. ?
   *
   *    3. run the code 
   *       needs to communicate location for window 
   *       and maybe setup presentation space as well
   *       currently done internally
   *       would be nice to use external (at least on non-os9)
   *
   *    X. afterwards, some of these steps need a cleanup function
   */
  public void doRun(boolean present) {
    //System.out.println(System.getProperty("java.class.path"));

    //doStop();
    doClose();
    running = true;
    //System.out.println("RUNNING");
    buttons.run();

    for (int i = 0; i < 10; i++) System.out.println();

    if (PdePreferences.getBoolean("editor.external")) {
      // history gets screwed by the open..
      String historySaved = history.lastRecorded;
      handleOpen(sketchName, sketchFile, sketchDir);
      history.lastRecorded = historySaved;
    }

    presenting = present;

    try {
      if (presenting) {
        presentationWindow.show();
        presentationWindow.toFront();
        //doRun(true);
      }

      String program = textarea.getText();
      //makeHistory(program, RUN);
      history.record(program, PdeHistory.RUN);

      //if (PdeBase.platform == PdeBase.MACOSX) {
      //String pkg = "Proce55ing.app/Contents/Resources/Java/";
      //buildPath = pkg + "build";
      //}
      //buildPath = "lib" + File.separator + "build";
      tempBuildPath = "lib" + File.separator + "build";

      File buildDir = new File(tempBuildPath);
      if (!buildDir.exists()) {
        buildDir.mkdirs();
      }

      String dataPath = sketchFile.getParent() + File.separator + "data";

      //if (dataPath != null) {
      File dataDir = new File(dataPath);
      if (dataDir.exists()) {
        PdeBase.copyDir(dataDir, buildDir);
      }
      //}
      int numero1 = (int) (Math.random() * 10000);
      int numero2 = (int) (Math.random() * 10000);
      String className = TEMP_CLASS + "_" + numero1 + "_" + numero2;

      // handle building the code
      className = build(program, className, tempBuildPath, false);

      // if the compilation worked, run the applet
      if (className != null) {

        if (externalPaths == null) {
          externalPaths = 
            PdeCompiler.calcClassPath(null) + File.pathSeparator + 
            tempBuildPath;
        } else {
          externalPaths = 
            tempBuildPath + File.pathSeparator +
            PdeCompiler.calcClassPath(null) + File.pathSeparator +
            externalPaths;
        }

        // get a useful folder name for the 'code' folder
        // so that it can be included in the java.library.path
        String codeFolderPath = "";
        if (externalCode != null) {
          codeFolderPath = externalCode.getCanonicalPath();
        }

        // create a runtime object
        runtime = new PdeRuntime(this, className,
                                 externalRuntime, 
                                 codeFolderPath, externalPaths);

        // if programType is ADVANCED
        //   or the code/ folder is not empty -> or just exists (simpler)
        // then set boolean for external to true
        // include path to build in front, then path for code folder
        //   when passing the classpath through
        //   actually, build will already be in there, just prepend code

        // use the runtime object to consume the errors now
        //messageStream.setMessageConsumer(runtime);
        // no need to bother recycling the old guy
        PdeMessageStream messageStream = new PdeMessageStream(runtime);

        // start the applet
        runtime.start(presenting ? presentLocation : appletLocation,
                         new PrintStream(messageStream));
                         //leechErr);

        // spawn a thread to update PDE GUI state
        watcher = new RunButtonWatcher();

      } else {
        // [dmose] throw an exception here?
        // [fry] iirc the exception will have already been thrown
        cleanTempFiles(); //tempBuildPath);
      }
    } catch (PdeException e) { 
      // if we made it to the runtime stage, unwind that thread
      if (runtime != null) runtime.stop();
      cleanTempFiles(); //tempBuildPath);

      // printing the stack trace may be overkill since it happens
      // even on a simple parse error
      //e.printStackTrace();

      error(e);

    } catch (Exception e) {  // something more general happened
      e.printStackTrace();

      // if we made it to the runtime stage, unwind that thread
      if (runtime != null) runtime.stop();

      cleanTempFiles(); //tempBuildPath);
    }        

    //engine = null;
    //System.out.println("out of doRun()");
    // required so that key events go to the panel and <key> works
    //graphics.requestFocus();  // removed for pde    
  }


  class RunButtonWatcher implements Runnable {
    Thread thread;

    public RunButtonWatcher() {
      thread = new Thread(this);
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


  public void doStop() {
    if (runtime != null) runtime.stop();
    if (watcher != null) watcher.stop();
    //System.out.println("stop2");
    message(EMPTY);
    //System.out.println("stop3");
    buttons.clear();
    //System.out.println("stop4");
    running = false;
    //System.out.println("NOT RUNNING");
    //System.out.println("stop5");
  }


  // this is the former 'kill' function
  // may just roll this in with the other code
  // -> keep this around for closing the external window
  public void doClose() {
    //System.out.println("doclose1");
    if (presenting) {
      presentationWindow.hide();
      //if ((presentationWindow == null) || 
      //(!presentationWindow.isVisible())) {

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
    //System.out.println("doclose2");

    if (running) {
      //System.out.println("was running, will call doStop()");
      doStop();
    }

    //System.out.println("doclose3");
    try {
      if (runtime != null) {
        runtime.close();  // kills the window
        runtime = null; // will this help?
      }
    } catch (Exception e) { }
    //System.out.println("doclose4");
    //buttons.clear();  // done by doStop

    //if (buildPath != null) {
    cleanTempFiles(); //buildPath);
    //}

    // toxi_030903: focus the PDE again after quitting presentation mode
    toFront();
  }


  public void setSketchModified(boolean what) {
    header.sketchModified = what;
    //header.update();
    header.repaint();
    sketchModified = what;
  }


  // check to see if there have been changes
  // if so, prompt user whether or not to save first
  // if the user cancels, return false to abort parent operation
  protected void checkModified(int checking) {
    checkModified(checking, null, null);
  }

  protected void checkModified(int checking, String path, String name) {
    this.checking = checking;
    openingPath = path;
    openingName = name;

    if (sketchModified) {
      String prompt = "Save changes to " + sketchName + "?  ";

      if (checking == DO_QUIT) {

        int result = 0;

        //if (PdeBase.platform == PdeBase.MACOSX) {

        // macosx java kills the app even though cancel might get hit
        // so the cancel button is (temporarily) left off
        // this may be treated differently in macosx java 1.4, 
        // but 1.4 isn't currently stable enough to use.

        // turns out windows has the same problem (sometimes)
        // disable cancel for now until a fix can be found.

        Object[] options = { "Yes", "No" };
        result = JOptionPane.showOptionDialog(this,
                                              prompt,
                                              "Quit",
                                              JOptionPane.YES_NO_OPTION,
                                              JOptionPane.QUESTION_MESSAGE,
                                              null,
                                              options, 
                                              options[0]);  // default to save

          /*
        } else {
          Object[] options = { "Yes", "No", "Cancel" };
          result = JOptionPane.showOptionDialog(this,
                                                prompt,
                                                "Quit",
                                                JOptionPane.YES_NO_CANCEL_OPTION,
                                                JOptionPane.QUESTION_MESSAGE,
                                                null,
                                                options, 
                                                options[2]);
          */

        if (result == JOptionPane.YES_OPTION) {
          //System.out.println("yes");
          //System.out.println("saving");
          doSave();
          //System.out.println("done saving");
          checkModified2();

        } else if (result == JOptionPane.NO_OPTION) {
          //System.out.println("no");
          checkModified2();  // though this may just quit

        } else if (result == JOptionPane.CANCEL_OPTION) {
          //System.out.println("cancel");
          // does nothing
        }

      } else {  // not quitting
        status.prompt(prompt);
      }

    } else {
      checkModified2();
    }
    //System.out.println("exiting checkmodified");
  }

  public void checkModified2() {
    //System.out.println("checkmodified2");
    switch (checking) {
    case SK_NEW: skNew2(); break;
    case SK_OPEN: skOpen2(openingPath, openingName); break;
    case DO_OPEN: doOpen2(); break;
    case DO_QUIT: doQuit2(); break;
    }
    checking = 0;
  }


  // local vars prevent sketchName from being set
  public void skNew() {
    doStop();
    checkModified(SK_NEW);
  }

  protected void skNew2() {
    try {
      System.out.println("skNew2()");
      // does all the plumbing to create a new project
      // then calls handleOpen to load it up

      File sketchDir = null;
      String sketchName = null;

      if (PdePreferences.getBoolean("sketchbook.prompt")) {
        FileDialog fd = new FileDialog(new Frame(), 
                                       "Save new sketch as:", 
                                       FileDialog.SAVE);
        fd.setDirectory(PdePreferences.get("sketchbook.path"));
        fd.show();

        String sketchParentDir = fd.getDirectory();
        sketchName = fd.getFile();
        if (sketchName == null) return;

        sketchDir = new File(sketchParentDir, sketchName);

      } else {
        System.out.println("this is the thing");
        String sketchParentDir = PdePreferences.get("sketchbook.path");

        int index = 0;
        SimpleDateFormat formatter = new SimpleDateFormat("yyMMdd");
        String purty = formatter.format(new Date());
        do {
          sketchName = "sketch_" + purty + ((char) ('a' + index));
          sketchDir = new File(sketchParentDir, sketchName);
          index++;
        } while (sketchDir.exists());
      }

      // mkdir for new project name
      sketchDir.mkdirs();

      //new File(sketchDir, "data").mkdirs();

      // make empty pde file
      File sketchFile = new File(sketchDir, sketchName + ".pde");
      new FileOutputStream(sketchFile);

#ifdef MACOS
      // thank you apple, for changing this
      //com.apple.eio.setFileTypeAndCreator(String filename, int, int);

      // jdk13 on osx, or jdk11
      // though apparently still available for 1.4
      if ((PdeBase.platform == PdeBase.MACOS9) ||
          (PdeBase.platform == PdeBase.MACOSX)) {
        MRJFileUtils.setFileTypeAndCreator(sketchFile,
                                           MRJOSType.kTypeTEXT,
                                           new MRJOSType("Pde1"));
      }
#endif

      // make 'data' 'applet' dirs inside that
      // actually, don't, that way can avoid too much extra mess

      // rebuild the menu here
      //base.rebuildSketchbookMenu();
      sketchbook.rebuildMenu();

      // now open it up
      //skOpen(sketchFile, sketchDir);
      handleOpen(sketchName, sketchFile, sketchDir);

    } catch (IOException e) {
      // NEED TO DO SOME ERROR REPORTING HERE ***
      e.printStackTrace();
    }
  }


  public void skOpen(String path, String name) {
    doStop();
    checkModified(SK_OPEN, path, name);
  }

  protected void skOpen2(String path, String name) {
    File osketchFile = new File(path, name + ".pde");
    File osketchDir = new File(path);
    handleOpen(name, osketchFile, osketchDir);
  }


  public void doOpen() {
    checkModified(DO_OPEN);
  }

  protected void doOpen2() {
    // at least set the default dir here to the sketchbook folder

    FileDialog fd = new FileDialog(new Frame(), 
                                   "Open a PDE program...", 
                                   FileDialog.LOAD);
    if (sketchFile != null) {
      fd.setDirectory(sketchFile.getPath());
    }
    fd.show();

    String directory = fd.getDirectory();
    String filename = fd.getFile();
    if (filename == null) {
      buttons.clear();
      return; // user cancelled
    }

    handleOpen(filename, new File(directory, filename), null);
  }


  protected void handleOpen(String isketchName, 
                            File isketchFile, File isketchDir) {
    if (!isketchFile.exists()) {
      status.error("no file named " + isketchName);
      return;
    }

    try {
      String program = null;

      if (isketchFile.length() != 0) {
        FileInputStream input = new FileInputStream(isketchFile);
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        StringBuffer buffer = new StringBuffer();
        String line = null;
        while ((line = reader.readLine()) != null) {
          buffer.append(line);
          buffer.append('\n');
        }
        program = buffer.toString();
        changeText(program, true);

      } else {
        changeText("", true);
      }

      sketchName = isketchName;
      sketchFile = isketchFile;
      sketchDir = isketchDir;
      setSketchModified(false);

      //historyFile = new File(sketchFile.getParent(), "history.gz");
      history.setPath(sketchFile.getParent());
      //base.rebuildHistoryMenu(historyFile.getPath());
      history.rebuildMenu();
      //historyLast = program;
      history.lastRecorded = program;

      header.reset();

      presentLocation = null;
      appletLocation = null;

    } catch (FileNotFoundException e1) {
      e1.printStackTrace();

    } catch (IOException e2) {
      e2.printStackTrace();
    }
    buttons.clear();
  }


  public void doSave() {
    // true if lastfile not set, otherwise false, meaning no prompt
    //handleSave(lastFile == null);
    // actually, this will always be false...
    handleSave(sketchName == null);
  }

  public void doSaveAs() {
    handleSave(true);
  }

  protected void handleSave(boolean promptUser) {
    message("Saving file...");
    String s = textarea.getText();

    String directory = sketchFile.getParent(); //lastDirectory;
    String filename = sketchFile.getName(); //lastFile;

    if (promptUser) {
      FileDialog fd = new FileDialog(new Frame(), 
                                     "Save PDE program as...", 
                                     FileDialog.SAVE);
      fd.setDirectory(directory);
      fd.setFile(filename);
      fd.show();

      directory = fd.getDirectory();
      filename = fd.getFile();
      if (filename == null) {
        message(EMPTY);
        buttons.clear();
        return; // user cancelled
      }
    }
    //makeHistory(s, SAVE);
    history.record(s, PdeHistory.SAVE);
    File file = new File(directory, filename);

    try {
      //System.out.println("handleSave: results of getText");
      //System.out.print(s);

      BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(s.getBytes())));

      PrintWriter writer = 
        new PrintWriter(new BufferedWriter(new FileWriter(file)));

      String line = null;
      while ((line = reader.readLine()) != null) {
        //System.out.println("w '" + line + "'");
        writer.println(line);
      }
      writer.flush();
      writer.close();

      sketchFile = file;
      setSketchModified(false);
      message("Done saving " + filename + ".");

    } catch (IOException e) {
      e.printStackTrace();
      //message("Did not write file.");
      message("Could not write " + filename + ".");
    }
    buttons.clear();
  }


  public void skSaveAs(boolean rename) {
    doStop();

    this.renaming = rename;
    if (rename) {
      status.edit("Rename sketch to...", sketchName);
    } else {
      status.edit("Save sketch as...", sketchName);
    }
  }

  public void skSaveAs2(String newSketchName) {
    if (newSketchName.equals(sketchName)) {
      // nothing changes
      return;
    }

    File newSketchDir = new File(sketchDir.getParent() +
                                 File.separator + newSketchName);
    File newSketchFile = new File(newSketchDir, newSketchName + ".pde");

    //doSave(); // save changes before renaming.. risky but oh well
    String textareaContents = textarea.getText();
    int textareaPosition = textarea.getCaretPosition();

    // if same name, but different case, just use renameTo
    if (newSketchName.toLowerCase().
        equals(sketchName.toLowerCase())) {
      //System.out.println("using renameTo");

      boolean problem = (sketchDir.renameTo(newSketchDir) || 
                         sketchFile.renameTo(newSketchFile));
      if (problem) {
        status.error("Error while trying to re-save the sketch.");
      }

    } else {
      // make new dir
      newSketchDir.mkdirs();
      // copy the sketch file itself with new name
      PdeBase.copyFile(sketchFile, newSketchFile);

      // copy everything from the old dir to the new one
      PdeBase.copyDir(sketchDir, newSketchDir);

      // remove the old sketch file from the new dir
      new File(newSketchDir, sketchName + ".pde").delete();

      // remove the old dir (!)
      if (renaming) {
        // in case java is holding on to any files we want to delete
        System.gc();
        PdeBase.removeDir(sketchDir);
      }

      // (important!) has to be done before opening, 
      // otherwise the new dir is set to sketchDir.. 
      // remove .jar, .class, and .java files from the applet dir
      File appletDir = new File(newSketchDir, "applet");
      File oldjar = new File(appletDir, sketchName + ".jar");
      if (oldjar.exists()) oldjar.delete();
      File oldjava = new File(appletDir, sketchName + ".java");
      if (oldjava.exists()) oldjava.delete();
      File oldclass = new File(appletDir, sketchName + ".class");
      if (oldclass.exists()) oldclass.delete();
    }

    // get the changes into the sketchbook menu
    //base.rebuildSketchbookMenu();
    sketchbook.rebuildMenu();

    // open the new guy
    handleOpen(newSketchName, newSketchFile, newSketchDir);

    // update with the new junk and save that as the new code
    changeText(textareaContents, true);
    textarea.setCaretPosition(textareaPosition);
    doSave();
  }


  public void skExport() {
    doStop();
    message("Exporting for the web...");
    File appletDir = new File(sketchDir, "applet");
    handleExport(appletDir, sketchName, new File(sketchDir, "data"));
  }

  /*
  public void doExport() {
    message("Exporting for the web...");
    String s = textarea.getText();
    FileDialog fd = new FileDialog(new Frame(), 
                                   "Create applet project named...", 
                                   FileDialog.SAVE);

    String directory = sketchFile.getPath(); //lastDirectory;
    String project = sketchFile.getName(); //lastFile;

    fd.setDirectory(directory);
    fd.setFile(project);
    fd.show();

    directory = fd.getDirectory();
    project = fd.getFile();
    if (project == null) {   // user cancelled
      message(EMPTY);
      buttons.clear();
      return;

    } else if (project.indexOf(' ') != -1) {  // space in filename
      message("Project name cannot have spaces.");
      buttons.clear();
      return;
    }
    handleExport(new File(directory), project, null);
  }
  */

  protected void handleExport(File appletDir, String exportSketchName, 
                              File dataDir) {
    try {
      String program = textarea.getText();

      // create the project directory
      // pass null for datapath because the files shouldn't be 
      // copied to the build dir.. that's only for the temp stuff
      appletDir.mkdirs();

      // build the sketch 
      exportSketchName = 
        build(program, exportSketchName, appletDir.getPath(), true);

      // (already reported) error during export, exit this function
      if (exportSketchName == null) {
        buttons.clear();
        return;
      }

      int wide = BApplet.DEFAULT_WIDTH;
      int high = BApplet.DEFAULT_HEIGHT;

      try {
        PatternMatcher matcher = new Perl5Matcher();
        PatternCompiler compiler = new Perl5Compiler();

        // don't just use this version, since it only grabs the numbers
        //String sizing = "[\\s\\;]size\\s*\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\);";

        // this matches against any uses of the size() function, whether they
        // contain numbers of variables or whatever. this way, no warning is 
        // shown if size() isn't actually used in the applet, which is the case 
        // especially for beginners that are cutting/pasting from the reference.
        String sizing = "[\\s\\;]size\\s*\\(\\s*(\\S+)\\s*,\\s*(\\S+)\\s*\\);";

        Pattern pattern = compiler.compile(sizing);

        // adds a space at the beginning, in case size() is the very first
        // thing in the program (very common), since the regexp needs to check
        // for things in front of it.
        PatternMatcherInput input = new PatternMatcherInput(" " + program);
        if (matcher.contains(input, pattern)) {
          MatchResult result = matcher.getMatch();
          try {
            wide = Integer.parseInt(result.group(1).toString());
            high = Integer.parseInt(result.group(2).toString());
            //System.out.println("width " + wide + " high " + high);

          } catch (NumberFormatException e) {

            // found a reference to size, but it didn't seem to contain numbers
            final String message = 
              "The size of this applet could not automatically be\n" +
              "determined from your code. You'll have to edit the\n" + 
              "HTML file to set the size of the applet.";

            JOptionPane.showMessageDialog(this, message, 
                                          "Could not find applet size",
                                          JOptionPane.WARNING_MESSAGE);
          }
        }  // else no size() command found

      } catch (MalformedPatternException e){
        e.printStackTrace();
        //System.err.println("Bad pattern.");
        //System.err.println(e.getMessage());
      }

      File htmlOutputFile = new File(appletDir, "index.html");
      FileOutputStream fos = new FileOutputStream(htmlOutputFile);
      PrintStream ps = new PrintStream(fos);

      // wide, high, and exportSketchName

      // @@sketch@@, @@width@@, @@height@@, @@archive@@

      InputStream is = PdeBase.getStream("applet.html");
      BufferedReader reader = new BufferedReader(new InputStreamReader(is));

      String line = null;
      while ((line = reader.readLine()) != null) {
        if (line.indexOf("@@") != -1) {
          StringBuffer sb = new StringBuffer(line);
          int index = 0;
          while ((index = sb.indexOf("@@sketch@@")) != -1) {
            sb.replace(index, index + "@@sketch@@".length(), 
                       exportSketchName);
          }
          while ((index = sb.indexOf("@@archive@@")) != -1) {
            sb.replace(index, index + "@@archive@@".length(), 
                       exportSketchName + ".jar");
          }
          while ((index = sb.indexOf("@@width@@")) != -1) {
            sb.replace(index, index + "@@width@@".length(), 
                       String.valueOf(wide));
          }
          while ((index = sb.indexOf("@@height@@")) != -1) {
            sb.replace(index, index + "@@height@@".length(), 
                       String.valueOf(wide));
          }
          line = sb.toString();
        }
        ps.println(line);
      }

      reader.close();

      /*
      ps.println("<html>");
      ps.println("<head>");
      ps.println("<title>" + exportSketchName + " : Built with Processing</title>");
      ps.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">");
      ps.println("<BODY BGCOLOR=\"#666666\" text=\"#FFFFFF\" link=\"#CCCC00\" vlink=\"#CCCC00\" alink=\"#999900\">");
      ps.println("<center>");
      ps.println("  <table width=\"400\" border=\"0\" cellspacing=\"0\" cellpadding=\"10\">");
      ps.println("    <tr>");
      ps.println("      <td>&nbsp;</td>");
      ps.println("    </tr>");
      ps.println("    <tr>");
      ps.println("      <td><applet code=\"" + exportSketchName + "\" archive=\"" + exportSketchName + ".jar\" width=" + wide + " height=" + high + ">");
      ps.println("        </applet></td>");
      ps.println("    </tr>");
      ps.println("    <tr>");
      ps.println("      <td>&nbsp;</td>");
      ps.println("    </tr>");
      ps.println("    <tr>");
      ps.println("      <td><a href=\"" + exportSketchName + ".pde\"><font face=\"Arial, Helvetica, sans-serif\" size=\"2\">Source code</font></a></td>");
      ps.println("    </tr>");
      ps.println("    <tr>");
      ps.println("      <td><font size=\"2\" face=\"Arial, Helvetica, sans-serif\">Built with <a href=\"http://Proce55ing.net\">Processing</a></font></td>");
      ps.println("    </tr>");
      ps.println("  </table>");
      ps.println("</center>");
      ps.println("</body>");
      ps.println("</html>");
      */

      ps.flush();
      ps.close();

#ifdef MACOS
      // this chunk left disabled, because safari doesn't actually
      // set the type/creator of html files it makes

      // however, for macos9, this should be re-enabled
      // (but it's not here since macos9 isn't supported for beta)

      /*
      // thank you apple, for changing this
      //com.apple.eio.setFileTypeAndCreator(String filename, int, int);

      // jdk13 on osx, or jdk11
      // though apparently still available for 1.4
      if ((PdeBase.platform == PdeBase.MACOS9) ||
          (PdeBase.platform == PdeBase.MACOSX)) {
        MRJFileUtils.setFileTypeAndCreator(sketchFile, 
                                           MRJOSType.kTypeTEXT,
                                           new MRJOSType("MSIE"));
      }
      */
#endif

      // create new .jar file
      FileOutputStream zipOutputFile = 
        new FileOutputStream(new File(appletDir, exportSketchName + ".jar"));
        //new FileOutputStream(new File(projectDir, projectName + ".jar"));
      ZipOutputStream zos = new ZipOutputStream(zipOutputFile);
      ZipEntry entry;

      File codeFolder = new File(sketchDir, "code");
      if (codeFolder.exists()) {
        String includes = PdeCompiler.includeFolder(codeFolder);
        PdeCompiler.magicExports(includes, zos);
      }

      // add standard .class files to the jar
      // these are the bagel classes found in export
      // they are a jdk11-only version of bagel
      String exportDir = ("lib" + File.separator + 
                          "export" + File.separator);
      String bagelClasses[] = new File(exportDir).list();

      for (int i = 0; i < bagelClasses.length; i++) {
        if (!bagelClasses[i].endsWith(".class")) continue;
        entry = new ZipEntry(bagelClasses[i]);
        zos.putNextEntry(entry);
        zos.write(PdeBase.grabFile(new File(exportDir + bagelClasses[i])));
        zos.closeEntry();
      }

      // files to include from data directory
      if ((dataDir != null) && (dataDir.exists())) {
        String datafiles[] = dataDir.list();
        for (int i = 0; i < datafiles.length; i++) {
          // don't export hidden files, this handles, . .. .DS_Store
          if (datafiles[i].charAt(0) == '.') continue;
          //if (datafiles[i].equals(".") || datafiles[i].equals("..")) {
          //continue;
          //}
          entry = new ZipEntry(datafiles[i]);
          zos.putNextEntry(entry);
          zos.write(PdeBase.grabFile(new File(dataDir, datafiles[i])));
          zos.closeEntry();
        }
      }

      // add the project's .class to the jar
      // actually, these should grab everything from the build directory
      // since there may be some inner classes
      // (add any .class files from the applet dir, then delete them)
      String classfiles[] = appletDir.list();
      for (int i = 0; i < classfiles.length; i++) {
        if (classfiles[i].endsWith(".class")) {
          entry = new ZipEntry(classfiles[i]);
          zos.putNextEntry(entry);
          zos.write(PdeBase.grabFile(new File(appletDir, classfiles[i])));
          zos.closeEntry();
        }
      }

      // remove the .class files from the applet folder. if they're not 
      // removed, the msjvm will complain about an illegal access error, 
      // since the classes are outside the jar file.
      for (int i = 0; i < classfiles.length; i++) {
        if (classfiles[i].endsWith(".class")) {
          File deadguy = new File(appletDir, classfiles[i]);
          if (!deadguy.delete()) {
            System.err.println(classfiles[i] + 
                               " could not be deleted from the applet folder.");
            System.err.println("You'll need to remove it by hand.");
          }
        }
      }

      // close up the jar file
      zos.flush();
      zos.close();

      // make a copy of the .pde file to post on the web
      FileOutputStream sketchOutput = 
        new FileOutputStream(new File(appletDir, exportSketchName + ".pde"));
      PrintWriter sketchWriter = 
        new PrintWriter(new OutputStreamWriter(sketchOutput));
      sketchWriter.print(program);
      sketchWriter.flush();
      sketchWriter.close();

      message("Done exporting.");
      PdeBase.openFolder(appletDir);

    } catch (Exception e) {
      message("Error during export.");
      e.printStackTrace();
    }
    buttons.clear();
  }


  public void doPrint() {
    /*
    Frame frame = new Frame(); // bullocks
    int screenWidth = getToolkit().getScreenSize().width;
    frame.reshape(screenWidth + 20, 100, screenWidth + 100, 200);
    frame.show();

    Properties props = new Properties();
    PrintJob pj = getToolkit().getPrintJob(frame, "PDE", props);
    if (pj != null) {
      Graphics g = pj.getGraphics();
      // awful way to do printing, but sometimes brute force is
      // just the way. java printing across multiple platforms is
      // outrageously inconsistent.
      int offsetX = 100;
      int offsetY = 100;
      int index = 0;
      for (int y = 0; y < graphics.height; y++) {
        for (int x = 0; x < graphics.width; x++) {
          g.setColor(new Color(graphics.pixels[index++]));
          g.drawLine(offsetX + x, offsetY + y,
                     offsetX + x, offsetY + y);
        }
      }
      g.dispose();
      g = null;
      pj.end();
    }
    frame.dispose();
    buttons.clear();
    */
  }


  public void doQuit() {
    // stop isn't sufficient with external vm & quit
    // instead use doClose() which will kill the external vm
    //doStop();
    doClose();  

    //if (!checkModified()) return;
    checkModified(DO_QUIT);
    //System.out.println("exiting doquit");
  }


  protected void doQuit2() {
    storePreferences();
    preferences.save();

    sketchbook.clean();

    //System.out.println("exiting here");
    System.exit(0);
  }


  /*
  public void find() {
    if (find == null) { 
      find = new PdeEditorFind(this);
    } else {
      find.show();
    }
  }

  public void findNext() {
    if (find != null) find.find();
  }
  */


  public void doBeautify() {
    String prog = textarea.getText();
    //makeHistory(prog, BEAUTIFY);
    history.record(prog, PdeHistory.BEAUTIFY);

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
        for (int i = 0; i < level; i++) {
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
    changeText(buffer.toString(), false);

    // make sure the caret would be past the end of the text
    if (buffer.length() < selectionEnd - 1) {
      selectionEnd = buffer.length() - 1;
    }

    // at least in the neighborhood
    textarea.select(selectionEnd, selectionEnd);

    setSketchModified(true);
    buttons.clear();
  }


  public void addFile() {
    // get a dialog, select a file to add to the sketch
    String prompt = "Select an image or other data file to copy to your sketch";
    FileDialog fd = new FileDialog(new Frame(), prompt, FileDialog.LOAD);
    //if (sketchFile != null) {
    //fd.setDirectory(sketchFile.getPath());
    //}
    fd.show();

    String directory = fd.getDirectory();
    String filename = fd.getFile();
    if (filename == null) return;

    // copy the file into the folder
    // if people don't want it to copy, they can do it by hand
    File sourceFile = new File(directory, filename);

    File destFile = null;

    // if the file appears to be code related, drop it 
    // into the code folder, instead of the data folder
    if (filename.toLowerCase().endsWith(".class") || 
        filename.toLowerCase().endsWith(".jar") || 
        filename.toLowerCase().endsWith(".dll") || 
        filename.toLowerCase().endsWith(".jnilib") || 
        filename.toLowerCase().endsWith(".so")) {
      File codeFolder = new File(sketchDir, "code");
      if (!codeFolder.exists()) codeFolder.mkdirs();
      destFile = new File(codeFolder, filename);

    } else {
      File dataFolder = new File(sketchDir, "data");
      if (!dataFolder.exists()) dataFolder.mkdirs();
      destFile = new File(dataFolder, filename);
    }
    //System.out.println("copying from " + sourceFile);
    //System.out.println("copying to " + destFile);
    PdeBase.copyFile(sourceFile, destFile);
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


  public void error(PdeException e) {   // part of PdeEnvironment
    if (e.line >= 0) highlightLine(e.line); 

    status.error(e.getMessage());
    buttons.clearRun();
  }


  public void finished() {  // part of PdeEnvironment
    running = false;
    buttons.clearRun();
    message("Done.");
  }


  public void message(String msg) {  // part of PdeEnvironment
    status.notice(msg);
  }
  
  
  public void messageClear(String msg) {
    status.unnotice(msg);
  }


  // ...................................................................


  /**
   * Cleanup temp files
   */
  protected void cleanTempFiles() {
    if (tempBuildPath == null) return;

    // if the java runtime is holding onto any files in the build dir, we
    // won't be able to delete them, so we need to force a gc here
    //
    System.gc();

    //File dirObject = new File(buildPath);
    File dirObject = new File(tempBuildPath);

    // note that we can't remove the builddir itself, otherwise
    // the next time we start up, internal runs using PdeRuntime won't
    // work because the build dir won't exist at startup, so the classloader
    // will ignore the fact that that dir is in the CLASSPATH in run.sh
    //
    if (dirObject.exists()) {
      PdeBase.removeDescendants(dirObject);
    }
  }


  /**
   * Returns the edit popup menu.
   */  
  class TextAreaPopup extends JPopupMenu {
    //protected ReferenceKeys referenceItems = new ReferenceKeys();
    String currentDir = System.getProperty("user.dir");
    String referenceFile = null;

    //static JEditTextArea parent;

    JMenuItem cutItem, copyItem;
    JMenuItem referenceItem;


    public TextAreaPopup(/*JEditTextArea parent*/) {
      //this.parent = parent;

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
      
      //this.addSeparator();

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

    /*
    // enables/disables "Reference Lookup" option
    public void setReferenceLookup(String file)
    {
      if (file != null) {
        this.getComponent(5).setEnabled(true);
        referenceFile = file;
      }
      else {
        this.getComponent(5).setEnabled(false);
      }
    }
    */

    /*
    public void showReferenceFile()
    {
      PdeBase.openURL(currentDir + File.separator + 
                      "reference" + File.separator + 
                      referenceFile + ".html");
    }
    */

    // if no text is selected, disable copy and cut menu items
    public void show(Component component, int x, int y)
    {
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

      /*
      //popup.setEnabledCut(selectionIsActive);
      //popup.setEnabledCopy(selectionIsActive);

      String file = null;
      if (selectionIsActive) {
        file = (String) PdeBase.keywords.get(getSelectedText());
      }
      popup.setReferenceLookup(file);
      
      super.show(invoker, x, y);
      */
    }
  }
}


  /**
   * Handle menu selections.
   */
  /*
  public void actionPerformed(ActionEvent event) {
    String command = event.getActionCommand();
    //System.out.println(command);

    if (command.equals("New")) {
      editor.skNew();

    } else if (command.equals("Save")) {
      editor.doSave();

    } else if (command.equals("Save as...")) {
      editor.skSaveAs(false);

    } else if (command.equals("Rename...")) {
      editor.skSaveAs(true);

    } else if (command.equals("Export to Web")) {
      editor.skExport();

    } else if (command.equals("Preferences")) {
      handlePrefs();

    } else if (command.equals("Quit")) {
      handleQuit();

    } else if (command.equals("Run")) {
      editor.doRun(false);

    } else if (command.equals("Present")) {
      editor.doRun(true);

    } else if (command.equals("Stop")) {    
      if (editor.presenting) {
        editor.doClose();
      } else {
        editor.doStop();
      }
    } else if (command.equals("Beautify")) {
      editor.doBeautify();

    } else if (command.equals("Add file...")) {
      editor.addFile();

    } else if (command.equals("Create font...")) {
      new PdeFontBuilder(new File(editor.sketchDir, "data"));

    } else if (command.equals("Show sketch folder")) {
      openFolder(editor.sketchDir);

    } else if (command.equals("Help")) {
      openURL(System.getProperty("user.dir") + 
              File.separator + "reference" + 
              File.separator + "environment" +
              File.separator + "index.html");

    } else if (command.equals("Proce55ing.net")) {
      openURL("http://Proce55ing.net/");

    } else if (command.equals("Reference")) {
      openURL(System.getProperty("user.dir") + File.separator + 
              "reference" + File.separator + "index.html");

    } else if (command.equals("About Processing")) {
      handleAbout();
    }
  }
  */

/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-19 The Processing Foundation
  Copyright (c) 2004-12 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

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

package processing.app.ui;

import processing.app.Base;
import processing.app.Formatter;
import processing.app.Language;
import processing.app.Messages;
import processing.app.Mode;
import processing.app.Platform;
import processing.app.Preferences;
import processing.app.Problem;
import processing.app.RunnerListener;
import processing.app.Sketch;
import processing.app.SketchCode;
import processing.app.SketchException;
import processing.app.Util;
import processing.app.contrib.ContributionManager;
import processing.app.syntax.*;
import processing.core.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Point;
import java.awt.Window;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.print.*;
import java.io.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.basic.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.undo.*;


/**
 * Main editor panel for the Processing Development Environment.
 */
public abstract class Editor extends JFrame implements RunnerListener {
  protected Base base;
  protected EditorState state;
  protected Mode mode;

  static public final int LEFT_GUTTER = Toolkit.zoom(44);
  static public final int RIGHT_GUTTER = Toolkit.zoom(12);
  static public final int GUTTER_MARGIN = Toolkit.zoom(3);

  protected MarkerColumn errorColumn;

  // Otherwise, if the window is resized with the message label
  // set to blank, its preferredSize() will be fuckered
  static protected final String EMPTY =
    "                                                                     " +
    "                                                                     " +
    "                                                                     ";

  /**
   * true if this file has not yet been given a name by the user
   */
//  private boolean untitled;

  private PageFormat pageFormat;
  private PrinterJob printerJob;

  // File and sketch menus for re-inserting items
  private JMenu fileMenu;
//  private JMenuItem saveMenuItem;
//  private JMenuItem saveAsMenuItem;

  private JMenu sketchMenu;

  protected EditorHeader header;
  protected EditorToolbar toolbar;
  protected JEditTextArea textarea;
  protected EditorStatus status;
  protected JSplitPane splitPane;
  protected EditorFooter footer;
  protected EditorConsole console;
  protected ErrorTable errorTable;

  // currently opened program
  protected Sketch sketch;

  // runtime information and window placement
  private Point sketchWindowLocation;

  // undo fellers
  private JMenuItem undoItem, redoItem;
  protected UndoAction undoAction;
  protected RedoAction redoAction;
  protected CutAction cutAction;
  protected CopyAction copyAction;
  protected CopyAsHtmlAction copyAsHtmlAction;
  protected PasteAction pasteAction;
  /** Menu Actions updated on the opening of the edit menu. */
  protected List<UpdatableAction> editMenuUpdatable = new ArrayList<>();

  /** The currently selected tab's undo manager */
  private UndoManager undo;
  // used internally for every edit. Groups hotkey-event text manipulations and
  // groups  multi-character inputs into a single undos.
  private CompoundEdit compoundEdit;
  // timer to decide when to group characters into an undo
  private Timer timer;
  private TimerTask endUndoEvent;
  // true if inserting text, false if removing text
  private boolean isInserting;
  // maintain caret position during undo operations
  private final Stack<Integer> caretUndoStack = new Stack<>();
  private final Stack<Integer> caretRedoStack = new Stack<>();

  private FindReplace find;
  JMenu toolsMenu;
  JMenu modePopup;

  Image backgroundGradient;

  protected List<Problem> problems = Collections.emptyList();


  protected Editor(final Base base, String path, final EditorState state,
                   final Mode mode) throws EditorException {
    super("Processing", state.checkConfig());
    this.base = base;
    this.state = state;
    this.mode = mode;

    // Make sure Base.getActiveEditor() never returns null
    base.checkFirstEditor(this);

    // This is a Processing window. Get rid of that ugly ass coffee cup.
    Toolkit.setIcon(this);

    // add listener to handle window close box hit event
    addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          base.handleClose(Editor.this, false);
        }
      });
    // don't close the window when clicked, the app will take care
    // of that via the handleQuitInternal() methods
    // http://dev.processing.org/bugs/show_bug.cgi?id=440
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

    // When bringing a window to front, let the Base know
    addWindowListener(new WindowAdapter() {

        public void windowActivated(WindowEvent e) {
          base.handleActivated(Editor.this);
          fileMenu.insert(Recent.getMenu(), 2);
          Toolkit.setMenuMnemsInside(fileMenu);

          mode.insertImportMenu(sketchMenu);
          Toolkit.setMenuMnemsInside(sketchMenu);
          mode.insertToolbarRecentMenu();
        }

        public void windowDeactivated(WindowEvent e) {
          // TODO call handleActivated(null)? or do we run the risk of the
          // deactivate call for old window being called after the activate?
          fileMenu.remove(Recent.getMenu());
          mode.removeImportMenu(sketchMenu);
          mode.removeToolbarRecentMenu();
        }
      });

    timer = new Timer();

    buildMenuBar();

    /*
    //backgroundGradient = Toolkit.getLibImage("vertical-gradient.png");
    backgroundGradient = mode.getGradient("editor", 400, 400);
    JPanel contentPain = new JPanel() {
      @Override
      public void paintComponent(Graphics g) {
//        super.paintComponent(g);
        Dimension dim = getSize();
        g.drawImage(backgroundGradient, 0, 0, dim.width, dim.height, this);
//        g.setColor(Color.RED);
//        g.fillRect(0, 0, dim.width, dim.height);
      }
    };
    */
    //contentPain.setBorder(new EmptyBorder(0, 0, 0, 0));
    //System.out.println(contentPain.getBorder());
    JPanel contentPain = new JPanel();

//    JFrame f = new JFrame();
//    f.setContentPane(new JPanel() {
//      @Override
//      public void paintComponent(Graphics g) {
////        super.paintComponent(g);
//        Dimension dim = getSize();
//        g.drawImage(backgroundGradient, 0, 0, dim.width, dim.height, this);
////        g.setColor(Color.RED);
////        g.fillRect(0, 0, dim.width, dim.height);
//      }
//    });
//    f.setResizable(true);
//    f.setVisible(true);

    //Container contentPain = getContentPane();
    setContentPane(contentPain);
    contentPain.setLayout(new BorderLayout());
//    JPanel pain = new JPanel();
//    pain.setOpaque(false);
//    pain.setLayout(new BorderLayout());
//    contentPain.add(pain, BorderLayout.CENTER);
//    contentPain.setBorder(new EmptyBorder(10, 10, 10, 10));

    Box box = Box.createVerticalBox();
    Box upper = Box.createVerticalBox();
//    upper.setOpaque(false);
//    box.setOpaque(false);

    rebuildModePopup();
    toolbar = createToolbar();
    upper.add(toolbar);

    header = createHeader();
    upper.add(header);

    textarea = createTextArea();
    textarea.setRightClickPopup(new TextAreaPopup());
    textarea.setHorizontalOffset(JEditTextArea.leftHandGutter);

    { // Hack: add Numpad Slash as alternative shortcut for Comment/Uncomment
      int modifiers = Toolkit.awtToolkit.getMenuShortcutKeyMask();
      KeyStroke keyStroke =
        KeyStroke.getKeyStroke(KeyEvent.VK_DIVIDE, modifiers);
      final String ACTION_KEY = "COMMENT_UNCOMMENT_ALT";
      textarea.getInputMap().put(keyStroke, ACTION_KEY);
      textarea.getActionMap().put(ACTION_KEY, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          handleCommentUncomment();
        }
      });
    }
    textarea.addCaretListener(new CaretListener() {
      public void caretUpdate(CaretEvent e) {
        updateEditorStatus();
      }
    });

    footer = createFooter();

    // build the central panel with the text area & error marker column
    JPanel editorPanel = new JPanel(new BorderLayout());
    errorColumn =  new MarkerColumn(this, textarea.getMinimumSize().height);
    editorPanel.add(errorColumn, BorderLayout.EAST);
    textarea.setBounds(0, 0, errorColumn.getX() - 1, textarea.getHeight());
    editorPanel.add(textarea);
    upper.add(editorPanel);

    // set colors and fonts for the painter object
    PdeTextArea pta = getPdeTextArea();
    if (pta != null) {
      pta.setMode(mode);
    }

    splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, upper, footer);

    // disable this because it hides the message area (Google Code issue #745)
    splitPane.setOneTouchExpandable(false);
    // repaint child panes while resizing
    splitPane.setContinuousLayout(true);
    // if window increases in size, give all of increase to
    // the textarea in the upper pane
    splitPane.setResizeWeight(1D);
    // remove any ugly borders added by PLAFs (doesn't fix everything)
    splitPane.setBorder(null);
    // remove an ugly border around anything in a SplitPane !$*&!%
    UIManager.getDefaults().put("SplitPane.border", BorderFactory.createEmptyBorder());
    // set the height per our gui design
    splitPane.setDividerSize(EditorStatus.HIGH);

    // override the look of the SplitPane so that it's identical across OSes
    splitPane.setUI(new BasicSplitPaneUI() {
      public BasicSplitPaneDivider createDefaultDivider() {
        status = new EditorStatus(this, Editor.this);
        return status;
      }


      @Override
      public void finishDraggingTo(int location) {
        super.finishDraggingTo(location);
        // JSplitPane issue: if you only make the lower component visible at
        // the last minute, its minmum size is ignored.
        if (location > splitPane.getMaximumDividerLocation()) {
          splitPane.setDividerLocation(splitPane.getMaximumDividerLocation());
        }
      }
    });

    box.add(splitPane);

    contentPain.add(box);

    // end an undo-chunk any time the caret moves unless it's when text is edited
    textarea.addCaretListener(new CaretListener() {
      String lastText = textarea.getText();
      public void caretUpdate(CaretEvent e) {
        String newText = textarea.getText();
        if (lastText.equals(newText) && isDirectEdit() && !textarea.isOverwriteEnabled()) {
          endTextEditHistory();
        }
        lastText = newText;
      }
    });

    textarea.addKeyListener(toolbar);

    contentPain.setTransferHandler(new FileDropHandler());

    // Finish preparing Editor
    pack();

    // Set the window bounds and the divider location before setting it visible
    state.apply(this);

    // Set the minimum size for the editor window
    int minWidth =
      Toolkit.zoom(Preferences.getInteger("editor.window.width.min"));
    int minHeight =
      Toolkit.zoom(Preferences.getInteger("editor.window.height.min"));
    setMinimumSize(new Dimension(minWidth, minHeight));

    // Bring back the general options for the editor
    applyPreferences();

    // Make textField get the focus whenever frame is activated.
    // http://download.oracle.com/javase/tutorial/uiswing/misc/focus.html
    // May not be necessary, but helps avoid random situations with
    // the editor not being able to request its own focus.
    addWindowFocusListener(new WindowAdapter() {
      public void windowGainedFocus(WindowEvent e) {
        textarea.requestFocusInWindow();
      }
    });

    // TODO: Subclasses can't initialize anything before Doc Open happens since
    //       super() has to be the first line in subclass constructor; we might
    //       want to keep constructor light and call methods later [jv 160318]

    // Open the document that was passed in
    handleOpenInternal(path);

    // Add a window listener to watch for changes to the files in the sketch
    addWindowFocusListener(new ChangeDetector(this));

    // Try to enable fancy fullscreen on OSX
    if (Platform.isMacOS()) {
      try {
        Class util = Class.forName("com.apple.eawt.FullScreenUtilities");
        Class params[] = new Class[]{Window.class, Boolean.TYPE};
        Method method = util.getMethod("setWindowCanFullScreen", params);
        method.invoke(util, this, true);
      } catch (Exception e) {
        Messages.loge("Could not enable OSX fullscreen", e);
      }
    }

  }


  /*
  protected List<ToolContribution> getCoreTools() {
    return coreTools;
  }


  public List<ToolContribution> getToolContribs() {
    return contribTools;
  }


  public void removeToolContrib(ToolContribution tc) {
    contribTools.remove(tc);
  }
  */


  protected JEditTextArea createTextArea() {
    return new JEditTextArea(new PdeTextAreaDefaults(mode),
                             new PdeInputHandler(this));
  }


  public EditorFooter createFooter() {
    EditorFooter ef = new EditorFooter(this);
    console = new EditorConsole(this);
    ef.addPanel(console, Language.text("editor.footer.console"), "/lib/footer/console");
    return ef;
  }


  public void addErrorTable(EditorFooter ef) {
    JScrollPane scrollPane = new JScrollPane();
    errorTable = new ErrorTable(this);
    scrollPane.setBorder(BorderFactory.createEmptyBorder());
    scrollPane.setViewportView(errorTable);
    ef.addPanel(scrollPane, Language.text("editor.footer.errors"), "/lib/footer/error");
  }


  public EditorState getEditorState() {
    return state;
  }


  /**
   * Handles files dragged & dropped from the desktop and into the editor
   * window. Dragging files into the editor window is the same as using
   * "Sketch &rarr; Add File" for each file.
   */
  class FileDropHandler extends TransferHandler {
    public boolean canImport(TransferHandler.TransferSupport support) {
      return !sketch.isReadOnly();
    }

    @SuppressWarnings("unchecked")
    public boolean importData(TransferHandler.TransferSupport support) {
      int successful = 0;

      if (!canImport(support)) {
        return false;
      }

      try {
        Transferable transferable = support.getTransferable();
        DataFlavor uriListFlavor =
          new DataFlavor("text/uri-list;class=java.lang.String");

        if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
          List list = (List)
            transferable.getTransferData(DataFlavor.javaFileListFlavor);
          for (int i = 0; i < list.size(); i++) {
            File file = (File) list.get(i);
            if (sketch.addFile(file)) {
              successful++;
            }
          }
        } else if (transferable.isDataFlavorSupported(uriListFlavor)) {
          // Some platforms (Mac OS X and Linux, when this began) preferred
          // this method of moving files.
          String data = (String)transferable.getTransferData(uriListFlavor);
          String[] pieces = PApplet.splitTokens(data, "\r\n");
          for (int i = 0; i < pieces.length; i++) {
            if (pieces[i].startsWith("#")) continue;

            String path = null;
            if (pieces[i].startsWith("file:///")) {
              path = pieces[i].substring(7);
            } else if (pieces[i].startsWith("file:/")) {
              path = pieces[i].substring(5);
            }
            if (sketch.addFile(new File(path))) {
              successful++;
            }
          }
        }
      } catch (Exception e) {
        Messages.showWarning("Drag & Drop Problem",
                             "An error occurred while trying to add files to the sketch.", e);
        return false;
      }
      statusNotice(Language.pluralize("editor.status.drag_and_drop.files_added", successful));
      return true;
    }
  }


  public Base getBase() {
    return base;
  }


  public Mode getMode() {
    return mode;
  }


  public void repaintHeader() {
    header.repaint();
  }


  public void rebuildHeader() {
    header.rebuild();
  }


  public void rebuildModePopup() {
    modePopup = new JMenu();
    ButtonGroup modeGroup = new ButtonGroup();
    for (final Mode m : base.getModeList()) {
      JRadioButtonMenuItem item = new JRadioButtonMenuItem(m.getTitle());
      item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (!sketch.isModified()) {
            if (!base.changeMode(m)) {
              reselectMode();
              Messages.showWarning(Language.text("warn.cannot_change_mode.title"),
                                   Language.interpolate("warn.cannot_change_mode.body", m));
            }
          } else {
            reselectMode();
            Messages.showWarning("Save",
                                 "Please save the sketch before changing the mode.");
          }
        }
      });
      modePopup.add(item);
      modeGroup.add(item);
      if (mode == m) {
        item.setSelected(true);
      }
    }

    modePopup.addSeparator();
    JMenuItem addLib = new JMenuItem(Language.text("toolbar.add_mode"));
    addLib.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        ContributionManager.openModes();
      }
    });
    modePopup.add(addLib);

    Toolkit.setMenuMnemsInside(modePopup);
  }

  // Re-select the old checkbox, because it was automatically
  // updated by Java, even though the Mode could not be changed.
  // https://github.com/processing/processing/issues/2615
  private void reselectMode() {
    for (Component c : getModePopup().getComponents()) {
      if (c instanceof JRadioButtonMenuItem) {
        if (((JRadioButtonMenuItem)c).getText() == mode.getTitle()) {
          ((JRadioButtonMenuItem)c).setSelected(true);
          break;
        }
      }
    }
  }

  public JPopupMenu getModePopup() {
    return modePopup.getPopupMenu();
  }


//  public JMenu getModeMenu() {
//    return modePopup;
//  }


  public EditorConsole getConsole() {
    return console;
  }



//  public Settings getTheme() {
//    return mode.getTheme();
//  }


  public EditorHeader createHeader() {
    return new EditorHeader(this);
  }


  abstract public EditorToolbar createToolbar();


  public void rebuildToolbar() {
    toolbar.rebuild();
    toolbar.revalidate();  // necessary to handle sub-components
  }


  abstract public Formatter createFormatter();


//  protected void setPlacement(int[] location) {
//    setBounds(location[0], location[1], location[2], location[3]);
//    if (location[4] != 0) {
//      splitPane.setDividerLocation(location[4]);
//    }
//  }
//
//
//  protected int[] getPlacement() {
//    int[] location = new int[5];
//
//    // Get the dimensions of the Frame
//    Rectangle bounds = getBounds();
//    location[0] = bounds.x;
//    location[1] = bounds.y;
//    location[2] = bounds.width;
//    location[3] = bounds.height;
//
//    // Get the current placement of the divider
//    location[4] = splitPane.getDividerLocation();
//
//    return location;
//  }


  protected void setDividerLocation(int pos) {
    splitPane.setDividerLocation(pos);
  }


  protected int getDividerLocation() {
    return splitPane.getDividerLocation();
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Read and apply new values from the preferences, either because
   * the app is just starting up, or the user just finished messing
   * with things in the Preferences window.
   */
  protected void applyPreferences() {
    // Update fonts and other items controllable from the prefs
    textarea.getPainter().updateAppearance();
    textarea.repaint();

    console.updateAppearance();

    // All of this code was specific to using an external editor.
    /*
//    // apply the setting for 'use external editor'
//    boolean external = Preferences.getBoolean("editor.external");
//    textarea.setEditable(!external);
//    saveMenuItem.setEnabled(!external);
//    saveAsMenuItem.setEnabled(!external);

    TextAreaPainter painter = textarea.getPainter();
//    if (external) {
//      // disable line highlight and turn off the caret when disabling
//      Color color = mode.getColor("editor.external.bgcolor");
//      painter.setBackground(color);
//      painter.setLineHighlightEnabled(false);
//      textarea.setCaretVisible(false);
//    } else {
    Color color = mode.getColor("editor.bgcolor");
    painter.setBackground(color);
    boolean highlight = Preferences.getBoolean("editor.linehighlight");
    painter.setLineHighlightEnabled(highlight);
    textarea.setCaretVisible(true);
//    }

    // apply changes to the font size for the editor
//    painter.setFont(Preferences.getFont("editor.font"));

    // in case tab expansion stuff has changed
    // removing this, just checking prefs directly instead
//    listener.applyPreferences();

    // in case moved to a new location
    // For 0125, changing to async version (to be implemented later)
    //sketchbook.rebuildMenus();
    // For 0126, moved into Base, which will notify all editors.
    //base.rebuildMenusAsync();
     */
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  protected void buildMenuBar() {
    JMenuBar menubar = new JMenuBar();
    fileMenu = buildFileMenu();
    menubar.add(fileMenu);
    menubar.add(buildEditMenu());
    menubar.add(buildSketchMenu());

    // For 3.0a4 move mode menu to the left of the Tool menu
    JMenu modeMenu = buildModeMenu();
    if (modeMenu != null) {
      menubar.add(modeMenu);
    }

    toolsMenu = new JMenu(Language.text("menu.tools"));
    base.populateToolsMenu(toolsMenu);
    menubar.add(toolsMenu);

    menubar.add(buildHelpMenu());
    Toolkit.setMenuMnemonics(menubar);
    setJMenuBar(menubar);
  }


  abstract public JMenu buildFileMenu();


//  public JMenu buildFileMenu(Editor editor) {
//    return buildFileMenu(editor, null);
//  }
//
//
//  // most of these items are per-mode
//  protected JMenu buildFileMenu(Editor editor, JMenuItem[] exportItems) {


  protected JMenu buildFileMenu(JMenuItem[] exportItems) {
    JMenuItem item;
    JMenu fileMenu = new JMenu(Language.text("menu.file"));

    item = Toolkit.newJMenuItem(Language.text("menu.file.new"), 'N');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          base.handleNew();
        }
      });
    fileMenu.add(item);

    item = Toolkit.newJMenuItem(Language.text("menu.file.open"), 'O');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          base.handleOpenPrompt();
        }
      });
    fileMenu.add(item);

//    fileMenu.add(base.getSketchbookMenu());

    item = Toolkit.newJMenuItemShift(Language.text("menu.file.sketchbook"), 'K');
    item.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        mode.showSketchbookFrame();
      }
    });
    fileMenu.add(item);

    item = Toolkit.newJMenuItemShift(Language.text("menu.file.examples"), 'O');
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        mode.showExamplesFrame();
      }
    });
    fileMenu.add(item);

    item = Toolkit.newJMenuItem(Language.text("menu.file.close"), 'W');
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        base.handleClose(Editor.this, false);
      }
    });
    fileMenu.add(item);

    item = Toolkit.newJMenuItem(Language.text("menu.file.save"), 'S');
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        handleSave(false);
      }
    });
//    saveMenuItem = item;
    fileMenu.add(item);

    item = Toolkit.newJMenuItemShift(Language.text("menu.file.save_as"), 'S');
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        handleSaveAs();
      }
    });
//    saveAsMenuItem = item;
    fileMenu.add(item);

    if (exportItems != null) {
      for (JMenuItem ei : exportItems) {
        fileMenu.add(ei);
      }
    }
    fileMenu.addSeparator();

    item = Toolkit.newJMenuItemShift(Language.text("menu.file.page_setup"), 'P');
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        handlePageSetup();
      }
    });
    fileMenu.add(item);

    item = Toolkit.newJMenuItem(Language.text("menu.file.print"), 'P');
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        handlePrint();
      }
    });
    fileMenu.add(item);

    // Mac OS X already has its own preferences and quit menu.
    // That's right! Think different, b*tches!
    if (!Platform.isMacOS()) {
      fileMenu.addSeparator();

      item = Toolkit.newJMenuItem(Language.text("menu.file.preferences"), ',');
      item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          base.handlePrefs();
        }
      });
      fileMenu.add(item);

      fileMenu.addSeparator();

      item = Toolkit.newJMenuItem(Language.text("menu.file.quit"), 'Q');
      item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          base.handleQuit();
        }
      });
      fileMenu.add(item);
    }
    return fileMenu;
  }


//  public void setSaveItem(JMenuItem item) {
//    saveMenuItem = item;
//  }


//  public void setSaveAsItem(JMenuItem item) {
//    saveAsMenuItem = item;
//  }


  protected JMenu buildEditMenu() {
    JMenu menu = new JMenu(Language.text("menu.edit"));
    JMenuItem item;

    undoItem = Toolkit.newJMenuItem(undoAction = new UndoAction(), 'Z');
    menu.add(undoItem);

    redoItem = new JMenuItem(redoAction = new RedoAction());
    redoItem.setAccelerator(Toolkit.getKeyStrokeExt("menu.edit.redo"));
    menu.add(redoItem);

    menu.addSeparator();

    item = Toolkit.newJMenuItem(cutAction = new CutAction(), 'X');
    editMenuUpdatable.add(cutAction);
    menu.add(item);

    item = Toolkit.newJMenuItem(copyAction = new CopyAction(), 'C');
    editMenuUpdatable.add(copyAction);
    menu.add(item);

    item = Toolkit.newJMenuItemShift(copyAsHtmlAction = new CopyAsHtmlAction(), 'C');
    editMenuUpdatable.add(copyAsHtmlAction);
    menu.add(item);

    item = Toolkit.newJMenuItem(pasteAction = new PasteAction(), 'V');
    editMenuUpdatable.add(pasteAction);
    menu.add(item);

    item = Toolkit.newJMenuItem(Language.text("menu.edit.select_all"), 'A');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          textarea.selectAll();
        }
      });
    menu.add(item);

    /*
    menu.addSeparator();

    item = Toolkit.newJMenuItem("Delete Selected Lines", 'D');
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        handleDeleteLines();
      }
    });
    menu.add(item);

    item = new JMenuItem("Move Selected Lines Up");
    item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_UP, Event.ALT_MASK));
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleMoveLines(true);
        }
      });
    menu.add(item);

    item = new JMenuItem("Move Selected Lines Down");
    item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, Event.ALT_MASK));
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleMoveLines(false);
        }
      });
    menu.add(item);
     */

    menu.addSeparator();

    item = Toolkit.newJMenuItem(Language.text("menu.edit.auto_format"), 'T');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleAutoFormat();
        }
    });
    menu.add(item);

    item = Toolkit.newJMenuItemExt("menu.edit.comment_uncomment");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleCommentUncomment();
        }
    });
    menu.add(item);

    item = Toolkit.newJMenuItemExt("menu.edit.increase_indent");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleIndentOutdent(true);
        }
    });
    menu.add(item);

    item = Toolkit.newJMenuItemExt("menu.edit.decrease_indent");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleIndentOutdent(false);
        }
    });
    menu.add(item);

    menu.addSeparator();

    item = Toolkit.newJMenuItem(Language.text("menu.edit.find"), 'F');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (find == null) {
            find = new FindReplace(Editor.this);
          }
          // https://github.com/processing/processing/issues/3457
          String selection = getSelectedText();
          if (selection != null && selection.length() != 0 &&
              !selection.contains("\n")) {
            find.setFindText(selection);
          }
          find.setVisible(true);
        }
      });
    menu.add(item);

    UpdatableAction action;
    item = Toolkit.newJMenuItem(action = new FindNextAction(), 'G');
    editMenuUpdatable.add(action);
    menu.add(item);

    item = Toolkit.newJMenuItemShift(action = new FindPreviousAction(), 'G');
    editMenuUpdatable.add(action);
    menu.add(item);

    item = Toolkit.newJMenuItem(action = new SelectionForFindAction(), 'E');
    editMenuUpdatable.add(action);
    menu.add(item);

    // Update copy/cut state on selection/de-selection
    menu.addMenuListener(new MenuListener() {
      // UndoAction and RedoAction do this for themselves.
      @Override
      public void menuCanceled(MenuEvent e) {
        for (UpdatableAction a : editMenuUpdatable) {
          a.setEnabled(true);
        }
      }

      @Override
      public void menuDeselected(MenuEvent e) {
        for (UpdatableAction a : editMenuUpdatable) {
          a.setEnabled(true);
        }
      }

      @Override
      public void menuSelected(MenuEvent e) {
        for (UpdatableAction a : editMenuUpdatable) {
          a.updateState();
        }
      }
    });
    return menu;
  }


  abstract public JMenu buildSketchMenu();


  protected JMenu buildSketchMenu(JMenuItem[] runItems) {
    JMenuItem item;
    sketchMenu = new JMenu(Language.text("menu.sketch"));

    for (JMenuItem mi : runItems) {
      sketchMenu.add(mi);
    }

    sketchMenu.addSeparator();

    sketchMenu.add(mode.getImportMenu());

    item = Toolkit.newJMenuItem(Language.text("menu.sketch.show_sketch_folder"), 'K');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Platform.openFolder(sketch.getFolder());
        }
      });
    sketchMenu.add(item);
    item.setEnabled(Platform.openFolderAvailable());

    item = new JMenuItem(Language.text("menu.sketch.add_file"));
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          sketch.handleAddFile();
        }
      });
    sketchMenu.add(item);

    if (runItems != null && runItems.length != 0) {
      sketchMenu.addSeparator();
    }

//    final Editor editorName = this;

    sketchMenu.addMenuListener(new MenuListener() {
      // Menu Listener that populates the menu only when the menu is opened
      List<JMenuItem> menuList = new ArrayList<>();

      @Override
      public void menuSelected(MenuEvent event) {
        JMenuItem item;
        for (final Editor editor : base.getEditors()) {
          //if (Editor.this.getSketch().getName().trim().contains(editor2.getSketch().getName().trim()))
          if (getSketch().getMainFilePath().equals(editor.getSketch().getMainFilePath())) {
            item = new JCheckBoxMenuItem(editor.getSketch().getName());
            item.setSelected(true);
          } else {
            item = new JMenuItem(editor.getSketch().getName());
          }
          item.setText(editor.getSketch().getName() +
                       " (" + editor.getMode().getTitle() + ")");

          // Action listener to bring the appropriate sketch in front
          item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
              editor.setState(Frame.NORMAL);
              editor.setVisible(true);
              editor.toFront();
            }
          });
          sketchMenu.add(item);
          menuList.add(item);
          Toolkit.setMenuMnemsInside(sketchMenu);
        }
      }

      @Override
      public void menuDeselected(MenuEvent event) {
        for (JMenuItem item : menuList) {
          sketchMenu.remove(item);
        }
        menuList.clear();
      }

      @Override
      public void menuCanceled(MenuEvent event) {
        menuDeselected(event);
      }
    });

    return sketchMenu;
  }


  abstract public void handleImportLibrary(String name);


  public void librariesChanged() { }

  public void codeFolderChanged() { }

  public void sketchChanged() { }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public JMenu getToolMenu() {
    return toolsMenu;
  }


  /*
  public JMenu getToolMenu() {
    if (toolsMenu == null) {
      rebuildToolMenu();
    }
    return toolsMenu;
  }

  public void removeTool() {
    rebuildToolMenu();
  }
  */


  /**
   * Clears the Tool menu and runs the gc so that contributions can be updated
   * without classes still being in use.
   */
  public void clearToolMenu() {
    toolsMenu.removeAll();
    System.gc();
  }


  /**
   * Updates update count in the UI. Called on EDT.
   * @param count number of available updates
   */
  public void setUpdatesAvailable(int count) {
    footer.setUpdateCount(count);
  }


  /**
   * Override this if you want a special menu for your particular 'mode'.
   */
  public JMenu buildModeMenu() {
    return null;
  }


  /*
  protected void addToolMenuItem(JMenu menu, String className) {
    try {
      Class<?> toolClass = Class.forName(className);
      final Tool tool = (Tool) toolClass.newInstance();

      JMenuItem item = new JMenuItem(tool.getMenuTitle());

      tool.init(Editor.this);

      item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          EventQueue.invokeLater(tool);
        }
      });
      menu.add(item);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  protected JMenu addInternalTools(JMenu menu) {
    addToolMenuItem(menu, "processing.app.tools.CreateFont");
    addToolMenuItem(menu, "processing.app.tools.ColorSelector");
    addToolMenuItem(menu, "processing.app.tools.Archiver");

    if (Platform.isMacOS()) {
      addToolMenuItem(menu, "processing.app.tools.InstallCommander");
    }

    return menu;
  }
  */


  /*
  // testing internal web server to serve up docs from a zip file
  item = new JMenuItem("Web Server Test");
  item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        //WebServer ws = new WebServer();
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            try {
              int port = WebServer.launch("/Users/fry/coconut/processing/build/shared/reference.zip");
              Base.openURL("http://127.0.0.1:" + port + "/reference/setup_.html");

            } catch (IOException e1) {
              e1.printStackTrace();
            }
          }
        });
      }
    });
  menu.add(item);
  */

  /*
  item = new JMenuItem("Browser Test");
  item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        //Base.openURL("http://processing.org/learning/gettingstarted/");
        //JFrame browserFrame = new JFrame("Browser");
        BrowserStartup bs = new BrowserStartup("jar:file:/Users/fry/coconut/processing/build/shared/reference.zip!/reference/setup_.html");
        bs.initUI();
        bs.launch();
      }
    });
  menu.add(item);
  */


  abstract public JMenu buildHelpMenu();


  public void showReference(String filename) {
    File file = new File(mode.getReferenceFolder(), filename);
    showReferenceFile(file);
  }


  /**
   * Given the .html file, displays it in the default browser.
   *
   * @param file
   */
  public void showReferenceFile(File file) {
    try {
      file = file.getCanonicalFile();
    } catch (IOException e) {
      e.printStackTrace();
    }

    // Prepend with file:// and also encode spaces & other characters
    Platform.openURL(file.toURI().toString());
  }


  static public void showChanges() {
    // http://code.google.com/p/processing/issues/detail?id=1520
    if (!Base.isCommandLine()) {
      Platform.openURL("https://github.com/processing/processing/wiki/Changes");
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  /**
   * Subclass if you want to have setEnabled(canDo()); called when your menu
   * is opened.
   */
  abstract class UpdatableAction extends AbstractAction {
    public UpdatableAction(String name) {
      super(name);
    }

    abstract public boolean canDo();

    public void updateState() {
      setEnabled(canDo());
    }
  }


  class CutAction extends UpdatableAction {
    public CutAction() {
      super(Language.text("menu.edit.cut"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      handleCut();
    }

    public boolean canDo() {
      return textarea.isSelectionActive();
    }
  }


  class CopyAction extends UpdatableAction {
    public CopyAction() {
      super(Language.text("menu.edit.copy"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      handleCopy();
    }

    public boolean canDo() {
      return textarea.isSelectionActive();
    }
  }


  class CopyAsHtmlAction extends UpdatableAction {
    public CopyAsHtmlAction() {
      super(Language.text("menu.edit.copy_as_html"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      handleCopyAsHTML();
    }

    public boolean canDo() {
      return textarea.isSelectionActive();
    }
  }


  class PasteAction extends UpdatableAction {
    public PasteAction() {
      super(Language.text("menu.edit.paste"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      textarea.paste();
      sketch.setModified(true);
    }

    public boolean canDo() {
      return getToolkit().getSystemClipboard()
          .isDataFlavorAvailable(DataFlavor.stringFlavor);
    }
  }


  class UndoAction extends AbstractAction {
    public UndoAction() {
      super(Language.text("menu.edit.undo"));
      this.setEnabled(false);
    }

    public void actionPerformed(ActionEvent e) {
      stopCompoundEdit();

      try {
        final Integer caret = caretUndoStack.pop();
        caretRedoStack.push(caret);
        textarea.setCaretPosition(caret);
        textarea.scrollToCaret();
      } catch (Exception ignore) {
      }
      try {
        undo.undo();
      } catch (CannotUndoException ex) {
        //System.out.println("Unable to undo: " + ex);
        //ex.printStackTrace();
      }
      updateUndoState();
      redoAction.updateRedoState();
      if (sketch != null) {
        sketch.setModified(!getText().equals(sketch.getCurrentCode().getSavedProgram()));
        // Go through all tabs; Replace All, Rename or Undo could have changed them
        for (SketchCode sc : sketch.getCode()) {
          if (sc.getDocument() != null) {
            try {
              sc.setModified(!sc.getDocumentText().equals(sc.getSavedProgram()));
            } catch (BadLocationException ignore) { }
          }
        }
        repaintHeader();
      }
    }

    protected void updateUndoState() {
      if (undo.canUndo() || compoundEdit != null && compoundEdit.isInProgress()) {
        this.setEnabled(true);
        undoItem.setEnabled(true);
        String newUndoPresentationName = Language.text("menu.edit.undo");
        if (undo.getUndoPresentationName().equals("Undo addition")) {
          newUndoPresentationName += " "+Language.text("menu.edit.action.addition");
        } else if (undo.getUndoPresentationName().equals("Undo deletion")) {
          newUndoPresentationName += " "+Language.text("menu.edit.action.deletion");
        }
        undoItem.setText(newUndoPresentationName);
        putValue(Action.NAME, newUndoPresentationName);
//        if (sketch != null) {
//          sketch.setModified(true);  // 0107, removed for 0196
//        }
      } else {
        this.setEnabled(false);
        undoItem.setEnabled(false);
        undoItem.setText(Language.text("menu.edit.undo"));
        putValue(Action.NAME, Language.text("menu.edit.undo"));
//        if (sketch != null) {
//          sketch.setModified(false);  // 0107
//        }
      }
    }
  }


  class RedoAction extends AbstractAction {
    public RedoAction() {
      super(Language.text("menu.edit.redo"));
      this.setEnabled(false);
    }

    public void actionPerformed(ActionEvent e) {
      stopCompoundEdit();

      try {
        undo.redo();
      } catch (CannotRedoException ex) {
        //System.out.println("Unable to redo: " + ex);
        //ex.printStackTrace();
      }
      try {
        final Integer caret = caretRedoStack.pop();
        caretUndoStack.push(caret);
        textarea.setCaretPosition(caret);
      } catch (Exception ignore) {
      }
      updateRedoState();
      undoAction.updateUndoState();
      if (sketch != null) {
        sketch.setModified(!getText().equals(sketch.getCurrentCode().getSavedProgram()));
        // Go through all tabs; Replace All, Rename or Undo could have changed them
        for (SketchCode sc : sketch.getCode()) {
          if (sc.getDocument() != null) {
            try {
              sc.setModified(!sc.getDocumentText().equals(sc.getSavedProgram()));
            } catch (BadLocationException ignore) {
            }
          }
        }
        repaintHeader();
      }
    }

    protected void updateRedoState() {
      if (undo.canRedo()) {
        redoItem.setEnabled(true);
        String newRedoPresentationName = Language.text("menu.edit.redo");
        if (undo.getRedoPresentationName().equals("Redo addition")) {
          newRedoPresentationName += " " + Language.text("menu.edit.action.addition");
        } else if (undo.getRedoPresentationName().equals("Redo deletion")) {
          newRedoPresentationName += " " + Language.text("menu.edit.action.deletion");
        }
        redoItem.setText(newRedoPresentationName);
        putValue(Action.NAME, newRedoPresentationName);
      } else {
        this.setEnabled(false);
        redoItem.setEnabled(false);
        redoItem.setText(Language.text("menu.edit.redo"));
        putValue(Action.NAME, Language.text("menu.edit.redo"));
      }
    }
  }


  class FindNextAction extends UpdatableAction {
    public FindNextAction() {
      super(Language.text("menu.edit.find_next"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (find != null) find.findNext();
    }

    public boolean canDo() {
      return find != null && find.canFindNext();
    }
  }


  class FindPreviousAction extends UpdatableAction {
    public FindPreviousAction() {
      super(Language.text("menu.edit.find_previous"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (find != null) find.findPrevious();
    }

    public boolean canDo() {
      return find != null && find.canFindNext();
    }
  }


  class SelectionForFindAction extends UpdatableAction {
    public SelectionForFindAction() {
      super(Language.text("menu.edit.use_selection_for_find"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (find == null) {
        find = new FindReplace(Editor.this);
      }
      find.setFindText(getSelectedText());
    }

    public boolean canDo() {
      return textarea.isSelectionActive();
    }
  }

  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  // these will be done in a more generic way soon, more like:
  // setHandler("action name", Runnable);
  // but for the time being, working out the kinks of how many things to
  // abstract from the editor in this fashion.


//  public void setHandlers(Runnable runHandler, Runnable presentHandler,
//                          Runnable stopHandler,
//                          Runnable exportHandler, Runnable exportAppHandler) {
//    this.runHandler = runHandler;
//    this.presentHandler = presentHandler;
//    this.stopHandler = stopHandler;
//    this.exportHandler = exportHandler;
//    this.exportAppHandler = exportAppHandler;
//  }


//  public void resetHandlers() {
//    runHandler = new DefaultRunHandler();
//    presentHandler = new DefaultPresentHandler();
//    stopHandler = new DefaultStopHandler();
//    exportHandler = new DefaultExportHandler();
//    exportAppHandler = new DefaultExportAppHandler();
//  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Gets the current sketch object.
   */
  public Sketch getSketch() {
    return sketch;
  }


  /**
   * Get the JEditTextArea object for use (not recommended). This should only
   * be used in obscure cases that really need to hack the internals of the
   * JEditTextArea. Most tools should only interface via the get/set functions
   * found in this class. This will maintain compatibility with future releases,
   * which will not use JEditTextArea.
   */
  public JEditTextArea getTextArea() {
    return textarea;
  }


  public PdeTextArea getPdeTextArea() {
    return (textarea instanceof PdeTextArea) ? (PdeTextArea) textarea : null;
  }


  /**
   * Get the contents of the current buffer. Used by the Sketch class.
   */
  public String getText() {
    return textarea.getText();
  }


  /**
   * Get a range of text from the current buffer.
   */
  public String getText(int start, int stop) {
    return textarea.getText(start, stop - start);
  }


  /**
   * Replace the entire contents of the front-most tab. Note that this does
   * a compound edit, so internal callers may want to use textarea.setText()
   * if this is part of a larger compound edit.
   */
  public void setText(String what) {
    startCompoundEdit();
    textarea.setText(what);
    stopCompoundEdit();
  }


  public void insertText(String what) {
    startCompoundEdit();
    int caret = getCaretOffset();
    setSelection(caret, caret);
    textarea.setSelectedText(what);
    stopCompoundEdit();
  }


  public String getSelectedText() {
    return textarea.getSelectedText();
  }


  public void setSelectedText(String what) {
    textarea.setSelectedText(what);
  }


  public void setSelectedText(String what, boolean ever) {
    textarea.setSelectedText(what, ever);
  }


  public void setSelection(int start, int stop) {
    // make sure that a tool isn't asking for a bad location
    start = PApplet.constrain(start, 0, textarea.getDocumentLength());
    stop = PApplet.constrain(stop, 0, textarea.getDocumentLength());

    textarea.select(start, stop);
  }


  /**
   * Get the position (character offset) of the caret. With text selected,
   * this will be the last character actually selected, no matter the direction
   * of the selection. That is, if the user clicks and drags to select lines
   * 7 up to 4, then the caret position will be somewhere on line four.
   */
  public int getCaretOffset() {
    return textarea.getCaretPosition();
  }


  /**
   * True if some text is currently selected.
   */
  public boolean isSelectionActive() {
    return textarea.isSelectionActive();
  }


  /**
   * Get the beginning point of the current selection.
   */
  public int getSelectionStart() {
    return textarea.getSelectionStart();
  }


  /**
   * Get the end point of the current selection.
   */
  public int getSelectionStop() {
    return textarea.getSelectionStop();
  }


  /**
   * Get text for a specified line.
   */
  public String getLineText(int line) {
    return textarea.getLineText(line);
  }


  /**
   * Replace the text on a specified line.
   */
  public void setLineText(int line, String what) {
    startCompoundEdit();
    textarea.select(getLineStartOffset(line), getLineStopOffset(line));
    textarea.setSelectedText(what);
    stopCompoundEdit();
  }


  /**
   * Get character offset for the start of a given line of text.
   */
  public int getLineStartOffset(int line) {
    return textarea.getLineStartOffset(line);
  }


  /**
   * Get character offset for end of a given line of text.
   */
  public int getLineStopOffset(int line) {
    return textarea.getLineStopOffset(line);
  }


  /**
   * Get the number of lines in the currently displayed buffer.
   */
  public int getLineCount() {
    return textarea.getLineCount();
  }


  /**
   * Use before a manipulating text to group editing operations together
   * as a single undo. Use stopCompoundEdit() once finished.
   */
  public void startCompoundEdit() {
    // Call endTextEditHistory() before starting a new CompoundEdit,
    // because there's a timer that's possibly set off for 3 seconds after
    // which endTextEditHistory() is called, which means that things get
    // messed up. Hence, manually call this method so that auto-format gets
    // undone in one fell swoop if the user calls auto-formats within 3
    // seconds of typing in the last character. Then start a new compound
    // edit so that the auto-format can be undone in one go.
    // https://github.com/processing/processing/issues/3003
    endTextEditHistory();  // also calls stopCompoundEdit()

    //stopCompoundEdit();
    compoundEdit = new CompoundEdit();
    caretUndoStack.push(textarea.getCaretPosition());
    caretRedoStack.clear();
  }


  /**
   * Use with startCompoundEdit() to group edit operations in a single undo.
   */
  public void stopCompoundEdit() {
    if (compoundEdit != null) {
      compoundEdit.end();
      undo.addEdit(compoundEdit);
      undoAction.updateUndoState();
      redoAction.updateRedoState();
      compoundEdit = null;
    }
  }


  public int getScrollPosition() {
    return textarea.getVerticalScrollPosition();
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Switch between tabs, this swaps out the Document object
   * that's currently being manipulated.
   */
  public void setCode(SketchCode code) {
    SyntaxDocument document = (SyntaxDocument) code.getDocument();

    if (document == null) {  // this document not yet inited
      document = new SyntaxDocument() {
        @Override
        public void beginCompoundEdit() {
          if (compoundEdit == null)
            startCompoundEdit();
          super.beginCompoundEdit();
        }

        @Override
        public void endCompoundEdit() {
          stopCompoundEdit();
          super.endCompoundEdit();
        }
      };
      code.setDocument(document);

      // turn on syntax highlighting
      document.setTokenMarker(mode.getTokenMarker(code));

      // insert the program text into the document object
      try {
        document.insertString(0, code.getProgram(), null);
      } catch (BadLocationException bl) {
        bl.printStackTrace();
      }

      // set up this guy's own undo manager
//      code.undo = new UndoManager();

      document.addDocumentListener(new DocumentListener() {

        public void removeUpdate(DocumentEvent e) {
          if (isInserting && isDirectEdit() && !textarea.isOverwriteEnabled()) {
            endTextEditHistory();
          }
          isInserting = false;
        }

        public void insertUpdate(DocumentEvent e) {
          if (!isInserting && !textarea.isOverwriteEnabled() && isDirectEdit()) {
            endTextEditHistory();
          }

          if (!textarea.isOverwriteEnabled()) {
            isInserting = true;
          }
        }

        public void changedUpdate(DocumentEvent e) {
          endTextEditHistory();
        }
      });

      // connect the undo listener to the editor
      document.addUndoableEditListener(new UndoableEditListener() {

          public void undoableEditHappened(UndoableEditEvent e) {
            // if an edit is in progress, reset the timer
            if (endUndoEvent != null) {
              endUndoEvent.cancel();
              endUndoEvent = null;
              startTimerEvent();
            }

            // if this edit is just getting started, create a compound edit
            if (compoundEdit == null) {
              startCompoundEdit();
              startTimerEvent();
            }

            compoundEdit.addEdit(e.getEdit());
            undoAction.updateUndoState();
            redoAction.updateRedoState();
          }
        });
    }

    // update the document object that's in use
    textarea.setDocument(document,
                         code.getSelectionStart(), code.getSelectionStop(),
                         code.getScrollPosition());

//    textarea.requestFocus();  // get the caret blinking
    textarea.requestFocusInWindow();  // required for caret blinking

    this.undo = code.getUndo();
    undoAction.updateUndoState();
    redoAction.updateRedoState();
  }

  /**
   * @return true if the text is being edited from direct input from typing and
   *         not shortcuts that manipulate text
   */
  boolean isDirectEdit() {
    return endUndoEvent != null;
  }


  void startTimerEvent() {
    endUndoEvent = new TimerTask() {
      public void run() {
        EventQueue.invokeLater(Editor.this::endTextEditHistory);
      }
    };
    timer.schedule(endUndoEvent, 3000);
    // let the gc eat the cancelled events
    timer.purge();
  }


  void endTextEditHistory() {
    if (endUndoEvent != null) {
      endUndoEvent.cancel();
      endUndoEvent = null;
    }
    stopCompoundEdit();
  }


  public void removeNotify() {
    timer.cancel();
    super.removeNotify();
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Implements Edit &rarr; Cut.
   */
  public void handleCut() {
    textarea.cut();
    sketch.setModified(true);
  }


  /**
   * Implements Edit &rarr; Copy.
   */
  public void handleCopy() {
    textarea.copy();
  }


  /**
   * Implements Edit &rarr; Copy as HTML.
   */
  public void handleCopyAsHTML() {
    textarea.copyAsHTML();
    statusNotice(Language.text("editor.status.copy_as_html"));
  }


  /**
   * Implements Edit &rarr; Paste.
   */
  public void handlePaste() {
    textarea.paste();
    sketch.setModified(true);
  }


  /**
   * Implements Edit &rarr; Select All.
   */
  public void handleSelectAll() {
    textarea.selectAll();
  }

//  /**
//   * @param moveUp
//   *          true to swap the selected lines with the line above, false to swap
//   *          with the line beneath
//   */
  /*
  public void handleMoveLines(boolean moveUp) {
    startCompoundEdit();

    int startLine = textarea.getSelectionStartLine();
    int stopLine = textarea.getSelectionStopLine();

    // if more than one line is selected and none of the characters of the end
    // line are selected, don't move that line
    if (startLine != stopLine
        && textarea.getSelectionStop() == textarea.getLineStartOffset(stopLine))
      stopLine--;

    int replacedLine = moveUp ? startLine - 1 : stopLine + 1;
    if (replacedLine < 0 || replacedLine >= textarea.getLineCount())
      return;

    final String source = getText();

    int replaceStart = textarea.getLineStartOffset(replacedLine);
    int replaceEnd = textarea.getLineStopOffset(replacedLine);
    if (replaceEnd == source.length() + 1)
      replaceEnd--;

    int selectionStart = textarea.getLineStartOffset(startLine);
    int selectionEnd = textarea.getLineStopOffset(stopLine);
    if (selectionEnd == source.length() + 1)
      selectionEnd--;

    String replacedText = source.substring(replaceStart, replaceEnd);
    String selectedText = source.substring(selectionStart, selectionEnd);
    if (replacedLine == textarea.getLineCount() - 1) {
      replacedText += "\n";
      selectedText = selectedText.substring(0, selectedText.length() - 1);
    } else if (stopLine == textarea.getLineCount() - 1) {
      selectedText += "\n";
      replacedText = replacedText.substring(0, replacedText.length() - 1);
    }

    int newSelectionStart, newSelectionEnd;
    if (moveUp) {
      // Change the selection, then change the line above
      textarea.select(selectionStart, selectionEnd);
      textarea.setSelectedText(replacedText);

      textarea.select(replaceStart, replaceEnd);
      textarea.setSelectedText(selectedText);

      newSelectionStart = textarea.getLineStartOffset(startLine - 1);
      newSelectionEnd = textarea.getLineStopOffset(stopLine - 1) -  1;
    } else {
      // Change the line beneath, then change the selection
      textarea.select(replaceStart, replaceEnd);
      textarea.setSelectedText(selectedText);

      textarea.select(selectionStart, selectionEnd);
      textarea.setSelectedText(replacedText);

      newSelectionStart = textarea.getLineStartOffset(startLine + 1);
      newSelectionEnd = textarea.getLineStopOffset(stopLine + 1) - 1;
    }

    textarea.select(newSelectionStart, newSelectionEnd);
    stopCompoundEdit();
  }
  */


  /*
  public void handleDeleteLines() {
    int startLine = textarea.getSelectionStartLine();
    int stopLine = textarea.getSelectionStopLine();

    int start = textarea.getLineStartOffset(startLine);
    int end = textarea.getLineStopOffset(stopLine);
    if (end == getText().length() + 1)
      end--;

    textarea.select(start, end);
    textarea.setSelectedText("");
  }
  */


  public void handleAutoFormat() {
    final String source = getText();

    try {
      final String formattedText = createFormatter().format(source);
      // save current (rough) selection point
      int selectionEnd = getSelectionStop();

//      boolean wasVisible =
//        textarea.getSelectionStopLine() >= textarea.getFirstLine() &&
//        textarea.getSelectionStopLine() < textarea.getLastLine();

      // make sure the caret would be past the end of the text
      if (formattedText.length() < selectionEnd - 1) {
        selectionEnd = formattedText.length() - 1;
      }

      if (formattedText.equals(source)) {
        statusNotice(Language.text("editor.status.autoformat.no_changes"));

      } else {  // replace with new bootiful text
        startCompoundEdit();
        // selectionEnd hopefully at least in the neighborhood
        int scrollPos = textarea.getVerticalScrollPosition();
        textarea.setText(formattedText);
        setSelection(selectionEnd, selectionEnd);

        // Put the scrollbar position back, otherwise it jumps on each format.
        // Since we're not doing a good job of maintaining position anyway,
        // a more complicated workaround here is fairly pointless.
        // http://code.google.com/p/processing/issues/detail?id=1533
        if (scrollPos != textarea.getVerticalScrollPosition()) {
          textarea.setVerticalScrollPosition(scrollPos);
        }
        stopCompoundEdit();
        sketch.setModified(true);
        statusNotice(Language.text("editor.status.autoformat.finished"));
      }

    } catch (final Exception e) {
      statusError(e);
    }
  }


  abstract public String getCommentPrefix();


  protected void handleCommentUncomment() {
    // log("Entering handleCommentUncomment()");
    startCompoundEdit();

    String prefix = getCommentPrefix();
    int prefixLen = prefix.length();

    int startLine = textarea.getSelectionStartLine();
    int stopLine = textarea.getSelectionStopLine();

    int lastLineStart = textarea.getLineStartOffset(stopLine);
    int selectionStop = textarea.getSelectionStop();
    // If the selection ends at the beginning of the last line,
    // then don't (un)comment that line.
    if (selectionStop == lastLineStart) {
      // Though if there's no selection, don't do that
      if (textarea.isSelectionActive()) {
        stopLine--;
      }
    }

    // If the text is empty, ignore the user.
    // Also ensure that all lines are commented (not just the first)
    // when determining whether to comment or uncomment.
    boolean commented = true;
    for (int i = startLine; commented && (i <= stopLine); i++) {
      String lineText = textarea.getLineText(i).trim();
      if (lineText.length() == 0) {
        continue; //ignore blank lines
      }
      commented = lineText.startsWith(prefix);
    }

    // log("Commented: " + commented);

    // This is the min line start offset of the selection, which is added to
    // all lines while adding a comment. Required when commenting
    // lines which have uneven whitespaces in the beginning. Makes the
    // commented lines look more uniform.
    int lso = Math.abs(textarea.getLineStartNonWhiteSpaceOffset(startLine)
        - textarea.getLineStartOffset(startLine));

    if (!commented) {
      // get min line start offset of all selected lines
      for (int line = startLine+1; line <= stopLine; line++) {
        String lineText = textarea.getLineText(line);
        if (lineText.trim().length() == 0) {
          continue; //ignore blank lines
        }
        int so = Math.abs(textarea.getLineStartNonWhiteSpaceOffset(line)
                              - textarea.getLineStartOffset(line));
        lso = Math.min(lso, so);
      }
    }

    for (int line = startLine; line <= stopLine; line++) {
      int location = textarea.getLineStartNonWhiteSpaceOffset(line);
      String lineText = textarea.getLineText(line);
      if (lineText.trim().length() == 0)
        continue; //ignore blank lines
      if (commented) {
        // remove a comment
        textarea.select(location, location + prefixLen);
        textarea.setSelectedText("");
      } else {
        // add a comment
        location = textarea.getLineStartOffset(line) + lso;
        textarea.select(location, location);
        textarea.setSelectedText(prefix);
      }
    }
    // Subtract one from the end, otherwise selects past the current line.
    // (Which causes subsequent calls to keep expanding the selection)
    textarea.select(textarea.getLineStartOffset(startLine),
                    textarea.getLineStopOffset(stopLine) - 1);
    stopCompoundEdit();
    sketch.setModified(true);
  }


  public void handleIndent() {
    handleIndentOutdent(true);
  }


  public void handleOutdent() {
    handleIndentOutdent(false);
  }


  public void handleIndentOutdent(boolean indent) {
    int tabSize = Preferences.getInteger("editor.tabs.size");
    String tabString = Editor.EMPTY.substring(0, tabSize);

    startCompoundEdit();

    int startLine = textarea.getSelectionStartLine();
    int stopLine = textarea.getSelectionStopLine();

    // If the selection ends at the beginning of the last line,
    // then don't (un)comment that line.
    int lastLineStart = textarea.getLineStartOffset(stopLine);
    int selectionStop = textarea.getSelectionStop();
    if (selectionStop == lastLineStart) {
      // Though if there's no selection, don't do that
      if (textarea.isSelectionActive()) {
        stopLine--;
      }
    }

    for (int line = startLine; line <= stopLine; line++) {
      int location = textarea.getLineStartOffset(line);

      if (indent) {
        textarea.select(location, location);
        textarea.setSelectedText(tabString);

      } else {  // outdent
        int last = Math.min(location + tabSize, textarea.getDocumentLength());
        textarea.select(location, last);
        // Don't eat code if it's not indented
        if (tabString.equals(textarea.getSelectedText())) {
          textarea.setSelectedText("");
        }
      }
    }
    // Subtract one from the end, otherwise selects past the current line.
    // (Which causes subsequent calls to keep expanding the selection)
    textarea.select(textarea.getLineStartOffset(startLine),
                    textarea.getLineStopOffset(stopLine) - 1);
    stopCompoundEdit();
    sketch.setModified(true);
  }


  static public boolean checkParen(char[] array, int index, int stop) {
//  boolean paren = false;
//  int stepper = i + 1;
//  while (stepper < mlength) {
//    if (array[stepper] == '(') {
//      paren = true;
//      break;
//    }
//    stepper++;
//  }
    while (index < stop) {
//    if (array[index] == '(') {
//      return true;
//    } else if (!Character.isWhitespace(array[index])) {
//      return false;
//    }
      switch (array[index]) {
      case '(':
        return true;

      case ' ':
      case '\t':
      case '\n':
      case '\r':
        index++;
        break;

      default:
//      System.out.println("defaulting because " + array[index] + " " + PApplet.hex(array[index]));
        return false;
      }
    }
//  System.out.println("exiting " + new String(array, index, stop - index));
    return false;
  }


  protected boolean functionable(char c) {
    return (c == '_') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
  }


  /**
   * Check the current selection for reference. If no selection is active,
   * expand the current selection.
   * @return
   */
  protected String referenceCheck(boolean selectIfFound) {
    int start = textarea.getSelectionStart();
    int stop = textarea.getSelectionStop();
    if (stop < start) {
      int temp = stop;
      stop = start;
      start = temp;
    }
    char[] c = textarea.getText().toCharArray();

//    System.out.println("checking reference");
    if (start == stop) {
      while (start > 0 && functionable(c[start - 1])) {
        start--;
      }
      while (stop < c.length && functionable(c[stop])) {
        stop++;
      }
//      System.out.println("start is stop");
    }
    String text = new String(c, start, stop - start).trim();
//    System.out.println("  reference piece is '" + text + "'");
    if (checkParen(c, stop, c.length)) {
      text += "_";
    }
    String ref = mode.lookupReference(text);
    if (selectIfFound) {
      textarea.select(start, stop);
    }
    return ref;
  }


  protected void handleFindReference() {
    String ref = referenceCheck(true);
    if (ref != null) {
      showReference(ref + ".html");
    } else {
      String text = textarea.getSelectedText();
      if (text == null) {
        statusNotice(Language.text("editor.status.find_reference.select_word_first"));
      } else {
        statusNotice(Language.interpolate("editor.status.find_reference.not_available", text.trim()));
      }
    }
  }


  /*
  protected void handleFindReference() {
    String text = textarea.getSelectedText().trim();

    if (text.length() == 0) {
      statusNotice("First select a word to find in the reference.");

    } else {
      char[] c = textarea.getText().toCharArray();
      int after = Math.max(textarea.getSelectionStart(), textarea.getSelectionStop());
      if (checkParen(c, after, c.length)) {
        text += "_";
        System.out.println("looking up ref for " + text);
      }
      String referenceFile = mode.lookupReference(text);
      System.out.println("reference file is " + referenceFile);
      if (referenceFile == null) {
        statusNotice("No reference available for \"" + text + "\"");
      } else {
        showReference(referenceFile + ".html");
      }
    }
  }


  protected void handleFindReference() {
    String text = textarea.getSelectedText().trim();

    if (text.length() == 0) {
      statusNotice("First select a word to find in the reference.");

    } else {
      String referenceFile = mode.lookupReference(text);
      //System.out.println("reference file is " + referenceFile);
      if (referenceFile == null) {
        statusNotice("No reference available for \"" + text + "\"");
      } else {
        showReference(referenceFile + ".html");
      }
    }
  }
  */


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Set the location of the sketch run window. Used by Runner to update the
   * Editor about window drag events while the sketch is running.
   */
  public void setSketchLocation(Point p) {
    sketchWindowLocation = p;
  }


  /**
   * Get the last location of the sketch's run window. Used by Runner to make
   * the window show up in the same location as when it was last closed.
   */
  public Point getSketchLocation() {
    return sketchWindowLocation;
  }


//  public void internalCloseRunner() {
//    mode.internalCloseRunner(this);
//  }


  public boolean isDebuggerEnabled() {
    return false;
  }


  public void toggleBreakpoint(int lineIndex) { }


  /**
   * Check if the sketch is modified and ask user to save changes.
   * @return false if canceling the close/quit operation
   */
  public boolean checkModified() {
    if (!sketch.isModified()) return true;

    // As of Processing 1.0.10, this always happens immediately.
    // http://dev.processing.org/bugs/show_bug.cgi?id=1456

    // With Java 7u40 on OS X, need to bring the window forward.
    toFront();

    if (!Platform.isMacOS()) {
      String prompt =
        Language.interpolate("close.unsaved_changes", sketch.getName());
      int result =
        JOptionPane.showConfirmDialog(this, prompt,
                                      Language.text("menu.file.close"),
                                      JOptionPane.YES_NO_CANCEL_OPTION,
                                      JOptionPane.QUESTION_MESSAGE);

      if (result == JOptionPane.YES_OPTION) {
        return handleSave(true);

      } else if (result == JOptionPane.NO_OPTION) {
        return true;  // ok to continue

      } else if (result == JOptionPane.CANCEL_OPTION ||
                 result == JOptionPane.CLOSED_OPTION) {
        return false;

      } else {
        throw new IllegalStateException();
      }

    } else {
      // This code is disabled unless Java 1.5 is being used on Mac OS X
      // because of a Java bug that prevents the initial value of the
      // dialog from being set properly (at least on my MacBook Pro).
      // The bug causes the "Don't Save" option to be the highlighted,
      // blinking, default. This sucks. But I'll tell you what doesn't
      // suck--workarounds for the Mac and Apple's snobby attitude about it!
      // I think it's nifty that they treat their developers like dirt.

      // Pane formatting adapted from the quaqua guide
      // http://www.randelshofer.ch/quaqua/guide/joptionpane.html
      JOptionPane pane =
        new JOptionPane("<html> " +
                        "<head> <style type=\"text/css\">"+
                        "b { font: 13pt \"Lucida Grande\" }"+
                        "p { font: 11pt \"Lucida Grande\"; margin-top: 8px }"+
                        "</style> </head>" +
                        "<b>" + Language.interpolate("save.title", sketch.getName()) + "</b>" +
                        "<p>" + Language.text("save.hint") + "</p>",
                        JOptionPane.QUESTION_MESSAGE);

      String[] options = new String[] {
        Language.text("save.btn.save"),
        Language.text("prompt.cancel"),
        Language.text("save.btn.dont_save")
      };
      pane.setOptions(options);

      // highlight the safest option ala apple hig
      pane.setInitialValue(options[0]);

      // on macosx, setting the destructive property places this option
      // away from the others at the lefthand side
      pane.putClientProperty("Quaqua.OptionPane.destructiveOption",
                             Integer.valueOf(2));

      JDialog dialog = pane.createDialog(this, null);
      dialog.setVisible(true);

      Object result = pane.getValue();
      if (result == options[0]) {  // save (and close/quit)
        return handleSave(true);

      } else if (result == options[2]) {  // don't save (still close/quit)
        return true;

      } else {  // cancel?
        return false;
      }
    }
  }


  /**
   * Second stage of open, occurs after having checked to see if the
   * modifications (if any) to the previous sketch need to be saved.
   * Because this method is called in Editor's constructor, a subclass
   * shouldn't rely on any of its variables being initialized already.
   */
  protected void handleOpenInternal(String path) throws EditorException {
    // check to make sure that this .pde file is
    // in a folder of the same name
    final File file = new File(path);
    final File parentFile = new File(file.getParent());
    final String parentName = parentFile.getName();
    final String defaultName = parentName + "." + mode.getDefaultExtension();
    final File altFile = new File(file.getParent(), defaultName);

    if (defaultName.equals(file.getName())) {
      // no beef with this guy
    } else if (altFile.exists()) {
      // The user selected a source file from the same sketch,
      // but open the file with the default extension instead.
      path = altFile.getAbsolutePath();
    } else if (!mode.canEdit(file)) {
      final String modeName = mode.getTitle().equals("Java") ?
        "Processing" : (mode.getTitle() + " Mode");
      throw new EditorException(modeName + " can only open its own sketches\n" +
                                "and other files ending in " +
                                mode.getDefaultExtension());
    } else {
      final String properParent =
        file.getName().substring(0, file.getName().lastIndexOf('.'));

      Object[] options = { Language.text("prompt.ok"), Language.text("prompt.cancel") };
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
          throw new EditorException("A folder named \"" + properParent + "\" " +
                                    "already exists. Can't open sketch.");
        }
        if (!properFolder.mkdirs()) {
          throw new EditorException("Could not create the sketch folder.");
        }
        // copy the sketch inside
        File properPdeFile = new File(properFolder, file.getName());
        File origPdeFile = new File(path);
        try {
          Util.copyFile(origPdeFile, properPdeFile);
        } catch (IOException e) {
          throw new EditorException("Could not copy to a proper location.", e);
        }

        // remove the original file, so user doesn't get confused
        origPdeFile.delete();

        // update with the new path
        path = properPdeFile.getAbsolutePath();

      } else {  //if (result == JOptionPane.NO_OPTION) {
        // Catch all other cases, including Cancel or ESC
        //return false;
        throw new EditorException();
      }
    }

    try {
      sketch = new Sketch(path, this);
    } catch (IOException e) {
      throw new EditorException("Could not create the sketch.", e);
    }

    header.rebuild();
    updateTitle();
    // Disable untitled setting from previous document, if any
//    untitled = false;

    // Store information on who's open and running
    // (in case there's a crash or something that can't be recovered)
    // TODO this probably need not be here because of the Recent menu, right?
    Preferences.save();
  }


  /**
   * Set the title of the PDE window based on the current sketch, i.e.
   * something like "sketch_070752a - Processing 0126"
   */
  public void updateTitle() {
    setTitle(sketch.getName() + " | Processing " + Base.getVersionName());

    if (!sketch.isUntitled()) {
      // set current file for OS X so that cmd-click in title bar works
      File sketchFile = sketch.getMainFile();
      getRootPane().putClientProperty("Window.documentFile", sketchFile);
    } else {
      // per other applications, don't set this until the file has been saved
      getRootPane().putClientProperty("Window.documentFile", null);
    }

//    toolbar.setText(sketch.getName());
  }


  /**
   * Actually handle the save command. If 'immediately' is set to false,
   * this will happen in another thread so that the message area
   * will update and the save button will stay highlighted while the
   * save is happening. If 'immediately' is true, then it will happen
   * immediately. This is used during a quit, because invokeLater()
   * won't run properly while a quit is happening. This fixes
   * <A HREF="http://dev.processing.org/bugs/show_bug.cgi?id=276">Bug 276</A>.
   */
  public boolean handleSave(boolean immediately) {
//    handleStop();  // 0136

    if (sketch.isUntitled()) {
      return handleSaveAs();
      // need to get the name, user might also cancel here

    } else if (immediately) {
      handleSaveImpl();

    } else {
      EventQueue.invokeLater(new Runnable() {
          public void run() {
            handleSaveImpl();
          }
        });
    }
    return true;
  }


  protected void handleSaveImpl() {
    statusNotice(Language.text("editor.status.saving"));
    try {
      if (sketch.save()) {
        statusNotice(Language.text("editor.status.saving.done"));
      } else {
        statusEmpty();
      }

    } catch (Exception e) {
      // show the error as a message in the window
      statusError(e);

      // zero out the current action,
      // so that checkModified2 will just do nothing
      //checkModifiedMode = 0;
      // this is used when another operation calls a save
    }
  }


  public boolean handleSaveAs() {
    statusNotice(Language.text("editor.status.saving"));
    try {
      if (sketch.saveAs()) {
        //statusNotice(Language.text("editor.status.saving.done"));
        // status is now printed from Sketch so that "Done Saving."
        // is only printed after Save As when progress bar is shown.
      } else {
        statusNotice(Language.text("editor.status.saving.canceled"));
        return false;
      }
    } catch (Exception e) {
      // show the error as a message in the window
      statusError(e);
    }
    return true;
  }


  /*
  public void handleSaveAs() {
    statusNotice(Language.text("editor.status.saving"));
    sketch.saveAs();
  }


  public void handleSaveAsSuccess() {
    statusNotice(Language.text("editor.status.saving.done"));
  }


  public void handleSaveAsCanceled() {
    statusNotice(Language.text("editor.status.saving.canceled"));
  }


  public void handleSaveAsError(Exception e) {
    statusError(e);
  }
  */


  /**
   * Handler for File &rarr; Page Setup.
   */
  public void handlePageSetup() {
    //printerJob = null;
    if (printerJob == null) {
      printerJob = PrinterJob.getPrinterJob();
    }
    if (pageFormat == null) {
      pageFormat = printerJob.defaultPage();
    }
    pageFormat = printerJob.pageDialog(pageFormat);
    //System.out.println("page format is " + pageFormat);
  }


  /**
   * Handler for File &rarr; Print.
   */
  public void handlePrint() {
    statusNotice(Language.text("editor.status.printing"));

    StringBuilder html = new StringBuilder("<html><body>");
    for (SketchCode tab : sketch.getCode()) {
      html.append("<b>" + tab.getPrettyName() + "</b><br>");
      html.append(textarea.getTextAsHtml((SyntaxDocument)tab.getDocument()));
      html.append("<br>");
    }
    html.setLength(html.length() - 4); // Don't want last <br>.
    html.append("</body></html>");
    JTextPane jtp = new JTextPane();
    // Needed for good line wrapping; otherwise one very long word breaks
    // wrapping for the whole document.
    jtp.setEditorKit(new HTMLEditorKit() {
      public ViewFactory getViewFactory() {
        return new HTMLFactory() {
          public View create(Element e) {
            View v = super.create(e);
            if (!(v instanceof javax.swing.text.html.ParagraphView))
              return v;
            else
              return new javax.swing.text.html.ParagraphView(e) {
                protected SizeRequirements calculateMinorAxisRequirements(
                    int axis, SizeRequirements r) {
                  r = super.calculateMinorAxisRequirements(axis, r);
                  r.minimum = 1;
                  return r;
                }
              };
          }
        };
      }
    });
    jtp.setFont(new Font(Preferences.get("editor.font.family"), Font.PLAIN, 10));
    jtp.setText(html.toString().replace("\n", "<br>") // Not in a <pre>.
        .replaceAll("(?<!&nbsp;)&nbsp;", " "));       // Allow line wrap.

    //printerJob = null;
    if (printerJob == null) {
      printerJob = PrinterJob.getPrinterJob();
    }
    if (pageFormat != null) {
      //System.out.println("setting page format " + pageFormat);
      printerJob.setPrintable(jtp.getPrintable(null, null), pageFormat);
    } else {
      printerJob.setPrintable(jtp.getPrintable(null, null));
    }
    // set the name of the job to the code name
    printerJob.setJobName(sketch.getCurrentCode().getPrettyName());

    if (printerJob.printDialog()) {
      try {
        printerJob.print();
        statusNotice(Language.text("editor.status.printing.done"));

      } catch (PrinterException pe) {
        statusError(Language.text("editor.status.printing.error"));
        pe.printStackTrace();
      }
    } else {
      statusNotice(Language.text("editor.status.printing.canceled"));
    }
    //printerJob = null;  // clear this out?
  }


  /**
   * Grab current contents of the sketch window, advance the console,
   * stop any other running sketches... not in that order.
   * It's essential that this function be called by any Mode subclass,
   * otherwise current edits may not be stored for getProgram().
   */
  public void prepareRun() {
    internalCloseRunner();
    statusEmpty();

    // do this to advance/clear the terminal window / dos prompt / etc
    for (int i = 0; i < 10; i++) System.out.println();

    // clear the console on each run, unless the user doesn't want to
    if (Preferences.getBoolean("console.auto_clear")) {
      console.clear();
    }

    // make sure the user didn't hide the sketch folder
    sketch.ensureExistence();

    // make sure any edits have been stored
    //current.setProgram(editor.getText());
    // Go through all tabs; Replace All, Rename or Undo could have changed them
    for (SketchCode sc : sketch.getCode()) {
      if (sc.getDocument() != null) {
        try {
          sc.setProgram(sc.getDocumentText());
        } catch (BadLocationException e) {
        }
      }
    }

//    // if an external editor is being used, need to grab the
//    // latest version of the code from the file.
//    if (Preferences.getBoolean("editor.external")) {
//      sketch.reload();
//    }
  }


  /**
   * Halt the current runner for whatever reason. Might be the VM dying,
   * the window closing, an error...
   */
  abstract public void internalCloseRunner();


  abstract public void deactivateRun();


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public ErrorTable getErrorTable() {
    return errorTable;
  }


  /**
   * Called by ErrorTable when a row is selected. Action taken is specific
   * to each Mode, based on the object passed in.
   */
  public void errorTableClick(Object item) {
    highlight((Problem) item);
  }


  public void errorTableDoubleClick(Object item) { }


  /**
   * Handle whether the tiny red error indicator is shown near
   * the error button at the bottom of the PDE
   */
  public void updateErrorToggle(boolean hasErrors) {
    if (errorTable != null) {
      footer.setNotification(errorTable.getParent(), hasErrors);
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Show an error in the status bar.
   */
  public void statusError(String what) {
    status.error(what);
    //new Exception("deactivating RUN").printStackTrace();
//    toolbar.deactivate(EditorToolbar.RUN);
  }


  /**
   * Show an exception in the editor status bar.
   */
  public void statusError(Exception e) {
    e.printStackTrace();
//    if (e == null) {
//      System.err.println("Editor.statusError() was passed a null exception.");
//      return;
//    }

    if (e instanceof SketchException) {
      SketchException re = (SketchException) e;

      // Make sure something is printed into the console
      // Status bar is volatile
      if (!re.isStackTraceEnabled()) {
        System.err.println(re.getMessage());
      }

      // Move the cursor to the line before updating the status bar, otherwise
      // status message might get hidden by a potential message caused by moving
      // the cursor to a line with warning in it

      if (re.hasCodeIndex()) {
        sketch.setCurrentCode(re.getCodeIndex());
      }
      if (re.hasCodeLine()) {
        int line = re.getCodeLine();
        // subtract one from the end so that the \n ain't included
        if (line >= textarea.getLineCount()) {
          // The error is at the end of this current chunk of code,
          // so the last line needs to be selected.
          line = textarea.getLineCount() - 1;
          if (textarea.getLineText(line).length() == 0) {
            // The last line may be zero length, meaning nothing to select.
            // If so, back up one more line.
            line--;
          }
        }
        if (line < 0 || line >= textarea.getLineCount()) {
          System.err.println("Bad error line: " + line);
        } else {
          textarea.select(textarea.getLineStartOffset(line),
                          textarea.getLineStopOffset(line) - 1);
        }
      }
    }

    // Since this will catch all Exception types, spend some time figuring
    // out which kind and try to give a better error message to the user.
    String mess = e.getMessage();
    if (mess != null) {
      String javaLang = "java.lang.";
      if (mess.indexOf(javaLang) == 0) {
        mess = mess.substring(javaLang.length());
      }
      // The phrase "RuntimeException" isn't useful for most users
      String rxString = "RuntimeException: ";
      if (mess.startsWith(rxString)) {
        mess = mess.substring(rxString.length());
      }
      // This is just confusing for most PDE users (save it for Eclipse users)
      String illString = "IllegalArgumentException: ";
      if (mess.startsWith(illString)) {
        mess = mess.substring(illString.length());
      }
      // This is confusing and common with the size() and fullScreen() changes
      String illState = "IllegalStateException: ";
      if (mess.startsWith(illState)) {
        mess = mess.substring(illState.length());
      }
      statusError(mess);
    }
//    e.printStackTrace();
  }


  /**
   * Show a notice message in the editor status bar.
   */
  public void statusNotice(String msg) {
    if (msg == null) {
      new IllegalArgumentException("This code called statusNotice(null)").printStackTrace();
      msg = "";
    }
    status.notice(msg);
  }


  public void clearNotice(String msg) {
    if (status.message.equals(msg)) {
      statusEmpty();
    }
  }


  /**
   * Returns the current notice message in the editor status bar.
   */
  public String getStatusMessage() {
    return status.message;
  }


  /**
   * Returns the current notice message in the editor status bar.
   */
  public int getStatusMode() {
    return status.mode;
  }


//  /**
//   * Returns the current mode of the editor status bar: NOTICE, ERR or EDIT.
//   */
//  public int getStatusMode() {
//    return status.mode;
//  }


  /**
   * Clear the status area.
   */
  public void statusEmpty() {
    statusNotice(EMPTY);
  }


  public void statusMessage(String message, int type) {
    if (EventQueue.isDispatchThread()) {
      status.message(message, type);
    } else {
      EventQueue.invokeLater(() -> statusMessage(message, type));
    }
  }


  public void startIndeterminate() {
    status.startIndeterminate();
  }


  public void stopIndeterminate() {
    status.stopIndeterminate();
  }


  public void statusHalt() {
    // stop called by someone else
  }


  public boolean isHalted() {
    return false;
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public void setProblemList(List<Problem> problems) {
    this.problems = problems;
    boolean hasErrors = problems.stream().anyMatch(Problem::isError);
    updateErrorTable(problems);
    errorColumn.updateErrorPoints(problems);
    textarea.repaint();
    updateErrorToggle(hasErrors);
    updateEditorStatus();
  }


  /**
   * Updates the error table in the Error Window.
   */
  public void updateErrorTable(List<Problem> problems) {
    if (errorTable != null) {
      errorTable.clearRows();

      for (Problem p : problems) {
        String message = p.getMessage();
        System.err.println("******* " + message);
        errorTable.addRow(p, message,
                          sketch.getCode(p.getTabIndex()).getPrettyName(),
                          Integer.toString(p.getLineNumber() + 1));
        // Added +1 because lineNumbers internally are 0-indexed
      }
    }
  }


  public void highlight(Problem p) {
    if (p != null) {
      highlight(p.getTabIndex(), p.getStartOffset(), p.getStopOffset());
    }
  }


  public void highlight(int tabIndex, int startOffset, int stopOffset) {
    // Switch to tab
    toFront();
    sketch.setCurrentCode(tabIndex);

    // Make sure offsets are in bounds
    int length = textarea.getDocumentLength();
    startOffset = PApplet.constrain(startOffset, 0, length);
    stopOffset = PApplet.constrain(stopOffset, 0, length);

    // Highlight the code
    textarea.select(startOffset, stopOffset);

    // Scroll to error line
    textarea.scrollToCaret();
    repaint();
  }


  public List<Problem> getProblems() {
    return problems;
  }


  /**
   * Updates editor status bar, depending on whether the caret is on an error
   * line or not
   */
  public void updateEditorStatus() {
    Problem problem = findProblem(textarea.getCaretLine());
    if (problem != null) {
      int type = problem.isError() ?
        EditorStatus.CURSOR_LINE_ERROR : EditorStatus.CURSOR_LINE_WARNING;
      statusMessage(problem.getMessage(), type);
    } else {
      switch (getStatusMode()) {
        case EditorStatus.CURSOR_LINE_ERROR:
        case EditorStatus.CURSOR_LINE_WARNING:
          statusEmpty();
          break;
      }
    }
  }


  /**
   * @return the Problem for the most relevant error or warning on 'line',
   *         defaults to the first error, if there are no errors first warning.
   */
  protected Problem findProblem(int line) {
    List<Problem> problems = findProblems(line);
    for (Problem p : problems) {
      if (p.isError()) return p;
    }
    return problems.isEmpty() ? null : problems.get(0);
  }


  public List<Problem> findProblems(int line) {
    int currentTab = getSketch().getCurrentCodeIndex();
    return problems.stream()
        .filter(p -> p.getTabIndex() == currentTab)
        .filter(p -> {
          int pStartLine = p.getLineNumber();
          int pEndOffset = p.getStopOffset();
          int pEndLine = textarea.getLineOfOffset(pEndOffset);
          return line >= pStartLine && line <= pEndLine;
        })
        .collect(Collectors.toList());
  }


  public void repaintErrorBar() {
    errorColumn.repaint();
  }


  public void showConsole() {
    footer.setPanel(console);
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  static Font font;
  static Color textColor;
  static Color bgColorWarning;
  static Color bgColorError;


  public void statusToolTip(JComponent comp, String message, boolean error) {
    if (font == null) {
      font = Toolkit.getSansFont(Toolkit.zoom(9), Font.PLAIN);
      textColor = mode.getColor("errors.selection.fgcolor");
      bgColorWarning = mode.getColor("errors.selection.warning.bgcolor");
      bgColorError = mode.getColor("errors.selection.error.bgcolor");
    }

    Color bgColor = error ? bgColorError : bgColorWarning;
    int m = Toolkit.zoom(3);
    String css =
      String.format("margin: %d %d %d %d; ", -m, -m, -m, -m) +
      String.format("padding: %d %d %d %d; ", m, m, m, m) +
      "background: #" + PApplet.hex(bgColor.getRGB(), 8).substring(2) + ";" +
      "font-family: " + font.getFontName() + ", sans-serif;" +
      "font-size: " + font.getSize() + "px;";
    String content =
      "<html> <div style='" + css + "'>" + message + "</div> </html>";
    comp.setToolTipText(content);
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Returns the edit popup menu.
   */
  class TextAreaPopup extends JPopupMenu {
    JMenuItem cutItem, copyItem, discourseItem, pasteItem,
      referenceItem;

    public TextAreaPopup() {
      JMenuItem item;

      cutItem = new JMenuItem(Language.text("menu.edit.cut"));
      cutItem.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            handleCut();
          }
      });
      this.add(cutItem);

      copyItem = new JMenuItem(Language.text("menu.edit.copy"));
      copyItem.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            handleCopy();
          }
        });
      this.add(copyItem);

      discourseItem = new JMenuItem(Language.text("menu.edit.copy_as_html"));
      discourseItem.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            handleCopyAsHTML();
          }
        });
      this.add(discourseItem);

      pasteItem = new JMenuItem(Language.text("menu.edit.paste"));
      pasteItem.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            handlePaste();
          }
        });
      this.add(pasteItem);

      item = new JMenuItem(Language.text("menu.edit.select_all"));
      item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleSelectAll();
        }
      });
      this.add(item);

      this.addSeparator();

      item = new JMenuItem(Language.text("menu.edit.comment_uncomment"));
      item.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            handleCommentUncomment();
          }
      });
      this.add(item);

      item = new JMenuItem("\u2192 " + Language.text("menu.edit.increase_indent"));
      item.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            handleIndentOutdent(true);
          }
      });
      this.add(item);

      item = new JMenuItem("\u2190 " + Language.text("menu.edit.decrease_indent"));
      item.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            handleIndentOutdent(false);
          }
      });
      this.add(item);

      this.addSeparator();

      referenceItem = new JMenuItem(Language.text("find_in_reference"));
      referenceItem.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            handleFindReference();
          }
        });
      this.add(referenceItem);

      Toolkit.setMenuMnemonics(this);
    }

    // if no text is selected, disable copy and cut menu items
    public void show(Component component, int x, int y) {
//      if (textarea.isSelectionActive()) {
//        cutItem.setEnabled(true);
//        copyItem.setEnabled(true);
//        discourseItem.setEnabled(true);
//
////        String sel = textarea.getSelectedText().trim();
////        String referenceFile = mode.lookupReference(sel);
////        referenceItem.setEnabled(referenceFile != null);
//
//      } else {
//        cutItem.setEnabled(false);
//        copyItem.setEnabled(false);
//        discourseItem.setEnabled(false);
////        referenceItem.setEnabled(false);
//      }

      // Centralize the checks for each item at the Action.
//      boolean active = textarea.isSelectionActive();
      cutItem.setEnabled(cutAction.canDo());
      copyItem.setEnabled(copyAction.canDo());
      discourseItem.setEnabled(copyAsHtmlAction.canDo());
      pasteItem.setEnabled(pasteAction.canDo());
      referenceItem.setEnabled(referenceCheck(false) != null);
      super.show(component, x, y);
    }
  }
}

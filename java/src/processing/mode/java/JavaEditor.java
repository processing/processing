package processing.mode.java;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.table.TableModel;
import javax.swing.text.Document;

import org.eclipse.jdt.core.compiler.IProblem;

import processing.core.PApplet;
import processing.app.*;
import processing.app.Toolkit;
import processing.app.contrib.AvailableContribution;
import processing.app.contrib.Contribution;
import processing.app.contrib.ContributionListing;
import processing.app.contrib.ContributionManager;
import processing.app.contrib.ToolContribution;
import processing.app.syntax.JEditTextArea;
import processing.app.syntax.PdeTextAreaDefaults;
import processing.mode.java.debug.LineBreakpoint;
import processing.mode.java.debug.LineHighlight;
import processing.mode.java.debug.LineID;
import processing.mode.java.pdex.ErrorCheckerService;
import processing.mode.java.pdex.ErrorMarker;
import processing.mode.java.pdex.ErrorMessageSimplifier;
import processing.mode.java.pdex.JavaTextArea;
import processing.mode.java.pdex.Problem;
import processing.mode.java.pdex.XQErrorTable;
import processing.mode.java.runner.Runner;
import processing.mode.java.tweak.ColorControlBox;
import processing.mode.java.tweak.Handle;
import processing.mode.java.tweak.SketchParser;
import processing.mode.java.tweak.TweakClient;


public class JavaEditor extends Editor {
  JavaMode jmode;

  // Runner associated with this editor window
  private Runner runtime;

  // Need to sort through the rest of these additions...

  protected Color breakpointColor;
  protected Color currentLineColor;
  protected Color breakpointMarkerColor;
  protected Color currentLineMarkerColor;
  protected List<LineHighlight> breakpointedLines =
    new ArrayList<LineHighlight>();
  protected LineHighlight currentLine; // where the debugger is suspended
  protected final String breakpointMarkerComment = " //<>//";

  protected JMenu debugMenu;
//  JCheckBoxMenuItem enableDebug;
//  boolean debugEnabled;
//  JMenuItem enableDebug;

  protected JMenuItem debugItem;
  protected Debugger debugger;
  protected boolean debugEnabled;

  protected VariableInspector inspector;
  protected JMenuItem inspectorItem;

//  private EditorToolbar javaToolbar;
//  private DebugToolbar debugToolbar;

  private ErrorColumn errorBar;

//  protected XQConsoleToggle btnShowConsole;
//  protected XQConsoleToggle btnShowErrors;
  protected JScrollPane errorTableScrollPane;
//  protected JPanel consoleProblemsPane;
  protected XQErrorTable errorTable;
  static final int ERROR_TAB_INDEX = 0;

  private boolean hasJavaTabs;
  private boolean javaTabWarned;

  protected ErrorCheckerService errorCheckerService;


  protected JavaEditor(Base base, String path, EditorState state, Mode mode) {
    super(base, path, state, mode);

    jmode = (JavaMode) mode;
    debugger = new Debugger(this);
    inspector = new VariableInspector(this);

    // Add show usage option
    JMenuItem showUsageItem = new JMenuItem("Show Usage...");
    showUsageItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        handleShowUsage();
      }
    });
    getTextArea().getRightClickPopup().add(showUsageItem);

    // add refactor option
    JMenuItem renameItem = new JMenuItem("Rename...");
    renameItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        handleRefactor();
      }
    });

    // TODO: Add support for word select on right click and rename.
    //        ta.customPainter.addMouseListener(new MouseAdapter() {
    //          public void mouseClicked(MouseEvent evt) {
    //            System.out.println(evt);
    //          }
    //        });
    textarea.getRightClickPopup().add(renameItem);
    // set action on frame close
    //        addWindowListener(new WindowAdapter() {
    //            @Override
    //            public void windowClosing(WindowEvent e) {
    //                onWindowClosing(e);
    //            }
    //        });

    Toolkit.setMenuMnemonics(textarea.getRightClickPopup());

    // load settings from theme.txt
    breakpointColor = mode.getColor("breakpoint.bgcolor");
    breakpointMarkerColor = mode.getColor("breakpoint.marker.color");
    currentLineColor = mode.getColor("currentline.bgcolor");
    currentLineMarkerColor = mode.getColor("currentline.marker.color");

    // set breakpoints from marker comments
    for (LineID lineID : stripBreakpointComments()) {
      //System.out.println("setting: " + lineID);
      debugger.setBreakpoint(lineID);
    }
    // setting breakpoints will flag sketch as modified, so override this here
    getSketch().setModified(false);

    hasJavaTabs = checkForJavaTabs();
    //initializeErrorChecker();

    errorCheckerService = new ErrorCheckerService(this);
    new Thread(errorCheckerService).start();

    // hack to add a JPanel to the right-hand side of the text area
    JPanel textAndError = new JPanel();
    // parent is a vertical box with the toolbar, the header, and the text area
    Box box = (Box) textarea.getParent();
    // remove the text area temporarily
    box.remove(2);
    textAndError.setLayout(new BorderLayout());
    errorBar =  new ErrorColumn(this, textarea.getMinimumSize().height, jmode);
    textAndError.add(errorBar, BorderLayout.EAST);
    textarea.setBounds(0, 0, errorBar.getX() - 1, textarea.getHeight());
    textAndError.add(textarea);
    // add our hacked version back to the editor
    box.add(textAndError);

    getJavaTextArea().setMode(jmode);

    // ensure completion is hidden when editor loses focus
    addWindowFocusListener(new WindowFocusListener() {
      public void windowLostFocus(WindowEvent e) {
        getJavaTextArea().hideSuggestion();
      }

      public void windowGainedFocus(WindowEvent e) { }
    });
  }


  protected JEditTextArea createTextArea() {
    return new JavaTextArea(new PdeTextAreaDefaults(mode), this);
  }


  public EditorToolbar createToolbar() {
    return new JavaToolbar(this);
  }


  // Override the parent call to add hook to the rebuild() method
  public EditorHeader createHeader() {
    return new EditorHeader(this) {
      public void rebuild() {
        super.rebuild();

        // after Rename and New Tab, we may have new .java tabs
        hasJavaTabs = checkForJavaTabs();
      }
    };
  }


  @Override
  public EditorFooter createFooter() {
//    //JPanel consolePanel = new JPanel();
//    JTabbedPane footer = new JTabbedPane(JTabbedPane.BOTTOM);
////    tabPane.setUI(new BasicTabbedPaneUI());
//    footer.setUI(new SimpleTabbedPaneUI());
////    tabPane.setUI(new SillyTabbedPaneUI());
////    tabPane.setUI(new PlasticTabbedPaneUI());
////    tabPane.setBorder(BorderFactory.createEmptyBorder());
////    tabPane.setBackground(Color.RED);

//    EditorFooter footer = new EditorFooter(this);
    EditorFooter footer = super.createFooter();

    // Adding Error Table in a scroll pane
    errorTableScrollPane = new JScrollPane();
    errorTable = new XQErrorTable(this);
    // errorTableScrollPane.setBorder(new EmptyBorder(2, 2, 2, 2));
//    errorTableScrollPane.setBorder(new EtchedBorder());
    errorTableScrollPane.setBorder(BorderFactory.createEmptyBorder());
    errorTableScrollPane.setViewportView(errorTable);

//    // Adding toggle console button
////    consolePanel.remove(2);  // removes the line status
//    JPanel lineStatusPanel = new JPanel();
//    lineStatusPanel.setLayout(new BorderLayout());
//    btnShowConsole = new XQConsoleToggle(this, Language.text("editor.footer.console"), mode.getInteger("linestatus.height"));
//    btnShowErrors = new XQConsoleToggle(this, Language.text("editor.footer.errors"), mode.getInteger("linestatus.height"));
//    btnShowConsole.addMouseListener(btnShowConsole);
//    btnShowErrors.addMouseListener(btnShowErrors);

//    JPanel toggleButtonPanel = new JPanel(new BorderLayout());
//    toggleButtonPanel.add(btnShowConsole, BorderLayout.EAST);
//    toggleButtonPanel.add(btnShowErrors, BorderLayout.WEST);
//    lineStatusPanel.add(toggleButtonPanel, BorderLayout.EAST);
////    lineStatus.setBounds(0, 0, toggleButtonPanel.getX() - 1,
////                         toggleButtonPanel.getHeight());
////    lineStatusPanel.add(lineStatus);
//    consolePanel.add(lineStatusPanel, BorderLayout.SOUTH);
//    lineStatusPanel.repaint();

//    // Adding JPanel with CardLayout for Console/Problems Toggle
////    consolePanel.remove(1);  // removes the console itself
//    consoleProblemsPane = new JPanel(new CardLayout());
//    consoleProblemsPane.add(errorTableScrollPane, Language.text("editor.footer.errors"));
//    consoleProblemsPane.add(super.createConsolePanel(), Language.text("editor.footer.console"));
//    consolePanel.add(consoleProblemsPane, BorderLayout.CENTER);

//    console = new EditorConsole(this);
//    footer.addPanel(Language.text("editor.footer.console"), console);
    footer.addPanel(Language.text("editor.footer.errors"), errorTableScrollPane);

    //return consolePanel;
    return footer;
  }


  public Formatter createFormatter() {
    return new AutoFormat();
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public JMenu buildFileMenu() {
    //String appTitle = JavaToolbar.getTitle(JavaToolbar.EXPORT, false);
    String appTitle = Language.text("menu.file.export_application");
    JMenuItem exportApplication = Toolkit.newJMenuItemShift(appTitle, 'E');
    exportApplication.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        handleExportApplication();
      }
    });

    return buildFileMenu(new JMenuItem[] { exportApplication });
  }


  public JMenu buildSketchMenu() {
    JMenuItem runItem = Toolkit.newJMenuItem(Language.text("menu.sketch.run"), 'R');
    runItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        handleRun();
      }
    });

    JMenuItem presentItem = Toolkit.newJMenuItemShift(Language.text("menu.sketch.present"), 'R');
    presentItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        handlePresent();
      }
    });

    JMenuItem stopItem = new JMenuItem(Language.text("menu.sketch.stop"));
    stopItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (isDebuggerEnabled()) {
          Logger.getLogger(JavaEditor.class.getName()).log(Level.INFO, "Invoked 'Stop' menu item");
          debugger.stopDebug();
        } else {
          handleStop();
        }
      }
    });

    JMenuItem tweakItem = Toolkit.newJMenuItemShift(Language.text("menu.sketch.tweak"), 'T');
      tweakItem.setSelected(JavaMode.enableTweak);
      tweakItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          JavaMode.enableTweak = true;
          handleRun();
        }
      });

    return buildSketchMenu(new JMenuItem[] {
      runItem, presentItem, tweakItem, stopItem
    });
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

        List<ToolContribution> contribTools = getToolContribs();
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
  private boolean addToolReferencesToSubMenu(List<ToolContribution> toolsList, JMenu subMenu) {
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
    showStopButton.setEnabled(Preferences.getBoolean("export.application.present"));
    showStopButton.setBorder(new EmptyBorder(3, 13 + indent, 6, 13));

    final JCheckBox presentButton = new JCheckBox(Language.text("export.options.present"));
    presentButton.setSelected(Preferences.getBoolean("export.application.present"));
    presentButton.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        boolean sal = presentButton.isSelected();
        Preferences.setBoolean("export.application.present", sal);
        showStopButton.setEnabled(sal);
      }
    });
    presentButton.setBorder(new EmptyBorder(3, 13, 3, 13));

    JPanel presentPanel = new JPanel();
    presentPanel.setLayout(new BoxLayout(presentPanel, BoxLayout.Y_AXIS));
    Box fullScreenBox = Box.createHorizontalBox();
    fullScreenBox.add(presentButton);

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
      "Users on other platforms will need to " +
      "<a href=\"\">install Java " + PApplet.javaPlatform + "</a>.";
    final String nopeWarning =
      "<html><div width=\"" + divWidth + "\"><font size=\"2\">" +
//      "<html><body><font size=2>" +
      "Users on all platforms will have to install the latest " +
      "version of Java " + PApplet.javaPlatform +
      " from <a href=\"\">http://java.com/download</a>. " +
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
    if (isDebuggerEnabled()) {
      // Don't start the sketch paused, continue until a breakpoint or error
      // https://github.com/processing/processing/issues/3096
      debugger.continueDebug();

    } else {
      new Thread(new Runnable() {
        public void run() {
          prepareRun();
          try {
            toolbar.activateRun();
            runtime = jmode.handleRun(sketch, JavaEditor.this);
          } catch (Exception e) {
            statusError(e);
          }
        }
      }).start();
    }
  }


  public void handlePresent() {
    new Thread(new Runnable() {
      public void run() {
        prepareRun();
        try {
//          toolbar.activate(JavaToolbar.RUN);
          toolbar.activateRun();
          runtime = jmode.handlePresent(sketch, JavaEditor.this);
        } catch (Exception e) {
          statusError(e);
        }
      }
    }).start();
  }


  /**
   * Event handler called when hitting the stop button. Stops a running debug
   * session or performs standard stop action if not currently debugging.
   */
  public void handleStop() {
    if (debugger.isStarted()) {
      debugger.stopDebug();

    } else {
//      toolbar.activate(JavaToolbar.STOP);
      toolbar.activateStop();

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

//      toolbar.deactivate(JavaToolbar.RUN);
//      toolbar.deactivate(JavaToolbar.STOP);
      toolbar.deactivateStop();
      toolbar.deactivateRun();

      // focus the PDE again after quitting presentation mode [toxi 030903]
      toFront();
    }
  }


  public void handleStep(int modifiers) {
    if (modifiers == 0) {
      Logger.getLogger(getClass().getName()).log(Level.INFO, "Invoked 'Step Over' menu item");
      debugger.stepOver();

    } else if ((modifiers & KeyEvent.SHIFT_DOWN_MASK) != 0) {
      Logger.getLogger(getClass().getName()).log(Level.INFO, "Invoked 'Step Into' menu item");
      debugger.stepInto();

    } else if ((modifiers & KeyEvent.ALT_DOWN_MASK) != 0) {
      Logger.getLogger(getClass().getName()).log(Level.INFO, "Invoked 'Step Out' menu item");
      debugger.stepOut();
    }
  }


  public void handleContinue() {
    Logger.getLogger(JavaEditor.class.getName()).log(Level.INFO, "Invoked 'Continue' menu item");
    debugger.continueDebug();
  }


  /** Toggle a breakpoint on the current line. */
  public void toggleBreakpoint() {
    debugger.toggleBreakpoint(getCurrentLineID().lineIdx());
  }


  public void toggleBreakpoint(int lineIndex) {
    debugger.toggleBreakpoint(lineIndex);
  }



  public boolean handleSaveAs() {
    //System.out.println("handleSaveAs");
    String oldName = getSketch().getCode(0).getFileName();
    //System.out.println("old name: " + oldName);
    boolean saved = super.handleSaveAs();
    if (saved) {
      // re-set breakpoints in first tab (name has changed)
      List<LineBreakpoint> bps = debugger.getBreakpoints(oldName);
      debugger.clearBreakpoints(oldName);
      String newName = getSketch().getCode(0).getFileName();
      //System.out.println("new name: " + newName);
      for (LineBreakpoint bp : bps) {
        LineID line = new LineID(newName, bp.lineID().lineIdx());
        //System.out.println("setting: " + line);
        debugger.setBreakpoint(line);
      }
      // add breakpoint marker comments to source file
      for (SketchCode code : getSketch().getCode()) {
        addBreakpointComments(code.getFileName());
      }

      // set new name of variable inspector
      //inspector.setTitle(getSketch().getName());
    }
    return saved;
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
//    toolbar.deactivate(JavaToolbar.RUN);
    toolbar.deactivateRun();
  }


  public void internalCloseRunner() {
    // Added temporarily to dump error log. TODO: Remove this later [mk29]
    if (JavaMode.errorLogsEnabled) {
      writeErrorsToFile();
    }
    handleStop();
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  // Additions from PDE X, Debug Mode, Twerk Mode...


  /**
   * Used instead of the windowClosing event handler, since it's not called on
   * mode switch. Called when closing the editor window. Stops running debug
   * sessions and kills the variable inspector window.
   */
  @Override
  public void dispose() {
    //System.out.println("window dispose");
    // quit running debug session
    if (debugEnabled) {
      debugger.stopDebug();
    }
    if (inspector != null) {
      inspector.dispose();
    }
    errorCheckerService.stopThread();
    super.dispose();
  }


  /**
   * Writes all error messages to a csv file.
   * For analytics purposes only.
   */
  private void writeErrorsToFile() {
    if (errorCheckerService.tempErrorLog.size() == 0) return;

    try {
      System.out.println("Writing errors");
      StringBuilder sb = new StringBuilder();
      sb.append("Sketch: " + getSketch().getFolder() + ", "
                + new java.sql.Timestamp(new java.util.Date().getTime())
                + "\nComma in error msg is substituted with ^ symbol\nFor separating arguments in error args | symbol is used\n");
      sb.append("ERROR TYPE, ERROR ARGS, ERROR MSG\n");

      for (String errMsg : errorCheckerService.tempErrorLog.keySet()) {
        IProblem ip = errorCheckerService.tempErrorLog.get(errMsg);
        if (ip != null) {
          sb.append(ErrorMessageSimplifier.getIDName(ip.getID()));
          sb.append(',');
          sb.append("{");
          for (int i = 0; i < ip.getArguments().length; i++) {
            sb.append(ip.getArguments()[i]);
            if (i < ip.getArguments().length-1)
              sb.append("| ");
          }
          sb.append("}");
          sb.append(',');
          sb.append(ip.getMessage().replace(',', '^'));
          sb.append("\n");
        }
      }
      System.out.println(sb);
      File opFile = new File(getSketch().getFolder(), "ErrorLogs"
                             + File.separator + "ErrorLog_" + System.currentTimeMillis() + ".csv");
      PApplet.saveStream(opFile, new ByteArrayInputStream(sb.toString()
                                                          .getBytes(Charset.defaultCharset())));
    } catch (Exception e) {
      System.err.println("Failed to save log file for sketch " + getSketch().getName());
      e.printStackTrace();
    }
  }


  /*
    private AtomicBoolean debugToolbarEnabled;

    public boolean isDebugToolbarEnabled() {
      return debugToolbarEnabled != null && debugToolbarEnabled.get();
    }


    /// Toggles between java mode and debug mode toolbar
    protected void switchToolbars(){
      final EditorToolbar nextToolbar;
      if(debugToolbarEnabled.get()){
        // switch to java
        if(javaToolbar == null)
          javaToolbar = createToolbar();
        nextToolbar = javaToolbar;
        debugToolbarEnabled.set(false);
        Base.log("Switching to Java Mode Toolbar");
      }
      else{
        // switch to debug
        if(debugToolbar == null)
          debugToolbar = new DebugToolbar(this, getBase());
        nextToolbar = debugToolbar;
        debugToolbarEnabled.set(true);
        Base.log("Switching to Debugger Toolbar");
      }

      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          Box upper = (Box)splitPane.getComponent(0);
          upper.remove(0);
          upper.add(nextToolbar, 0);
          upper.validate();
          nextToolbar.repaint();
          toolbar = nextToolbar;
          // The toolbar responds to shift down/up events
          // in order to show the alt version of toolbar buttons.
          // With toolbar switch, KeyListener has to be changed as well
          for (KeyListener kl : textarea.getKeyListeners()) {
            if(kl instanceof EditorToolbar)
            {
              textarea.removeKeyListener(kl);
              textarea.addKeyListener(toolbar);
              break;
            }
          }
          textarea.repaint();
        }
      });
    }
  */

  /**
   * Creates the debug menu. Includes ActionListeners for the menu items.
   * Intended for adding to the menu bar.
   *
   * @return The debug menu
   */
  protected JMenu buildDebugMenu() {
    debugMenu = new JMenu(Language.text("menu.debug"));
    JMenuItem item;

    // "use the debugger" sounds too colloquial, and "enable" sounds too technical
//    enableDebug =
//      Toolkit.newJCheckBoxMenuItem(Language.text("menu.debug.enable"),
//                                   KeyEvent.VK_D);
//    enableDebug =
//      Toolkit.newJCheckBoxMenuItem(Language.text("menu.debug.enable"),
//                                   KeyEvent.VK_D);
    debugItem = Toolkit.newJMenuItem(Language.text("menu.debug.enable"), 'D');
//    enableDebug.setSelected(false);
    debugItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
//        updateDebugToggle();
        toggleDebug();
      }
    });
//    toggleDebugger.addChangeListener(new ChangeListener() {
//      public void stateChanged(ChangeEvent e) {
//      }
//    });
    debugMenu.add(debugItem);
    debugMenu.addSeparator();

//    item = Toolkit.newJMenuItemAlt(Language.text("menu.debug.debug"), KeyEvent.VK_R);
//    item.addActionListener(new ActionListener() {
//        public void actionPerformed(ActionEvent e) {
//          Logger.getLogger(JavaEditor.class.getName()).log(Level.INFO, "Invoked 'Debug' menu item");
//          debugger.startDebug();
//        }
//      });
//    debugMenu.add(item);

    item = Toolkit.newJMenuItem(Language.text("menu.debug.continue"), KeyEvent.VK_U);
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleContinue();
        }
      });
    debugMenu.add(item);
    item.setEnabled(false);

//    item = new JMenuItem(Language.text("menu.debug.stop"));
//    item.addActionListener(new ActionListener() {
//        public void actionPerformed(ActionEvent e) {
//          Logger.getLogger(JavaEditor.class.getName()).log(Level.INFO, "Invoked 'Stop' menu item");
//          debugger.stopDebug();
//        }
//      });
//    debugMenu.add(item);

    item = Toolkit.newJMenuItem(Language.text("menu.debug.step"), KeyEvent.VK_J);
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        handleStep(0);
      }
    });
    debugMenu.add(item);
    item.setEnabled(false);

    item = Toolkit.newJMenuItemShift(Language.text("menu.debug.step_into"), KeyEvent.VK_J);
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleStep(KeyEvent.SHIFT_DOWN_MASK);
        }
      });
    debugMenu.add(item);
    item.setEnabled(false);

    item = Toolkit.newJMenuItemAlt(Language.text("menu.debug.step_out"), KeyEvent.VK_J);
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleStep(KeyEvent.ALT_DOWN_MASK);
        }
      });
    debugMenu.add(item);
    item.setEnabled(false);

    debugMenu.addSeparator();

    item =
      Toolkit.newJMenuItem(Language.text("menu.debug.toggle_breakpoint"), 'B');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Logger.getLogger(JavaEditor.class.getName()).log(Level.INFO, "Invoked 'Toggle Breakpoint' menu item");
          toggleBreakpoint();
        }
      });
    debugMenu.add(item);
    item.setEnabled(false);

    /*
    inspectorItem = Toolkit.newJMenuItem(Language.text("menu.debug.show_variables"), 'Y');
    inspectorItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          toggleVariableInspector();
        }
      });
    debugMenu.add(inspectorItem);
    inspectorItem.setEnabled(false);
    */

    /*
    item = new JMenuItem(Language.text("menu.debug.list_breakpoints"));
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Logger.getLogger(JavaEditor.class.getName()).log(Level.INFO, "Invoked 'List Breakpoints' menu item");
          debugger.listBreakpoints();
        }
      });
    debugMenu.add(item);

    debugMenu.addSeparator();
     */

    /*
    item = new JMenuItem(Language.text("menu.debug.print_stack_trace"));
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Logger.getLogger(JavaEditor.class.getName()).log(Level.INFO, "Invoked 'Print Stack Trace' menu item");
          debugger.printStackTrace();
        }
      });
    debugMenu.add(item);

    item = new JMenuItem(Language.text("menu.debug.print_locals"));
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Logger.getLogger(JavaEditor.class.getName()).log(Level.INFO, "Invoked 'Print Locals' menu item");
          debugger.printLocals();
        }
      });
    debugMenu.add(item);

    item = new JMenuItem(Language.text("menu.debug.print_fields"));
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Logger.getLogger(JavaEditor.class.getName()).log(Level.INFO, "Invoked 'Print This' menu item");
          debugger.printThis();
        }
      });
    debugMenu.add(item);

    item = new JMenuItem(Language.text("menu.debug.print_source_location"));
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Logger.getLogger(JavaEditor.class.getName()).log(Level.INFO, "Invoked 'Print Source' menu item");
          debugger.printSource();
        }
      });
    debugMenu.add(item);

    item = new JMenuItem(Language.text("menu.debug.print_threads"));
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Logger.getLogger(JavaEditor.class.getName()).log(Level.INFO, "Invoked 'Print Threads' menu item");
          debugger.printThreads();
        }
      });
    debugMenu.add(item);

    debugMenu.addSeparator();
    */

//    item = Toolkit.newJMenuItem(Language.text("menu.debug.toggle_variable_inspector"), KeyEvent.VK_I);
//    item.addActionListener(new ActionListener() {
//        public void actionPerformed(ActionEvent e) {
//          Logger.getLogger(JavaEditor.class.getName()).log(Level.INFO, "Invoked 'Toggle Variable Inspector' menu item");
//          toggleVariableInspector();
//        }
//      });
//    debugMenu.add(item);

    /*
    item = Toolkit.newJMenuItem(Language.text("menu.debug.show_sketch_outline"), KeyEvent.VK_L);
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Base.log("Show Sketch Outline:");
          errorCheckerService.getASTGenerator().showSketchOutline();
        }
      });
    debugMenu.add(item);

    item = Toolkit.newJMenuItem(Language.text("menu.debug.show_tabs_list"), KeyEvent.VK_Y);
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Base.log("Show Tab Outline:");
          errorCheckerService.getASTGenerator().showTabOutline();
        }
      });
    debugMenu.add(item);
    */

    return debugMenu;
  }


  protected boolean isDebuggerEnabled() {
    //return enableDebug.isSelected();
    return debugEnabled;
  }


  @Override
  public JMenu buildModeMenu() {
    return buildDebugMenu();
  }


  /**
   * Event handler called when loading another sketch in this editor. Clears
   * breakpoints of previous sketch.
   *
   * @param path
   * @return true if a sketch was opened, false if aborted
   */
  @Override
  protected boolean handleOpenInternal(String path) {
    // log("handleOpenInternal, path: " + path);
    boolean didOpen = super.handleOpenInternal(path);
    if (didOpen && debugger != null) {
      // should already been stopped (open calls handleStop)
      debugger.clearBreakpoints();
      clearBreakpointedLines(); // force clear breakpoint highlights
      variableInspector().reset(); // clear contents of variable inspector
    }
    //if(didOpen){
    // autosaver = new AutoSaveUtil(this, ExperimentalMode.autoSaveInterval); // this is used instead of loadAutosaver(), temp measure
    // loadAutoSaver();
    // viewingAutosaveBackup = autosaver.isAutoSaveBackup();
    // log("handleOpenInternal, viewing autosave? " + viewingAutosaveBackup);
    //}
    return didOpen;
  }


  /**
   * Extract breakpointed lines from source code marker comments. This removes
   * marker comments from the editor text. Intended to be called on loading a
   * sketch, since re-setting the sketches contents after removing the markers
   * will clear all breakpoints.
   *
   * @return the list of {@link LineID}s where breakpoint marker comments were
   * removed from.
   */
  protected List<LineID> stripBreakpointComments() {
    List<LineID> bps = new ArrayList<LineID>();
    // iterate over all tabs
    Sketch sketch = getSketch();
    for (int i = 0; i < sketch.getCodeCount(); i++) {
      SketchCode tab = sketch.getCode(i);
      String code = tab.getProgram();
      String lines[] = code.split("\\r?\\n"); // newlines not included
      //System.out.println(code);

      // scan code for breakpoint comments
      int lineIdx = 0;
      for (String line : lines) {
        //System.out.println(line);
        if (line.endsWith(breakpointMarkerComment)) {
          LineID lineID = new LineID(tab.getFileName(), lineIdx);
          bps.add(lineID);
          //System.out.println("found breakpoint: " + lineID);
          // got a breakpoint
          //dbg.setBreakpoint(lineID);
          int index = line.lastIndexOf(breakpointMarkerComment);
          lines[lineIdx] = line.substring(0, index);
        }
        lineIdx++;
      }
      //tab.setProgram(code);
      code = PApplet.join(lines, "\n");
      setTabContents(tab.getFileName(), code);
    }
    return bps;
  }


  /**
   * Add breakpoint marker comments to the source file of a specific tab. This
   * acts on the source file on disk, not the editor text. Intended to be
   * called just after saving the sketch.
   *
   * @param tabFilename the tab file name
   */
  protected void addBreakpointComments(String tabFilename) {
    SketchCode tab = getTab(tabFilename);
    if (tab == null) {
      // this method gets called twice when saving sketch for the first time
      // once with new name and another with old(causing NPE). Keep an eye out
      // for potential issues. See #2675. TODO:
      Base.loge("Illegal tab name to addBreakpointComments() " + tabFilename);
      return;
    }
    List<LineBreakpoint> bps = debugger.getBreakpoints(tab.getFileName());

    // load the source file
    ////switched to using methods provided by the SketchCode class
    // File sourceFile = new File(sketch.getFolder(), tab.getFileName());
    //System.out.println("file: " + sourceFile);
    try {
      tab.load();
      String code = tab.getProgram();
      //System.out.println("code: " + code);
      String lines[] = code.split("\\r?\\n"); // newlines not included
      for (LineBreakpoint bp : bps) {
        //System.out.println("adding bp: " + bp.lineID());
        lines[bp.lineID().lineIdx()] += breakpointMarkerComment;
      }
      code = PApplet.join(lines, "\n");
      //System.out.println("new code: " + code);
      tab.setProgram(code);
      tab.save();
    } catch (IOException ex) {
      Logger.getLogger(JavaEditor.class.getName()).log(Level.SEVERE, null, ex);
    }
  }


  @Override
  public boolean handleSave(boolean immediately) {
    //System.out.println("handleSave " + immediately);

    //log("handleSave, viewing autosave? " + viewingAutosaveBackup);
    /* If user wants to save a backup, the backup sketch should get
     * copied to the main sketch directory, simply reload the main sketch.
     */
    if(viewingAutosaveBackup){
          /*
          File files[] = autosaver.getSketchBackupFolder().listFiles();
          File src = autosaver.getSketchBackupFolder(), dst = autosaver
              .getActualSketchFolder();
          for (File f : files) {
            log("Copying " + f.getAbsolutePath() + " to " + dst.getAbsolutePath());
            try {
              if (f.isFile()) {
                f.delete();
                Base.copyFile(f, new File(dst + File.separator + f.getName()));
              } else {
                Base.removeDir(f);
                Base.copyDir(f, new File(dst + File.separator + f.getName()));
              }
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
          File sk = autosaver.getActualSketchFolder();
          Base.removeDir(autosaver.getAutoSaveDir());
          //handleOpenInternal(sk.getAbsolutePath() + File.separator + sk.getName() + ".pde");
          getBase().handleOpen(sk.getAbsolutePath() + File.separator + sk.getName() + ".pde");
          //viewingAutosaveBackup = false;
          */
    }

    // note modified tabs
    final List<String> modified = new ArrayList<String>();
    for (int i = 0; i < getSketch().getCodeCount(); i++) {
      SketchCode tab = getSketch().getCode(i);
      if (tab.isModified()) {
        modified.add(tab.getFileName());
      }
    }

    boolean saved = super.handleSave(immediately);
    if (saved) {
      if (immediately) {
        for (String tabFilename : modified) {
          addBreakpointComments(tabFilename);
        }
      } else {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
              for (String tabFilename : modified) {
                addBreakpointComments(tabFilename);
              }
            }
          });
      }
    }
    //  if file location has changed, update autosaver
    // autosaver.reloadAutosaveDir();
    return saved;
  }


  private boolean viewingAutosaveBackup;


  /**
   * Set text contents of a specific tab. Updates underlying document and text
   * area. Clears Breakpoints.
   *
   * @param tabFilename the tab file name
   * @param code the text to set
   */
  protected void setTabContents(String tabFilename, String code) {
    // remove all breakpoints of this tab
    debugger.clearBreakpoints(tabFilename);

    SketchCode currentTab = getCurrentTab();

    // set code of tab
    SketchCode tab = getTab(tabFilename);
    if (tab != null) {
      tab.setProgram(code);
      // this updates document and text area
      // TODO: does this have any negative effects? (setting the doc to null)
      tab.setDocument(null);
      setCode(tab);

      // switch back to original tab
      setCode(currentTab);
    }
  }


  public void clearConsole() {
    console.clear();
  }


  public void clearSelection() {
    setSelection(getCaretOffset(), getCaretOffset());
  }


  /**
   * Select a line in the current tab.
   * @param lineIdx 0-based line number
   */
  public void selectLine(int lineIdx) {
    setSelection(getLineStartOffset(lineIdx), getLineStopOffset(lineIdx));
  }


  /**
   * Set the cursor to the start of a line.
   * @param lineIdx 0-based line number
   */
  public void cursorToLineStart(int lineIdx) {
    setSelection(getLineStartOffset(lineIdx), getLineStartOffset(lineIdx));
  }


  /**
   * Set the cursor to the end of a line.
   * @param lineIdx 0-based line number
   */
  public void cursorToLineEnd(int lineIdx) {
    setSelection(getLineStopOffset(lineIdx), getLineStopOffset(lineIdx));
  }


  /**
   * Switch to a tab.
   * @param tabFileName the file name identifying the tab. (as in
   * {@link SketchCode#getFileName()})
   */
  public void switchToTab(String tabFileName) {
    Sketch s = getSketch();
    for (int i = 0; i < s.getCodeCount(); i++) {
      if (tabFileName.equals(s.getCode(i).getFileName())) {
        s.setCurrentCode(i);
        break;
      }
    }
  }


  public Debugger getDebugger() {
    return debugger;
  }


  /**
   * Access the custom text area object.
   * @return the text area object
   */
  public JavaTextArea getJavaTextArea() {
    return (JavaTextArea) textarea;
  }


  public ErrorCheckerService getErrorChecker() {
    return errorCheckerService;
  }


  /**
   * Grab current contents of the sketch window, advance the console, stop any
   * other running sketches, auto-save the user's code... not in that order.
   */
  @Override
  public void prepareRun() {
    autoSave();
    super.prepareRun();
    downloadImports();
  }


  /**
   * Downloads libraries that have been imported, that aren't available as a
   * LocalContribution, but that have an AvailableContribution associated with
   * them.
   */
  protected void downloadImports() {
    String importRegex = errorCheckerService.importRegexp;
    String tabCode;
    for (SketchCode sc : sketch.getCode()) {
      if (sc.isExtension("pde")) {
        tabCode = sc.getProgram();

        String[][] pieces = PApplet.matchAll(tabCode, importRegex);

        if (pieces != null) {
          ArrayList<String> importHeaders = new ArrayList<String>();
          for (String[] importStatement : pieces) {
            importHeaders.add(importStatement[2]);
          }
          List<AvailableContribution> installLibsHeaders = getNotInstalledAvailableLibs(importHeaders);
          if (!installLibsHeaders.isEmpty()) {
            StringBuilder libList = new StringBuilder("Would you like to install them now?");
            for (AvailableContribution ac : installLibsHeaders) {
              libList.append("\n  • " + ac.getName());
            }
            int option = Base.showYesNoQuestion(this,
                Language.text("contrib.import.dialog.title"),
                Language.text("contrib.import.dialog.primary_text"),
                libList.toString());

            if (option == JOptionPane.YES_OPTION) {
              ContributionManager.downloadAndInstallOnImport(base,
                  installLibsHeaders);
            }
          }
        }
      }
    }
  }


  /**
   * Returns a list of AvailableContributions of those libraries that the user
   * wants imported, but that are not installed.
   *
   * @param importHeaders
   */
  private List<AvailableContribution> getNotInstalledAvailableLibs(ArrayList<String> importHeadersList) {
    Map<String, Contribution> importMap = ContributionListing.getInstance().librariesByImportHeader;
    ArrayList<AvailableContribution> libList = new ArrayList<AvailableContribution>();
    for (String importHeaders : importHeadersList) {
      int dot = importHeaders.lastIndexOf('.');
      String entry = (dot == -1) ? importHeaders : importHeaders.substring(0,
          dot);

      if (entry.startsWith("java.") || entry.startsWith("javax.")
          || entry.startsWith("processing.")) {
        continue;// null;
      }

      Library library = null;
      try {
        library = this.getMode().getLibrary(entry);
        if (library == null) {
          Contribution c = importMap.get(importHeaders);
          if (c != null && c instanceof AvailableContribution) {
            libList.add((AvailableContribution) c);// System.out.println(importHeaders
                                                   // + "not found");
          }
        }
      } catch (Exception e) {
        // Not gonna happen (hopefully)
        Contribution c = importMap.get(importHeaders);
        if (c != null && c instanceof AvailableContribution) {
          libList.add((AvailableContribution) c);// System.out.println(importHeaders
                                                 // + "not found");
        }
      }
    }
    return libList;
  }


  /**
   * Displays a JDialog prompting the user to save when the user hits
   * run/present/etc.
   */
  protected void autoSave() {
    if (!JavaMode.autoSaveEnabled) {
      return;
    }

    try {
      if (sketch.isModified() && !sketch.isUntitled()) {
        if (JavaMode.autoSavePromptEnabled) {
          final JDialog autoSaveDialog =
            new JDialog(base.getActiveEditor(), getSketch().getName(), true);
          Container container = autoSaveDialog.getContentPane();

          JPanel panelMain = new JPanel();
          panelMain.setBorder(BorderFactory.createEmptyBorder(4, 0, 2, 2));
          panelMain.setLayout(new BoxLayout(panelMain, BoxLayout.PAGE_AXIS));

          JPanel panelLabel = new JPanel(new FlowLayout(FlowLayout.LEFT));
          JLabel label = new JLabel("<html><body>&nbsp;There are unsaved"
                                    + " changes in your sketch.<br />"
                                    + "&nbsp;&nbsp;&nbsp; Do you want to save it before"
                                    + " running? </body></html>");
          label.setFont(new Font(label.getFont().getName(),
                                 Font.PLAIN, label.getFont().getSize() + 1));
          panelLabel.add(label);
          panelMain.add(panelLabel);
          final JCheckBox dontRedisplay = new JCheckBox("Remember this decision");
          JPanel panelButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 2));

          JButton btnRunSave = new JButton("Save and Run");
          btnRunSave.addActionListener(new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                handleSave(true);
                if (dontRedisplay.isSelected()) {
                  JavaMode.autoSavePromptEnabled = !dontRedisplay.isSelected();
                  JavaMode.defaultAutoSaveEnabled = true;
                  jmode.savePreferences();
                }
                autoSaveDialog.dispose();
              }
            });
          panelButtons.add(btnRunSave);

          JButton btnRunNoSave = new JButton("Run, Don't Save");
          btnRunNoSave.addActionListener(new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                if (dontRedisplay.isSelected()) {
                  JavaMode.autoSavePromptEnabled = !dontRedisplay.isSelected();
                  JavaMode.defaultAutoSaveEnabled = false;
                  jmode.savePreferences();
                }
                autoSaveDialog.dispose();
              }
            });
          panelButtons.add(btnRunNoSave);
          panelMain.add(panelButtons);

          JPanel panelCheck = new JPanel();
          panelCheck.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
          panelCheck.add(dontRedisplay);
          panelMain.add(panelCheck);

          container.add(panelMain);

          autoSaveDialog.setResizable(false);
          autoSaveDialog.pack();
          autoSaveDialog.setLocationRelativeTo(base.getActiveEditor());
          autoSaveDialog.setVisible(true);

        } else if (JavaMode.defaultAutoSaveEnabled) {
          handleSave(true);
        }
      }
    } catch (Exception e) {
      statusError(e);
    }
  }


  /**
   * Access variable inspector window.
   * @return the variable inspector object
   */
  public VariableInspector variableInspector() {
    return inspector;
  }


  protected void activateRun() {
    debugItem.setEnabled(false);
//    toolbar.activate(JavaToolbar.RUN);
    toolbar.activateRun();
  }


  /**
   * Deactivate the Run button. This is called by Runner to notify that the
   * sketch has stopped running, usually in response to an error (or maybe
   * the sketch completing and exiting?) Tools should not call this function.
   * To initiate a "stop" action, call handleStop() instead.
   */
  public void deactivateRun() {
//    if (toolbar instanceof DebugToolbar){
//      toolbar.deactivate(DebugToolbar.RUN);
//    } else {
//    toolbar.deactivate(JavaToolbar.RUN);
    toolbar.deactivateRun();
    debugItem.setEnabled(true);
//    }
  }


  protected void activateDebug() {
    //debugToolbar.activate(DebugToolbar.DEBUG);
    activateRun();
  }


  protected void deactivateDebug() {
    deactivateRun();
  }


  protected void activateContinue() {
    ((JavaToolbar) toolbar).activateContinue();
  }


  protected void deactivateContinue() {
    ((JavaToolbar) toolbar).deactivateContinue();
  }


  protected void activateStep() {
    ((JavaToolbar) toolbar).activateStep();
  }


  protected void deactivateStep() {
    ((JavaToolbar) toolbar).deactivateStep();
  }


  public void toggleDebug() {
//    enableDebug.setSelected(!enableDebug.isSelected());
    debugEnabled = !debugEnabled;
//    updateDebugToggle();
//  }
//
//
//  public void updateDebugToggle() {
//    final boolean enabled = isDebuggerEnabled();
    rebuildToolbar();

    if (debugEnabled) {
      debugItem.setText(Language.text("menu.debug.disable"));
    } else {
      debugItem.setText(Language.text("menu.debug.enable"));
    }

//    // Hide the variable inspector if it's currently visible
//    if (!debugEnabled && inspector.isVisible()) {
//      toggleVariableInspector();
//    }
    inspector.setVisible(debugEnabled);

    /*
    if (enabled) {
      inspector.setFocusableWindowState(false); // to not get focus when set visible
      inspector.setVisible(true);
      inspector.setFocusableWindowState(true); // allow to get focus again
    } else {
      inspector.setVisible(false);
    }
    */
    for (Component item : debugMenu.getMenuComponents()) {
      if (item instanceof JMenuItem && item != debugItem) {
        ((JMenuItem) item).setEnabled(debugEnabled);
      }
    }
  }


  /*
  public void toggleVariableInspector() {
    if (inspector.isVisible()) {
      inspectorItem.setText(Language.text("menu.debug.show_variables"));
      inspector.setVisible(false);
    } else {
//      inspector.setFocusableWindowState(false); // to not get focus when set visible
      inspectorItem.setText(Language.text("menu.debug.show_variables"));
      inspector.setVisible(true);
//      inspector.setFocusableWindowState(true); // allow to get focus again
    }
  }
  */


//  public void showVariableInspector() {
//    tray.setVisible(true);
//  }


//  /**
//   * Set visibility of the variable inspector window.
//   * @param visible true to set the variable inspector visible,
//   * false for invisible.
//   */
//  public void showVariableInspector(boolean visible) {
//    tray.setVisible(visible);
//  }
//
//
//  public void hideVariableInspector() {
//    tray.setVisible(true);
//  }
//
//
//  /** Toggle visibility of the variable inspector window. */
//  public void toggleVariableInspector() {
//    tray.setFocusableWindowState(false); // to not get focus when set visible
//    tray.setVisible(!tray.isVisible());
//    tray.setFocusableWindowState(true); // allow to get focus again
//  }


  /**
   * Set the line to highlight as currently suspended at. Will override the
   * breakpoint color, if set. Switches to the appropriate tab and scroll to
   * the line by placing the cursor there.
   * @param line the line to highlight as current suspended line
   */
  public void setCurrentLine(LineID line) {
    clearCurrentLine();
    if (line == null) {
      // safety, e.g. when no line mapping is found and the null line is used.
      return;
    }
    switchToTab(line.fileName());
    // scroll to line, by setting the cursor
    cursorToLineStart(line.lineIdx());
    // highlight line
    currentLine = new LineHighlight(line.lineIdx(), currentLineColor, this);
    currentLine.setMarker(getJavaTextArea().currentLineMarker, currentLineMarkerColor);
    currentLine.setPriority(10); // fixes current line being hidden by the breakpoint when moved down
  }


  /** Clear the highlight for the debuggers current line. */
  public void clearCurrentLine() {
    if (currentLine != null) {
      currentLine.clear();
      currentLine.dispose();

      // revert to breakpoint color if any is set on this line
      for (LineHighlight hl : breakpointedLines) {
        if (hl.getLineID().equals(currentLine.getLineID())) {
          hl.paint();
          break;
        }
      }
      currentLine = null;
    }
  }


  /**
   * Add highlight for a breakpointed line.
   * @param lineID the line id to highlight as breakpointed
   */
  public void addBreakpointedLine(LineID lineID) {
    LineHighlight hl = new LineHighlight(lineID, breakpointColor, this);
    hl.setMarker(getJavaTextArea().breakpointMarker, breakpointMarkerColor);
    breakpointedLines.add(hl);
    // repaint current line if it's on this line
    if (currentLine != null && currentLine.getLineID().equals(lineID)) {
      currentLine.paint();
    }
  }


  /**
   * Add highlight for a breakpointed line on the current tab.
   * @param lineIdx the line index on the current tab to highlight as
   * breakpointed
   */
  //TODO: remove and replace by {@link #addBreakpointedLine(LineID lineID)}
  public void addBreakpointedLine(int lineIdx) {
    addBreakpointedLine(getLineIDInCurrentTab(lineIdx));
  }


  /**
   * Remove a highlight for a breakpointed line. Needs to be on the current tab.
   * @param lineIdx the line index on the current tab to remove a breakpoint
   * highlight from
   */
  public void removeBreakpointedLine(int lineIdx) {
    LineID line = getLineIDInCurrentTab(lineIdx);
    //System.out.println("line id: " + line.fileName() + " " + line.lineIdx());
    LineHighlight foundLine = null;
    for (LineHighlight hl : breakpointedLines) {
      if (hl.getLineID().equals(line)) {
        foundLine = hl;
        break;
      }
    }
    if (foundLine != null) {
      foundLine.clear();
      breakpointedLines.remove(foundLine);
      foundLine.dispose();
      // repaint current line if it's on this line
      if (currentLine != null && currentLine.getLineID().equals(line)) {
        currentLine.paint();
      }
    }
  }


  /** Remove all highlights for breakpointed lines. */
  public void clearBreakpointedLines() {
    for (LineHighlight hl : breakpointedLines) {
      hl.clear();
      hl.dispose();
    }
    breakpointedLines.clear(); // remove all breakpoints
    // fix highlights not being removed when tab names have
    // changed due to opening a new sketch in same editor
    getJavaTextArea().clearLineBgColors(); // force clear all highlights
    getJavaTextArea().clearGutterText();

    // repaint current line
    if (currentLine != null) {
      currentLine.paint();
    }
  }


  /**
   * Retrieve a {@link LineID} object for a line on the current tab.
   * @param lineIdx the line index on the current tab
   * @return the {@link LineID} object representing a line index on the
   * current tab
   */
  public LineID getLineIDInCurrentTab(int lineIdx) {
    return new LineID(getSketch().getCurrentCode().getFileName(), lineIdx);
  }


  /**
   * Retrieve line of sketch where the cursor currently resides.
   * @return the current {@link LineID}
   */
  protected LineID getCurrentLineID() {
    String tab = getSketch().getCurrentCode().getFileName();
    int lineNo = getTextArea().getCaretLine();
    return new LineID(tab, lineNo);
  }


  /**
   * Check whether a {@link LineID} is on the current tab.
   * @param line the {@link LineID}
   * @return true, if the {@link LineID} is on the current tab.
   */
  public boolean isInCurrentTab(LineID line) {
    return line.fileName().equals(getSketch().getCurrentCode().getFileName());
  }


  /**
   * Event handler called when switching between tabs. Loads all line
   * background colors set for the tab.
   * @param code tab to switch to
   */
  @Override
  protected void setCode(SketchCode code) {
    //System.out.println("tab switch: " + code.getFileName());
    // set the new document in the textarea, etc. need to do this first
    super.setCode(code);

    // set line background colors for tab
    final JavaTextArea ta = getJavaTextArea();
    // can be null when setCode is called the first time (in constructor)
    if (ta != null) {
      // clear all line backgrounds
      ta.clearLineBgColors();
      // clear all gutter text
      ta.clearGutterText();
      // load appropriate line backgrounds for tab
      // first paint breakpoints
      if (breakpointedLines != null) {
        for (LineHighlight hl : breakpointedLines) {
          if (isInCurrentTab(hl.getLineID())) {
            hl.paint();
          }
        }
      }
      // now paint current line (if any)
      if (currentLine != null) {
        if (isInCurrentTab(currentLine.getLineID())) {
          currentLine.paint();
        }
      }
    }
    if (getDebugger() != null && getDebugger().isStarted()) {
      getDebugger().startTrackingLineChanges();
    }
  }


  /**
   * Get a tab by its file name.
   * @param fileName the filename to search for.
   * @return the {@link SketchCode} object representing the tab, or null if
   * not found
   */
  public SketchCode getTab(String fileName) {
    Sketch s = getSketch();
    for (SketchCode c : s.getCode()) {
      if (c.getFileName().equals(fileName)) {
        return c;
      }
    }
    return null;
  }


  /**
   * Retrieve the current tab.
   * @return the {@link SketchCode} representing the current tab
   */
  public SketchCode getCurrentTab() {
    return getSketch().getCurrentCode();
  }


  /**
   * Access the currently edited document.
   * @return the document object
   */
  public Document currentDocument() {
    //return ta.getDocument();
    return getCurrentTab().getDocument();
  }


//  /**
//   * Event Handler for double clicking in the left hand gutter area.
//   * @param lineIdx the line (0-based) that was double clicked
//   */
//  public void gutterDblClicked(int lineIdx) {
//    if (debugger != null) {
//      debugger.toggleBreakpoint(lineIdx);
//    }
//  }


  public void statusBusy() {
    statusNotice(Language.text("editor.status.debug.busy"));
  }


  public void statusHalted() {
    statusNotice(Language.text("editor.status.debug.halt"));
  }


  public static final int STATUS_EMPTY = 100, STATUS_COMPILER_ERR = 200,
    STATUS_WARNING = 300, STATUS_INFO = 400, STATUS_ERR = 500;
  public int statusMessageType = STATUS_EMPTY;
  public String statusMessage;
  public void statusMessage(final String what, int type){
    // Don't re-display the old message again
    if (type != STATUS_EMPTY) {
      if (what.equals(statusMessage) && type == statusMessageType) {
        return;
      }
    }
    statusMessage = new String(what);
    statusMessageType = type;
    switch (type) {
    case STATUS_COMPILER_ERR:
    case STATUS_ERR:
      super.statusError(what);
      break;
    case STATUS_INFO:
    case STATUS_WARNING:
      statusNotice(what);
      break;
    }
    // Don't need to clear compiler error messages
    if (type == STATUS_COMPILER_ERR) return;

    // Clear the message after a delay
    SwingWorker<Object, Object> s = new SwingWorker<Object, Object>() {
      @Override
      protected Object doInBackground() throws Exception {
        try {
          Thread.sleep(2 * 1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        statusEmpty();
        return null;
      }
    };
    s.execute();
  }


  public void statusEmpty(){
    statusMessage = null;
    statusMessageType = STATUS_EMPTY;
    super.statusEmpty();
  }


//  /**
//   * Initializes and starts Error Checker Service
//   */
//  private void initializeErrorChecker() {
//    Thread errorCheckerThread = null;
//
//    if (errorCheckerThread == null) {
//      errorCheckerService = new ErrorCheckerService(this);
//      errorCheckerThread = new Thread(errorCheckerService);
//      try {
//        errorCheckerThread.start();
//      } catch (Exception e) {
//        Base.loge("Error Checker Service not initialized", e);
//      }
//    }
//  }


  public void updateErrorBar(List<Problem> problems) {
    errorBar.updateErrorPoints(problems);
  }


  public List<ErrorMarker> getErrorPoints() {
    return errorBar.errorPoints;
  }


  public void repaintErrorBar() {
    errorBar.repaint();
  }


  public void showConsole() {
    footer.setPanel(console);
  }


//  /** Toggle between Console and Errors List */
//  public void showProblemListView(String buttonName) {
////    CardLayout cl = (CardLayout) consoleProblemsPane.getLayout();
////    cl.show(consoleProblemsPane, buttonName);
////    ((JTabbedPane) consolePanel).setSelectedIndex(ERROR_TAB_INDEX);
//    footer.setPanel(errorTableScrollPane);
//  }


  /** Updates the error table */
  synchronized public boolean updateTable(final TableModel tableModel) {
    return errorTable.updateTable(tableModel);
  }


  /**
   * Handle whether the tiny red error indicator is shown near
   * the error button at the bottom of the PDE
   */
  public void updateErrorToggle() {
    footer.setNotification(errorTableScrollPane,
                           JavaMode.errorCheckEnabled &&
                           errorCheckerService.hasErrors());
//    String title = Language.text("editor.footer.errors");
//    if (JavaMode.errorCheckEnabled && errorCheckerService.hasErrors()) {
//      title += "*";
//    }
//    ((JTabbedPane) footer).setTitleAt(ERROR_TAB_INDEX, title);
////    btnShowErrors.updateMarker(JavaMode.errorCheckEnabled &&
////                               errorCheckerService.hasErrors(),
////                               errorBar.errorColor);
  }


  /** Handle refactor operation */
  private void handleRefactor() {
    Base.log("Caret at:" + textarea.getLineText(textarea.getCaretLine()));
    errorCheckerService.getASTGenerator().handleRefactor();
  }


  /** Handle show usage operation */
  private void handleShowUsage() {
    Base.log("Caret at:" + textarea.getLineText(textarea.getCaretLine()));
    errorCheckerService.getASTGenerator().handleShowUsage();
  }


  public boolean hasJavaTabs() {
    return hasJavaTabs;
  }


  /**
   * Checks if the sketch contains java tabs. If it does, the editor ain't
   * built for it, yet. Also, user should really start looking at a full IDE
   * like Eclipse. Disable compilation check and some more features.
   */
  private boolean checkForJavaTabs() {
    for (SketchCode code : getSketch().getCode()) {
      if (code.getExtension().equals("java")) {
        if (!javaTabWarned) {
          System.out.println(getSketch().getName() + " contains .java tabs. ");
          System.out.println("Some editor features (like completion " +
                             "and error checking) will be disabled.");
          //Base.showWarning("Cannot debug advanced sketches", msg);
          javaTabWarned = true;
        }
        return true;
      }
    }
    return false;
  }


  protected void applyPreferences() {
    super.applyPreferences();
    if (jmode != null) {
      jmode.loadPreferences();
      Base.log("Applying prefs");
      // trigger it once to refresh UI
      errorCheckerService.runManualErrorCheck();
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  // TWEAK MODE

  static final String prefTweakPort = "tweak.port";
  static final String prefTweakShowCode = "tweak.showcode";

  public String[] baseCode;
  TweakClient tweakClient;


  protected void startInteractiveMode() {
    getJavaTextArea().startInteractiveMode();
  }


  //public void stopInteractiveMode(ArrayList<Handle> handles[]) {
  protected void stopInteractiveMode(List<List<Handle>> handles) {
    tweakClient.shutdown();
    getJavaTextArea().stopInteractiveMode();

    // remove space from the code (before and after)
    //removeSpacesFromCode();

    // check which tabs were modified
    boolean modified = false;
    boolean[] modifiedTabs = getModifiedTabs(handles);
    for (boolean mod : modifiedTabs) {
      if (mod) {
        modified = true;
        break;
      }
    }

    if (modified) {
      // ask to keep the values
      int ret =
        Base.showYesNoQuestion(this, Language.text("tweak_mode"),
                               Language.text("tweak_mode.keep_changes.line1"),
                               Language.text("tweak_mode.keep_changes.line2"));
      if (ret == 1) {
        // NO! don't keep changes
        loadSavedCode();
        // update the painter to draw the saved (old) code
        textarea.invalidate();

      } else {
        // YES! keep changes
        // the new values are already present, just make sure the user can save the modified tabs
        for (int i=0; i<sketch.getCodeCount(); i++) {
          if (modifiedTabs[i]) {
            sketch.getCode(i).setModified(true);
          }
          else {
            // load the saved code of tabs that didn't change
            // (there might be formatting changes that should not be saved)
            sketch.getCode(i).setProgram(sketch.getCode(i).getSavedProgram());
            /* Wild Hack: set document to null so the text editor will refresh
               the program contents when the document tab is being clicked */
            sketch.getCode(i).setDocument(null);

            if (i == sketch.getCurrentCodeIndex()) {
              // this will update the current code
              setCode(sketch.getCurrentCode());
            }
          }
        }

        // save the sketch
        try {
          sketch.save();
        } catch (IOException e) {
          Base.showWarning("Error", "Could not save the modified sketch.", e);
        }

        // repaint the editor header (show the modified tabs)
        header.repaint();
        textarea.invalidate();
      }
    } else {
      // number values were not modified but we need to load the saved code
      // because of some formatting changes
      loadSavedCode();
      textarea.invalidate();
    }
  }


  protected void updateInterface(List<List<Handle>> handles,
                              List<List<ColorControlBox>> colorBoxes) {
    getJavaTextArea().updateInterface(handles, colorBoxes);
  }


  static private boolean[] getModifiedTabs(List<List<Handle>> handles) {
    boolean[] modifiedTabs = new boolean[handles.size()];

    for (int i = 0; i < handles.size(); i++) {
      for (Handle h : handles.get(i)) {
        if (h.valueChanged()) {
          modifiedTabs[i] = true;
        }
      }
    }
    return modifiedTabs;
  }


  protected void initBaseCode() {
    SketchCode[] code = sketch.getCode();

    baseCode = new String[code.length];
    for (int i = 0; i < code.length; i++) {
      baseCode[i] = new String(code[i].getSavedProgram());
    }
  }


  protected void initEditorCode(List<List<Handle>> handles, boolean withSpaces) {
    SketchCode[] sketchCode = sketch.getCode();
    for (int tab=0; tab<baseCode.length; tab++) {
        // beautify the numbers
        int charInc = 0;
        String code = baseCode[tab];

        for (Handle n : handles.get(tab)) {
          int s = n.startChar + charInc;
          int e = n.endChar + charInc;
          String newStr = n.strNewValue;
          if (withSpaces) {
            newStr = "  " + newStr + "  ";
          }
          code = replaceString(code, s, e, newStr);
          n.newStartChar = n.startChar + charInc;
          charInc += n.strNewValue.length() - n.strValue.length();
          if (withSpaces) {
            charInc += 4;
          }
          n.newEndChar = n.endChar + charInc;
        }

        sketchCode[tab].setProgram(code);
        /* Wild Hack: set document to null so the text editor will refresh
           the program contents when the document tab is being clicked */
        sketchCode[tab].setDocument(null);
      }

    // this will update the current code
    setCode(sketch.getCurrentCode());
  }


  private void loadSavedCode() {
    SketchCode[] code = sketch.getCode();
    for (int i=0; i<code.length; i++) {
      if (!code[i].getProgram().equals(code[i].getSavedProgram())) {
        code[i].setProgram(code[i].getSavedProgram());
        /* Wild Hack: set document to null so the text editor will refresh
           the program contents when the document tab is being clicked */
        code[i].setDocument(null);
      }
    }
    // this will update the current code
    setCode(sketch.getCurrentCode());
  }


  /*
  private void removeSpacesFromCode() {
    SketchCode[] code = sketch.getCode();
    for (int i=0; i<code.length; i++) {
      String c = code[i].getProgram();
      //c = c.substring(SPACE_AMOUNT, c.length() - SPACE_AMOUNT);
      code[i].setProgram(c);
      // TODO Wild Hack: set document to null so the text editor will refresh
      // the program contents when the document tab is being clicked
      code[i].setDocument(null);
    }
    // this will update the current code
    setCode(sketch.getCurrentCode());
  }
  */


  /**
   * Replace all numbers with variables and add code to initialize
   * these variables and handle update messages.
   */
  protected boolean automateSketch(Sketch sketch, SketchParser parser) {
    SketchCode[] code = sketch.getCode();

    List<List<Handle>> handles = parser.allHandles;

    if (code.length < 1) {
      return false;
    }

    if (handles.size() == 0) {
      return false;
    }

    int setupEndPos = SketchParser.getSetupEnd(baseCode[0]);
    if (setupEndPos < 0) {
      return false;
    }

    // get port number from preferences.txt
    int port;
    String portStr = Preferences.get(prefTweakPort);
    if (portStr == null) {
      Preferences.set(prefTweakPort, "auto");
      portStr = "auto";
    }

    if (portStr.equals("auto")) {
      // random port for udp (0xc000 - 0xffff)
      port = (int)(Math.random()*0x3fff) + 0xc000;
    } else {
      port = Preferences.getInteger(prefTweakPort);
    }

    // create the client that will send the new values to the sketch
    tweakClient = new TweakClient(port);
    // update handles with a reference to the client object
    for (int tab=0; tab<code.length; tab++) {
      for (Handle h : handles.get(tab)) {
        h.setTweakClient(tweakClient);
      }
    }


    // Copy current program to interactive program
    // modify the code below, replace all numbers with their variable names
    // loop through all tabs in the current sketch
    for (int tab=0; tab<code.length; tab++) {
      int charInc = 0;
      String c = baseCode[tab];
      for (Handle n : handles.get(tab)) {
        // replace number value with a variable
        c = replaceString(c, n.startChar + charInc, n.endChar + charInc, n.name);
        charInc += n.name.length() - n.strValue.length();
      }
      code[tab].setProgram(c);
    }

    // add the main header to the code in the first tab
    String c = code[0].getProgram();

    // header contains variable declaration, initialization,
    // and OSC listener function
    String header;
    header = "\n\n" +
      "/*************************/\n" +
      "/* MODIFIED BY TWEAKMODE */\n" +
      "/*************************/\n" +
      "\n\n";

    // add needed OSC imports and the global OSC object
    header += "import java.net.*;\n";
    header += "import java.io.*;\n";
    header += "import java.nio.*;\n\n";

    // write a declaration for int and float arrays
    int numOfInts = howManyInts(handles);
    int numOfFloats = howManyFloats(handles);
    if (numOfInts > 0) {
      header += "int[] tweakmode_int = new int["+numOfInts+"];\n";
    }
    if (numOfFloats > 0) {
      header += "float[] tweakmode_float = new float["+numOfFloats+"];\n\n";
    }

    // add the server code that will receive the value change messages
    header += TweakClient.getServerCode(port, numOfInts>0, numOfFloats>0);
    header += "TweakModeServer tweakmode_Server;\n";

    header += "void tweakmode_initAllVars() {\n";
    //for (int i=0; i<handles.length; i++) {
    for (List<Handle> list : handles) {
      //for (Handle n : handles[i]) {
      for (Handle n : list) {
        header += "  " + n.name + " = " + n.strValue + ";\n";
      }
    }
    header += "}\n\n";
    header += "void tweakmode_initCommunication() {\n";
    header += " tweakmode_Server = new TweakModeServer();\n";
    header += " tweakmode_Server.setup();\n";
    header += " tweakmode_Server.start();\n";
    header += "}\n";

    header += "\n\n\n\n\n";

    // add call to our initAllVars and initOSC functions
    // from the setup() function.
    String addToSetup = "\n"+
      " tweakmode_initAllVars();\n"+
      " tweakmode_initCommunication();\n\n";

    setupEndPos = SketchParser.getSetupEnd(c);
    c = replaceString(c, setupEndPos, setupEndPos, addToSetup);

    code[0].setProgram(header + c);

    // print out modified code
    String showModCode = Preferences.get(prefTweakShowCode);
    if (showModCode == null) {
      Preferences.setBoolean(prefTweakShowCode, false);
    }

    if (Preferences.getBoolean(prefTweakShowCode)) {
      System.out.println("\nTweakMode modified code:\n");
      for (int i=0; i<code.length; i++) {
        System.out.println("tab " + i + "\n");
        System.out.println("=======================================================\n");
        System.out.println(code[i].getProgram());
      }
    }
    return true;
  }


  static private String replaceString(String str, int start, int end, String put) {
    return str.substring(0, start) + put + str.substring(end, str.length());
  }


  //private int howManyInts(ArrayList<Handle> handles[])
  static private int howManyInts(List<List<Handle>> handles) {
    int count = 0;
    //for (int i=0; i<handles.length; i++) {
    for (List<Handle> list : handles) {
      //for (Handle n : handles[i]) {
      for (Handle n : list) {
        if (n.type == "int" || n.type == "hex" || n.type == "webcolor") {
          count++;
        }
      }
    }
    return count;
  }


  //private int howManyFloats(ArrayList<Handle> handles[])
  static private int howManyFloats(List<List<Handle>> handles) {
    int count = 0;
    //for (int i=0; i<handles.length; i++) {
    for (List<Handle> list : handles) {
      //for (Handle n : handles[i]) {
      for (Handle n : list) {
        if (n.type == "float") {
          count++;
        }
      }
    }
    return count;
  }
}

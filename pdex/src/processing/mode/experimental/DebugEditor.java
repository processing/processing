/*
 * Copyright (C) 2012-14 Martin Leopold <m@martinleopold.com> and Manindra Moharana <me@mkmoharana.com>
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package processing.mode.experimental;
import static processing.mode.experimental.ExperimentalMode.log;
import static processing.mode.experimental.ExperimentalMode.logE;
import galsasson.mode.tweak.ColorControlBox;
import galsasson.mode.tweak.Handle;
import galsasson.mode.tweak.SketchParser;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EtchedBorder;
import javax.swing.table.TableModel;
import javax.swing.text.Document;

import org.eclipse.jdt.core.compiler.IProblem;

import processing.app.Base;
import processing.app.EditorState;
import processing.app.EditorToolbar;
import processing.app.Mode;
import processing.app.Sketch;
import processing.app.SketchCode;
import processing.app.Toolkit;
import processing.app.syntax.JEditTextArea;
import processing.app.syntax.PdeTextAreaDefaults;
import processing.core.PApplet;
import processing.mode.java.JavaEditor;

/**
 * Main View Class. Handles the editor window including tool bar and menu. Has
 * access to the Sketch. Provides line highlighting (for breakpoints and the
 * debuggers current line).
 *
 * @author Martin Leopold <m@martinleopold.com>
 * @author Manindra Moharana &lt;me@mkmoharana.com&gt;
 * 
 * 
 */
public class DebugEditor extends JavaEditor implements ActionListener {
    // important fields from superclass
    //protected Sketch sketch;
    //private JMenu fileMenu;
    //protected EditorToolbar toolbar;

    // highlighting
    protected Color breakpointColor = new Color(240, 240, 240); // the background color for highlighting lines
    protected Color currentLineColor = new Color(255, 255, 150); // the background color for highlighting lines
    protected Color breakpointMarkerColor = new Color(74, 84, 94); // the color of breakpoint gutter markers
    protected Color currentLineMarkerColor = new Color(226, 117, 0); // the color of current line gutter markers
    protected List<LineHighlight> breakpointedLines = new ArrayList<LineHighlight>(); // breakpointed lines
    protected LineHighlight currentLine; // line the debugger is currently suspended at
    protected final String breakpointMarkerComment = " //<>//"; // breakpoint marker comment
    // menus
    protected JMenu debugMenu; // the debug menu
    // debugger control
    protected JMenuItem debugMenuItem;
    protected JMenuItem continueMenuItem;
    protected JMenuItem stopMenuItem;
    // breakpoints
    protected JMenuItem toggleBreakpointMenuItem;
    protected JMenuItem listBreakpointsMenuItem;
    // stepping
    protected JMenuItem stepOverMenuItem;
    protected JMenuItem stepIntoMenuItem;
    protected JMenuItem stepOutMenuItem;
    // info
    protected JMenuItem printStackTraceMenuItem;
    protected JMenuItem printLocalsMenuItem;
    protected JMenuItem printThisMenuItem;
    protected JMenuItem printSourceMenuItem;
    protected JMenuItem printThreads;
    // variable inspector
    protected JMenuItem toggleVariableInspectorMenuItem;
    // references
    protected ExperimentalMode dmode; // the mode
    protected Debugger dbg; // the debugger
    protected VariableInspector vi; // the variable inspector frame
    protected TextArea ta; // the text area

    
    protected ErrorBar errorBar;
    /**
     * Show Console button
     */
    protected XQConsoleToggle btnShowConsole;

    /**
     * Show Problems button
     */
    protected XQConsoleToggle btnShowErrors;

    /**
     * Scroll pane for Error Table
     */
    protected JScrollPane errorTableScrollPane;

    /**
     * Panel with card layout which contains the p5 console and Error Table
     * panes
     */
    protected JPanel consoleProblemsPane;
    
    protected XQErrorTable errorTable;
    
    /**
     * Enable/Disable compilation checking
     */
    protected boolean compilationCheckEnabled = true;
    
    /**
     * Show warnings menu item
     */
    protected JCheckBoxMenuItem showWarnings;
    
    /**
     * Check box menu item for show/hide Problem Window
     */
    public JCheckBoxMenuItem problemWindowMenuCB;
    
    /**
     * Enable/Disable debug ouput
     */
    protected JCheckBoxMenuItem debugMessagesEnabled;
    
    /**
     * Show outline view
     */
    protected JMenuItem showOutline, showTabOutline;
    
    /**
     * Enable/Disable error logging
     */
    protected JCheckBoxMenuItem writeErrorLog;
    
    /**
     * Enable/Disable code completion
     */
    protected JCheckBoxMenuItem completionsEnabled;
    
    /**
     * If sketch contains java tabs, some editor features are disabled
     */
    protected boolean hasJavaTabs;
    
    /**
     * UNUSED. Disbaled for now.
     */
    protected AutoSaveUtil autosaver;
    
    public DebugEditor(Base base, String path, EditorState state, Mode mode) {
        super(base, path, state, mode);

        // get mode
        dmode = (ExperimentalMode) mode;

        // init controller class
        dbg = new Debugger(this);

        // variable inspector window
        vi = new VariableInspector(this);

        // access to customized (i.e. subclassed) text area
        ta = (TextArea) textarea;
        
        // Add show usage option
        JMenuItem showUsageItem = new JMenuItem("Show Usage..");
        showUsageItem.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            handleShowUsage();
          }
        });
        ta.getRightClickPopup().add(showUsageItem);
        
        // add refactor option
        JMenuItem renameItem = new JMenuItem("Rename..");
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
        ta.getRightClickPopup().add(renameItem);
        // set action on frame close
//        addWindowListener(new WindowAdapter() {
//            @Override
//            public void windowClosing(WindowEvent e) {
//                onWindowClosing(e);
//            }
//        });

        // load settings from theme.txt
        ExperimentalMode theme = dmode;
        breakpointColor = theme.getThemeColor("breakpoint.bgcolor", breakpointColor);
        breakpointMarkerColor = theme.getThemeColor("breakpoint.marker.color", breakpointMarkerColor);
        currentLineColor = theme.getThemeColor("currentline.bgcolor", currentLineColor);
        currentLineMarkerColor = theme.getThemeColor("currentline.marker.color", currentLineMarkerColor);

        // set breakpoints from marker comments
        for (LineID lineID : stripBreakpointComments()) {
            //System.out.println("setting: " + lineID);
            dbg.setBreakpoint(lineID);
        }
        getSketch().setModified(false); // setting breakpoints will flag sketch as modified, so override this here
        
        checkForJavaTabs();
        initializeErrorChecker();
        ta.setECSandThemeforTextArea(errorCheckerService, dmode);
        addXQModeUI();    
        debugToolbarEnabled = new AtomicBoolean(false);
        //log("Sketch Path: " + path);

        // TweakMode code

        // random port for OSC (0xff0 - 0xfff0)
		oscPort = (int)(Math.random()*0xf000) + 0xff0;
    }
    
    private void addXQModeUI(){
      
      // Adding ErrorBar
      JPanel textAndError = new JPanel();
      Box box = (Box) textarea.getParent();
      box.remove(2); // Remove textArea from it's container, i.e Box
      textAndError.setLayout(new BorderLayout());
      errorBar =  new ErrorBar(this, textarea.getMinimumSize().height, dmode);
      textAndError.add(errorBar, BorderLayout.EAST);
      textarea.setBounds(0, 0, errorBar.getX() - 1, textarea.getHeight());
      textAndError.add(textarea);
      box.add(textAndError);
      
      // Adding Error Table in a scroll pane
      errorTableScrollPane = new JScrollPane();
      errorTable = new XQErrorTable(errorCheckerService);
      // errorTableScrollPane.setBorder(new EmptyBorder(2, 2, 2, 2));
      errorTableScrollPane.setBorder(new EtchedBorder());
      errorTableScrollPane.setViewportView(errorTable);

      // Adding toggle console button
      consolePanel.remove(2);
      JPanel lineStatusPanel = new JPanel();
      lineStatusPanel.setLayout(new BorderLayout());
      btnShowConsole = new XQConsoleToggle(this,
          XQConsoleToggle.CONSOLE, lineStatus.getHeight());
      btnShowErrors = new XQConsoleToggle(this,
          XQConsoleToggle.ERRORSLIST, lineStatus.getHeight());
      btnShowConsole.addMouseListener(btnShowConsole);

      // lineStatusPanel.add(btnShowConsole, BorderLayout.EAST);
      // lineStatusPanel.add(btnShowErrors);
      btnShowErrors.addMouseListener(btnShowErrors);

      JPanel toggleButtonPanel = new JPanel(new BorderLayout());
      toggleButtonPanel.add(btnShowConsole, BorderLayout.EAST);
      toggleButtonPanel.add(btnShowErrors, BorderLayout.WEST);
      lineStatusPanel.add(toggleButtonPanel, BorderLayout.EAST);
      lineStatus.setBounds(0, 0, toggleButtonPanel.getX() - 1,
          toggleButtonPanel.getHeight());
      lineStatusPanel.add(lineStatus);
      consolePanel.add(lineStatusPanel, BorderLayout.SOUTH);
      lineStatusPanel.repaint();

      // Adding JPanel with CardLayout for Console/Problems Toggle
      consolePanel.remove(1);
      consoleProblemsPane = new JPanel(new CardLayout());
      consoleProblemsPane.add(errorTableScrollPane, XQConsoleToggle.ERRORSLIST);
      consoleProblemsPane.add(console, XQConsoleToggle.CONSOLE);
      consolePanel.add(consoleProblemsPane, BorderLayout.CENTER);
      
      // ensure completion gets hidden on editor losing focus
      addWindowFocusListener(new WindowFocusListener() {        
        public void windowLostFocus(WindowEvent e) {
         ta.hideSuggestion();
        }        
        public void windowGainedFocus(WindowEvent e) {
          
        }
      });
    }

//    /**
//     * Event handler called when closing the editor window. Kills the variable
//     * inspector window.
//     *
//     * @param e the event object
//     */
//    protected void onWindowClosing(WindowEvent e) {
//        // remove var.inspector
//        vi.dispose();
//        // quit running debug session
//        dbg.stopDebug();
//    }
    /**
     * Used instead of the windowClosing event handler, since it's not called on
     * mode switch. Called when closing the editor window. Stops running debug
     * sessions and kills the variable inspector window.
     */
    @Override
    public void dispose() {
        //System.out.println("window dispose");
        // quit running debug session
        dbg.stopDebug();        
        // remove var.inspector
        vi.dispose();
        errorCheckerService.stopThread();
        // original dispose
        super.dispose();
    }
    
    // Added temporarily to dump error log. TODO: Remove this later
    public void internalCloseRunner(){      
      if(ExperimentalMode.errorLogsEnabled) writeErrorsToFile();
//      if(autosaver != null && !viewingAutosaveBackup) {
//        log("stopping autosaver in internalCloseRunner");
//        autosaver.stop();
//      }
      super.internalCloseRunner();
    }
    
    /**
     * Writes all error messages to a csv file.
     * For analytics purposes only.
     */
    private void writeErrorsToFile(){
    if (errorCheckerService.tempErrorLog.size() == 0)
      return;
      try {
        System.out.println("Writing errors");
        StringBuffer sbuff = new StringBuffer();
      sbuff.append("Sketch: " + getSketch().getFolder() + ", "
          + new java.sql.Timestamp(new java.util.Date().getTime())
              + "\nComma in error msg is substituted with ^ symbol\nFor separating arguments in error args | symbol is used\n");
      sbuff.append("ERROR TYPE, ERROR ARGS, ERROR MSG\n");
        for (String errMsg : errorCheckerService.tempErrorLog.keySet()) {
          IProblem ip = errorCheckerService.tempErrorLog.get(errMsg);
          if(ip != null){
            sbuff.append(ErrorMessageSimplifier.getIDName(ip.getID()));
            sbuff.append(',');
            sbuff.append("{");
            for (int i = 0; i < ip.getArguments().length; i++) {
              sbuff.append(ip.getArguments()[i]);
              if(i < ip.getArguments().length - 1)
                sbuff.append("| ");
            }
            sbuff.append("}");
            sbuff.append(',');
            sbuff.append(ip.getMessage().replace(',', '^'));
            sbuff.append("\n");
          }
        }
        System.out.println(sbuff);
        File opFile = new File(getSketch().getFolder(), "ErrorLogs"
          + File.separator + "ErrorLog_" + System.currentTimeMillis() + ".csv");
        PApplet.saveStream(opFile, new ByteArrayInputStream(sbuff.toString()
          .getBytes(Charset.defaultCharset())));
      } catch (Exception e) {
        System.err.println("Failed to save log file for sketch " + getSketch().getName());
        e.printStackTrace();
      }
    }

    /**
     * Overrides sketch menu creation to change keyboard shortcuts from "Run".
     *
     * @return the sketch menu
     */
    /*@Override
    public JMenu buildSketchMenu() {
        JMenuItem runItem = Toolkit.newJMenuItemShift(DebugToolbar.getTitle(DebugToolbar.RUN, false), KeyEvent.VK_R);
        runItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleRun();
            }
        });

        JMenuItem presentItem = new JMenuItem(DebugToolbar.getTitle(DebugToolbar.RUN, true));
        presentItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handlePresent();
            }
        });

        JMenuItem stopItem = new JMenuItem(DebugToolbar.getTitle(DebugToolbar.STOP, false));
        stopItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleStop();
            }
        });
        return buildSketchMenu(new JMenuItem[]{runItem, presentItem, stopItem});
    }*/
    
    /**
     * Whether debug toolbar is enabled
     */
    AtomicBoolean debugToolbarEnabled;
    
    protected EditorToolbar javaToolbar, debugToolbar;
    
    /**
     * Toggles between java mode and debug mode toolbar
     */
    protected void switchToolbars(){
      final EditorToolbar nextToolbar;
      if(debugToolbarEnabled.get()){
        // switch to java
        if(javaToolbar == null)
          javaToolbar = createToolbar();
        nextToolbar = javaToolbar;
        debugToolbarEnabled.set(false);
        log("Switching to Java Mode Toolbar");
      }
      else{
        // switch to debug
        if(debugToolbar == null)
          debugToolbar = new DebugToolbar(this, getBase());
        nextToolbar = debugToolbar;
        debugToolbarEnabled.set(true);
        log("Switching to Debugger Toolbar");
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
          ta.repaint();
        }
      });
    }

    /**
     * Creates the debug menu. Includes ActionListeners for the menu items.
     * Intended for adding to the menu bar.
     *
     * @return The debug menu
     */
    protected JMenu buildDebugMenu() {
        debugMenu = new JMenu("Debug");
        //debugMenu = new JMenu("PDE X");

        JCheckBoxMenuItem toggleDebugger = new JCheckBoxMenuItem("Show Debug Toolbar");
        toggleDebugger.setSelected(false);
        toggleDebugger.addActionListener(new ActionListener() {          
          public void actionPerformed(ActionEvent e) {
            switchToolbars();
          }
        });
        debugMenu.add(toggleDebugger);
        debugMenuItem = Toolkit.newJMenuItemAlt("Debug", KeyEvent.VK_R);
        debugMenuItem.addActionListener(this);
        continueMenuItem = Toolkit.newJMenuItem("Continue", KeyEvent.VK_U);
        continueMenuItem.addActionListener(this);
        stopMenuItem = new JMenuItem("Stop");
        stopMenuItem.addActionListener(this);

        toggleBreakpointMenuItem = Toolkit.newJMenuItem("Toggle Breakpoint", KeyEvent.VK_B);
        toggleBreakpointMenuItem.addActionListener(this);
        listBreakpointsMenuItem = new JMenuItem("List Breakpoints");
        listBreakpointsMenuItem.addActionListener(this);

        stepOverMenuItem = Toolkit.newJMenuItem("Step", KeyEvent.VK_H);
        stepOverMenuItem.addActionListener(this);
        stepIntoMenuItem = Toolkit.newJMenuItemShift("Step Into", KeyEvent.VK_H);
        stepIntoMenuItem.addActionListener(this);
        stepOutMenuItem = Toolkit.newJMenuItemAlt("Step Out", KeyEvent.VK_H);
        stepOutMenuItem.addActionListener(this);

        printStackTraceMenuItem = new JMenuItem("Print Stack Trace");
        printStackTraceMenuItem.addActionListener(this);
        printLocalsMenuItem = new JMenuItem("Print Locals");
        printLocalsMenuItem.addActionListener(this);
        printThisMenuItem = new JMenuItem("Print Fields");
        printThisMenuItem.addActionListener(this);
        printSourceMenuItem = new JMenuItem("Print Source Location");
        printSourceMenuItem.addActionListener(this);
        printThreads = new JMenuItem("Print Threads");
        printThreads.addActionListener(this);

        toggleVariableInspectorMenuItem = Toolkit.newJMenuItem("Toggle Variable Inspector", KeyEvent.VK_I);
        toggleVariableInspectorMenuItem.addActionListener(this);

        debugMenu.add(debugMenuItem);
        debugMenu.add(continueMenuItem);
        debugMenu.add(stopMenuItem);
        debugMenu.addSeparator();
        debugMenu.add(toggleBreakpointMenuItem);
        debugMenu.add(listBreakpointsMenuItem);
        debugMenu.addSeparator();
        debugMenu.add(stepOverMenuItem);
        debugMenu.add(stepIntoMenuItem);
        debugMenu.add(stepOutMenuItem);
        debugMenu.addSeparator();
        debugMenu.add(printStackTraceMenuItem);
        debugMenu.add(printLocalsMenuItem);
        debugMenu.add(printThisMenuItem);
        debugMenu.add(printSourceMenuItem);
        debugMenu.add(printThreads);
        debugMenu.addSeparator();
        debugMenu.add(toggleVariableInspectorMenuItem);
        // debugMenu.addSeparator();

        // XQMode menu items
        /*        
        JCheckBoxMenuItem item;
        item = new JCheckBoxMenuItem("Error Checker Enabled");
        item.setSelected(ExperimentalMode.errorCheckEnabled);
        item.addActionListener(new ActionListener() {

          @Override
          public void actionPerformed(ActionEvent e) {
            ExperimentalMode.errorCheckEnabled = ((JCheckBoxMenuItem) e.getSource()).isSelected();
            errorCheckerService.handleErrorCheckingToggle();
            dmode.savePreferences();
          }
        });
        debugMenu.add(item);

        problemWindowMenuCB = new JCheckBoxMenuItem("Show Problem Window");
        // problemWindowMenuCB.setSelected(true);
        problemWindowMenuCB.addActionListener(new ActionListener() {

          @Override
          public void actionPerformed(ActionEvent e) {
            if (errorCheckerService.errorWindow == null) {
              return;
            }
            errorCheckerService.errorWindow
                .setVisible(((JCheckBoxMenuItem) e.getSource())
                    .isSelected());
            // switch to console, now that Error Window is open
            showProblemListView(XQConsoleToggle.CONSOLE);
          }
        });
        debugMenu.add(problemWindowMenuCB);

        showWarnings = new JCheckBoxMenuItem("Warnings Enabled");
        showWarnings.setSelected(ExperimentalMode.warningsEnabled);
        showWarnings.addActionListener(new ActionListener() {

          @Override
          public void actionPerformed(ActionEvent e) {
            ExperimentalMode.warningsEnabled = ((JCheckBoxMenuItem) e
                .getSource()).isSelected();
            errorCheckerService.runManualErrorCheck();
            dmode.savePreferences();
          }
        });
        debugMenu.add(showWarnings);
        
        completionsEnabled = new JCheckBoxMenuItem("Code Completion Enabled");
        completionsEnabled.setSelected(ExperimentalMode.codeCompletionsEnabled);
        completionsEnabled.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
              ExperimentalMode.codeCompletionsEnabled = (((JCheckBoxMenuItem) e
                  .getSource()).isSelected());
              dmode.savePreferences();
          }
        });
        debugMenu.add(completionsEnabled);
        
        debugMessagesEnabled = new JCheckBoxMenuItem("Show Debug Messages");
        debugMessagesEnabled.setSelected(ExperimentalMode.DEBUG);
        debugMessagesEnabled.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            ExperimentalMode.DEBUG = ((JCheckBoxMenuItem) e
                .getSource()).isSelected();
            dmode.savePreferences();
          }
        });
        debugMenu.add(debugMessagesEnabled);     
        
        
        writeErrorLog = new JCheckBoxMenuItem("Write Errors to Log");
        writeErrorLog.setSelected(ExperimentalMode.errorLogsEnabled);
        writeErrorLog.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            ExperimentalMode.errorLogsEnabled = ((JCheckBoxMenuItem) e
                .getSource()).isSelected();
            dmode.savePreferences();
          }
        });
        debugMenu.add(writeErrorLog);
        
        debugMenu.addSeparator();
        JMenuItem jitem = new JMenuItem("PDE X on GitHub");
        jitem.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Base.openURL("https://github.com/processing/processing-experimental");
          }
        });
        debugMenu.add(jitem);
        */
        showOutline = Toolkit.newJMenuItem("Show Sketch Outline", KeyEvent.VK_L);
        showOutline.addActionListener(this);
        debugMenu.add(showOutline);
        
        showTabOutline = Toolkit.newJMenuItem("Show Tabs List", KeyEvent.VK_Y);
        showTabOutline.addActionListener(this);
        debugMenu.add(showTabOutline);
        
        
        return debugMenu;
    }
    
    @Override
    public JMenu buildModeMenu() {
        return buildDebugMenu();
    }
    
    public JMenu buildSketchMenu() {
      JMenuItem runItem = Toolkit.newJMenuItem(DebugToolbar
          .getTitle(DebugToolbar.RUN, false), 'R');
      runItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleRun();
        }
      });
  
      JMenuItem presentItem = Toolkit.newJMenuItemShift(DebugToolbar
          .getTitle(DebugToolbar.RUN, true), 'R');
      presentItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handlePresent();
        }
      });
  
      JMenuItem stopItem = new JMenuItem(DebugToolbar.getTitle(DebugToolbar.STOP,
                                                               false));
      stopItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleStop();
        }
      });
  
      JMenuItem enableTweak = Toolkit.newJMenuItemShift("Tweak", 'T');
      enableTweak.setSelected(ExperimentalMode.enableTweak);
      enableTweak.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          ExperimentalMode.enableTweak = true;
          handleRun();
        }
      });
  
      return buildSketchMenu(new JMenuItem[] {
        runItem, presentItem, enableTweak, stopItem });
    }

    /**
     * Callback for menu items. Implementation of Swing ActionListener.
     *
     * @param ae Action event
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        //System.out.println("ActionEvent: " + ae.toString());

        JMenuItem source = (JMenuItem) ae.getSource();
        if (source == debugMenuItem) {
            Logger.getLogger(DebugEditor.class.getName()).log(Level.INFO, "Invoked 'Debug' menu item");
            //dmode.handleDebug(sketch, this);
            dbg.startDebug();
        } else if (source == stopMenuItem) {
            Logger.getLogger(DebugEditor.class.getName()).log(Level.INFO, "Invoked 'Stop' menu item");
            //dmode.handleDebug(sketch, this);
            dbg.stopDebug();
        } else if (source == continueMenuItem) {
            Logger.getLogger(DebugEditor.class.getName()).log(Level.INFO, "Invoked 'Continue' menu item");
            //dmode.handleDebug(sketch, this);
            dbg.continueDebug();
        } else if (source == stepOverMenuItem) {
            Logger.getLogger(DebugEditor.class.getName()).log(Level.INFO, "Invoked 'Step Over' menu item");
            dbg.stepOver();
        } else if (source == stepIntoMenuItem) {
            Logger.getLogger(DebugEditor.class.getName()).log(Level.INFO, "Invoked 'Step Into' menu item");
            dbg.stepInto();
        } else if (source == stepOutMenuItem) {
            Logger.getLogger(DebugEditor.class.getName()).log(Level.INFO, "Invoked 'Step Out' menu item");
            dbg.stepOut();
        } else if (source == printStackTraceMenuItem) {
            Logger.getLogger(DebugEditor.class.getName()).log(Level.INFO, "Invoked 'Print Stack Trace' menu item");
            dbg.printStackTrace();
        } else if (source == printLocalsMenuItem) {
            Logger.getLogger(DebugEditor.class.getName()).log(Level.INFO, "Invoked 'Print Locals' menu item");
            dbg.printLocals();
        } else if (source == printThisMenuItem) {
            Logger.getLogger(DebugEditor.class.getName()).log(Level.INFO, "Invoked 'Print This' menu item");
            dbg.printThis();
        } else if (source == printSourceMenuItem) {
            Logger.getLogger(DebugEditor.class.getName()).log(Level.INFO, "Invoked 'Print Source' menu item");
            dbg.printSource();
        } else if (source == printThreads) {
            Logger.getLogger(DebugEditor.class.getName()).log(Level.INFO, "Invoked 'Print Threads' menu item");
            dbg.printThreads();
        } else if (source == toggleBreakpointMenuItem) {
            Logger.getLogger(DebugEditor.class.getName()).log(Level.INFO, "Invoked 'Toggle Breakpoint' menu item");
            dbg.toggleBreakpoint();
        } else if (source == listBreakpointsMenuItem) {
            Logger.getLogger(DebugEditor.class.getName()).log(Level.INFO, "Invoked 'List Breakpoints' menu item");
            dbg.listBreakpoints();
        } else if (source == toggleVariableInspectorMenuItem) {
            Logger.getLogger(DebugEditor.class.getName()).log(Level.INFO, "Invoked 'Toggle Variable Inspector' menu item");
            toggleVariableInspector();
        } else if (source.equals(showOutline)){
            log("Show Sketch Outline:");
            errorCheckerService.getASTGenerator().showSketchOutline();
        }
        else if (source.equals(showTabOutline)){
          log("Show Tab Outline:");
          errorCheckerService.getASTGenerator().showTabOutline();
      }
    }

//    @Override
//    public void handleRun() {
//        dbg.continueDebug();
//    }
    /**
     * Event handler called when hitting the stop button. Stops a running debug
     * session or performs standard stop action if not currently debugging.
     */
    @Override
    public void handleStop() {
        if (dbg.isStarted()) {
            dbg.stopDebug();
        } else {
            super.handleStop();
        }
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
        if (didOpen && dbg != null) {
            // should already been stopped (open calls handleStop)
            dbg.clearBreakpoints();
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
        if(tab == null) {
          // this method gets called twice when saving sketch for the first time
          // once with new name and another with old(causing NPE). Keep an eye out 
          // for potential issues. See #2675. TODO:
          logE("Illegal tab name to addBreakpointComments() " + tabFilename);          
          return;
        }
        List<LineBreakpoint> bps = dbg.getBreakpoints(tab.getFileName());

        // load the source file
        File sourceFile = new File(sketch.getFolder(), tab.getFileName());
        //System.out.println("file: " + sourceFile);
        try {
            String code = Base.loadFile(sourceFile);
            //System.out.println("code: " + code);
            String lines[] = code.split("\\r?\\n"); // newlines not included
            for (LineBreakpoint bp : bps) {
                //System.out.println("adding bp: " + bp.lineID());
                lines[bp.lineID().lineIdx()] += breakpointMarkerComment;
            }
            code = PApplet.join(lines, "\n");
            //System.out.println("new code: " + code);
            Base.saveFile(code, sourceFile);
        } catch (IOException ex) {
            Logger.getLogger(DebugEditor.class.getName()).log(Level.SEVERE, null, ex);
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

    @Override
    public boolean handleSaveAs() {
        //System.out.println("handleSaveAs");
        String oldName = getSketch().getCode(0).getFileName();
        //System.out.println("old name: " + oldName);
        boolean saved = super.handleSaveAs();
        if (saved) {
            // re-set breakpoints in first tab (name has changed)
            List<LineBreakpoint> bps = dbg.getBreakpoints(oldName);
            dbg.clearBreakpoints(oldName);
            String newName = getSketch().getCode(0).getFileName();
            //System.out.println("new name: " + newName);
            for (LineBreakpoint bp : bps) {
                LineID line = new LineID(newName, bp.lineID().lineIdx());
                //System.out.println("setting: " + line);
                dbg.setBreakpoint(line);
            }
            // add breakpoint marker comments to source file
            for (int i = 0; i < getSketch().getCodeCount(); i++) {
                addBreakpointComments(getSketch().getCode(i).getFileName());
            }

            // set new name of variable inspector
            vi.setTitle(getSketch().getName());
        }
        //  if file location has changed, update autosaver
//        autosaver.reloadAutosaveDir();
        return saved;
    }
    
    private boolean viewingAutosaveBackup;
    
    /**
     * Loads and starts the auto save service
     * Also handles the case where an auto save backup is found.
     * The user is asked to save the sketch to a new location
     */
    private void loadAutoSaver(){
      log("Load Auto Saver()");
      autosaver = new AutoSaveUtil(this, ExperimentalMode.autoSaveInterval);      
      if(!autosaver.checkForPastSave()) {
        autosaver.init();
        return;
      }
      File pastSave = autosaver.getPastSave();
      int response = Base
        .showYesNoQuestion(this,
                           "Unsaved backup found!",
                           "An automatic backup of \""
                               + pastSave.getParentFile().getName()
                               + "\" sketch has been found. This may mean Processing " +
                               "was closed unexpectedly last time.",
                           "Select YES to view it or NO to delete the backup.");
      if(response == JOptionPane.YES_OPTION){
        handleOpenInternal(pastSave.getAbsolutePath());        
        // Base.showMessage("Save it..", "Remember to save the backup sketch to a specific location if you want to.");
        //log(getSketch().getMainFilePath());
        log("loadAutoSaver, viewing autosave? " + viewingAutosaveBackup);
        return;
      }
      else{
        autosaver.init();
      }
    }

    /**
     * Set text contents of a specific tab. Updates underlying document and text
     * area. Clears Breakpoints.
     *
     * @param tabFilename the tab file name
     * @param code the text to set
     */
    protected void setTabContents(String tabFilename, String code) {
        // remove all breakpoints of this tab
        dbg.clearBreakpoints(tabFilename);

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

    /**
     * Clear the console.
     */
    public void clearConsole() {
        console.clear();
    }

    /**
     * Clear current text selection.
     */
    public void clearSelection() {
        setSelection(getCaretOffset(), getCaretOffset());
    }

    /**
     * Select a line in the current tab.
     *
     * @param lineIdx 0-based line number
     */
    public void selectLine(int lineIdx) {
        setSelection(getLineStartOffset(lineIdx), getLineStopOffset(lineIdx));
    }

    /**
     * Set the cursor to the start of a line.
     *
     * @param lineIdx 0-based line number
     */
    public void cursorToLineStart(int lineIdx) {
        setSelection(getLineStartOffset(lineIdx), getLineStartOffset(lineIdx));
    }

    /**
     * Set the cursor to the end of a line.
     *
     * @param lineIdx 0-based line number
     */
    public void cursorToLineEnd(int lineIdx) {
        setSelection(getLineStopOffset(lineIdx), getLineStopOffset(lineIdx));
    }

    /**
     * Switch to a tab.
     *
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

    /**
     * Access the debugger.
     *
     * @return the debugger controller object
     */
    public Debugger dbg() {
        return dbg;
    }

    /**
     * Access the mode.
     *
     * @return the mode object
     */
    public ExperimentalMode mode() {
        return dmode;
    }

    /**
     * Access the custom text area object.
     *
     * @return the text area object
     */
    public TextArea textArea() {
        return ta;
    }

    
    /**
     * Grab current contents of the sketch window, advance the console, stop any
     * other running sketches, auto-save the user's code... not in that order.
     */
    @Override
    public void prepareRun() {
        autoSave();
        super.prepareRun();
    }

    /**
     * Displays a JDialog prompting the user to save when the user hits
     * run/present/etc.
     */
    protected void autoSave() {
        if (!ExperimentalMode.autoSaveEnabled)
            return;

        try {
            // if (sketch.isUntitled() &&
            // ExperimentalMode.untitledAutoSaveEnabled) {
            // if (handleSave(true))
            // statusTimedNotice("Saved. Running...", 5);
            // else
            // statusTimedNotice("Save Canceled. Running anyway...", 5);
            // }
            // else
            if (sketch.isModified() && !sketch.isUntitled()) {
                if (ExperimentalMode.autoSavePromptEnabled) {
                    final JDialog autoSaveDialog = new JDialog(
                            base.getActiveEditor(), this.getSketch().getName(),
                            true);
                    Container container = autoSaveDialog.getContentPane();

                    JPanel panelMain = new JPanel();
                    panelMain.setBorder(BorderFactory.createEmptyBorder(4, 0,
                            2, 2));
                    panelMain.setLayout(new BoxLayout(panelMain,
                            BoxLayout.PAGE_AXIS));

                    JPanel panelLabel = new JPanel(new FlowLayout(
                            FlowLayout.LEFT));
                    JLabel label = new JLabel(
                            "<html><body>&nbsp;There are unsaved"
                                    + " changes in your sketch.<br />"
                                    + "&nbsp;&nbsp;&nbsp; Do you want to save it before"
                                    + " running? </body></html>");
                    label.setFont(new Font(label.getFont().getName(),
                            Font.PLAIN, label.getFont().getSize() + 1));
                    panelLabel.add(label);
                    panelMain.add(panelLabel);
                    final JCheckBox dontRedisplay = new JCheckBox(
                            "Remember this decision");

                    JPanel panelButtons = new JPanel(new FlowLayout(
                            FlowLayout.CENTER, 8, 2));
                    JButton btnRunSave = new JButton("Save and Run");
                    btnRunSave.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            handleSave(true);
                            if (dontRedisplay.isSelected()) {
                                ExperimentalMode.autoSavePromptEnabled = !dontRedisplay
                                        .isSelected();
                                ExperimentalMode.defaultAutoSaveEnabled = true;
                                dmode.savePreferences();
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
                                ExperimentalMode.autoSavePromptEnabled = !dontRedisplay
                                        .isSelected();
                                ExperimentalMode.defaultAutoSaveEnabled = false;
                                dmode.savePreferences();
                            }
                            autoSaveDialog.dispose();
                        }
                    });
                    panelButtons.add(btnRunNoSave);
                    panelMain.add(panelButtons);

                    JPanel panelCheck = new JPanel();
                    panelCheck
                            .setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
                    panelCheck.add(dontRedisplay);
                    panelMain.add(panelCheck);

                    container.add(panelMain);

                    autoSaveDialog.setResizable(false);
                    autoSaveDialog.pack();
                    autoSaveDialog
                            .setLocationRelativeTo(base.getActiveEditor());
                    autoSaveDialog.setVisible(true);

                } else if (ExperimentalMode.defaultAutoSaveEnabled)
                    handleSave(true);
            }
        } catch (Exception e) {
            statusError(e);
        }

    }    
    
    /**
     * Access variable inspector window.
     *
     * @return the variable inspector object
     */
    public VariableInspector variableInspector() {
        return vi;
    }

    public DebugToolbar toolbar() {
      if(toolbar instanceof DebugToolbar)
        return (DebugToolbar) toolbar;
      return null;
    }

    /**
     * Show the variable inspector window.
     */
    public void showVariableInspector() {
        vi.setVisible(true);
    }

    /**
     * Set visibility of the variable inspector window.
     *
     * @param visible true to set the variable inspector visible, false for
     * invisible.
     */
    public void showVariableInspector(boolean visible) {
        vi.setVisible(visible);
    }

    /**
     * Hide the variable inspector window.
     */
    public void hideVariableInspector() {
        vi.setVisible(true);
    }

    /**
     * Toggle visibility of the variable inspector window.
     */
    public void toggleVariableInspector() {
        vi.setFocusableWindowState(false); // to not get focus when set visible
        vi.setVisible(!vi.isVisible());
        vi.setFocusableWindowState(true); // allow to get focus again
    }

    /**
     * Text area factory method. Instantiates the customized TextArea.
     *
     * @return the customized text area object
     */
    @Override
    protected JEditTextArea createTextArea() {
        //System.out.println("overriding creation of text area");
        return new TextArea(new PdeTextAreaDefaults(mode), this);
    }

    /**
     * Set the line to highlight as currently suspended at. Will override the
     * breakpoint color, if set. Switches to the appropriate tab and scroll to
     * the line by placing the cursor there.
     *
     * @param line the line to highlight as current suspended line
     */
    public void setCurrentLine(LineID line) {
        clearCurrentLine();
        if (line == null) {
            return; // safety, e.g. when no line mapping is found and the null line is used.
        }
        switchToTab(line.fileName());
        // scroll to line, by setting the cursor
        cursorToLineStart(line.lineIdx());
        // highlight line
        currentLine = new LineHighlight(line.lineIdx(), currentLineColor, this);
        currentLine.setMarker(ta.currentLineMarker, currentLineMarkerColor);
        currentLine.setPriority(10); // fixes current line being hidden by the breakpoint when moved down
    }

    /**
     * Clear the highlight for the debuggers current line.
     */
    public void clearCurrentLine() {
        if (currentLine != null) {
            currentLine.clear();
            currentLine.dispose();

            // revert to breakpoint color if any is set on this line
            for (LineHighlight hl : breakpointedLines) {
                if (hl.lineID().equals(currentLine.lineID())) {
                    hl.paint();
                    break;
                }
            }
            currentLine = null;
        }
    }

    /**
     * Add highlight for a breakpointed line.
     *
     * @param lineID the line id to highlight as breakpointed
     */
    public void addBreakpointedLine(LineID lineID) {
        LineHighlight hl = new LineHighlight(lineID, breakpointColor, this);
        hl.setMarker(ta.breakpointMarker, breakpointMarkerColor);
        breakpointedLines.add(hl);
        // repaint current line if it's on this line
        if (currentLine != null && currentLine.lineID().equals(lineID)) {
            currentLine.paint();
        }
    }

    /**
     * Add highlight for a breakpointed line on the current tab.
     *
     * @param lineIdx the line index on the current tab to highlight as
     * breakpointed
     */
    //TODO: remove and replace by {@link #addBreakpointedLine(LineID lineID)}
    public void addBreakpointedLine(int lineIdx) {
        addBreakpointedLine(getLineIDInCurrentTab(lineIdx));
    }

    /**
     * Remove a highlight for a breakpointed line. Needs to be on the current
     * tab.
     *
     * @param lineIdx the line index on the current tab to remove a breakpoint
     * highlight from
     */
    public void removeBreakpointedLine(int lineIdx) {
        LineID line = getLineIDInCurrentTab(lineIdx);
        //System.out.println("line id: " + line.fileName() + " " + line.lineIdx());
        LineHighlight foundLine = null;
        for (LineHighlight hl : breakpointedLines) {
            if (hl.lineID.equals(line)) {
                foundLine = hl;
                break;
            }
        }
        if (foundLine != null) {
            foundLine.clear();
            breakpointedLines.remove(foundLine);
            foundLine.dispose();
            // repaint current line if it's on this line
            if (currentLine != null && currentLine.lineID().equals(line)) {
                currentLine.paint();
            }
        }
    }

    /**
     * Remove all highlights for breakpointed lines.
     */
    public void clearBreakpointedLines() {
        for (LineHighlight hl : breakpointedLines) {
            hl.clear();
            hl.dispose();
        }
        breakpointedLines.clear(); // remove all breakpoints
        // fix highlights not being removed when tab names have changed due to opening a new sketch in same editor
        ta.clearLineBgColors(); // force clear all highlights
        ta.clearGutterText();

        // repaint current line
        if (currentLine != null) {
            currentLine.paint();
        }
    }

    /**
     * Retrieve a {@link LineID} object for a line on the current tab.
     *
     * @param lineIdx the line index on the current tab
     * @return the {@link LineID} object representing a line index on the
     * current tab
     */
    public LineID getLineIDInCurrentTab(int lineIdx) {
        return new LineID(getSketch().getCurrentCode().getFileName(), lineIdx);
    }

    /**
     * Retrieve line of sketch where the cursor currently resides.
     *
     * @return the current {@link LineID}
     */
    protected LineID getCurrentLineID() {
        String tab = getSketch().getCurrentCode().getFileName();
        int lineNo = getTextArea().getCaretLine();
        return new LineID(tab, lineNo);
    }

    /**
     * Check whether a {@link LineID} is on the current tab.
     *
     * @param line the {@link LineID}
     * @return true, if the {@link LineID} is on the current tab.
     */
    public boolean isInCurrentTab(LineID line) {
        return line.fileName().equals(getSketch().getCurrentCode().getFileName());
    }

    /**
     * Event handler called when switching between tabs. Loads all line
     * background colors set for the tab.
     *
     * @param code tab to switch to
     */
    @Override
    protected void setCode(SketchCode code) {
        //System.out.println("tab switch: " + code.getFileName());
        super.setCode(code); // set the new document in the textarea, etc. need to do this first

        // set line background colors for tab
        if (ta != null) { // can be null when setCode is called the first time (in constructor)
            // clear all line backgrounds
            ta.clearLineBgColors();
            // clear all gutter text
            ta.clearGutterText();
            // load appropriate line backgrounds for tab
            // first paint breakpoints
            for (LineHighlight hl : breakpointedLines) {
                if (isInCurrentTab(hl.lineID())) {
                    hl.paint();
                }
            }
            // now paint current line (if any)
            if (currentLine != null) {
                if (isInCurrentTab(currentLine.lineID())) {
                    currentLine.paint();
                }
            }
        }
        if (dbg() != null && dbg().isStarted()) {
            dbg().startTrackingLineChanges();
        }
    }

    /**
     * Get a tab by its file name.
     *
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
     *
     * @return the {@link SketchCode} representing the current tab
     */
    public SketchCode getCurrentTab() {
        return getSketch().getCurrentCode();
    }

    /**
     * Access the currently edited document.
     *
     * @return the document object
     */
    public Document currentDocument() {
        //return ta.getDocument();
        return getCurrentTab().getDocument();
    }

    /**
     * Factory method for the editor toolbar. Instantiates the customized
     * toolbar.
     *
     * @return the toolbar
     */
    /*@Override
    public EditorToolbar createToolbar() {
        return new DebugToolbar(this, base);
    }*/

    /**
     * Event Handler for double clicking in the left hand gutter area.
     *
     * @param lineIdx the line (0-based) that was double clicked
     */
    public void gutterDblClicked(int lineIdx) {
        if (dbg != null) {
            dbg.toggleBreakpoint(lineIdx);
        }
    }

    public void statusBusy() {
        statusNotice("Debugger busy...");
    }

    public void statusHalted() {
        statusNotice("Debugger halted.");
    }
    
    public static final int STATUS_EMPTY = 100, STATUS_COMPILER_ERR = 200,
        STATUS_WARNING = 300, STATUS_INFO = 400, STATUS_ERR = 500;
    public int statusMessageType = STATUS_EMPTY;
    public String statusMessage;
    public void statusMessage(final String what, int type){
      // Don't re-display the old message again
      if(type != STATUS_EMPTY) {
        if(what.equals(statusMessage) && type == statusMessageType) {
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
      if(type == STATUS_COMPILER_ERR) return;
      
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
    
    ErrorCheckerService errorCheckerService;
    
    /**
     * Initializes and starts Error Checker Service
     */
    private void initializeErrorChecker() {
      Thread errorCheckerThread = null;

      if (errorCheckerThread == null) {
        errorCheckerService = new ErrorCheckerService(this);
        errorCheckerThread = new Thread(errorCheckerService);
        try {
          errorCheckerThread.start();
        } catch (Exception e) {
          System.err
          .println("Error Checker Service not initialized [XQEditor]: "
            + e);
          // e.printStackTrace();
        }
        // System.out.println("Error Checker Service initialized.");
      }

    }
    
    /**
     * Updates the error bar
     * @param problems
     */
    public void updateErrorBar(ArrayList<Problem> problems) {
          errorBar.updateErrorPoints(problems);
    }

    /**
     * Toggle between Console and Errors List
     * 
     * @param buttonName
     *            - Button Label
     */
    public void showProblemListView(String buttonName) {
      CardLayout cl = (CardLayout) consoleProblemsPane.getLayout();
      cl.show(consoleProblemsPane, buttonName);
    }
    
    /**
     * Updates the error table
     * @param tableModel
     * @return
     */
    synchronized public boolean updateTable(final TableModel tableModel) {
      return errorTable.updateTable(tableModel);
    }
    
    /**
     * Handle whether the tiny red error indicator is shown near the error button
     * at the bottom of the PDE
     */
    public void updateErrorToggle(){
		btnShowErrors.updateMarker(ExperimentalMode.errorCheckEnabled
				&& errorCheckerService.hasErrors(), errorBar.errorColor);
    }
    
    /**
     * Handle refactor operation
     */
    private void handleRefactor() {
      log("Caret at:");
      log(ta.getLineText(ta.getCaretLine()));
      errorCheckerService.getASTGenerator().handleRefactor();
    }
    
    /**
     * Handle show usage operation
     */
    private void handleShowUsage() {
      log("Caret at:");
      log(ta.getLineText(ta.getCaretLine()));
      errorCheckerService.getASTGenerator().handleShowUsage();
    }
    
    /**
     * Checks if the sketch contains java tabs. If it does, the editor ain't built
     * for it, yet. Also, user should really start looking at more powerful IDEs 
     * likeEclipse. Disable compilation check and some more features.
     */
    private void checkForJavaTabs() {
      hasJavaTabs = false;
      for (int i = 0; i < this.getSketch().getCodeCount(); i++) {
        if (this.getSketch().getCode(i).getExtension().equals("java")) {
          compilationCheckEnabled = false;
          hasJavaTabs = true;
          JOptionPane.showMessageDialog(new Frame(), this
              .getSketch().getName()
              + " contains .java tabs. Some editor features are not supported " +
              "for .java tabs and will be disabled.");
          break;
        }
      }
    }
    
	protected void applyPreferences() {
		super.applyPreferences();
		if (dmode != null) {
			dmode.loadPreferences();
			log("Applying prefs");
			// trigger it once to refresh UI
			errorCheckerService.runManualErrorCheck();
		}
	}
	
  /**
   * Handles toggle comment. Slightly improved from the default implementation
   * in {@link processing.app.Editor}
   */
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
      if (lineText.length() == 0)
        continue; //ignore blank lines
      commented = lineText.startsWith(prefix);
    }

    // log("Commented: " + commented);

    // This is the line start offset of the first line, which is added to
    // all other lines while adding a comment. Required when commenting 
    // lines which have uneven whitespaces in the beginning. Makes the 
    // commented lines look more uniform.    
    int lso = Math.abs(textarea.getLineStartNonWhiteSpaceOffset(startLine)
        - textarea.getLineStartOffset(startLine));

    for (int line = startLine; line <= stopLine; line++) {
      int location = textarea.getLineStartNonWhiteSpaceOffset(line);
      String lineText = textarea.getLineText(line);
      if (lineText.trim().length() == 0)
        continue; //ignore blank lines
      if (commented) {
        // remove a comment
        if (lineText.trim().startsWith(prefix + " ")) {
          textarea.select(location, location + prefixLen + 1);
        } else {
          textarea.select(location, location + prefixLen);
        }
        textarea.setSelectedText("");
      } else {
        // add a comment
        location = textarea.getLineStartOffset(line) + lso;
        textarea.select(location, location);
        textarea.setSelectedText(prefix + " "); //Add a '// '
      }
    }
    // Subtract one from the end, otherwise selects past the current line.
    // (Which causes subsequent calls to keep expanding the selection)
    textarea.select(textarea.getLineStartOffset(startLine),
                    textarea.getLineStopOffset(stopLine) - 1);
    stopCompoundEdit();
    sketch.setModified(true);
  }

    // TweakMode code
    /**
     * Show warnings menu item
     */
    //protected JCheckBoxMenuItem enableTweakCB;

	String[] baseCode;

	final static int SPACE_AMOUNT = 0;

	int oscPort;

	public void startInteractiveMode()
	{
		ta.startInteractiveMode();
	}

	public void stopInteractiveMode(ArrayList<Handle> handles[])
	{
		ta.stopInteractiveMode();

		// remove space from the code (before and after)
		removeSpacesFromCode();

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
			int ret = Base.showYesNoQuestion(this, "Tweak Mode",
									"Keep the changes?",
									"You changed some values in your sketch. Would you like to keep the changes?");
			if (ret == 1) {
				// NO! don't keep changes
				loadSavedCode();
				// update the painter to draw the saved (old) code
				ta.invalidate();
			}
			else {
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
				}
				catch (IOException e) {
					Base.showWarning("Tweak Mode", "Could not save the modified sketch!", e);
				}

				// repaint the editor header (show the modified tabs)
				header.repaint();
				ta.invalidate();
			}
		}
		else {
			// number values were not modified but we need to load the saved code
			// because of some formatting changes
			loadSavedCode();
			ta.invalidate();
		}
	}

	public void updateInterface(ArrayList<Handle> handles[], ArrayList<ColorControlBox> colorBoxes[])
	{
		// set OSC port of handles
		for (int i=0; i<handles.length; i++) {
			for (Handle h : handles[i]) {
				h.setOscPort(oscPort);
			}
		}

		ta.updateInterface(handles, colorBoxes);
	}

	/**
	* Deactivate run button
	* Do this because when Mode.handleRun returns null the play button stays on.
	*/
	public void deactivateRun()
	{
//		toolbar.deactivate(TweakToolbar.RUN);
	  if(toolbar instanceof DebugToolbar){
	    toolbar.deactivate(DebugToolbar.RUN);
	  } else {
	    super.deactivateRun();
	  }
	}

	private boolean[] getModifiedTabs(ArrayList<Handle> handles[])
	{
		boolean[] modifiedTabs = new boolean[handles.length];

		for (int i=0; i<handles.length; i++) {
			for (Handle h : handles[i]) {
				if (h.valueChanged()) {
					modifiedTabs[i] = true;
				}
			}
		}

		return modifiedTabs;
	}

	public void initBaseCode()
	{
    	SketchCode[] code = sketch.getCode();

    	String space = new String();

    	for (int i=0; i<SPACE_AMOUNT; i++) {
    		space += "\n";
    	}

    	baseCode = new String[code.length];
		for (int i=0; i<code.length; i++)
		{
			baseCode[i] = new String(code[i].getSavedProgram());
			baseCode[i] = space + baseCode[i] + space;
		}
	}

	public void initEditorCode(ArrayList<Handle> handles[], boolean withSpaces)
	{
		SketchCode[] sketchCode = sketch.getCode();
		for (int tab=0; tab<baseCode.length; tab++) {
				// beautify the numbers
				int charInc = 0;
				String code = baseCode[tab];

				for (Handle n : handles[tab])
				{
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

	private void loadSavedCode()
	{
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

	private void removeSpacesFromCode()
	{
		SketchCode[] code = sketch.getCode();
		for (int i=0; i<code.length; i++) {
			String c = code[i].getProgram();
			c = c.substring(SPACE_AMOUNT, c.length() - SPACE_AMOUNT);
			code[i].setProgram(c);
			/* Wild Hack: set document to null so the text editor will refresh
			   the program contents when the document tab is being clicked */
			code[i].setDocument(null);
		}
		// this will update the current code
		setCode(sketch.getCurrentCode());
	}

    /**
     * Replace all numbers with variables and add code to initialize these variables and handle OSC messages.
     * @param sketch
     * 	the sketch to work on
     * @param handles
     * 	list of numbers to replace in this sketch
     * @return
     *  true on success
     */
    public boolean automateSketch(Sketch sketch, ArrayList<Handle> handles[])
    {
    	SketchCode[] code = sketch.getCode();

    	if (code.length<1)
    		return false;

    	if (handles.length == 0)
    		return false;

    	int setupStartPos = SketchParser.getSetupStart(baseCode[0]);
    	if (setupStartPos < 0) {
    		return false;
    	}

		// Copy current program to interactive program

    	/* modify the code below, replace all numbers with their variable names */
    	// loop through all tabs in the current sketch
    	for (int tab=0; tab<code.length; tab++)
    	{
    		int charInc = 0;
			String c = baseCode[tab];
			for (Handle n : handles[tab])
    		{
    			// replace number value with a variable
    			c = replaceString(c, n.startChar + charInc, n.endChar + charInc, n.name);
    			charInc += n.name.length() - n.strValue.length();
    		}
			code[tab].setProgram(c);
    	}

    	/* add the main header to the code in the first tab */
    	String c = code[0].getProgram();

    	// header contains variable declaration, initialization, and OSC listener function
    	String header;
    	header = "\n\n" +
    		 "/*************************/\n" +
    		 "/* MODIFIED BY TWEAKMODE */\n" +
		 	 "/*************************/\n" +
    		 "\n\n";

    	// add needed OSC imports and the global OSC object
    	header += "import oscP5.*;\n";
    	header += "import netP5.*;\n\n";
    	header += "OscP5 tweakmode_oscP5;\n\n";

    	// write a declaration for int and float arrays
    	int numOfInts = howManyInts(handles);
    	int numOfFloats = howManyFloats(handles);
    	if (numOfInts > 0) {
    		header += "int[] tweakmode_int = new int["+numOfInts+"];\n";
    	}
    	if (numOfFloats > 0) {
    		header += "float[] tweakmode_float = new float["+numOfFloats+"];\n\n";
    	}

    	/* add the class for the OSC event handler that will respond to our messages */
    	header += "public class TweakMode_OscHandler {\n" +
    			  "  public void oscEvent(OscMessage msg) {\n" +
                  "    String type = msg.addrPattern();\n";
        if (numOfInts > 0) {
        	header += "    if (type.contains(\"/tm_change_int\")) {\n" +
                      "      int index = msg.get(0).intValue();\n" +
                      "      int value = msg.get(1).intValue();\n" +
                      "      tweakmode_int[index] = value;\n" +
                      "    }\n";
        	if (numOfFloats > 0) {
        		header += "    else ";
        	}
        }
        if (numOfFloats > 0) {
            header += "if (type.contains(\"/tm_change_float\")) {\n" +
                      "      int index = msg.get(0).intValue();\n" +
                      "      float value = msg.get(1).floatValue();\n" +
                      "      tweakmode_float[index] = value;\n" +
                      "    }\n";
        }
        header += "  }\n" +
                  "}\n";
    	header += "TweakMode_OscHandler tweakmode_oscHandler = new TweakMode_OscHandler();\n";

    	header += "void tweakmode_initAllVars() {\n";
    	for (int i=0; i<handles.length; i++) {
    		for (Handle n : handles[i])
    		{
    			header += "  " + n.name + " = " + n.strValue + ";\n";
    		}
    	}
    	header += "}\n\n";
    	header += "void tweakmode_initOSC() {\n";
    	header += "  tweakmode_oscP5 = new OscP5(tweakmode_oscHandler,"+oscPort+");\n";
    	header += "}\n";

    	header += "\n\n\n\n\n";

    	// add call to our initAllVars and initOSC functions from the setup() function.
    	String addToSetup = "\n  tweakmode_initAllVars();\n  tweakmode_initOSC();\n\n";
    	setupStartPos = SketchParser.getSetupStart(c);
    	c = replaceString(c, setupStartPos, setupStartPos, addToSetup);

    	code[0].setProgram(header + c);

    	/* print out modified code */
//    	if (tweakMode.dumpModifiedCode) {
//    		System.out.println("\nModified code:\n");
//    		for (int i=0; i<code.length; i++)
//    		{
//    			System.out.println("file " + i + "\n=========");
//    			System.out.println(code[i].getProgram());
//    		}
//    	}

    	return true;
    }

	private String replaceString(String str, int start, int end, String put)
	{
		return str.substring(0, start) + put + str.substring(end, str.length());
	}

	private int howManyInts(ArrayList<Handle> handles[])
	{
		int count = 0;
		for (int i=0; i<handles.length; i++) {
			for (Handle n : handles[i]) {
				if (n.type == "int" || n.type == "hex" || n.type == "webcolor")
					count++;
			}
		}
		return count;
	}

	private int howManyFloats(ArrayList<Handle> handles[])
	{
		int count = 0;
		for (int i=0; i<handles.length; i++) {
			for (Handle n : handles[i]) {
				if (n.type == "float")
					count++;
			}
		}
		return count;
	}

}

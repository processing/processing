package processing.mode.javascript;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import processing.app.Base;
import processing.app.Editor;
import processing.app.EditorToolbar;
import processing.app.Formatter;
import processing.app.Mode;
import processing.mode.java.AutoFormat;

public class JavaScriptEditor extends Editor {
  private JavaScriptMode jsMode;

  
  protected JavaScriptEditor(Base base, String path, int[] location, Mode mode) {
    super(base, path, location, mode);
    jsMode = (JavaScriptMode) mode;
  }

  
  public EditorToolbar createToolbar() {
    return new JavaScriptToolbar(this, base);
  }

  
  public Formatter createFormatter() { 
    return new AutoFormat();    
  }

  
  // - - - - - - - - - - - - - - - - - -
  // Menu methods

  
  public JMenu buildFileMenu() {
    JMenuItem exportItem = Base.newJMenuItem("export title", 'E');
    exportItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        handleExport();
      }
    });
    return buildFileMenu(new JMenuItem[] { exportItem });
  }


  public JMenu buildSketchMenu() {
    return buildSketchMenu(new JMenuItem[] {});
  }

  
  public JMenu buildHelpMenu() {
    JMenu menu = new JMenu("Help ");
    JMenuItem item;

    item = new JMenuItem("QuickStart for JS Devs");
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Base.openURL("http://processingjs.org/reference/articles/jsQuickStart");
      }
    });
    menu.add(item);

    item = new JMenuItem("QuickStart for Processing Devs");
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Base.openURL("http://processingjs.org/reference/articles/p5QuickStart");
      }
    });
    menu.add(item);

    /* TODO Implement an environment page
    item = new JMenuItem("Environment");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          showReference("environment" + File.separator + "index.html");
        }
      });
    menu.add(item);
     */

    /* TODO Implement a troubleshooting page
    item = new JMenuItem("Troubleshooting");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Base.openURL("http://wiki.processing.org/w/Troubleshooting");
        }
      });
    menu.add(item);
     */

    item = new JMenuItem("Reference");
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        //TODO get offline reference archive corresponding to the release 
        // packaged with this mode see: P.js ticket 1146 "Offline Reference"
        Base.openURL("http://processingjs.org/reference");
      }
    });
    menu.add(item);

    item = Base.newJMenuItemShift("Find in Reference", 'F');
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (textarea.isSelectionActive()) {
          Base.openURL(
            "http://www.google.com/search?q=" +
            textarea.getSelectedText() + 
            "+site%3Ahttp%3A%2F%2Fprocessingjs.org%2Freference"
          );
        }
      }
    });
    menu.add(item);

    /* TODO FAQ
    item = new JMenuItem("Frequently Asked Questions");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Base.openURL("http://wiki.processing.org/w/FAQ");
        }
      });
    menu.add(item);
    */
    
    item = new JMenuItem("Visit Processingjs.org");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Base.openURL("http://processingjs.org/");
        }
      });
    menu.add(item);
    
    // OSX has its own about menu
    if (!Base.isMacOS()) {
      menu.addSeparator();
      item = new JMenuItem("About Processing");
      item.addActionListener( new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          base.handleAbout();
        }
      });
      menu.add(item);
    }
    
    return menu;
  }

  
  // - - - - - - - - - - - - - - - - - -
  
  
  public String getCommentPrefix() { 
    return "//";
  }
  
  
  // - - - - - - - - - - - - - - - - - -
  
  
  /**
   * Call the export method of the sketch and handle the gui stuff
   */
  public void handleExport() {
    if (handleExportCheckModified()) {
      toolbar.activate(JavaScriptToolbar.EXPORT);
      try {
        boolean success = jsMode.handleExport(sketch);
        if (success) {
          File appletJSFolder = new File(sketch.getFolder(), "applet_js");
          Base.openFolder(appletJSFolder);
          statusNotice("Finished exporting.");
        } else { 
          // error message already displayed by handleExport          
        }
      } catch (Exception e) {
        statusError(e);
      }
      toolbar.deactivate(JavaScriptToolbar.EXPORT);
    }
  }
  
  
  public boolean handleExportCheckModified() {
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
  
  
  public void handleSave() { 
    toolbar.activate(JavaScriptToolbar.SAVE);
    super.handleSave();
    toolbar.deactivate(JavaScriptToolbar.SAVE);
  }
  
  
  public boolean handleSaveAs() { 
    toolbar.activate(JavaScriptToolbar.SAVE);
    boolean result = super.handleSaveAs();
    toolbar.deactivate(JavaScriptToolbar.SAVE);
    return result;
  }


  public void handleImportLibrary(String item) {
    Base.showWarning("Processing.js doesn't support libraries",
                     "Libraries are not supported. Import statements are " +
                     "ignored, and code relying on them will break.",
                     null);
  }


  /** JavaScript mode has no runner. This method is empty. */
  public void internalCloseRunner() { }


  /** JavaScript mode does not run anything. This method is empty. */
  public void deactivateRun() { } 
}

package processing.mode.java;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import processing.app.Base;
import processing.app.Editor;
import processing.app.Mode;

public class JavaEditor extends Editor {
    
  protected JavaEditor(Base base, String path, int[] location, Mode mode) {
    super(base, path, location, mode);
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  
  public JMenu buildFileMenu() {
    JMenuItem exportApplet = Base.newJMenuItem("Export Applet", 'E');
    exportApplet.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        handleExport();
      }
    });
      
    JMenuItem exportApplication = Base.newJMenuItemShift("Export Application", 'E');
    exportApplication.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        handleExportApplication();
      }
    });
    return buildFileMenu(new JMenuItem[] { exportApplet, exportApplication });
  }
  
  
  public JMenu buildSketchMenu() {
    JMenuItem runItem = Base.newJMenuItem("Run", 'R');
    runItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleRun();
        }
      });

    JMenuItem presentItem = Base.newJMenuItemShift("Present", 'R');
    presentItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handlePresent();
        }
      });

    JMenuItem stopItem = new JMenuItem("Stop");
    stopItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleStop();
        }
      });
    return buildSketchMenu(new JMenuItem[] { runItem, presentItem, stopItem });
  }
  

  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  
  public String getCommentPrefix() {
    return "//";
  }
  
  
  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
  
  
  public void handleRun() {
    
  }
  
  
  public void handlePresent() {
    
  }
  
  
  public void handleStop() {
    
  }
  
  
  public void handleExport() {
    // set toolbar item state
    // call export inside javamode
    // show error messages as necessary
    // set toolbar item state
  }
  
  
  public void handleExportApplication() {
    
  }
}
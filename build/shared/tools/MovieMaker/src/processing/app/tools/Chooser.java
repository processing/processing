package processing.app.tools;

import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;

import javax.swing.JFileChooser;


/** File chooser additions, cannibalized from PApplet. */
class Chooser {
  static final boolean useNativeSelect = true;

  
  static abstract class Callback {
    //abstract void select(File file);
    void handle(final File file) {
      EventQueue.invokeLater(new Runnable() {
//      new Thread(new Runnable() {
        public void run() {
          select(file);
        }
      });
//      }).start();
    }
    
    abstract void select(File file);
  }
  

//  Frame parent;
//  
//  public Chooser(Frame parent) {
//    this.parent = parent;
//  }
  
  /**
   * Open a platform-specific file chooser dialog to select a file for input.
   * After the selection is made, the selected File will be passed to the
   * 'callback' function. If the dialog is closed or canceled, null will be
   * sent to the function, so that the program is not waiting for additional
   * input. The callback is necessary because of how threading works.
   *
   * <pre>
   * void setup() {
   *   selectInput("Select a file to process:", "fileSelected");
   * }
   *
   * void fileSelected(File selection) {
   *   if (selection == null) {
   *     println("Window was closed or the user hit cancel.");
   *   } else {
   *     println("User selected " + fileSeleted.getAbsolutePath());
   *   }
   * }
   * </pre>
   *
   * For advanced users, the method must be 'public', which is true for all
   * methods inside a sketch when run from the PDE, but must explicitly be
   * set when using Eclipse or other development environments.
   *
   * @webref input:files
   * @param prompt message to the user
   * @param callback name of the method to be called when the selection is made
   */
//  public void selectInput(String prompt, String callback) {
//    selectInput(prompt, callback, null);
//  }


//  public void selectInput(String prompt, String callback, File file) {
//    selectInput(prompt, callback, file, this);
//  }


//  public void selectInput(String prompt, String callback,
//                          File file, Object callbackObject) {
//    selectInput(prompt, callback, file, callbackObject, selectFrame());
//  }


  static public void selectInput(Frame parent, String prompt, File file, 
                                 Callback callback) {
    selectImpl(parent, prompt, file, callback, FileDialog.LOAD);
  }


  /**
   * See selectInput() for details.
   *
   * @webref output:files
   * @param prompt message to the user
   * @param callback name of the method to be called when the selection is made
   */
//  public void selectOutput(String prompt, String callback) {
//    selectOutput(prompt, callback, null);
//  }
//
//  public void selectOutput(String prompt, String callback, File file) {
//    selectOutput(prompt, callback, file, this);
//  }
//
//
//  public void selectOutput(String prompt, String callback,
//                           File file, Object callbackObject) {
//    selectOutput(prompt, callback, file, callbackObject, selectFrame());
//  }


  static public void selectOutput(Frame parent, String prompt, File file, 
                                  Callback callback) {
    selectImpl(parent, prompt, file, callback, FileDialog.SAVE);
  }


  static protected void selectImpl(final Frame parentFrame,
                                   final String prompt,
                                   final File defaultSelection,
                                   final Callback callback,
                                   final int mode) {
//    EventQueue.invokeLater(new Runnable() {
//      public void run() {
    File selectedFile = null;

    if (useNativeSelect) {
      FileDialog dialog = new FileDialog(parentFrame, prompt, mode);
      if (defaultSelection != null) {
        dialog.setDirectory(defaultSelection.getParent());
        dialog.setFile(defaultSelection.getName());
      }
      dialog.setVisible(true);
      String directory = dialog.getDirectory();
      String filename = dialog.getFile();
      if (filename != null) {
        selectedFile = new File(directory, filename);
      }

    } else {
      JFileChooser chooser = new JFileChooser();
      chooser.setDialogTitle(prompt);
      if (defaultSelection != null) {
        chooser.setSelectedFile(defaultSelection);
      }

      int result = -1;
      if (mode == FileDialog.SAVE) {
        result = chooser.showSaveDialog(parentFrame);
      } else if (mode == FileDialog.LOAD) {
        result = chooser.showOpenDialog(parentFrame);
      }
      if (result == JFileChooser.APPROVE_OPTION) {
        selectedFile = chooser.getSelectedFile();
      }
    }
    //selectCallback(selectedFile, callbackMethod, callbackObject);
    callback.handle(selectedFile);
//      }
//    });
  }


  /**
   * See selectInput() for details.
   *
   * @webref input:files
   * @param prompt message to the user
   * @param callback name of the method to be called when the selection is made
   */
//  public void selectFolder(String prompt, String callback) {
//    selectFolder(prompt, callback, null);
//  }
//
//
//  public void selectFolder(String prompt, String callback, File file) {
//    selectFolder(prompt, callback, file, this);
//  }
//
//
//  public void selectFolder(String prompt, String callback,
//                           File file, Object callbackObject) {
//    selectFolder(prompt, callback, file, callbackObject, selectFrame());
//  }


  static public void selectFolder(final Frame parentFrame,
                                  final String prompt,
                                  final File defaultSelection,
                                  final Callback callback) {
//    EventQueue.invokeLater(new Runnable() {
//      public void run() {
    File selectedFile = null;

    if (System.getProperty("os.name").contains("Mac") && useNativeSelect) {
      FileDialog fileDialog =
        new FileDialog(parentFrame, prompt, FileDialog.LOAD);
      System.setProperty("apple.awt.fileDialogForDirectories", "true");
      fileDialog.setVisible(true);
      System.setProperty("apple.awt.fileDialogForDirectories", "false");
      String filename = fileDialog.getFile();
      if (filename != null) {
        selectedFile = new File(fileDialog.getDirectory(), fileDialog.getFile());
      }
    } else {
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setDialogTitle(prompt);
      fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      if (defaultSelection != null) {
        fileChooser.setSelectedFile(defaultSelection);
      }

      int result = fileChooser.showOpenDialog(parentFrame);
      if (result == JFileChooser.APPROVE_OPTION) {
        selectedFile = fileChooser.getSelectedFile();
      }
    }
    //selectCallback(selectedFile, callbackMethod, callbackObject);
    callback.handle(selectedFile);
//      }
//    });
  }


//  static private void selectCallback(File selectedFile,
//                                     String callbackMethod,
//                                     Object callbackObject) {
//    try {
//      Class<?> callbackClass = callbackObject.getClass();
//      Method selectMethod =
//        callbackClass.getMethod(callbackMethod, new Class[] { File.class });
//      selectMethod.invoke(callbackObject, new Object[] { selectedFile });
//
//    } catch (IllegalAccessException iae) {
//      System.err.println(callbackMethod + "() must be public");
//
//    } catch (InvocationTargetException ite) {
//      ite.printStackTrace();
//
//    } catch (NoSuchMethodException nsme) {
//      System.err.println(callbackMethod + "() could not be found");
//    }
//  }
}
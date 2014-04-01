package processing.app;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

//Class used to handle progress bar, and run Save As or Add File in
//background so that
//progress bar can update without freezing
public class ProgressFrame extends JFrame implements PropertyChangeListener {

  private static final long serialVersionUID = 1L;

  private JProgressBar progressBar;

  private JLabel saveAsLabel;

  private TaskSaveAs t;

  private TaskAddFile t2;

  private File[] copyItems;

  private File newFolder;

  private File addFile, sourceFile;

  private Editor editor;

  // create a new background thread to save as
  public class TaskSaveAs extends SwingWorker<Void, Void> {

    @Override
    protected Void doInBackground() throws Exception {
      // a large part of the file copying happens in this background
      // thread

      long totalSize = 0;
      for (File copyable : copyItems) {
        totalSize += getFileLength(copyable);
      }

      long progress = 0;
      setProgress(0);
      for (File copyable : ProgressFrame.this.copyItems) {
        // loop to copy over the items that make sense, and to set the
        // current progress

        if (copyable.isDirectory()) {
          Base.copyDir(copyable, new File(ProgressFrame.this.newFolder,
                                          copyable.getName()), this, progress,
                       totalSize);
          progress += getFileLength(copyable);
        } else {
          Base.copyFile(copyable, new File(ProgressFrame.this.newFolder,
                                           copyable.getName()), this, progress,
                        totalSize);
          if (getFileLength(copyable) < 524288) {
            // If the file length > 0.5MB, the Base.copyFile() function has 
            // been redesigned to change progress every 0.5MB so that
            // the progress bar doesn't stagnate during that time
            progress += getFileLength(copyable);
            setProgress((int) Math.min(Math.ceil(progress * 100.0 / totalSize),
                                       100));
          }
        }
      }

      return null;
    }

    public void setProgressBarStatus(int status) {

      setProgress(status);
    }

    @Override
    public void done() {
      // to close the progress bar automatically when done, and to 
      // print that Saving is done in Message Area

      editor.statusNotice("Done Saving.");
      ProgressFrame.this.closeProgressBar();
    }

  }

  // create a new background thread to add a file
  public class TaskAddFile extends SwingWorker<Void, Void> {

    @Override
    protected Void doInBackground() throws Exception {
      // a large part of the file copying happens in this background
      // thread

      setProgress(0);

      Base.copyFile(sourceFile, addFile, this);

      if (addFile.length() < 1024) {
        // If the file length > 1kB, the Base.copyFile() function has 
        // been redesigned to change progress every 1kB so that
        // the progress bar doesn't stagnate during that time

        // If file <1 kB, just fill up Progress Bar to 100%
        // directly, since time to copy is now negligable (when
        // perceived by a human, anyway)
        setProgress(100);
      }

      return null;
    }

    public void setProgressBarStatus(int status) {
      setProgress(status);
    }

    @Override
    public void done() {
      // to close the progress bar automatically when done, and to 
      // print that adding file is done in Message Area

      editor.statusNotice("One file added to the sketch.");
      ProgressFrame.this.closeProgressBar();
    }

  }

  //Use for Save As
  public ProgressFrame(File[] c, File nf, String oldName, String newName,
                       Editor editor) {
    // initialize a copyItems and newFolder, which are used for file
    // copying in the background thread
    copyItems = c;
    newFolder = nf;
    this.editor = editor;

    // the UI of the progress bar follows
    setDefaultCloseOperation(HIDE_ON_CLOSE);
    setBounds(200, 200, 400, 140);
    setResizable(false);
    setTitle("Saving As...");
    JPanel panel = new JPanel(null);
    add(panel);
    setContentPane(panel);
    saveAsLabel = new JLabel("Saving " + oldName + " as " + newName + "...");
    saveAsLabel.setBounds(40, 20, 300, 20);

    progressBar = new JProgressBar(0, 100);
    progressBar.setValue(0);
    progressBar.setBounds(40, 50, 300, 30);
    progressBar.setStringPainted(true);

    panel.add(progressBar);
    panel.add(saveAsLabel);
    Toolkit.setIcon(this);
    this.setVisible(true);

    // create an instance of TaskSaveAs and run execute() on this
    // instance to
    // start background thread
    t = new TaskSaveAs();
    t.addPropertyChangeListener(this);
    t.execute();
  }

  //Use for Add File
  public ProgressFrame(File sf, File add, Editor editor) {

    addFile = add;
    sourceFile = sf;
    this.editor = editor;

    // the UI of the progress bar follows
    setDefaultCloseOperation(HIDE_ON_CLOSE);
    setBounds(200, 200, 400, 140);
    setResizable(false);
    setTitle("Adding File...");
    JPanel panel = new JPanel(null);
    add(panel);
    setContentPane(panel);
    saveAsLabel = new JLabel("Adding " + addFile.getName());
    saveAsLabel.setBounds(40, 20, 300, 20);

    progressBar = new JProgressBar(0, 100);
    progressBar.setValue(0);
    progressBar.setBounds(40, 50, 300, 30);
    progressBar.setStringPainted(true);

    panel.add(progressBar);
    panel.add(saveAsLabel);
    Toolkit.setIcon(this);
    this.setVisible(true);

    // create an instance of TaskAddFile and run execute() on this
    // instance to
    // start background thread
    t2 = new TaskAddFile();
    t2.addPropertyChangeListener(this);
    t2.execute();
  }

  public long getFileLength(File f)// function to return the length of
  // the file, or
  // ENTIRE directory, including the
  // component files
  // and sub-folders if passed
  {
    long fol_len = 0;
    if (f.isDirectory()) {
      String files[] = f.list();
      for (int i = 0; i < files.length; i++) {
        File temp = new File(f, files[i]);
        if (temp.isDirectory()) {
          fol_len += getFileLength(temp);
        } else {
          fol_len += (temp.length());
        }
      }
    } else {
      return (f.length());
    }
    return fol_len;
  }

  public void propertyChange(PropertyChangeEvent evt)
  // detects a change in the property of the background task, i.e., is
  // called when the size of files already copied changes
  {
    if ("progress" == evt.getPropertyName()) {
      int progress = (Integer) evt.getNewValue();
      progressBar.setValue(progress);
    }
  }

  private void closeProgressBar()
  // closes progress bar
  {
    this.dispose();
  }

}

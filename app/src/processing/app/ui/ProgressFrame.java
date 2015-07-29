package processing.app.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import processing.app.Language;
import processing.app.Util;


// TODO This code was contributed and needs a lot of work. [fry]
// + It probably shouldn't a "Frame" object at all.
// + It should be more general to the tasks at hand (why Save As and Add File
//   in here?) We'd have to copy the code again for a new "Task"
// + Instead of multiple implementations of file and directory copy, it should
//   just use a single version from Base that takes a ProgressMonitor as an arg

/**
 * Class used to handle progress bar, and run Save As or Add File in
 * background so that progress bar can update without freezing.
 */
public class ProgressFrame extends JFrame implements PropertyChangeListener {
  private JProgressBar progressBar;
  private JLabel label;
  private File[] copyItems;
  private File newFolder;
  private File addFile, sourceFile;
  private Editor editor;


  /** Create a new background thread to Save As */
  public class TaskSaveAs extends SwingWorker<Void, Void> {

    @Override
    protected Void doInBackground() throws Exception {
      // a large part of the file copying happens in this background
      // thread

      long totalSize = 0;
      for (File copyable : copyItems) {
        totalSize += calcSize(copyable);
      }

      long progress = 0;
      setProgress(0);
      for (File copyable : ProgressFrame.this.copyItems) {
        // loop to copy over the items that make sense, and to set the
        // current progress

        if (copyable.isDirectory()) {
          copyDir(copyable,
                  new File(ProgressFrame.this.newFolder, copyable.getName()),
                  progress, totalSize);
          progress += calcSize(copyable);
        } else {
          copyFile(copyable,
                   new File(ProgressFrame.this.newFolder, copyable.getName()),
                   progress, totalSize);
          if (calcSize(copyable) < 524288) {
            // If the file length > 0.5MB, the copyFile() function has
            // been redesigned to change progress every 0.5MB so that
            // the progress bar doesn't stagnate during that time
            progress += calcSize(copyable);
            setProgress((int) (progress * 100L / totalSize));
          }
        }
      }
      return null;
    }


    /**
     * Overloaded copyFile that is called whenever a Save As is being done,
     * so that the ProgressBar is updated for very large files as well.
     */
    private void copyFile(File sourceFile, File targetFile,
                          double progress, long totalSize) throws IOException {
      BufferedInputStream from = new BufferedInputStream(new FileInputStream(sourceFile));
      BufferedOutputStream to = new BufferedOutputStream(new FileOutputStream(targetFile));
      byte[] buffer = new byte[16 * 1024];
      int bytesRead;
      int totalRead = 0;
      while ((bytesRead = from.read(buffer)) != -1) {
        to.write(buffer, 0, bytesRead);
        totalRead += bytesRead;
        if (totalRead >= 512 * 1024) {  // to update progress bar every 0.5MB
          progress += totalRead;
          setProgressBarStatus((int) Math.min(Math.ceil(progress * 100.0 / totalSize), 100));
          totalRead = 0;
        }
      }
      if (sourceFile.length() > 512 * 1024) {
        // Update the progress bar one final time if file size is more than 0.5MB,
        //    otherwise, the update is handled either by the copyDir function,
        //    or directly by ProgressFrame.TaskSaveAs.doInBackground()
        progress += totalRead;
        setProgressBarStatus((int) Math.min(Math.ceil(progress * 100.0 / totalSize), 100));
      }
      from.close();
      from = null;
      to.flush();
      to.close();
      to = null;

      targetFile.setLastModified(sourceFile.lastModified());
      targetFile.setExecutable(sourceFile.canExecute());
    }


    private double copyDir(File sourceDir, File targetDir,
                           double progress, long totalSize) throws IOException {
      // Overloaded copyDir so that the Save As progress bar gets updated when the
      //    files are in folders as well (like in the data folder)
      if (sourceDir.equals(targetDir)) {
        final String urDum = "source and target directories are identical";
        throw new IllegalArgumentException(urDum);
      }
      targetDir.mkdirs();
      String files[] = sourceDir.list();
      for (int i = 0; i < files.length; i++) {
        // Ignore dot files (.DS_Store), dot folders (.svn) while copying
        if (files[i].charAt(0) == '.')
          continue;
        //if (files[i].equals(".") || files[i].equals("..")) continue;
        File source = new File(sourceDir, files[i]);
        File target = new File(targetDir, files[i]);
        if (source.isDirectory()) {
          //target.mkdirs();
          progress = copyDir(source, target, progress, totalSize);
          setProgressBarStatus((int) Math.min(Math.ceil(progress * 100.0 / totalSize), 100));
          target.setLastModified(source.lastModified());
        } else {
          copyFile(source, target, progress, totalSize);
          // Update SaveAs progress bar
          progress += source.length();
          setProgressBarStatus((int) Math.min(Math.ceil(progress * 100.0 / totalSize), 100));
        }
      }
      return progress;
    }


    public void setProgressBarStatus(int status) {
      setProgress(status);
    }


    @Override
    public void done() {
      // to close the progress bar automatically when done, and to
      // print that Saving is done in Message Area
      editor.statusNotice(Language.text("editor.status.saving.done"));
      dispose();
    }
  }


  /** Create a new background thread to add a file. */
  public class TaskAddFile extends SwingWorker<Void, Void> {

    @Override
    protected Void doInBackground() throws Exception {
      // a large part of the file copying happens in this background
      // thread

      setProgress(0);

      copyFile(sourceFile, addFile);

      if (addFile.length() < 1024) {
        // If the file length > 1kB, the copyFile() function has
        // been redesigned to change progress every 1kB so that
        // the progress bar doesn't stagnate during that time

        // If file <1 kB, just fill up Progress Bar to 100%
        // directly, since time to copy is now negligable (when
        // perceived by a human, anyway)
        setProgress(100);
      }

      return null;
    }


    /**
     * Overloaded copyFile that is called whenever a addFile is being done,
     * so that the ProgressBar is updated.
     */
    private void copyFile(File sourceFile, File targetFile) throws IOException {
      long totalSize = sourceFile.length();
      int progress = 0;
      BufferedInputStream from = new BufferedInputStream(new FileInputStream(sourceFile));
      BufferedOutputStream to = new BufferedOutputStream(new FileOutputStream(targetFile));
      byte[] buffer = new byte[16 * 1024];
      int bytesRead;
      int totalRead = 0;
      while ((bytesRead = from.read(buffer)) != -1) {
        to.write(buffer, 0, bytesRead);
        totalRead += bytesRead;
        if (totalRead >= 1024) {  // to update progress bar every 1kB
          progress += totalRead;
          setProgressBarStatus((int) Math.min(Math.ceil(progress * 100.0 / totalSize), 100));
          totalRead = 0;
        }
      }
      if (sourceFile.length() > 1024) {
        // Update the progress bar one final time if file size is more than
        // 1kB, otherwise, the update is handled directly by
        // ProgressFrame.TaskAddFile.doInBackground()
        progress += totalRead;
        setProgressBarStatus((int) Math.min(Math.ceil(progress * 100.0 / totalSize), 100));
      }
      from.close();
      from = null;

      to.flush();
      to.close();
      to = null;

      targetFile.setLastModified(sourceFile.lastModified());
      targetFile.setExecutable(sourceFile.canExecute());
    }


    public void setProgressBarStatus(int status) {
      setProgress(status);
    }


    @Override
    public void done() {
      dispose();
    }
  }


  /** Use for Save As */
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
    label = new JLabel("Saving " + oldName + " as " + newName + "...");
    label.setBounds(40, 20, 300, 20);

    progressBar = new JProgressBar(0, 100);
    progressBar.setValue(0);
    progressBar.setBounds(40, 50, 300, 30);
    progressBar.setStringPainted(true);

    panel.add(progressBar);
    panel.add(label);
    Toolkit.setIcon(this);
    this.setVisible(true);

    // create an instance of TaskSaveAs and run execute() on this
    // instance to start background thread
    TaskSaveAs t = new TaskSaveAs();
    t.addPropertyChangeListener(this);
    t.execute();
  }


  /** Used for Add File */
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
    label = new JLabel("Adding " + addFile.getName());
    label.setBounds(40, 20, 300, 20);

    progressBar = new JProgressBar(0, 100);
    progressBar.setValue(0);
    progressBar.setBounds(40, 50, 300, 30);
    progressBar.setStringPainted(true);

    panel.add(progressBar);
    panel.add(label);
    Toolkit.setIcon(this);
    this.setVisible(true);

    // create an instance of TaskAddFile and run execute() on this
    // instance to
    // start background thread
    TaskAddFile task = new TaskAddFile();
    task.addPropertyChangeListener(this);
    task.execute();
  }


  /**
   * Function to return the length of the file, or entire directory, including
   * the component files and sub-folders if passed.
   */
  long calcSize(File file) {
    return file.isFile() ? file.length() : Util.calcFolderSize(file);
  }


  /**
   * Detects a change in the property of the background task,
   * i.e., is called when the size of files already copied changes.
   */
  public void propertyChange(PropertyChangeEvent evt) {
    if ("progress".equals(evt.getPropertyName())) {
      progressBar.setValue((Integer) evt.getNewValue());
    }
  }
}

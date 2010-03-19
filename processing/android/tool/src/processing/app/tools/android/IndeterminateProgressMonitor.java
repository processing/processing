package processing.app.tools.android;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.HeadlessException;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.UIManager;

class IndeterminateProgressMonitor {
  private JDialog dialog;
  private JOptionPane pane;
  private JProgressBar progressBar;
  private final JLabel noteLabel;
  private Object[] cancelOption = null;

  IndeterminateProgressMonitor(final Component parentComponent,
                               final Object message, final String note) {

    cancelOption = new Object[] { UIManager
        .getString("OptionPane.cancelButtonText") };
    progressBar = new JProgressBar();
    progressBar.setIndeterminate(true);
    noteLabel = new JLabel(note);
    pane = new IndefiniteProgressOptionPane(message, noteLabel, progressBar);
    dialog = pane.createDialog(parentComponent, "Progress");
    dialog.setVisible(true);
  }

  @SuppressWarnings("serial")
  private class IndefiniteProgressOptionPane extends JOptionPane {
    IndefiniteProgressOptionPane(final Object... messageList) {
      super(messageList, JOptionPane.INFORMATION_MESSAGE,
            JOptionPane.DEFAULT_OPTION, null,
            IndeterminateProgressMonitor.this.cancelOption, null);
      setPreferredSize(new Dimension(getPreferredSize().width + 80,
                                     getPreferredSize().height));
    }

    @Override
    public int getMaxCharactersPerLineCount() {
      return 60;
    }

    @Override
    public JDialog createDialog(final Component parentComponent,
                                final String title) throws HeadlessException {
      final JDialog d = super.createDialog(parentComponent, title);
      d.setModal(false);
      return d;
    }
  }

  public void close() {
    if (dialog != null) {
      dialog.setVisible(false);
      dialog.dispose();
      dialog = null;
      pane = null;
      progressBar = null;
    }
  }

  public boolean isCanceled() {
    final Object v = pane.getValue();
    return ((v != null) && (cancelOption.length == 1) && (v
        .equals(cancelOption[0])));
  }

  public void setNote(final String note) {
    noteLabel.setText(note);
  }

}

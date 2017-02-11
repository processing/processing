/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2013-16 The Processing Foundation
  Copyright (c) 2011-12 Ben Fry and Casey Reas

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License version 2
  as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License along
  with this program; if not, write to the Free Software Foundation, Inc.
  59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
package processing.app.contrib;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Date;
import java.text.DateFormat;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;

import processing.app.Base;
import processing.app.Language;
import processing.app.Messages;
import processing.app.Platform;
import processing.app.ui.Editor;
import processing.app.ui.Toolkit;


/**
 * Panel that expands and gives a brief overview of a library when clicked.
 */
class DetailPanel extends JPanel {
  static public final String REMOVE_RESTART_MESSAGE =
    String.format("<i>%s</i>", Language.text("contrib.messages.remove_restart"));

  static public final String INSTALL_RESTART_MESSAGE =
    String.format("<i>%s</i>", Language.text("contrib.messages.install_restart"));

  static public final String UPDATE_RESTART_MESSAGE =
    String.format("<i>%s</i>", Language.text("contrib.messages.update_restart"));

  static public final String PROGRESS_BAR_CONSTRAINT = "Install/Remove Progress Bar Panel";

  static public final String BUTTON_CONSTRAINT = "Install/Remove Button Panel";

  static public final String INCOMPATIBILITY_BLUR = "This contribution is not compatible with "
    + "the current revision of Processing";

  private final ListPanel listPanel;
  private final ContributionListing contribListing = ContributionListing.getInstance();

  static final int BUTTON_WIDTH = Toolkit.zoom(100);
  static Icon foundationIcon;

  /**
   * Should only be set through setContribution(),
   * otherwise UI components will not be updated.
   */
  private Contribution contrib;

  public Contribution getContrib() {
    return contrib;
  }

  private LocalContribution getLocalContrib() {
    return (LocalContribution) contrib;
  }

  private boolean alreadySelected;
  private boolean enableHyperlinks;
  //private HyperlinkListener conditionalHyperlinkOpener;
  private JTextPane descriptionPane;
  private JLabel notificationLabel;
  private JButton updateButton;
  JProgressBar installProgressBar;
  private JButton installRemoveButton;
  private JPopupMenu contextMenu;
  private JMenuItem openFolder;

  private JPanel barButtonCardPane;
  private CardLayout barButtonCardLayout;

  static private final String installText = Language.text("contrib.install");
  static private final String removeText = Language.text("contrib.remove");
  static private final String undoText = Language.text("contrib.undo");

  boolean updateInProgress;
  boolean installInProgress;
  boolean removeInProgress;

  String description;


  DetailPanel(ListPanel contributionListPanel) {
    if (foundationIcon == null) {
      foundationIcon = Toolkit.getLibIconX("icons/foundation", 32);
    }

    listPanel = contributionListPanel;
    barButtonCardPane = new JPanel();

    contextMenu = new JPopupMenu();
    openFolder = new JMenuItem("Open Folder");
    openFolder.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (contrib instanceof LocalContribution) {
          File folder = ((LocalContribution) contrib).getFolder();
          Platform.openFolder(folder);
        }
      }
    });

    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

    addPaneComponents();

    setBackground(listPanel.getBackground());
    setOpaque(true);
    setSelected(false);

    setExpandListener(this, new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
        if (contrib.isCompatible(Base.getRevision())) {
          listPanel.setSelectedPanel(DetailPanel.this);
        } else {
          setErrorMessage(contrib.getName() +
                          " cannot be used with this version of Processing");
        }
      }
    });
  }


  /**
   * Create the widgets for the header panel which is visible when the
   * library panel is not clicked.
   */
  private void addPaneComponents() {
    setLayout(new BorderLayout());

    descriptionPane = new JTextPane();
    descriptionPane.setInheritsPopupMenu(true);
    descriptionPane.setEditable(false);  // why would this ever be true?
    Insets margin = descriptionPane.getMargin();
    margin.bottom = 0;
    descriptionPane.setMargin(margin);
    descriptionPane.setContentType("text/html");
    setTextStyle(descriptionPane, "0.95em");
    descriptionPane.setOpaque(false);
    if (UIManager.getLookAndFeel().getID().equals("Nimbus")) {
      descriptionPane.setBackground(new Color(0, 0, 0, 0));
    }

    descriptionPane.setBorder(new EmptyBorder(4, 7, 7, 7));
    descriptionPane.setHighlighter(null);
    descriptionPane.addHyperlinkListener(new HyperlinkListener() {
      public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
          // for 3.2.3, added the isSelected() prompt here, rather than
          // adding/removing the listener repeatedly
          if (isSelected()) {
            if (enableHyperlinks && e.getURL() != null) {
              Platform.openURL(e.getURL().toString());
            }
          }
        }
      }
    });

    add(descriptionPane, BorderLayout.CENTER);

    JPanel updateBox = new JPanel();
    updateBox.setLayout(new BorderLayout());

    notificationLabel = new JLabel();
    notificationLabel.setInheritsPopupMenu(true);
    notificationLabel.setVisible(false);
    notificationLabel.setOpaque(false);
    notificationLabel.setFont(ManagerFrame.SMALL_PLAIN);

    {
      updateButton = new JButton("Update");
      updateButton.setInheritsPopupMenu(true);
      Dimension dim =
        new Dimension(BUTTON_WIDTH, updateButton.getPreferredSize().height);
      updateButton.setMinimumSize(dim);
      updateButton.setPreferredSize(dim);
      updateButton.setOpaque(false);
      updateButton.setVisible(false);

      updateButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          update();
        }
      });
    }

    updateBox.add(updateButton, BorderLayout.EAST);
    updateBox.add(notificationLabel, BorderLayout.WEST);
    updateBox.setBorder(new EmptyBorder(4, 7, 7, 7));
    updateBox.setOpaque(false);
    add(updateBox, BorderLayout.SOUTH);

    JPanel rightPane = new JPanel();
    rightPane.setInheritsPopupMenu(true);
    rightPane.setOpaque(false);
    rightPane.setLayout(new BoxLayout(rightPane, BoxLayout.Y_AXIS));
    rightPane.setMinimumSize(new Dimension(BUTTON_WIDTH, 1));
    add(rightPane, BorderLayout.EAST);

    barButtonCardLayout = new CardLayout();
    barButtonCardPane.setLayout(barButtonCardLayout);
    barButtonCardPane.setInheritsPopupMenu(true);
    barButtonCardPane.setOpaque(false);
    barButtonCardPane.setMinimumSize(new Dimension(BUTTON_WIDTH, 1));

    {
      installProgressBar = new JProgressBar();
      installProgressBar.setInheritsPopupMenu(true);
      installProgressBar.setStringPainted(true);
      resetInstallProgressBarState();
      Dimension dim =
        new Dimension(BUTTON_WIDTH,
                      installProgressBar.getPreferredSize().height);
      installProgressBar.setPreferredSize(dim);
      installProgressBar.setMaximumSize(dim);
      installProgressBar.setMinimumSize(dim);
      installProgressBar.setOpaque(false);
      installProgressBar.setAlignmentX(CENTER_ALIGNMENT);
    }

    installRemoveButton = new JButton(" ");
    installRemoveButton.setInheritsPopupMenu(true);
    installRemoveButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String mode = installRemoveButton.getText();
        if (mode.equals(installText)) {
          install();
        } else if (mode.equals(removeText)) {
          remove();
        } else if (mode.equals(undoText)) {
          undo();
        }
      }
    });

    Dimension installButtonDimensions = installRemoveButton.getPreferredSize();
    installButtonDimensions.width = BUTTON_WIDTH;
    installRemoveButton.setPreferredSize(installButtonDimensions);
    installRemoveButton.setMaximumSize(installButtonDimensions);
    installRemoveButton.setMinimumSize(installButtonDimensions);
    installRemoveButton.setOpaque(false);
    installRemoveButton.setAlignmentX(CENTER_ALIGNMENT);

    JPanel barPane = new JPanel();
    barPane.setOpaque(false);

    JPanel buttonPane = new JPanel();
    buttonPane.setOpaque(false);
    buttonPane.add(installRemoveButton);

    barButtonCardPane.add(buttonPane, BUTTON_CONSTRAINT);
    barButtonCardPane.add(barPane, PROGRESS_BAR_CONSTRAINT);
    barButtonCardLayout.show(barButtonCardPane, BUTTON_CONSTRAINT);

    rightPane.add(barButtonCardPane);

    // Set the minimum size of this pane to be the sum of the height of the
    // progress bar and install button
    Dimension dim =
      new Dimension(BUTTON_WIDTH,
                    installRemoveButton.getPreferredSize().height);
    rightPane.setMinimumSize(dim);
    rightPane.setPreferredSize(dim);
  }


  private void reorganizePaneComponents() {
    BorderLayout layout = (BorderLayout) this.getLayout();
    remove(layout.getLayoutComponent(BorderLayout.SOUTH));
    remove(layout.getLayoutComponent(BorderLayout.EAST));

    JPanel updateBox = new JPanel();
    updateBox.setLayout(new BorderLayout());
    updateBox.setInheritsPopupMenu(true);
    updateBox.add(notificationLabel, BorderLayout.WEST);
    updateBox.setBorder(new EmptyBorder(4, 7, 7, 7));
    updateBox.setOpaque(false);
    add(updateBox, BorderLayout.SOUTH);

    JPanel rightPane = new JPanel();
    rightPane.setInheritsPopupMenu(true);
    rightPane.setOpaque(false);
    rightPane.setLayout(new BoxLayout(rightPane, BoxLayout.Y_AXIS));
    rightPane.setMinimumSize(new Dimension(BUTTON_WIDTH, 1));
    add(rightPane, BorderLayout.EAST);

    if (updateButton.isVisible() && !removeInProgress && !contrib.isDeletionFlagged()) {
      JPanel updateRemovePanel = new JPanel();
      updateRemovePanel.setLayout(new FlowLayout());
      updateRemovePanel.setOpaque(false);
      updateRemovePanel.add(updateButton);
      updateRemovePanel.setInheritsPopupMenu(true);
      updateRemovePanel.add(installRemoveButton);
      updateBox.add(updateRemovePanel, BorderLayout.EAST);

      JPanel barPane = new JPanel();
      barPane.setOpaque(false);
      barPane.setInheritsPopupMenu(true);
      rightPane.add(barPane);

      if (updateInProgress) {
        barButtonCardLayout.show(barButtonCardPane, PROGRESS_BAR_CONSTRAINT);
      }
    } else {
      updateBox.add(updateButton, BorderLayout.EAST);
      barButtonCardPane.removeAll();

      JPanel barPane = new JPanel();
      barPane.setOpaque(false);
      barPane.setInheritsPopupMenu(true);

      JPanel buttonPane = new JPanel();
      buttonPane.setOpaque(false);
      buttonPane.setInheritsPopupMenu(true);
      buttonPane.add(installRemoveButton);

      barButtonCardPane.add(buttonPane, BUTTON_CONSTRAINT);
      barButtonCardPane.add(barPane, PROGRESS_BAR_CONSTRAINT);
      if (installInProgress || removeInProgress || updateInProgress) {
        barButtonCardLayout.show(barButtonCardPane, PROGRESS_BAR_CONSTRAINT);
      } else {
        barButtonCardLayout.show(barButtonCardPane, BUTTON_CONSTRAINT);
      }
      rightPane.add(barButtonCardPane);
    }

    Dimension progressDim = installProgressBar.getPreferredSize();
    Dimension installDim = installRemoveButton.getPreferredSize();
    progressDim.width = BUTTON_WIDTH;
    progressDim.height = Math.max(progressDim.height, installDim.height);
    rightPane.setMinimumSize(progressDim);
    rightPane.setPreferredSize(progressDim);
  }


  private void setExpandListener(Component component,
                                 MouseListener expandListener) {
    // If it's a JButton, adding the listener will make this stick on OS X
    // https://github.com/processing/processing/issues/3172
    if (!(component instanceof JButton)) {
      component.addMouseListener(expandListener);
      if (component instanceof Container) {
        for (Component child : ((Container) component).getComponents()) {
          setExpandListener(child, expandListener);
        }
      }
    }
  }


  private void blurContributionPanel(Component component) {
    component.setFocusable(false);
    component.setEnabled(false);
    if (component instanceof JComponent) {
      ((JComponent) component).setToolTipText(INCOMPATIBILITY_BLUR);
    }
    if (component instanceof Container) {
      for (Component child : ((Container) component).getComponents()) {
        blurContributionPanel(child);
      }
    }
  }


  public void setContribution(Contribution contrib) {
    this.contrib = contrib;

    if (contrib.isSpecial()) {
      JLabel iconLabel = new JLabel(foundationIcon);
      iconLabel.setBorder(new EmptyBorder(4, 7, 7, 7));
      iconLabel.setVerticalAlignment(SwingConstants.TOP);
      add(iconLabel, BorderLayout.WEST);
    }

    // Avoid ugly synthesized bold
    Font boldFont = ManagerFrame.SMALL_BOLD;
    String fontFace = "<font face=\"" + boldFont.getName() + "\">";

    StringBuilder desc = new StringBuilder();
    desc.append("<html><body>" + fontFace);
    if (contrib.getUrl() == null) {
      desc.append(contrib.getName());
    } else {
      desc.append("<a href=\"" + contrib.getUrl() + "\">" + contrib.getName() + "</a>");
    }
    desc.append("</font> ");

    String prettyVersion = contrib.getPrettyVersion();
    if (prettyVersion != null) {
      desc.append(prettyVersion);
    }
    desc.append(" <br/>");

    String authorList = contrib.getAuthorList();
    if (authorList != null && !authorList.isEmpty()) {
      desc.append(toHtmlLinks(contrib.getAuthorList()));
    }
    desc.append("<br/><br/>");

    if (contrib.isDeletionFlagged()) {
      desc.append(REMOVE_RESTART_MESSAGE);
    } else if (contrib.isRestartFlagged()) {
      desc.append(INSTALL_RESTART_MESSAGE);
    } else if (contrib.isUpdateFlagged()) {
      desc.append(UPDATE_RESTART_MESSAGE);
    } else {
      String sentence = contrib.getSentence();
      if (sentence == null || sentence.isEmpty()) {
        sentence = String.format("<i>%s</i>", Language.text("contrib.errors.description_unavailable"));
      } else {
        sentence = sanitizeHtmlTags(sentence);
        sentence = toHtmlLinks(sentence);
      }
      desc.append(sentence);
    }

    long lastUpdatedUTC = contrib.getLastUpdated();
    if (lastUpdatedUTC != 0) {
      DateFormat dateFormatter = DateFormat.getDateInstance(DateFormat.MEDIUM);
      Date lastUpdatedDate = new Date(lastUpdatedUTC);
      if (prettyVersion != null) {
        desc.append(", ");
      }
      desc.append("Last Updated on " + dateFormatter.format(lastUpdatedDate));
    }

    desc.append("</body></html>");
    description = desc.toString();
    descriptionPane.setText(description);

    if (contribListing.hasUpdates(contrib) && contrib.isCompatible(Base.getRevision())) {
      StringBuilder versionText = new StringBuilder();
      versionText.append("<html><body><i>");
      if (contrib.isUpdateFlagged() || contrib.isDeletionFlagged()) {
        // Already marked for deletion, see requiresRestart() notes below.
        // versionText.append("To finish an update, reinstall this contribution after restarting.");
        ;
      } else {
        String latestVersion = contribListing.getLatestPrettyVersion(contrib);
        if (latestVersion != null) {
          versionText.append("New version (" + latestVersion + ") available.");
        } else {
          versionText.append("New version available.");
        }
      }
      versionText.append("</i></body></html>");
      notificationLabel.setText(versionText.toString());
      notificationLabel.setVisible(true);
    } else {
      notificationLabel.setText("");
      notificationLabel.setVisible(false);
    }

    updateButton.setEnabled(true);
    if (contrib != null) {
      updateButton.setVisible((contribListing.hasUpdates(contrib) && !contrib.isUpdateFlagged() && !contrib.isDeletionFlagged()) || updateInProgress);
    }

    if (contrib.isDeletionFlagged()) {
      installRemoveButton.setText(undoText);

    } else if (contrib.isInstalled()) {
      installRemoveButton.setText(removeText);
      installRemoveButton.setVisible(true);
      installRemoveButton.setEnabled(!contrib.isUpdateFlagged());
      reorganizePaneComponents();
    } else {
      installRemoveButton.setText(installText);
    }

    contextMenu.removeAll();
    if (contrib.isInstalled()) {
      contextMenu.add(openFolder);
      setComponentPopupMenu(contextMenu);
    } else {
      setComponentPopupMenu(null);
    }

    if (!contrib.isCompatible(Base.getRevision())) {
      blurContributionPanel(this);
    }
  }


  private void installContribution(AvailableContribution info) {
    if (info.link == null) {
      setErrorMessage(Language.interpolate("contrib.unsupported_operating_system", info.getType()));
    } else {
      installContribution(info, info.link);
    }
  }


  private void finishInstall(boolean error) {
    resetInstallProgressBarState();
    installRemoveButton.setEnabled(!contrib.isUpdateFlagged());

    if (error) {
      setErrorMessage(Language.text("contrib.download_error"));
    }
    barButtonCardLayout.show(barButtonCardPane, BUTTON_CONSTRAINT);
    installInProgress = false;
    if (updateInProgress) {
      updateInProgress = false;
    }
    updateButton.setVisible(contribListing.hasUpdates(contrib) && !contrib.isUpdateFlagged());
    setSelected(true);
  }


  private void installContribution(AvailableContribution ad, String url) {
    installRemoveButton.setEnabled(false);

    try {
      URL downloadUrl = new URL(url);
      installProgressBar.setVisible(true);

      ContribProgressBar downloadProgress = new ContribProgressBar(installProgressBar) {
        public void finishedAction() {
          // nothing?
        }

        public void cancelAction() {
          finishInstall(false);
        }
      };

      ContribProgressBar installProgress = new ContribProgressBar(installProgressBar) {
        public void finishedAction() {
          finishInstall(isError());
        }

        public void cancelAction() {
          finishedAction();
        }
      };

      ContributionManager.downloadAndInstall(getBase(), downloadUrl, ad,
                                             downloadProgress, installProgress,
                                             getStatusPanel());

    } catch (MalformedURLException e) {
      Messages.showWarning(Language.text("contrib.errors.install_failed"),
                           Language.text("contrib.errors.malformed_url"), e);
      // not sure why we'd re-enable the button if it had an error...
      //installRemoveButton.setEnabled(true);
    }
  }


  protected void resetInstallProgressBarState() {
    installProgressBar.setString(Language.text("contrib.progress.starting"));
    installProgressBar.setIndeterminate(false);
    installProgressBar.setValue(0);
    installProgressBar.setVisible(false);
  }


  /*
  static final HyperlinkListener NULL_HYPERLINK_LISTENER = new HyperlinkListener() {
    public void hyperlinkUpdate(HyperlinkEvent e) { }
  };
  */


  /**
   * Should be called whenever this component is selected (clicked on)
   * or unselected, even if it is already selected.
   */
  public void setSelected(boolean isSelected) {
    // Only enable hyperlinks if this component is already selected.
    // Why? Because otherwise if the user happened to click on what is
    // now a hyperlink, it will be opened as the mouse is released.
    enableHyperlinks = alreadySelected;

    if (contrib != null) {
      updateButton.setVisible((contribListing.hasUpdates(contrib) && !contrib.isUpdateFlagged() && !contrib.isDeletionFlagged()) || updateInProgress);
      updateButton.setEnabled(!contribListing.hasListDownloadFailed());
    }
    installRemoveButton.setVisible(isSelected() || installRemoveButton.getText().equals(Language.text("contrib.remove")) || updateInProgress);
    installRemoveButton.setEnabled(installRemoveButton.getText().equals(Language.text("contrib.remove")) ||!contribListing.hasListDownloadFailed());
    reorganizePaneComponents();

    /*
    descriptionPane.removeHyperlinkListener(NULL_HYPERLINK_LISTENER);
    descriptionPane.removeHyperlinkListener(conditionalHyperlinkOpener);
    if (isSelected()) {
      descriptionPane.addHyperlinkListener(conditionalHyperlinkOpener);
//      descriptionPane.setEditable(false);
    } else {
      descriptionPane.addHyperlinkListener(NULL_HYPERLINK_LISTENER);
//      descriptionPane.setEditable(true);
    }
    */

    // Update style of hyperlinks
    setSelectionStyle(descriptionPane, isSelected());

    alreadySelected = isSelected();
  }


  public boolean isSelected() {
    return listPanel.getSelectedPanel() == this;
  }


  public void setForeground(Color fg) {
    super.setForeground(fg);

    if (contrib != null) {
      boolean installed = contrib.isInstalled();
      setForegroundStyle(descriptionPane, installed, isSelected());
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  static String sanitizeHtmlTags(String stringIn) {
    stringIn = stringIn.replaceAll("<", "&lt;");
    stringIn = stringIn.replaceAll(">", "&gt;");
    return stringIn;
  }


  /**
   * This has a [link](http://example.com/) in [it](http://example.org/).
   *
   * Becomes...
   *
   * This has a <a href="http://example.com/">link</a> in <a
   * href="http://example.org/">it</a>.
   */
  static String toHtmlLinks(String stringIn) {
    Pattern p = Pattern.compile("\\[(.*?)\\]\\((.*?)\\)");
    Matcher m = p.matcher(stringIn);

    StringBuilder sb = new StringBuilder();

    int start = 0;
    while (m.find(start)) {
      sb.append(stringIn.substring(start, m.start()));

      String text = m.group(1);
      String url = m.group(2);

      sb.append("<a href=\"");
      sb.append(url);
      sb.append("\">");
      sb.append(text);
      sb.append("</a>");

      start = m.end();
    }
    sb.append(stringIn.substring(start));
    return sb.toString();
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Sets coloring based on whether installed or not;
   * also makes ugly blue HTML links into the specified color (black).
   */
  static void setForegroundStyle(JTextPane textPane,
                                 boolean installed, boolean selected) {
    Document doc = textPane.getDocument();
    if (doc instanceof HTMLDocument) {
      HTMLDocument html = (HTMLDocument) doc;
      StyleSheet stylesheet = html.getStyleSheet();

      // slightly grayed when installed
      String c = (installed && !selected) ? "#555555" : "#000000";
      stylesheet.addRule("body { color:" + c + "; }");
      stylesheet.addRule("a { color:" + c + "; }");
    }
  }


  static void setTextStyle(JTextPane textPane, String fontSize) {
    Document doc = textPane.getDocument();
    if (doc instanceof HTMLDocument) {
      HTMLDocument html = (HTMLDocument) doc;
      StyleSheet stylesheet = html.getStyleSheet();
      stylesheet.addRule("body { " +
                         "  margin: 0; padding: 0;" +
                         "  font-family: " + Toolkit.getSansFontName() + ", Arial, Helvetica, sans-serif;" +
                         "  font-size: 100%;" + "font-size: " + fontSize + "; " +
                         "}");
    }
  }


  static void setSelectionStyle(JTextPane textPane, boolean selected) {
    Document doc = textPane.getDocument();
    if (doc instanceof HTMLDocument) {
      HTMLDocument html = (HTMLDocument) doc;
      StyleSheet styleSheet = html.getStyleSheet();
      if (selected) {
        styleSheet.addRule("a { text-decoration:underline } ");
      } else {
        styleSheet.addRule("a { text-decoration:none }");
      }
    }
  }


  public void install() {
    clearStatusMessage();
    installInProgress = true;
    barButtonCardLayout.show(barButtonCardPane, PROGRESS_BAR_CONSTRAINT);
    if (contrib instanceof AvailableContribution) {
      installContribution((AvailableContribution) contrib);
      contribListing.replaceContribution(contrib, contrib);
    }
  }


  public void update() {
    clearStatusMessage();
    updateInProgress = true;
    if (contrib.getType().requiresRestart()) {
      installRemoveButton.setEnabled(false);
      installProgressBar.setVisible(true);
      installProgressBar.setIndeterminate(true);

      ContribProgressBar progress = new UpdateProgressBar(installProgressBar);
      getLocalContrib().removeContribution(getBase(), progress, getStatusPanel());
    } else {
      updateButton.setEnabled(false);
      installRemoveButton.setEnabled(false);
      AvailableContribution ad =
        contribListing.getAvailableContribution(contrib);
      installContribution(ad, ad.link);
    }
  }


  class UpdateProgressBar extends ContribProgressBar {
    public UpdateProgressBar(JProgressBar progressBar) {
      super(progressBar);
    }

    public void finishedAction() {
      resetInstallProgressBarState();
      updateButton.setEnabled(false);
      AvailableContribution ad =
        contribListing.getAvailableContribution(contrib);
      String url = ad.link;
      installContribution(ad, url);
    }

    @Override
    public void cancelAction() {
      resetInstallProgressBarState();
      //listPanel.contributionTab.statusPanel.setMessage("");  // same as clear?
      clearStatusMessage();
      updateInProgress = false;
      installRemoveButton.setEnabled(true);
      if (contrib.isDeletionFlagged()) {
        getLocalContrib().setUpdateFlag(true);
        getLocalContrib().setDeletionFlag(false);
        contribListing.replaceContribution(contrib, contrib);
      }

      if (isModeActive(contrib)) {
        updateButton.setEnabled(true);
      } else {
        // TODO: remove or uncomment if the button was added
        //listPanel.contributionTab.restartButton.setVisible(true);
      }
    }
  }


  public void remove() {
    clearStatusMessage();
    if (contrib.isInstalled() && contrib instanceof LocalContribution) {
      removeInProgress = true;
      barButtonCardLayout.show(barButtonCardPane, PROGRESS_BAR_CONSTRAINT);
      updateButton.setEnabled(false);
      installRemoveButton.setEnabled(false);
      installProgressBar.setVisible(true);
      installProgressBar.setIndeterminate(true);

      ContribProgressBar monitor = new RemoveProgressBar(installProgressBar);
      getLocalContrib().removeContribution(getBase(), monitor, getStatusPanel());
    }
  }


  class RemoveProgressBar extends ContribProgressBar {
    public RemoveProgressBar(JProgressBar progressBar) {
      super(progressBar);
    }

    private void preAction() {
      resetInstallProgressBarState();
      removeInProgress = false;
      installRemoveButton.setEnabled(true);
      reorganizePaneComponents();
      setSelected(true); // Needed for smooth working. Dunno why, though...
    }

    public void finishedAction() {
      // Finished uninstalling the library
      preAction();
    }

    public void cancelAction() {
      preAction();

      if (isModeActive(contrib)) {
        updateButton.setEnabled(true);
      } else {
        // TODO: remove or uncomment if the button was added
        //contributionTab.restartButton.setVisible(true);
      }
    }
  }


  private void undo() {
    clearStatusMessage();
    if (contrib instanceof LocalContribution) {
      LocalContribution installed = getLocalContrib();
      installed.setDeletionFlag(false);
      contribListing.replaceContribution(contrib, contrib);  // ??
      Iterator<Contribution> contribsListIter = contribListing.allContributions.iterator();
      boolean toBeRestarted = false;
      while (contribsListIter.hasNext()) {
        Contribution contribElement = contribsListIter.next();
        if (contrib.getType().equals(contribElement.getType())) {
          if (contribElement.isDeletionFlagged() ||
            contribElement.isUpdateFlagged()) {
            toBeRestarted = !toBeRestarted;
            break;
          }
        }
      }
      // TODO: remove or uncomment if the button was added
      //listPanel.contributionTab.restartButton.setVisible(toBeRestarted);
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  // Can't be called from the constructor because the path isn't set all the
  // way down. However, it's not Base changes over time. More importantly,
  // though, is that the functions being called in Base are somewhat suspect
  // since they're contribution-related, and should perhaps live closer.
  private Base getBase() {
    return listPanel.contributionTab.editor.getBase();
  }


  private boolean isModeActive(Contribution contrib) {
    if (contrib.getType() == ContributionType.MODE) {
      ModeContribution m = (ModeContribution) contrib;
      for (Editor e : getBase().getEditors()) {
        if (e.getMode().equals(m.getMode())) {
          return true;
        }
      }
    }
    return false;
  }


  private StatusPanel getStatusPanel() {
    return listPanel.contributionTab.statusPanel;  // TODO this is gross [fry]
  }


  private void clearStatusMessage() {
    getStatusPanel().clearMessage();
  }


  private void setErrorMessage(String message) {
    getStatusPanel().setErrorMessage(message);
  }
}

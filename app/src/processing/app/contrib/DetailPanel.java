/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2013-15 The Processing Foundation
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.text.DateFormat;

import javax.swing.*;

import processing.app.Base;
import processing.app.Language;
import processing.app.Messages;
import processing.app.ui.Editor;
import processing.app.ui.Toolkit;


// TODO clean up accessors (too many cases of several de-references for basic tasks
// TODO hyperlink listener seems far too complicated for what it does,
//      and why have a 'null' version rather than detecting whether selected or not
// TODO don't add/remove listeners for install/remove/undo based on function,
//      just keep track of current behavior and call that. too many things can go wrong.
// TODO get rid of huge actionPerformed() blocks with anonymous classes,
//      just make handleInstall(), etc methods and a single actionPerformed
//      for the button that calls the necessary behavior (see prev note)

/**
 * Panel that expands and gives a brief overview of a library when clicked.
 */
class DetailPanel {
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

  /**
   * Should only be set through setContribution(),
   * otherwise UI components will not be updated.
   */
  private Contribution contrib;

  public Contribution getContrib() {
    return contrib;
  }

  JProgressBar installProgressBar;
  
  boolean updateInProgress;
  boolean installInProgress;
  boolean removeInProgress;

  String description;


  DetailPanel(ListPanel contributionListPanel) {
    listPanel = contributionListPanel;
    installProgressBar = new JProgressBar();
    installProgressBar.setInheritsPopupMenu(true);
    installProgressBar.setStringPainted(true);
    resetInstallProgressBarState();
    installProgressBar.setOpaque(false);
  }



  public void setContribution(Contribution contrib) {
    this.contrib = contrib;

    // Avoid ugly synthesized bold
    Font boldFont = Toolkit.getSansFont(12, Font.BOLD);
    String fontFace = "<font face=\"" + boldFont.getName() + "\">";

    StringBuilder desc = new StringBuilder();
    desc.append("<html><body>" + fontFace);
    if (contrib.getUrl() == null) {
      desc.append(contrib.getName());
    } else {
      desc.append("<a href=\"" + contrib.getUrl() + "\">" + contrib.getName() + "</a>");
    }
    desc.append("</font> ");

    String version = contrib.getPrettyVersion();
    if (version != null) {
      desc.append(version);
    }
    desc.append(" <br/>");

    String authorList = contrib.getAuthorList();
    if (authorList != null && !authorList.isEmpty()) {
      desc.append(Toolkit.toHtmlLinks(contrib.getAuthorList()));
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
        sentence = Toolkit.sanitizeHtmlTags(sentence);
        sentence = Toolkit.toHtmlLinks(sentence);
      }
      desc.append(sentence);
    }

    long lastUpdatedUTC = contrib.getLastUpdated();
    if (lastUpdatedUTC != 0) {
      DateFormat dateFormatter = DateFormat.getDateInstance(DateFormat.MEDIUM);
      Date lastUpdatedDate = new Date(lastUpdatedUTC);
      if (version != null && !version.isEmpty())
        desc.append(", ");
      desc.append("Last Updated on " + dateFormatter.format(lastUpdatedDate));
    }

    desc.append("</body></html>");
    description = desc.toString();

    if (contribListing.hasUpdates(contrib) && contrib.isCompatible(Base.getRevision())) {
      StringBuilder versionText = new StringBuilder();
      versionText.append("<html><body><i>");
      if (contrib.isUpdateFlagged() || contrib.isDeletionFlagged()) {
        // Already marked for deletion, see requiresRestart() notes below.
        // versionText.append("To finish an update, reinstall this contribution after restarting.");
        ;
      } else {
        String latestVersion = contribListing.getLatestVersion(contrib);
        if (latestVersion != null) {
          versionText.append("New version (" + latestVersion + ") available.");
        } else {
          versionText.append("New version available.");
        }
      }
      versionText.append("</i></body></html>");
    }
  }

  private void installContribution(AvailableContribution info) {
    if (info.link == null) {
      listPanel.contributionTab.statusPanel.setErrorMessage(Language.interpolate("contrib.unsupported_operating_system", info.getType()));
    } else {
      installContribution(info, info.link);
    }
  }


  private void installContribution(AvailableContribution ad, String url) {

    try {
      URL downloadUrl = new URL(url);
      installProgressBar.setVisible(true);

      ContribProgressBar downloadProgress = new ContribProgressBar(installProgressBar) {
        public void finishedAction() {
          // Finished downloading library
        }

        public void cancelAction() {
          // Finished installing library
          resetInstallProgressBarState();

          installInProgress = false;
          if(updateInProgress)
            updateInProgress = !updateInProgress;
        }
      };

      ContribProgressBar installProgress = new ContribProgressBar(installProgressBar) {
        public void finishedAction() {
          // Finished installing library
          resetInstallProgressBarState();

          if (isError()) {
            listPanel.contributionTab.statusPanel.setErrorMessage(Language.text("contrib.download_error"));
          }
          installInProgress = false;
          if(updateInProgress)
            updateInProgress = !updateInProgress;
        }

        public void cancelAction() {
          finishedAction();
        }
      };

      ContributionManager.downloadAndInstall(listPanel.contributionTab.editor.getBase(),
                                             downloadUrl, ad,
                                             downloadProgress, installProgress,
                                             listPanel.contributionTab.statusPanel);

    } catch (MalformedURLException e) {
      Messages.showWarning(Language.text("contrib.errors.install_failed"),
                           Language.text("contrib.errors.malformed_url"), e);
      // not sure why we'd re-enable the button if it had an error...
//      installRemoveButton.setEnabled(true);
    }
  }


  // This doesn't actually seem to work?
  /*
  static void stripTextSelectionListeners(JEditorPane editorPane) {
    for (MouseListener listener : editorPane.getMouseListeners()) {
      String className = listener.getClass().getName();
      if (className.endsWith("MutableCaretEvent") ||
          className.endsWith("DragListener") ||
          className.endsWith("BasicCaret")) {
        editorPane.removeMouseListener(listener);
      }
    }
  }
  */


  protected void resetInstallProgressBarState() {
    installProgressBar.setString(Language.text("contrib.progress.starting"));
    installProgressBar.setIndeterminate(false);
    installProgressBar.setValue(0);
    installProgressBar.setVisible(false);
  }


  public boolean isSelected() {
    return listPanel.getSelectedPanel() == this;
  }

  public void install() {
    listPanel.contributionTab.statusPanel.clearMessage();
    installInProgress = true;
    if (contrib instanceof AvailableContribution) {
      installContribution((AvailableContribution) contrib);
      contribListing.replaceContribution(contrib, contrib);
    }
  }


  public void update() {

    listPanel.contributionTab.statusPanel.clearMessage();
    updateInProgress = true;
    if (contrib.getType().requiresRestart()) {
      installProgressBar.setVisible(true);
      installProgressBar.setIndeterminate(true);

      ContribProgressBar progress = new ContribProgressBar(installProgressBar) {
        public void finishedAction() {
          // Finished uninstalling the library
          resetInstallProgressBarState();
          AvailableContribution ad =
            contribListing.getAvailableContribution(contrib);
          String url = ad.link;
          installContribution(ad, url);
        }

        @Override
        public void cancelAction() {
          resetInstallProgressBarState();
          listPanel.contributionTab.statusPanel.setMessage("");
          updateInProgress = false;
          if (contrib.isDeletionFlagged()) {
            ((LocalContribution)contrib).setUpdateFlag(true);
            ((LocalContribution)contrib).setDeletionFlag(false);
            contribListing.replaceContribution(contrib,contrib);
          }

          boolean isModeActive = false;
          if (contrib.getType() == ContributionType.MODE) {
            ModeContribution m = (ModeContribution) contrib;
            //Iterator<Editor> iter = listPanel.contribManager.editor.getBase().getEditors().iterator();
            //while (iter.hasNext()) {
            // TODO there's gotta be a cleaner way to do this accessor
            Base base = listPanel.contributionTab.editor.getBase();
            for (Editor e : base.getEditors()) {
              //Editor e = iter.next();
              if (e.getMode().equals(m.getMode())) {
                isModeActive = true;
                break;
              }
            }
          }
          if (isModeActive) {
          } else {
            // TODO: remove or uncomment if the button was added
            //listPanel.contributionTab.restartButton.setVisible(true);
          }
        }
      };
      ((LocalContribution) contrib)
        .removeContribution(listPanel.contributionTab.editor.getBase(),
                            progress, listPanel.contributionTab.statusPanel);
    } else {
      AvailableContribution ad = contribListing.getAvailableContribution(contrib);
      String url = ad.link;
      installContribution(ad, url);
    }

  }


  public void remove() {

    listPanel.contributionTab.statusPanel.clearMessage();
    if (contrib.isInstalled() && contrib instanceof LocalContribution) {
      removeInProgress = true;
      installProgressBar.setVisible(true);
      installProgressBar.setIndeterminate(true);

      ContribProgressBar monitor = new ContribProgressBar(installProgressBar) {
        public void finishedAction() {
          // Finished uninstalling the library
          resetInstallProgressBarState();
          removeInProgress = false;

        }

        public void cancelAction() {
          resetInstallProgressBarState();
          removeInProgress = false;

          ContributionTab contributionTab = listPanel.contributionTab;
          if (contrib.getType() == ContributionType.MODE) {
            ModeContribution m = (ModeContribution) contrib;
            // TODO there's gotta be a cleaner way to do this accessor
            for (Editor e : contributionTab.editor.getBase().getEditors()) {
            //Iterator<Editor> iter = listPanel.contribManager.editor.getBase().getEditors().iterator();
            //while (iter.hasNext()) {
              //Editor e = iter.next();
              if (e.getMode().equals(m.getMode())) {
                break;
              }
            }
          }
        }
      };
      ContributionTab contributionTab = listPanel.contributionTab;
      LocalContribution localContrib = (LocalContribution) contrib;
      localContrib.removeContribution(contributionTab.editor.getBase(), monitor, contributionTab.statusPanel);
    }

  }
}

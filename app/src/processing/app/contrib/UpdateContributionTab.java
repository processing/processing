package processing.app.contrib;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.GroupLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import processing.app.ui.Editor;
import processing.app.ui.Toolkit;


public class UpdateContributionTab extends ContributionTab {

  public UpdateContributionTab(ContributionType type,
                               ContributionManagerDialog contributionManagerDialog) {
    super();

    filter = new Contribution.Filter() {
      public boolean matches(Contribution contrib) {
        if (contrib instanceof LocalContribution) {
          return ContributionListing.getInstance().hasUpdates(contrib);
        }
        return false;
      }
    };
    contributionListPanel = new UpdateContributionListPanel(this, filter);
    statusPanel = new UpdateStatusPanel(this, 650, this);
    this.contributionType = type;
    this.contributionManagerDialog = contributionManagerDialog;
    contribListing = ContributionListing.getInstance();
    contribListing.addContributionListener(contributionListPanel);
  }


  @Override
  public void setLayout(Editor editor, boolean activateErrorPanel,
                        boolean isLoading) {
    if (panel == null) {
      progressBar = new JProgressBar();
      progressBar.setVisible(false);
      buildErrorPanel();
      panel = new JPanel(false){
        @Override
        protected void paintComponent(Graphics g) {
          super.paintComponent(g);
          g.setColor(new Color(0xe0fffd));
          g.fillRect(getX(), panel.getY() - ContributionManagerDialog.TAB_HEIGHT - 2 , panel.getWidth(), 2);

        }
      };
      loaderLabel = new JLabel(Toolkit.getLibIcon("icons/loader.gif"));
      loaderLabel.setOpaque(false);
      loaderLabel.setBackground(Color.WHITE);
    }

    GroupLayout layout = new GroupLayout(panel);
    panel.setLayout(layout);
    layout.setHorizontalGroup(layout
      .createParallelGroup(GroupLayout.Alignment.CENTER)
      .addComponent(loaderLabel)
      .addComponent(contributionListPanel).addComponent(errorPanel)
      .addComponent(statusPanel));

    layout.setVerticalGroup(layout
      .createSequentialGroup()
      .addGap(2)
      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                  .addComponent(loaderLabel)
                  .addComponent(contributionListPanel))
      .addComponent(errorPanel)
      .addComponent(statusPanel, GroupLayout.PREFERRED_SIZE,
                    GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE));
    layout.setHonorsVisibility(contributionListPanel, false);

    panel.setBackground(Color.WHITE);
  }
}

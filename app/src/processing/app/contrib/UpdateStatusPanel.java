package processing.app.contrib;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.LayoutStyle;
import javax.swing.SwingConstants;

import processing.app.ui.Toolkit;

public class UpdateStatusPanel extends StatusPanel {

  /**
   * 
   */
  private final UpdateContributionTab parent;

  public UpdateStatusPanel(UpdateContributionTab updateContributionTab, int width, final ContributionTab contributionTab) {
    super();
    parent = updateContributionTab;

    updateButton = new JButton("Update All", Toolkit.getLibIconX("manager/update"));
    updateButton.setFont(Toolkit.getSansFont(14, Font.PLAIN));
    updateButton.setHorizontalAlignment(SwingConstants.LEFT);
    updateButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        Collection<ContributionPanel> c =
          contributionTab.contributionListPanel.panelByContribution.values();
        for (ContributionPanel contributionPanel : c) {
          contributionPanel.update();
        }
      }
    });
    setBackground(new Color(0xebebeb));
    layout = new GroupLayout(this);
    this.setLayout(layout);

    layout.setAutoCreateContainerGaps(true);
    layout.setAutoCreateGaps(true);

    layout.setHorizontalGroup(layout
      .createSequentialGroup()
      .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,
                       GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
      .addComponent(updateButton, BUTTON_WIDTH, BUTTON_WIDTH, BUTTON_WIDTH));
    layout.setVerticalGroup(layout.createParallelGroup()
      .addComponent(updateButton));
    updateButton.setVisible(true);
    updateButton.setEnabled(false);
  }

  public void update() {
    updateButton.setEnabled(parent.contributionListPanel.getNoOfRows() > 0);
  }
}
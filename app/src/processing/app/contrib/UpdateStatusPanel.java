package processing.app.contrib;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.LayoutStyle;

public class UpdateStatusPanel extends StatusPanel {
  public UpdateStatusPanel(int width, final ContributionTab contributionTab) {
    super();
    updateButton = new JButton("Update All");
    updateButton.addActionListener(new ActionListener() {
      
      @Override
      public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub
        for(ContributionPanel contributionPanel : contributionTab.contributionListPanel.panelByContribution.values()){
          contributionPanel.update();
        }
      }
    });
    layout = new GroupLayout(this);
    this.setLayout(layout);
    
    layout.setAutoCreateContainerGaps(true);
    layout.setAutoCreateGaps(true);

    layout.setHorizontalGroup(layout
      .createSequentialGroup()
      .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,
                       GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
      .addComponent(updateButton));
    layout.setVerticalGroup(layout.createParallelGroup()
      .addComponent(updateButton));
    updateButton.setVisible(true);
  }
  @Override
  public void update(ContributionPanel panel) {
    
  }
}

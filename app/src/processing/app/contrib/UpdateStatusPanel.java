package processing.app.contrib;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.LayoutStyle;

public class UpdateStatusPanel extends StatusPanel {
  public UpdateStatusPanel(int width, final ContributionTab contributionTab) {
    super();
    updateButton = new JButton("Update All");
    updateButton.setContentAreaFilled(false);
    updateButton.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK, 1),BorderFactory.createEmptyBorder(3, 0, 3, 0)));
    updateButton.addActionListener(new ActionListener() {
      
      @Override
      public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub
        for(ContributionPanel contributionPanel : contributionTab.contributionListPanel.panelByContribution.values()){
          contributionPanel.update();
        }
      }
    });
    this.setBackground(Color.WHITE);
    this.setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, Color.BLACK));
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
  }
  @Override
  public void update(ContributionPanel panel) {
    
  }
}

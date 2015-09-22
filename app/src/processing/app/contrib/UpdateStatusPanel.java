package processing.app.contrib;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.LayoutStyle;
import javax.swing.SwingConstants;

import processing.app.ui.Toolkit;

public class UpdateStatusPanel extends StatusPanel {

  public UpdateStatusPanel(UpdateContributionTab tab, int width) {
    super();
    this.contributionTab = tab;

    updateButton = new JButton("Update All", Toolkit.getLibIconX("manager/update"));
    updateButton.setFont(Toolkit.getSansFont(14, Font.PLAIN));
    updateButton.setHorizontalAlignment(SwingConstants.LEFT);
    updateButton.setVisible(true);
    updateButton.setEnabled(false);

    updateButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        contributionTab.updateAll();
      }
    });
    setBackground(new Color(0xebebeb));
    layout = new GroupLayout(this);
    setLayout(layout);

    layout.setAutoCreateContainerGaps(true);
    layout.setAutoCreateGaps(true);

    layout.setHorizontalGroup(layout
      .createSequentialGroup()
      .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,
                       GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
      .addComponent(updateButton, BUTTON_WIDTH, BUTTON_WIDTH, BUTTON_WIDTH)
      .addGap(12));  // make button line up relative to the scrollbar

    layout.setVerticalGroup(layout.createParallelGroup()
      .addComponent(updateButton));
  }

  public void update() {
    updateButton.setEnabled(contributionTab.hasUpdates());
  }
}
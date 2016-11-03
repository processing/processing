/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2013-16 The Processing Foundation

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

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.GroupLayout;
import javax.swing.LayoutStyle;
import javax.swing.SwingConstants;

import processing.app.ui.Toolkit;


public class UpdateStatusPanel extends StatusPanel {

  public UpdateStatusPanel(UpdateContributionTab tab, int width) {
    super();
    this.contributionTab = tab;

    updateButton = Toolkit.createIconButton("Update All", "manager/update");
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
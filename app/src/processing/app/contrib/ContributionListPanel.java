/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2013 The Processing Foundation
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

import java.util.*;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

import java.awt.*;

import processing.app.Base;


// The "Scrollable" implementation and its methods here take care of preventing
// the scrolling area from running exceptionally slowly. Not sure why they're
// necessary in the first place, however; seems like odd behavior.
// It also allows the description text in the panels to wrap properly.

public class ContributionListPanel extends JPanel implements Scrollable, ContributionChangeListener {

  ContributionManagerDialog contribManager;
  TreeMap<Contribution, ContributionPanel> panelByContribution;

  static HyperlinkListener nullHyperlinkListener = new HyperlinkListener() {
    public void hyperlinkUpdate(HyperlinkEvent e) { }
  };

  private ContributionPanel selectedPanel;
//  protected JPanel statusPlaceholder;
  private StatusPanel status;
  private ContributionFilter filter;
//  private ContributionListing contribListing;
  private ContributionListing contribListing = ContributionListing.getInstance();


  public ContributionListPanel(ContributionManagerDialog libraryManager,
                               ContributionFilter filter) {
    super();
    this.contribManager = libraryManager;
    this.filter = filter;

//    contribListing = ContributionListing.getInstance();

    setLayout(new GridBagLayout());
    setOpaque(true);

    if (Base.isLinux()) {
      // Because of a bug with GNOME, getColor returns the wrong value for
      // List.background. We'll just assume its white. The number of people
      // using Linux and an inverted color theme should be small enough.
      setBackground(Color.white);
    } else {
      setBackground(UIManager.getColor("List.background"));
    }

    panelByContribution = new TreeMap<Contribution, ContributionPanel>(
        contribListing.getComparator());

//    statusPlaceholder = new JPanel();
//    statusPlaceholder.setVisible(false);
    status = new StatusPanel();
  }


  private void updatePanelOrdering() {
    int row = 0;
    for (Entry<Contribution, ContributionPanel> entry : panelByContribution.entrySet()) {
      GridBagConstraints c = new GridBagConstraints();
      c.fill = GridBagConstraints.HORIZONTAL;
      c.weightx = 1;
      c.gridx = 0;
      c.gridy = row++;
      c.anchor = GridBagConstraints.NORTH;

      add(entry.getValue(), c);
    }

    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.BOTH;
    c.weightx = 1;
    c.weighty = 1;
    c.gridx = 0;
    c.gridy = row++;
    c.anchor = GridBagConstraints.NORTH;
    add(status, c);
  }


  public void contributionAdded(final Contribution contribution) {
    if (filter.matches(contribution)) {
      EventQueue.invokeLater(new Runnable() {
        public void run() {
          if (!panelByContribution.containsKey(contribution)) {
            ContributionPanel newPanel = new ContributionPanel(ContributionListPanel.this);
            synchronized (panelByContribution) {
              panelByContribution.put(contribution, newPanel);
            }
            if (newPanel != null) {
              newPanel.setContribution(contribution);
              add(newPanel);
              updatePanelOrdering();
              updateColors();  // XXX this is the place
            }
          }
          // To make the scroll shift to the first element
          // http://stackoverflow.com/questions/19400239/scrolling-to-the-top-jpanel-inside-a-jscrollpane
          EventQueue.invokeLater(new Runnable() {
            public void run() {
              scrollRectToVisible(new Rectangle(0, 0, 1, 1));
            }
          });
        }
      });
    }
  }


  public void contributionRemoved(final Contribution contribution) {
    EventQueue.invokeLater(new Runnable() {
      public void run() {
        synchronized (panelByContribution) {
          ContributionPanel panel = panelByContribution.get(contribution);
          if (panel != null) {
            remove(panel);
            panelByContribution.remove(contribution);
          }
        }
        updatePanelOrdering();
        updateColors();
        updateUI();
      }
    });
  }


  public void contributionChanged(final Contribution oldContrib,
                                  final Contribution newContrib) {
    EventQueue.invokeLater(new Runnable() {
      public void run() {
        synchronized (panelByContribution) {
          ContributionPanel panel = panelByContribution.get(oldContrib);
          if (panel == null) {
            contributionAdded(newContrib);
          } else {
            panelByContribution.remove(oldContrib);
            panel.setContribution(newContrib);
            panelByContribution.put(newContrib, panel);
            updatePanelOrdering();
          }
        }
      }
    });
  }


  public void filterLibraries(List<Contribution> filteredContributions) {
    synchronized (panelByContribution) {
      Set<Contribution> hiddenPanels =
        new TreeSet<Contribution>(contribListing.getComparator());
      hiddenPanels.addAll(panelByContribution.keySet());

      for (Contribution info : filteredContributions) {
        ContributionPanel panel = panelByContribution.get(info);
        if (panel != null) {
          panel.setVisible(true);
          hiddenPanels.remove(info);
        }
      }

      for (Contribution info : hiddenPanels) {
        ContributionPanel panel = panelByContribution.get(info);
        if (panel != null) {
          panel.setVisible(false);
        }
      }
    }
  }


  protected void setSelectedPanel(ContributionPanel panel) {
    if (selectedPanel == panel) {
      selectedPanel.setSelected(true);

    } else {
      ContributionPanel lastSelected = selectedPanel;
      selectedPanel = panel;

      if (lastSelected != null) {
        lastSelected.setSelected(false);
      }
      panel.setSelected(true);

      updateColors();
      requestFocusInWindow();
    }
  }


  protected ContributionPanel getSelectedPanel() {
    return selectedPanel;
  }


  /**
   * Updates the colors of all library panels that are visible.
   */
  protected void updateColors() {
    int count = 0;
    synchronized (panelByContribution) {
      for (Entry<Contribution, ContributionPanel> entry : panelByContribution.entrySet()) {
        ContributionPanel panel = entry.getValue();

        if (panel.isVisible() && panel.isSelected()) {
          panel.setBackground(UIManager.getColor("List.selectionBackground"));
          panel.setForeground(UIManager.getColor("List.selectionForeground"));
          panel.setBorder(UIManager.getBorder("List.focusCellHighlightBorder"));
          count++;

        } else {
          Border border = null;
          if (panel.isVisible()) {
            if (Base.isMacOS()) {
              if (count % 2 == 1) {
                border = UIManager.getBorder("List.oddRowBackgroundPainter");
              } else {
                border = UIManager.getBorder("List.evenRowBackgroundPainter");
              }
            } else {
              if (count % 2 == 1) {
                panel.setBackground(new Color(219, 224, 229));
              } else {
                panel.setBackground(new Color(241, 241, 241));
              }
            }
            count++;
          }

          if (border == null) {
            border = BorderFactory.createEmptyBorder(1, 1, 1, 1);
          }
          panel.setBorder(border);
          panel.setForeground(UIManager.getColor("List.foreground"));
        }
      }
    }
  }


  public Dimension getPreferredScrollableViewportSize() {
    return getPreferredSize();
  }


  /**
   * Amount to scroll to reveal a new page of items
   */
  public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
    if (orientation == SwingConstants.VERTICAL) {
      int blockAmount = visibleRect.height;
      if (direction > 0) {
        visibleRect.y += blockAmount;
      } else {
        visibleRect.y -= blockAmount;
      }

      blockAmount += getScrollableUnitIncrement(visibleRect, orientation, direction);
      return blockAmount;
    }
    return 0;
  }


  /**
   * Amount to scroll to reveal the rest of something we are on or a new item
   */
  public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
    if (orientation == SwingConstants.VERTICAL) {
      int lastHeight = 0, height = 0;
      int bottomOfScrollArea = visibleRect.y + visibleRect.height;

      for (Component c : getComponents()) {
        if (c.isVisible()) {
          if (c instanceof ContributionPanel) {
            Dimension d = c.getPreferredSize();

            int nextHeight = height + d.height;

            if (direction > 0) {
              // scrolling down
              if (nextHeight > bottomOfScrollArea) {
                return nextHeight - bottomOfScrollArea;
              }
            } else {
              // scrolling up
              if (nextHeight > visibleRect.y) {
                if (visibleRect.y != height) {
                  return visibleRect.y - height;
                } else {
                  return visibleRect.y - lastHeight;
                }
              }
            }

            lastHeight = height;
            height = nextHeight;
          }
        }
      }
    }
    return 0;
  }


  public boolean getScrollableTracksViewportHeight() {
    return false;
  }


  public boolean getScrollableTracksViewportWidth() {
    return true;
  }
}

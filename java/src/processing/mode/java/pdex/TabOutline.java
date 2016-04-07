/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
Part of the Processing project - http://processing.org
Copyright (c) 2012-15 The Processing Foundation

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License version 2
as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software Foundation, Inc.
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
*/

package processing.mode.java.pdex;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import processing.app.SketchCode;
import processing.mode.java.JavaEditor;


public class TabOutline {
  protected JFrame frmOutlineView;
  protected JScrollPane jsp;
  protected DefaultMutableTreeNode tabNode;
  protected DefaultMutableTreeNode tempNode;
  protected JTree tabTree;
  protected JTextField searchField;
  protected JLabel lblCaption;
  protected JavaEditor editor;
  protected boolean internalSelection = false;


  public TabOutline(JavaEditor editor) {
    this.editor = editor;
    createGUI();
  }


  private void createGUI() {
    frmOutlineView = new JFrame();
    frmOutlineView.setAlwaysOnTop(true);
    frmOutlineView.setUndecorated(true);
    Point tp = editor.getTextArea().getLocationOnScreen();
    lblCaption = new JLabel("Tabs List (type to filter)");
    int minWidth = estimateFrameWidth();
    int maxWidth = (int) (editor.getMinimumSize().width * 0.9f);
    frmOutlineView.setLayout(new BoxLayout(frmOutlineView.getContentPane(),
                                           BoxLayout.Y_AXIS));
    JPanel panelTop = new JPanel(), panelMiddle = new JPanel(), panelBottom = new JPanel();
    panelTop.setLayout(new GridBagLayout());
    panelMiddle.setLayout(new BoxLayout(panelMiddle, BoxLayout.Y_AXIS));
    panelBottom.setLayout(new BoxLayout(panelBottom, BoxLayout.Y_AXIS));
    lblCaption.setAlignmentX(Component.LEFT_ALIGNMENT);
    panelTop.add(lblCaption);
    searchField = new JTextField();
    searchField.setMinimumSize(new Dimension(minWidth, 25));
    panelMiddle.add(searchField);

    jsp = new JScrollPane();
    populateTabTree();
    jsp.setViewportView(tabTree);
    jsp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
    jsp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    jsp.setMinimumSize(new Dimension(minWidth, editor.getTextArea().getHeight() - 10));
    jsp.setMaximumSize(new Dimension(maxWidth, editor.getTextArea().getHeight() - 10));

    panelBottom.add(jsp);
    frmOutlineView.add(panelTop);
    frmOutlineView.add(panelMiddle);
    frmOutlineView.add(panelBottom);
    frmOutlineView.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    frmOutlineView.pack();
    frmOutlineView.setBounds(tp.x + editor.getTextArea().getWidth() - minWidth,
                             tp.y,
                             minWidth,
                             estimateFrameHeight());
    frmOutlineView.setMinimumSize(new Dimension(minWidth, Math
        .min(editor.getTextArea().getHeight(),
             frmOutlineView.getHeight())));
    frmOutlineView.setLocation(tp.x + editor.getTextArea().getWidth()/2 - frmOutlineView.getWidth()/2,
                               frmOutlineView.getY() + (editor.getTextArea().getHeight() - frmOutlineView.getHeight()) / 2);
    DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) tabTree.getCellRenderer();
    renderer.setLeafIcon(null);
    renderer.setClosedIcon(null);
    renderer.setOpenIcon(null);
    addListeners();
  }


  private void addListeners() {
    searchField.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent evt) {
        if (tabTree.getRowCount() == 0)
          return;

        internalSelection = true;

        if (evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
          close();
        } else if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
          if (tabTree.getLastSelectedPathComponent() != null) {
            DefaultMutableTreeNode tnode = (DefaultMutableTreeNode) tabTree
                .getLastSelectedPathComponent();
            //log("Enter Key, Tab: " + tnode);
            switchToTab(tnode.toString());
            close();
          }
        } else if (evt.getKeyCode() == KeyEvent.VK_UP) {
          if (tabTree.getLastSelectedPathComponent() == null) {
            tabTree.setSelectionRow(0);
            return;
          }

          int x = tabTree.getLeadSelectionRow() - 1;
          int step = jsp.getVerticalScrollBar().getMaximum()
              / tabTree.getRowCount();
          if (x == -1) {
            x = tabTree.getRowCount() - 1;
            jsp.getVerticalScrollBar().setValue(jsp.getVerticalScrollBar()
                                                    .getMaximum());
          } else {
            jsp.getVerticalScrollBar().setValue((jsp.getVerticalScrollBar()
                                                    .getValue() - step));
          }
          tabTree.setSelectionRow(x);
        } else if (evt.getKeyCode() == KeyEvent.VK_DOWN) {
          if (tabTree.getLastSelectedPathComponent() == null) {
            tabTree.setSelectionRow(0);
            return;
          }
          int x = tabTree.getLeadSelectionRow() + 1;

          int step = jsp.getVerticalScrollBar().getMaximum()
              / tabTree.getRowCount();
          if (x == tabTree.getRowCount()) {
            x = 0;
            jsp.getVerticalScrollBar().setValue(jsp.getVerticalScrollBar()
                                                    .getMinimum());
          } else {
            jsp.getVerticalScrollBar().setValue((jsp.getVerticalScrollBar()
                                                    .getValue() + step));
          }
          tabTree.setSelectionRow(x);
        }
      }
    });

    searchField.getDocument().addDocumentListener(new DocumentListener() {

      public void insertUpdate(DocumentEvent e) {
        updateSelection();
      }

      public void removeUpdate(DocumentEvent e) {
        updateSelection();
      }

      public void changedUpdate(DocumentEvent e) {
        updateSelection();
      }

      private void updateSelection() {
        SwingWorker<Object, Object> worker = new SwingWorker<Object, Object>() {
          protected Object doInBackground() throws Exception {
            String text = searchField.getText().toLowerCase();
            tempNode = new DefaultMutableTreeNode();
            filterTree(text, tempNode, tabNode); // TODO: is using tabNode thread-safe? [jv]
            return null;
          }

          protected void done() {
            tabTree.setModel(new DefaultTreeModel(tempNode));
            ((DefaultTreeModel) tabTree.getModel()).reload();
//            for (int i = 0; i < tabTree.getRowCount(); i++) {
//              tabTree.expandRow(i);
//            }
            internalSelection = true;
            tabTree.setSelectionRow(0);
          }
        };
        worker.execute();
      }
    });

    tabTree.addTreeSelectionListener(new TreeSelectionListener() {

      public void valueChanged(TreeSelectionEvent e) {
        if (internalSelection) {
          //log("Internal selection");
          internalSelection = (false);
          return;
        }
        // log(e);

        if (tabTree.getLastSelectedPathComponent() == null) {
          return;
        }
        DefaultMutableTreeNode tnode = (DefaultMutableTreeNode) tabTree
            .getLastSelectedPathComponent();
        //log("Clicked " + tnode);
        switchToTab(tnode.toString());
        close();
      }
    });

    tabTree.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent me) {
        if (tabTree.getLastSelectedPathComponent() == null) {
          return;
        }
        DefaultMutableTreeNode tnode =
          (DefaultMutableTreeNode) tabTree.getLastSelectedPathComponent();
        //log("Clicked " + tnode);
        switchToTab(tnode.toString());
        close();
      }
    });

    frmOutlineView.addWindowFocusListener(new WindowFocusListener() {
      public void windowLostFocus(WindowEvent e) {
        close();
      }

      public void windowGainedFocus(WindowEvent e) {
      }
    });
  }

  private void switchToTab(String tabName) {
    for (SketchCode sc : editor.getSketch().getCode()) {
      if (sc.getPrettyName().equals(tabName)) {
        editor.getSketch().setCurrentCode(editor.getSketch().getCodeIndex(sc));
      }
    }
  }

  private void populateTabTree() {
    tabNode = new DefaultMutableTreeNode("Tabs");
    for (SketchCode sc : editor.getSketch().getCode()) {
      DefaultMutableTreeNode tab = new DefaultMutableTreeNode(
                                                              sc.getPrettyName());
      tabNode.add(tab);
    }
    tempNode = tabNode;
    tabTree = new JTree(tabNode);
    tabTree.getSelectionModel()
        .setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    tabTree.setRootVisible(false);
    tabTree.setSelectionRow(editor.getSketch().getCurrentCodeIndex());
  }

  protected boolean filterTree(String prefix, DefaultMutableTreeNode tree,
                               DefaultMutableTreeNode mainTree) {
    if (mainTree.isLeaf()) {
      return (mainTree.getUserObject().toString().toLowerCase()
          .startsWith(prefix));
    }

    boolean found = false;
    for (int i = 0; i < mainTree.getChildCount(); i++) {
      DefaultMutableTreeNode tNode = new DefaultMutableTreeNode(
                                                                ((DefaultMutableTreeNode) mainTree
                                                                    .getChildAt(i))
                                                                    .getUserObject());
      if (filterTree(prefix, tNode,
                     (DefaultMutableTreeNode) mainTree.getChildAt(i))) {
        found = true;
        tree.add(tNode);
      }
    }
    return found;
  }

  private int estimateFrameWidth() {
    FontMetrics fm = editor.getTextArea().getGraphics().getFontMetrics();
    int w = fm.stringWidth(lblCaption.getText()) + 10;
    for (int i = 0; i < editor.getSketch().getCodeCount(); i++) {
      w = Math.max(w, fm.stringWidth(editor.getSketch().getCode(i).getPrettyName()) + 10);
    }
    return w;
  }

  private int estimateFrameHeight() {
    int textHeight = jsp.getGraphics().getFontMetrics().getHeight() + 2;
    int t = Math.max(4, editor.getSketch().getCodeCount() + 3);
    return Math.min(textHeight * t, frmOutlineView.getHeight());
  }

  public void show() {
    frmOutlineView.setVisible(true);
  }

  public void close() {
    frmOutlineView.setVisible(false);
    frmOutlineView.dispose();
  }

  public boolean isVisible() {
    return frmOutlineView.isVisible();
  }
}

/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2013-15 The Processing Foundation

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.app.ui;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;

import processing.app.Base;
import processing.app.Language;
import processing.app.Mode;
import processing.app.Platform;
import processing.app.SketchReference;


public class SketchbookFrame extends JFrame {
  protected Base base;
  protected Mode mode;


  public SketchbookFrame(final Base base, final Mode mode) {
    super(Language.text("sketchbook"));
    this.base = base;
    this.mode = mode;

    final ActionListener listener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        setVisible(false);
      }
    };
    Toolkit.registerWindowCloseKeys(getRootPane(), listener);
    Toolkit.setIcon(this);

    final JTree tree = new JTree(mode.buildSketchbookTree());
    tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    tree.setShowsRootHandles(true);
    tree.expandRow(0);
    tree.setRootVisible(false);

    tree.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
          DefaultMutableTreeNode node =
            (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

          int selRow = tree.getRowForLocation(e.getX(), e.getY());
          //TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
          //if (node != null && node.isLeaf() && node.getPath().equals(selPath)) {
          if (node != null && node.isLeaf() && selRow != -1) {
            SketchReference sketch = (SketchReference) node.getUserObject();
            base.handleOpen(sketch.getPath());
          }
        }
      }
    });

    tree.addKeyListener(new KeyAdapter() {
      // ESC doesn't fire keyTyped(), so we have to catch it on keyPressed
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
          setVisible(false);
        }
      }

      public void keyTyped(KeyEvent e) {
        if (e.getKeyChar() == KeyEvent.VK_ENTER) {
          DefaultMutableTreeNode node =
            (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
          if (node != null && node.isLeaf()) {
            SketchReference sketch = (SketchReference) node.getUserObject();
            base.handleOpen(sketch.getPath());
          }
        }
      }
    });

    tree.setBorder(new EmptyBorder(5, 5, 5, 5));
    if (Platform.isMacOS()) {
      tree.setToggleClickCount(2);
    } else {
      tree.setToggleClickCount(1);
    }
    JScrollPane treePane = new JScrollPane(tree);
    treePane.setPreferredSize(new Dimension(250, 450));
    treePane.setBorder(new EmptyBorder(0, 0, 0, 0));

    getContentPane().add(treePane);
    pack();
  }


  public void setVisible() {
    // TODO The ExamplesFrame code doesn't do this, is it necessary?
    // Either one is wrong or we're papering over something [fry 150811]
    EventQueue.invokeLater(new Runnable() {
      @Override
      public void run() {
        // Space for the editor plus a li'l gap
        int roughWidth = getWidth() + 20;
        Point p = null;
        // If no window open, or the editor is at the edge of the screen
        Editor editor = base.getActiveEditor();
        if (editor == null ||
            (p = editor.getLocation()).x < roughWidth) {
          // Center the window on the screen
          setLocationRelativeTo(null);
        } else {
          // Open the window relative to the editor
          setLocation(p.x - roughWidth, p.y);
        }
        //Check whether sketch book is empty or not
        DefaultMutableTreeNode checksb =
          new DefaultMutableTreeNode(Language.text("sketchbook.tree"));
        try {
              //Opens sketch book only if created sketches exist
              if (base.addSketches(checksb, Base.getSketchbookFolder(), false)) {
                  setVisible(true);
              } else {
                //Shows message in case sketch book is empty
                JOptionPane.showMessageDialog(getParent(), "Your Sketchbook is empty!",
                "Message", DO_NOTHING_ON_CLOSE, new ImageIcon(Toolkit.getLibImage("icons/pde-32.png")));
              }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    });
  }
}
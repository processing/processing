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
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
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

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import processing.app.Mode;
import processing.mode.java.JavaEditor;


public class SketchOutline {
  protected final JavaEditor editor;

  protected JFrame frmOutlineView;
  protected JScrollPane jsp;
  protected DefaultMutableTreeNode soNode, tempNode;
  protected final JTree soTree;
  protected JTextField searchField;
  protected boolean internalSelection = false;

  ImageIcon classIcon, fieldIcon, methodIcon;


  public SketchOutline(JavaEditor editor, DefaultMutableTreeNode codeTree) {
    this.editor = editor;
    soNode = new DefaultMutableTreeNode();
    generateSketchOutlineTree(soNode, codeTree);
    soNode = (DefaultMutableTreeNode) soNode.getChildAt(0);
    tempNode = soNode;
    soTree = new JTree(soNode);

    Mode mode = editor.getMode();
    classIcon = mode.loadIcon("theme/icon_class_obj.png");
    methodIcon = mode.loadIcon("theme/icon_methpub_obj.png");
    fieldIcon = mode.loadIcon("theme/icon_field_protected_obj.png");

    createGUI();
  }


  private void createGUI(){
    frmOutlineView = new JFrame();
    frmOutlineView.setAlwaysOnTop(true);
    frmOutlineView.setUndecorated(true);
    Point tp = editor.getTextArea().getLocationOnScreen();

    int minWidth = (int) (editor.getMinimumSize().width * 0.7f);
    int maxWidth = (int) (editor.getMinimumSize().width * 0.9f);
    frmOutlineView.setLayout(new BoxLayout(frmOutlineView.getContentPane(),
                                           BoxLayout.Y_AXIS));
    JPanel panelTop = new JPanel(), panelBottom = new JPanel();
    panelTop.setLayout(new BoxLayout(panelTop, BoxLayout.Y_AXIS));
    panelBottom.setLayout(new BoxLayout(panelBottom, BoxLayout.Y_AXIS));
    searchField = new JTextField();
    searchField.setMinimumSize(new Dimension(minWidth, 25));
    panelTop.add(searchField);

    jsp = new JScrollPane();

    soTree.getSelectionModel()
        .setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    soTree.setRootVisible(false);
    soTree.setCellRenderer(new CustomCellRenderer());
    for (int i = 0; i < soTree.getRowCount(); i++) {
      soTree.expandRow(i);
    }
    soTree.setSelectionRow(0);

    jsp.setViewportView(soTree);
    jsp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
    jsp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    jsp.setMinimumSize(new Dimension(minWidth, editor.getTextArea().getHeight() - 10));
    jsp.setMaximumSize(new Dimension(maxWidth, editor.getTextArea().getHeight() - 10));

    panelBottom.add(jsp);
    frmOutlineView.add(panelTop);
    frmOutlineView.add(panelBottom);
    frmOutlineView.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    frmOutlineView.pack();
    frmOutlineView.setBounds(tp.x + editor.getTextArea().getWidth() - minWidth, tp.y, minWidth,
                             Math.min(editor.getTextArea().getHeight(), frmOutlineView.getHeight()));
    frmOutlineView.setMinimumSize(new Dimension(minWidth, Math.min(editor.getTextArea().getHeight(), frmOutlineView.getHeight())));
    frmOutlineView.setLocation(tp.x + editor.getTextArea().getWidth()/2 - frmOutlineView.getWidth()/2,
                               frmOutlineView.getY() + (editor.getTextArea().getHeight() - frmOutlineView.getHeight()) / 2);
    addListeners();
  }


  protected void addListeners() {

    searchField.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent evt) {
        if (soTree.getRowCount() == 0)
          return;

        internalSelection = true;

        if (evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
         close();

        } else if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
          if (soTree.getLastSelectedPathComponent() != null) {
            DefaultMutableTreeNode tnode = (DefaultMutableTreeNode) soTree
                .getLastSelectedPathComponent();
            if (tnode.getUserObject() instanceof ASTNodeWrapper) {
              ASTNodeWrapper awrap = (ASTNodeWrapper) tnode.getUserObject();
              awrap.highlightNode(editor);
              //errorCheckerService.highlightNode(awrap);
              close();
            }
          }

        } else if (evt.getKeyCode() == KeyEvent.VK_UP) {
          if (soTree.getLastSelectedPathComponent() == null) {
            soTree.setSelectionRow(0);
            return;
          }

          int x = soTree.getLeadSelectionRow() - 1;
          int step = jsp.getVerticalScrollBar().getMaximum()
              / soTree.getRowCount();
          if (x == -1) {
            x = soTree.getRowCount() - 1;
            jsp.getVerticalScrollBar().setValue(jsp.getVerticalScrollBar().getMaximum());
          } else {
            jsp.getVerticalScrollBar().setValue((jsp.getVerticalScrollBar()
                                                    .getValue() - step));
          }
          soTree.setSelectionRow(x);

        } else if (evt.getKeyCode() == KeyEvent.VK_DOWN) {
          if (soTree.getLastSelectedPathComponent() == null) {
            soTree.setSelectionRow(0);
            return;
          }
          int x = soTree.getLeadSelectionRow() + 1;

          int step = jsp.getVerticalScrollBar().getMaximum()
              / soTree.getRowCount();
          if (x == soTree.getRowCount()) {
            x = 0;
            jsp.getVerticalScrollBar().setValue(jsp.getVerticalScrollBar().getMinimum());
          } else {
            jsp.getVerticalScrollBar().setValue((jsp.getVerticalScrollBar()
                                                    .getValue() + step));
          }
          soTree.setSelectionRow(x);
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

      private void updateSelection(){
        SwingWorker<Object, Object> worker = new SwingWorker<Object, Object>() {
          protected Object doInBackground() throws Exception {
            String text = searchField.getText().toLowerCase();
            tempNode = new DefaultMutableTreeNode();
            filterTree(text, tempNode, soNode); // TODO: is using soNode thread-safe? [jv]
            return null;
          }

          protected void done() {
            soTree.setModel(new DefaultTreeModel(tempNode));
            ((DefaultTreeModel) soTree.getModel()).reload();
            for (int i = 0; i < soTree.getRowCount(); i++) {
              soTree.expandRow(i);
            }
            internalSelection = true;
            soTree.setSelectionRow(0);
          }
        };
        worker.execute();
      }
    });

    frmOutlineView.addWindowFocusListener(new WindowFocusListener() {
      public void windowLostFocus(WindowEvent e) {
        close();
      }

      public void windowGainedFocus(WindowEvent e) {
      }
    });

    soTree.addTreeSelectionListener(new TreeSelectionListener() {

      public void valueChanged(TreeSelectionEvent e) {

        if (internalSelection) {
          internalSelection = (false);
          return;
        }
        // log(e);
        scrollToNode();
      }
    });

    soTree.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent me) {
        scrollToNode();
      }
    });
  }


  private void scrollToNode() {
    SwingWorker<Object, Object> worker = new SwingWorker<Object, Object>() {

      protected Object doInBackground() throws Exception {
        return null;
      }

      protected void done() {
        if (soTree.getLastSelectedPathComponent() == null) {
          return;
        }
        DefaultMutableTreeNode tnode = (DefaultMutableTreeNode) soTree
            .getLastSelectedPathComponent();
        if (tnode.getUserObject() instanceof ASTNodeWrapper) {
          ASTNodeWrapper awrap = (ASTNodeWrapper) tnode.getUserObject();
          awrap.highlightNode(editor);
          // log(awrap);
          //errorCheckerService.highlightNode(awrap);
          close();
        }
      }
    };
    worker.execute();
  }


  protected boolean filterTree(String prefix, DefaultMutableTreeNode tree,
                               DefaultMutableTreeNode mainTree) {
    if (mainTree.isLeaf()) {
      return mainTree.getUserObject().toString().toLowerCase().startsWith(prefix);
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


  @SuppressWarnings("unchecked")
  protected void generateSketchOutlineTree(DefaultMutableTreeNode node,
                                           DefaultMutableTreeNode codetree) {
    if (codetree == null)
      return;
    //log("Visi " + codetree + codetree.getUserObject().getClass().getSimpleName());
    if (!(codetree.getUserObject() instanceof ASTNodeWrapper))
      return;
    ASTNodeWrapper awnode = (ASTNodeWrapper) codetree.getUserObject(), aw2 = null;

    if (awnode.getNode() instanceof TypeDeclaration) {
      aw2 = new ASTNodeWrapper( ((TypeDeclaration) awnode.getNode()).getName(),
                               ((TypeDeclaration) awnode.getNode()).getName()
                                   .toString());
    } else if (awnode.getNode() instanceof MethodDeclaration) {
      aw2 = new ASTNodeWrapper(
                               ((MethodDeclaration) awnode.getNode()).getName(),
                               new CompletionCandidate(
                                                       ((MethodDeclaration) awnode
                                                           .getNode()))
                                   .toString());
    } else if (awnode.getNode() instanceof FieldDeclaration) {
      FieldDeclaration fd = (FieldDeclaration) awnode.getNode();
      for (VariableDeclarationFragment vdf : (List<VariableDeclarationFragment>) fd.fragments()) {
        final String text = new CompletionCandidate(vdf).toString();
        DefaultMutableTreeNode newNode =
          new DefaultMutableTreeNode(new ASTNodeWrapper(vdf.getName(), text));
        node.add(newNode);
      }
      return;
    }
    if (aw2 == null)
      return;
    DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(aw2);
    node.add(newNode);
    for (int i = 0; i < codetree.getChildCount(); i++) {
      generateSketchOutlineTree(newNode,
                                (DefaultMutableTreeNode) codetree.getChildAt(i));
    }
  }


  public void show() {
    frmOutlineView.setVisible(true);
  }


  public void close(){
    frmOutlineView.setVisible(false);
    frmOutlineView.dispose();
  }


  public boolean isVisible(){
    return frmOutlineView.isVisible();
  }


  protected class CustomCellRenderer extends DefaultTreeCellRenderer {

    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean sel, boolean expanded,
                                                  boolean leaf, int row,
                                                  boolean hasFocus) {
      super.getTreeCellRendererComponent(tree, value, sel, expanded,
                                         leaf, row, hasFocus);
      if (value instanceof DefaultMutableTreeNode)
        setIcon(getTreeIcon(value));

      return this;
    }

    public Icon getTreeIcon(Object o) {
      if (((DefaultMutableTreeNode) o).getUserObject() instanceof ASTNodeWrapper) {
        ASTNodeWrapper awrap = (ASTNodeWrapper)
          ((DefaultMutableTreeNode) o).getUserObject();

        int type = awrap.getNode().getParent().getNodeType();
        if (type == ASTNode.METHOD_DECLARATION) {
          return methodIcon;
        } else if (type == ASTNode.TYPE_DECLARATION) {
          return classIcon;
        } else if (type == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
          return fieldIcon;
        }
      }
      return null;
    }
  }
}

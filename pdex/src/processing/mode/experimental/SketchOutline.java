package processing.mode.experimental;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingWorker;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class SketchOutline {
  protected JFrame frmOutlineView;

  protected ErrorCheckerService errorCheckerService;

  protected JScrollPane jsp;

  protected DefaultMutableTreeNode soNode, tempNode;

  protected final JTree soTree;

  protected JTextField searchField;
  
  protected DebugEditor editor;

  public SketchOutline(DefaultMutableTreeNode codeTree, ErrorCheckerService ecs) {
    errorCheckerService = ecs;
    editor = ecs.getEditor();
    frmOutlineView = new JFrame();
    frmOutlineView.setAlwaysOnTop(true);
    frmOutlineView.setUndecorated(true);
    Point tp = errorCheckerService.getEditor().ta.getLocationOnScreen();
//    frmOutlineView.setBounds(tp.x
//                                 + errorCheckerService.getEditor().ta
//                                     .getWidth() - 300, tp.y, 300,
//                             errorCheckerService.getEditor().ta.getHeight());
    
    //TODO: ^Absolute dimensions are bad bro

    int minWidth = 200;
    frmOutlineView.setLayout(new BoxLayout(frmOutlineView.getContentPane(),
                                           BoxLayout.Y_AXIS));
    JPanel panelTop = new JPanel(), panelBottom = new JPanel();
    panelTop.setLayout(new BoxLayout(panelTop,
                             BoxLayout.Y_AXIS));
    panelBottom.setLayout(new BoxLayout(panelBottom,
                                     BoxLayout.Y_AXIS));
    searchField = new JTextField();
    searchField.setMinimumSize(new Dimension(minWidth, 25));
    panelTop.add(searchField);    

    jsp = new JScrollPane();
    soNode = new DefaultMutableTreeNode();
    generateSketchOutlineTree(soNode, codeTree);
    soNode = (DefaultMutableTreeNode) soNode.getChildAt(0);
    soTree = new JTree(soNode);
    soTree.getSelectionModel()
        .setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    soTree.setRootVisible(false);
    for (int i = 0; i < soTree.getRowCount(); i++) {
      soTree.expandRow(i);
    }
    soTree.setSelectionRow(0);
    jsp.setViewportView(soTree);
    jsp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
    jsp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    jsp.setMinimumSize(new Dimension(minWidth, 100));
    panelBottom.add(jsp);
    frmOutlineView.add(panelTop);
    frmOutlineView.add(panelBottom);
    frmOutlineView.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
//    frmOutlineView.setLocation(tp.x
//                               + errorCheckerService.getEditor().ta
//                               .getWidth() - minWidth, tp.y);
    frmOutlineView.setBounds(tp.x
                                 + errorCheckerService.getEditor().ta
                                     .getWidth() - minWidth, tp.y, minWidth,Math
                                     .min(editor.ta.getHeight(), 150));
    frmOutlineView.setMinimumSize(new Dimension(minWidth, Math
        .min(errorCheckerService.getEditor().ta.getHeight(), 150)));
    frmOutlineView.pack();
    frmOutlineView.setLocation(tp.x
                                   + errorCheckerService.getEditor().ta
                                       .getWidth() - frmOutlineView.getWidth(),
                               frmOutlineView.getY()
                                   + (editor.ta.getHeight() - frmOutlineView
                                       .getHeight()) / 2);
    addListeners();

  }

  protected void addListeners() {

    searchField.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent evt) {
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
          if (soTree.getLastSelectedPathComponent() != null) {
            DefaultMutableTreeNode tnode = (DefaultMutableTreeNode) soTree
                .getLastSelectedPathComponent();
            if (tnode.getUserObject() != null) {
              if (tnode.getUserObject() instanceof ASTNodeWrapper) {
                ASTNodeWrapper awrap = (ASTNodeWrapper) tnode.getUserObject();
                errorCheckerService.highlightNode(awrap);
              }
            }
          }
          return;
        } else if (evt.getKeyCode() == KeyEvent.VK_UP) {
          if (soTree.getLastSelectedPathComponent() == null) {
            soTree.setSelectionRow(0);
          }
          int x = soTree.getLeadSelectionRow();
          x = (x - 1) % soTree.getRowCount();
          soTree.setSelectionRow(x);
          return;
        } else if (evt.getKeyCode() == KeyEvent.VK_DOWN) {
          if (soTree.getLastSelectedPathComponent() == null) {
            soTree.setSelectionRow(0);
          }
          int x = soTree.getLeadSelectionRow();
          x = (x + 1) % soTree.getRowCount();
          soTree.setSelectionRow(x);
          return;
        }

        SwingWorker worker = new SwingWorker() {
          protected Object doInBackground() throws Exception {
            return null;
          }

          protected void done() {
            String text = searchField.getText().toLowerCase();
            tempNode = new DefaultMutableTreeNode();
            filterTree(text, tempNode, soNode);
            soTree.setModel(new DefaultTreeModel(tempNode));
            ((DefaultTreeModel) soTree.getModel()).reload();
            for (int i = 0; i < soTree.getRowCount(); i++) {
              soTree.expandRow(i);
            }
            soTree.setSelectionRow(0);
          }
        };
        worker.execute();

      }
    });

    frmOutlineView.addWindowFocusListener(new WindowFocusListener() {
      public void windowLostFocus(WindowEvent e) {
        frmOutlineView.setVisible(false);
        frmOutlineView.dispose();
      }

      public void windowGainedFocus(WindowEvent e) {
      }
    });
  }

  protected boolean filterTree(String prefix, DefaultMutableTreeNode tree,
                               DefaultMutableTreeNode mainTree) {
    if(mainTree.isLeaf()){
      return (mainTree.getUserObject().toString().toLowerCase().startsWith(prefix));
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

  protected void generateSketchOutlineTree(DefaultMutableTreeNode node,
                                           DefaultMutableTreeNode codetree) {
    if (codetree == null)
      return;
    //log("Visi " + codetree + codetree.getUserObject().getClass().getSimpleName());
    if (!(codetree.getUserObject() instanceof ASTNodeWrapper))
      return;
    ASTNodeWrapper awnode = (ASTNodeWrapper) codetree.getUserObject(), aw2 = null;

    if (awnode.getNode() instanceof TypeDeclaration) {
      aw2 = new ASTNodeWrapper(awnode.getNode(),
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
      for (VariableDeclarationFragment vdf : (List<VariableDeclarationFragment>) fd
          .fragments()) {
        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(
                                                                    new ASTNodeWrapper(
                                                                                       vdf.getName(),
                                                                                       new CompletionCandidate(
                                                                                                               vdf)
                                                                                           .toString()));
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
}

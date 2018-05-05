package processing.mode.java.pdex;

import static processing.mode.java.pdex.ASTUtils.findAllOccurrences;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.WindowConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.SimpleName;

import processing.app.Language;
import processing.app.ui.EditorStatus;
import processing.app.ui.Toolkit;
import processing.app.ui.ZoomTreeCellRenderer;
import processing.mode.java.JavaEditor;
import processing.mode.java.pdex.PreprocessedSketch.SketchInterval;


class ShowUsage {
  final JDialog window;
  final JTree tree;

  final JavaEditor editor;
  final PreprocessingService pps;

  final Consumer<PreprocessedSketch> reloadListener;

  IBinding binding;


  ShowUsage(JavaEditor editor, PreprocessingService pps) {
    this.editor = editor;
    this.pps = pps;

    // Add show usage option
    JMenuItem showUsageItem =
      new JMenuItem(Language.text("editor.popup.show_usage"));
    showUsageItem.addActionListener(e -> handleShowUsage());
    editor.getTextArea().getRightClickPopup().add(showUsageItem);

    reloadListener = this::reloadShowUsage;

    { // Show Usage window
      window = new JDialog(editor);
      window.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
      window.setAutoRequestFocus(false);
      window.addComponentListener(new ComponentAdapter() {
        @Override
        public void componentHidden(ComponentEvent e) {
          binding = null;
          tree.setModel(null);
          pps.unregisterListener(reloadListener);
        }

        @Override
        public void componentShown(ComponentEvent e) {
          pps.registerListener(reloadListener);
        }
      });
      window.setSize(Toolkit.zoom(300, 400));
      window.setFocusableWindowState(false);
      Toolkit.setIcon(window);
      JScrollPane sp2 = new JScrollPane();
      tree = new JTree();
      ZoomTreeCellRenderer renderer =
        new ZoomTreeCellRenderer(editor.getMode());
      tree.setCellRenderer(renderer);
      renderer.setLeafIcon(null);
      renderer.setClosedIcon(null);
      renderer.setOpenIcon(null);
      renderer.setBackgroundSelectionColor(new Color(228, 248, 246));
      renderer.setBorderSelectionColor(new Color(0, 0, 0, 0));
      renderer.setTextSelectionColor(Color.BLACK);
      sp2.setViewportView(tree);
      window.add(sp2);
    }

    tree.addTreeSelectionListener(e -> {
      if (tree.getLastSelectedPathComponent() != null) {
        DefaultMutableTreeNode tnode =
          (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

        if (tnode.getUserObject() instanceof ShowUsageTreeNode) {
          ShowUsageTreeNode node = (ShowUsageTreeNode) tnode.getUserObject();
          editor.highlight(node.tabIndex, node.startTabOffset, node.stopTabOffset);
        }
      }
    });
  }


  // Thread: EDT
  void handleShowUsage() {
    int startOffset = editor.getSelectionStart();
    int stopOffset = editor.getSelectionStop();
    int tabIndex = editor.getSketch().getCurrentCodeIndex();

    pps.whenDoneBlocking(ps -> handleShowUsage(ps, tabIndex, startOffset, stopOffset));
  }


  // Thread: worker
  void handleShowUsage(PreprocessedSketch ps, int tabIndex,
                       int startTabOffset, int stopTabOffset) {
    // Map offsets
    int startJavaOffset = ps.tabOffsetToJavaOffset(tabIndex, startTabOffset);
    int stopJavaOffset = ps.tabOffsetToJavaOffset(tabIndex, stopTabOffset);

    // Find the node
    SimpleName name = ASTUtils.getSimpleNameAt(ps.compilationUnit, startJavaOffset, stopJavaOffset);
    if (name == null) {
      editor.statusMessage("Cannot find any name under cursor", EditorStatus.NOTICE);
      return;
    }

    // Find binding
    IBinding binding = ASTUtils.resolveBinding(name);
    if (binding == null) {
      editor.statusMessage("Cannot find usages, try to fix errors in your code first",
                           EditorStatus.NOTICE);
      return;
    }

    findUsageAndUpdateTree(ps, binding);
  }


  // Thread: worker
  void findUsageAndUpdateTree(PreprocessedSketch ps, IBinding binding) {

    this.binding = binding;

    // Get label
    String bindingType = "";
    switch (binding.getKind()) {
      case IBinding.METHOD:
        IMethodBinding method = (IMethodBinding) binding;
        if (method.isConstructor()) bindingType = "Constructor";
        else bindingType = "Method";
        break;
      case IBinding.TYPE:
        bindingType = "Type";
        break;
      case IBinding.VARIABLE:
        IVariableBinding variable = (IVariableBinding) binding;
        if (variable.isField()) bindingType = "Field";
        else if (variable.isParameter()) bindingType = "Parameter";
        else if (variable.isEnumConstant()) bindingType = "Enum constant";
        else bindingType = "Local variable";
        break;
    }

    // Find usages, map to tree nodes, add to root node
    String bindingKey = binding.getKey();
    List<SketchInterval> intervals =
        findAllOccurrences(ps.compilationUnit, bindingKey).stream()
            .map(ps::mapJavaToSketch)
            // remove occurrences which fall into generated header
            .filter(ps::inRange)
            // remove empty intervals (happens when occurence was inserted)
            .filter(in -> in.startPdeOffset < in.stopPdeOffset)
            .collect(Collectors.toList());

    int usageCount = intervals.size();

    // Get element name from PDE code if possible, otherwise use one from Java
    String elementName = intervals.stream()
        .findAny()
        .map(si -> ps.pdeCode.substring(si.startPdeOffset, si.stopPdeOffset))
        .orElseGet(binding::getName);

    // Create root node
    DefaultMutableTreeNode rootNode =
        new DefaultMutableTreeNode(bindingType + ": " + elementName);

    intervals.stream()
        // Convert to TreeNodes
        .map(in -> ShowUsageTreeNode.fromSketchInterval(ps, in))
        // Group by tab index
        .collect(Collectors.groupingBy(node -> node.tabIndex))
        // Stream Map Entries of (tab index) <-> (List<ShowUsageTreeNode>)
        .entrySet().stream()
        // Sort by tab index
        .sorted(Comparator.comparing(Map.Entry::getKey))
        .map(entry -> {
          Integer tabIndex = entry.getKey();
          List<ShowUsageTreeNode> nodes = entry.getValue();

          int count = nodes.size();
          String usageLabel = count == 1 ? "usage" : "usages";

          // Create new DefaultMutableTreeNode for this tab
          String tabLabel = "<html><font color=#222222>" +
              ps.sketch.getCode(tabIndex).getPrettyName() +
              "</font> <font color=#999999>" + count + " " + usageLabel + "</font></html>";
          DefaultMutableTreeNode tabNode = new DefaultMutableTreeNode(tabLabel);

          // Stream nodes belonging to this tab
          nodes.stream()
              // Convert TreeNodes to DefaultMutableTreeNodes
              .map(DefaultMutableTreeNode::new)
              // Add all as children of tab node
              .forEach(tabNode::add);
          return tabNode;
        })
        // Add all tab nodes as children of root node
        .forEach(rootNode::add);

    TreeModel treeModel = new DefaultTreeModel(rootNode);

    // Update tree
    EventQueue.invokeLater(() -> {
      tree.setModel(treeModel);

      // Expand all nodes
      for (int i = 0; i < tree.getRowCount(); i++) {
        tree.expandRow(i);
      }

      tree.setRootVisible(true);

      if (!window.isVisible()) {
        window.setVisible(true);
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice defaultScreen = ge.getDefaultScreenDevice();
        Rectangle rect = defaultScreen.getDefaultConfiguration().getBounds();
        int maxX = (int) rect.getMaxX() - window.getWidth();
        int x = Math.min(editor.getX() + editor.getWidth(), maxX);
        int y = (x == maxX) ? 10 : editor.getY();
        window.setLocation(x, y);
      }
      window.toFront();
      window.setTitle("Usage of \"" + elementName + "\" : " +
                          usageCount + " time(s)");
    });
  }


  // Thread: worker
  void reloadShowUsage(PreprocessedSketch ps) {
    if (binding != null) {
      findUsageAndUpdateTree(ps, binding);
    }
  }


  void hide() {
    window.setVisible(false);
  }


  void dispose() {
    if (window != null) {
      window.dispose();
    }
  }
}


class ShowUsageTreeNode {
  final int tabIndex;
  final int startTabOffset;
  final int stopTabOffset;

  final String text;


  ShowUsageTreeNode(int tabIndex, int startTabOffset, int stopTabOffset, String text) {
    this.tabIndex = tabIndex;
    this.startTabOffset = startTabOffset;
    this.stopTabOffset = stopTabOffset;
    this.text = text;
  }


  static ShowUsageTreeNode fromSketchInterval(PreprocessedSketch ps, SketchInterval in) {
    int lineStartPdeOffset = ps.pdeCode.lastIndexOf('\n', in.startPdeOffset) + 1;
    int lineStopPdeOffset = ps.pdeCode.indexOf('\n', in.stopPdeOffset);
    if (lineStopPdeOffset == -1) lineStopPdeOffset = ps.pdeCode.length();

    int highlightStartOffset = in.startPdeOffset - lineStartPdeOffset;
    int highlightStopOffset = in.stopPdeOffset - lineStartPdeOffset;

    int tabLine = ps.tabOffsetToTabLine(in.tabIndex, in.startTabOffset);

    // TODO: what a mess
    String line = ps.pdeCode.substring(lineStartPdeOffset, lineStopPdeOffset);
    String pre = line.substring(0, highlightStartOffset)
        .replace("&", "&amp;").replace(">", "&gt;").replace("<", "&lt;");
    String highlight = line.substring(highlightStartOffset, highlightStopOffset)
        .replace("&", "&amp;").replace(">", "&gt;").replace("<", "&lt;");
    String post = line.substring(highlightStopOffset)
        .replace("&", "&amp;").replace(">", "&gt;").replace("<", "&lt;");
    line = pre + "<font color=#222222><b>" + highlight + "</b></font>" + post;
    line = line.trim();


    String text = "<html><font color=#bbbbbb>" +
        (tabLine + 1) + "</font> <font color=#777777>" + line + "</font></html>";

    return new ShowUsageTreeNode(in.tabIndex, in.startTabOffset, in.stopTabOffset, text);
  }

  @Override
  public String toString() {
    return text;
  }
}

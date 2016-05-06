package processing.mode.java.pdex;

import com.google.classpath.ClassPath;
import com.google.classpath.RegExpResourceFilter;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import processing.app.Messages;
import processing.app.Preferences;
import processing.app.Sketch;
import processing.app.ui.EditorStatus;
import processing.app.ui.Toolkit;
import processing.mode.java.JavaEditor;
import processing.mode.java.JavaMode;
import processing.mode.java.pdex.PreprocessedSketch.SketchInterval;

import static processing.mode.java.pdex.ASTUtils.*;

public class PDEX {

  private static final boolean SHOW_DEBUG_TREE = false;

  private boolean enabled = true;

  private ErrorChecker errorChecker;

  private ShowUsage showUsage;
  private Rename rename;
  private DebugTree debugTree;

  private JavaEditor editor;
  private PreprocessingService pps;


  public PDEX(JavaEditor editor, PreprocessingService pps) {
    this.editor = editor;
    this.pps = pps;

    this.enabled = !editor.hasJavaTabs();

    errorChecker = new ErrorChecker(editor, pps);

    showUsage = new ShowUsage(editor, pps);
    rename = new Rename(editor);
    if (SHOW_DEBUG_TREE) {
      debugTree = new DebugTree(editor, pps);
    }
  }


  public void handleShowUsage(int tabIndex, int startTabOffset, int stopTabOffset) {
    Messages.log("* handleShowUsage");
    if (!enabled) return; // show usage disabled if java tabs
    pps.whenDoneBlocking(ps -> showUsage.findUsageAndUpdateTree(ps, tabIndex, startTabOffset, stopTabOffset));
  }


  public void handleRename(int tabIndex, int startTabOffset, int stopTabOffset) {
    Messages.log("* handleRename");
    if (!enabled) return;  // refactoring disabled w/ java tabs
    pps.whenDoneBlocking(ps -> rename.handleRename(ps, tabIndex, startTabOffset, stopTabOffset));
  }


  public void handleCtrlClick(int tabIndex, int offset) {
    Messages.log("* handleCtrlClick");
    if (!enabled) return;  // disabled w/ java tabs
    pps.whenDoneBlocking(ps -> handleCtrlClick(ps, tabIndex, offset));
  }


  public void handleHasJavaTabsChange(boolean hasJavaTabs) {
    enabled = !hasJavaTabs;
    if (!enabled) {
      showUsage.hide();
    }
  }


  public void notifySketchChanged() {
    errorChecker.notifySketchChanged();
  }


  public void dispose() {
    errorChecker.dispose();
    showUsage.dispose();
    rename.dispose();
    if (debugTree != null) {
      debugTree.dispose();
    }
  }


  // Thread: worker
  private void handleCtrlClick(PreprocessedSketch ps, int tabIndex, int offset) {
    ASTNode root = ps.compilationUnit;
    int javaOffset = ps.tabOffsetToJavaOffset(tabIndex, offset);

    SimpleName simpleName = getSimpleNameAt(root, javaOffset, javaOffset);

    if (simpleName == null) {
      Messages.log("no simple name found at click location");
      return;
    }

    IBinding binding = resolveBinding(simpleName);
    if (binding == null) {
      Messages.log("binding not resolved");
      return;
    }

    String key = binding.getKey();
    ASTNode decl = ps.compilationUnit.findDeclaringNode(key);
    if (decl == null) {
      Messages.log("decl not found, showing usage instead");
      showUsage.findUsageAndUpdateTree(ps, binding);
      return;
    }

    SimpleName declName = null;
    switch (binding.getKind()) {
      case IBinding.TYPE: declName = ((TypeDeclaration) decl).getName(); break;
      case IBinding.METHOD: declName = ((MethodDeclaration) decl).getName(); break;
      case IBinding.VARIABLE: declName = ((VariableDeclaration) decl).getName(); break;
    }
    if (declName == null) {
      Messages.log("decl name not found " + decl);
      return;
    }

    if (declName.equals(simpleName)) {
      showUsage.findUsageAndUpdateTree(ps, binding);
    } else {
      Messages.log("found declaration, offset " + decl.getStartPosition() + ", name: " + declName);
      SketchInterval si = ps.mapJavaToSketch(declName);
      EventQueue.invokeLater(() -> {
        editor.highlight(si.tabIndex, si.startTabOffset, si.stopTabOffset);
      });
    }
  }



  private static class ShowUsage {

    final JDialog window;
    final JTree tree;

    final JavaEditor editor;
    final PreprocessingService pps;

    final Consumer<PreprocessedSketch> reloadListener;

    IBinding binding;


    ShowUsage(JavaEditor editor, PreprocessingService pps) {
      this.editor = editor;
      this.pps = pps;

      reloadListener = this::reloadShowUsage;

      { // Show Usage window
        window = new JDialog(editor);
        window.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
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
        window.setSize(300, 400);
        Toolkit.setIcon(window);
        JScrollPane sp2 = new JScrollPane();
        tree = new JTree();
        DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) tree.getCellRenderer();
        renderer.setLeafIcon(null);
        renderer.setClosedIcon(null);
        renderer.setOpenIcon(null);
        sp2.setViewportView(tree);
        window.add(sp2);
      }

      tree.addTreeSelectionListener(e -> {
        if (tree.getLastSelectedPathComponent() == null) {
          return;
        }
        DefaultMutableTreeNode tnode = (DefaultMutableTreeNode) tree
            .getLastSelectedPathComponent();

        if (tnode.getUserObject() instanceof ShowUsageTreeNode) {
          ShowUsageTreeNode node = (ShowUsageTreeNode) tnode.getUserObject();
          editor.highlight(node.tabIndex, node.startTabOffset, node.stopTabOffset);
        }
      });

    }


    // Thread: worker
    void findUsageAndUpdateTree(PreprocessedSketch ps, int tabIndex,
                                int startTabOffset, int stopTabOffset) {
      // Map offsets
      int startJavaOffset = ps.tabOffsetToJavaOffset(tabIndex, startTabOffset);
      int stopJavaOffset = ps.tabOffsetToJavaOffset(tabIndex, stopTabOffset);

      // Find the node
      SimpleName name = ASTUtils.getSimpleNameAt(ps.compilationUnit, startJavaOffset, stopJavaOffset);
      if (name == null) {
        editor.statusMessage("Cannot find any name under cursor", EditorStatus.ERROR);
        return;
      }

      // Find binding
      IBinding binding = ASTUtils.resolveBinding(name);
      if (binding == null) {
        editor.statusMessage("Cannot find usages, try to fix errors in your code first",
                             EditorStatus.ERROR);
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

      String elementName = binding.getName();

      // Create root node
      DefaultMutableTreeNode rootNode =
          new DefaultMutableTreeNode(bindingType + ": " + elementName);

      int usageCount;

      { // Find usages, map to tree nodes, add to root node
        String bindingKey = binding.getKey();
        List<SketchInterval> intervals =
            findAllOccurrences(ps.compilationUnit, bindingKey).stream()
                .map(ps::mapJavaToSketch)
                // TODO: this has to be fixed with better token mapping
                // remove occurrences which fall into generated header
                .filter(in -> in.tabIndex != 0 ||
                    (in.startTabOffset >= 0 && in.stopTabOffset > 0))
                .collect(Collectors.toList());

        usageCount = intervals.size();

        Map<Integer, List<ShowUsageTreeNode>> tabGroupedTreeNodes = intervals.stream()
            // Convert to TreeNodes
            .map(in -> ShowUsageTreeNode.fromSketchInterval(ps, in))
            // Group by tab
            .collect(Collectors.groupingBy(node -> node.tabIndex));

        tabGroupedTreeNodes.entrySet().stream()
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

              nodes.stream()
                  // Convert TreeNodes to DefaultMutableTreeNodes
                  .map(DefaultMutableTreeNode::new)
                  // Add all as children of tab node
                  .forEach(tabNode::add);
              return tabNode;
            })
            // Add all tab nodes as children of root node
            .forEach(rootNode::add);
      }

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


  private static class ShowUsageTreeNode {

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



  private class Rename {

    final JDialog window;
    final JTextField textField;
    final JLabel oldNameLabel;

    final JavaEditor editor;

    IBinding binding;
    PreprocessedSketch ps;


    Rename(JavaEditor editor) {
      this.editor = editor;

      window = new JDialog(editor);
      window.setTitle("Enter new name:");
      window.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
      window.setModal(true);
      window.setResizable(false);
      window.addComponentListener(new ComponentAdapter() {
        @Override
        public void componentHidden(ComponentEvent e) {
          binding = null;
          ps = null;
        }
      });
      window.setSize(250, 130);
      window.setLayout(new BoxLayout(window.getContentPane(), BoxLayout.Y_AXIS));
      Toolkit.setIcon(window);

      { // Top panel

        // Text field
        textField = new JTextField();
        textField.setPreferredSize(new Dimension(150, 60));

        // Old name label
        oldNameLabel = new JLabel();
        oldNameLabel.setText("Old Name: ");

        // Top panel
        JPanel panelTop = new JPanel();
        panelTop.setLayout(new BoxLayout(panelTop, BoxLayout.Y_AXIS));
        panelTop.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panelTop.add(textField);
        panelTop.add(Box.createRigidArea(new Dimension(0, 10)));
        panelTop.add(oldNameLabel);
        window.add(panelTop);
      }

      { // Bottom panel
        JButton showUsageButton = new JButton("Show Usage");
        showUsageButton.addActionListener(e -> {
          showUsage.findUsageAndUpdateTree(ps, binding);
          window.setVisible(false);
        });

        JButton renameButton = new JButton("Rename");
        renameButton.addActionListener(e -> {
          if (textField.getText().length() == 0) {
            return;
          }
          String newName = textField.getText().trim();
          boolean isNewNameValid = newName.length() >= 1 &&
              newName.chars().limit(1).allMatch(Character::isUnicodeIdentifierStart) &&
              newName.chars().skip(1).allMatch(Character::isUnicodeIdentifierPart);
          if (!isNewNameValid) {
            JOptionPane.showMessageDialog(new JFrame(), "'" + newName
                + "' isn't a valid name.", "Uh oh..", JOptionPane.PLAIN_MESSAGE);
          } else {
            rename(ps, binding, newName);
            window.setVisible(false);
          }
        });

        JPanel panelBottom = new JPanel();
        panelBottom.setLayout(new BoxLayout(panelBottom, BoxLayout.X_AXIS));
        panelBottom.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panelBottom.add(Box.createHorizontalGlue());
        panelBottom.add(showUsageButton);
        panelBottom.add(Box.createRigidArea(new Dimension(15, 0)));
        panelBottom.add(renameButton);
        window.add(panelBottom);
      }

      window.setMinimumSize(window.getSize());
    }



    // Thread: worker
    void handleRename(PreprocessedSketch ps, int tabIndex, int startTabOffset, int stopTabOffset) {
      if (ps.hasSyntaxErrors) {
        editor.statusMessage("Can't perform action until syntax errors are fixed",
                             EditorStatus.WARNING);
        return;
      }

      ASTNode root = ps.compilationUnit;

      // Map offsets
      int startJavaOffset = ps.tabOffsetToJavaOffset(tabIndex, startTabOffset);
      int stopJavaOffset = ps.tabOffsetToJavaOffset(tabIndex, stopTabOffset);

      // Find the node
      SimpleName name = getSimpleNameAt(root, startJavaOffset, stopJavaOffset);
      if (name == null) {
        editor.statusMessage("Highlight the class/function/variable name first",
                             EditorStatus.NOTICE);
        return;
      }

      // Find binding
      IBinding binding = resolveBinding(name);
      if (binding == null) {
        editor.statusMessage(name.getIdentifier() + " isn't defined in this sketch, " +
                                 "so it cannot be renamed", EditorStatus.ERROR);
        return;
      }

      ASTNode decl = ps.compilationUnit.findDeclaringNode(binding.getKey());
      if (decl == null) {
        editor.statusMessage(name.getIdentifier() + " isn't defined in this sketch, " +
                                 "so it cannot be renamed", EditorStatus.ERROR);
        return;
      }

      // Display the rename dialog
      EventQueue.invokeLater(() -> {
        if (!window.isVisible()) {
          this.ps = ps;
          this.binding = binding;
          window.setLocation(editor.getX()
                                       + (editor.getWidth() - window.getWidth()) / 2,
                             editor.getY()
                                       + (editor.getHeight() - window.getHeight())
                                       / 2);
          oldNameLabel.setText("Current name: " + binding.getName());
          textField.setText(binding.getName());
          textField.requestFocus();
          textField.selectAll();
          window.setVisible(true);
          window.toFront();

          int x = editor.getX() + (editor.getWidth() - window.getWidth()) / 2;
          int y = editor.getY() + (editor.getHeight() - window.getHeight()) / 2;
          window.setLocation(x, y);
        }
      });
    }


    // Thread: EDT (we can't allow user to mess with sketch while renaming)
    void rename(PreprocessedSketch ps, IBinding binding, String newName) {
      CompilationUnit root = ps.compilationUnit;

      // Renaming constructor should rename class
      if (binding.getKind() == IBinding.METHOD) {
        IMethodBinding method = (IMethodBinding) binding;
        if (method.isConstructor()) {
          binding = method.getDeclaringClass();
        }
      }

      ASTNode decl = root.findDeclaringNode(binding.getKey());
      if (decl == null) return;

      showUsage.hide();

      List<SimpleName> occurrences = new ArrayList<>();
      occurrences.addAll(findAllOccurrences(root, binding.getKey()));

      // Renaming class should rename all constructors
      if (binding.getKind() == IBinding.TYPE) {
        ITypeBinding type = (ITypeBinding) binding;
        //type = type.getErasure();
        IMethodBinding[] methods = type.getDeclaredMethods();
        Arrays.stream(methods)
            .filter(IMethodBinding::isConstructor)
            .flatMap(c -> findAllOccurrences(root, c.getKey()).stream())
            .forEach(occurrences::add);
      }

      Map<Integer, List<SketchInterval>> mappedNodes = occurrences.stream()
          .map(ps::mapJavaToSketch)
          .collect(Collectors.groupingBy(interval -> interval.tabIndex));

      Sketch sketch = ps.sketch;

      editor.startCompoundEdit();

      int currentTabIndex = sketch.getCurrentCodeIndex();
      final int currentOffset = editor.getCaretOffset();
      mappedNodes.entrySet().forEach(entry -> {
        int tabIndex = entry.getKey();
        sketch.setCurrentCode(tabIndex);

        List<SketchInterval> nodes = entry.getValue();
        nodes.stream()
            // Replace from the end so all unprocess offsets stay valid
            .sorted(Comparator.comparing((SketchInterval si) -> si.startTabOffset).reversed())
            .forEach(si -> {
              // Make sure offsets are in bounds
              int length = editor.getTextArea().getDocumentLength();
              if (si.startTabOffset >= 0 && si.startTabOffset <= length &&
                  si.stopTabOffset >= 0 && si.stopTabOffset <= length) {
                // Replace the code
                editor.getTextArea().select(si.startTabOffset, si.stopTabOffset);
                editor.getTextArea().setSelectedText(newName);
              }
            });

        sketch.setModified(true);
      });

      int precedingIntervals =
          (int) mappedNodes.getOrDefault(currentTabIndex, Collections.emptyList())
              .stream()
              .filter(interval -> interval.stopTabOffset < currentOffset)
              .count();
      int intervalLengthDiff = newName.length() - binding.getName().length();
      int offsetDiff = precedingIntervals * intervalLengthDiff;

      sketch.setCurrentCode(currentTabIndex);
      editor.getTextArea().setCaretPosition(currentOffset + offsetDiff);

      editor.stopCompoundEdit();
    }


    void dispose() {
      if (window != null) {
        window.dispose();
      }
    }

  }



  private static class DebugTree {

    final JDialog window;
    final JTree tree;

    final Consumer<PreprocessedSketch> updateListener;


    DebugTree(JavaEditor editor, PreprocessingService pps) {
      updateListener = this::buildAndUpdateTree;

      window = new JDialog(editor);

      tree = new JTree() {
        @Override
        public String convertValueToText(Object value, boolean selected,
                                         boolean expanded, boolean leaf,
                                         int row, boolean hasFocus) {
          if (value instanceof DefaultMutableTreeNode) {
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) value;
            Object o = treeNode.getUserObject();
            if (o instanceof ASTNode) {
              ASTNode node = (ASTNode) o;
              return CompletionGenerator.getNodeAsString(node);
            }
          }
          return super.convertValueToText(value, selected, expanded, leaf, row, hasFocus);
        }
      };
      window.addComponentListener(new ComponentAdapter() {
        @Override
        public void componentHidden(ComponentEvent e) {
          pps.unregisterListener(updateListener);
          tree.setModel(null);
        }
      });
      window.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
      window.setBounds(new Rectangle(680, 100, 460, 620));
      window.setTitle("AST View - " + editor.getSketch().getName());
      JScrollPane sp = new JScrollPane();
      sp.setViewportView(tree);
      window.add(sp);
      pps.whenDone(updateListener);
      pps.registerListener(updateListener);


      tree.addTreeSelectionListener(e -> {
        if (tree.getLastSelectedPathComponent() == null) {
          return;
        }
        DefaultMutableTreeNode tnode =
            (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        if (tnode.getUserObject() instanceof ASTNode) {
          ASTNode node = (ASTNode) tnode.getUserObject();
          pps.whenDone(ps -> {
            SketchInterval si = ps.mapJavaToSketch(node);
            EventQueue.invokeLater(() -> {
              editor.highlight(si.tabIndex, si.startTabOffset, si.stopTabOffset);
            });
          });
        }
      });
    }


    void dispose() {
      if (window != null) {
        window.dispose();
      }
    }


    // Thread: worker
    void buildAndUpdateTree(PreprocessedSketch ps) {
      CompilationUnit cu = ps.compilationUnit;
      if (cu.types().isEmpty()){
        Messages.loge("No Type found in CU");
        return;
      }

      Deque<DefaultMutableTreeNode> treeNodeStack = new ArrayDeque<>();

      ASTNode type0 = (ASTNode) cu.types().get(0);
      type0.accept(new ASTVisitor() {
        @Override
        public boolean preVisit2(ASTNode node) {
          treeNodeStack.push(new DefaultMutableTreeNode(node));
          return super.preVisit2(node);
        }

        @Override
        public void postVisit(ASTNode node) {
          if (treeNodeStack.size() > 1) {
            DefaultMutableTreeNode treeNode = treeNodeStack.pop();
            treeNodeStack.peek().add(treeNode);
          }
        }
      });

      DefaultMutableTreeNode codeTree = treeNodeStack.pop();

      EventQueue.invokeLater(() -> {
        if (tree.hasFocus() || window.hasFocus()) {
          return;
        }
        tree.setModel(new DefaultTreeModel(codeTree));
        ((DefaultTreeModel) tree.getModel()).reload();
        tree.validate();
        if (!window.isVisible()) {
          window.setVisible(true);
        }
      });
    }

  }



  private static class ErrorChecker {

    // Delay delivering error check result after last sketch change #2677
    private final static long DELAY_BEFORE_UPDATE = 650;

    private ScheduledExecutorService scheduler;
    private volatile ScheduledFuture<?> scheduledUiUpdate = null;
    private volatile long nextUiUpdate = 0;

    private final Consumer<PreprocessedSketch> errorHandlerListener = this::handleSketchProblems;

    private JavaEditor editor;


    public ErrorChecker(JavaEditor editor, PreprocessingService pps) {
      this.editor = editor;
      scheduler = Executors.newSingleThreadScheduledExecutor();
      pps.registerListener(errorHandlerListener);
    }


    public void notifySketchChanged() {
      nextUiUpdate = System.currentTimeMillis() + DELAY_BEFORE_UPDATE;
    }


    public void dispose() {
      if (scheduler != null) {
        scheduler.shutdownNow();
      }
    }


    private void handleSketchProblems(PreprocessedSketch ps) {
      // Process problems
      final List<Problem> problems = ps.problems.stream()
          // Filter Warnings if they are not enabled
          .filter(iproblem -> !(iproblem.isWarning() && !JavaMode.warningsEnabled))
          // Hide a useless error which is produced when a line ends with
          // an identifier without a semicolon. "Missing a semicolon" is
          // also produced and is preferred over this one.
          // (Syntax error, insert ":: IdentifierOrNew" to complete Expression)
          // See: https://bugs.eclipse.org/bugs/show_bug.cgi?id=405780
          .filter(iproblem -> !iproblem.getMessage()
              .contains("Syntax error, insert \":: IdentifierOrNew\""))
          // Transform into our Problems
          .map(iproblem -> {
            int start = iproblem.getSourceStart();
            int stop = iproblem.getSourceEnd() + 1; // make it exclusive
            SketchInterval in = ps.mapJavaToSketch(start, stop);
            int line = ps.tabOffsetToTabLine(in.tabIndex, in.startTabOffset);
            Problem p = new Problem(iproblem, in.tabIndex, line);
            p.setPDEOffsets(in.startTabOffset, in.stopTabOffset);
            return p;
          })
          .collect(Collectors.toList());

      // Handle import suggestions
      if (JavaMode.importSuggestEnabled) {
        Map<String, List<Problem>> undefinedTypeProblems = problems.stream()
            // Get only problems with undefined types/names
            .filter(p -> {
              int id = p.getIProblem().getID();
              return id == IProblem.UndefinedType ||
                  id == IProblem.UndefinedName ||
                  id == IProblem.UnresolvedVariable;
            })
            // Group problems by the missing type/name
            .collect(Collectors.groupingBy(p -> p.getIProblem().getArguments()[0]));

        if (!undefinedTypeProblems.isEmpty()) {
          final ClassPath cp = ps.searchClassPath;

          // Get suggestions for each missing type, update the problems
          undefinedTypeProblems.entrySet().stream()
              .forEach(entry -> {
                String missingClass = entry.getKey();
                List<Problem> affectedProblems = entry.getValue();
                String[] suggestions = getImportSuggestions(cp, missingClass);
                affectedProblems.forEach(p -> p.setImportSuggestions(suggestions));
              });
        }
      }

      if (scheduledUiUpdate != null) {
        scheduledUiUpdate.cancel(true);
      }
      // Update UI after a delay. See #2677
      long delay = nextUiUpdate - System.currentTimeMillis();
      Runnable uiUpdater = () -> {
        if (nextUiUpdate > 0 && System.currentTimeMillis() >= nextUiUpdate) {
          EventQueue.invokeLater(() -> editor.setProblemList(problems));
        }
      };
      scheduledUiUpdate = scheduler.schedule(uiUpdater, delay,
                                             TimeUnit.MILLISECONDS);
    }


    public static String[] getImportSuggestions(ClassPath cp, String className) {
      RegExpResourceFilter regf = new RegExpResourceFilter(
          Pattern.compile(".*"),
          Pattern.compile("(.*\\$)?" + className + "\\.class",
                          Pattern.CASE_INSENSITIVE));

      String[] resources = cp.findResources("", regf);
      return Arrays.stream(resources)
          // remove ".class" suffix
          .map(res -> res.substring(0, res.length() - 6))
          // replace path separators with dots
          .map(res -> res.replace('/', '.'))
          // replace inner class separators with dots
          .map(res -> res.replace('$', '.'))
          // sort, prioritize clases from java. package
          .sorted((o1, o2) -> {
            // put java.* first, should be prioritized more
            boolean o1StartsWithJava = o1.startsWith("java");
            boolean o2StartsWithJava = o2.startsWith("java");
            if (o1StartsWithJava != o2StartsWithJava) {
              if (o1StartsWithJava) return -1;
              return 1;
            }
            return o1.compareTo(o2);
          })
          .toArray(String[]::new);
    }

  }
}

package processing.mode.java.pdex;

import com.google.classpath.ClassPath;
import com.google.classpath.ClassPathFactory;
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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import processing.app.Language;
import processing.app.Messages;
import processing.app.Platform;
import processing.app.Problem;
import processing.app.Sketch;
import processing.app.SketchCode;
import processing.app.syntax.SyntaxDocument;
import processing.app.ui.EditorStatus;
import processing.app.ui.Toolkit;
import processing.app.ui.ZoomTreeCellRenderer;
import processing.mode.java.JavaEditor;
import processing.mode.java.JavaMode;
import processing.mode.java.pdex.PreprocessedSketch.SketchInterval;

import static processing.mode.java.pdex.ASTUtils.*;


public class PDEX {

  private static final boolean SHOW_DEBUG_TREE = false;

  private boolean enabled = true;

  private ErrorChecker errorChecker;

  private InspectMode inspectMode;
  private ShowUsage showUsage;
  private Rename rename;
  private DebugTree debugTree;

  private PreprocessingService pps;


  public PDEX(JavaEditor editor, PreprocessingService pps) {
    this.pps = pps;

    this.enabled = !editor.hasJavaTabs();

    errorChecker = new ErrorChecker(editor, pps);

    inspectMode = new InspectMode(editor, pps);
    showUsage = new ShowUsage(editor, pps);
    rename = new Rename(editor, pps);
    if (SHOW_DEBUG_TREE) {
      debugTree = new DebugTree(editor, pps);
    }

    for (SketchCode code : editor.getSketch().getCode()) {
      Document document = code.getDocument();
      addDocumentListener(document);
    }

    sketchChanged();
  }


  public void addDocumentListener(Document doc) {
    if (doc != null) doc.addDocumentListener(sketchChangedListener);
  }


  protected final DocumentListener sketchChangedListener = new DocumentListener() {
    @Override
    public void insertUpdate(DocumentEvent e) {
      sketchChanged();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
      sketchChanged();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
      sketchChanged();
    }
  };


  public void sketchChanged() {
    errorChecker.notifySketchChanged();
    pps.notifySketchChanged();
  }


  public void preferencesChanged() {
    errorChecker.preferencesChanged();
    sketchChanged();
  }


  public void hasJavaTabsChanged(boolean hasJavaTabs) {
    enabled = !hasJavaTabs;
    if (!enabled) {
      showUsage.hide();
    }
  }


  public void dispose() {
    inspectMode.dispose();
    errorChecker.dispose();
    showUsage.dispose();
    rename.dispose();
    if (debugTree != null) {
      debugTree.dispose();
    }
  }


  public void documentChanged(Document newDoc) {
    addDocumentListener(newDoc);
  }


  private class InspectMode {
    boolean inspectModeEnabled;

    boolean isMouse1Down;
    boolean isMouse2Down;
    boolean isHotkeyDown;

    Predicate<MouseEvent> mouseEventHotkeyTest = Platform.isMacOS() ?
        InputEvent::isMetaDown : InputEvent::isControlDown;
    Predicate<KeyEvent> keyEventHotkeyTest = Platform.isMacOS() ?
        e -> e.getKeyCode() == KeyEvent.VK_META :
        e -> e.getKeyCode() == KeyEvent.VK_CONTROL;

    JavaEditor editor;
    PreprocessingService pps;

    InspectMode(JavaEditor editor, PreprocessingService pps) {
      this.editor = editor;
      this.pps = pps;

      // Add listeners

      JMenuItem showUsageItem = new JMenuItem(Language.text("editor.popup.jump_to_declaration"));
      showUsageItem.addActionListener(e -> handleInspect());
      editor.getTextArea().getRightClickPopup().add(showUsageItem);

      editor.getJavaTextArea().getPainter().addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
          isMouse1Down = isMouse1Down || (e.getButton() == MouseEvent.BUTTON1);
          isMouse2Down = isMouse2Down || (e.getButton() == MouseEvent.BUTTON2);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
          boolean releasingMouse1 = e.getButton() == MouseEvent.BUTTON1;
          boolean releasingMouse2 = e.getButton() == MouseEvent.BUTTON2;
          if (JavaMode.inspectModeHotkeyEnabled && inspectModeEnabled &&
              isMouse1Down && releasingMouse1) {
            handleInspect(e);
          } else if (!inspectModeEnabled && isMouse2Down && releasingMouse2) {
            handleInspect(e);
          }
          isMouse1Down = isMouse1Down && !releasingMouse1;
          isMouse2Down = isMouse2Down && !releasingMouse2;
        }
      });

      editor.getJavaTextArea().getPainter().addMouseMotionListener(new MouseAdapter() {
        @Override
        public void mouseDragged(MouseEvent e) {
          if (editor.isSelectionActive()) {
            // Mouse was dragged too much, disable
            inspectModeEnabled = false;
            // Cancel possible mouse 2 press
            isMouse2Down = false;
          }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
          isMouse1Down = false;
          isMouse2Down = false;
          isHotkeyDown = mouseEventHotkeyTest.test(e);
          inspectModeEnabled = isHotkeyDown;
        }
      });

      editor.getJavaTextArea().addMouseWheelListener(new MouseAdapter() {
        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
          // Editor was scrolled while mouse 1 was pressed, disable
          if (isMouse1Down) inspectModeEnabled = false;
        }
      });

      editor.getJavaTextArea().addKeyListener(new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
          isHotkeyDown = isHotkeyDown || keyEventHotkeyTest.test(e);
          // Enable if hotkey was just pressed and mouse 1 is not down
          inspectModeEnabled = inspectModeEnabled || (!isMouse1Down && isHotkeyDown);
        }

        @Override
        public void keyReleased(KeyEvent e) {
          isHotkeyDown = isHotkeyDown && !keyEventHotkeyTest.test(e);
          // Disable if hotkey was just released
          inspectModeEnabled = inspectModeEnabled && isHotkeyDown;
        }
      });
    }


    void handleInspect() {
      int off = editor.getSelectionStart();
      int tabIndex = editor.getSketch().getCurrentCodeIndex();

      pps.whenDoneBlocking(ps -> handleInspect(ps, tabIndex, off));
    }


    // Thread: EDT
    void handleInspect(MouseEvent evt) {
      int off = editor.getJavaTextArea().xyToOffset(evt.getX(), evt.getY());
      if (off < 0) return;
      int tabIndex = editor.getSketch().getCurrentCodeIndex();

      pps.whenDoneBlocking(ps -> handleInspect(ps, tabIndex, off));
    }


    // Thread: worker
    private void handleInspect(PreprocessedSketch ps, int tabIndex, int offset) {
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
        if (!ps.inRange(si)) return;
        EventQueue.invokeLater(() -> {
          editor.highlight(si.tabIndex, si.startTabOffset, si.stopTabOffset);
        });
      }
    }


    void dispose() {
      // Nothing to do
    }
  }


  static private class ShowUsage {
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


  static private class ShowUsageTreeNode {
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
    final PreprocessingService pps;

    IBinding binding;
    PreprocessedSketch ps;


    Rename(JavaEditor editor, PreprocessingService pps) {
      this.editor = editor;
      this.pps = pps;

      // Add rename option
      JMenuItem renameItem = new JMenuItem(Language.text("editor.popup.rename"));
      renameItem.addActionListener(e -> handleRename());
      editor.getTextArea().getRightClickPopup().add(renameItem);


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


    // Thread: EDT
    void handleRename() {
      int startOffset = editor.getSelectionStart();
      int stopOffset = editor.getSelectionStop();
      int tabIndex = editor.getSketch().getCurrentCodeIndex();

      pps.whenDoneBlocking(ps -> handleRename(ps, tabIndex, startOffset, stopOffset));
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
          oldNameLabel.setText("Current name: " + binding.getName());
          textField.setText(binding.getName());
          textField.requestFocus();
          textField.selectAll();
          int x = editor.getX() + (editor.getWidth() - window.getWidth()) / 2;
          int y = editor.getY() + (editor.getHeight() - window.getHeight()) / 2;
          window.setLocation(x, y);
          window.setVisible(true);
          window.toFront();
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
          .filter(ps::inRange)
          .collect(Collectors.groupingBy(interval -> interval.tabIndex));

      Sketch sketch = ps.sketch;

      editor.startCompoundEdit();

      mappedNodes.entrySet().forEach(entry -> {
        int tabIndex = entry.getKey();
        SketchCode sketchCode = sketch.getCode(tabIndex);

        SyntaxDocument document = (SyntaxDocument) sketchCode.getDocument();

        List<SketchInterval> nodes = entry.getValue();
        nodes.stream()
            // Replace from the end so all unprocess offsets stay valid
            .sorted(Comparator.comparing((SketchInterval si) -> si.startTabOffset).reversed())
            .forEach(si -> {
              // Make sure offsets are in bounds
              int documentLength = document.getLength();
              if (si.startTabOffset >= 0 && si.startTabOffset <= documentLength &&
                  si.stopTabOffset >= 0 && si.stopTabOffset <= documentLength) {
                // Replace the code
                int length = si.stopTabOffset - si.startTabOffset;
                try {
                  document.remove(si.startTabOffset, length);
                  document.insertString(si.startTabOffset, newName, null);
                } catch (BadLocationException e) { /* Whatever */ }
              }
            });

        try {
          sketchCode.setProgram(document.getText(0, document.getLength()));
        } catch (BadLocationException e) { /* Whatever */ }
        sketchCode.setModified(true);
      });

      editor.stopCompoundEdit();

      editor.repaintHeader();

      int currentTabIndex = sketch.getCurrentCodeIndex();
      final int currentOffset = editor.getCaretOffset();

      int precedingIntervals =
          (int) mappedNodes.getOrDefault(currentTabIndex, Collections.emptyList())
              .stream()
              .filter(interval -> interval.stopTabOffset < currentOffset)
              .count();
      int intervalLengthDiff = newName.length() - binding.getName().length();
      int offsetDiff = precedingIntervals * intervalLengthDiff;

      editor.getTextArea().setCaretPosition(currentOffset + offsetDiff);
    }


    void dispose() {
      if (window != null) {
        window.dispose();
      }
    }
  }


  static private class DebugTree {
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
      tree.setCellRenderer(new ZoomTreeCellRenderer(editor.getMode()));
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
        if (tree.getLastSelectedPathComponent() != null) {
          DefaultMutableTreeNode tnode =
            (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
          if (tnode.getUserObject() instanceof ASTNode) {
            ASTNode node = (ASTNode) tnode.getUserObject();
            pps.whenDone(ps -> {
              SketchInterval si = ps.mapJavaToSketch(node);
              if (!ps.inRange(si)) return;
              EventQueue.invokeLater(() -> {
                editor.highlight(si.tabIndex, si.startTabOffset, si.stopTabOffset);
              });
            });
          }
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


  static private class ErrorChecker {
    // Delay delivering error check result after last sketch change #2677
    private final static long DELAY_BEFORE_UPDATE = 650;

    private ScheduledExecutorService scheduler;
    private volatile ScheduledFuture<?> scheduledUiUpdate = null;
    private volatile long nextUiUpdate = 0;
    private volatile boolean enabled = true;

    private final Consumer<PreprocessedSketch> errorHandlerListener = this::handleSketchProblems;

    private JavaEditor editor;
    private PreprocessingService pps;


    public ErrorChecker(JavaEditor editor, PreprocessingService pps) {
      this.editor = editor;
      this.pps = pps;
      scheduler = Executors.newSingleThreadScheduledExecutor();
      this.enabled = JavaMode.errorCheckEnabled;
      if (enabled) {
        pps.registerListener(errorHandlerListener);
      }
    }


    public void notifySketchChanged() {
      nextUiUpdate = System.currentTimeMillis() + DELAY_BEFORE_UPDATE;
    }


    public void preferencesChanged() {
      if (enabled != JavaMode.errorCheckEnabled) {
        enabled = JavaMode.errorCheckEnabled;
        if (enabled) {
          pps.registerListener(errorHandlerListener);
        } else {
          pps.unregisterListener(errorHandlerListener);
          editor.setProblemList(Collections.emptyList());
          nextUiUpdate = 0;
        }
      }
    }


    public void dispose() {
      if (scheduler != null) {
        scheduler.shutdownNow();
      }
    }


    private void handleSketchProblems(PreprocessedSketch ps) {
      Map<String, String[]> suggCache =
          JavaMode.importSuggestEnabled ? new HashMap<>() : Collections.emptyMap();

      final List<Problem> problems = new ArrayList<>();

      IProblem[] iproblems = ps.compilationUnit.getProblems();

      { // Handle missing brace problems
        IProblem missingBraceProblem = Arrays.stream(iproblems)
            .filter(ErrorChecker::isMissingBraceProblem)
            .findFirst()
            // Ignore if it is at the end of file
            .filter(p -> p.getSourceEnd() + 1 < ps.javaCode.length())
            // Ignore if the tab number does not match our detected tab number
            .filter(p -> ps.missingBraceProblems.isEmpty() ||
                ps.missingBraceProblems.get(0).getTabIndex() ==
                    ps.mapJavaToSketch(p.getSourceStart(), p.getSourceEnd()+1).tabIndex
            )
            .orElse(null);

        // If there is missing brace ignore all other problems
        if (missingBraceProblem != null) {
          // Prefer ECJ problem, shows location more accurately
          iproblems = new IProblem[]{missingBraceProblem};
        } else if (!ps.missingBraceProblems.isEmpty()) {
          // Fallback to manual detection
          problems.addAll(ps.missingBraceProblems);
        }
      }

      AtomicReference<ClassPath> searchClassPath = new AtomicReference<>(null);

      if (problems.isEmpty()) {
        List<Problem> cuProblems = Arrays.stream(iproblems)
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
              if (in == SketchInterval.BEFORE_START) return null;
              int line = ps.tabOffsetToTabLine(in.tabIndex, in.startTabOffset);
              JavaProblem p = JavaProblem.fromIProblem(iproblem, in.tabIndex, line);
              p.setPDEOffsets(in.startTabOffset, in.stopTabOffset);

              // Handle import suggestions
              if (JavaMode.importSuggestEnabled && isUndefinedTypeProblem(iproblem)) {
                ClassPath cp = searchClassPath.updateAndGet(prev -> prev != null ?
                    prev : new ClassPathFactory().createFromPaths(ps.searchClassPathArray));
                String[] s = suggCache.computeIfAbsent(iproblem.getArguments()[0],
                                                       name -> getImportSuggestions(cp, name));
                p.setImportSuggestions(s);
              }

              return p;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        problems.addAll(cuProblems);
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


    static private boolean isUndefinedTypeProblem(IProblem iproblem) {
      int id = iproblem.getID();
      return id == IProblem.UndefinedType ||
          id == IProblem.UndefinedName ||
          id == IProblem.UnresolvedVariable;
    }


    static private boolean isMissingBraceProblem(IProblem iproblem) {
      switch (iproblem.getID()) {
        case IProblem.ParsingErrorInsertToComplete: {
          char brace = iproblem.getArguments()[0].charAt(0);
          return brace == '{' || brace == '}';
        }
        case IProblem.ParsingErrorInsertTokenAfter: {
          char brace = iproblem.getArguments()[1].charAt(0);
          return brace == '{' || brace == '}';
        }
        default:
          return false;
      }
    }


    static public String[] getImportSuggestions(ClassPath cp, String className) {
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

package processing.mode.java.pdex;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.text.BadLocationException;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.SimpleName;

import processing.app.Language;
import processing.app.Platform;
import processing.app.Sketch;
import processing.app.SketchCode;
import processing.app.syntax.SyntaxDocument;
import processing.app.ui.EditorStatus;
import processing.app.ui.Toolkit;
import processing.mode.java.JavaEditor;
import processing.mode.java.pdex.PreprocessedSketch.SketchInterval;

import static processing.mode.java.pdex.ASTUtils.findAllOccurrences;
import static processing.mode.java.pdex.ASTUtils.getSimpleNameAt;
import static processing.mode.java.pdex.ASTUtils.resolveBinding;


class Rename {
  final JavaEditor editor;
  final PreprocessingService pps;
  final ShowUsage showUsage;

  final JDialog window;
  final JTextField textField;
  final JLabel oldNameLabel;

  IBinding binding;
  PreprocessedSketch ps;


  Rename(JavaEditor editor, PreprocessingService pps, ShowUsage showUsage) {
    this.editor = editor;
    this.pps = pps;
    this.showUsage = showUsage;

    // Add rename option
    JMenuItem renameItem = new JMenuItem(Language.text("editor.popup.rename"));
    renameItem.addActionListener(e -> handleRename());
    editor.getTextArea().getRightClickPopup().add(renameItem);

    window = new JDialog(editor);
    JRootPane rootPane = window.getRootPane();
    window.setTitle("Rename");
    window.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
    Toolkit.registerWindowCloseKeys(rootPane, new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        window.setVisible(false);
      }
    });
    Toolkit.setIcon(window);

    window.setModal(true);
    window.setResizable(false);
    window.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentHidden(ComponentEvent e) {
        binding = null;
        ps = null;
      }
    });
    //window.setSize(Toolkit.zoom(250, 130));
    //window.setLayout(new BoxLayout(window.getContentPane(), BoxLayout.Y_AXIS));
    Box windowBox = Box.createVerticalBox();
    Toolkit.setBorder(windowBox);
    final int GAP = Toolkit.zoom(5);

    { // old name
      Box oldBox = Box.createHorizontalBox();
      oldNameLabel = new JLabel("Current Name: ");
      oldBox.add(oldNameLabel);
      //oldBox.add(Box.createHorizontalStrut(10));
      oldBox.add(Box.createHorizontalGlue());
      windowBox.add(oldBox);
      windowBox.add(Box.createVerticalStrut(GAP));
    }

    { // new name
      Box newBox = Box.createHorizontalBox();
      JLabel newNameLabel = new JLabel("New Name: ");
      newBox.add(newNameLabel);
      newBox.add(textField = new JTextField(20));
      newBox.add(Box.createHorizontalGlue());
      windowBox.add(newBox);
      windowBox.add(Box.createVerticalStrut(GAP*2));
    }

    { // button panel
      JButton showUsageButton = new JButton("Show Usage");
      showUsageButton.addActionListener(e -> {
        showUsage.findUsageAndUpdateTree(ps, binding);
        window.setVisible(false);
      });

      JButton renameButton = new JButton("Rename");
      renameButton.addActionListener(e -> {
        final String newName = textField.getText().trim();
        if (!newName.isEmpty()) {
          if (newName.length() >= 1 &&
              newName.chars().limit(1).allMatch(Character::isUnicodeIdentifierStart) &&
              newName.chars().skip(1).allMatch(Character::isUnicodeIdentifierPart)) {
            rename(ps, binding, newName);
            window.setVisible(false);
          } else {
            String msg = String.format("'%s' is not a valid name", newName);
            JOptionPane.showMessageDialog(editor, msg, "Naming is Hard",
                                          JOptionPane.PLAIN_MESSAGE);
          }
        }
      });
      rootPane.setDefaultButton(renameButton);

      //JPanel panelBottom = new JPanel();
      //panelBottom.setLayout(new BoxLayout(panelBottom, BoxLayout.X_AXIS));
      Box buttonBox = Box.createHorizontalBox();
      //Toolkit.setBorder(panelBottom, 5, 5, 5, 5);

      buttonBox.add(Box.createHorizontalGlue());
      buttonBox.add(showUsageButton);
      if (!Platform.isMacOS()) {
        buttonBox.add(Box.createHorizontalStrut(GAP));
      }
      buttonBox.add(renameButton);
      buttonBox.add(Box.createHorizontalGlue());

      Dimension showDim = showUsageButton.getPreferredSize();
      Dimension renameDim = renameButton.getPreferredSize();
      final int niceSize = Math.max(showDim.width, renameDim.width) + GAP;
      final Dimension buttonDim = new Dimension(niceSize, showDim.height);
      showUsageButton.setPreferredSize(buttonDim);
      renameButton.setPreferredSize(buttonDim);

      windowBox.add(buttonBox);

      //window.add(panelBottom);
    }
    window.add(windowBox);
    window.pack();
    //window.setMinimumSize(window.getSize());
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
      editor.statusMessage("Cannot rename until syntax errors are fixed",
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
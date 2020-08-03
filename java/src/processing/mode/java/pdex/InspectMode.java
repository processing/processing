package processing.mode.java.pdex;

import java.awt.EventQueue;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.function.Predicate;

import javax.swing.JMenuItem;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;

import processing.app.Language;
import processing.app.Messages;
import processing.app.Platform;
import processing.mode.java.JavaEditor;
import processing.mode.java.JavaMode;
import processing.mode.java.pdex.PreprocessedSketch.SketchInterval;

import static processing.mode.java.pdex.ASTUtils.getSimpleNameAt;
import static processing.mode.java.pdex.ASTUtils.resolveBinding;


class InspectMode {
  final JavaEditor editor;
  final PreprocessingService pps;
  final ShowUsage usage;

  boolean inspectModeEnabled;

  boolean isMouse1Down;
  boolean isMouse2Down;
  boolean isHotkeyDown;

  Predicate<MouseEvent> mouseEventHotkeyTest = Platform.isMacOS() ?
      InputEvent::isMetaDown : InputEvent::isControlDown;
  Predicate<KeyEvent> keyEventHotkeyTest = Platform.isMacOS() ?
      e -> e.getKeyCode() == KeyEvent.VK_META :
      e -> e.getKeyCode() == KeyEvent.VK_CONTROL;


  InspectMode(JavaEditor editor, PreprocessingService pps, ShowUsage usage) {
    this.editor = editor;
    this.pps = pps;
    this.usage = usage;

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
      usage.findUsageAndUpdateTree(ps, binding);
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
      usage.findUsageAndUpdateTree(ps, binding);
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
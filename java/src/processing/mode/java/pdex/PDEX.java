package processing.mode.java.pdex;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import processing.app.SketchCode;
import processing.mode.java.JavaEditor;


public class PDEX {
  static private final boolean SHOW_DEBUG_TREE = false;

  private boolean enabled = true;

  private ErrorChecker errorChecker;

  private InspectMode inspect;
  private ShowUsage usage;
  private Rename rename;
  private DebugTree debugTree;

  private PreprocessingService pps;


  public PDEX(JavaEditor editor, PreprocessingService pps) {
    this.pps = pps;

    this.enabled = !editor.hasJavaTabs();

    errorChecker = new ErrorChecker(editor, pps);

    usage = new ShowUsage(editor, pps);
    inspect = new InspectMode(editor, pps, usage);
    rename = new Rename(editor, pps, usage);

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


  final DocumentListener sketchChangedListener = new DocumentListener() {
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
      usage.hide();
    }
  }


  public void dispose() {
    inspect.dispose();
    errorChecker.dispose();
    usage.dispose();
    rename.dispose();
    if (debugTree != null) {
      debugTree.dispose();
    }
  }


  public void documentChanged(Document newDoc) {
    addDocumentListener(newDoc);
  }
}

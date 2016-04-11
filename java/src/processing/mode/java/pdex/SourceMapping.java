package processing.mode.java.pdex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

import processing.app.Base;
import processing.core.PApplet;

import static java.awt.SystemColor.text;


public class SourceMapping {

  private static final Comparator<Edit> INPUT_OFFSET_COMP =
      (o1, o2) -> Integer.compare(o1.fromOffset, o2.fromOffset);

  private static final Comparator<Edit> OUTPUT_OFFSET_COMP =
      (o1, o2) -> Integer.compare(o1.toOffset, o2.toOffset);

  private boolean applied;

  private List<Edit> edits = new ArrayList<>();

  private List<Edit> inMap = new ArrayList<>();
  private List<Edit> outMap = new ArrayList<>();


  public void add(Edit edit) {
    edits.add(edit);
  }


  public void addAll(Collection<Edit> edits) {
    this.edits.addAll(edits);
  }


  public String apply(CharSequence input) {

    final int inLength = input.length();
    final StringBuilder output = new StringBuilder(inLength);

    // Make copies of Edits to preserve original edits
    List<Edit> inEdits = edits.stream().map(Edit::new).collect(Collectors.toList());
    List<Edit> outEdits = new ArrayList<>(inEdits);

    // Edits sorted by input offsets
    Collections.sort(inEdits, INPUT_OFFSET_COMP);

    // Edits sorted by output offsets
    Collections.sort(outEdits, OUTPUT_OFFSET_COMP);

    // TODO: add some validation

    // Input
    ListIterator<Edit> inIt = inEdits.listIterator();
    Edit inEdit = inIt.hasNext() ? inIt.next() : null;
    int inEditOff = inEdit == null ? input.length() : inEdit.fromOffset;

    // Output
    ListIterator<Edit> outIt = outEdits.listIterator();
    Edit outEdit = outIt.hasNext() ? outIt.next() : null;
    int outEditOff = outEdit == null ? input.length() : outEdit.toOffset;

    int offset = 0;

    inMap.clear();
    outMap.clear();

    // Walk through the input, apply changes, create mapping
    while (offset < inLength || inEdit != null || outEdit != null) {

      { // Copy the unmodified portion of the input, create mapping for it
        int nextEditOffset = Math.min(inEditOff, outEditOff);

        { // Insert move block to have mapping for unmodified portions too
          int length = nextEditOffset - offset;
          if (length > 0) {
            Edit ch = Edit.move(offset, length, output.length());
            inMap.add(ch);
            outMap.add(ch);
          }
        }

        // Copy the block without changes from the input
        output.append(input, offset, nextEditOffset);

        // Move offset accordingly
        offset = nextEditOffset;
      }

      // Process encountered input edits
      while (inEdit != null && offset >= inEditOff) {
        offset += inEdit.fromLength;
        inMap.add(inEdit);
        inEdit = inIt.hasNext() ? inIt.next() : null;
        inEditOff = inEdit != null ? inEdit.fromOffset : inLength;
      }

      // Process encountered output edits
      while (outEdit != null && offset >= outEditOff) {
        outEdit.toOffset = output.length();
        outMap.add(outEdit);
        if (outEdit.toLength > 0) {
          if (outEdit.outputText != null) {
            output.append(outEdit.outputText);
          } else {
            output.append(input, outEdit.fromOffset, outEdit.fromOffset + outEdit.fromLength);
          }
        }
        outEdit = outIt.hasNext() ? outIt.next() : null;
        outEditOff = outEdit != null ? outEdit.toOffset : inLength;
      }
    }

    applied = true;

    return output.toString();
  }


  public int getInputOffset(int outputOffset) {
    if (Base.DEBUG) checkApplied();
    Edit searchKey = new Edit(0, 0, outputOffset, Integer.MAX_VALUE, null);
    int i = Collections.binarySearch(outMap, searchKey, OUTPUT_OFFSET_COMP);
    if (i < 0) {
      i = -(i + 1);
    }
    i = PApplet.constrain(i-1, 0, outMap.size()-1);
    Edit edit = outMap.get(i);
    int diff = outputOffset - edit.toOffset;
    return edit.fromOffset + Math.min(diff, Math.max(0, edit.fromLength - 1));
  }


  public int getOutputOffset(int inputOffset) {
    if (Base.DEBUG) checkApplied();
    Edit searchKey = new Edit(inputOffset, Integer.MAX_VALUE, 0, 0, null);
    int i = Collections.binarySearch(inMap, searchKey, INPUT_OFFSET_COMP);
    if (i < 0) {
      i = -(i + 1);
    }
    i = PApplet.constrain(i-1, 0, inMap.size()-1);
    Edit edit = inMap.get(i);
    int diff = inputOffset - edit.fromOffset;
    return edit.toOffset + Math.min(diff, Math.max(0, edit.toLength - 1));
  }


  public void clear() {
    applied = false;
    edits.clear();
    inMap.clear();
    outMap.clear();
  }


  private void checkNotApplied() {
    if (applied) throw new RuntimeException("this mapping was already applied");
  }


  private void checkApplied() {
    if (!applied) throw new RuntimeException("this mapping was not applied yet");
  }


  @Override
  public String toString() {
    return "SourceMapping{" +
        "edits=" + edits +
        ", applied=" + applied +
        '}';
  }


  protected static class Edit {

    static Edit insert(int offset, String text) {
      return new Edit(offset, 0, offset, text.length(), text);
    }

    static Edit replace(int offset, int length, String text) {
      return new Edit(offset, length, offset, text.length(), text);
    }

    static Edit move(int fromOffset, int length, int toOffset) {
      Edit result = new Edit(fromOffset, length, toOffset, length, null);
      result.toOffset = toOffset;
      return result;
    }

    static Edit delete(int position, int length) {
      return new Edit(position, length, position, 0, null);
    }

    Edit(Edit edit) {
      this.fromOffset = edit.fromOffset;
      this.fromLength = edit.fromLength;
      this.toOffset = edit.toOffset;
      this.toLength = edit.toLength;
      this.outputText = edit.outputText;
    }

    Edit(int fromOffset, int fromLength, int toOffset, int toLength, String text) {
      this.fromOffset = fromOffset;
      this.fromLength = fromLength;
      this.toOffset = toOffset;
      this.toLength = toLength;
      this.outputText = text;
    }

    final int fromOffset;
    final int fromLength;
    int toOffset;
    final int toLength;
    final String outputText;

    @Override
    public String toString() {
      return "Edit{" +
          "from=" + fromOffset + ":" + fromLength +
          ", to=" + toOffset + ":" + toLength +
          ((text != null) ? (", text='" + outputText + '\'') : "") +
          '}';
    }
  }

}

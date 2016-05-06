package processing.mode.java.pdex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

import processing.core.PApplet;


public class TextTransform {

  private static final Comparator<Edit> INPUT_OFFSET_COMP =
      (o1, o2) -> Integer.compare(o1.fromOffset, o2.fromOffset);

  private static final Comparator<Edit> OUTPUT_OFFSET_COMP =
      (o1, o2) -> Integer.compare(o1.toOffset, o2.toOffset);


  private CharSequence input;

  private List<Edit> edits = new ArrayList<>();

  private List<Edit> inMap = new ArrayList<>();
  private List<Edit> outMap = new ArrayList<>();

  private boolean built;
  private int builtForLength;


  TextTransform(CharSequence input) {
    this.input = input;
  }


  public void add(Edit edit) {
    edits.add(edit);
    built = false;
  }


  public void addAll(Collection<Edit> edits) {
    this.edits.addAll(edits);
    built = false;
  }


  public String apply() {
    final int inLength = input.length();
    final StringBuilder output = new StringBuilder(inLength);

    buildIfNeeded(inLength);

    outMap.stream()
        // Filter out Delete edits
        .filter(outEdit -> outEdit.toLength > 0)
        .forEach(outEdit -> {
          if (outEdit.outputText != null) {
            // Insert or Replace edit
            output.append(outEdit.outputText);
          } else {
            // Move edit
            output.append(input, outEdit.fromOffset, outEdit.fromOffset + outEdit.fromLength);
          }
        });

    return output.toString();
  }


  public OffsetMapper getMapper() {
    int inLength = input.length();
    buildIfNeeded(inLength);
    return new SimpleOffsetMapper(inMap, outMap);
  }


  private void buildIfNeeded(int inLength) {
    if (built && inLength == builtForLength) return;

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
    int inEditOff = inEdit == null ? inLength : inEdit.fromOffset;

    // Output
    ListIterator<Edit> outIt = outEdits.listIterator();
    Edit outEdit = outIt.hasNext() ? outIt.next() : null;
    int outEditOff = outEdit == null ? inLength : outEdit.toOffset;

    int inOffset = 0;
    int outOffset = 0;

    inMap.clear();
    outMap.clear();

    // Walk through the input, apply changes, create mapping
    while (inOffset < inLength || inEdit != null || outEdit != null) {

      { // Create mapping for unmodified portion of the input
        int nextEditOffset = Math.min(inEditOff, outEditOff);

        { // Insert move block to have mapping for unmodified portions too
          int length = nextEditOffset - inOffset;
          if (length > 0) {
            Edit ch = Edit.move(inOffset, length, outOffset);
            inMap.add(ch);
            outMap.add(ch);
          }
        }

        // Move offsets accordingly
        outOffset += nextEditOffset - inOffset;
        inOffset = nextEditOffset;
      }

      // Process encountered input edits
      while (inEdit != null && inOffset >= inEditOff) {
        inOffset += inEdit.fromLength;
        if (inEdit.fromLength > 0) inMap.add(inEdit);
        inEdit = inIt.hasNext() ? inIt.next() : null;
        inEditOff = inEdit != null ? inEdit.fromOffset : inLength;
      }

      // Process encountered output edits
      while (outEdit != null && inOffset >= outEditOff) {
        outEdit.toOffset = outOffset;
        if (outEdit.toLength > 0) outMap.add(outEdit);
        outOffset += outEdit.toLength;
        outEdit = outIt.hasNext() ? outIt.next() : null;
        outEditOff = outEdit != null ? outEdit.toOffset : inLength;
      }
    }

    built = true;
    builtForLength = inLength;
  }


  @Override
  public String toString() {
    return "SourceMapping{" +
        "edits=" + edits +
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

    private final int fromOffset;
    private final int fromLength;
    private int toOffset;
    private final int toLength;
    private final String outputText;

    @Override
    public String toString() {
      return "Edit{" +
          "from=" + fromOffset + ":" + fromLength +
          ", to=" + toOffset + ":" + toLength +
          ((outputText != null) ? (", text='" + outputText + '\'') : "") +
          '}';
    }
  }


  protected interface OffsetMapper {
    int getInputOffset(int outputOffset);
    int getOutputOffset(int inputOffset);
    OffsetMapper thenMapping(OffsetMapper mapper);
    OffsetMapper EMPTY_MAPPER = CompositeOffsetMapper.of();
  }


  private static class SimpleOffsetMapper implements OffsetMapper {
    private List<Edit> inMap = new ArrayList<>();
    private List<Edit> outMap = new ArrayList<>();

    private int outputOffsetOfInputStart;
    private int inputOffsetOfOutputStart;

    private SimpleOffsetMapper(List<Edit> inMap, List<Edit> outMap) {
      this.inMap.addAll(inMap);
      this.outMap.addAll(outMap);

      Edit inStart = null;
      for (Edit in : this.inMap) {
        inStart = in;
        if (in.fromLength > 0) break;
      }
      outputOffsetOfInputStart = inStart == null ? 0 : inStart.toOffset;

      Edit outStart = null;
      for (Edit out : this.inMap) {
        outStart = out;
        if (out.toLength > 0) break;
      }
      inputOffsetOfOutputStart = outStart == null ? 0 : outStart.fromOffset;
    }

    @Override
    public int getInputOffset(int outputOffset) {
      if (outputOffset < outputOffsetOfInputStart) return -1;
      Edit searchKey = new Edit(0, 0, outputOffset, Integer.MAX_VALUE, null);
      int i = Collections.binarySearch(outMap, searchKey, OUTPUT_OFFSET_COMP);
      if (i < 0) {
        i = -(i + 1);
        i -= 1;
      }
      i = PApplet.constrain(i, 0, outMap.size()-1);
      Edit edit = outMap.get(i);
      int diff = outputOffset - edit.toOffset;
      return edit.fromOffset + Math.min(diff, Math.max(0, edit.fromLength - 1));
    }

    @Override
    public int getOutputOffset(int inputOffset) {
      if (inputOffset < inputOffsetOfOutputStart) return -1;
      Edit searchKey = new Edit(inputOffset, Integer.MAX_VALUE, 0, 0, null);
      int i = Collections.binarySearch(inMap, searchKey, INPUT_OFFSET_COMP);
      if (i < 0) {
        i = -(i + 1);
        i -= 1;
      }
      i = PApplet.constrain(i, 0, inMap.size()-1);
      Edit edit = inMap.get(i);
      int diff = inputOffset - edit.fromOffset;
      return edit.toOffset + Math.min(diff, Math.max(0, edit.toLength - 1));
    }

    @Override
    public OffsetMapper thenMapping(OffsetMapper mapper) {
      return CompositeOffsetMapper.of(this, mapper);
    }
  }


  private static class CompositeOffsetMapper implements OffsetMapper {
    private List<OffsetMapper> mappers = new ArrayList<>();

    public static CompositeOffsetMapper of(OffsetMapper... inMappers) {
      CompositeOffsetMapper composite = new CompositeOffsetMapper();

      // Add mappers one by one, unwrap if Composite
      for (OffsetMapper mapper : inMappers) {
        if (mapper instanceof CompositeOffsetMapper) {
          composite.mappers.addAll(((CompositeOffsetMapper) mapper).mappers);
        } else {
          composite.mappers.add(mapper);
        }
      }

      return composite;
    }

    @Override
    public int getInputOffset(int outputOffset) {
      for (int i = mappers.size() - 1; i >= 0; i--) {
        outputOffset = mappers.get(i).getInputOffset(outputOffset);
      }
      return outputOffset;
    }

    @Override
    public int getOutputOffset(int inputOffset) {
      for (OffsetMapper mapper : mappers) {
        inputOffset = mapper.getOutputOffset(inputOffset);
      }
      return inputOffset;
    }

    @Override
    public OffsetMapper thenMapping(OffsetMapper mapper) {
      return CompositeOffsetMapper.of(this, mapper);
    }
  }

}

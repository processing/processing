package processing.data;

import java.io.*;
import java.util.HashMap;

import processing.core.PApplet;


/**
 * A simple table class to use a String as a lookup for another String value.
 */
public class StringHash {

  /** Number of elements in the table */
  public int count;

  /**
   * List of keys, available for sake of speed,
   * but should be manipulated (consider it read-only).
   */
  protected String[] keys;

  /**
   * List of values, available for sake of speed,
   * but should be manipulated (consider it read-only).
   */
  protected String[] values;

  /** Internal implementation for faster lookups */
  private HashMap<String, Integer> indices = new HashMap<String, Integer>();


  public StringHash() {
    count = 0;
    keys = new String[10];
    values = new String[10];
  }


  public StringHash(int length) {
    count = 0;
    keys = new String[length];
    values = new String[length];
  }


  public StringHash(PApplet parent, String filename) {
    String[] lines = parent.loadStrings(filename);
    keys = new String[lines.length];
    values = new String[lines.length];

//    boolean csv = (lines[0].indexOf('\t') == -1);

    for (int i = 0; i < lines.length; i++) {
//      String[] pieces = csv ? Table.splitLineCSV(lines[i]) : PApplet.split(lines[i], '\t');
      String[] pieces = PApplet.split(lines[i], '\t');
      if (pieces.length == 2) {
//        keys[count] = pieces[0];
//        values[count] = pieces[1];
//        count++;
        create(pieces[0], pieces[1]);
      }
    }
  }


  public void write(PApplet parent, String filename) {
    PrintWriter writer = parent.createWriter(filename);
    boolean csv =
      (filename.toLowerCase().endsWith(".csv") ||
       filename.toLowerCase().endsWith(".csv.gz"));
    for (int i = 0; i < count; i++) {
      if (csv) {
//        String k = key(i);
//        if (k.indexOf("))
      } else {
        writer.println(key(i) + "\t" + value(i));
      }
    }
  }


  public int getCount() {
    return count;
  }


  public String key(int index) {
    return keys[index];
  }


  public void crop() {
    if (count != keys.length) {
      keys = PApplet.subset(keys, 0, count);
      values = PApplet.subset(values, 0, count);
    }
  }


  /**
   * Return the internal array being used to store the keys. Allocated but
   * unused entries will be removed. This array should not be modified.
   */
  public String[] keys() {
    crop();
    return keys;
  }


  /**
   * Return a copy of the internal keys array. This array can be modified.
   */
  public String[] keyArray() {
    String[] outgoing = new String[count];
    System.arraycopy(keys, 0, outgoing, 0, count);
    return outgoing;
  }


  public String value(int index) {
    return values[index];
  }


  public String[] values() {
    crop();
    return values;
  }


  public String[] valueArray() {
    String[] outgoing = new String[count];
    System.arraycopy(values, 0, outgoing, 0, count);
    return outgoing;
  }


  public String get(String what) {
    int index = keyIndex(what);
    if (index == -1) return null;
    return values[index];
  }


  public void set(String key, String val) {
    int index = keyIndex(key);
    if (index == -1) {
      create(key, val);
    } else {
      values[index] = val;
    }
  }


  public void append(String key, String val) {
    int index = keyIndex(key);
    if (index == -1) {
      create(key, val);
    } else {
      values[index] += val;
    }
  }


  public int keyIndex(String what) {
    Integer found = indices.get(what);
    return (found == null) ? -1 : found.intValue();
  }


  public int valueIndex(String what) {
    for (int i = 0; i < count; i++) {
      if (values[i].equals(what)) {
        return i;
      }
    }
    return -1;
  }


  protected void create(String k, String v) {
    if (count == keys.length) {
      keys = PApplet.expand(keys);
      values = PApplet.expand(values);
    }
    indices.put(k, new Integer(count));
    keys[count] = k;
    values[count] = v;
    count++;
  }


  public void print() {
    write(new PrintWriter(System.out));
  }


  public void write(PrintWriter writer) {
    for (int i = 0; i < count; i++) {
      writer.println(keys[i] + "\t" + values[i]);
    }
    writer.flush();
  }


  public void remove(String which) {
    remove(keyIndex(which));
  }


  public void remove(int which) {
    indices.remove(keys[which]);
    for (int i = which; i < count-1; i++) {
      keys[i] = keys[i+1];
      values[i] = values[i+1];
      indices.put(keys[i], i);
    }
    count--;
    keys[count] = null;
    values[count] = null;
  }


  public void swap(int a, int b) {
    String tkey = keys[a];
    String tvalue = values[a];
    keys[a] = keys[b];
    values[a] = values[b];
    keys[b] = tkey;
    values[b] = tvalue;

    indices.put(keys[a], new Integer(a));
    indices.put(keys[b], new Integer(b));
  }


  public void sortKeys() {
    Sort s = new Sort() {
      @Override
      public int size() {
        return count;
      }

      @Override
      public float compare(int a, int b) {
        int result = keys[a].compareToIgnoreCase(keys[b]);
        if (result != 0) {
          return result;
        }
        return values[a].compareToIgnoreCase(values[b]);
      }

      @Override
      public void swap(int a, int b) {
        StringHash.this.swap(a, b);
      }
    };
    s.run();
  }


  /**
   * Sort by values in descending order (largest value will be at [0]).
   */
  public void sortValues() {
    sortValues(true, true);
  }


  public void sortValues(final boolean descending) {
    sortValues(descending, true);
  }


  // ascending puts the largest value at the end
  // descending puts the largest value at 0
  public void sortValues(final boolean descending, final boolean tiebreaker) {
    Sort s = new Sort() {
      @Override
      public int size() {
        return count;
      }

      @Override
      public float compare(int a, int b) {
        float diff = values[a].compareToIgnoreCase(values[b]);
        if (tiebreaker) {
          if (diff == 0) {
            diff = keys[a].compareToIgnoreCase(keys[b]);
          }
        }
        return descending ? diff : -diff;
      }

      @Override
      public void swap(int a, int b) {
        StringHash.this.swap(a, b);
      }
    };
    s.run();
  }
}

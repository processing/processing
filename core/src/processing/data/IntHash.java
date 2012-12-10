package processing.data;

import java.io.*;
import java.util.HashMap;

import processing.core.PApplet;


/**
 * A simple table class to use a String as a lookup for an int value.
 */
public class IntHash {

  /** Number of elements in the table */
  protected int count;

  /**
   * List of keys, available for sake of speed,
   * but should be manipulated (consider it read-only).
   */
  protected String[] keys;

  /**
   * List of values, available for sake of speed,
   * but should be manipulated (consider it read-only).
   */
  protected int[] values;

  /** Internal implementation for faster lookups */
  private HashMap<String, Integer> indices = new HashMap<String, Integer>();


  public IntHash() {
    count = 0;
    keys = new String[10];
    values = new int[10];
  }


  public IntHash(int length) {
    count = 0;
    keys = new String[length];
    values = new int[length];
  }


  public IntHash(String[] k, int[] v) {
    count = Math.min(k.length, v.length);
    keys = new String[count];
    values = new int[count];
    System.arraycopy(k, 0, keys, 0, count);
    System.arraycopy(v, 0, values, 0, count);
  }


  static public IntHash fromTally(String[] list) {
    IntHash outgoing = new IntHash();
    for (String s : list) {
      outgoing.increment(s);
    }
    outgoing.crop();
    return outgoing;
  }


  static public IntHash fromOrder(String[] list) {
    IntHash outgoing = new IntHash();
    for (int i = 0; i < list.length; i++) {
      outgoing.set(list[i], i);
    }
    return outgoing;
  }


  public IntHash(PApplet parent, String filename) {
    String[] lines = parent.loadStrings(filename);
    keys = new String[lines.length];
    values = new int[lines.length];

//    boolean csv = (lines[0].indexOf('\t') == -1);
    for (int i = 0; i < lines.length; i++) {
//    if (lines[i].trim().length() != 0) {
//      String[] pieces = csv ? Table.splitLineCSV(lines[i]) : PApplet.split(lines[i], '\t');
      String[] pieces = PApplet.split(lines[i], '\t');
      if (pieces.length == 2) {
        keys[count] = pieces[0];
        values[count] = PApplet.parseInt(pieces[1]);
        count++;
      }
    }
  }


  public int size() {
    return count;
  }


  public String key(int index) {
    return keys[index];
  }


  protected void crop() {
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


  public int value(int index) {
    return values[index];
  }


  public int[] values() {
    crop();
    return values;
  }


  public int[] valueArray() {
    int[] outgoing = new int[count];
    System.arraycopy(values, 0, outgoing, 0, count);
    return outgoing;
  }


  public int get(String what) {
    int index = index(what);
    if (index == -1) return 0;
    return values[index];
  }


  public void set(String who, int amount) {
    int index = index(who);
    if (index == -1) {
      create(who, amount);
    } else {
      values[index] = amount;
    }
  }


  public void add(String who, int amount) {
    int index = index(who);
    if (index == -1) {
      create(who, amount);
    } else {
      values[index] += amount;
    }
  }


  public void increment(String who) {
    int index = index(who);
    if (index == -1) {
      create(who, 1);
    } else {
      values[index]++;
    }
  }


  public int index(String what) {
    Integer found = indices.get(what);
    return (found == null) ? -1 : found.intValue();
  }


  protected void create(String what, int much) {
    if (count == keys.length) {
      String ktemp[] = new String[count << 1];
      System.arraycopy(keys, 0, ktemp, 0, count);
      keys = ktemp;
      int vtemp[] = new int[count << 1];
      System.arraycopy(values, 0, vtemp, 0, count);
      values = vtemp;
    }
    indices.put(what, new Integer(count));
    keys[count] = what;
    values[count] = much;
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
    removeIndex(index(which));
  }


  public void removeIndex(int which) {
    //System.out.println("index is " + which + " and " + keys[which]);
    indices.remove(keys[which]);
    for (int i = which; i < count-1; i++) {
      keys[i] = keys[i+1];
      values[i] = values[i+1];
      indices.put(keys[i], i);
    }
    count--;
    keys[count] = null;
    values[count] = 0;
  }


  protected void swap(int a, int b) {
    String tkey = keys[a];
    int tvalue = values[a];
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
        return values[b] - values[a];
      }

      @Override
      public void swap(int a, int b) {
        IntHash.this.swap(a, b);
      }
    };
    s.run();
  }


  /**
   * Sort by values in descending order (largest value will be at [0]).
   */
  public void sortValues() {
    sortValues(true);
  }


  /**
   * Sort by values. Identical values will use the keys as tie-breaker.
   * @param descending true to put the largest value at position 0.
   */
  public void sortValues(final boolean descending) {
    Sort s = new Sort() {
      @Override
      public int size() {
        return count;
      }

      @Override
      public float compare(int a, int b) {
        int diff = values[b] - values[a];
        if (diff == 0) {
          diff = keys[a].compareToIgnoreCase(keys[b]);
        }
        return descending ? diff : -diff;
      }

      @Override
      public void swap(int a, int b) {
        IntHash.this.swap(a, b);
      }
    };
    s.run();
  }
}
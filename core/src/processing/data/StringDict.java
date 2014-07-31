package processing.data;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;

import processing.core.PApplet;


/**
 * A simple table class to use a String as a lookup for another String value.
 *
 * @webref data:composite
 * @see IntDict
 * @see FloatDict
 */
public class StringDict {

  /** Number of elements in the table */
  protected int count;

  protected String[] keys;
  protected String[] values;

  /** Internal implementation for faster lookups */
  private HashMap<String, Integer> indices = new HashMap<String, Integer>();


  public StringDict() {
    count = 0;
    keys = new String[10];
    values = new String[10];
  }


  /**
   * Create a new lookup pre-allocated to a specific length. This will not
   * change the size(), but is more efficient than not specifying a length.
   * Use it when you know the rough size of the thing you're creating.
   *
   * @nowebref
   */
  public StringDict(int length) {
    count = 0;
    keys = new String[length];
    values = new String[length];
  }


  /**
   * Read a set of entries from a Reader that has each key/value pair on
   * a single line, separated by a tab.
   *
   * @nowebref
   */
  public StringDict(BufferedReader reader) {
    String[] lines = PApplet.loadStrings(reader);
    keys = new String[lines.length];
    values = new String[lines.length];

    for (int i = 0; i < lines.length; i++) {
      String[] pieces = PApplet.split(lines[i], '\t');
      if (pieces.length == 2) {
        keys[count] = pieces[0];
        values[count] = pieces[1];
        indices.put(keys[count], count);
        count++;
      }
    }
  }

  /**
   * @nowebref
   */
  public StringDict(String[] keys, String[] values) {
    if (keys.length != values.length) {
      throw new IllegalArgumentException("key and value arrays must be the same length");
    }
    this.keys = keys;
    this.values = values;
    count = keys.length;
    for (int i = 0; i < count; i++) {
      indices.put(keys[i], i);
    }
  }

  /**
   * @webref stringdict:method
   * @brief Returns the number of key/value pairs
   */
  public int size() {
    return count;
  }


  /**
   * Remove all entries.
   *
   * @webref stringdict:method
   * @brief Remove all entries
   */
  public void clear() {
    count = 0;
    indices = new HashMap<String, Integer>();
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


//  /**
//   * Return the internal array being used to store the keys. Allocated but
//   * unused entries will be removed. This array should not be modified.
//   */
//  public String[] keys() {
//    crop();
//    return keys;
//  }

  /**
   * @webref stringdict:method
   * @brief Return the internal array being used to store the keys
   */
  public Iterable<String> keys() {
    return new Iterable<String>() {

      @Override
      public Iterator<String> iterator() {
        return new Iterator<String>() {
          int index = -1;

          public void remove() {
            removeIndex(index);
          }

          public String next() {
            return key(++index);
          }

          public boolean hasNext() {
            return index+1 < size();
          }
        };
      }
    };
  }


  /**
   * Return a copy of the internal keys array. This array can be modified.
   *
   * @webref stringdict:method
   * @brief Return a copy of the internal keys array
   */
  public String[] keyArray() {
    return keyArray(null);
  }


  public String[] keyArray(String[] outgoing) {
    if (outgoing == null || outgoing.length != count) {
      outgoing = new String[count];
    }
    System.arraycopy(keys, 0, outgoing, 0, count);
    return outgoing;
  }


  public String value(int index) {
    return values[index];
  }

  /**
   * @webref stringdict:method
   * @brief Return the internal array being used to store the values
   */
  public Iterable<String> values() {
    return new Iterable<String>() {

      @Override
      public Iterator<String> iterator() {
        return new Iterator<String>() {
          int index = -1;

          public void remove() {
            removeIndex(index);
          }

          public String next() {
            return value(++index);
          }

          public boolean hasNext() {
            return index+1 < size();
          }
        };
      }
    };
  }


  /**
   * Create a new array and copy each of the values into it.
   *
   * @webref stringdict:method
   * @brief Create a new array and copy each of the values into it
   */
  public String[] valueArray() {
    return valueArray(null);
  }


  /**
   * Fill an already-allocated array with the values (more efficient than
   * creating a new array each time). If 'array' is null, or not the same
   * size as the number of values, a new array will be allocated and returned.
   */
  public String[] valueArray(String[] array) {
    if (array == null || array.length != size()) {
      array = new String[count];
    }
    System.arraycopy(values, 0, array, 0, count);
    return array;
  }


  /**
   * Return a value for the specified key.
   *
   * @webref stringdict:method
   * @brief Return a value for the specified key
   */
  public String get(String key) {
    int index = index(key);
    if (index == -1) return null;
    return values[index];
  }

  /**
   * @webref stringdict:method
   * @brief Create a new key/value pair or change the value of one
   */
  public void set(String key, String amount) {
    int index = index(key);
    if (index == -1) {
      create(key, amount);
    } else {
      values[index] = amount;
    }
  }


  public int index(String what) {
    Integer found = indices.get(what);
    return (found == null) ? -1 : found.intValue();
  }

  /**
   * @webref stringdict:method
   * @brief Check if a key is a part of the data structure
   */
  public boolean hasKey(String key) {
    return index(key) != -1;
  }


  protected void create(String key, String value) {
    if (count == keys.length) {
      keys = PApplet.expand(keys);
      values = PApplet.expand(values);
    }
    indices.put(key, new Integer(count));
    keys[count] = key;
    values[count] = value;
    count++;
  }

  /**
   * @webref stringdict:method
   * @brief Remove a key/value pair
   */
  public int remove(String key) {
    int index = index(key);
    if (index != -1) {
      removeIndex(index);
    }
    return index;
  }


  public String removeIndex(int index) {
    if (index < 0 || index >= count) {
      throw new ArrayIndexOutOfBoundsException(index);
    }
    //System.out.println("index is " + which + " and " + keys[which]);
    String key = keys[index];
    indices.remove(key);
    for (int i = index; i < count-1; i++) {
      keys[i] = keys[i+1];
      values[i] = values[i+1];
      indices.put(keys[i], i);
    }
    count--;
    keys[count] = null;
    values[count] = null;
    return key;
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


  /**
   * Sort the keys alphabetically (ignoring case). Uses the value as a
   * tie-breaker (only really possible with a key that has a case change).
   *
   * @webref stringdict:method
   * @brief Sort the keys alphabetically
   */
  public void sortKeys() {
    sortImpl(true, false);
  }

  /**
   * @webref stringdict:method
   * @brief Sort the keys alphabetially in reverse
   */
  public void sortKeysReverse() {
    sortImpl(true, true);
  }


  /**
   * Sort by values in descending order (largest value will be at [0]).
   *
   * @webref stringdict:method
   * @brief Sort by values in ascending order
   */
  public void sortValues() {
    sortImpl(false, false);
  }


  /**
   * @webref stringdict:method
   * @brief Sort by values in descending order
   */
  public void sortValuesReverse() {
    sortImpl(false, true);
  }


  protected void sortImpl(final boolean useKeys, final boolean reverse) {
    Sort s = new Sort() {
      @Override
      public int size() {
        return count;
      }

      @Override
      public float compare(int a, int b) {
        int diff = 0;
        if (useKeys) {
          diff = keys[a].compareToIgnoreCase(keys[b]);
          if (diff == 0) {
            diff = values[a].compareToIgnoreCase(values[b]);
          }
        } else {  // sort values
          diff = values[a].compareToIgnoreCase(values[b]);
          if (diff == 0) {
            diff = keys[a].compareToIgnoreCase(keys[b]);
          }
        }
        return reverse ? -diff : diff;
      }

      @Override
      public void swap(int a, int b) {
        StringDict.this.swap(a, b);
      }
    };
    s.run();
  }


  /** Returns a duplicate copy of this object. */
  public StringDict copy() {
    StringDict outgoing = new StringDict(count);
    System.arraycopy(keys, 0, outgoing.keys, 0, count);
    System.arraycopy(values, 0, outgoing.values, 0, count);
    for (int i = 0; i < count; i++) {
      outgoing.indices.put(keys[i], i);
    }
    outgoing.count = count;
    return outgoing;
  }


  /**
   * Write tab-delimited entries out to
   * @param writer
   */
  public void write(PrintWriter writer) {
    for (int i = 0; i < count; i++) {
      writer.println(keys[i] + "\t" + values[i]);
    }
    writer.flush();
  }


  public void print() {
    for (int i = 0; i < size(); i++) {
      System.out.println(keys[i] + " = " + values[i]);
    }
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName() + " size=" + size() + " { ");
    for (int i = 0; i < size(); i++) {
      if (i != 0) {
        sb.append(", ");
      }
      sb.append("\"" + keys[i] + "\": \"" + values[i] + "\"");
    }
    sb.append(" }");
    return sb.toString();
  }
}

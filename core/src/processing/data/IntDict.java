package processing.data;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;

import processing.core.PApplet;


/**
 * A simple class to use a String as a lookup for an int value.
 *
 * @webref data:composite
 * @see FloatDict
 * @see StringDict
 */
public class IntDict {

  /** Number of elements in the table */
  protected int count;

  protected String[] keys;
  protected int[] values;

  /** Internal implementation for faster lookups */
  private HashMap<String, Integer> indices = new HashMap<String, Integer>();


  public IntDict() {
    count = 0;
    keys = new String[10];
    values = new int[10];
  }


  /**
   * Create a new lookup with a specific size. This is more efficient than not
   * specifying a size. Use it when you know the rough size of the thing you're creating.
   *
   * @nowebref
   */
  public IntDict(int length) {
    count = 0;
    keys = new String[length];
    values = new int[length];
  }


  /**
   * Read a set of entries from a Reader that has each key/value pair on
   * a single line, separated by a tab.
   *
   * @nowebref
   */
  public IntDict(BufferedReader reader) {
    String[] lines = PApplet.loadStrings(reader);
    keys = new String[lines.length];
    values = new int[lines.length];

    for (int i = 0; i < lines.length; i++) {
      String[] pieces = PApplet.split(lines[i], '\t');
      if (pieces.length == 2) {
        keys[count] = pieces[0];
        values[count] = PApplet.parseInt(pieces[1]);
        indices.put(pieces[0], count);
        count++;
      }
    }
  }

  /**
   * @nowebref
   */
  public IntDict(String[] keys, int[] values) {
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
   * Constructor to allow (more intuitive) inline initialization, e.g.:
   * <pre>
   * new FloatDict(new Object[][] {
   *   { "key1", 1 },
   *   { "key2", 2 }
   * });
   * </pre>
   */
  public IntDict(Object[][] pairs) {
    count = pairs.length;
    this.keys = new String[count];
    this.values = new int[count];
    for (int i = 0; i < count; i++) {
      keys[i] = (String) pairs[i][0];
      values[i] = (Integer) pairs[i][1];
      indices.put(keys[i], i);
    }
  }


  /**
   * Returns the number of key/value pairs
   *
   * @webref intdict:method
   * @brief Returns the number of key/value pairs
   */
  public int size() {
    return count;
  }


  /**
   * Remove all entries.
   *
   * @webref intdict:method
   * @brief Remove all entries
   */
  public void clear() {
    count = 0;
    indices = new HashMap<String, Integer>();
  }


  public String key(int index) {
    return keys[index];
  }


//  private void crop() {
//    if (count != keys.length) {
//      keys = PApplet.subset(keys, 0, count);
//      values = PApplet.subset(values, 0, count);
//    }
//  }


  /**
   * Return the internal array being used to store the keys. Allocated but
   * unused entries will be removed. This array should not be modified.
   *
   * @webref intdict:method
   * @brief Return the internal array being used to store the keys
   */
//  public String[] keys() {
//    crop();
//    return keys;
//  }


//  public Iterable<String> keys() {
//    return new Iterable<String>() {
//
//      @Override
//      public Iterator<String> iterator() {
//        return new Iterator<String>() {
//          int index = -1;
//
//          public void remove() {
//            removeIndex(index);
//          }
//
//          public String next() {
//            return key(++index);
//          }
//
//          public boolean hasNext() {
//            return index+1 < size();
//          }
//        };
//      }
//    };
//  }


  // Use this with 'for' loops
  public Iterable<String> keys() {
    return new Iterable<String>() {

      @Override
      public Iterator<String> iterator() {
        return keyIterator();
//        return new Iterator<String>() {
//          int index = -1;
//
//          public void remove() {
//            removeIndex(index);
//          }
//
//          public String next() {
//            return key(++index);
//          }
//
//          public boolean hasNext() {
//            return index+1 < size();
//          }
//        };
      }
    };
  }


  // Use this to iterate when you want to be able to remove elements along the way
  public Iterator<String> keyIterator() {
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


  /**
   * Return a copy of the internal keys array. This array can be modified.
   *
   * @webref intdict:method
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


  public int value(int index) {
    return values[index];
  }


  /**
   * @webref intdict:method
   * @brief Return the internal array being used to store the keys
   */
  public Iterable<Integer> values() {
    return new Iterable<Integer>() {

      @Override
      public Iterator<Integer> iterator() {
        return valueIterator();
      }
    };
  }


  public Iterator<Integer> valueIterator() {
    return new Iterator<Integer>() {
      int index = -1;

      public void remove() {
        removeIndex(index);
      }

      public Integer next() {
        return value(++index);
      }

      public boolean hasNext() {
        return index+1 < size();
      }
    };
  }


  /**
   * Create a new array and copy each of the values into it.
   *
   * @webref intdict:method
   * @brief Create a new array and copy each of the values into it
   */
  public int[] valueArray() {
    return valueArray(null);
  }


  /**
   * Fill an already-allocated array with the values (more efficient than
   * creating a new array each time). If 'array' is null, or not the same
   * size as the number of values, a new array will be allocated and returned.
   *
   * @param array values to copy into the array
   */
  public int[] valueArray(int[] array) {
    if (array == null || array.length != size()) {
      array = new int[count];
    }
    System.arraycopy(values, 0, array, 0, count);
    return array;
  }


  /**
   * Return a value for the specified key.
   *
   * @webref intdict:method
   * @brief Return a value for the specified key
   */
  public int get(String key) {
    int index = index(key);
    if (index == -1) {
      throw new IllegalArgumentException("No key named '" + key + "'");
    }
    return values[index];
  }


  public int get(String key, int alternate) {
    int index = index(key);
    if (index == -1) return alternate;
    return values[index];
  }


  /**
   * Create a new key/value pair or change the value of one.
   *
   * @webref intdict:method
   * @brief Create a new key/value pair or change the value of one
   */
  public void set(String key, int amount) {
    int index = index(key);
    if (index == -1) {
      create(key, amount);
    } else {
      values[index] = amount;
    }
  }

  /**
   * @webref intdict:method
   * @brief Check if a key is a part of the data structure
   */
  public boolean hasKey(String key) {
    return index(key) != -1;
  }


  /**
   * Increase the value associated with a specific key by 1.
   *
   * @webref intdict:method
   * @brief Increase the value of a specific key value by 1
   */
  public void increment(String key) {
    add(key, 1);
  }


  /**
   * @webref intdict:method
   * @brief Add to a value
   */
  public void add(String key, int amount) {
    int index = index(key);
    if (index == -1) {
      create(key, amount);
    } else {
      values[index] += amount;
    }
  }


  /**
   * @webref intdict:method
   * @brief Subtract from a value
   */
  public void sub(String key, int amount) {
    add(key, -amount);
  }


  /**
   * @webref intdict:method
   * @brief Multiply a value
   */
  public void mult(String key, int amount) {
    int index = index(key);
    if (index != -1) {
      values[index] *= amount;
    }
  }


  /**
   * @webref intdict:method
   * @brief Divide a value
   */
  public void div(String key, int amount) {
    int index = index(key);
    if (index != -1) {
      values[index] /= amount;
    }
  }


  private void checkMinMax(String functionName) {
    if (count == 0) {
      String msg =
        String.format("Cannot use %s() on an empty %s.",
                      functionName, getClass().getSimpleName());
      throw new RuntimeException(msg);
    }
  }


  // return the index of the minimum value
  public int minIndex() {
    checkMinMax("minIndex");
    int index = 0;
    int value = values[0];
    for (int i = 1; i < count; i++) {
      if (values[i] < value) {
        index = i;
        value = values[i];
      }
    }
    return index;
  }


  // return the minimum value
  public int minValue() {
    checkMinMax("minValue");
    return values[minIndex()];
  }


  // return the key for the minimum value
  public String minKey() {
    checkMinMax("minKey");
    return keys[minIndex()];
  }


  // return the index of the max value
  public int maxIndex() {
    checkMinMax("maxIndex");
    int index = 0;
    int value = values[0];
    for (int i = 1; i < count; i++) {
      if (values[i] > value) {
        index = i;
        value = values[i];
      }
    }
    return index;
  }


  // return the maximum value
  public int maxValue() {
    checkMinMax("maxValue");
    return values[maxIndex()];
  }


  // return the key corresponding to the maximum value
  public String maxKey() {
    checkMinMax("maxKey");
    return keys[maxIndex()];
  }


  public int index(String what) {
    Integer found = indices.get(what);
    return (found == null) ? -1 : found.intValue();
  }


  protected void create(String what, int much) {
    if (count == keys.length) {
      keys = PApplet.expand(keys);
      values = PApplet.expand(values);
    }
    indices.put(what, Integer.valueOf(count));
    keys[count] = what;
    values[count] = much;
    count++;
  }

  /**
   * @webref intdict:method
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
    indices.remove(keys[index]);
    for (int i = index; i < count-1; i++) {
      keys[i] = keys[i+1];
      values[i] = values[i+1];
      indices.put(keys[i], i);
    }
    count--;
    keys[count] = null;
    values[count] = 0;
    return key;
  }


  public void swap(int a, int b) {
    String tkey = keys[a];
    int tvalue = values[a];
    keys[a] = keys[b];
    values[a] = values[b];
    keys[b] = tkey;
    values[b] = tvalue;

    indices.put(keys[a], Integer.valueOf(a));
    indices.put(keys[b], Integer.valueOf(b));
  }


  /**
   * Sort the keys alphabetically (ignoring case). Uses the value as a
   * tie-breaker (only really possible with a key that has a case change).
   *
   * @webref intdict:method
   * @brief Sort the keys alphabetically
   */
  public void sortKeys() {
    sortImpl(true, false);
  }

  /**
   * Sort the keys alphabetically in reverse (ignoring case). Uses the value as a
   * tie-breaker (only really possible with a key that has a case change).
   *
   * @webref intdict:method
   * @brief Sort the keys alphabetially in reverse
   */
  public void sortKeysReverse() {
    sortImpl(true, true);
  }


  /**
   * Sort by values in ascending order. The smallest value will be at [0].
   *
   * @webref intdict:method
   * @brief Sort by values in ascending order
   */
  public void sortValues() {
    sortImpl(false, false);
  }

  /**
   * Sort by values in descending order. The largest value will be at [0].
   *
   * @webref intdict:method
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
            return values[a] - values[b];
          }
        } else {  // sort values
          diff = values[a] - values[b];
          if (diff == 0) {
            diff = keys[a].compareToIgnoreCase(keys[b]);
          }
        }
        return reverse ? -diff : diff;
      }

      @Override
      public void swap(int a, int b) {
        IntDict.this.swap(a, b);
      }
    };
    s.run();
  }


  /**
   * Sum all of the values in this dictionary, then return a new FloatDict of
   * each key, divided by the total sum. The total for all values will be ~1.0.
   * @return a Dict with the original keys, mapped to their pct of the total
   */
  public FloatDict getPercent() {
    double sum = 0;
    for (int value : valueArray()) {
      sum += value;
    }
    FloatDict outgoing = new FloatDict();
    for (int i = 0; i < size(); i++) {
      double percent = value(i) / sum;
      outgoing.set(key(i), (float) percent);
    }
    return outgoing;
  }


  /** Returns a duplicate copy of this object. */
  public IntDict copy() {
    IntDict outgoing = new IntDict(count);
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
      sb.append("\"" + keys[i] + "\": " + values[i]);
    }
    sb.append(" }");
    return sb.toString();
  }
}

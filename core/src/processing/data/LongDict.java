package processing.data;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;

import processing.core.PApplet;


/**
 * A simple class to use a String as a lookup for an int value.
 *
 * @webref data:composite
 * @see FloatDict
 * @see StringDict
 */
public class LongDict {

  /** Number of elements in the table */
  protected int count;

  protected String[] keys;
  protected long[] values;

  /** Internal implementation for faster lookups */
  private HashMap<String, Integer> indices = new HashMap<>();


  public LongDict() {
    count = 0;
    keys = new String[10];
    values = new long[10];
  }


  /**
   * Create a new lookup with a specific size. This is more efficient than not
   * specifying a size. Use it when you know the rough size of the thing you're creating.
   *
   * @nowebref
   */
  public LongDict(int length) {
    count = 0;
    keys = new String[length];
    values = new long[length];
  }


  /**
   * Read a set of entries from a Reader that has each key/value pair on
   * a single line, separated by a tab.
   *
   * @nowebref
   */
  public LongDict(BufferedReader reader) {
    String[] lines = PApplet.loadStrings(reader);
    keys = new String[lines.length];
    values = new long[lines.length];

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
  public LongDict(String[] keys, long[] values) {
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
  public LongDict(Object[][] pairs) {
    count = pairs.length;
    this.keys = new String[count];
    this.values = new long[count];
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
   * Resize the internal data, this can only be used to shrink the list.
   * Helpful for situations like sorting and then grabbing the top 50 entries.
   */
  public void resize(int length) {
    if (length > count) {
      throw new IllegalArgumentException("resize() can only be used to shrink the dictionary");
    }
    if (length < 1) {
      throw new IllegalArgumentException("resize(" + length + ") is too small, use 1 or higher");
    }

    String[] newKeys = new String[length];
    long[] newValues = new long[length];
    PApplet.arrayCopy(keys, newKeys, length);
    PApplet.arrayCopy(values, newValues, length);
    keys = newKeys;
    values = newValues;
    count = length;
    resetIndices();
  }


  /**
   * Remove all entries.
   *
   * @webref intdict:method
   * @brief Remove all entries
   */
  public void clear() {
    count = 0;
    indices = new HashMap<>();
  }


  private void resetIndices() {
    indices = new HashMap<>(count);
    for (int i = 0; i < count; i++) {
      indices.put(keys[i], i);
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public class Entry {
    public String key;
    public long value;

    Entry(String key, long value) {
      this.key = key;
      this.value = value;
    }
  }


  public Iterable<Entry> entries() {
    return new Iterable<Entry>() {

      public Iterator<Entry> iterator() {
        return entryIterator();
      }
    };
  }


  public Iterator<Entry> entryIterator() {
    return new Iterator<Entry>() {
      int index = -1;

      public void remove() {
        removeIndex(index);
        index--;
      }

      public Entry next() {
        ++index;
        Entry e = new Entry(keys[index], values[index]);
        return e;
      }

      public boolean hasNext() {
        return index+1 < size();
      }
    };
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public String key(int index) {
    return keys[index];
  }


  protected void crop() {
    if (count != keys.length) {
      keys = PApplet.subset(keys, 0, count);
      values = PApplet.subset(values, 0, count);
    }
  }


  public Iterable<String> keys() {
    return new Iterable<String>() {

      @Override
      public Iterator<String> iterator() {
        return keyIterator();
      }
    };
  }


  // Use this to iterate when you want to be able to remove elements along the way
  public Iterator<String> keyIterator() {
    return new Iterator<String>() {
      int index = -1;

      public void remove() {
        removeIndex(index);
        index--;
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
    crop();
    return keyArray(null);
  }


  public String[] keyArray(String[] outgoing) {
    if (outgoing == null || outgoing.length != count) {
      outgoing = new String[count];
    }
    System.arraycopy(keys, 0, outgoing, 0, count);
    return outgoing;
  }


  public long value(int index) {
    return values[index];
  }


  /**
   * @webref intdict:method
   * @brief Return the internal array being used to store the values
   */
  public Iterable<Long> values() {
    return new Iterable<Long>() {

      @Override
      public Iterator<Long> iterator() {
        return valueIterator();
      }
    };
  }


  public Iterator<Long> valueIterator() {
    return new Iterator<Long>() {
      int index = -1;

      public void remove() {
        removeIndex(index);
        index--;
      }

      public Long next() {
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
    crop();
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
  public long get(String key) {
    int index = index(key);
    if (index == -1) {
      throw new IllegalArgumentException("No key named '" + key + "'");
    }
    return values[index];
  }


  public long get(String key, long alternate) {
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
  public void set(String key, long amount) {
    int index = index(key);
    if (index == -1) {
      create(key, amount);
    } else {
      values[index] = amount;
    }
  }


  public void setIndex(int index, String key, long value) {
    if (index < 0 || index >= count) {
      throw new ArrayIndexOutOfBoundsException(index);
    }
    keys[index] = key;
    values[index] = value;
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
   * Merge another dictionary into this one. Calling this increment()
   * since it doesn't make sense in practice for the other dictionary types,
   * even though it's technically an add().
   */
  public void increment(LongDict dict) {
    for (int i = 0; i < dict.count; i++) {
      add(dict.key(i), dict.value(i));
    }
  }


  /**
   * @webref intdict:method
   * @brief Add to a value
   */
  public void add(String key, long amount) {
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
  public void sub(String key, long amount) {
    add(key, -amount);
  }


  /**
   * @webref intdict:method
   * @brief Multiply a value
   */
  public void mult(String key, long amount) {
    int index = index(key);
    if (index != -1) {
      values[index] *= amount;
    }
  }


  /**
   * @webref intdict:method
   * @brief Divide a value
   */
  public void div(String key, long amount) {
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
    //checkMinMax("minIndex");
    if (count == 0) return -1;

    int index = 0;
    long value = values[0];
    for (int i = 1; i < count; i++) {
      if (values[i] < value) {
        index = i;
        value = values[i];
      }
    }
    return index;
  }


  // return the key for the minimum value
  public String minKey() {
    checkMinMax("minKey");
    int index = minIndex();
    if (index == -1) {
      return null;
    }
    return keys[index];
  }


  // return the minimum value, or throw an error if there are no values
  public long minValue() {
    checkMinMax("minValue");
    return values[minIndex()];
  }


  // return the index of the max value
  public int maxIndex() {
    //checkMinMax("maxIndex");
    if (count == 0) {
      return -1;
    }
    int index = 0;
    long value = values[0];
    for (int i = 1; i < count; i++) {
      if (values[i] > value) {
        index = i;
        value = values[i];
      }
    }
    return index;
  }


  /** return the key corresponding to the maximum value or null if no entries */
  public String maxKey() {
    //checkMinMax("maxKey");
    int index = maxIndex();
    if (index == -1) {
      return null;
    }
    return keys[index];
  }


  // return the maximum value or throw an error if zero length
  public long maxValue() {
    checkMinMax("maxIndex");
    return values[maxIndex()];
  }


  public long sum() {
    long sum = 0;
    for (int i = 0; i < count; i++) {
      sum += values[i];
    }
    return sum;
  }


  public int index(String what) {
    Integer found = indices.get(what);
    return (found == null) ? -1 : found.intValue();
  }


  protected void create(String what, long much) {
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
  public long remove(String key) {
    int index = index(key);
    if (index == -1) {
      throw new NoSuchElementException("'" + key + "' not found");
    }
    long value = values[index];
    removeIndex(index);
    return value;
  }


  public long removeIndex(int index) {
    if (index < 0 || index >= count) {
      throw new ArrayIndexOutOfBoundsException(index);
    }
    long value = values[index];
    indices.remove(keys[index]);
    for (int i = index; i < count-1; i++) {
      keys[i] = keys[i+1];
      values[i] = values[i+1];
      indices.put(keys[i], i);
    }
    count--;
    keys[count] = null;
    values[count] = 0;
    return value;
  }


  public void swap(int a, int b) {
    String tkey = keys[a];
    long tvalue = values[a];
    keys[a] = keys[b];
    values[a] = values[b];
    keys[b] = tkey;
    values[b] = tvalue;

//    indices.put(keys[a], Integer.valueOf(a));
//    indices.put(keys[b], Integer.valueOf(b));
  }


  /**
   * Sort the keys alphabetically (ignoring case). Uses the value as a
   * tie-breaker (only really possible with a key that has a case change).
   *
   * @webref intdict:method
   * @brief Sort the keys alphabetically
   */
  public void sortKeys() {
    sortImpl(true, false, true);
  }

  /**
   * Sort the keys alphabetically in reverse (ignoring case). Uses the value as a
   * tie-breaker (only really possible with a key that has a case change).
   *
   * @webref intdict:method
   * @brief Sort the keys alphabetically in reverse
   */
  public void sortKeysReverse() {
    sortImpl(true, true, true);
  }


  /**
   * Sort by values in ascending order. The smallest value will be at [0].
   *
   * @webref intdict:method
   * @brief Sort by values in ascending order
   */
  public void sortValues() {
    sortValues(true);
  }


  /**
   * Set true to ensure that the order returned is identical. Slightly
   * slower because the tie-breaker for identical values compares the keys.
   * @param stable
   */
  public void sortValues(boolean stable) {
    sortImpl(false, false, stable);
  }


  /**
   * Sort by values in descending order. The largest value will be at [0].
   *
   * @webref intdict:method
   * @brief Sort by values in descending order
   */
  public void sortValuesReverse() {
    sortValuesReverse(true);
  }


  public void sortValuesReverse(boolean stable) {
    sortImpl(false, true, stable);
  }


  protected void sortImpl(final boolean useKeys, final boolean reverse,
                          final boolean stable) {
    Sort s = new Sort() {
      @Override
      public int size() {
        return count;
      }

      @Override
      public int compare(int a, int b) {
        long diff = 0;
        if (useKeys) {
          diff = keys[a].compareToIgnoreCase(keys[b]);
          if (diff == 0) {
            diff = values[a] - values[b];
          }
        } else {  // sort values
          diff = values[a] - values[b];
          if (diff == 0 && stable) {
            diff = keys[a].compareToIgnoreCase(keys[b]);
          }
        }
        if (diff == 0) {
          return 0;
        } else if (reverse) {
          return diff < 0 ? 1 : -1;
        } else {
          return diff < 0 ? -1 : 1;
        }
      }

      @Override
      public void swap(int a, int b) {
        LongDict.this.swap(a, b);
      }
    };
    s.run();

    // Set the indices after sort/swaps (performance fix 160411)
    resetIndices();
  }


  /**
   * Sum all of the values in this dictionary, then return a new FloatDict of
   * each key, divided by the total sum. The total for all values will be ~1.0.
   * @return an IntDict with the original keys, mapped to their pct of the total
   */
  public FloatDict getPercent() {
    double sum = sum();  // a little more accuracy
    FloatDict outgoing = new FloatDict();
    for (int i = 0; i < size(); i++) {
      double percent = value(i) / sum;
      outgoing.set(key(i), (float) percent);
    }
    return outgoing;
  }


  /** Returns a duplicate copy of this object. */
  public LongDict copy() {
    LongDict outgoing = new LongDict(count);
    System.arraycopy(keys, 0, outgoing.keys, 0, count);
    System.arraycopy(values, 0, outgoing.values, 0, count);
    for (int i = 0; i < count; i++) {
      outgoing.indices.put(keys[i], i);
    }
    outgoing.count = count;
    return outgoing;
  }


  public void print() {
    for (int i = 0; i < size(); i++) {
      System.out.println(keys[i] + " = " + values[i]);
    }
  }


  /**
   * Save tab-delimited entries to a file (TSV format, UTF-8 encoding)
   */
  public void save(File file) {
    PrintWriter writer = PApplet.createWriter(file);
    write(writer);
    writer.close();
  }


  /**
   * Write tab-delimited entries to a PrintWriter
   */
  public void write(PrintWriter writer) {
    for (int i = 0; i < count; i++) {
      writer.println(keys[i] + "\t" + values[i]);
    }
    writer.flush();
  }


  /**
   * Return this dictionary as a String in JSON format.
   */
  public String toJSON() {
    StringList items = new StringList();
    for (int i = 0; i < count; i++) {
      items.append(JSONObject.quote(keys[i])+ ": " + values[i]);
    }
    return "{ " + items.join(", ") + " }";
  }


  @Override
  public String toString() {
    return getClass().getSimpleName() + " size=" + size() + " " + toJSON();
  }
}

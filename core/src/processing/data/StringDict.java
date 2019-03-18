package processing.data;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;

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
  private HashMap<String, Integer> indices = new HashMap<>();


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
   * Constructor to allow (more intuitive) inline initialization, e.g.:
   * <pre>
   * new StringDict(new String[][] {
   *   { "key1", "value1" },
   *   { "key2", "value2" }
   * });
   * </pre>
   * It's no Python, but beats a static { } block with HashMap.put() statements.
   */
  public StringDict(String[][] pairs) {
    count = pairs.length;
    this.keys = new String[count];
    this.values = new String[count];
    for (int i = 0; i < count; i++) {
      keys[i] = pairs[i][0];
      values[i] = pairs[i][1];
      indices.put(keys[i], i);
    }
  }


  /**
   * Create a dictionary that maps between column titles and cell entries
   * in a TableRow. If two columns have the same name, the later column's
   * values will override the earlier values.
   */
  public StringDict(TableRow row) {
    this(row.getColumnCount());

    String[] titles = row.getColumnTitles();
    if (titles == null) {
      titles = new StringList(IntList.fromRange(row.getColumnCount())).array();
    }
    for (int col = 0; col < row.getColumnCount(); col++) {
      set(titles[col], row.getString(col));
    }
    // remove unused and overwritten entries
    crop();
  }


  /**
   * @webref stringdict:method
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
    String[] newValues = new String[length];
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
   * @webref stringdict:method
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
    public String value;

    Entry(String key, String value) {
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
   * @webref stringdict:method
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
        return valueIterator();
      }
    };
  }


  public Iterator<String> valueIterator() {
    return new Iterator<String>() {
      int index = -1;

      public void remove() {
        removeIndex(index);
        index--;
      }

      public String next() {
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
   * @webref stringdict:method
   * @brief Create a new array and copy each of the values into it
   */
  public String[] valueArray() {
    crop();
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


  public String get(String key, String alternate) {
    int index = index(key);
    if (index == -1) return alternate;
    return values[index];
  }


  /**
   * @webref stringdict:method
   * @brief Create a new key/value pair or change the value of one
   */
  public void set(String key, String value) {
    int index = index(key);
    if (index == -1) {
      create(key, value);
    } else {
      values[index] = value;
    }
  }


  public void setIndex(int index, String key, String value) {
    if (index < 0 || index >= count) {
      throw new ArrayIndexOutOfBoundsException(index);
    }
    keys[index] = key;
    values[index] = value;
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
    indices.put(key, Integer.valueOf(count));
    keys[count] = key;
    values[count] = value;
    count++;
  }

  /**
   * @webref stringdict:method
   * @brief Remove a key/value pair
   */
  public String remove(String key) {
    int index = index(key);
    if (index == -1) {
      throw new NoSuchElementException("'" + key + "' not found");
    }
    String value = values[index];
    removeIndex(index);
    return value;
  }


  public String removeIndex(int index) {
    if (index < 0 || index >= count) {
      throw new ArrayIndexOutOfBoundsException(index);
    }
    String value = values[index];
    indices.remove(keys[index]);
    for (int i = index; i < count-1; i++) {
      keys[i] = keys[i+1];
      values[i] = values[i+1];
      indices.put(keys[i], i);
    }
    count--;
    keys[count] = null;
    values[count] = null;
    return value;
  }



  public void swap(int a, int b) {
    String tkey = keys[a];
    String tvalue = values[a];
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
   * @webref stringdict:method
   * @brief Sort the keys alphabetically
   */
  public void sortKeys() {
    sortImpl(true, false);
  }

  /**
   * @webref stringdict:method
   * @brief Sort the keys alphabetically in reverse
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
      public int compare(int a, int b) {
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

    // Set the indices after sort/swaps (performance fix 160411)
    resetIndices();
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
      items.append(JSONObject.quote(keys[i])+ ": " + JSONObject.quote(values[i]));
    }
    return "{ " + items.join(", ") + " }";
  }


  @Override
  public String toString() {
    return getClass().getSimpleName() + " size=" + size() + " " + toJSON();
  }
}

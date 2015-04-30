package processing.data;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;

import processing.core.PApplet;


/**
 * A simple table class to use a String as a lookup for an float value.
 *
 * @webref data:composite
 * @see IntDict
 * @see StringDict
 */
public class FloatDict {

  /** Number of elements in the table */
  protected int count;

  protected String[] keys;
  protected float[] values;

  /** Internal implementation for faster lookups */
  private HashMap<String, Integer> indices = new HashMap<String, Integer>();


  public FloatDict() {
    count = 0;
    keys = new String[10];
    values = new float[10];
  }


  /**
   * Create a new lookup with a specific size. This is more efficient than not
   * specifying a size. Use it when you know the rough size of the thing you're creating.
   *
   * @nowebref
   */
  public FloatDict(int length) {
    count = 0;
    keys = new String[length];
    values = new float[length];
  }


  /**
   * Read a set of entries from a Reader that has each key/value pair on
   * a single line, separated by a tab.
   *
   * @nowebref
   */
  public FloatDict(BufferedReader reader) {
    String[] lines = PApplet.loadStrings(reader);
    keys = new String[lines.length];
    values = new float[lines.length];

    for (int i = 0; i < lines.length; i++) {
      String[] pieces = PApplet.split(lines[i], '\t');
      if (pieces.length == 2) {
        keys[count] = pieces[0];
        values[count] = PApplet.parseFloat(pieces[1]);
        indices.put(pieces[0], count);
        count++;
      }
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
  public FloatDict(Object[][] pairs) {
    count = pairs.length;
    this.keys = new String[count];
    this.values = new float[count];
    for (int i = 0; i < count; i++) {
      keys[i] = (String) pairs[i][0];
      values[i] = (Float) pairs[i][1];
      indices.put(keys[i], i);
    }
  }


  /**
   * @nowebref
   */
  public FloatDict(String[] keys, float[] values) {
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
   * @webref floatdict:method
   * @brief Returns the number of key/value pairs
   */
  public int size() {
    return count;
  }


  /**
   * Remove all entries.
   *
   * @webref floatdict:method
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
   * @webref floatdict:method
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


  /*
  static class KeyIterator implements Iterator<String> {
    FloatHash parent;
    int index;

    public KeyIterator(FloatHash parent) {
      this.parent = parent;
      index = -1;
    }

    public void remove() {
      parent.removeIndex(index);
    }

    public String next() {
      return parent.key(++index);
    }

    public boolean hasNext() {
      return index+1 < parent.size();
    }

    public void reset() {
      index = -1;
    }
  }
  */


  /**
   * Return a copy of the internal keys array. This array can be modified.
   *
   * @webref floatdict:method
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


  public float value(int index) {
    return values[index];
  }


//  public float[] values() {
//    crop();
//    return values;
//  }

  /**
   * @webref floatdict:method
   * @brief Return the internal array being used to store the values
   */
  public Iterable<Float> values() {
    return new Iterable<Float>() {

      @Override
      public Iterator<Float> iterator() {
        return new Iterator<Float>() {
          int index = -1;

          public void remove() {
            removeIndex(index);
          }

          public Float next() {
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
   * @webref floatdict:method
   * @brief Create a new array and copy each of the values into it
   */
  public float[] valueArray() {
    return valueArray(null);
  }


  /**
   * Fill an already-allocated array with the values (more efficient than
   * creating a new array each time). If 'array' is null, or not the same
   * size as the number of values, a new array will be allocated and returned.
   */
  public float[] valueArray(float[] array) {
    if (array == null || array.length != size()) {
      array = new float[count];
    }
    System.arraycopy(values, 0, array, 0, count);
    return array;
  }


  /**
   * Return a value for the specified key.
   *
   * @webref floatdict:method
   * @brief Return a value for the specified key
   */
  public float get(String key) {
    int index = index(key);
    if (index == -1) {
      throw new IllegalArgumentException("No key named '" + key + "'");
    }
    return values[index];
  }


  public float get(String key, float alternate) {
    int index = index(key);
    if (index == -1) {
      return alternate;
    }
    return values[index];
  }


  /**
   * @webref floatdict:method
   * @brief Create a new key/value pair or change the value of one
   */
  public void set(String key, float amount) {
    int index = index(key);
    if (index == -1) {
      create(key, amount);
    } else {
      values[index] = amount;
    }
  }


  /**
   * @webref floatdict:method
   * @brief Check if a key is a part of the data structure
   */
  public boolean hasKey(String key) {
    return index(key) != -1;
  }


//  /** Increase the value of a specific key by 1. */
//  public void inc(String key) {
//    inc(key, 1);
////    int index = index(key);
////    if (index == -1) {
////      create(key, 1);
////    } else {
////      values[index]++;
////    }
//  }


  /**
   * @webref floatdict:method
   * @brief Add to a value
   */
  public void add(String key, float amount) {
    int index = index(key);
    if (index == -1) {
      create(key, amount);
    } else {
      values[index] += amount;
    }
  }


//  /** Decrease the value of a key by 1. */
//  public void dec(String key) {
//    inc(key, -1);
//  }


  /**
   * @webref floatdict:method
   * @brief Subtract from a value
   */
  public void sub(String key, float amount) {
    add(key, -amount);
  }


  /**
   * @webref floatdict:method
   * @brief Multiply a value
   */
  public void mult(String key, float amount) {
    int index = index(key);
    if (index != -1) {
      values[index] *= amount;
    }
  }


  /**
   * @webref floatdict:method
   * @brief Divide a value
   */
  public void div(String key, float amount) {
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


  /**
   * @webref floatlist:method
   * @brief Return the smallest value
   */
  public int minIndex() {
    checkMinMax("minIndex");
    // Will still return NaN if there is 1 or more entries, and they're all NaN
    float m = Float.NaN;
    int mi = -1;
    for (int i = 0; i < count; i++) {
      // find one good value to start
      if (values[i] == values[i]) {
        m = values[i];
        mi = i;

        // calculate the rest
        for (int j = i+1; j < count; j++) {
          float d = values[j];
          if (!Float.isNaN(d) && (d < m)) {
            m = values[j];
            mi = j;
          }
        }
        break;
      }
    }
    return mi;
  }


  public String minKey() {
    checkMinMax("minKey");
    int index = minIndex();
    if (index == -1) {
      return null;
    }
    return keys[index];
  }


  public float minValue() {
    checkMinMax("minValue");
    int index = minIndex();
    if (index == -1) {
      return Float.NaN;
    }
    return values[index];
  }


  /**
   * @webref floatlist:method
   * @brief Return the largest value
   */
  // The index of the entry that has the max value. Reference above is incorrect.
  public int maxIndex() {
    checkMinMax("maxIndex");
    // Will still return NaN if there is 1 or more entries, and they're all NaN
    float m = Float.NaN;
    int mi = -1;
    for (int i = 0; i < count; i++) {
      // find one good value to start
      if (values[i] == values[i]) {
        m = values[i];
        mi = i;

        // calculate the rest
        for (int j = i+1; j < count; j++) {
          float d = values[j];
          if (!Float.isNaN(d) && (d > m)) {
            m = values[j];
            mi = j;
          }
        }
        break;
      }
    }
    return mi;
  }


  /** The key for a max value, or null if everything is NaN (no max). */
  public String maxKey() {
    checkMinMax("maxKey");
    int index = maxIndex();
    if (index == -1) {
      return null;
    }
    return keys[index];
  }


  /** The max value. (Or NaN if they're all NaN.) */
  public float maxValue() {
    checkMinMax("maxValue");
    int index = maxIndex();
    if (index == -1) {
      return Float.NaN;
    }
    return values[index];
  }


  public int index(String what) {
    Integer found = indices.get(what);
    return (found == null) ? -1 : found.intValue();
  }


  protected void create(String what, float much) {
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
   * @webref floatdict:method
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
    String key = keys[index];
    //System.out.println("index is " + which + " and " + keys[which]);
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
    float tvalue = values[a];
    keys[a] = keys[b];
    values[a] = values[b];
    keys[b] = tkey;
    values[b] = tvalue;

    indices.put(keys[a], Integer.valueOf(a));
    indices.put(keys[b], Integer.valueOf(b));
  }


//  abstract class InternalSort extends Sort {
//    @Override
//    public int size() {
//      return count;
//    }
//
//    @Override
//    public void swap(int a, int b) {
//      FloatHash.this.swap(a, b);
//    }
//  }


  /**
   * Sort the keys alphabetically (ignoring case). Uses the value as a
   * tie-breaker (only really possible with a key that has a case change).
   *
   * @webref floatdict:method
   * @brief Sort the keys alphabetically
   */
  public void sortKeys() {
    sortImpl(true, false);
//    new InternalSort() {
//      @Override
//      public float compare(int a, int b) {
//        int result = keys[a].compareToIgnoreCase(keys[b]);
//        if (result != 0) {
//          return result;
//        }
//        return values[b] - values[a];
//      }
//    }.run();
  }


  /**
   * @webref floatdict:method
   * @brief Sort the keys alphabetially in reverse
   */
  public void sortKeysReverse() {
    sortImpl(true, true);
//    new InternalSort() {
//      @Override
//      public float compare(int a, int b) {
//        int result = keys[b].compareToIgnoreCase(keys[a]);
//        if (result != 0) {
//          return result;
//        }
//        return values[a] - values[b];
//      }
//    }.run();
  }


  /**
   * Sort by values in descending order (largest value will be at [0]).
   *
   * @webref floatdict:method
   * @brief Sort by values in ascending order
   */
  public void sortValues() {
    sortImpl(false, false);
//    new InternalSort() {
//      @Override
//      public float compare(int a, int b) {
//
//      }
//    }.run();
  }


  /**
   * @webref floatdict:method
   * @brief Sort by values in descending order
   */
  public void sortValuesReverse() {
    sortImpl(false, true);
//    new InternalSort() {
//      @Override
//      public float compare(int a, int b) {
//        float diff = values[b] - values[a];
//        if (diff == 0 && keys[a] != null && keys[b] != null) {
//          diff = keys[a].compareToIgnoreCase(keys[b]);
//        }
//        return descending ? diff : -diff;
//      }
//    }.run();
  }


//  // ascending puts the largest value at the end
//  // descending puts the largest value at 0
//  public void sortValues(final boolean descending, final boolean tiebreaker) {
//    Sort s = new Sort() {
//      @Override
//      public int size() {
//        return count;
//      }
//
//      @Override
//      public float compare(int a, int b) {
//        float diff = values[b] - values[a];
//        if (tiebreaker) {
//          if (diff == 0) {
//            diff = keys[a].compareToIgnoreCase(keys[b]);
//          }
//        }
//        return descending ? diff : -diff;
//      }
//
//      @Override
//      public void swap(int a, int b) {
//        FloatHash.this.swap(a, b);
//      }
//    };
//    s.run();
//  }


  protected void sortImpl(final boolean useKeys, final boolean reverse) {
    Sort s = new Sort() {
      @Override
      public int size() {
        if (useKeys) {
          return count;  // don't worry about NaN values

        } else if (count == 0) {  // skip the NaN check, it'll AIOOBE
          return 0;

        } else {  // first move NaN values to the end of the list
          int right = count - 1;
          while (values[right] != values[right]) {
            right--;
            if (right == -1) {
              return 0;  // all values are NaN
            }
          }
          for (int i = right; i >= 0; --i) {
            if (Float.isNaN(values[i])) {
              swap(i, right);
              --right;
            }
          }
          return right + 1;
        }
      }

      @Override
      public float compare(int a, int b) {
        float diff = 0;
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
        FloatDict.this.swap(a, b);
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
    for (float value : valueArray()) {
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
  public FloatDict copy() {
    FloatDict outgoing = new FloatDict(count);
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
   * Write tab-delimited entries out to
   * @param writer
   */
  public void write(PrintWriter writer) {
    for (int i = 0; i < count; i++) {
      writer.println(keys[i] + "\t" + values[i]);
    }
    writer.flush();
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

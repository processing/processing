package processing.data;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

import processing.core.PApplet;

/**
 * Helper class for a list of Strings. Lists are designed to have some of the
 * features of ArrayLists, but to maintain the simplicity and efficiency of
 * working with arrays.
 *
 * Functions like sort() and shuffle() always act on the list itself. To get
 * a sorted copy, use list.copy().sort().
 *
 * @webref data:composite
 * @see IntList
 * @see FloatList
 */
public class StringList implements Iterable<String> {
  int count;
  String[] data;


  public StringList() {
    this(10);
  }

  /**
   * @nowebref
   */
  public StringList(int length) {
    data = new String[length];
  }

  /**
   * @nowebref
   */
  public StringList(String[] list) {
    count = list.length;
    data = new String[count];
    System.arraycopy(list, 0, data, 0, count);
  }


  /**
   * Create from something iterable, for instance:
   * StringList list = new StringList(hashMap.keySet());
   *
   * @nowebref
   */
  public StringList(Iterable<String> iter) {
    this(10);
    for (String s : iter) {
      append(s);
    }
  }


  /**
   * Improve efficiency by removing allocated but unused entries from the
   * internal array used to store the data. Set to private, though it could
   * be useful to have this public if lists are frequently making drastic
   * size changes (from very large to very small).
   */
  private void crop() {
    if (count != data.length) {
      data = PApplet.subset(data, 0, count);
    }
  }


  /**
   * Get the length of the list.
   *
   * @webref stringlist:method
   * @brief Get the length of the list
   */
  public int size() {
    return count;
  }


  public void resize(int length) {
    if (length > data.length) {
      String[] temp = new String[length];
      System.arraycopy(data, 0, temp, 0, count);
      data = temp;

    } else if (length > count) {
      Arrays.fill(data, count, length, 0);
    }
    count = length;
  }


  /**
   * Remove all entries from the list.
   *
   * @webref stringlist:method
   * @brief Remove all entries from the list
   */
  public void clear() {
    count = 0;
  }


  /**
   * Get an entry at a particular index.
   *
   * @webref stringlist:method
   * @brief Get an entry at a particular index
   */
  public String get(int index) {
    if (index >= count) {
      throw new ArrayIndexOutOfBoundsException(index);
    }
    return data[index];
  }


  /**
   * Set the entry at a particular index. If the index is past the length of
   * the list, it'll expand the list to accommodate, and fill the intermediate
   * entries with 0s.
   *
   * @webref stringlist:method
   * @brief Set an entry at a particular index
   */
  public void set(int index, String what) {
    if (index >= count) {
      data = PApplet.expand(data, index+1);
      for (int i = count; i < index; i++) {
        data[i] = null;
      }
      count = index+1;
    }
    data[index] = what;
  }


  /** Just an alias for append(), but matches pop() */
  public void push(String value) {
    append(value);
  }


  public String pop() {
    if (count == 0) {
      throw new RuntimeException("Can't call pop() on an empty list");
    }
    String value = get(count-1);
    count--;
    return value;
  }


  /**
   * Remove an element from the specified index.
   *
   * @webref stringlist:method
   * @brief Remove an element from the specified index
   */
  public String remove(int index) {
    if (index < 0 || index >= count) {
      throw new ArrayIndexOutOfBoundsException(index);
    }
    String entry = data[index];
//    int[] outgoing = new int[count - 1];
//    System.arraycopy(data, 0, outgoing, 0, index);
//    count--;
//    System.arraycopy(data, index + 1, outgoing, 0, count - index);
//    data = outgoing;
    for (int i = index; i < count-1; i++) {
      data[i] = data[i+1];
    }
    count--;
    return entry;
  }


  // Remove the first instance of a particular value and return its index.
  public int removeValue(String value) {
    if (value == null) {
      for (int i = 0; i < count; i++) {
        if (data[i] == null) {
          remove(i);
          return i;
        }
      }
    } else {
      int index = index(value);
      if (index != -1) {
        remove(index);
        return index;
      }
    }
    return -1;
  }


  // Remove all instances of a particular value and return the count removed.
  public int removeValues(String value) {
    int ii = 0;
    if (value == null) {
      for (int i = 0; i < count; i++) {
        if (data[i] != null) {
          data[ii++] = data[i];
        }
      }
    } else {
      for (int i = 0; i < count; i++) {
        if (!value.equals(data[i])) {
          data[ii++] = data[i];
        }
      }
    }
    int removed = count - ii;
    count = ii;
    return removed;
  }


  // replace the first value that matches, return the index that was replaced
  public int replaceValue(String value, String newValue) {
    if (value == null) {
      for (int i = 0; i < count; i++) {
        if (data[i] == null) {
          data[i] = newValue;
          return i;
        }
      }
    } else {
      for (int i = 0; i < count; i++) {
        if (value.equals(data[i])) {
          data[i] = newValue;
          return i;
        }
      }
    }
    return -1;
  }


  // replace all values that match, return the count of those replaced
  public int replaceValues(String value, String newValue) {
    int changed = 0;
    if (value == null) {
      for (int i = 0; i < count; i++) {
        if (data[i] == null) {
          data[i] = newValue;
          changed++;
        }
      }
    } else {
      for (int i = 0; i < count; i++) {
        if (value.equals(data[i])) {
          data[i] = newValue;
          changed++;
        }
      }
    }
    return changed;
  }


  /**
   * Add a new entry to the list.
   *
   * @webref stringlist:method
   * @brief Add a new entry to the list
   */
  public void append(String value) {
    if (count == data.length) {
      data = PApplet.expand(data);
    }
    data[count++] = value;
  }


  public void append(String[] values) {
    for (String v : values) {
      append(v);
    }
  }


  public void append(StringList list) {
    for (String v : list.values()) {  // will concat the list...
      append(v);
    }
  }


//  public void insert(int index, int value) {
//    if (index+1 > count) {
//      if (index+1 < data.length) {
//    }
//  }
//    if (index >= data.length) {
//      data = PApplet.expand(data, index+1);
//      data[index] = value;
//      count = index+1;
//
//    } else if (count == data.length) {
//    if (index >= count) {
//      //int[] temp = new int[count << 1];
//      System.arraycopy(data, 0, temp, 0, index);
//      temp[index] = value;
//      System.arraycopy(data, index, temp, index+1, count - index);
//      data = temp;
//
//    } else {
//      // data[] has room to grow
//      // for() loop believed to be faster than System.arraycopy over itself
//      for (int i = count; i > index; --i) {
//        data[i] = data[i-1];
//      }
//      data[index] = value;
//      count++;
//    }
//  }


  public void insert(int index, String value) {
    insert(index, new String[] { value });
  }


  // same as splice
  public void insert(int index, String[] values) {
    if (index < 0) {
      throw new IllegalArgumentException("insert() index cannot be negative: it was " + index);
    }
    if (index >= data.length) {
      throw new IllegalArgumentException("insert() index " + index + " is past the end of this list");
    }

    String[] temp = new String[count + values.length];

    // Copy the old values, but not more than already exist
    System.arraycopy(data, 0, temp, 0, Math.min(count, index));

    // Copy the new values into the proper place
    System.arraycopy(values, 0, temp, index, values.length);

//    if (index < count) {
    // The index was inside count, so it's a true splice/insert
    System.arraycopy(data, index, temp, index+values.length, count - index);
    count = count + values.length;
//    } else {
//      // The index was past 'count', so the new count is weirder
//      count = index + values.length;
//    }
    data = temp;
  }


  public void insert(int index, StringList list) {
    insert(index, list.values());
  }


    // below are aborted attempts at more optimized versions of the code
    // that are harder to read and debug...

//    if (index + values.length >= count) {
//      // We're past the current 'count', check to see if we're still allocated
//      // index 9, data.length = 10, values.length = 1
//      if (index + values.length < data.length) {
//        // There's still room for these entries, even though it's past 'count'.
//        // First clear out the entries leading up to it, however.
//        for (int i = count; i < index; i++) {
//          data[i] = 0;
//        }
//        data[index] =
//      }
//      if (index >= data.length) {
//        int length = index + values.length;
//        int[] temp = new int[length];
//        System.arraycopy(data, 0, temp, 0, count);
//        System.arraycopy(values, 0, temp, index, values.length);
//        data = temp;
//        count = data.length;
//      } else {
//
//      }
//
//    } else if (count == data.length) {
//      int[] temp = new int[count << 1];
//      System.arraycopy(data, 0, temp, 0, index);
//      temp[index] = value;
//      System.arraycopy(data, index, temp, index+1, count - index);
//      data = temp;
//
//    } else {
//      // data[] has room to grow
//      // for() loop believed to be faster than System.arraycopy over itself
//      for (int i = count; i > index; --i) {
//        data[i] = data[i-1];
//      }
//      data[index] = value;
//      count++;
//    }


  /** Return the first index of a particular value. */
  public int index(String what) {
    if (what == null) {
      for (int i = 0; i < count; i++) {
        if (data[i] == null) {
          return i;
        }
      }
    } else {
      for (int i = 0; i < count; i++) {
        if (what.equals(data[i])) {
          return i;
        }
      }
    }
    return -1;
  }


  // !!! TODO this is not yet correct, because it's not being reset when
  // the rest of the entries are changed
//  protected void cacheIndices() {
//    indexCache = new HashMap<Integer, Integer>();
//    for (int i = 0; i < count; i++) {
//      indexCache.put(data[i], i);
//    }
//  }

  /**
   * @webref stringlist:method
   * @brief Check if a value is a part of the list
   */
  public boolean hasValue(String value) {
    if (value == null) {
      for (int i = 0; i < count; i++) {
        if (data[i] == null) {
          return true;
        }
      }
    } else {
      for (int i = 0; i < count; i++) {
        if (value.equals(data[i])) {
          return true;
        }
      }
    }
    return false;
  }


  /**
   * Sorts the array in place.
   *
   * @webref stringlist:method
   * @brief Sorts the array in place
   */
  public void sort() {
    sortImpl(false);
  }


  /**
   * Reverse sort, orders values from highest to lowest.
   *
   * @webref stringlist:method
   * @brief Reverse sort, orders values from highest to lowest
   */
  public void sortReverse() {
    sortImpl(true);
  }


  private void sortImpl(final boolean reverse) {
    new Sort() {
      @Override
      public int size() {
        return count;
      }

      @Override
      public float compare(int a, int b) {
        float diff = data[a].compareToIgnoreCase(data[b]);
        return reverse ? -diff : diff;
      }

      @Override
      public void swap(int a, int b) {
        String temp = data[a];
        data[a] = data[b];
        data[b] = temp;
      }
    }.run();
  }


  // use insert()
//  public void splice(int index, int value) {
//  }


//  public void subset(int start) {
//    subset(start, count - start);
//  }
//
//
//  public void subset(int start, int num) {
//    for (int i = 0; i < num; i++) {
//      data[i] = data[i+start];
//    }
//    count = num;
//  }

  /**
   * @webref stringlist:method
   * @brief Reverse the order of the list elements
   */
  public void reverse() {
    int ii = count - 1;
    for (int i = 0; i < count/2; i++) {
      String t = data[i];
      data[i] = data[ii];
      data[ii] = t;
      --ii;
    }
  }


  /**
   * Randomize the order of the list elements. Note that this does not
   * obey the randomSeed() function in PApplet.
   *
   * @webref stringlist:method
   * @brief Randomize the order of the list elements
   */
  public void shuffle() {
    Random r = new Random();
    int num = count;
    while (num > 1) {
      int value = r.nextInt(num);
      num--;
      String temp = data[num];
      data[num] = data[value];
      data[value] = temp;
    }
  }


  /**
   * Randomize the list order using the random() function from the specified
   * sketch, allowing shuffle() to use its current randomSeed() setting.
   */
  public void shuffle(PApplet sketch) {
    int num = count;
    while (num > 1) {
      int value = (int) sketch.random(num);
      num--;
      String temp = data[num];
      data[num] = data[value];
      data[value] = temp;
    }
  }


  /**
   * Make the entire list lower case.
   *
   * @webref stringlist:method
   * @brief Make the entire list lower case
   */
  public void lower() {
    for (int i = 0; i < count; i++) {
      if (data[i] != null) {
        data[i] = data[i].toLowerCase();
      }
    }
  }


  /**
   * Make the entire list upper case.
   *
   * @webref stringlist:method
   * @brief Make the entire list upper case
   */
  public void upper() {
    for (int i = 0; i < count; i++) {
      if (data[i] != null) {
        data[i] = data[i].toUpperCase();
      }
    }
  }


  public StringList copy() {
    StringList outgoing = new StringList(data);
    outgoing.count = count;
    return outgoing;
  }


  /**
   * Returns the actual array being used to store the data. Suitable for
   * iterating with a for() loop, but modifying the list could cause terrible
   * things to happen.
   */
  public String[] values() {
    crop();
    return data;
  }


  @Override
  public Iterator<String> iterator() {
//    return valueIterator();
//  }
//
//
//  public Iterator<String> valueIterator() {
    return new Iterator<String>() {
      int index = -1;

      public void remove() {
        StringList.this.remove(index);
      }

      public String next() {
        return data[++index];
      }

      public boolean hasNext() {
        return index+1 < count;
      }
    };
  }


  /**
   * Create a new array with a copy of all the values.
   *
   * @return an array sized by the length of the list with each of the values.
   * @webref stringlist:method
   * @brief Create a new array with a copy of all the values
   */
  public String[] array() {
    return array(null);
  }


  /**
   * Copy values into the specified array. If the specified array is null or
   * not the same size, a new array will be allocated.
   * @param array
   */
  public String[] array(String[] array) {
    if (array == null || array.length != count) {
      array = new String[count];
    }
    System.arraycopy(data, 0, array, 0, count);
    return array;
  }


  public StringList getSubset(int start) {
    return getSubset(start, count - start);
  }


  public StringList getSubset(int start, int num) {
    String[] subset = new String[num];
    System.arraycopy(data, start, subset, 0, num);
    return new StringList(subset);
  }


  /** Get a list of all unique entries. */
  public String[] getUnique() {
    return getTally().keyArray();
  }


  /** Count the number of times each String entry is found in this list. */
  public IntDict getTally() {
    IntDict outgoing = new IntDict();
    for (int i = 0; i < count; i++) {
      outgoing.increment(data[i]);
    }
    return outgoing;
  }


  /** Create a dictionary associating each entry in this list to its index. */
  public IntDict getOrder() {
    IntDict outgoing = new IntDict();
    for (int i = 0; i < count; i++) {
      outgoing.set(data[i], i);
    }
    return outgoing;
  }


  public String join(String separator) {
    if (count == 0) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    sb.append(data[0]);
    for (int i = 1; i < count; i++) {
      sb.append(separator);
      sb.append(data[i]);
    }
    return sb.toString();
  }


  public void print() {
    for (int i = 0; i < size(); i++) {
      System.out.format("[%d] %s%n", i, data[i]);
    }
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName() + " size=" + size() + " [ ");
    for (int i = 0; i < size(); i++) {
      if (i != 0) {
        sb.append(", ");
      }
      sb.append(i + ": \"" + data[i] + "\"");
    }
    sb.append(" ]");
    return sb.toString();
  }
}
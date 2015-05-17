package processing.data;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

import processing.core.PApplet;


// splice, slice, subset, concat, reverse

// trim, join for String versions


/**
 * Helper class for a list of ints. Lists are designed to have some of the
 * features of ArrayLists, but to maintain the simplicity and efficiency of
 * working with arrays.
 *
 * Functions like sort() and shuffle() always act on the list itself. To get
 * a sorted copy, use list.copy().sort().
 *
 * @webref data:composite
 * @see FloatList
 * @see StringList
 */
public class IntList implements Iterable<Integer> {
  protected int count;
  protected int[] data;


  public IntList() {
    data = new int[10];
  }

  /**
   * @nowebref
   */
  public IntList(int length) {
    data = new int[length];
  }

  /**
   * @nowebref
   */
  public IntList(int[] source) {
    count = source.length;
    data = new int[count];
    System.arraycopy(source, 0, data, 0, count);
  }

  /**
   * @nowebref
   */
  public IntList(Iterable<Integer> iter) {
    this(10);
    for (int v : iter) {
      append(v);
    }
  }


  static public IntList fromRange(int stop) {
    return fromRange(0, stop);
  }


  static public IntList fromRange(int start, int stop) {
    int count = stop - start;
    IntList newbie = new IntList(count);
    for (int i = 0; i < count; i++) {
      newbie.set(i, start+i);
    }
    return newbie;
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
   * @webref intlist:method
   * @brief Get the length of the list
   */
  public int size() {
    return count;
  }


  public void resize(int length) {
    if (length > data.length) {
      int[] temp = new int[length];
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
   * @webref intlist:method
   * @brief Remove all entries from the list
   */
  public void clear() {
    count = 0;
  }


  /**
   * Get an entry at a particular index.
   *
   * @webref intlist:method
   * @brief Get an entry at a particular index
   */
  public int get(int index) {
    if (index >= this.count) {
      throw new ArrayIndexOutOfBoundsException(index);
    }
    return data[index];
  }


  /**
   * Set the entry at a particular index. If the index is past the length of
   * the list, it'll expand the list to accommodate, and fill the intermediate
   * entries with 0s.
   *
   * @webref intlist:method
   * @brief Set the entry at a particular index
   */
  public void set(int index, int what) {
    if (index >= count) {
      data = PApplet.expand(data, index+1);
      for (int i = count; i < index; i++) {
        data[i] = 0;
      }
      count = index+1;
    }
    data[index] = what;
  }


  /** Just an alias for append(), but matches pop() */
  public void push(int value) {
    append(value);
  }


  public int pop() {
    if (count == 0) {
      throw new RuntimeException("Can't call pop() on an empty list");
    }
    int value = get(count-1);
    count--;
    return value;
  }


  /**
   * Remove an element from the specified index
   *
   * @webref intlist:method
   * @brief Remove an element from the specified index
   */
  public int remove(int index) {
    if (index < 0 || index >= count) {
      throw new ArrayIndexOutOfBoundsException(index);
    }
    int entry = data[index];
//    int[] outgoing = new int[count - 1];
//    System.arraycopy(data, 0, outgoing, 0, index);
//    count--;
//    System.arraycopy(data, index + 1, outgoing, 0, count - index);
//    data = outgoing;
    // For most cases, this actually appears to be faster
    // than arraycopy() on an array copying into itself.
    for (int i = index; i < count-1; i++) {
      data[i] = data[i+1];
    }
    count--;
    return entry;
  }


  // Remove the first instance of a particular value,
  // and return the index at which it was found.
  public int removeValue(int value) {
    int index = index(value);
    if (index != -1) {
      remove(index);
      return index;
    }
    return -1;
  }


  // Remove all instances of a particular value,
  // and return the number of values found and removed
  public int removeValues(int value) {
    int ii = 0;
    for (int i = 0; i < count; i++) {
      if (data[i] != value) {
        data[ii++] = data[i];
      }
    }
    int removed = count - ii;
    count = ii;
    return removed;
  }


  /**
   * Add a new entry to the list.
   *
   * @webref intlist:method
   * @brief Add a new entry to the list
   */
  public void append(int value) {
    if (count == data.length) {
      data = PApplet.expand(data);
    }
    data[count++] = value;
  }


  public void append(int[] values) {
    for (int v : values) {
      append(v);
    }
  }


  public void append(IntList list) {
    for (int v : list.values()) {  // will concat the list...
      append(v);
    }
  }


  /** Add this value, but only if it's not already in the list. */
  public void appendUnique(int value) {
    if (!hasValue(value)) {
      append(value);
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


  public void insert(int index, int value) {
    insert(index, new int[] { value });
  }


  // same as splice
  public void insert(int index, int[] values) {
    if (index < 0) {
      throw new IllegalArgumentException("insert() index cannot be negative: it was " + index);
    }
    if (index >= data.length) {
      throw new IllegalArgumentException("insert() index " + index + " is past the end of this list");
    }

    int[] temp = new int[count + values.length];

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


  public void insert(int index, IntList list) {
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
  public int index(int what) {
    /*
    if (indexCache != null) {
      try {
        return indexCache.get(what);
      } catch (Exception e) {  // not there
        return -1;
      }
    }
    */
    for (int i = 0; i < count; i++) {
      if (data[i] == what) {
        return i;
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
   * @webref intlist:method
   * @brief Check if a number is a part of the list
   */
  public boolean hasValue(int value) {
//    if (indexCache == null) {
//      cacheIndices();
//    }
//    return index(what) != -1;
    for (int i = 0; i < count; i++) {
      if (data[i] == value) {
        return true;
      }
    }
    return false;
  }

  /**
   * @webref intlist:method
   * @brief Add one to a value
   */
  public void increment(int index) {
    if (count <= index) {
      resize(index + 1);
    }
    data[index]++;
  }


  private void boundsProblem(int index, String method) {
    final String msg = String.format("The list size is %d. " +
      "You cannot %s() to element %d.", count, method, index);
    throw new ArrayIndexOutOfBoundsException(msg);
  }


  /**
   * @webref intlist:method
   * @brief Add to a value
   */
  public void add(int index, int amount) {
    if (index < count) {
      data[index] += amount;
    } else {
      boundsProblem(index, "add");
    }
  }

  /**
   * @webref intlist:method
   * @brief Subtract from a value
   */
  public void sub(int index, int amount) {
    if (index < count) {
      data[index] -= amount;
    } else {
      boundsProblem(index, "sub");
    }
  }

  /**
   * @webref intlist:method
   * @brief Multiply a value
   */
  public void mult(int index, int amount) {
    if (index < count) {
      data[index] *= amount;
    } else {
      boundsProblem(index, "mult");
    }
  }

  /**
   * @webref intlist:method
   * @brief Divide a value
   */
  public void div(int index, int amount) {
    if (index < count) {
      data[index] /= amount;
    } else {
      boundsProblem(index, "div");
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
   * @webref intlist:method
   * @brief Return the smallest value
   */
  public int min() {
    checkMinMax("min");
    int outgoing = data[0];
    for (int i = 1; i < count; i++) {
      if (data[i] < outgoing) outgoing = data[i];
    }
    return outgoing;
  }


  // returns the index of the minimum value.
  // if there are ties, it returns the first one found.
  public int minIndex() {
    checkMinMax("minIndex");
    int value = data[0];
    int index = 0;
    for (int i = 1; i < count; i++) {
      if (data[i] < value) {
        value = data[i];
        index = i;
      }
    }
    return index;
  }


  /**
   * @webref intlist:method
   * @brief Return the largest value
   */
  public int max() {
    checkMinMax("max");
    int outgoing = data[0];
    for (int i = 1; i < count; i++) {
      if (data[i] > outgoing) outgoing = data[i];
    }
    return outgoing;
  }


  // returns the index of the maximum value.
  // if there are ties, it returns the first one found.
  public int maxIndex() {
    checkMinMax("maxIndex");
    int value = data[0];
    int index = 0;
    for (int i = 1; i < count; i++) {
      if (data[i] > value) {
        value = data[i];
        index = i;
      }
    }
    return index;
  }


  public int sum() {
    int outgoing = 0;
    for (int i = 0; i < count; i++) {
      outgoing += data[i];
    }
    return outgoing;
  }


  /**
   * Sorts the array in place.
   *
   * @webref intlist:method
   * @brief Sorts the array, lowest to highest
   */
  public void sort() {
    Arrays.sort(data, 0, count);
  }


  /**
   * Reverse sort, orders values from highest to lowest.
   *
   * @webref intlist:method
   * @brief Reverse sort, orders values from highest to lowest
   */
  public void sortReverse() {
    new Sort() {
      @Override
      public int size() {
        return count;
      }

      @Override
      public float compare(int a, int b) {
        return data[b] - data[a];
      }

      @Override
      public void swap(int a, int b) {
        int temp = data[a];
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
   * @webref intlist:method
   * @brief Reverse the order of the list elements
   */
  public void reverse() {
    int ii = count - 1;
    for (int i = 0; i < count/2; i++) {
      int t = data[i];
      data[i] = data[ii];
      data[ii] = t;
      --ii;
    }
  }


  /**
   * Randomize the order of the list elements. Note that this does not
   * obey the randomSeed() function in PApplet.
   *
   * @webref intlist:method
   * @brief Randomize the order of the list elements
   */
  public void shuffle() {
    Random r = new Random();
    int num = count;
    while (num > 1) {
      int value = r.nextInt(num);
      num--;
      int temp = data[num];
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
      int temp = data[num];
      data[num] = data[value];
      data[value] = temp;
    }
  }


  public IntList copy() {
    IntList outgoing = new IntList(data);
    outgoing.count = count;
    return outgoing;
  }


  /**
   * Returns the actual array being used to store the data. For advanced users,
   * this is the fastest way to access a large list. Suitable for iterating
   * with a for() loop, but modifying the list will have terrible consequences.
   */
  public int[] values() {
    crop();
    return data;
  }


  @Override
  public Iterator<Integer> iterator() {
//  public Iterator<Integer> valueIterator() {
    return new Iterator<Integer>() {
      int index = -1;

      public void remove() {
        IntList.this.remove(index);
      }

      public Integer next() {
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
   * @webref intlist:method
   * @brief Create a new array with a copy of all the values
   */
  public int[] array() {
    return array(null);
  }


  /**
   * Copy values into the specified array. If the specified array is null or
   * not the same size, a new array will be allocated.
   * @param array
   */
  public int[] array(int[] array) {
    if (array == null || array.length != count) {
      array = new int[count];
    }
    System.arraycopy(data, 0, array, 0, count);
    return array;
  }


//  public int[] toIntArray() {
//    int[] outgoing = new int[count];
//    for (int i = 0; i < count; i++) {
//      outgoing[i] = (int) data[i];
//    }
//    return outgoing;
//  }


//  public long[] toLongArray() {
//    long[] outgoing = new long[count];
//    for (int i = 0; i < count; i++) {
//      outgoing[i] = (long) data[i];
//    }
//    return outgoing;
//  }


//  public float[] toFloatArray() {
//    float[] outgoing = new float[count];
//    System.arraycopy(data, 0, outgoing, 0, count);
//    return outgoing;
//  }


//  public double[] toDoubleArray() {
//    double[] outgoing = new double[count];
//    for (int i = 0; i < count; i++) {
//      outgoing[i] = data[i];
//    }
//    return outgoing;
//  }


//  public String[] toStringArray() {
//    String[] outgoing = new String[count];
//    for (int i = 0; i < count; i++) {
//      outgoing[i] = String.valueOf(data[i]);
//    }
//    return outgoing;
//  }


  /**
   * Returns a normalized version of this array. Called getPercent() for
   * consistency with the Dict classes. It's a getter method because it needs
   * to returns a new list (because IntList/Dict can't do percentages or
   * normalization in place on int values).
   */
  public FloatList getPercent() {
    double sum = 0;
    for (float value : array()) {
      sum += value;
    }
    FloatList outgoing = new FloatList(count);
    for (int i = 0; i < count; i++) {
      double percent = data[i] / sum;
      outgoing.set(i, (float) percent);
    }
    return outgoing;
  }


  public IntList getSubset(int start) {
    return getSubset(start, count - start);
  }


  public IntList getSubset(int start, int num) {
    int[] subset = new int[num];
    System.arraycopy(data, start, subset, 0, num);
    return new IntList(subset);
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
      System.out.format("[%d] %d%n", i, data[i]);
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
      sb.append(i + ": " + data[i]);
    }
    sb.append(" ]");
    return sb.toString();
  }
}

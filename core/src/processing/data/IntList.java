package processing.data;

import processing.core.PApplet;


/**
 * Helper class for a list of floats.
 */
public class IntList {
  int count;
  int[] data;


  public IntList() {
    data = new int[10];
  }


  public IntList(int[] source) {
    count = source.length;
    data = new int[count];
    System.arraycopy(source, 0, data, 0, count);
  }


  /**
   * Get the length of the list.
   */
  public int size() {
    return count;
  }


  /**
   * Get an entry at a particular index.
   */
  public int get(int index) {
    return data[index];
  }


  /**
   * Set the entry at a particular index.
   */
  public void set(int index, int what) {
    if (index >= count) {
      data = PApplet.expand(data, index+1);
    }
    data[index] = what;
  }


  public int removeIndex(int index) {
    int bye = data[index];
    PApplet.println(data);
    int[] outgoing = new int[count - 1];
    System.arraycopy(data, 0, outgoing, 0, index);
    count--;
    System.arraycopy(data, index + 1, outgoing, 0, count - index);
    data = outgoing;
    PApplet.println(data);
    PApplet.println();
    return bye;
  }


  /**
   * Add a new entry to the list.
   */
  public void append(int value) {
    if (count == data.length) {
      data = PApplet.expand(data);
    }
    data[count++] = value;
  }


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


  public boolean contains(int what) {
//    if (indexCache == null) {
//      cacheIndices();
//    }
//    return index(what) != -1;
    for (int i = 0; i < count; i++) {
      if (data[i] == what) {
        return true;
      }
    }
    return false;
  }


  /**
   * Remove all entries from the list.
   */
  public void clear() {
    count = 0;
  }


  /**
   * Improve efficiency by removing allocated but unused entries from the
   * internal array used to store the data.
   */
  public void crop() {
    if (count != data.length) {
      data = PApplet.subset(data, 0, count);
    }
  }


  public void add(int amt) {
    for (int i = 0; i < count; i++) {
      data[i] += amt;
    }
  }


  public void sub(int amt) {
    for (int i = 0; i < count; i++) {
      data[i] -= amt;
    }
  }


  public void mul(int amt) {
    for (int i = 0; i < count; i++) {
      data[i] *= amt;
    }
  }


  public void div(int amt) {
    for (int i = 0; i < count; i++) {
      data[i] /= amt;
    }
  }


  /**
   * Returns the actual array being used to store the data.
   */
  public int[] values() {
    crop();
    return data;
  }


  /**
   * Create a new array with a copy of all the values.
   * @return an array sized by the length of the list with each of the values.
   */
  public int[] toArray() {
    int[] outgoing = new int[count];
    System.arraycopy(data, 0, outgoing, 0, count);
    return outgoing;
  }


  /**
   * Copy as many values as possible into the specified array.
   * @param array
   */
  public void toArray(int[] array) {
    System.arraycopy(data, 0, array, 0, Math.min(count, array.length));
  }
}
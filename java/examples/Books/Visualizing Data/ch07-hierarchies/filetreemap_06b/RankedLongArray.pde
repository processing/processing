// Code from Visualizing Data, First Edition, Copyright 2008 Ben Fry.


class RankedLongArray {
  long[] values = new long[100];
  int count;
  boolean dirty;

  public void add(long what) {
    if (count == values.length) {
      values = (long[]) expand(values);
    }
    values[count++] = what;
    dirty = true;
  }

  public void remove(long what) {
    int index = find(what, 0, count-1);
    arraycopy(values, index+1, values, index, count-index-1);
    count--;
  }

  private void update() {
    Arrays.sort(values, 0, count);
    dirty = false;
  }

  public float percentile(long what) {
    int index = find(what);
    return index / (float)count;
  }

  public int find(long what) {
    return find(what, 0, count-1);
  }

  private int find(long num, int start, int stop) {
    if (dirty) update();
      
    int middle = (start + stop) / 2;

    // if this is the char, then return it
    if (num == values[middle]) return middle;

    // doesn't exist, otherwise would have been the middle
    if (start >= stop) return -1;

    // if it's in the lower half, continue searching that
    if (num < values[middle]) {
      return find(num, start, middle-1);
    }
    // otherwise continue in the upper half
    return find(num, middle+1, stop);
  }
}

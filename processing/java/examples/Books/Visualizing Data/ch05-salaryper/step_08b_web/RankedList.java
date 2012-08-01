// Code from Visualizing Data, First Edition, Copyright 2008 Ben Fry.


import processing.core.PApplet;


public class RankedList {
  // Number of elements in the list
  protected int count; 
  // Array of values for the list
  protected float[] value;
  // Minimum and maximum values in the list
  protected float minValue, maxValue;
  // How this value is represented visually
  protected String[] title;
  // Rank for each item (0 is highest) 
  protected int[] rank;
  // Ordering used while sorting by rank
  protected int[] order;
  // True if the element 0 is the lowest value, and count-1 the largest.
  // (This has no bearing on what is considered the minValue and maxValue.)
  protected boolean ascending;


  RankedList(int count, boolean ascending) {
    this.count = count;
    this.ascending = ascending;

    value = new float[count];
    title = new String[count];
    rank = new int[count];
  }


  public int getCount() {
    return count;
  }


  public float getValue(int index) {
    return value[index];
  }
  
  
  public float getMinValue() {
    return minValue;
  }
  
  
  public float getMaxValue() {
    return maxValue;
  }


  public String getTitle(int index) {
    return title[index];
  }


  public int getRank(int index) {
    return rank[index];
  }


  // Sort the data and calculate min/max values
  void update() {
    // Set up an initial order to be sorted
    order = new int[count];
    for (int i = 0; i < count; i++) {
      order[i] = i;
    }
    sort(0, count-1);

    // Assign rankings based on the order after sorting
    for (int i = 0; i < count; i++) {
      rank[order[i]] = i;
    }

    // Calculate minimum and maximum values
    minValue = PApplet.min(value);
    maxValue = PApplet.max(value);
  }


  void sort(int left, int right) {
    int pivotIndex = (left+right)/2;
    swap(pivotIndex, right);
    int k = partition(left-1, right);
    swap(k, right);
    if ((k-left) > 1) sort(left, k-1);
    if ((right-k) > 1) sort(k+1, right);
  }


  int partition(int left, int right) {
    int pivot = right;
    do {
      while (compare(++left, pivot) < 0) ;
      while ((right != 0) && (compare(--right, pivot) > 0)) ;
      swap(left, right);
    } while (left < right);
    swap(left, right);
    return left;
  }


  float compare(int a, int b) {
    if (ascending) {
      return value[order[a]] - value[order[b]];
    } else {
      return value[order[b]] - value[order[a]];
    }
  }


  void swap(int a, int b) {
    int temp = order[a];
    order[a] = order[b];
    order[b] = temp;
  }
}

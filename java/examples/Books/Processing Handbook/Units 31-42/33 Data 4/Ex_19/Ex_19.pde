int[] x = new int[100]; // Array to store x-coordinates
int count; // Store the number of array positions

void setup() {
  size(100, 100);
}

void draw() {
  x[count] = mouseX; // Assign new x-coordinate to the array
  count++; // Increment the counter
  if (count == x.length) { // If the x array is full,
    x = expand(x); // double the size of x
    println(x.length); // Write the new size to the console
  }
}

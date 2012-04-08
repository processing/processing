float[] data = {19.0, 40.0, 75.0, 76.0, 90.0};
float[] halfData;

void setup() {
  halfData = halve(data); // Run the halve() function
  println(data[0] + ", " + halfData[0]); // Prints "19.0, 9.5"
  println(data[1] + ", " + halfData[1]); // Prints "40.0, 20.0"
  println(data[2] + ", " + halfData[2]); // Prints "75.0, 37.5"
  println(data[3] + ", " + halfData[3]); // Prints "76.0, 38.0"
  println(data[4] + ", " + halfData[4]); // Prints "90.0, 45.0"
}
float[] halve(float[] d) {
  float[] numbers = new float[d.length]; // Create a new array
  arraycopy(d, numbers);
  for (int i = 0; i < numbers.length; i++) { // For each element,
    numbers[i] = numbers[i] / 2; // divide the value by 2
  }
  return numbers; // Return the new array
}

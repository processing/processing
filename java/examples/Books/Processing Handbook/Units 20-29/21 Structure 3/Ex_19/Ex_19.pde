void setup() {
  size(100, 100);
  float f = average(12.0, 6.0); // Assign 9.0 to f
  println(f);
}

float average(float num1, float num2) {
  float av = (num1 + num2) / 2.0;
  return av;
}
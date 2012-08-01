void setup() {
  size(100, 100);
  float c = fahrenheitToCelsius(451.0); // Assign 232.77779 to c
  println(c);
}

float fahrenheitToCelsius(float t) {
  float f = (t-32.0) * (5.0/9.0);
  return f;
}
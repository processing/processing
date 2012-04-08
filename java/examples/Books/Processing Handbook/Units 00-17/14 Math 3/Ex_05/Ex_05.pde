for (float angle = 0; angle < TWO_PI; angle += PI/24.0) {
  float newValue = map(sin(angle), -1, 1, 0, 1000);
  println(newValue);
}
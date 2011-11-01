void draw() {
  int d = day(); // Values from 1 to 31
  int m = month(); // Values from 1 to 12
  if ((d == 1) && (m == 1)) {
    println("Today is the first day of the year!");
  }
}
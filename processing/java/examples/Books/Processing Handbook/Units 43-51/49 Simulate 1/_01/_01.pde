int[] rules = { 0, 0, 0, 1, 1, 1, 1, 0 };
int gen = 1; // Generation
color on = color(255);
color off = color(0);

void setup() {
  size(101, 101);
  frameRate(8); // Slow down to 8 frames each second
  background(0);
  set(width / 2, 0, on); // Set the top, middle pixel to white
}

void draw() {
// For each pixel, determine new state by examining current
// state and neighbor states and ignore edges that have only
// one neighbor
  for (int i = 1; i < width - 1; i++) {
    int left = get(i - 1, gen - 1); // Left neighbor
    int me = get(i, gen - 1); // Current pixel
    int right = get(i + 1, gen - 1); // Right neighbor
    if (rules(left, me, right) == 1) {
      set(i, gen, on);
    }
  }
  gen++; // Increment the generation by 1
  if (gen > height - 1) { // If reached the bottom of the screen,
    noLoop(); // stop the program
  }
}
// Implement the rules
int rules(color a, color b, color c) {
  if ((a == on) && (b == on) && (c == on)) {
    return rules[0];
  }
  if ((a == on) && (b == on) && (c == off)) {
    return rules[1];
  }
  if ((a == on) && (b == off) && (c == on)) {
    return rules[2];
  }
  if ((a == on) && (b == off) && (c == off)) {
    return rules[3];
  }
  if ((a == off) && (b == on) && (c == on)) {
    return rules[4];
  }
  if ((a == off) && (b == on) && (c == off)) {
    return rules[5];
  }
  if ((a == off) && (b == off) && (c == on)) {
    return rules[6];
  }
  if ((a == off) && (b == off) && (c == off)) {
    return rules[7];
  }
  return 0;
}

int num = 20;
int[] dx = new int[num]; // Declare and create an array
int[] dy = new int[num]; // Declare and create an array

void setup() {
  size(100, 100);
  for (int i=0; i<num; i++) {
    dx[i] = i*5;
    dy[i] = 12 + (i*6);
  }
}

void draw() {
  background(204);
  for (int i=0; i<num; i++) {
    dx[i] = dx[i] + 1;
    if (dx[i] > 100) { 
      dx[i] = -100; 
    }
    diagonals(dx[i], dy[i]);
  }
}

void diagonals(int x, int y) {
  line(x, y, x+20, y-40);
  line(x+10, y, x+30, y-40);
  line(x+20, y, x+40, y-40);
}

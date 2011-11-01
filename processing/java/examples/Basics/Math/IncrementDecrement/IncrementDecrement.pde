/**
 * Increment Decrement. 
 * 
 * Writing "a++" is equivalent to "a = a + 1".  
 * Writing "a--" is equivalent to "a = a - 1".   
 */
 
int a;
int b;
boolean direction;

void setup() {
  size(640, 360);
  colorMode(RGB, width);
  a = 0;
  b = width;
  direction = true;
  frameRate(30);
}

void draw() {
  a++;
  if(a > width) {
    a = 0;
    direction = !direction;
  }
  if(direction == true){
    stroke(a);
  } else {
    stroke(width-a);
  }
  line(a, 0, a, height/2);

  b--;
  if(b < 0) {
    b = width;
  }
  if(direction == true) {
    stroke(width-b);
  } else {
    stroke(b);
  }
  line(b, height/2+1, b, height);
}

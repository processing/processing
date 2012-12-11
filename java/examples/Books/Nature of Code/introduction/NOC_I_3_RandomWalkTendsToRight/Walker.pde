// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// A random walker object!

class Walker {
  int x,y;

  Walker() {
    x = width/2;
    y = height/2;
  }

  void render() {
    stroke(0);
    strokeWeight(2);
    point(x,y);
  }

  // Randomly move up, down, left, right, or stay in one place
 void step() {
    
    float r = random(1);
    // A 40% of moving to the right!
    if (r < 0.4) {    
      x++;
    } else if (r < 0.6) {
      x--;
    } else if (r < 0.8) {
      y++;
    } else {
      y--;
    }
  
    x = constrain(x,0,width-1);
    y = constrain(y,0,height-1);
  }
}

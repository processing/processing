/**
 * ArrayList of objects
 * by Daniel Shiffman.  
 * 
 * This example demonstrates how to use a Java ArrayList to store 
 * a variable number of objects.  Items can be added and removed
 * from the ArrayList.
 *
 * Click the mouse to add bouncing balls.
 */

ArrayList<Ball> balls;
int ballWidth = 48;

void setup() {
  size(640, 360);
  noStroke();

  // Create an empty ArrayList (will store Ball objects)
  balls = new ArrayList<Ball>();
  
  // Start by adding one element
  balls.add(new Ball(width/2, 0, ballWidth));
}

void draw() {
  background(255);

  // With an array, we say balls.length, with an ArrayList, we say balls.size()
  // The length of an ArrayList is dynamic
  // Notice how we are looping through the ArrayList backwards
  // This is because we are deleting elements from the list  
  for (int i = balls.size()-1; i >= 0; i--) { 
    // An ArrayList doesn't know what it is storing so we have to cast the object coming out
    Ball ball = balls.get(i);
    ball.move();
    ball.display();
    if (ball.finished()) {
      // Items can be deleted with remove()
      balls.remove(i);
    }
    
  }  
  
}

void mousePressed() {
  // A new ball object is added to the ArrayList (by default to the end)
  balls.add(new Ball(mouseX, mouseY, ballWidth));
}


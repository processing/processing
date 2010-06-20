Robot[] bots;  // Declare array of Robot objects

void setup() {
  size(720, 480);
  PShape robotShape = loadShape("robot1.svg");
  // Create the array of Robot objects
  bots = new Robot[20];  
  // Create each object
  for (int i = 0; i < bots.length; i++) {
    // Create a random x-coordinate
    float x = random(-40, width-40);
    // Assign the y-coordinate based on the order
    float y = map(i, 0, bots.length, -100, height-200);
    bots[i] = new Robot(robotShape, x, y);
  }
  smooth();
}

void draw() {
  background(204);
  // Update and display each bot in the array
  for (int i = 0; i < bots.length; i++) {
    bots[i].update();
    bots[i].display();
  }
}

class Robot {
  float xpos;
  float ypos;
  float angle;
  PShape botShape;
  float yoffset = 0.0;
    
  // Set initial values in constructor
  Robot(PShape shape, float tempX, float tempY) {
    botShape = shape;
    xpos = tempX;
    ypos = tempY;
    angle = random(0, TWO_PI);
  }
 
  // Update the fields
  void update() {
    angle += 0.05;
    yoffset = sin(angle) * 20;
  }
  
  // Draw the robot to the screen
  void display() {
    shape(botShape, xpos, ypos + yoffset);
  } 
  
}


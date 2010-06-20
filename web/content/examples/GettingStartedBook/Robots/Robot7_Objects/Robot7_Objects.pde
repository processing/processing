Robot bot1;
Robot bot2;

void setup() {
  size(720, 480);
  bot1 = new Robot("robot1.svg", 90, 80);
  bot2 = new Robot("robot2.svg", 440, 30);
  smooth();
}

void draw() {
  background(204);

  // Update and display first robot
  bot1.update();
  bot1.display();

  // Update and display second robot
  bot2.update();
  bot2.display();
}

class Robot {
  float xpos;
  float ypos;
  float angle;
  PShape botShape;
  float yoffset = 0.0;
    
  // Set initial values in constructor
  Robot(String svgName, float tempX, float tempY) {
    botShape = loadShape(svgName);
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


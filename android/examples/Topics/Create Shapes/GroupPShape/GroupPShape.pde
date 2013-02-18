/**
 * GroupPShape
 * 
 * How to group multiple PShapes into one PShape
 */


// A PShape that will group PShapes
PShape group;

void setup() {
  size(640, 360, P2D);
  orientation(LANDSCAPE);
  smooth();
  // Create the shape as a group
  group = createShape(GROUP);
  
  // Make a polygon PShape
  PShape star = createShape();
  star.beginShape();
  star.noFill();
  star.stroke(255);
  star.vertex(0, -50);
  star.vertex(14, -20);
  star.vertex(47, -15);
  star.vertex(23, 7);
  star.vertex(29, 40);
  star.vertex(0, 25);
  star.vertex(-29, 40);
  star.vertex(-23, 7);
  star.vertex(-47, -15);
  star.vertex(-14, -20);
  star.endShape(CLOSE);
  
  // Make a path PShape
  PShape path = createShape();
  path.beginShape();
  path.noFill();
  path.stroke(255);
  for (float a = -PI; a < 0; a += 0.1) {
    float r = random(60, 70);
    path.vertex(r*cos(a), r*sin(a));
  }
  path.endShape();
  
  // Make a primitive (Rectangle) PShape
  PShape rectangle = createShape(RECT,-10,-10,20,20);
  rectangle.setFill(false);
  rectangle.setStroke(color(255));
  
  // Add them all to the group
  group.addChild(star);
  group.addChild(path);
  group.addChild(rectangle);
}

void draw() {  
  // We can access them individually via the group PShape
  PShape rectangle = group.getChild(2);
  // Shapes can be rotated
  rectangle.rotate(0.1);
   
  background(52);
  // Display the group PShape
  translate(mouseX, mouseY);
  shape(group);
}


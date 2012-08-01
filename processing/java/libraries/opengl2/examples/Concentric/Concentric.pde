// Concentric, by Andres Colubri
// This example shows how to use the shape recording functionality
// to create a PShape3D object with multiple child shapes that can
// be later organized in a tree structure to apply geometrical
// transformations to different levels of the tree.

import processing.opengl2.*;

PShape3D object;
PShape group1, group2, group3;

void setup() {
  size(600, 600, OPENGL2);
  noStroke();
  
  // We record all the geometry in object.
  object = (PShape3D)beginRecord();
  
  // By setting different names, the different shapes
  // drawn here will be stored in the tree as separate
  // child shapes.
  shapeName("sphere");
  fill(50, 200, 50);
  sphere(50);

  fill(200, 50, 50);  
  shapeName("torus1");
  drawTorus(80, 5, 12, 48, 0);

  shapeName("torus2");
  drawTorus(80, 5, 12, 48, 1);

  shapeName("torus3");
  drawTorus(80, 5, 12, 48, 3);

  fill(50, 50, 200);  
  shapeName("torus4");
  drawTorus(150, 10, 12, 48, 0);
  
  shapeName("torus5");
  drawTorus(150, 10, 12, 48, 1);

  shapeName("torus6");
  drawTorus(150, 10, 12, 48, 3);

  endRecord();
  
  println("BEFORE GROUPING");
  printShape(object, "");  
  
  // Now we group the child shapes by indicating the name of the shapes 
  // we want to put together.
  group1 = object.groupChildren(new String[] {"torus1", "torus2", "torus3", "torus4", "torus5", "torus6"}, "group1");
  group2 = object.groupChildren(new String[] {"torus1", "torus2", "torus3"}, "group2");
  group3 = object.groupChildren(new String[] {"torus4", "torus5", "torus6"}, "group3");
    
  println("AFTER GROUPING");  
  printShape(object, "");    
}

void draw() {
  background(0);

  // Transformations to parts of the shape can be applied 
  // before drawing the entire object.
  object.resetMatrix();
  group1.resetMatrix();
  group2.resetMatrix();
  group3.resetMatrix();
  object.translate(0, 0, map(sin(frameCount * PI / 600), -1, 1, 550, 0));
  group1.scale(map(cos(frameCount * PI / 200), -1, 1, 1, 1.5));
  group2.rotateX(frameCount * PI / 100);  
  group2.rotateY(frameCount * PI / 100);
  group3.rotateX(-frameCount * PI / 140);  
  group3.rotateY(-frameCount * PI / 160);

  pointLight(250, 250, 250, 0, 0, 400);

  translate(width/2, height/2);
  shape(object);
}

void printShape(PShape pshape, String tab) {
  int f = pshape.getFamily();
  if (f == PShape.GROUP) println(tab + "GROUP shape");
  else if (f == PShape.GEOMETRY) println(tab + "GEOMETRY shape");
  println(tab + "Name: " + pshape.getName());  
  println(tab + "Children: " + pshape.getChildCount());  

  for (int i = 0; i < pshape.getChildCount(); i++) {
    PShape child = pshape.getChild(i);
    printShape(child, tab + "  ");
  } 
  println(tab + "-------------------");
}

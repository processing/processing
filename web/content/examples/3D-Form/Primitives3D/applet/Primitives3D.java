import processing.core.*; import java.applet.*; import java.awt.*; import java.awt.image.*; import java.awt.event.*; import java.io.*; import java.net.*; import java.text.*; import java.util.*; import java.util.zip.*; public class Primitives3D extends PApplet {public void setup() {// Primitives 3D
// by REAS <http://reas.com> 

// Placing mathematically 3D objects in synthetic space.
// The lights() method reveals their imagined dimension.
// The box() and sphere() functions each have one parameter
// which is used to specify their size. These shapes are
// positioned using the translate() function.

// Created 16 January 2003

size(200, 200, P3D); 
background(0);
lights();
noStroke();

pushMatrix();
translate(47, height/2, 0);
rotateY(0.75f);
box(50);
popMatrix();

pushMatrix();
translate(200, height/2, 0);
sphere(100);
popMatrix();


noLoop(); }}
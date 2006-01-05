import processing.core.*; import java.applet.*; import java.awt.*; import java.awt.image.*; import java.awt.event.*; import java.io.*; import java.net.*; import java.text.*; import java.util.*; import java.util.zip.*; public class CurvePrimitives extends PApplet {public void setup() {// Curve Primitives
// by REAS <http://reas.com>

// The two curve primitive functions curve() and
// bezier() can each draw elegant curves, but they
// operate in entirely different ways. 
// For the curve() function, the first and second parameters 
// specify the control points the curve and the last two 
// parameters specify the second point of the curve. 
// The middle parameters set points along the curve. 
// For the bezier() function, the first two parameters specify the 
// first point in the curve and the last two parameters specify 
// the last point. The middle parameters set the control points
// which define the shape of the curve. 


// Created 16 January 2003

size(200, 200); 
background(0); 

stroke(255);
curve(55, 35, 15, 130, 60, 145, 150, 110);
 
stroke(176); 
for(int i=0; i<100; i+=5) {
  bezier(90-(i/2.0f), 20+i, 210, 10, 220, 150, 120-(i/8.0f), 150+(i/4.0f));
}
noLoop(); }}
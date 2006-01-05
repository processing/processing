// Curve Primitives
// by REAS <http://www.groupc.net> 

// The two curve primitive functions curve() and
// bezier() can each draw elegant curves, but they
// operate in entirely different ways. 
// For the curve() function, the first and second parameters 
// specify the first point of the curve and the last two 
// parameters specify the second point of the curve. 
// The middle parameters set additional points along the curve. 
// For the bezier() function, the first two parameters specify the 
// first point in the curve and the last two parameters specify 
// the last point. The middle parameters set the control points
// which define the shape of the curve. 


// Created 16 January 2003

size(200, 200); 
background(0); 
stroke(102);
curve(55, 35, 15, 130, 60, 145, 150, 110);
 
stroke(204); 
for(int i=0; i<100; i+=5) {
  bezier(90-(i/2.0), 20+i, 210, 10, 220, 150, 120-(i/8.0), 150+(i/4.0));
}

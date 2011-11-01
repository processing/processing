/**
 * Coordinates. 
 * 
 * All shapes drawn to the screen have a position that is 
 * specified as a coordinate. All coordinates are measured 
 * as the distance from the origin in units of pixels.
 * The origin [0, 0] is the coordinate is in the upper left 
 * of the window and the coordinate in the lower right is 
 * [width-1, height-1].  
 */

// Sets the screen to be 200, 200, so the width of the window is 200 pixels
// and the height of the window is 200 pixels
size(640, 360);
background(0);
noFill();


// The two parameters of the point() method each specify coordinates.
// The first parameter is the x-coordinate and the second is the Y 
stroke(255);
point(width * 0.5, height * 0.5);
point(width * 0.5, height * 0.25); 

// Coordinates are used for drawing all shapes, not just points.
// Parameters for different functions are used for different purposes.
// For example, the first two parameters to line() specify 
// the coordinates of the first point and the second two parameters 
// specify the second point
stroke(0, 153, 255);
line(0, height*0.33, width, height*0.33);

// By defaulty, the first two parameters to rect() are the 
// coordinates of the upper-left corner and the second pair
// is the width and height
stroke(255, 153, 0);
rect(width*0.25, height*0.1, width * 0.5, height * 0.8);

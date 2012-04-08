/**
 * Synthesis 1: Form and Code
 * Mandelbrot Set by William Ngan (http:/www.metaphorical.net)
 * p. 153
 */


size(600, 600);

float co; // color
int accuracy = 200;  // Lower numbers are less accurate (lower resolution)
float zoom = 200.0;  // Try these: 200 250 350 500 700 950 1250 1600 2000 2450 2950 3500
float dx = width/2 - mouseX;
float dy = height/2 - mouseY;
float xscale = width / zoom;
float yscale = height / zoom;
float xOff = dx/width * xscale / 2;
float yOff = -1 + (dy/height * yscale / 2);

for ( int i=0; i<width; i++ ) {
  for (int j=0; j<height; j++) { 
    // For every point on complex plane     
    float re = i/zoom - xOff - width/2.0/zoom; 
    float im = j/zoom - yOff - height/2.0/zoom; 
    // Complex number z, c 
    float z1 = 0.0;
    float z2 = 0.0; 
    co = 0;  

    for (int k=0; k<accuracy; k++) { 
      float zz1 = sq(z1) - sq(z2) + re;
      float zz2 = 2*z1*z2 + im;
      z1 = zz1;
      z2 = zz2; 
      // Check if the modulus of complex num is within limit 
      if ( sqrt((sq(z1) + sq(z2)) ) > 2 ) { 
        co = 1 - k/float(accuracy); 
        break; // NOTE: This is new syntax!!!
      } 
    } 
    stroke(color(255*co, 255*co, 255*co));
    point(i, j);
  } 
}


// save("Synthesis-04--" + zoom + ".tif");

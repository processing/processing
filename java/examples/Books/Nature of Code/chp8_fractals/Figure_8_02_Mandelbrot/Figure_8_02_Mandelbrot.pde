// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// The Mandelbrot Set

// Simple rendering of the Mandelbrot set
// c = a + bi
// Iterate z = z^2 + c, i.e.
// z(0) = 0
// z(1) = 0*0 + c
// z(2) = c*c + c
// z(3) = (c*c + c) * (c*c + c) + c
// etc.

// c*c = (a+bi) * (a+bi) = a^2 - b^2 + 2abi

// Establish a range of values on the complex plane
double xmin = -2.5; double ymin = -1; double w = 4; double h = 2;
// A different range will allow us to "zoom" in or out on the fractal
// double xmin = -1.5; double ymin = -.1; double wh = 0.15;

void setup() {
  size(863,863/2);
}

void draw() {

  loadPixels();
  
  // Maximum number of iterations for each point on the complex plane
  int maxiterations = 200;

  // x goes from xmin to xmax
  double xmax = xmin + w;
  // y goes from ymin to ymax
  double ymax = ymin + h;
  
  // Calculate amount we increment x,y for each pixel
  double dx = (xmax - xmin) / (width);
  double dy = (ymax - ymin) / (height);

  // Start y
  double y = ymin;
  for(int j = 0; j < height; j++) {
    // Start x
    double x = xmin;
    for(int i = 0;  i < width; i++) {
      
      // Now we test, as we iterate z = z^2 + cm does z tend towards infinity?
      double a = x;
      double b = y;
      int n = 0;
      while (n < maxiterations) {
        double aa = a * a;
        double bb = b * b;
        double twoab = 2.0 * a * b;
        a = aa - bb + x;
        b = twoab + y;
        // Infinty in our finite world is simple, let's just consider it 16
        if(aa + bb > 16.0f) {
          break;  // Bail
        }
        n++;
      }
      
      // We color each pixel based on how long it takes to get to infinity
      // If we never got there, let's pick the color black
      if (n == maxiterations) pixels[i+j*width] = color(0);
      else pixels[i+j*width] = color(n*16 % 255);  // Gosh, we could make fancy colors here if we wanted
      x += dx;
    }
    y += dy;
  }
  updatePixels();
  
  save("chapter08_02.png");
  noLoop();
}


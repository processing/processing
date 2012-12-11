// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// "Landscape" example

class Landscape {

  int scl;           // size of each cell
  int w,h;           // width and height of thingie
  int rows, cols;    // number of rows and columns
  float zoff = 0.0;  // perlin noise argument
  float[][] z;       // using an array to store all the height values 

  Landscape(int scl_, int w_, int h_) {
    scl = scl_;
    w = w_;
    h = h_;
    cols = w/scl;
    rows = h/scl;
    z = new float[cols][rows];
  }


  // Calculate height values (based off a neural netork)
  void calculate(Network nn) {
    float x = 0;
    float dx = (float) 1.0 / cols;
    for (int i = 0; i < cols; i++)
    { 
      float y = 0;
      float dy = (float) 1.0 / rows;
      for (int j = 0; j < rows; j++)
      {
        float[] input = new float[2];
        input[0] = x; 
        input[1] = y;
        float result = nn.feedForward(input);
        z[i][j] = z[i][j]*0.95 + 0.05*(float)(result*280.0f-140.0);
        y += dy;
      }
      x += dx;
    }

  }

  // Render landscape as grid of quads
  void render() {
    // Every cell is an individual quad
    // (could use quad_strip here, but produces funny results, investigate this)
    for (int x = 0; x < z.length-1; x++)
    {
      for (int y = 0; y < z[x].length-1; y++)
      {
        // one quad at a time
        // each quad's color is determined by the height value at each vertex
        // (clean this part up)
        noStroke();
        pushMatrix();
        beginShape(QUADS);
        translate(x*scl-w/2,y*scl-h/2,0);
        fill(z[x][y]+127,220);
        vertex(0,0,z[x][y]);
        fill(z[x+1][y]+127,220);
        vertex(scl,0,z[x+1][y]);
        fill(z[x+1][y+1]+127,220);
        vertex(scl,scl,z[x+1][y+1]);
        fill(z[x][y+1]+127,220);
        vertex(0,scl,z[x][y+1]);
        endShape();
        popMatrix();
      }
    }
  }
}

// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

// "Landscape" example

class Landscape {

  int scl;           // size of each cell
  int w, h;           // width and height of thingie
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
  void calculate() {
    float xoff = 0;
    for (int i = 0; i < cols; i++)
    { 
      float yoff = 0;
      for (int j = 0; j < rows; j++)
      {
        z[i][j] = map(noise(xoff, yoff,zoff), 0, 1, -120, 120);
        yoff += 0.1;
      }
      xoff += 0.1;
    }
    zoff+=0.01;
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
        stroke(0);
        fill(100, 100);
        pushMatrix();
        beginShape(QUADS);
        translate(x*scl-w/2, y*scl-h/2, 0);
        vertex(0, 0, z[x][y]);
        vertex(scl, 0, z[x+1][y]);
        vertex(scl, scl, z[x+1][y+1]);
        vertex(0, scl, z[x][y+1]);
        endShape();
        popMatrix();
      }
    }
  }
}


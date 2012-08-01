// Code from Visualizing Data, First Edition, Copyright 2008 Ben Fry.


public class ColorIntegrator extends Integrator {

  float r0, g0, b0, a0;
  float rs, gs, bs, as; 

  int colorValue;


  public ColorIntegrator(int color0, int color1) {
    int a1 = (color0 >> 24) & 0xff;
    int r1 = (color0 >> 16) & 0xff;
    int g1 = (color0 >>  8) & 0xff;
    int b1 = (color0      ) & 0xff;

    int a2 = (color1 >> 24) & 0xff;
    int r2 = (color1 >> 16) & 0xff;
    int g2 = (color1 >>  8) & 0xff;
    int b2 = (color1      ) & 0xff;

    r0 = (float)r1 / 255.0f;
    g0 = (float)g1 / 255.0f;
    b0 = (float)b1 / 255.0f;
    a0 = (float)a1 / 255.0f;

    rs = (r2 - r1) / 255.0f;
    gs = (g2 - g1) / 255.0f;
    bs = (b2 - b1) / 255.0f;
    as = (a2 - a1) / 255.0f;
  }


  public boolean update() {
    boolean updated = super.update();
    if (updated) {
      colorValue = 
        (((int) ((a0 + as*value) * 255f) << 24) |
          ((int) ((r0 + rs*value) * 255f) << 16) |
          ((int) ((g0 + gs*value) * 255f) <<  8) |
          ((int) ((b0 + bs*value) * 255f)));
    }
    return updated;
  }


  public int get() {
    return colorValue;
  }
}

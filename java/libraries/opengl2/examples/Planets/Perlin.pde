// Implementation of 1D, 2D, and 3D Perlin noise. Based on the 
// C code by Paul Bourke:
// http://local.wasp.uwa.edu.au/~pbourke/texture_colour/perlin/
class Perlin {
  int B = 0x100;
  int BM = 0xff;
  int N = 0x1000;
  int NP = 12; 
  int NM = 0xfff;

  int p[];
  float g3[][];
  float g2[][];
  float g1[];

  void normalize2(float v[]) {
    float s = sqrt(v[0] * v[0] + v[1] * v[1]);
    v[0] = v[0] / s;
    v[1] = v[1] / s;
  }

  void normalize3(float v[]) {
    float s = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    v[0] = v[0] / s;
    v[1] = v[1] / s;
    v[2] = v[2] / s;
  }

  float sCurve(float t) {
    return t * t * (3.0 - 2.0 * t);
  }

  float at2(float q[], float rx, float ry) { 
    return rx * q[0] + ry * q[1];
  }

  float at3(float q[], float rx, float ry, float rz) { 
    return rx * q[0] + ry * q[1] + rz * q[2];
  }

  Perlin() {
    p = new int[B + B + 2];
    g3 = new float[B + B + 2][3];
    g2 = new float[B + B + 2][2];
    g1 = new float[B + B + 2];  

    init();
  }  

  void init() {
    int i, j, k;

    for (i = 0 ; i < B ; i++) {
      p[i] = i;
      g1[i] = (random(B + B) - B) / B;

      for (j = 0 ; j < 2 ; j++)
        g2[i][j] = (random(B + B) - B) / B;
      normalize2(g2[i]);

      for (j = 0 ; j < 3 ; j++)
        g3[i][j] = (random(B + B) - B) / B;
      normalize3(g3[i]);
    }

    while (0 < --i) {
      k = p[i];
      p[i] = p[j = int(random(B))];
      p[j] = k;
    }

    for (i = 0 ; i < B + 2 ; i++) {
      p[B + i] = p[i];
      g1[B + i] = g1[i];
      for (j = 0 ; j < 2 ; j++)
        g2[B + i][j] = g2[i][j];
      for (j = 0 ; j < 3 ; j++)
        g3[B + i][j] = g3[i][j];
    }
  }

  float noise1(float[] vec) {
    int bx0, bx1;
    float rx0, rx1, sx, t, u, v;

    t = vec[0] + N;
    bx0 = int(t) & BM;
    bx1 = (bx0 + 1) & BM;
    rx0 = t - int(t);
    rx1 = rx0 - 1.0; 

    sx = sCurve(rx0);
    u = rx0 * g1[p[bx0]];
    v = rx1 * g1[p[bx1]];

    return lerp(u, v, sx);
  }

  float noise2(float[] vec) {
    int bx0, bx1, by0, by1, b00, b10, b01, b11;
    float rx0, rx1, ry0, ry1, sx, sy, a, b, t, u, v;
    float[] q;    
    int i, j;

    t = vec[0] + N;
    bx0 = int(t) & BM;
    bx1 = (bx0 + 1) & BM;
    rx0 = t - int(t);
    rx1 = rx0 - 1.0; 

    t = vec[1] + N;
    by0 = int(t) & BM;
    by1 = (by0 + 1) & BM;
    ry0 = t - int(t);
    ry1 = ry0 - 1.0;

    i = p[bx0];
    j = p[bx1];

    b00 = p[i + by0];
    b10 = p[j + by0];
    b01 = p[i + by1];
    b11 = p[j + by1];

    sx = sCurve(rx0);
    sy = sCurve(ry0);

    q = g2[b00]; 
    u = at2(q, rx0, ry0);
    q = g2[b10]; 
    v = at2(q, rx1, ry0);
    a = lerp(u, v, sx);

    q = g2[b01] ; 
    u = at2(q, rx0, ry1);
    q = g2[b11] ; 
    v = at2(q, rx1, ry1);
    b = lerp(u, v, sx);

    return lerp(a, b, sy);
  }

  float noise3(float[] vec) {
    int bx0, bx1, by0, by1, bz0, bz1, b00, b10, b01, b11;
    float rx0, rx1, ry0, ry1, rz0, rz1, sy, sz, a, b, c, d, t, u, v;
    float[] q;
    int i, j;

    t = vec[0] + N;
    bx0 = int(t) & BM;
    bx1 = (bx0 + 1) & BM;
    rx0 = t - int(t);
    rx1 = rx0 - 1.0;

    t = vec[1] + N;
    by0 = int(t) & BM;
    by1 = (by0 + 1) & BM;
    ry0 = t - int(t);
    ry1 = ry0 - 1.0;

    t = vec[2] + N;
    bz0 = int(t) & BM;
    bz1 = (bz0 + 1) & BM;
    rz0 = t - int(t);
    rz1 = rz0 - 1.0;

    i = p[bx0];
    j = p[bx1];

    b00 = p[i + by0];
    b10 = p[j + by0];
    b01 = p[i + by1];
    b11 = p[j + by1];

    t  = sCurve(rx0);
    sy = sCurve(ry0);
    sz = sCurve(rz0);

    q = g3[b00 + bz0]; 
    u = at3(q, rx0, ry0, rz0);
    q = g3[b10 + bz0]; 
    v = at3(q, rx1, ry0, rz0);
    a = lerp(u, v, t);

    q = g3[b01 + bz0]; 
    u = at3(q, rx0, ry1, rz0);
    q = g3[b11 + bz0]; 
    v = at3(q, rx1, ry1, rz0);
    b = lerp(u, v, t);

    c = lerp(a, b, sy);

    q = g3[b00 + bz1]; 
    u = at3(q, rx0, ry0, rz1);
    q = g3[b10 + bz1]; 
    v = at3(q, rx1, ry0, rz1);
    a = lerp(u, v, t);

    q = g3[b01 + bz1]; 
    u = at3(q, rx0, ry1, rz1);
    q = g3[b11 + bz1]; 
    v = at3(q, rx1, ry1, rz1);
    b = lerp(u, v, t);

    d = lerp(a, b, sy);

    return lerp(c, d, sz);
  }

  // In what follows "nalpha" is the weight when the sum is formed.
  // Typically it is 2, as this approaches 1 the function is noisier.
  // "nbeta" is the harmonic scaling/spacing, typically 2. n is the
  // number of harmonics added up in the final result. Higher number 
  // results in more detailed noise.

  float noise1D(float x, float nalpha, float nbeta, int n) {
    float val, sum = 0;
    float v[] = {x};
    float nscale = 1;

    for (int i = 0; i < n; i++) {
      val = noise1(v);
      sum += val / nscale;
      nscale *= nalpha;
      v[0] *= nbeta;
    }
    return sum;
  }

  float noise2D(float x, float y, float nalpha, float nbeta, int n) {
   float val,sum = 0;
   float v[] = {x, y};
   float nscale = 1;

   for (int i = 0; i < n; i++) {
      val = noise2(v);
      sum += val / nscale;
      nscale *= nalpha;
      v[0] *= nbeta;
      v[1] *= nbeta;
   }
   return sum;
  }

  float noise3D(float x, float y, float z, float nalpha, float nbeta, int n) {
    float val, sum = 0;
    float v[] = {x, y, z};
    float nscale = 1;

    for (int i = 0 ; i < n; i++) {
      val = noise3(v);
      sum += val / nscale;
      nscale *= nalpha;
      v[0] *= nbeta;
      v[1] *= nbeta;
      v[2] *= nbeta;
    }
    return sum;
  }
}


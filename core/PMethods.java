/*
package processing.core;



// this file is semiautomatically generated. no touchy-touchy.

public interface PMethods {

  public void beginFrame();

  public void endFrame();

  public void defaults();

  public void hint(int which);

  public void unhint(int which);

  //

  public void beginShape();

  public void beginShape(int kind);

  public void normal(float nx, float ny, float nz);  // 3D

  public void textureMode(int mode);  // 3D

  public void texture(PImage image);  // 3D

  public void vertex(float x, float y);

  public void vertex(float x, float y, float u, float v);  // 3D

  public void vertex(float x, float y, float z);  // 3D

  public void vertex(float x, float y, float z,  // 3D
                     float u, float v);

  public void bezierVertex(float x1, float y1,
                           float x2, float y2,
                           float x3, float y3);

  public void bezierVertex(float x1, float y1, float z1,  // 3D
                           float x2, float y2, float z2,
                           float x3, float y3, float z3);

  public void curveVertex(float x, float y);

  public void curveVertex(float x, float y, float z);  // 3D

  public void endShape();

  //

  public void point(float x, float y);

  public void point(float x, float y, float z);  // 3D

  public void line(float x1, float y1, float x2, float y2);

  public void line(float x1, float y1, float z1,
                   float x2, float y2, float z2);  // 3D

  public void triangle(float x1, float y1, float x2, float y2,
                       float x3, float y3);

  public void quad(float x1, float y1, float x2, float y2,
                   float x3, float y3, float x4, float y4);

  //

  public void rectMode(int mode);

  public void rect(float x1, float y1, float x2, float y2);

  //protected void rectImpl(float x1, float y1, float x2, float y2);

  //

  public void ellipseMode(int mode);

  public void ellipse(float x, float y, float hradius, float vradius);

  //protected void ellipseImpl(float x, float y, float hradius, float vradius);

  //

  public void arc(float x, float y, float hr, float vr,
                  float start, float stop);

  //protected void arcImpl(float start, float stop,
  //                      float x, float y, float hr, float vr);

  //

  // also considered using the modes for box() and sphere(),
  // but then decided against because when rotating/scaling/etc
  // it's gonna make things totally bizarre.

  // rectMode(CORNER, CORNERS, CENTER, CENTER_DIAMETER
  // box(x, y, z, w, h, d) CORNER
  // box(x, y, z, x2, y2, z2) CORNERS
  // box(x, y, z, w, h, d) CENTER (but centered around x, y, z)
  // box(x, y, z, xr, yr, zr) CENTER_RADIUS

  // sphere(x, y, z, r) CENTER
  // sphere(x, y, z, r) CORNER draws the ellipse starting int the corner

  public void box(float size);  // 3D

  public void box(float w, float h, float d);  // 3D

  public void sphereDetail(int res);  // 3D

  public void sphere(float r);  // 3D

  //

  public float bezierPoint(float a, float b, float c, float d,
                           float t);

  public float bezierTangent(float a, float b, float c, float d,
                             float t);

  public void bezierDetail(int detail);

  public void bezier(float x1, float y1,
                     float x2, float y2,
                     float x3, float y3,
                     float x4, float y4);

  public void bezier(float x1, float y1, float z1,
                     float x2, float y2, float z2,
                     float x3, float y3, float z3,
                     float x4, float y4, float z4);  // 3D

  //

  public float curvePoint(float a, float b, float c, float d,
                          float t);

  public float curveTangent(float a, float b, float c, float d,
                            float t);

  public void curveDetail(int detail);

  public void curveTightness(float tightness);

  public void curve(float x1, float y1,
                    float x2, float y2,
                    float x3, float y3,
                    float x4, float y4);

  public void curve(float x1, float y1, float z1,
                    float x2, float y2, float z2,
                    float x3, float y3, float z3,
                    float x4, float y4, float z4);  // 3D
  //

  public void image(PImage image, float x1, float y1);

  public void image(PImage image,
                    float x1, float y1, float x2, float y2);

  public void image(PImage image,
                    float x1, float y1, float x2, float y2,
                    int u1, int v1, int u2, int v2);

  //protected void imageImpl(PImage image,
  //                         float x1, float y1, float x2, float y2,
  //                         int u1, int v1, int u2, int v2);

  //

  public void textAlign(int alignment);

  public float textAscent();

  public float textDescent();

  public void textFont(PFont which);

  public void textFont(PFont which, float size);

  public void textLeading(float leading);

  public void textMode(int mode);

  public void textSize(float size);

  public float textWidth(char c);

  public float textWidth(String s);

  //

  public void text(char c, float x, float y);

  public void text(char c, float x, float y, float z);  // 3D

  public void text(String s, float x, float y);

  public void text(String s, float x, float y, float z);  // 3D

  public void text(String s, float x, float y, float w, float h);

  public void text(String s, float x, float y, float w, float h,  // 3D
                   float z);

  public void text(int num, float x, float y);

  public void text(int num, float x, float y, float z);  // 3D

  public void text(float num, float x, float y);

  public void text(float num, float x, float y, float z);  // 3D

  //protected void textImpl(char ch, float x, float y, float z);

  //protected void textImplObject(PImage glyph,
  //                              float x1, float y1, float z1,
  //                              float x2, float y2, float z2,
  //                              int u2, int v2);

  //protected void textImplScreen(PImage glyph,
  //                              int xx, int yy, //int x2, int y2,
  //                              int w0, int h0);

  //

  public void translate(float tx, float ty);

  public void translate(float tx, float ty, float tz);  // 3D

  public void rotate(float angle);

  public void rotateX(float angle);  // 3D

  public void rotateY(float angle);  // 3D

  public void rotateZ(float angle);  // 3D

  public void rotate(float angle, float v0, float v1, float v2);  // 3D

  public void scale(float s);

  public void scale(float sx, float sy);

  public void scale(float sx, float sy, float sz);  // 3D

  //

  public void pushMatrix();

  public void popMatrix();

  public void resetMatrix();

  public void applyMatrix(float n00, float n01, float n02,
                          float n10, float n11, float n12);

  public void applyMatrix(float n00, float n01, float n02, float n03,
                          float n10, float n11, float n12, float n13,
                          float n20, float n21, float n22, float n23,
                          float n30, float n31, float n32, float n33);  // 3D

  public void loadMatrix();

  public void printMatrix();

  //

  public void beginCamera();  // 3D

  public void endCamera();  // 3D

  public void camera();  // 3D

  public void camera(float eyeX, float eyeY, float eyeZ,
                     float centerX, float centerY, float centerZ,
                     float upX, float upY, float upZ);  // 3D

  public void printCamera();  // 3D

  //

  public void ortho();  // 3D

  public void ortho(float left, float right,  // 3D
                    float bottom, float top,
                    float near, float far);

  public void perspective();  // 3D

  public void perspective(float fovy, float aspect,
                          float zNear, float zFar);  // 3D

  public void frustum(float left, float right, float bottom,
                      float top, float znear, float zfar);  // 3D

  public void printProjection();  // 3D

  //

  public float screenX(float x, float y);

  public float screenY(float x, float y);

  public float screenX(float x, float y, float z);  // 3D

  public float screenY(float x, float y, float z);  // 3D

  public float screenZ(float x, float y, float z);  // 3D

  public float modelX(float x, float y, float z);  // 3D

  public float modelY(float x, float y, float z);  // 3D

  public float modelZ(float x, float y, float z);  // 3D

  //

  public void colorMode(int mode);

  public void colorMode(int mode, float max);

  public void colorMode(int mode,
                        float maxX, float maxY, float maxZ);

  public void colorMode(int mode,
                        float maxX, float maxY, float maxZ, float maxA);

  //

  //protected void colorCalc(float gray);

  //protected void colorCalc(float gray, float alpha);

  //protected void colorCalc(float x, float y, float z);

  //protected void colorCalc(float x, float y, float z, float a);

  //protected void colorCalcARGB(int argb);

  //

  public void strokeWeight(float weight);

  public void strokeJoin(int join);

  public void strokeCap(int cap);

  public void noStroke();

  public void stroke(int rgb);

  public void stroke(float gray);

  public void stroke(float gray, float alpha);

  public void stroke(float x, float y, float z);

  public void stroke(float x, float y, float z, float a);

  protected void colorStroke();

  //

  public void noTint();

  public void tint(int rgb);

  public void tint(float gray);

  public void tint(float gray, float alpha);

  public void tint(float x, float y, float z);

  public void tint(float x, float y, float z, float a);

  //protected void tintFromCalc();

  //

  public void noFill();

  public void fill(int rgb);

  public void fill(float gray);

  public void fill(float gray, float alpha);

  public void fill(float x, float y, float z);

  public void fill(float x, float y, float z, float a);

  //protected void fillFromCalc();

  //

  public void ambient(int rgb);  // 3D

  public void ambient(float gray);  // 3D

  public void ambient(float x, float y, float z);  // 3D

  //protected void ambientFromCalc();  // 3D

  //

  public void specular(int rgb);  // 3D

  public void specular(float gray);  // 3D

  public void specular(float gray, float alpha);  // 3D

  public void specular(float x, float y, float z);  // 3D

  public void specular(float x, float y, float z, float a);  // 3D

  //protected void specularFromCalc();  // 3D

  public void shininess(float shine);  // 3D

  //

  public void emissive(int rgb);  // 3D

  public void emissive(float gray);  // 3D

  public void emissive(float x, float y, float z);  // 3D

  //protected void emissiveFromCalc();

  //

  public void lights();  // 3D

  public void ambientLight(float red, float green, float blue);  // 3D

  public void ambientLight(float red, float green, float blue,  // 3D
                           float x, float y, float z);

  public void directionalLight(float red, float green, float blue,  // 3D
                               float nx, float ny, float nz);

  public void pointLight(float red, float green, float blue,  // 3D
                         float x, float y, float z);

  public void spotLight(float red, float green, float blue,  // 3D
                        float x, float y, float z,
                        float nx, float ny, float nz,
                        float angle, float concentration);

  public void lightFalloff(float constant,
                           float linear, float quadratic);  // 3D

  public void lightSpecular(float x, float y, float z);  // 3D

  //

  public void background(int rgb);

  public void background(float gray);

  public void background(float x, float y, float z);

  public void background(PImage image);

  //protected void backgroundFromCalc();

  public void clear();

  //

  //public final int color(int gray)

  //public final int color(float gray)

  //public final int color(int gray, int alpha)

  //public final int color(float gray, float alpha)

  //public final int color(int x, int y, int z)

  //public final int color(float x, float y, float z)

  //public final int color(int x, int y, int z, int a)

  //public final int color(float x, float y, float z, float a)

  //public final float alpha(int what)

  //public final float red(int what)

  //public final float green(int what)

  //public final float blue(int what)

  //public final float hue(int what)

  //public final float saturation(int what)

  //public final float brightness(int what)

  //

  public void imageMode(int mode);

  public void smooth();

  public void noSmooth();


  ///////////////////////////////////////////////////////////

  // all functions below this point require pixel buffer manipulation
  // and generally are wrapped or handled internally with a call to
  // loadPixels or updatePixels.. not supported for vector subclasses.


  public void loadPixels();

  public void updatePixels();

  public void updatePixels(int x, int y, int c, int d);

  //

  public int get(int x, int y);

  public PImage get(int x, int y, int c, int d);

  public PImage get();

  public void set(int x, int y, int argb);

  public void set(int x, int y, PImage image);

  //protected void setImpl(int dx, int dy, int sx, int sy, int sw, int sh,
  //                       PImage src) {

  //

  public void mask(int alpha[]);

  public void mask(PImage alpha);

  //

  public void filter(int kind);

  public void filter(int kind, float param);

  //

  public void copy(int sx1, int sy1, int sx2, int sy2,
                   int dx1, int dy1, int dx2, int dy2);

  public void copy(PImage src,
                   int sx1, int sy1, int sx2, int sy2,
                   int dx1, int dy1, int dx2, int dy2);

  //

  public void blend(int sx, int sy, int dx, int dy, int mode);

  public void blend(PImage src,
                    int sx, int sy, int dx, int dy, int mode);

  public void blend(int sx1, int sy1, int sx2, int sy2,
                    int dx1, int dy1, int dx2, int dy2, int mode);

  public void blend(PImage src,
                    int sx1, int sy1, int sx2, int sy2,
                    int dx1, int dy1, int dx2, int dy2, int mode);

  //

  //static public boolean saveHeaderTIFF(OutputStream output,
  //                                     int width, int height);

  //static public boolean saveTIFF(OutputStream output, int pixels[],
  //                               int width, int height);

  //static public boolean saveHeaderTGA(OutputStream output,
  //                                    int width, int height) {

  //static public boolean saveTGA(OutputStream output, int pixels[],
  //                              int width, int height) {

  public void save(String filename);

*/

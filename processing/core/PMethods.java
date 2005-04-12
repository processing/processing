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

  public void normal(float nx, float ny, float nz);

  public void textureMode(int mode);

  public void texture(PImage image);

  public void vertex(float x, float y);

  public void vertex(float x, float y, float u, float v);

  public void vertex(float x, float y, float z);

  public void vertex(float x, float y, float z,
                     float u, float v);

  public void bezierVertex(float x1, float y1,
                           float x2, float y2,
                           float x3, float y3);

  public void bezierVertex(float x1, float y1, float z1,
                           float x2, float y2, float z2,
                           float x3, float y3, float z3);

  public void curveVertex(float x, float y);

  public void curveVertex(float x, float y, float z);

  public void endShape();

  //

  public void point(float x, float y);

  public void point(float x, float y, float z);

  public void line(float x1, float y1, float x2, float y2);

  public void line(float x1, float y1, float z1,
                   float x2, float y2, float z2);

  public void triangle(float x1, float y1, float x2, float y2,
                       float x3, float y3);

  public void quad(float x1, float y1, float x2, float y2,
                   float x3, float y3, float x4, float y4);

  //

  public void rectMode(int mode);

  public void rect(float x1, float y1, float x2, float y2);

  //protected void rectImpl(float x1, float y1, float x2, float y2);

  // REMOVED public void circle(float x, float y, float radius);

  public void ellipseMode(int mode);

  public void ellipse(float x, float y, float hradius, float vradius);

  //protected void ellipseImpl(float x, float y, float hradius, float vradius);

  // REMOVED public void arcMode(int mode);

  // REMOVED public void arc(float start, float stop,
  //               float x, float y, float radius);

  public void arc(float x, float y, float hr, float vr,
                  float start, float stop);

  //protected void arcImpl(float start, float stop,
  //                      float x, float y, float hr, float vr);

  //

  // also discussed using the modes..
  // but when rotating it's gonna make things totally bizarre.

  // rectMode(CORNER, CORNERS, CENTER, CENTER_DIAMETER
  // box(x, y, z, w, h, d) CORNER
  // box(x, y, z, x2, y2, z2) CORNERS
  // box(x, y, z, w, h, d) CENTER (but centered around x, y, z)
  // box(x, y, z, xr, yr, zr) CENTER_RADIUS

  // sphere(x, y, z, r) CENTER
  // sphere(x, y, z, r) CORNER draws the ellipse starting int the corner

  public void box(float size);

  public void box(float w, float h, float d);

  public void sphereDetail(int res);

  public void sphere(float r);

  //public void sphere(float x, float y, float z, float r);

  //

  public float bezierPoint(float a, float b, float c, float d,
                           float t);

  public float bezierTangent(float a, float b, float c, float d,
                             float t);

  public void bezier(float x1, float y1,
                     float x2, float y2,
                     float x3, float y3,
                     float x4, float y4);

  public void bezier(float x1, float y1, float z1,
                     float x2, float y2, float z2,
                     float x3, float y3, float z3,
                     float x4, float y4, float z4);

  public void bezierDetail(int detail);

  public void curveDetail(int detail);

  public void curveTightness(float tightness);

  public float curvePoint(float a, float b, float c, float d,
                          float t);

  public float curveTangent(float a, float b, float c, float d,
                            float t);

  public void curve(float x1, float y1,
                    float x2, float y2,
                    float x3, float y3,
                    float x4, float y4);

  public void curve(float x1, float y1, float z1,
                    float x2, float y2, float z2,
                    float x3, float y3, float z3,
                    float x4, float y4, float z4);
  //

  public void image(PImage image, float x1, float y1);

  public void image(PImage image,
                    float x1, float y1, float x2, float y2);

  public void image(PImage image,
                    float x1, float y1, float x2, float y2,
                    int u1, int v1, int u2, int v2);

  //protected void imageImpl(PImage image,
  //               float x1, float y1, float x2, float y2,
  //               int u1, int v1, int u2, int v2);

  //

  public void textAlign(int alignment);

  public float textAscent();

  public float textDescent();

  public void textFont(PFont which);

  public void textFont(PFont which, float size);

  public void textSize(float size);

  public void textLeading(float leading);

  public void textMode(int mode);

  public float textWidth(char c);

  public float textWidth(String s);

  public void text(char c, float x, float y);

  public void text(char c, float x, float y, float z);

  public void text(String s, float x, float y);

  public void text(String s, float x, float y, float z);

  public void text(String s, float x, float y, float w, float h);

  public void text(String s, float x1, float y1, float x2, float y2, float z);

  public void text(int num, float x, float y);

  public void text(int num, float x, float y, float z);

  public void text(float num, float x, float y);

  public void text(float num, float x, float y, float z);

  //

  public void translate(float tx, float ty);

  public void translate(float tx, float ty, float tz);

  public void angleMode(int mode);

  public void rotate(float angle);

  public void rotateX(float angle);

  public void rotateY(float angle);

  public void rotateZ(float angle);

  public void rotate(float angle, float v0, float v1, float v2);

  public void scale(float s);

  public void scale(float sx, float sy);

  public void scale(float sx, float sy, float sz);

  //

  public void push();

  public void pop();

  public void resetMatrix();

  public void applyMatrix(float n00, float n01, float n02,
                          float n10, float n11, float n12);

  public void applyMatrix(float n00, float n01, float n02, float n03,
                          float n10, float n11, float n12, float n13,
                          float n20, float n21, float n22, float n23,
                          float n30, float n31, float n32, float n33);

  public void printMatrix();

  //

  public void cameraMode(int mode);

  public void beginCamera();

  public void endCamera();

  public void ortho(float left, float right,
                    float bottom, float top,
                    float near, float far);

  public void perspective(float fovy, float aspect, float zNear, float zFar);

  public void frustum(float left, float right, float bottom,
                      float top, float znear, float zfar);

  public void lookat(float eyeX, float eyeY, float eyeZ,
                     float centerX, float centerY, float centerZ,
                     float upX, float upY, float upZ);

  public void printCamera();

  public void printProjection();

  //

  public float screenX(float x, float y);

  public float screenY(float x, float y);

  public float screenX(float x, float y, float z);

  public float screenY(float x, float y, float z);

  public float screenZ(float x, float y, float z);

  public float objectX(float x, float y, float z);

  public float objectY(float x, float y, float z);

  public float objectZ(float x, float y, float z);

  //

  public void lights();

  public PLight createAmbient();

  public PLight createDiffuse();

  public PLight createSpecular();

  public PLight createSpotlight();

  public void light(myLight);


  public void noLights();

  public void light(int num, float x, float y, float z,
                    float red, float green, float blue);

  public void lightEnable(int num);

  public void lightDisable(int num);

  public void lightPosition(int num, float x, float y, float z);

  public void lightAmbient(int num, float x, float y, float z);

  public void lightDiffuse(int num, float x, float y, float z);

  public void lightSpecular(int num, float x, float y, float z);

  public void lightDirection(int num, float x, float y, float z);

  public void lightFalloff(int num, float constant, float linear, float quadratic);

  public void lightSpotAngle(int num, float spotAngle);

  public void lightSpotConcentration(int num, float concentration);

  //

  public void colorMode(int mode);

  public void colorMode(int mode, float max);

  public void colorMode(int mode,
                        float maxX, float maxY, float maxZ);

  public void colorMode(int mode,
                        float maxX, float maxY, float maxZ, float maxA);

  public void noTint();

  public void tint(int rgb);

  public void tint(float gray);

  public void tint(float gray, float alpha);

  public void tint(float x, float y, float z);

  public void tint(float x, float y, float z, float a);

  public void noFill();

  public void fill(int rgb);

  public void fill(float gray);

  public void fill(float gray, float alpha);

  public void fill(float x, float y, float z);

  public void fill(float x, float y, float z, float a);

  public void strokeWeight(float weight);

  public void strokeJoin(int join);

  public void strokeCap(int cap);

  public void noStroke();

  public void stroke(int rgb);

  public void stroke(float gray);

  public void stroke(float gray, float alpha);

  public void stroke(float x, float y, float z);

  public void stroke(float x, float y, float z, float a);

  public void background(int rgb);

  public void background(float gray);

  public void background(float x, float y, float z);

  public void background(PImage image);

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

  //

  // now handled by PApplet
  //public void loadPixels();
  //public void updatePixels();
  //public void updatePixels(int x, int y, int c, int d);

  //

  public int get(int x, int y);

  public PImage get(int x, int y, int c, int d);

  public PImage get();

  public void set(int x, int y, int argb);

  public void set(int x, int y, PImage image);

  //

  public void mask(int alpha[]);

  public void mask(PImage alpha);

  public void filter(int kind);

  public void filter(int kind, float param);

  //

  //public void copy(PImage src, int dx, int dy);  // set instead

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

  public void save(String filename);

  public void postSetup();

  public void diffuse(int rgb);

  public void diffuse(float gray);


  public void diffuse(float gray, float alpha);


  public void diffuse(float x, float y, float z);


  public void diffuse(float x, float y, float z, float a);

//////////////////////////////////////////////////////////////

  public void ambient(int rgb);

  public void ambient(float gray);

  public void ambient(float x, float y, float z);

//////////////////////////////////////////////////////////////

  public void specular(int rgb);

  public void specular(float gray);

  public void specular(float gray, float alpha);

  public void specular(float x, float y, float z);

  public void specular(float x, float y, float z, float a);

  public void shininess(float shine);

  public void emissive(int rgb);

  public void emissive(float gray);

  public void emissive(float x, float y, float z);

//////////////////////////////////////////////////////////////

  public int createAmbientLight(int rgb);

  public int createAmbientLight(float gray);

  public int createAmbientLight(float lr, float lg, float lb);

  public int createDirectionalLight(int rgb, float nx, float ny, float nz);

  public int createDirectionalLight(float gray, float nx, float ny, float nz);

  public int createDirectionalLight(float r, float g, float b, float nx, float ny, float nz);

  public int createPointLight(int rgb, float x, float y, float z);

  public int createPointLight(float gray, float x, float y, float z);

  public int createPointLight(float lr, float lg, float lb, float x, float y, float z);

  public int createSpotLight(int rgb, float x, float y, float z, float nx, float ny, float nz, float angle);

  public int createSpotLight(float gray, float x, float y, float z, float nx, float ny, float nz, float angle);

  public int createSpotLight(float lr, float lg, float lb, float x, float y, float z, float nx, float ny, float nz, float angle);
}

*/
package processing.core;



// this file is auto-generated. no touchy-touchy.

public interface PMethods {

  public void modified();

  public void alpha(int alpha[]);

  public void alpha(PImage alpha);

  public void filter(int kind);

  public void filter(int kind, float param);

  public int get(int x, int y);

  public PImage get(int x, int y, int w, int h);

  public void set(int x, int y, int c);

  public void copy(PImage src, int dx, int dy);

  public void copy(int sx1, int sy1, int sx2, int sy2,
                   int dx1, int dy1, int dx2, int dy2);

  public void copy(PImage src, int sx1, int sy1, int sx2, int sy2,
                   int dx1, int dy1, int dx2, int dy2);

  public void blend(PImage src, int sx, int sy, int dx, int dy, int mode);

  public void blend(int sx, int sy, int dx, int dy, int mode);

  public void blend(int sx1, int sy1, int sx2, int sy2,
                    int dx1, int dy1, int dx2, int dy2, int mode);

  public void blend(PImage src, int sx1, int sy1, int sx2, int sy2,
                    int dx1, int dy1, int dx2, int dy2, int mode);

  public PImage get();

  public void save(String filename);

  public void smooth();

  public void noSmooth();

  public void imageMode(int mode);

  public void defaults();

  public void beginFrame();

  public void endFrame();

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

  public void bezierVertex(float x, float y);

  public void bezierVertex(float x, float y, float z);

  public void curveVertex(float x, float y);

  public void curveVertex(float x, float y, float z);

  public void endShape();

  public void point(float x, float y);

  public void point(float x, float y, float z);

  public void line(float x1, float y1, float x2, float y2);

  public void line(float x1, float y1, float z1,
                   float x2, float y2, float z2);

  public void triangle(float x1, float y1, float x2, float y2,
                       float x3, float y3);

  public void rectMode(int mode);

  public void rect(float x1, float y1, float x2, float y2);

  public void quad(float x1, float y1, float x2, float y2,
                   float x3, float y3, float x4, float y4);

  public void image(PImage image, float x1, float y1);

  public void image(PImage image,
                    float x1, float y1, float x2, float y2);

  public void image(PImage image,
                    float x1, float y1, float x2, float y2,
                    float u1, float v1, float u2, float v2);

  public void cache(PImage image);

  public void cache(PImage images[]);

  public void arcMode(int mode);

  public void arc(float start, float stop,
                  float x, float y, float radius);

  public void arc(float start, float stop,
                  float x, float y, float hr, float vr);

  public void ellipseMode(int mode);

  public void ellipse(float x, float y, float hradius, float vradius);

  public void circle(float x, float y, float radius);

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

  public void box(float size);

  public void box(float w, float h, float d);

  public void sphereDetail(int res);

  public void sphere(float r);

  public void sphere(float x, float y, float z, float r);

  public void textFont(PFont which);

  public void textSize(float size);

  public void textFont(PFont which, float size);

  public void textLeading(float leading);

  public void textMode(int mode);

  public void textSpace(int space);

  public void text(char c, float x, float y);

  public void text(char c, float x, float y, float z);

  public void text(String s, float x, float y);

  public void text(String s, float x, float y, float z);

  public void text(String s, float x, float y, float w, float h);

  public void text(String s, float x1, float y1, float z, float x2, float y2);

  public void text(int num, float x, float y);

  public void text(int num, float x, float y, float z);

  public void text(float num, float x, float y);

  public void text(float num, float x, float y, float z);

  public void angleMode(int mode);

  public void translate(float tx, float ty);

  public void translate(float tx, float ty, float tz);

  public void rotateX(float angle);

  public void rotateY(float angle);

  public void rotate(float angle);

  public void rotateZ(float angle);

  public void rotate(float angle, float v0, float v1, float v2);

  public void scale(float s);

  public void scale(float sx, float sy);

  public void scale(float x, float y, float z);

  public void transform(float n00, float n01, float n02, float n03,
                        float n10, float n11, float n12, float n13,
                        float n20, float n21, float n22, float n23,
                        float n30, float n31, float n32, float n33);

  public void push();

  public void pop();

  public void resetMatrix();

  public void applyMatrix(float n00, float n01, float n02, float n03,
                          float n10, float n11, float n12, float n13,
                          float n20, float n21, float n22, float n23,
                          float n30, float n31, float n32, float n33);

  public void applyMatrix(float n00, float n01, float n02,
                          float n10, float n11, float n12);

  public void printMatrix();

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

  public float screenX(float x, float y);

  public float screenY(float x, float y);

  public float screenX(float x, float y, float z);

  public float screenY(float x, float y, float z);

  public float screenZ(float x, float y, float z);

  public float objectX(float x, float y, float z);

  public float objectY(float x, float y, float z);

  public float objectZ(float x, float y, float z);

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

  public void depth();

  public void noDepth();

  public void lights();

  public void noLights();

  public void light(int num, float x, float y, float z,
                    float red, float green, float blue);

  public void lightEnable(int num);

  public void lightDisable(int num);

  public void lightPosition(int num, float x, float y, float z);

  public void lightAmbient(int num, float x, float y, float z);

  public void lightDiffuse(int num, float x, float y, float z);

  public void lightSpecular(int num, float x, float y, float z);

  public void hint(int which);

  public void unhint(int which);
}

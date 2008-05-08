/**
 * Yellowtail
 * by Golan Levin (www.flong.com)
 * 
 * Click, drag, and release to create a kinetic gesture.
 * 
 * Yellowtail (1998-2000) is an interactive software system for the gestural 
 * creation and performance of real-time abstract animation. Yellowtail repeats 
 * a user's strokes end-over-end, enabling simultaneous specification of a 
 * line's shape and quality of movement. Each line repeats according to its 
 * own period, producing an ever-changing and responsive display of lively, 
 * worm-like textures.
 * 
 */


import processing.opengl.*;

int myW;
int myH;
boolean theMouseDown = false;

Gesture gestureArray[];
final int nGestures = 36;  // Number of gestures
final int minMove = 3;     // Minimum travel for a new point
int currentGestureID;

Polygon tempP;
int tmpXp[];
int tmpYp[];

void setup()
{
  size(640, 480, OPENGL);
  myW = width;
  myH = height;
  background(0, 0, 0);
  noStroke();

  currentGestureID = -1;
  gestureArray = new Gesture[nGestures];
  for (int i=0; i<nGestures; i++){
    gestureArray[i] = new Gesture(myW, myH);
  }
  clearGestures();

}

void draw()
{
  background(0);

  updateGeometry();
  fill(255, 255, 245);
  for (int G=0; G<nGestures; G++){
    renderGesture(gestureArray[G],myW,myH);
  }
}

void mousePressed()
{
  theMouseDown = true;
  currentGestureID = (currentGestureID+1)%nGestures;
  Gesture G = gestureArray[currentGestureID];
  G.clear();
  G.clearPolys();
  G.addPoint(mouseX, mouseY);
}

void mouseDragged(){
  theMouseDown = true;
  if (currentGestureID >= 0){
    Gesture G = gestureArray[currentGestureID];
    if (G.distToLast(mouseX, mouseY) > minMove) {
      G.addPoint(mouseX, mouseY);
      G.smooth();
      G.compile();
    }
  }
}

void mouseMoved (Event evt, int x, int y){
  theMouseDown = false;
}


void mouseReleased(){
  theMouseDown = false;
}


void keyPressed(){
  switch (key){
    case '+':
    case '=':
    if (currentGestureID >= 0){
      float th = gestureArray[currentGestureID].thickness;
      gestureArray[currentGestureID].thickness = Math.min(96, th+1);
      gestureArray[currentGestureID].compile();
    }
    break;
    case '-':
    if (currentGestureID >= 0){
      float th = gestureArray[currentGestureID].thickness;
      gestureArray[currentGestureID].thickness = Math.max(2, th-1);
      gestureArray[currentGestureID].compile();
    }
    break;

    case ' ': clearGestures();
    break;
  }

}


void renderGesture (Gesture gesture, int w, int h){
  if (gesture.exists){
    if (gesture.nPolys > 0){
      Polygon polygons[] = gesture.polygons;
      int crosses[] = gesture.crosses;

      int xpts[];
      int ypts[];
      Polygon p;
      int cr;

      beginShape(QUADS);
      int gnp = gesture.nPolys;
      for (int i=0; i<gnp; i++){

        p = polygons[i];
        xpts = p.xpoints;
        ypts = p.ypoints;

        vertex(xpts[0], ypts[0]);
        vertex(xpts[1], ypts[1]);
        vertex(xpts[2], ypts[2]);
        vertex(xpts[3], ypts[3]);

        if ((cr = crosses[i]) > 0){
          if ((cr & 3)>0){
            vertex(xpts[0]+w, ypts[0]);
            vertex(xpts[1]+w, ypts[1]);
            vertex(xpts[2]+w, ypts[2]);
            vertex(xpts[3]+w, ypts[3]);

            vertex(xpts[0]-w, ypts[0]);
            vertex(xpts[1]-w, ypts[1]);
            vertex(xpts[2]-w, ypts[2]);
            vertex(xpts[3]-w, ypts[3]);
          }
          if ((cr & 12)>0){
            vertex(xpts[0], ypts[0]+h);
            vertex(xpts[1], ypts[1]+h);
            vertex(xpts[2], ypts[2]+h);
            vertex(xpts[3], ypts[3]+h);

            vertex(xpts[0], ypts[0]-h);
            vertex(xpts[1], ypts[1]-h);
            vertex(xpts[2], ypts[2]-h);
            vertex(xpts[3], ypts[3]-h);
          }

          // I have knowingly retained the small flaw of not
          // completely dealing with the corner conditions
          // (the case in which both of the above are true).
        }
      }
      endShape();
    }
  }
}

private void updateGeometry(){
  Gesture J;
  for (int g=0; g<nGestures; g++){
    if ((J=gestureArray[g]).exists){
      if (g!=currentGestureID){
        advanceGesture(J);
      } else if (!theMouseDown){
        advanceGesture(J);
      }
    }
  }
}

void advanceGesture(Gesture gesture){
  // move a Gesture one step
  if (gesture.exists){ // check
    int nPts = gesture.nPoints;
    int nPts1 = nPts-1;
    Vec3f path[];
    float jx = gesture.jumpDx;
    float jy = gesture.jumpDy;

    if (nPts > 0){
      path = gesture.path;
      for (int i=nPts1; i>0; i--){
        path[i].x = path[i-1].x;
        path[i].y = path[i-1].y;
      }
      path[0].x = path[nPts1].x - jx;
      path[0].y = path[nPts1].y - jy;
      gesture.compile();
    }
  }
}

void clearGestures(){
  for (int i=0; i<nGestures; i++){
    gestureArray[i].clear();
  }
}


class Vec3f {
  float x;
  float y;
  float p; // pressure

  Vec3f() {
    set(0,0,0);
  }
  Vec3f(float ix, float iy, float ip) {
    set(ix, iy, ip);
  }

  void set(float ix, float iy, float ip){
    x = ix;
    y = iy;
    p = ip;
  }
}

class Gesture {

  private static final double twoPi = Math.PI*2.0f;
  private static final float  radToDeg = (float)(360.0f/(2.0f*Math.PI)); // 57.2957795131
  private static final float  damp = 5f;
  private static final float  dampInv = 1.0f/damp;
  private static final float  damp1 = damp -1;

  private int w;
  private int h;
  private int capacity;

  public Vec3f path[];
  public int crosses[];
  public Polygon polygons[];
  public int nPoints;
  public int nPolys;

  public float   jumpDx, jumpDy;
  public boolean exists;
  private static final float INIT_TH = 14;
  public float   thickness = INIT_TH;

  Gesture(int mw, int mh) {
    w = mw;
    h = mh;
    capacity = 600;
    path = new Vec3f[capacity];
    polygons = new Polygon[capacity];
    crosses  = new int[capacity];
    for (int i=0;i<capacity;i++){
      polygons[i] = new Polygon();
      polygons[i].npoints = 4;
      path[i] = new Vec3f();
      crosses[i] = 0;
    }
    nPoints = 0;
    nPolys = 0;

    exists = false;
    jumpDx = 0;
    jumpDy = 0;
  }

  public synchronized void clear(){
    nPoints = 0;
    exists = false;
    thickness = INIT_TH;
  }

  public synchronized void clearPolys(){
    nPolys = 0;
  }

  public synchronized void addPoint(float x, float y){
    //synchronized (path){
      if (nPoints >= capacity){
        // there are all sorts of possible solutions here,
        // but for abject simplicity, I don't do anything.
      } else {
        float v = distToLast(x, y);
        float p = getPressureFromVelocity(v);
        path[nPoints++].set(x,y,p);

        if (nPoints > 1) {
          exists = true;
          jumpDx = path[nPoints-1].x - path[0].x;
          jumpDy = path[nPoints-1].y - path[0].y;
        }
      }
    //}

  }

  private float getPressureFromVelocity(float v){
    final float scale = 18f;
    final float minP = 0.02f;
    final float oldP = (nPoints > 0) ? path[nPoints-1].p : 0;
    return  ((minP + Math.max(0, 1.0f - v/scale)) + (damp1*oldP))*dampInv;
  }

  private void setPressures(){
    // pressures vary from 0...1
    float pressure;
    Vec3f tmp;
    double t = 0;

    double u = 1.0f/(double)(nPoints - 1)*twoPi;
    for (int i=0; i<nPoints; i++){
      pressure = (float) Math.sqrt((1.0f - Math.cos(t))*0.5f);
      path[i].p = pressure;
      t += u;
    }
  }

  public float distToLast(float ix, float iy){
    if (nPoints > 0){
      Vec3f v = path[nPoints-1];
      float dx = v.x - ix;
      float dy = v.y - iy;
      return (float) Math.sqrt(dx*dx + dy*dy);
    } else {
      return 30;
    }
  }

  public void compile(){
    // compute the polygons from the path of Vec3f's
    if (exists){
      clearPolys();

      Vec3f p0, p1, p2;
      float radius0, radius1;
      float ax, bx, cx, dx;
      float ay, by, cy, dy;
      int   axi, bxi, cxi, dxi, axip, axid;
      int   ayi, byi, cyi, dyi, ayip, ayid;
      float p1x, p1y;
      float dx01, dy01, hp01, si01, co01;
      float dx02, dy02, hp02, si02, co02;
      float dx13, dy13, hp13, si13, co13;
      float taper = 1.0f;

      final int  nPathPoints = nPoints - 1;
      final int  lastPolyIndex = nPathPoints - 1;
      final float npm1finv =  1.0f/(float)(Math.max(1, nPathPoints - 1));

      // handle the first point
      p0 = path[0];
      p1 = path[1];
      radius0 = p0.p*thickness;
      dx01 = p1.x - p0.x;
      dy01 = p1.y - p0.y;
      hp01 = (float) Math.sqrt(dx01*dx01 + dy01*dy01);
      if (hp01 == 0) {
        hp02 = 0.0001f;
      }
      co01 = radius0 * dx01 / hp01;
      si01 = radius0 * dy01 / hp01;
      ax = p0.x - si01; ay = p0.y + co01;
      bx = p0.x + si01; by = p0.y - co01;

      int xpts[];
      int ypts[];

      final int LC = 20;
      final int RC = w-LC;
      final int TC = 20;
      final int BC = h-TC;
      final float mint = 0.618f;
      final double tapow = 0.4f;

      // handle the middle points
      int i=1;
      Polygon apoly;
      for (i=1; i<nPathPoints; i++){
        taper = (float)(Math.pow((lastPolyIndex-i)*npm1finv,tapow));

        p0 = path[i-1];
        p1 = path[i  ];
        p2 = path[i+1];
        p1x = p1.x;
        p1y = p1.y;
        radius1 = Math.max(mint,taper*p1.p*thickness);

        // assumes all segments are roughly the same length...
        dx02 = p2.x - p0.x;
        dy02 = p2.y - p0.y;
        hp02 = (float) Math.sqrt(dx02*dx02 + dy02*dy02);
        if (hp02 != 0) {
          hp02 = radius1/hp02;
        }
        co02 = dx02 * hp02;
        si02 = dy02 * hp02;

        // translate the integer coordinates to the viewing rectangle
        axi = axip = (int)ax;
        ayi = ayip = (int)ay;
        axi=(axi<0)?(w-((-axi)%w)):axi%w;
        axid = axi-axip;
        ayi=(ayi<0)?(h-((-ayi)%h)):ayi%h;
        ayid = ayi-ayip;

        // set the vertices of the polygon
        apoly = polygons[nPolys++];
        xpts = apoly.xpoints;
        ypts = apoly.ypoints;
        xpts[0] = axi = axid + axip;
        xpts[1] = bxi = axid + (int) bx;
        xpts[2] = cxi = axid + (int)(cx = p1x + si02);
        xpts[3] = dxi = axid + (int)(dx = p1x - si02);
        ypts[0] = ayi = ayid + ayip;
        ypts[1] = byi = ayid + (int) by;
        ypts[2] = cyi = ayid + (int)(cy = p1y - co02);
        ypts[3] = dyi = ayid + (int)(dy = p1y + co02);

        // keep a record of where we cross the edge of the screen
        crosses[i] = 0;
      if ((axi<=LC)||(bxi<=LC)||(cxi<=LC)||(dxi<=LC)){ crosses[i]|=1;}
      if ((axi>=RC)||(bxi>=RC)||(cxi>=RC)||(dxi>=RC)){ crosses[i]|=2;}
      if ((ayi<=TC)||(byi<=TC)||(cyi<=TC)||(dyi<=TC)){ crosses[i]|=4;}
      if ((ayi>=BC)||(byi>=BC)||(cyi>=BC)||(dyi>=BC)){ crosses[i]|=8;}

        //swap data for next time
        ax = dx; ay = dy;
        bx = cx; by = cy;
      }

      // handle the last point
      p2 = path[nPathPoints];
      apoly = polygons[nPolys++];
      xpts = apoly.xpoints;
      ypts = apoly.ypoints;

      xpts[0] = (int)ax;
      xpts[1] = (int)bx;
      xpts[2] = (int)(p2.x);
      xpts[3] = (int)(p2.x);

      ypts[0] = (int)ay;
      ypts[1] = (int)by;
      ypts[2] = (int)(p2.y);
      ypts[3] = (int)(p2.y);

    }
  }

  public synchronized void smooth(){
    // average neighboring points

    final float weight = 18f;
    final float scale  = 1.0f/(weight + 2f);
    int nPointsMinusTwo = nPoints - 2;
    Vec3f lower, upper, center;

    for (int i=1; i<nPointsMinusTwo; i++){
      lower = path[i-1];
      center = path[i];
      upper = path[i+1];

      center.x = (lower.x + weight*center.x + upper.x)*scale;
      center.y = (lower.y + weight*center.y + upper.y)*scale;
    }
  }

}


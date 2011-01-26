// Electric by Andor Salga
// Ported from Processing.JS by Andres Colubri. Original 
// code available at
// http://matrix.senecac.on.ca/~asalga/pjswebide/index.php

float r = 0;
int bolts = 0;
float lightningOpacity = 255;
int level = -1;
int startTime = 0;

ArrayList LBolts;

void setup() {
  size(480, 800, A3D);
  orientation(PORTRAIT);

  LBolts = new ArrayList();

  for (int i=0; i < 12; i++) {
    LBolts.add(new Lightning());
  }

  startTime = millis();
}

void update(int deltaTime) {
  level = 0;
  for (int i=0; i < 12; i++) {
    Lightning l = (Lightning)LBolts.get(i);
    l.update(deltaTime);
  }
}

void draw() {
  background( abs(250* sin(frameCount/100)), 
  abs(100* cos(frameCount/50)), abs(150* cos(frameCount/150)));

  translate(width/2, height/2, 0);

  rotate(r+=0.005);
  rotateX(-r);
  rotateZ(r);

  update(millis()-startTime);

  for (int i=0; i < 12; i++) {
    Lightning l = (Lightning)LBolts.get(i);
    l.draw();
  }
}

class PVector3D
{
  float x, y, z;

  PVector3D() {
    set(0, 0, 0);
  }

  PVector3D(float x, float y, float z) {
    set(x, y, z);
  }

  void set(float x, float y, float z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }
}

PVector3D add(PVector3D v1, PVector3D v2) {
  return new PVector3D(v1.x + v2.x, v1.y + v2.y, v1.z + v2.z);
}

PVector3D sub(PVector3D initialPoint, PVector3D terminalPoint) {
  return new PVector3D( 
    terminalPoint.x - initialPoint.x, 
    terminalPoint.y - initialPoint.y, 
    terminalPoint.z - initialPoint.z);
}

float len(PVector3D v) {
  return sqrt(v.x*v.x + v.y*v.y + v.z*v.z);
}

PVector3D scale(PVector3D v, float s) {
  return new PVector3D(v.x*s, v.y*s, v.z*s);
}

class LightningBolt {
  PVector3D start;
  PVector3D end;

  ArrayList nodes = new ArrayList();
  ArrayList onodes = new ArrayList();

  int deviation;
  int bounds;
  int numNodes;
  boolean isInit;

  LightningBolt() {
    bounds = 0;
    isInit = false;
    start = new PVector3D();
    end = new PVector3D();
    numNodes = 0;
    deviation = 0;
    nodes = new ArrayList();
    onodes = new ArrayList();
  }

  void init() { 
    PVector3D startToEnd = sub(start, end);
    float lineLen = len(startToEnd);

    print(startToEnd);

    float nodeSeparation = lineLen / numNodes;

    for (int i = 1; i <= numNodes; i++) {

      PVector3D p = add(scale(startToEnd, i/(float)numNodes), start);
      PVector3D o = add(scale(startToEnd, i/(float)numNodes), start);

      nodes.add(p);
      onodes.add(o);
    }
    isInit = true;
  }

  void setDeviation(int d) {
    deviation = d;
  }

  void setNumNodes(int n) {
    numNodes = n;
  }

  void setBounds(int b) { 
    bounds = b;
  }

  void setStartPoint(PVector3D p) {
    start = p;
  }

  void setEndPoint(PVector3D p) {
    end = p;
  }

  void update(int deltaTime) {
    if (isInit == false) {
      init();
    }
    else {
      for (int i=1; i < nodes.size()-1; i++) {
        PVector3D p = (PVector3D)nodes.get(i);
        PVector3D op = (PVector3D)onodes.get(i);

        float rx = random(-deviation, deviation);
        if ( abs((rx + p.x - op.x)) < bounds) {
          p.x += rx;
          nodes.set(i, p);
        }

        float ry = random(-deviation, deviation);
        if ( abs((ry + p.y - op.y)) < bounds) {
          p.y += ry;
          nodes.set(i, p);
        }

        float rz = random(-deviation, deviation);
        if ( abs((rz + p.z - op.z)) < bounds) {
          p.z += rz;
          nodes.set(i, p);
        }
      }
    }
  }

  void draw()
  {
    PVector3D lastNode = start;
    int b = 250;
    for (int i = 0; i < nodes.size(); i++) {
      PVector3D node = (PVector3D)nodes.get(i);

      strokeWeight(1);
      stroke(10, 10, 100, lightningOpacity);
      line(lastNode.x, lastNode.y, lastNode.z, node.x, node.y, node.z);
      lastNode = node;
    }

    lastNode = start;
    for (int i = 0; i < nodes.size(); i++) {
      PVector3D node = (PVector3D)nodes.get(i);

      strokeWeight(1);
      stroke(255, 150);
      lastNode = node;
    }
  }
}

class Lightning {
  LightningBolt bolt;

  Lightning() {
    bolts++;
    bolt = new LightningBolt();
    bolt.setDeviation(13);
    bolt.setBounds(10);
    bolt.setNumNodes(7);

    if ( bolts == 1) {
      bolt.setEndPoint(new PVector3D(-100, 100, -100));
      bolt.setStartPoint(new PVector3D(100, 100, -100));
    }
    else if ( bolts == 2) {
      bolt.setEndPoint(new PVector3D(-100, 100, 100));
      bolt.setStartPoint(new PVector3D(100, 100, 100));
    }
    else if ( bolts == 3) {
      bolt.setEndPoint(new PVector3D(-100, 100, -100));
      bolt.setStartPoint(new PVector3D(-100, 100, 100));
    }
    else if ( bolts == 4) {
      bolt.setEndPoint(new PVector3D(100, 100, -100));
      bolt.setStartPoint(new PVector3D(100, 100, 100));
    }
    else if (bolts == 5) {
      bolt.setEndPoint(new PVector3D(100, -100, -100));
      bolt.setStartPoint(new PVector3D(100, -100, 100));
    }
    else if (bolts == 6) {
      bolt.setEndPoint(new PVector3D(-100, -100, 100));
      bolt.setStartPoint(new PVector3D(100, -100, 100));
    }
    else if (bolts == 7) {
      bolt.setEndPoint(new PVector3D(-100, -100, -100));
      bolt.setStartPoint(new PVector3D(-100, -100, 100));
    }
    else if (bolts == 8) {
      bolt.setEndPoint(new PVector3D(-100, -100, -100));
      bolt.setStartPoint(new PVector3D(100, -100, -100));
    }
    else if (bolts == 9) {
      bolt.setEndPoint(new PVector3D(100, 100, 100));
      bolt.setStartPoint(new PVector3D(100, -100, 100));
    }
    else if (bolts == 10) {
      bolt.setEndPoint(new PVector3D(-100, 100, -100));
      bolt.setStartPoint(new PVector3D(-100, -100, -100));
    }
    else if (bolts == 11) {
      bolt.setEndPoint(new PVector3D(100, 100, -100));
      bolt.setStartPoint(new PVector3D(100, -100, -100));
    }
    else if (bolts == 12) {
      bolt.setEndPoint(new PVector3D(-100, 100, 100));
      bolt.setStartPoint(new PVector3D(-100, -100, 100));
    }
    bolt.init();
  }

  void update(int deltaTime) {
    bolt.update(deltaTime);
  }

  void draw() {
    bolt.draw();
  }
}


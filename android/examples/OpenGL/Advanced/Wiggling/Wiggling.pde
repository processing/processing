// Press 'w' to start wiggling, space to restore
// original positions.

PShape cube;
float cubeSize = 200;
float circleRad = 50;
int circleRes = 20;
float noiseMag = 1;

boolean wiggling = false;

void setup() {
  size(400, 400, P3D);

  createCube();
}

void draw() {
  background(0);

  translate(width/2, height/2);
  rotateX(frameCount * 0.01f);
  rotateY(frameCount * 0.01f);

  shape(cube);

  if (wiggling) {
    PVector pos = null;
    for (int i = 0; i < cube.getChildCount(); i++) {
      PShape face = cube.getChild(i);
      for (int j = 0; j < face.getVertexCount(); j++) {
        pos = face.getVertex(j, pos);
        pos.x += random(-noiseMag/2, +noiseMag/2);
        pos.y += random(-noiseMag/2, +noiseMag/2);
        pos.z += random(-noiseMag/2, +noiseMag/2);
        face.setVertex(j, pos.x, pos.y, pos.z);
      }
    }
  }

  println(frameRate);
}

public void keyPressed() {
  if (key == 'w') {
    wiggling = !wiggling;
  } else if (key == ' ') {
    restoreCube();
  } else if (key == '1') {
    cube.strokeWeight(1);
  } else if (key == '2') {
    cube.strokeWeight(5);
  } else if (key == '3') {
    cube.strokeWeight(10);
  }
}

void createCube() {
  cube = createShape(GROUP);  

  PShape face;

  // Front face         
  face = createShape(POLYGON);
  face.stroke(255, 0, 0);
  face.fill(255);
  face.beginContour();
  face.vertex(-cubeSize/2, -cubeSize/2, +cubeSize/2);
  face.vertex(+cubeSize/2, -cubeSize/2, +cubeSize/2);
  face.vertex(+cubeSize/2, +cubeSize/2, +cubeSize/2);
  face.vertex(-cubeSize/2, +cubeSize/2, +cubeSize/2);
  face.endContour();
  face.beginContour();
  for (int i = 0; i < circleRes; i++) {
    float angle = TWO_PI * i / circleRes;
    float x = circleRad * sin(angle);
    float y = circleRad * cos(angle);
    float z = +cubeSize/2;
    face.vertex(x, y, z);
  }
  face.endContour();
  face.end(CLOSE);
  cube.addChild(face);

  // Back face
  face = createShape(POLYGON);
  face.stroke(255, 0, 0);
  face.fill(255);
  face.beginContour();
  face.vertex(+cubeSize/2, -cubeSize/2, -cubeSize/2);
  face.vertex(-cubeSize/2, -cubeSize/2, -cubeSize/2);
  face.vertex(-cubeSize/2, +cubeSize/2, -cubeSize/2);
  face.vertex(+cubeSize/2, +cubeSize/2, -cubeSize/2);
  face.endContour();
  face.beginContour();
  for (int i = 0; i < circleRes; i++) {
    float angle = TWO_PI * i / circleRes;
    float x = circleRad * sin(angle);
    float y = circleRad * cos(angle);
    float z = -cubeSize/2;
    face.vertex(x, y, z);
  }
  face.endContour();
  face.end(CLOSE);
  cube.addChild(face);

  // Right face
  face = createShape(POLYGON);
  face.stroke(255, 0, 0);
  face.fill(255);
  face.beginContour();
  face.vertex(+cubeSize/2, -cubeSize/2, +cubeSize/2);
  face.vertex(+cubeSize/2, -cubeSize/2, -cubeSize/2);
  face.vertex(+cubeSize/2, +cubeSize/2, -cubeSize/2);
  face.vertex(+cubeSize/2, +cubeSize/2, +cubeSize/2);
  face.endContour();
  face.beginContour();
  for (int i = 0; i < circleRes; i++) {
    float angle = TWO_PI * i / circleRes;
    float x = +cubeSize/2;
    float y = circleRad * sin(angle);
    float z = circleRad * cos(angle);
    face.vertex(x, y, z);
  }
  face.endContour();
  face.end(CLOSE);
  cube.addChild(face);

  // Left face
  face = createShape(POLYGON);
  face.stroke(255, 0, 0);
  face.fill(255);
  face.beginContour();
  face.vertex(-cubeSize/2, -cubeSize/2, -cubeSize/2);
  face.vertex(-cubeSize/2, -cubeSize/2, +cubeSize/2);
  face.vertex(-cubeSize/2, +cubeSize/2, +cubeSize/2);
  face.vertex(-cubeSize/2, +cubeSize/2, -cubeSize/2);
  face.endContour();
  face.beginContour();
  for (int i = 0; i < circleRes; i++) {
    float angle = TWO_PI * i / circleRes;
    float x = -cubeSize/2;
    float y = circleRad * sin(angle);
    float z = circleRad * cos(angle);
    face.vertex(x, y, z);
  }
  face.endContour();
  face.end(CLOSE);
  cube.addChild(face);

  // Top face
  face = createShape(POLYGON);
  face.stroke(255, 0, 0);
  face.fill(255);
  face.beginContour();
  face.vertex(-cubeSize/2, +cubeSize/2, +cubeSize/2);
  face.vertex(+cubeSize/2, +cubeSize/2, +cubeSize/2);
  face.vertex(+cubeSize/2, +cubeSize/2, -cubeSize/2);
  face.vertex(-cubeSize/2, +cubeSize/2, -cubeSize/2);
  face.endContour();
  face.beginContour();
  for (int i = 0; i < circleRes; i++) {
    float angle = TWO_PI * i / circleRes;
    float x = circleRad * sin(angle);
    float y = +cubeSize/2;
    float z = circleRad * cos(angle);
    face.vertex(x, y, z);
  }
  face.endContour();
  face.end(CLOSE);
  cube.addChild(face);     

  // Bottom face
  face = createShape(POLYGON);
  face.stroke(255, 0, 0);
  face.fill(255);
  face.beginContour();
  face.vertex(+cubeSize/2, -cubeSize/2, +cubeSize/2);
  face.vertex(-cubeSize/2, -cubeSize/2, +cubeSize/2);
  face.vertex(-cubeSize/2, -cubeSize/2, -cubeSize/2);
  face.vertex(+cubeSize/2, -cubeSize/2, -cubeSize/2);
  face.endContour();
  face.beginContour();
  for (int i = 0; i < circleRes; i++) {
    float angle = TWO_PI * i / circleRes;
    float x = circleRad * sin(angle);
    float y = -cubeSize/2;
    float z = circleRad * cos(angle);
    face.vertex(x, y, z);
  }
  face.endContour();
  face.end(CLOSE);
  cube.addChild(face);
}

void restoreCube() {
  PShape face;

  // Front face
  face = cube.getChild(0);
  face.setVertex(0, -cubeSize/2, -cubeSize/2, +cubeSize/2);
  face.setVertex(1, +cubeSize/2, -cubeSize/2, +cubeSize/2);
  face.setVertex(2, +cubeSize/2, +cubeSize/2, +cubeSize/2);
  face.setVertex(3, -cubeSize/2, +cubeSize/2, +cubeSize/2);
  for (int i = 0; i < circleRes; i++) {
    float angle = TWO_PI * i / circleRes;
    float x = circleRad * sin(angle);
    float y = circleRad * cos(angle);
    float z = +cubeSize/2;
    face.setVertex(4 + i, x, y, z);
  }

  // Back face
  face = cube.getChild(1);
  face.setVertex(0, +cubeSize/2, -cubeSize/2, -cubeSize/2);
  face.setVertex(1, -cubeSize/2, -cubeSize/2, -cubeSize/2);
  face.setVertex(2, -cubeSize/2, +cubeSize/2, -cubeSize/2);
  face.setVertex(3, +cubeSize/2, +cubeSize/2, -cubeSize/2);
  for (int i = 0; i < circleRes; i++) {
    float angle = TWO_PI * i / circleRes;
    float x = circleRad * sin(angle);
    float y = circleRad * cos(angle);
    float z = -cubeSize/2;
    face.setVertex(4 + i, x, y, z);
  }

  // Right face
  face = cube.getChild(2);
  face.setVertex(0, +cubeSize/2, -cubeSize/2, +cubeSize/2);
  face.setVertex(1, +cubeSize/2, -cubeSize/2, -cubeSize/2);
  face.setVertex(2, +cubeSize/2, +cubeSize/2, -cubeSize/2);
  face.setVertex(3, +cubeSize/2, +cubeSize/2, +cubeSize/2);
  for (int i = 0; i < circleRes; i++) {
    float angle = TWO_PI * i / circleRes;
    float x = +cubeSize/2;
    float y = circleRad * sin(angle);
    float z = circleRad * cos(angle);
    face.setVertex(4 + i, x, y, z);
  }

  // Left face
  face = cube.getChild(3);
  face.setVertex(0, -cubeSize/2, -cubeSize/2, -cubeSize/2);
  face.setVertex(1, -cubeSize/2, -cubeSize/2, +cubeSize/2);
  face.setVertex(2, -cubeSize/2, +cubeSize/2, +cubeSize/2);
  face.setVertex(3, -cubeSize/2, +cubeSize/2, -cubeSize/2);
  for (int i = 0; i < circleRes; i++) {
    float angle = TWO_PI * i / circleRes;
    float x = -cubeSize/2;
    float y = circleRad * sin(angle);
    float z = circleRad * cos(angle);
    face.setVertex(4 + i, x, y, z);
  }    

  // Top face
  face = cube.getChild(4);
  face.setVertex(0, -cubeSize/2, +cubeSize/2, +cubeSize/2);
  face.setVertex(1, +cubeSize/2, +cubeSize/2, +cubeSize/2);
  face.setVertex(2, +cubeSize/2, +cubeSize/2, -cubeSize/2);
  face.setVertex(3, -cubeSize/2, +cubeSize/2, -cubeSize/2);
  for (int i = 0; i < circleRes; i++) {
    float angle = TWO_PI * i / circleRes;
    float x = circleRad * sin(angle);
    float y = +cubeSize/2;
    float z = circleRad * cos(angle);
    face.setVertex(4 + i, x, y, z);
  }    

  // Bottom face
  face = cube.getChild(5);
  face.setVertex(0, +cubeSize/2, -cubeSize/2, +cubeSize/2);
  face.setVertex(1, -cubeSize/2, -cubeSize/2, +cubeSize/2);
  face.setVertex(2, -cubeSize/2, -cubeSize/2, -cubeSize/2);
  face.setVertex(3, +cubeSize/2, -cubeSize/2, -cubeSize/2);
  for (int i = 0; i < circleRes; i++) {
    float angle = TWO_PI * i / circleRes;
    float x = circleRad * sin(angle);
    float y = -cubeSize/2;
    float z = circleRad * cos(angle);
    face.setVertex(4 + i, x, y, z);
  }
}


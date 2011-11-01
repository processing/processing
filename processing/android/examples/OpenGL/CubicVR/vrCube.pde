// QTVR style cube.

class VRCube{
  int totalSides = 6;
  int dim;
  int cDim;
  PImage[] sides;
  PImage mainImage;

  VRCube (String sentFileName){
    noStroke();
    imageMode(CORNER);
    mainImage      = loadImage(sentFileName);
    dim            = mainImage.width/3;
    cDim           = dim/2;

    sides          = new PImage[totalSides];
    sides[0] = mainImage.get(dim,         0,    dim,dim);
    sides[1] = mainImage.get(0,         dim,    dim,dim);
    sides[2] = mainImage.get(dim,       dim,    dim,dim);
    sides[3] = mainImage.get(dim*2,     dim,    dim,dim);
    sides[4] = mainImage.get(dim,     dim*2,    dim,dim);
    sides[5] = mainImage.get(dim,     dim*3,    dim,dim);
  }
  
  void exist(){
    renderCube();
  }
  
  void renderCube(){
    pushMatrix();
    drawFace(sides[1]);
    rotateY(-HALF_PI);
    drawFace(sides[2]);
    rotateY(-HALF_PI);
    drawFace(sides[3]);
    rotateY(-HALF_PI);
    rotateZ(PI);
    drawFace(sides[5]);
    rotateX(HALF_PI);
    drawFace(sides[0]);
    rotateX(PI);
    drawFace(sides[4]);
    popMatrix();
  }

  void drawFace(PImage sentImage){
    pushMatrix();
    translate(0,0,-cDim+.5);
    beginShape();
    texture(sentImage);
    vertex(-cDim,-cDim,0,0,0);
    vertex(-cDim,cDim,0,0,dim);
    vertex(cDim,cDim,0,dim,dim);
    vertex(cDim,-cDim,0,dim,0);
    endShape();
    popMatrix();
  }
}

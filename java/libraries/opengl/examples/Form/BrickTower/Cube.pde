class Cube {

  PVector[] vertices = new PVector[24];
  PVector[] normals = new PVector[6];
  float w, h, d;

  Cube(){ }

  Cube(float w, float h, float d){
    this.w = w;
    this.h = h;
    this.d = d;

    // Cube composed of 6 quads
    // Front
    normals[0] = new PVector(0, 0, 1);
    vertices[0] = new PVector(-w/2, -h/2, d/2);
    vertices[1] = new PVector(w/2, -h/2, d/2);
    vertices[2] = new PVector(w/2, h/2, d/2);
    vertices[3] = new PVector(-w/2, h/2, d/2);

    // Left
    normals[1] = new PVector(-1, 0, 0);
    vertices[4] = new PVector(-w/2, -h/2, d/2);
    vertices[5] = new PVector(-w/2, -h/2, -d/2);
    vertices[6] = new PVector(-w/2, h/2, -d/2);
    vertices[7] = new PVector(-w/2, h/2, d/2);

    // Right
    normals[2] = new PVector(1, 0, 0);
    vertices[8] = new PVector(w/2, -h/2, d/2);
    vertices[9] = new PVector(w/2, -h/2, -d/2);
    vertices[10] = new PVector(w/2, h/2, -d/2);
    vertices[11] = new PVector(w/2, h/2, d/2);

    // Back
    normals[3] = new PVector(0, 0, -1);
    vertices[12] = new PVector(-w/2, -h/2, -d/2);  
    vertices[13] = new PVector(w/2, -h/2, -d/2);
    vertices[14] = new PVector(w/2, h/2, -d/2);
    vertices[15] = new PVector(-w/2, h/2, -d/2);

    // Top
    normals[4] = new PVector(0, 1, 0);
    vertices[16] = new PVector(-w/2, -h/2, d/2);
    vertices[17] = new PVector(-w/2, -h/2, -d/2);
    vertices[18] = new PVector(w/2, -h/2, -d/2);
    vertices[19] = new PVector(w/2, -h/2, d/2);

    // Bottom
    normals[5] = new PVector(0, 1, 0);
    vertices[20] = new PVector(-w/2, h/2, d/2);
    vertices[21] = new PVector(-w/2, h/2, -d/2);
    vertices[22] = new PVector(w/2, h/2, -d/2);
    vertices[23] = new PVector(w/2, h/2, d/2);
  }

  void create(){
    for (int i=0; i<6; i++){
      beginShape(QUADS);
      normal(normals[i].x, normals[i].y, normals[i].z);
      for (int j = 0; j < 4; j++){
        vertex(vertices[j+4*i].x, vertices[j+4*i].y, vertices[j+4*i].z);
      }
      endShape();
    }
  }
} 
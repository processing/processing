float[][] cubeVertexCoords = new float[][] {
  new float[] { // top
    1, 1,-1,
    -1, 1,-1,
    -1, 1, 1,
    1, 1, 1
  }
  ,
  new float[] { // bottom
    1,-1, 1,
    -1,-1, 1,
    -1,-1,-1,
    1,-1,-1
  }
  ,
  new float[] { // front
    1, 1, 1,
    -1, 1, 1,
    -1,-1, 1,
    1,-1, 1
  }
  ,
  new float[] { // back
    1,-1,-1,
    -1,-1,-1,
    -1, 1,-1,
    1, 1,-1
  }
  ,
  new float[] { // left
    -1, 1, 1,
    -1, 1,-1,
    -1,-1,-1,
    -1,-1, 1
  }
  ,
  new float[] { // right
    1, 1,-1,
    1, 1, 1,
    1,-1, 1,
    1,-1,-1
  }
  ,
};

float[][] cubeNormalCoords = new float[][] {
  new float[] { // top
    0, 1, 0,
    0, 1, 0,
    0, 1, 0,
    0, 1, 0
  }
  ,
  new float[] { // bottom
    0,-1, 0,
    0,-1, 0,
    0,-1, 0,
    0,-1, 0
  }
  ,
  new float[] { // front
    0, 0, 1,
    0, 0, 1,
    0, 0, 1,
    0, 0, 1
  }
  ,
  new float[] { // back
    0, 0,-1,
    0, 0,-1,
    0, 0,-1,
    0, 0,-1
  }
  ,
  new float[] { // left
    -1, 0, 0,
    -1, 0, 0,
    -1, 0, 0,
    -1, 0, 0
  }
  ,
  new float[] { // right
    1, 0, 0,
    1, 0, 0,
    1, 0, 0,
    1, 0, 0
  }
  ,
};

float[][] cubeTextureCoords = new float[][] {
  new float[] { // top
    1, 0,
    1, 1,
    0, 1,
    0, 0
  }
  ,
  new float[] { // bottom
    0, 0,
    1, 0,
    1, 1,
    0, 1
  }
  ,
  new float[] { // front
    1, 1,
    0, 1,
    0, 0,
    1, 0
  }
  ,
  new float[] { // back
    0, 1,
    0, 0,
    1, 0,
    1, 1
  }
  ,
  new float[] { // left
    1, 1,
    0, 1,
    0, 0,
    1, 0
  }
  ,
  new float[] { // right
    0, 1,
    0, 0,
    1, 0,
    1, 1
  }
  ,
};  

float lightAmb[]= {
  0.5f, 0.5f, 0.5f, 1.0f
};

float lightDif[]= {
  1.0f, 1.0f, 1.0f, 1.0f
};

float lightPos[]= {
  0.0f, 0.0f, 2.0f, 1.0f
};


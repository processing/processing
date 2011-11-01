// TexCube by Andres Colubri
// Creating a textured cube vertex by vertex using the
// PShape3D class. 

PShape3D cube;

float angleX;
float angleY;  

void setup() {
  size(480, 800, P3D); 
  orientation(PORTRAIT);  

  // The 3D shape has to be created with the total name of vertices 
  // it will hold (this number cannot be changed later). Also, how
  // the vertices will be connected is specified here (this can be
  // changed later though).
  cube = (PShape3D)createShape(36, PShape3D.newParameters(TRIANGLES));
 
  // The vertices array gives access to all the vertex data
  // in the PShape3D object. It can only be used between
  // loadVertices and updateVertices. Similar thing can be done
  // for colors, normals and texture coordinates (see below).
  cube.loadVertices();
  arrayCopy(vertArray, cube.vertices);
  cube.updateVertices(); 

  // 6 child shapes are created in order to assign different 
  // textures each face of the cube.
  cube.addChild("Face 0", 0, 5);    // First face from vertex 0 to 5.
  cube.addChild("Face 1", 6, 11);   // Second face from vertex 6 to 11.
  cube.addChild("Face 2", 12, 17);  // ...and so on.
  cube.addChild("Face 3", 18, 23);  
  cube.addChild("Face 4", 24, 29);
  cube.addChild("Face 5", 30, 35);

  // The entire shape is red.
  cube.setColor(color(200, 50, 50));

  // Setting the normal for each face.
  cube.setNormal(0, 0, 0, -1);
  cube.setNormal(1, +1, 0, 0);
  cube.setNormal(2, 0, 0, +1);
  cube.setNormal(3, -1, 0, 0);
  cube.setNormal(4, 0, +1, 0);
  cube.setNormal(5, 0, -1, 0);

  // Finally, setting texture images...
  cube.setTexture(0, loadImage("1.jpg"));
  cube.setTexture(1, loadImage("2.jpg"));
  cube.setTexture(2, loadImage("3.jpg"));
  cube.setTexture(3, loadImage("4.jpg"));
  cube.setTexture(4, loadImage("5.jpg"));
  cube.setTexture(5, loadImage("6.jpg"));

  // ...and texture coordinates.
  cube.loadTexcoords();
  arrayCopy(tcoordArray, cube.texcoords);
  cube.updateTexcoords();
}

public void draw() {	    
  background(0);    

  ambient(250, 250, 250);
  pointLight(255, 255, 255, 0, 0, 200);

  translate(width/2, height/2, 200);

  angleX += 0.01;
  angleY += 0.01;	  
  rotateX(angleX);
  rotateY(angleY);

  shape(cube);
}


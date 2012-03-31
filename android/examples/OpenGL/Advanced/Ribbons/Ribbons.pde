// Ribbons, by Andres Colubri
// ArcBall class by Ariel, V3ga and Robert Hodgin (flight404)
// This sketch loads 3D atomic coordinates of a protein molecule
// from a file in PDB format (http://www.pdb.org/) and displays 
// the structure using a ribbon representation.

String pdbFile = "4HHB.pdb"; // PDB file to read
//String pdbFile = "2POR.pdb";
//String pdbFile = "1CBS.pdb";

// Some parameters to control the visual appearance:
float scaleFactor = 5;          // Size factor
int renderMode = 1;             // 0 = lines, 1 = flat ribbons
int ribbonDetail = 4;           // Ribbon detail: from 1 (lowest) to 4 (highest)
float helixDiam = 10;           // Helix diameter.
int[] ribbonWidth = {10, 7, 2}; // Ribbon widths for helix, strand and coil
color ribbonColor = color(20, 30, 200, 255); // Ribbon color

// All the molecular models read from the PDB file (it could contain more than one)
ArrayList models;

Arcball arcball;

void setup() {
  size(screenWidth, screenHeight, P3D);
  orientation(LANDSCAPE);
  
  arcball = new Arcball(width/2, height/2, 600);  
  readPDB(pdbFile);
}

void draw() {
   background(0);
   
  if (renderMode == 1) {
    lights();
  }
  
  translate(width/2, height/2, 200);
  arcball.run();

  for (int i = 0; i < models.size(); i++) {
    shape((PShape)models.get(i));
  }
}

void mousePressed(){
  arcball.mousePressed();
}

void mouseDragged(){
  arcball.mouseDragged();
}



BSpline splineSide1;
BSpline splineCenter;
BSpline splineSide2;
PVector flipTestV;
int uspacing;

int HELIX = 0;
int STRAND = 1;
int COIL = 2;
int LHANDED = -1;
int RHANDED = 1;

void createRibbonModel(ArrayList residues, PShape model, ArrayList trj) {
  // For line ribbons
  ArrayList vertices0 = new ArrayList();
  ArrayList vertices1 = new ArrayList();
  ArrayList vertices2 = new ArrayList();
  
  // For flat ribbons
  ArrayList vertices = new ArrayList();
  ArrayList normals = new ArrayList();
    
  if (ribbonDetail == 1) uspacing = 10;
  else if (ribbonDetail == 2) uspacing = 5;
  else if (ribbonDetail == 3) uspacing = 2;
  else uspacing = 1;

  flipTestV = new PVector();
  splineSide1 = new BSpline(false);
  splineCenter = new BSpline(false);
  splineSide2 = new BSpline(false);

  int[] ss = new int[residues.size()];
  int[] handness = new int[residues.size()];

  calculateSecStr(residues, ss, handness);   

  for (int i = 0; i < residues.size(); i++) {
    constructControlPoints(residues, i, ss[i], handness[i]);

    if (renderMode == 0) {
      generateSpline(0, vertices0);
      generateSpline(1, vertices1);        
      generateSpline(2, vertices2);        
    } 
    else {
      generateFlatRibbon(vertices, normals);
    }
  }  

  if (renderMode == 0) {
    model = createShape();
    model.stroke(ribbonColor);
    model.noFill();    
    model.beginContour();
    for (int i = 0; i < vertices0.size(); i++) {
      PVector posVec = (PVector)vertices0.get(i);
      model.vertex(posVec.x, posVec.y, posVec.z);    
    }
    model.endContour();
    model.beginContour();
    for (int i = 0; i < vertices1.size(); i++) {
      PVector posVec = (PVector)vertices1.get(i);
      model.vertex(posVec.x, posVec.y, posVec.z);    
    }
    model.endContour();    
    model.beginContour();
    for (int i = 0; i < vertices2.size(); i++) {
      PVector posVec = (PVector)vertices2.get(i);
      model.vertex(posVec.x, posVec.y, posVec.z);    
    }
    model.endContour();    
    model.end(OPEN);    
  } else {
    // The ribbon construction is fairly inneficient here, since
    // it could use triangle strips instead to avoid duplicating
    // shared vertices...
    model = createShape(TRIANGLES);
    model.noStroke();
    model.fill(ribbonColor);
    for (int i = 0; i < vertices.size(); i++) {
      PVector posVec = (PVector)vertices.get(i);
      PVector normVec = (PVector)normals.get(i);
      model.normal(-normVec.x, -normVec.y, -normVec.z);
      model.vertex(posVec.x, posVec.y, posVec.z);
    }
    model.end();
  }
  
  trj.add(model);

  if (renderMode == 0) {
    int totCount = vertices0.size() + vertices1.size() + vertices2.size();
    println("Adding new model with " + totCount + " vertices.");
  } else {
    println("Adding new model with " + vertices.size() + " vertices.");
  }
}

float calculateGyrRadius(ArrayList atoms) {
  PVector ati, atj;
  float dx, dy, dz;    
  float r = 0;
  for (int i = 0; i < atoms.size(); i++) {
    ati = (PVector)atoms.get(i);
    for (int j = i + 1; j < atoms.size(); j++) {  
      atj = (PVector)atoms.get(j);

      dx = ati.x - atj.x;
      dy = ati.y - atj.y;
      dz = ati.z - atj.z;
      r +=  dx * dx + dy * dy + dz * dz;
    }
  }
  return sqrt(r) / (atoms.size() + 1);
}

// Does a cheap and dirty secondary structure assignment to the protein
// residues given in the array.
void calculateSecStr(ArrayList residues, int[] ss, int[] handness) {
  PVector c0, n1, ca1, c1, n2;
  HashMap res0, res1, res2;
  int n = residues.size();

  float[] phi = new float[n];
  float[] psi = new float[n];

  for (int i = 0; i < n; i++) {
    if (i == 0 || i == n - 1) {
      phi[i] = 90;
      psi[i] = 90;              
    } else {
      res0 = (HashMap)residues.get(i - 1);
      res1 = (HashMap)residues.get(i);
      res2 = (HashMap)residues.get(i + 1);

      c0 = (PVector)res0.get("C");
      n1 = (PVector)res1.get("N");
      ca1 = (PVector)res1.get("CA"); 
      c1 = (PVector)res1.get("C");
      n2 = (PVector)res2.get("N");

      phi[i] = calculateTorsionalAngle(c0, n1, ca1, c1);
      psi[i] = calculateTorsionalAngle(n1, ca1, c1, n2);
    }
  }  

  int firstHelix = 0;
  int nconsRHelix = 0;
  int nconsLHelix = 0;
  int firstStrand = 0;
  int nconsStrand = 0;
  for (int i = 0; i < n; i++) {
    // Right-handed helix      
    if ((dist(phi[i], psi[i], -60, -45) < 30) && (i < n - 1)) {
      if (nconsRHelix == 0) firstHelix = i;
      nconsRHelix++;
    } 
    else {
      if (3 <= nconsRHelix) {
        for (int k = firstHelix; k < i; k++) {
          ss[k] = HELIX;
          handness[k] = RHANDED;                  
        }
      }
      nconsRHelix = 0;
    }

    // Left-handed helix
    if ((dist(phi[i], psi[i], +60, +45) < 30) && (i < n - 1)) {
      if (nconsLHelix == 0) firstHelix = i;
      nconsLHelix++;

    } else {
      if (3 <= nconsLHelix) {
        for (int k = firstHelix; k < i; k++) {
          ss[k] = HELIX;
          handness[k] = LHANDED;
        }
      }
      nconsLHelix = 0;
    }

    // Strand
    if ((dist(phi[i], psi[i], -110, +130) < 30) && (i < n - 1)) {
      if (nconsStrand == 0) firstStrand = i;
      nconsStrand++;
    } else {
      if (2 <= nconsStrand) {
        for (int k = firstStrand; k < i; k++) {
          ss[k] = STRAND;
          handness[k] = RHANDED;

        }
      }
      nconsStrand = 0;
    }        

    ss[i] = COIL;
    handness[i] = RHANDED;
  }
}

// Calculates the torsional angle defined by four atoms with positions at0, at1, at2 and at3.
float calculateTorsionalAngle(PVector at0, PVector at1, PVector at2, PVector at3) {
  PVector r01 = PVector.sub(at0, at1);
  PVector r32 = PVector.sub(at3, at2);
  PVector r12 = PVector.sub(at1, at2);

  PVector p = r12.cross(r01);
  PVector q = r12.cross(r32);
  PVector r = r12.cross(q);

  float u = q.dot(q);
  float v = r.dot(r);    

  float a;
  if (u <= 0.0 || v <= 0.0) {
    a = 360.0;
  } else {
    float u1 = p.dot(q); // u1 = p * q
    float v1 = p.dot(r); // v1 = p * r

      u = u1 / sqrt(u);
    v = v1 / sqrt(v);

    if (abs(u) > 0.01 || abs(v) > 0.01) a = degrees(atan2(v, u));
    else a = 360.0;
  }    
  return a;
}

void generateSpline(int n, ArrayList vertices) {
  int ui;
  float u;
  PVector v0, v1;

  v1 = new PVector();

  if (n == 0) splineSide1.feval(0, v1); 
  else if (n == 1) splineCenter.feval(0, v1);
  else splineSide2.feval(0, v1);
  vertices.add(new PVector(v1.x, v1.y, v1.z));

  for (ui = 1; ui <= 10; ui ++) {
    if (ui % uspacing == 0) {
      u = 0.1 * ui; 

      if (n == 0) splineSide1.feval(u, v1); 
      else if (n == 1) splineCenter.feval(u, v1); 
      else splineSide2.feval(u, v1);

      vertices.add(new PVector(v1.x, v1.y, v1.z));            
    }
  }
}

void generateFlatRibbon(ArrayList vertices, ArrayList normals) {
  PVector CentPoint0, CentPoint1;
  PVector Sid1Point0, Sid1Point1;
  PVector Sid2Point0, Sid2Point1;
  PVector Transversal, Tangent;
  PVector Normal0, Normal1;
  int ui;
  float u;

  CentPoint0 = new PVector();
  CentPoint1 = new PVector();
  Sid1Point0 = new PVector();
  Sid1Point1 = new PVector();
  Sid2Point0 = new PVector();
  Sid2Point1 = new PVector();
  Transversal = new PVector();
  Tangent = new PVector();
  Normal0 = new PVector();
  Normal1 = new PVector();

  // The initial geometry is generated.
  splineSide1.feval(0, Sid1Point1);
  splineCenter.feval(0, CentPoint1);
  splineSide2.feval(0, Sid2Point1);

  // The tangents at the three previous points are the same.
  splineSide2.deval(0, Tangent);

  // Vector transversal to the ribbon.    
  Transversal = PVector.sub(Sid1Point1, Sid2Point1);

  // The normal is calculated.
  Normal1 = Transversal.cross(Tangent);
  Normal1.normalize();

  for (ui = 1; ui <= 10; ui ++) {
    if (ui % uspacing == 0) {
      u = 0.1 * ui;

      // The geometry of the previous iteration is saved.
      Sid1Point0.set(Sid1Point1);
      CentPoint0.set(CentPoint1);
      Sid2Point0.set(Sid2Point1);
      Normal0.set(Normal1);

      // The new geometry is generated.
      splineSide1.feval(u, Sid1Point1);
      splineCenter.feval(u, CentPoint1);
      splineSide2.feval(u, Sid2Point1);

      // The tangents at the three previous points are the same.
      splineSide2.deval(u, Tangent);
      // Vector transversal to the ribbon.
      Transversal = PVector.sub(Sid1Point1, Sid2Point1);
      // The normal is calculated.
      Normal1 = Transversal.cross(Tangent);
      Normal1.normalize();

      // The (Sid1Point0, Sid1Point1, CentPoint1) triangle is added.
      vertices.add(new PVector(Sid1Point0.x, Sid1Point0.y, Sid1Point0.z));
      normals.add(new PVector(Normal0.x, Normal0.y, Normal0.z));

      vertices.add(new PVector(Sid1Point1.x, Sid1Point1.y, Sid1Point1.z));
      normals.add(new PVector(Normal1.x, Normal1.y, Normal1.z));

      vertices.add(new PVector(CentPoint1.x, CentPoint1.y, CentPoint1.z));
      normals.add(new PVector(Normal1.x, Normal1.y, Normal1.z));      

      // The (Sid1Point0, CentPoint1, CentPoint0) triangle is added.
      vertices.add(new PVector(Sid1Point0.x, Sid1Point0.y, Sid1Point0.z));
      normals.add(new PVector(Normal0.x, Normal0.y, Normal0.z));

      vertices.add(new PVector(CentPoint1.x, CentPoint1.y, CentPoint1.z));
      normals.add(new PVector(Normal1.x, Normal1.y, Normal1.z));      

      vertices.add(new PVector(CentPoint0.x, CentPoint0.y, CentPoint0.z));
      normals.add(new PVector(Normal0.x, Normal0.y, Normal0.z));
      
      // (Sid2Point0, Sid2Point1, CentPoint1) triangle is added.
      vertices.add(new PVector(Sid2Point0.x, Sid2Point0.y, Sid2Point0.z));
      normals.add(new PVector(Normal0.x, Normal0.y, Normal0.z));

      vertices.add(new PVector(Sid2Point1.x, Sid2Point1.y, Sid2Point1.z));
      normals.add(new PVector(Normal1.x, Normal1.y, Normal1.z));                        

      vertices.add(new PVector(CentPoint1.x, CentPoint1.y, CentPoint1.z));
      normals.add(new PVector(Normal1.x, Normal1.y, Normal1.z));

      // (Sid2Point0, CentPoint1, CentPoint0) triangle is added.
      vertices.add(new PVector(Sid2Point0.x, Sid2Point0.y, Sid2Point0.z));
      normals.add(new PVector(Normal0.x, Normal0.y, Normal0.z));                        
     
      vertices.add(new PVector(CentPoint1.x, CentPoint1.y, CentPoint1.z));
      normals.add(new PVector(Normal1.x, Normal1.y, Normal1.z));

      vertices.add(new PVector(CentPoint0.x, CentPoint0.y, CentPoint0.z));
      normals.add(new PVector(Normal0.x, Normal0.y, Normal0.z));
    }
  }
}

/******************************************************************************
 * The code in the following three functions is based in the method introduced 
 * in this paper:
 * "Algorithm for ribbon models of proteins."
 * Authors: Mike Carson and Charles E. Bugg
 * Published in: J.Mol.Graphics 4, pp. 121-122 (1986)
 ******************************************************************************/

// Shifts the control points one place to the left.
void shiftControlPoints() {
  splineSide1.shiftBSplineCPoints();
  splineCenter.shiftBSplineCPoints();
  splineSide2.shiftBSplineCPoints();
}

// Adds a new control point to the arrays CPCenter, CPRight and CPLeft
void addControlPoints(PVector ca0, PVector ox0, PVector ca1, int ss, int handness) {
  PVector A, B, C, D, p0, cpt0, cpt1, cpt2;

  A = PVector.sub(ca1, ca0);
  B = PVector.sub(ox0, ca0);

  // Vector normal to the peptide plane (pointing outside in the case of the
  // alpha helix).
  C = A.cross(B);

  // Vector contained in the peptide plane (perpendicular to its direction).
  D = C.cross(A);

  // Normalizing vectors.
  C.normalize();
  D.normalize();

  // Flipping test (to avoid self crossing in the strands).
  if ((ss != HELIX) && (90.0 < degrees(PVector.angleBetween(flipTestV, D)))) {
    // Flip detected. The plane vector is inverted.
    D.mult(-1.0);
  }

  // The central control point is constructed.
  cpt0 = linearComb(0.5, ca0, 0.5, ca1);
  splineCenter.setCPoint(3, cpt0);

  if (ss == HELIX) {
    // When residue i is contained in a helix, the control point is moved away
    // from the helix axis, along the C direction. 
    p0 = new PVector();
    splineCenter.getCPoint(3, p0);
    cpt0 = linearComb(1.0, p0, handness * helixDiam, C);
    splineCenter.setCPoint(3, cpt0);
  }

  // The control points for the side ribbons are constructed.
  cpt1 = linearComb(1.0, cpt0, +ribbonWidth[ss], D);
  splineSide1.setCPoint(3, cpt1);

  cpt2 = linearComb(1.0, cpt0, -ribbonWidth[ss], D);
  splineSide2.setCPoint(3, cpt2);

  // Saving the plane vector (for the flipping test in the next call).
  flipTestV.set(D);
}

void constructControlPoints(ArrayList residues, int res, int ss, int handness) {
  PVector ca0, ox0, ca1;
  PVector p0, p1, p2, p3;

  p1 = new PVector();        
  p2 = new PVector();
  p3 = new PVector();

  HashMap res0, res1;

  res0 = res1 = null;
  if (res == 0) {
    // The control points 2 and 3 are created.
    flipTestV.set(0, 0, 0);

    res0 = (HashMap)residues.get(res);
    res1 = (HashMap)residues.get(res + 1);
    ca0 = (PVector)res0.get("CA");
    ox0 = (PVector)res0.get("O");
    ca1 = (PVector)res1.get("CA");        
    addControlPoints(ca0, ox0, ca1, ss, handness);
    splineSide1.copyCPoints(3, 2);
    splineCenter.copyCPoints(3, 2);
    splineSide2.copyCPoints(3, 2);        

    res0 = (HashMap)residues.get(res + 1);
    res1 = (HashMap)residues.get(res + 2);
    ca0 = (PVector)res0.get("CA");
    ox0 = (PVector)res0.get("O");
    ca1 = (PVector)res1.get("CA"); 
    addControlPoints(ca0, ox0, ca1, ss, handness);

    // We still need the two first control points.
    // Moving backwards along the cp_center[2] - cp_center[3] direction.
    splineCenter.getCPoint(2, p2);
    splineCenter.getCPoint(3, p3);

    p1 = linearComb(2.0, p2, -1, p3);
    splineCenter.setCPoint(1, p1);        
    splineSide1.setCPoint(1, linearComb(1.0, p1, +ribbonWidth[ss], flipTestV)); 
    splineSide2.setCPoint(1, linearComb(1.0, p1, -ribbonWidth[ss], flipTestV));        

    p0 = linearComb(2.0, p1, -1, p2);
    splineCenter.setCPoint(0, p0);
    splineSide1.setCPoint(0, linearComb(1.0, p0, +ribbonWidth[ss], flipTestV));
    splineSide2.setCPoint(0, linearComb(1.0, p0, -ribbonWidth[ss], flipTestV));
  } else {
    shiftControlPoints();
    if ((residues.size() - 1 == res) || (residues.size() - 2 == res)) { 
      // Moving forward along the cp_center[1] - cp_center[2] direction.
      splineCenter.getCPoint(1, p1);             
      splineCenter.getCPoint(2, p2);

      p3 = linearComb(2.0, p2, -1, p1);
      splineCenter.setCPoint(3, p3);
      splineSide1.setCPoint(3, linearComb(1.0, p3, +ribbonWidth[ss], flipTestV));
      splineSide2.setCPoint(3, linearComb(1.0, p3, -ribbonWidth[ss], flipTestV));
    } else {
      res0 = (HashMap)residues.get(res + 1);
      res1 = (HashMap)residues.get(res + 2);
      ca0 = (PVector)res0.get("CA");
      ox0 = (PVector)res0.get("O");
      ca1 = (PVector)res1.get("CA");        
      addControlPoints(ca0, ox0, ca1, ss, handness);
    }
  }
  splineSide1.updateMatrix3();
  splineCenter.updateMatrix3();
  splineSide2.updateMatrix3();
}

PVector linearComb(float scalar0, PVector vector0, float scalar1, PVector vector1) {
  return PVector.add(PVector.mult(vector0, scalar0), PVector.mult(vector1, scalar1));
}

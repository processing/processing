void readPDB(String filename) {
  String strLines[];

  float xmin, xmax, ymin, ymax, zmin, zmax;

  String xstr, ystr, zstr;
  float x, y, z;
  int res, res0;
  int nmdl;
  String atstr, resstr;

  PShape model;
  ArrayList atoms;  
  ArrayList residues;
  HashMap residue;
  PVector v;
  String s;
  strLines = loadStrings(filename);

  models = new ArrayList();

  xmin = ymin = zmin = 10000;
  xmax = ymax = zmax = -10000; 

  atoms = null;
  residues = null;
  residue = null;
  model = null;
  res0 = -1;    
  nmdl = -1;
  for (int i = 0; i < strLines.length; i++) {
    s = strLines[i];

    if (s.startsWith("MODEL") || (s.startsWith("ATOM") && res0 == -1)) {
      nmdl++;

      res0 = -1;

      atoms = new ArrayList();
      residues = new ArrayList();
    }

    if (s.startsWith("ATOM")) {
      atstr = s.substring(12, 15);
      atstr = atstr.trim();
      resstr = s.substring(22, 26);
      resstr = resstr.trim();
      res = parseInt(resstr);

      xstr = s.substring(30, 37);
      xstr = xstr.trim();
      ystr = s.substring(38, 45);
      ystr = ystr.trim();            
      zstr = s.substring(46, 53);
      zstr = zstr.trim();

      x = scaleFactor * parseFloat(xstr);
      y = scaleFactor * parseFloat(ystr);
      z = scaleFactor * parseFloat(zstr);            
      v = new PVector(x, y, z);

      xmin = min(xmin, x);
      xmax = max(xmax, x);
    
      ymin = min(ymin, y);
      ymax = max(ymax, y);

      zmin = min(zmin, z);
      zmax = max(zmax, z);

      atoms.add(v);

      if (res0 != res) {
        if (residue != null) residues.add(residue);
        residue = new HashMap();
      }
      residue.put(atstr, v);

      res0 = res;
    }

    if (s.startsWith("ENDMDL") || s.startsWith("TER")) {
      if (residue != null) residues.add(residue);

      createRibbonModel(residues, model, models);
      float rgyr = calculateGyrRadius(atoms);

      res0 = -1;
      residue = null;  
      atoms = null;
      residues = null;
    }
  }

  if (residue != null) {
    if (residue != null) residues.add(residue);

    createRibbonModel(residues, model, models);
    float rgyr = calculateGyrRadius(atoms);

    atoms = null;
    residues = null;
  }

  // Centering models at (0, 0, 0).
  float dx = -0.5f * (xmin + xmax);
  float dy = -0.5f * (ymin + ymax);
  float dz = -0.5f * (zmin + zmax);
  for (int n = 0; n < models.size(); n++) {
    model = (PShape) models.get(n);
    model.translate(dx, dy, dz);
  }

  println("Loaded PDB file with " + models.size() + " models.");
}

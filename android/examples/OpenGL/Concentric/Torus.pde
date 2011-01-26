void drawTorus(float outerRad, float innerRad, int numc, int numt, int axis) {
  float x, y, z, s, t, u, v;
  float nx, ny, nz;
  float aInner, aOuter;
  int idx = 0;
  
  beginShape(QUAD_STRIP);
  for (int i = 0; i < numc; i++) {
    for (int j = 0; j <= numt; j++) {
      t = j;
      v = t / (float)numt;
      aOuter = v * TWO_PI;
      float cOut = cos(aOuter);
      float sOut = sin(aOuter);
      for (int k = 1; k >= 0; k--) {
         s = (i + k);
         u = s / (float)numc;
         aInner = u * TWO_PI;
         float cIn = cos(aInner);
         float sIn = sin(aInner);
         
         if (axis == 0) {
           x = (outerRad + innerRad * cIn) * cOut;
           y = (outerRad + innerRad * cIn) * sOut;
           z = innerRad * sIn;
         } else if (axis == 1) {
           x = innerRad * sIn;
           y = (outerRad + innerRad * cIn) * sOut;
           z = (outerRad + innerRad * cIn) * cOut;           
         } else {
           x = (outerRad + innerRad * cIn) * cOut;
           y = innerRad * sIn;
           z = (outerRad + innerRad * cIn) * sOut;
         }           
         
         nx = cIn * cOut; 
         ny = cIn * sOut;
         nz = sIn;
         
         normal(nx, ny, nz);
         vertex(x, y, z);
      }
    }
  }
  endShape();
}

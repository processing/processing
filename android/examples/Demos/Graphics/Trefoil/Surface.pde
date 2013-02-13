// Code to draw a trefoil knot surface, with normals and texture 
// coordinates.
// Adapted from the parametric equations example by Philip Rideout:
// http://iphone-3d-programming.labs.oreilly.com/ch03.html

// This function draws a trefoil knot surface as a triangle mesh derived
// from its parametric equation.
PShape createTrefoil(float s, int ny, int nx, PImage tex) {
  PVector p0, p1, p2;
  PVector n0, n1, n2;
  float u0, u1, v0, v1;
  
  PShape obj = createShape();
  obj.beginShape(TRIANGLES);
  obj.texture(tex);
    
  for (int j = 0; j < nx; j++) {
    u0 = float(j) / nx;
    u1 = float(j + 1) / nx;
    for (int i = 0; i < ny; i++) {
      v0 = float(i) / ny;
      v1 = float(i + 1) / ny;
      
      p0 = evalPoint(u0, v0);
      n0 = evalNormal(u0, v0);
      
      p1 = evalPoint(u0, v1);
      n1 = evalNormal(u0, v1);
      
      p2 = evalPoint(u1, v1);
      n2 = evalNormal(u1, v1);

      // Triangle p0-p1-p2      
      obj.normal(n0.x, n0.y, n0.z);
      obj.vertex(s * p0.x, s * p0.y, s * p0.z, u0, v0);      
      obj.normal(n1.x, n1.y, n1.z);
      obj.vertex(s * p1.x, s * p1.y, s * p1.z, u0, v1);
      obj.normal(n2.x, n2.y, n2.z);
      obj.vertex(s * p2.x, s * p2.y, s * p2.z, u1, v1);

      p1 = evalPoint(u1, v0);
      n1 = evalNormal(u1, v0);

      // Triangle p0-p2-p1      
      obj.normal(n0.x, n0.y, n0.z);
      obj.vertex(s * p0.x, s * p0.y, s * p0.z, u0, v0);      
      obj.normal(n2.x, n2.y, n2.z);
      obj.vertex(s * p2.x, s * p2.y, s * p2.z, u1, v1);
      obj.normal(n1.x, n1.y, n1.z);
      obj.vertex(s * p1.x, s * p1.y, s * p1.z, u1, v0);      
    }
  }
  obj.endShape();
  return obj;
}

// Evaluates the surface normal corresponding to normalized 
// parameters (u, v)
PVector evalNormal(float u, float v) {
  // Compute the tangents and their cross product.
  PVector p = evalPoint(u, v);
  PVector tangU = evalPoint(u + 0.01, v);
  PVector tangV = evalPoint(u, v + 0.01);
  tangU.sub(p);
  tangV.sub(p);
  
  PVector normUV = tangV.cross(tangU);
  normUV.normalize();
  return normUV;
}

// Evaluates the surface point corresponding to normalized 
// parameters (u, v)
PVector evalPoint(float u, float v) {
  float a = 0.5;
  float b = 0.3;
  float c = 0.5;
  float d = 0.1;
  float s = TWO_PI * u;
  float t = (TWO_PI * (1 - v)) * 2;  
        
  float r = a + b * cos(1.5 * t);
  float x = r * cos(t);
  float y = r * sin(t);
  float z = c * sin(1.5 * t);
        
  PVector dv = new PVector();
  dv.x = -1.5 * b * sin(1.5 * t) * cos(t) -
         (a + b * cos(1.5 * t)) * sin(t);
  dv.y = -1.5 * b * sin(1.5 * t) * sin(t) +
         (a + b * cos(1.5 * t)) * cos(t);
  dv.z = 1.5 * c * cos(1.5 * t);
        
  PVector q = dv;      
  q.normalize();
  PVector qvn = new PVector(q.y, -q.x, 0);
  qvn.normalize();
  PVector ww = q.cross(qvn);
        
  PVector pt = new PVector();
  pt.x = x + d * (qvn.x * cos(s) + ww.x * sin(s));
  pt.y = y + d * (qvn.y * cos(s) + ww.y * sin(s));
  pt.z = z + d * ww.z * sin(s);
  return pt;
}

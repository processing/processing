// Just draws an sphere of the given radius and resolutoin, using up to
// two images for texturing.
void drawSphere(float r, int n, PImage tex0, PImage tex1) {
  float startLat = -90;
  float startLon = 0.0;

  float latInc = 180.0 / n;
  float lonInc = 360.0 / n;

  float u,  v;
  float phi1,  phi2;
  float theta1,  theta2;
  PVector p0 = new PVector();
  PVector p1 = new PVector();
  PVector p2 = new PVector();
  beginShape(TRIANGLES);
  if (tex1 != null) {
    texture(tex0, tex1);
  } else {
    texture(tex0);
  }

  for (int col = 0; col < n; col++) {
    phi1 = (startLon + col * lonInc) * DEG_TO_RAD;
    phi2 = (startLon + (col + 1) * lonInc) * DEG_TO_RAD;
    for (int row = 0; row < n; row++) {
      theta1 = (startLat + row * latInc) * DEG_TO_RAD;
      theta2 = (startLat + (row + 1) * latInc) * DEG_TO_RAD;

      p0.x = cos(phi1) * cos(theta1);
      p0.y = sin(theta1);            
      p0.z = sin(phi1) * cos(theta1);

      p1.x = cos(phi1) * cos(theta2);
      p1.y = sin(theta2);            
      p1.z = sin(phi1) * cos(theta2);

      p2.x = cos(phi2) * cos(theta2);
      p2.y = sin(theta2);            
      p2.z = sin(phi2) * cos(theta2);

      normal(p0.x,  p0.y,  p0.z);     
      u = map(phi1, TWO_PI, 0, 0, 1);
      v = map(theta1, -HALF_PI, HALF_PI, 0, 1);
      vertex(r * p0.x,  r * p0.y,  r * p0.z,  u,  v);
 
      normal(p1.x,  p1.y,  p1.z);
      u = map(phi1, TWO_PI, 0, 0, 1);
      v = map(theta2, -HALF_PI, HALF_PI, 0, 1);
      vertex(r * p1.x,  r * p1.y,  r * p1.z,  u,  v);

      normal(p2.x,  p2.y,  p2.z);
      u = map(phi2, TWO_PI, 0, 0, 1);
      v = map(theta2, -HALF_PI, HALF_PI, 0, 1);      
      vertex(r * p2.x,  r * p2.y,  r * p2.z,  u,  v);

      p1.x = cos(phi2) * cos(theta1);
      p1.y = sin(theta1);            
      p1.z = sin(phi2) * cos(theta1);

      normal(p0.x,  p0.y,  p0.z);
      u = map(phi1, TWO_PI, 0, 0, 1);
      v = map(theta1, -HALF_PI, HALF_PI, 0, 1);      
      vertex(r * p0.x,  r * p0.y,  r * p0.z,  u,  v);

      normal(p2.x,  p2.y,  p2.z);
      u = map(phi2, TWO_PI, 0, 0, 1);
      v = map(theta2, -HALF_PI, HALF_PI, 0, 1);            
      vertex(r * p2.x,  r * p2.y,  r * p2.z,  u,  v);

      normal(p1.x,  p1.y,  p1.z);
      u = map(phi2, TWO_PI, 0, 0, 1);
      v = map(theta1, -HALF_PI, HALF_PI, 0, 1);            
      vertex(r * p1.x,  r * p1.y,  r * p1.z,  u,  v);
    }
  }

  endShape();  
}

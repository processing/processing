/**
 * Letter K 
 * by Peter Cho. 
 * 
 * Move the mouse across the screen to fold the "K". 
 */
 
color backgroundColor;
color foregroundColor;
color foregroundColor2;

float px, py;
float pfx, pfy;
float pv2, pvx, pvy;
float pa2, pax, pay;
float pMass, pDrag;

void setup() {
  size(640, 360, P3D);
  noStroke();
  backgroundColor = color(134, 144, 154);
  foregroundColor = color(235, 235, 30);
  foregroundColor2 = color(240, 130, 20);
  initParticle(0.6, 0.9,  width/2, height/2);
}

void draw() {
  background(backgroundColor);
  pushMatrix();

  iterateParticle(0.15*(-px+mouseX), 0.15*(-py+(height-mouseY)));

  translate(width/2, height/2, 0);
  fill(foregroundColor);
  drawK();
 
  pushMatrix();
  translate(0, 0, 1);
  translate(0.75 * (px-width/2), -0.75 * (py-height/2), 0);
  translate(0.75 * (px-width/2), -0.75 * (py-height/2), 0);
  rotateZ(atan2(-(py-height/2), (px-width/2)) + PI/2);
  rotateX(PI);
  rotateZ(-(atan2(-(py-height/2), (px-width/2)) + PI/2));
  
  fill(foregroundColor2);
  drawK();
  popMatrix();

  translate(0.75 * (px-width/2), -0.75 * (py-height/2), 2);
  rotateZ(atan2(-(py-height/2), (px-width/2)) + PI/2);
  
  fill(backgroundColor);
  beginShape(QUADS);
  vertex(-640, 0);
  vertex( 640, 0);
  vertex( 640, -360);
  vertex(-640, -360);
  endShape();
  
  popMatrix();
 
}

void initParticle(float _mass, float _drag, float ox, float oy) {
  px = ox;
  py = oy;
  pv2 = 0.0;
  pvx = 0.0;
  pvy = 0.0;
  pa2 = 0.0;
  pax = 0.0;
  pay = 0.0;
  pMass = _mass;
  pDrag = _drag;
}

void iterateParticle(float fkx, float fky) {
  // iterate for a single force acting on the particle
  pfx = fkx;
  pfy = fky;
  pa2 = pfx*pfx + pfy*pfy;
  if (pa2 < 0.0000001) {
    return;
  }
  pax = pfx/pMass;
  pay = pfy/pMass;
  pvx += pax;
  pvy += pay;
  pv2 = pvx*pvx + pvy*pvy;
  if (pv2 < 0.0000001) {
    return;
  }
  pvx *= (1.0 - pDrag);
  pvy *= (1.0 - pDrag);
  px += pvx;
  py += pvy;
}

void drawK() {
  pushMatrix();
  
  scale(1.5);
  translate(-63, 71);
  beginShape(QUADS);
  vertex(0, 0, 0);
  vertex(0, -142.7979, 0);
  vertex(37.1992, -142.7979, 0);
  vertex(37.1992, 0, 0);
  
  vertex(37.1992, -87.9990, 0);
  vertex(84.1987, -142.7979, 0);
  vertex(130.3979, -142.7979, 0);
  vertex(37.1992, -43.999, 0);

  vertex(77.5986-.2, -86.5986-.3, 0);
  vertex(136.998, 0, 0);
  vertex(90.7988, 0, 0);
  vertex(52.3994-.2, -59.999-.3, 0);
  endShape();
  //translate(63, -71);
  popMatrix();
}




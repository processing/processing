/**
 * Bezier Ellipse  
 * By Ira Greenberg 
 * 
 * Generates an ellipse using bezier() and
 * trig functions. Approximately every 1/2 
 * second a new ellipse is plotted using 
 * random values for control/anchor points.
 */

// arrays to hold ellipse coordinate data
float[] px, py, cx, cy, cx2, cy2;

// global variable-points in ellipse
int pts = 4;

color controlPtCol = #222222;
color anchorPtCol = #BBBBBB;

void setup(){
  size(640, 360);
  setEllipse(pts, 65, 65);
  frameRate(1);
}

void draw(){
  background(145);
  drawEllipse();
  setEllipse(int(random(3, 12)), random(-100, 150), random(-100, 150));
}

// Draw ellipse with anchor/control points
void drawEllipse(){
  strokeWeight(1.125);
  stroke(255);
  noFill();
  // Create ellipse
  for (int i=0; i<pts; i++){
    if (i==pts-1) {
      bezier(px[i], py[i], cx[i], cy[i], cx2[i], cy2[i],  px[0], py[0]);
    }
    else{
      bezier(px[i], py[i], cx[i], cy[i], cx2[i], cy2[i],  px[i+1], py[i+1]);
    }
  }
  strokeWeight(.75);
  stroke(0);
  rectMode(CENTER);

  // Control handles and tangent lines
  for ( int i = 0; i < pts; i++){
    if (i==pts-1){  // Last loop iteration-close path
      line(px[0], py[0], cx2[i], cy2[i]);
    }
    if (i>0){
      line(px[i], py[i], cx2[i-1], cy2[i-1]);
    }
    line(px[i], py[i], cx[i], cy[i]);
  }

  for ( int i=0; i< pts; i++){
    fill(controlPtCol);
    noStroke();
    // Control handles
    ellipse(cx[i], cy[i], 4, 4);
    ellipse(cx2[i], cy2[i], 4, 4);

    fill(anchorPtCol);
    stroke(0);
    // Anchor points
    rect(px[i], py[i], 5, 5);
  }
}

// Fill arrays with ellipse coordinate data
void setEllipse(int points, float radius, float controlRadius){
  pts = points;
  px = new float[points];
  py = new float[points];
  cx = new float[points];
  cy = new float[points];
  cx2 = new float[points];
  cy2 = new float[points];
  float angle = 360.0/points;
  float controlAngle1 = angle/3.0;
  float controlAngle2 = controlAngle1*2.0;
  for ( int i=0; i<points; i++){
    px[i] = width/2+cos(radians(angle))*radius;
    py[i] = height/2+sin(radians(angle))*radius;
    cx[i] = width/2+cos(radians(angle+controlAngle1))* 
      controlRadius/cos(radians(controlAngle1));
    cy[i] = height/2+sin(radians(angle+controlAngle1))* 
      controlRadius/cos(radians(controlAngle1));
    cx2[i] = width/2+cos(radians(angle+controlAngle2))* 
      controlRadius/cos(radians(controlAngle1));
    cy2[i] = height/2+sin(radians(angle+controlAngle2))* 
      controlRadius/cos(radians(controlAngle1));

    // Increment angle so trig functions keep chugging along
    angle+=360.0/points;
  }
}

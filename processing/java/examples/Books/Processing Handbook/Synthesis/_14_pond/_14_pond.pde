/**
 * Synthesis 4: Structure and Interface
 * Pond by William Ngan (www.metaphorical.net)
 * p. 197
 * 
 * Click to generate ripples and attract the fish. 
 * Press keys 0-3 to turn that number of big fish ON or OFF.
 */


int NUM = 75; // number of fish

Fish[] flock = new Fish[NUM];
Fish bigfish1;
Fish bigfish2;
Fish bigfish3;

//ripple
float rippleX, rippleY;
float[] ripple = new float[20];
boolean hasRipple;
int countRipple;

int hasPredator = 1; // Number of predator (big fish)


void setup() {

  size(600, 600);

  colorMode(HSB, 360, 100, 100);
  background(85,46,83);

  frameRate(30);

  // Small fish
  for (int i=0; i<NUM; i++) {
    flock[i] = new Fish( random(width), random(height), 2f, random(PI), random(8f,12f) );
    flock[i].setSpeedLimit( random(1f,3f), 0.5f );
    flock[i].setColor( random(13), random(30,70), 100 );
  }

  // Ripple
  for (int i=0; i<ripple.length; i++) {
    ripple[i] = 999;
  }

  // Big fish 1
  bigfish1 = new Fish( random(width), random(height), 1f, random(PI), 18f );
  bigfish1.setSpeedLimit( 2f, 1f );
  bigfish1.setColor( 84,45,100 );

  // Big fish 2
  bigfish2 = new Fish( random(width), random(height), 1f, random(PI), 18f );
  bigfish2.setSpeedLimit( 1f, 0.5f );
  bigfish2.setColor( 90,60,70 );


  // Big fish 3
  bigfish3 = new Fish( random(width), random(height), 1f, random(PI), 22f );
  bigfish3.setSpeedLimit( 1f, 0.5f );
  bigfish3.setColor( 100,45,50 );

  smooth();
}



void draw() {

  background( 85,46,83);
  stroke(84,45,100);
  noFill();

  // Draw bigfish
  if (hasPredator>0) {

    bigfish1.scanPrey( flock, 150f );
    bigfish1.predator( bigfish2.x, bigfish2.y, 100f, 6*PI/180f, 2f);
    bigfish1.predator( bigfish3.x, bigfish3.y, 100f, 6*PI/180f, 2f);
    bigfish1.predator( mouseX, mouseY, 50f, 5*PI/180f, 1f);
    bigfish1.move();
    stroke( bigfish1.colour[0], bigfish1.colour[1], bigfish1.colour[2]);
    bigfish1.getFish();

    if (hasPredator>1) {
      bigfish2.scanPrey( flock, 120f );
      bigfish2.predator( bigfish1.x, bigfish1.y, 100f, 5*PI/180f, 1.5f);
      bigfish2.predator( bigfish3.x, bigfish3.y, 100f, 5*PI/180f, 1.5f);
      bigfish2.predator( mouseX, mouseY, 50f, 4*PI/180f, 0.8f);
      bigfish2.move();
      stroke( bigfish2.colour[0], bigfish2.colour[1], bigfish2.colour[2]);
      bigfish2.getFish();

      if (hasPredator>2) {
        bigfish3.scanPrey( flock, 100f );
        bigfish3.predator( bigfish1.x, bigfish1.y, 100f, 5*PI/180f, 1.5f);
        bigfish3.predator( bigfish2.x, bigfish2.y, 100f, 5*PI/180f, 1.5f);
        bigfish3.predator( mouseX, mouseY, 50f, 3*PI/180f, 0.5f);
        bigfish3.move();
        stroke( bigfish3.colour[0], bigfish3.colour[1], bigfish3.colour[2]);
        bigfish3.getFish();
      }
    }
  }


  // Draw small fish
  noStroke();
  for (int i=0; i<flock.length; i++) {

    fill(flock[i].colour[0], flock[i].colour[1]+flock[i].tone, flock[i].colour[2]);

    if (hasRipple) {
      flock[i].swarm( rippleX, rippleY, PI/20 );
    }

    flock[i].scanFlock( flock, 200, 50 );

    if (hasPredator>0) {
      flock[i].predator( bigfish1.x, bigfish1.y, 100f, 8*PI/180f, 1.5f);
      if (hasPredator>1) {
        flock[i].predator( bigfish2.x, bigfish2.y, 100f, 8*PI/180f, 1.5f);
        if (hasPredator>2) flock[i].predator( bigfish3.x, bigfish3.y, 100f, 8*PI/180f, 1.5f);
      }
    }
    if (!hasRipple) flock[i].predator( mouseX, mouseY, 100f, 5*PI/180f, 1f);
    flock[i].move();
    flock[i].getFish();

  }

  // Draw ripple
  stroke(84,66,96);
  noFill();

  if (hasRipple) {
    if (countRipple>0) { // ripple done, but active for another second
      countRipple++;
    } 
    else { // draw ripple
      countRipple = 1;
      for (int k=0; k<ripple.length; k++) {
        if (ripple[k]<width) {
          ripple[k]+=3f*(k+4);
          ellipse( rippleX, rippleY, ripple[k], ripple[k]); 
          countRipple = 0;
        }
      }
    }

    hasRipple = (countRipple>60) ? false : true;
  }

}


void mouseDragged() {
  rippleX = mouseX;
  rippleY = mouseY;
}

void mousePressed() {
  rippleX = mouseX;
  rippleY = mouseY;
}

void mouseReleased() {
  if (!hasRipple) {
    for (int k=0; k<ripple.length; k++) {
      ripple[k]=0;
    }
    hasRipple = true;
    countRipple = 0;
  }
}

void keyPressed() {
  if(key == '1') {
    hasPredator = 1;
  } 
  else if (key == '2') {
    hasPredator = 2;
  } 
  else if (key == '3') {
    hasPredator = 3;
  } 
  else if (key == '0') {
    hasPredator = 0;
  }
  
  // saveFrame("pond-####.tif");
}



// FISH CLASS

class Fish {

  float fsize;
  float[] tailP = { 0,0 };
  float[] tailPC = { 0,0 };
  float tailLength = 3.0f;
  float x, y, angle, speed;
  float maxSpeed, minSpeed;

  float energy = 1f; // Energy to wriggle
  float wave = 0; // Tail wave
  int wcount = 0;
  int uturn = 0;
  int boundTime = 0;

  float[] colour = { 255,255,255  };
  float tone = 0;
  boolean isBound = false;

  Fish( float px, float py, float s, float a, float size ) {
    tailP[1] = tailLength;
    tailPC[1] = tailLength;

    x = px;
    y = py;
    angle = a;
    speed = s;
    fsize = size;
  }


  //  Draw fish's curves
  void getFish(){
    float[] pos1, pos2, pos3;
    beginShape();

    pos1 = calc( 0f, -1f, fsize );
    vertex( pos1[0], pos1[1]);

    pos1 = calc( 0.5f, -1f, fsize );
    pos2 = calc( 1f, -0.5f, fsize );
    pos3 = calc( 1f, 0f, fsize );
    bezierVertex(pos1[0], pos1[1], pos2[0], pos2[1], pos3[0], pos3[1]);

    pos1 = calc( 1f, 1f, fsize );
    pos2 = calc( tailPC[0], tailPC[1], fsize );
    pos3 = calc( tailP[0], tailP[1], fsize );
    bezierVertex(pos1[0], pos1[1], pos2[0], pos2[1], pos3[0], pos3[1]);

    pos1 = calc( tailPC[0], tailPC[1], fsize );
    pos2 = calc( -1f, 1f, fsize );
    pos3 = calc( -1f, 0f, fsize );
    bezierVertex(pos1[0], pos1[1], pos2[0], pos2[1], pos3[0], pos3[1]);

    pos1 = calc( -1f, -0.5f, fsize );
    pos2 = calc( -0.5f, -1f, fsize );
    pos3 = calc( 0f, -1f, fsize );
    bezierVertex(pos1[0], pos1[1], pos2[0], pos2[1], pos3[0], pos3[1]);

    endShape();
  }


  // Set tail's position
  void setTail( float strength, float wave ) {
    tailP[0] = strength*wave;
    tailP[1] = tailLength+tailLength/2 - abs( tailLength/4*strength*wave );
    tailPC[0] = strength*wave*-1;
  }

  // Translate a bezier ctrl point according to fish's angle and pos.
  float[] calc( float px, float py, float s ) {
    float a = atan2( py, px) + angle+ PI/2;
    float r = sqrt( (px*px + py*py) );
    float[] pos ={ 
      x+r*s*cos(a), y+r*s*sin(a)     };
    return pos;
  }



  // Wriggle
  protected void wriggle() {

    // Calc energy
    if (energy > 1) {                           // if it has energy
      wcount+=energy*2;                       // tail sine-wave movement
    }

    // Sine-wave oscillation
    if (wcount>120) {
      wcount = 0;
      energy =0;
    }

    wave = sin( wcount*3*PI/180 ); //sine wave
    float strength = energy/5 * tailLength/2; //tail strength

    // Set tail position
    setTail( strength, wave );
    move();
  }


  ////////////////////////////////// /


  // Find distance
  float dist( float px, float py ) {
    px -= x;
    py -= y;
    return sqrt( px*px + py*py );
  }

  float dist( Fish p ) {
    float dx = p.x - x;
    float dy = p.y - y;
    return sqrt( dx*dx + dy*dy );
  }

  // Find angle
  float angle( float px, float py ) {
    return atan2( (py-y), (px-x) );
  }

  float angle( Fish p ) {
    return atan2( (p.y-y), (p.x-x) );
  }

  // Move one step
  void move() {
    x = x+( cos(angle)*speed );
    y = y+( sin(angle)*speed );
  }

  // Speed change
  void speedChange( float inc ) {
    speed += inc;
    if (speed<minSpeed) { speed=minSpeed; }
    if (speed>maxSpeed) { speed=maxSpeed; }
  }

  // Direction change
  void angleChange( float inc ) {
    angle += inc;
  }

  // Set speed limit
  void setSpeedLimit( float max, float min ) {
    maxSpeed = max;
    minSpeed = min;
  }

  // Set angle
  void setAngle( float a ) {
    angle = a;
  }

  // Turn towards an angle
  void turnTo( float ta, float inc ) {

    if (angle < ta) {
      angleChange( inc );
    } 
    else {
      angleChange( inc*-1 );
    }
  }


  // Set Color
  void setColor( float c1, float c2, float c3 ) {
    colour[0] = c1;
    colour[1] = c2;
    colour[2] = c3;
  }


  // Copy another fish's angle and pos
  void copyFish( Fish f ) {
    x = f.x;
    y = f.y;
    angle = f.angle;
    speed = f.speed;
  }

  //////////////////////////////////

  // Check bounds and U-turn when near bounds
  boolean checkBounds( float turn ) {

    boolean inbound = false;

    turn += boundTime/100;

    // Calculate the "buffer area" and turning angle
    float gap = speed * PI/2/turn;
    if (gap > width/4) {
      gap = width/4;
      turn = (gap/speed)/PI/2;
    }

    // Which direction to u-turn?
    if ( x-gap < 0 || x+gap > width || y-gap < 0 || y+gap > height) {

      if (uturn == 0) {

        float temp = angle;
        if (temp < 0) temp += PI*2;

        if ( temp >0 && temp<PI/2 ) {
          uturn = 1;
        } 
        else if ( temp >PI/2 && temp<PI ) {
          uturn = -1;
        } 
        else if ( temp>PI && temp<PI*3/2 ) {
          uturn = 1;
        } 
        else if ( temp>PI*3/2 && temp<PI*2 ) {
          uturn = -1;
        } 
        else {
          uturn = 1;
        }

        if (y-gap < 0 || y+gap > height) uturn *=-1;
      }

      // Turn
      angleChange( turn*uturn );

      inbound = true;

    } 
    else { // when out, clear uturn
      uturn = 0;
      inbound = false;
    }

    x = (x<0) ? 0 : ( (x>width) ? width : x );
    y = (y<0) ? 0 : ( (y>height) ? height : y );

    isBound = inbound;
    boundTime = (inbound) ? boundTime+1 : 0;

    return inbound;

  }


  // Alignment -- move towards the same direction as the flock within sight
  void align( Fish fp, float angleSpeed, float moveSpeed ) {

    turnTo( fp.angle, angleSpeed+random(angleSpeed*3) ); // 0.001

    if ( speed > fp.speed ) {
      speedChange( moveSpeed*(-1-random(1)) ); //0.2
    } 
    else {
      speedChange( moveSpeed );
    }

  }


  // Cohesion -- move towards the center of the flock within sight
  void cohere( Fish[] flocks, float angleSpeed, float moveSpeed  ) {

    // get normalised position
    float nx = 0;
    float ny = 0;

    for (int i=0; i<flocks.length; i++) {
      nx += flocks[i].x;
      ny += flocks[i].y;
    }

    nx /= flocks.length;
    ny /= flocks.length;

    turnTo( angle(nx, ny), angleSpeed+random(angleSpeed*2) ); //0.001
    speedChange( moveSpeed ); //-0.1

  }


  // Seperation -- moves away from the flock when it's too crowded
  void seperate( Fish[] flocks, float angleSpeed, float moveSpeed  ) {

    // find normalised away angle
    float nA = 0;

    for (int i=0; i<flocks.length; i++) {
      nA += (flocks[i].angle+PI);
    }

    nA /= flocks.length;
    turnTo( nA, angleSpeed+random(angleSpeed*2) ); //0.001
    speedChange( moveSpeed ); //0.05
  }



  // Collision aviodance -- moves away quickly when it's too close
  void avoid( Fish[] flocks, float angleSpeed, float moveSpeed ) {

    for (int i=0; i<flocks.length; i++) {
      float dA = angle( flocks[i] ) + PI;

      x = x + cos(dA)*moveSpeed/2;
      y = y + sin(dA)*moveSpeed/2;

      turnTo( dA, angleSpeed+random(angleSpeed) ); //0.005
    }
    speedChange( moveSpeed ); //0.1
  }

  // Flee from predator
  void predator( float px, float py, float alertDistance, float angleSpeed, float moveSpeed ) {

    float d = dist( px, py );
    if ( d < alertDistance) {
      float dA = angle(px, py) + PI;
      x = x + cos(dA)*moveSpeed; //0.01
      y = y + sin(dA)*moveSpeed;
      turnTo( dA, angleSpeed+ random(angleSpeed) );
      if (tone <50) tone+=5;
    } 
    else {
      if (tone>0) tone-=2;
    }

    speedChange( moveSpeed );
  }


  // Attracts towards a point (ie, ripple)
  void swarm( float px, float py, float d ) {
    float dA = angle(px, py);

    turnTo( dA, d );
    if (isBound) {
      turnTo( dA, d );
    }
  }

  //////////////////////////// //

  // Scan for the environment and determines behaviour
  void scanFlock( Fish[] flocks, float cohereR, float avoidR ) {


    Fish[] near = new Fish[NUM];
    int nCount = 0;
    Fish[] tooNear = new Fish[NUM];
    int tnCount = 0;
    Fish[] collide = new Fish[NUM];
    int cCount = 0;
    Fish nearest = null;
    float dist = 99999;

    float tempA = angle;

    // Check boundaries
    boolean inbound = (hasPredator>0) ? checkBounds(PI/16) : checkBounds( PI/24);


    for (int i=0; i<flocks.length; i++) {

      Fish fp = flocks[i];

      // check nearby fishes
      if (fp != this) {
        float d = dist( fp );
        if (d < cohereR ) {
          near[nCount++] = fp;
          if (dist > d ) {
            dist = d;
            nearest = fp;
          }
          if ( d <= avoidR ) {
            tooNear[tnCount++] = fp;
            if ( d <= avoidR/2 ) {
              collide[cCount++] = fp;
            }
          }
        }
      }

      // Calc and make flocking behaviours
      Fish[] near2 = new Fish[nCount];
      Fish[] tooNear2 = new Fish[tnCount];
      Fish[] collide2 = new Fish[cCount];

      int j=0;
      for (j=0; j<nCount; j++) {
        near2[j] = near[j];
      }
      for (j=0; j<tnCount; j++) {
        tooNear2[j] = tooNear[j];
      }
      for (j=0; j<cCount; j++) {
        collide2[j] = collide[j];
      }

      if (!inbound && !hasRipple) {
        if (nearest!=null) {
          align( nearest, 0.1f*PI/180, 0.2f );
        }
        cohere( near2, 0.1f*PI/180, -0.1f );
      }
      seperate( tooNear2, (random(0.1f)+0.1f)*PI/180, 0.05f );
      avoid( collide2, (random(0.2f)+0.2f)*PI/180, 0.1f );
    }

    float diffA = (angle - tempA)*5;
    float c = diffA*180/(float)Math.PI;

    // Wriggle tail
    energy += abs( c/100 );
    wriggle();

  }


  // Scan for food
  void scanPrey( Fish[] flocks, float range ) {


    Fish nearest = null;
    float dist = 99999;

    float tempA = angle;

    // Look for nearby food
    for (int i=0; i<flocks.length; i++) {   
      float d = dist( flocks[i] );
      if (dist > d ) {
        dist = d;
        nearest = flocks[i];
      }
    }

    // Move towards food
    if (dist < range) {

      if (dist > range/2) {
        speedChange( 0.5f );
      } 
      else {
        speedChange( -0.5f );
      }

      turnTo( angle( nearest ), 0.05f );

      float diffA = (angle - tempA)*10;

      float c = diffA*180/PI;

      energy += abs( c/150 );
    }

    // Check boundaries
    checkBounds( PI/16 );

    // Wriggle tail
    wriggle();

  }


}

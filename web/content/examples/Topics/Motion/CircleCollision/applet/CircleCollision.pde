/**
 * Circle Collision with Swapping Velocities
 * by Ira Greenberg. 
 * 
 * Based on Keith Peter's Solution in
 * Foundation Actionscript Animation: Making Things Move!
 */

Ball[] balls =  { 
  new Ball(100, 400, 20), 
  new Ball(700, 400, 80) 
  };

Vect2D[] vels = { 
  new Vect2D(2.15, -1.35), 
  new Vect2D(-1.65, .42) 
  };

void setup(){
  size(640, 360);
  smooth();
  noStroke();
}

void draw(){
  background(51);
  fill(204);
  for (int i=0; i< 2; i++){
    balls[i].x += vels[i].vx;
    balls[i].y += vels[i].vy;
    ellipse(balls[i].x, balls[i].y, balls[i].r*2, balls[i].r*2);
    checkBoundaryCollision(balls[i], vels[i]);
  }
  checkObjectCollision(balls, vels);
}

void checkObjectCollision(Ball[] b, Vect2D[] v){

  // get distances between the balls components
  Vect2D bVect = new Vect2D();
  bVect.vx = b[1].x - b[0].x;
  bVect.vy = b[1].y - b[0].y;

  // calculate magnitude of the vector separating the balls
  float bVectMag = sqrt(bVect.vx * bVect.vx + bVect.vy * bVect.vy);
  if (bVectMag < b[0].r + b[1].r){
    // get angle of bVect
    float theta  = atan2(bVect.vy, bVect.vx);
    // precalculate trig values
    float sine = sin(theta);
    float cosine = cos(theta);

    /* bTemp will hold rotated ball positions. You 
     just need to worry about bTemp[1] position*/
    Ball[] bTemp = {  
      new Ball(), new Ball()      };
    /* b[1]'s position is relative to b[0]'s
     so you can use the vector between them (bVect) as the 
     reference point in the rotation expressions.
     bTemp[0].x and bTemp[0].y will initialize
     automatically to 0.0, which is what you want
     since b[1] will rotate around b[0] */
    bTemp[1].x  = cosine * bVect.vx + sine * bVect.vy;
    bTemp[1].y  = cosine * bVect.vy - sine * bVect.vx;

    // rotate Temporary velocities
    Vect2D[] vTemp = { 
      new Vect2D(), new Vect2D()     };
    vTemp[0].vx  = cosine * v[0].vx + sine * v[0].vy;
    vTemp[0].vy  = cosine * v[0].vy - sine * v[0].vx;
    vTemp[1].vx  = cosine * v[1].vx + sine * v[1].vy;
    vTemp[1].vy  = cosine * v[1].vy - sine * v[1].vx;

    /* Now that velocities are rotated, you can use 1D
     conservation of momentum equations to calculate 
     the final velocity along the x-axis. */
    Vect2D[] vFinal = {  
      new Vect2D(), new Vect2D()      };
    // final rotated velocity for b[0]
    vFinal[0].vx = ((b[0].m - b[1].m) * vTemp[0].vx + 2 * b[1].m * 
      vTemp[1].vx) / (b[0].m + b[1].m);
    vFinal[0].vy = vTemp[0].vy;
    // final rotated velocity for b[0]
    vFinal[1].vx = ((b[1].m - b[0].m) * vTemp[1].vx + 2 * b[0].m * 
      vTemp[0].vx) / (b[0].m + b[1].m);
    vFinal[1].vy = vTemp[1].vy;

    // hack to avoid clumping
    bTemp[0].x += vFinal[0].vx;
    bTemp[1].x += vFinal[1].vx;

    /* Rotate ball positions and velocities back
     Reverse signs in trig expressions to rotate 
     in the opposite direction */
    // rotate balls
    Ball[] bFinal = { 
      new Ball(), new Ball()     };
    bFinal[0].x = cosine * bTemp[0].x - sine * bTemp[0].y;
    bFinal[0].y = cosine * bTemp[0].y + sine * bTemp[0].x;
    bFinal[1].x = cosine * bTemp[1].x - sine * bTemp[1].y;
    bFinal[1].y = cosine * bTemp[1].y + sine * bTemp[1].x;

    // update balls to screen position
    b[1].x = b[0].x + bFinal[1].x;
    b[1].y = b[0].y + bFinal[1].y;
    b[0].x = b[0].x + bFinal[0].x;
    b[0].y = b[0].y + bFinal[0].y;

    // update velocities
    v[0].vx = cosine * vFinal[0].vx - sine * vFinal[0].vy;
    v[0].vy = cosine * vFinal[0].vy + sine * vFinal[0].vx;
    v[1].vx = cosine * vFinal[1].vx - sine * vFinal[1].vy;
    v[1].vy = cosine * vFinal[1].vy + sine * vFinal[1].vx;
  }
}

class Ball{
  float x, y, r, m;

  // default constructor
  Ball() {
  }

  Ball(float x, float y, float r) {
    this.x = x;
    this.y = y;
    this.r = r;
    m = r*.1;
  }
}

class Vect2D{
  float vx, vy;

  // default constructor
  Vect2D() {
  }

  Vect2D(float vx, float vy) {
    this.vx = vx;
    this.vy = vy;
  }
}

// checkBoundaryCollision() function:
void checkBoundaryCollision(Ball ball, Vect2D vel){
  if (ball.x > width-ball.r){
    ball.x = width-ball.r;
    vel.vx *= -1;
  } 
  else if (ball.x < ball.r){
    ball.x = ball.r;
    vel.vx *= -1;
  } 
  else if (ball.y > height-ball.r){
    ball.y = height-ball.r;
    vel.vy *= -1;
  } 
  else if (ball.y < ball.r){
    ball.y = ball.r;
    vel.vy *= -1;
  }
}

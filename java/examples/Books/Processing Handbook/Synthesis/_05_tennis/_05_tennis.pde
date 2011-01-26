/**
 * Synthesis 2: Input and Response
 * Tennis by Casey Reas (www.processing.org)
 * p. 256
 * 
 * Only the right paddle works. As a challenge, try to
 * add code to activate the left paddle. You can make decide to make
 * it a one-player or two-player game. As an additional challenge, 
 * have the program keep score.
 */

 
int ballX;
int ballY;
int ballDir = 1;
int ballSize = 10;  // Radius
float ballAngle = 0.0;  // Direction

// Global variables for the paddle
int paddleWidth = 20;
int paddleHeight = 40;

int wallGap = 50;

int netSegment;

void setup() {
  size(640, 480);
  noStroke();
  ballY = height/2;
  ballX = 0;
  noCursor();
  netSegment = height/32;
}

void draw()  {
  background(0);
  
  stroke(255);
  // Draw Net
  for(int i=0; i<height; i=i+netSegment) {
    line(width/2, i, width/2, i+netSegment/3);
  } 
  noStroke();
  
  
  ballX += ballDir * 2;
  ballY += ballAngle;
  
  if(ballX > width+ballSize*2) {
    ballX = -ballSize;
    ballY = int(random(0, height-ballSize));
    ballAngle = 0;
    println(ballX + ":" + ballY + ":" + ballAngle);
  }
  
  if(ballX < -ballSize*2) {
    ballX = width;
    ballY = int(random(0, height-ballSize));
    ballAngle = 0;
    println(ballX + ":" + ballY + ":" + ballAngle);
  }
  
  // Constrain paddle to screen
  float paddleY = constrain(mouseY, 0, height-paddleHeight);

  // Test to see if the ball is touching the paddle
  float py = width-wallGap-ballSize;
  if(ballX >= py && ( ballY+ballSize >= paddleY && ballY <= paddleY + paddleHeight)) {
       
    ballDir *= -1;
    if(mouseY != pmouseY) {
      ballAngle = (mouseY-pmouseY)/2.0;
      ballAngle = constrain(ballAngle, -5, 5);
    }
  } 
  
  // If ball hits back wall, reverse direction
  if(ballX < 0) {
    ballDir *= -1;
  }
  
  // If the ball is touching top or bottom edge, reverse direction
  if((ballY > height-ballSize) || (ballY < 0)) {
    ballAngle = ballAngle * -1;
  }

  // Draw ball
  fill(255);
  rect(ballX, ballY, ballSize, ballSize);
  
  // Draw the paddle
  fill(153);
  rect(width-wallGap, paddleY, paddleWidth, paddleHeight);  

  // Draw the paddle
  rect(wallGap, height-paddleY-paddleHeight, paddleWidth, paddleHeight); 
}


void keyPressed() {
  //saveFrame("pong-####.tif"); 
}

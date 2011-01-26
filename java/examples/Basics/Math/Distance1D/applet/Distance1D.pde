/**
 * Distance 1D. 
 * 
 * Move the mouse left and right to control the 
 * speed and direction of the moving shapes. 
 */
 
int thin = 8;
int thick = 36;
float xpos1 = 134.0;
float xpos2 = 44.0;
float xpos3 = 58.0;
float xpos4 = 120.0;

void setup() 
{
  size(200, 200);
  noStroke();
  frameRate(60);
}

void draw() 
{
  background(0);
  
  float mx = mouseX * 0.4 - width/5.0;
  
  fill(102);
  rect(xpos2, 0, thick, height/2);
  fill(204);
  rect(xpos1, 0, thin, height/2);
  fill(102);
  rect(xpos4, height/2, thick, height/2);
  fill(204);
  rect(xpos3, height/2, thin, height/2);
	
  xpos1 += mx/16;
  xpos2 += mx/64;
  xpos3 -= mx/16;
  xpos4 -= mx/64;
  
  if(xpos1 < -thin)  { xpos1 =  width; }
  if(xpos1 >  width) { xpos1 = -thin; }
  if(xpos2 < -thick) { xpos2 =  width; }
  if(xpos2 >  width) { xpos2 = -thick; }
  if(xpos3 < -thin)  { xpos3 =  width; }
  if(xpos3 >  width) { xpos3 = -thin; }
  if(xpos4 < -thick) { xpos4 =  width; }
  if(xpos4 >  width) { xpos4 = -thick; }
}


/**
 * Springs. 
 * 
 * Move the mouse over one of the circles and click to re-position. 
 * When you release the mouse, it will snap back into position. 
 * Each circle has a slightly different behavior.  
 */
 

int num = 3; 
Spring[] springs = new Spring[num]; 

void setup()
{
  size(640, 360);
  noStroke(); 
  smooth();
  springs[0] = new Spring(240, 260,  40, 0.98, 8.0, 0.1, springs, 0); 
  springs[1] = new Spring(320, 210, 120, 0.95, 9.0, 0.1, springs, 1); 
  springs[2] = new Spring(180, 170, 200, 0.90, 9.9, 0.1, springs, 2);   
}

void draw() 
{
  background(51); 
  
  for (int i = 0; i < num; i++) { 
    springs[i].update(); 
    springs[i].display(); 
  }  
}

void mousePressed() 
{
  for (int i = 0; i < num; i++) { 
    springs[i].pressed(); 
  } 
}

void mouseReleased() 
{
  for (int i=0; i<num; i++) { 
    springs[i].released(); 
  } 
}

class Spring 
{ 
  // Screen values 
  float xpos, ypos;
  float tempxpos, tempypos; 
  int size = 20; 
  boolean over = false; 
  boolean move = false; 

  // Spring simulation constants 
  float mass;       // Mass 
  float k = 0.2;    // Spring constant 
  float damp;       // Damping 
  float rest_posx;  // Rest position X 
  float rest_posy;  // Rest position Y 

  // Spring simulation variables 
  //float pos = 20.0; // Position 
  float velx = 0.0;   // X Velocity 
  float vely = 0.0;   // Y Velocity 
  float accel = 0;    // Acceleration 
  float force = 0;    // Force 

  Spring[] friends;
  int me;
  
  // Constructor
  Spring(float x, float y, int s, float d, float m, 
         float k_in, Spring[] others, int id) 
  { 
    xpos = tempxpos = x; 
    ypos = tempypos = y;
    rest_posx = x;
    rest_posy = y;
    size = s;
    damp = d; 
    mass = m; 
    k = k_in;
    friends = others;
    me = id; 
  } 

  void update() 
  { 
    if (move) { 
      rest_posy = mouseY; 
      rest_posx = mouseX;
    } 

    force = -k * (tempypos - rest_posy);  // f=-ky 
    accel = force / mass;                 // Set the acceleration, f=ma == a=f/m 
    vely = damp * (vely + accel);         // Set the velocity 
    tempypos = tempypos + vely;           // Updated position 

    force = -k * (tempxpos - rest_posx);  // f=-ky 
    accel = force / mass;                 // Set the acceleration, f=ma == a=f/m 
    velx = damp * (velx + accel);         // Set the velocity 
    tempxpos = tempxpos + velx;           // Updated position 

    
    if ((overEvent() || move) && !otherOver() ) { 
      over = true; 
    } else { 
      over = false; 
    } 
  } 
  
  // Test to see if mouse is over this spring
  boolean overEvent() {
    float disX = tempxpos - mouseX;
    float disY = tempypos - mouseY;
    if (sqrt(sq(disX) + sq(disY)) < size/2 ) {
      return true;
    } else {
      return false;
    }
  }
  
  // Make sure no other springs are active
  boolean otherOver() {
    for (int i=0; i<num; i++) {
      if (i != me) {
        if (friends[i].over == true) {
          return true;
        }
      }
    }
    return false;
  }

  void display() 
  { 
    if (over) { 
      fill(153); 
    } else { 
      fill(255); 
    } 
    ellipse(tempxpos, tempypos, size, size);
  } 

  void pressed() 
  { 
    if (over) { 
      move = true; 
    } else { 
      move = false; 
    }  
  } 

  void released() 
  { 
    move = false; 
    rest_posx = xpos;
    rest_posy = ypos;
  } 
} 

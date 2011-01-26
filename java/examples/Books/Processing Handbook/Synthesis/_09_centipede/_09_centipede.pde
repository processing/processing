/**
 * Synthesis 3: Motion and Arrays
 * Centipede by Ariel Malka (www.chronotext.org)
 * p. 372
 */


float x, y;
float node_length = 30;
float node_size = node_length-1;
int n_nodes = 70;
float[] nodes_x;
float[] nodes_y;
float delay = 20;
color col_head = color(255, 0, 0);
color col_body = color(0);


void setup()
{
  size(600, 600);
  smooth();
  noStroke();

  x = width/2;
  y = height/2;

  int r1 = 10;
  int r2 = 100;
  int dr = r2-r1;
  float D = 0;
  
  nodes_x = new float[n_nodes];
  nodes_y = new float[n_nodes];
  
  for (int i=0; i<n_nodes; i++) {
    float r = sqrt(r1 * r1 + 2.0 * dr * D);
    float d = (r - r1) / dr;

    nodes_x[i] = x - sin(d) * r;
    nodes_y[i] = y + cos(d) * r;

    D += node_length;
  }
}


void draw()
{
  background(204);
  
  // Set the position of the head
  setTarget(mouseX, mouseY);
  
  // Draw the head
  fill(col_head);
  ellipse(nodes_x[0], nodes_y[0], node_size, node_size);
  
  // Draw the body
  fill(col_body);
  for (int i=1; i < n_nodes; i++) {
    ellipse(nodes_x[i], nodes_y[i], node_size, node_size);
  }
}


void setTarget(float tx, float ty)
{
  // Motion interpolation for the head
  x += (tx - x) / delay;
  y += (ty - y) / delay;
  nodes_x[0] = x;
  nodes_y[0] = y;
 
  // Constrained motion for the other nodes
  for (int i=1; i < n_nodes; i++)
  {
    float dx = nodes_x[i - 1] - nodes_x[i];
    float dy = nodes_y[i - 1] - nodes_y[i];
    float len = sqrt(sq(dx) + sq(dy));
    nodes_x[i] = nodes_x[i - 1] - (dx/len * node_length);
    nodes_y[i] = nodes_y[i - 1] - (dy/len * node_length);
  }
}


void keyPressed() {
  // saveFrame("centipede-####.tif"); 
}


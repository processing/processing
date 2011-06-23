// Esfera, by David Pena.  
// Uniform random distribution over the surface of a sphere.
//
// PERFORMANCE ON ANDROID of this example is very low because 
// line drawing in A3D needs to be reimplemented. See this bug 
// report: http://code.google.com/p/processing/issues/detail?id=123

int cuantos = 1000;
pelo[] lista ;
float[] z = new float[cuantos]; 
float[] phi = new float[cuantos]; 
float[] largos = new float[cuantos]; 
float radio = 200;
float rx = 0;
float ry =0;

void setup() {
  size(480, 800, P3D);
  orientation(PORTRAIT);
  
  radio = height/3.5;
  
  lista = new pelo[cuantos];
  for (int i=0; i<cuantos; i++){
    lista[i] = new pelo();
  }
  noiseDetail(3);

  //autoNormal(false);
}

void draw() {
  background(0);
  translate(width/2,height/2);

  float rxp = ((mouseX-(width/2))*0.005);
  float ryp = ((mouseY-(height/2))*0.005);
  rx = (rx*0.9)+(rxp*0.1);
  ry = (ry*0.9)+(ryp*0.1);

  rotateY(rx);
  rotateX(ry);
  fill(0);
  noStroke();
  sphere(radio);

  for (int i=0;i<cuantos;i++){
    lista[i].dibujar();
  }
}


class pelo
{
  float z = random(-radio,radio);
  float phi = random(TWO_PI);
  float largo = random(1.15,1.2);
  float theta = asin(z/radio);

    void dibujar(){

    float off = (noise(millis() * 0.0005,sin(phi))-0.5) * 0.3;
    float offb = (noise(millis() * 0.0007,sin(z) * 0.01)-0.5) * 0.3;

    float thetaff = theta+off;
    float phff = phi+offb;
    float x = radio * cos(theta) * cos(phi);
    float y = radio * cos(theta) * sin(phi);
    float z = radio * sin(theta);
    float msx= screenX(x,y,z);
    float msy= screenY(x,y,z);

    float xo = radio * cos(thetaff) * cos(phff);
    float yo = radio * cos(thetaff) * sin(phff);
    float zo = radio * sin(thetaff);

    float xb = xo * largo;
    float yb = yo * largo;
    float zb = zo * largo;
    
    beginShape(LINES);
    stroke(0);
    vertex(x,y,z);
    stroke(200,150);
    vertex(xb,yb,zb);
    endShape();
  }
}

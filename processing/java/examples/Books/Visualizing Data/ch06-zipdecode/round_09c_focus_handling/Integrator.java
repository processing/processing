// Code from Visualizing Data, First Edition, Copyright 2008 Ben Fry.


public class Integrator {

  static final float DAMPING = 0.5f;  // formerly 0.9f
  static final float ATTRACTION = 0.2f;  // formerly 0.1f

  float value = 0;
  float vel   = 0;
  float accel = 0;
  float force = 0;
  float mass  = 1;

  float damping; //     = DAMPING;
  float attraction; //  = ATTRACTION;
  
  boolean targeting; // = false;
  float target; //      = 0;


  public Integrator() { 
    this.value = 0;
    this.damping = DAMPING;
    this.attraction = ATTRACTION;
  }


  public Integrator(float value) {
    this.value = value;
    this.damping = DAMPING;
    this.attraction = ATTRACTION;
  }


  public Integrator(float value, float damping, float attraction) {
    this.value = value;
    this.damping = damping;
    this.attraction = attraction;
  }


  public void set(float v) {
    value = v;
    //targeting = false  ?
  }


  public boolean update() {  // default dtime = 1.0
    if (targeting) {
      force += attraction * (target - value);      
    }

    accel = force / mass;
    vel = (vel + accel) * damping; /* e.g. 0.90 */
    value += vel;

    force = 0; // implicit reset

    return (vel > 0.0001f);
  }


  public void target(float t) {
    targeting = true;
    target = t;
  }


  public void noTarget() {
    targeting = false;
  }
}


  /*
  public void attraction(float targetValue, float a) {
    force += attraction * (targetValue - value);
  }

  public void attract(float target, float a) {
    attraction(target, a);
    update();
  }

  public void setDecay(float d) {
    kDecay = d;
  }

  public void decay() {
    force -= kDecay * value;
  }

  public void decay(float d) {
    force -= d * value;
  }

  public void setImpulse(float i) {
    kImpulse = i;
  }

  public void impulse() {
    //printf("kimpulse is %f\n", kImpulse);
    force += kImpulse;
    //decay(-kImpulse);  // lazy
  }

  public void impulse(float i) {
    force += i;
    //decay(-i);  // lazy
  }

  public void setDamping(float d) {
    kDamping = d;
  }

  public void noise(float amount) {
    force += (float) ((Math.random() * 2) - 1) * amount;
  }

  public void add(float v) {
    value += v;
  }
  
  public void add(Integrator integrator) {
    value += integrator.value;
  }
  */



/*

void Integrator1f::updateRK() {  // default dtime = 1.0
#define H 0.001
  float f1 = force;
  float f2 = force + H*f1/2;
  float f3 = force + H*f2/2;
  float f4 = force + H*f3;
  velocity = velocity + (H/6)*(f1 + 2*f2 + 2*f3 + f4);
}

  eval(x) is the force
  i think x should be time, so x is normally 1.0.
  if dtime were incorporated, that would probably work
  >> need correct function for force and dtime

	double f1 = fn.evalX(x);
	double f2 = fn.evalX(x + h*f1/2);
	double f3 = fn.evalX(x + h*f2/2);
	double f4 = fn.evalX(x + h*f3);

	out = x + (h/6)*(f1 + 2*f2 + 2*f3 + f4);
*/

/*

    public void step(double t, double x, double y,
		     Function fn, double h, double out[]) {
	double f1 = fn.evalX(t, x, y);
	double g1 = fn.evalY(t, x, y);

	double f2 = fn.evalX(t + h/2, x + h*f1/2, y + h*g1/2);
	double g2 = fn.evalY(t + h/2, x + h*f1/2, y + h*g1/2);

	double f3 = fn.evalX(t + h/2, x + h*f2/2, y + h*g2/2);
	double g3 = fn.evalY(t + h/2, x + h*f2/2, y + h*g2/2);
	
	double f4 = fn.evalX(t + h, x + h*f3, y + h*g3);
	double g4 = fn.evalY(t + h, x + h*f3, y + h*g3);

	out[0] = x + (h/6)*(f1 + 2*f2 + 2*f3 + f4);
	out[1] = y + (h/6)*(g1 + 2*g2 + 2*g3 + g4);
    }
*/

//void Integrator1f::update(float dtime) {
//velocity += force * dtime;
//  value += velocity*dtime  +  0.5f*force*dtime*dtime;
//force = 0;
//}

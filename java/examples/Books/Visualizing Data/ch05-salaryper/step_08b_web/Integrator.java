// Code from Visualizing Data, First Edition, Copyright 2008 Ben Fry.


public class Integrator {

  static final float DAMPING = 0.5f;  // formerly 0.9f
  static final float ATTRACTION = 0.2f;  // formerly 0.1f

  public float value   = 0;
  public float vel     = 0;
  public float accel   = 0;
  public float force   = 0;
  public float mass    = 1;

  public float damping;
  public float attraction;
  public boolean targeting;
  public float target;

  public float prev = Float.MAX_VALUE;
  public float epsilon = 0.0001f;


  public Integrator() {
    this(0, DAMPING, ATTRACTION);
  }


  public Integrator(float value) {
    this(value, DAMPING, ATTRACTION);
  }


  public Integrator(float value, float damping, float attraction) {
    this.value = value;
    this.damping = damping;
    this.attraction = attraction;
  }


  public void set(float v) {
    value = v;
  }


  /**
   * Update for next time step.
   * Returns true if actually updated, false if no longer changing.
   */
  public boolean update() {
    if (targeting) {
      force += attraction * (target - value);
    }

    accel = force / mass;
    vel = (vel + accel) * damping;
    value += vel;

    force = 0;

    if (Math.abs(value - prev) < epsilon) {
      value = target;
      return false;
    }
    prev = value;
    return true;
  }


  public void target(float t) {
    targeting = true;
    target = t;
  }


  public void noTarget() {
    targeting = false;
  }
}

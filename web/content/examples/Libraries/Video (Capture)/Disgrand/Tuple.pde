// Simple vector class that holds an x,y,z position.

class Tuple {
  float x, y, z;

  public Tuple() { }

  public Tuple(float x, float y, float z) {
    set(x, y, z);
  }

  public void set(float x, float y, float z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }
  
  public void target(Tuple another, float amount) {
    float amount1 = 1.0 - amount;
    x = x*amount1 + another.x*amount;
    y = y*amount1 + another.y*amount;
    z = z*amount1 + another.z*amount;
  }
  
  public void phil() {
    fill(x, y, z);
  }
}


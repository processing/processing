// Simple vector class that holds an x,y,z position.

class Vec3f {
  float x, y, z;

  public Vec3f() { }

  public Vec3f(float x, float y, float z) {
    set(x, y, z);
  }

  public void set(float x, float y, float z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public void scale(float amt) {
    x *= amt;
    y *= amt;
    z *= amt;
  }
}

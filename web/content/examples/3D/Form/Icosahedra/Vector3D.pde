class Vector3D {
  
  float x, y, z;
  float[]origVals;

  Vector3D() { }

  Vector3D(float x, float y, float z) {
    this.x = x;
    this.y = y;
    this.z = z;

    // capture original values
    origVals  = new float[]{ 
      x, y, z     };
  }

  //methods
  void add(Vector3D v) {
    x+=v.x;
    y+=v.y;
    z+=v.z;
  }

  void subtract(Vector3D v) {
    x-=v.x;
    y-=v.y;
    z-=v.z;
  }

  void multiply(float s) {
    x*=s;
    y*=s;
    z*=s;
  }

  void divide(float s) {
    x/=s;
    y/=s;
    z/=s;
  }

  Vector3D getAverage(Vector3D v) {
    Vector3D u = new Vector3D();
    u.x = (x+v.x)/2;
    u.y = (y+v.y)/2;
    u.z = (z+v.z)/2;
    return u;
  }

  void setTo(Vector3D v) {
    x = v.x;
    y = v.y;
    z = v.z;
  }

  void reset() {
    x = origVals[0];
    y = origVals[1];
    z = origVals[2];
  }

  float getDotProduct(Vector3D v) {
    return x*v.x + y*v.y + z*v.z;
  }

  Vector3D getCrossProduct(Vector3D v, Vector3D u) {
    Vector3D v1 = new Vector3D(v.x-x, v.y-y, v.z-z);
    Vector3D v2 = new Vector3D(u.x-x, u.y-y, u.z-z);
    float xx = v1.y*v2.z-v1.z*v2.y;
    float yy = v1.z*v2.x-v1.x*v2.z;
    float zz = v1.x*v2.y-v1.y*v2.x;
    return new Vector3D(xx, yy, zz);
  }

  Vector3D getNormal(Vector3D v, Vector3D u) {
    Vector3D n = getCrossProduct(v, u);
    n.normalize();
    return(n);
  }

  void normalize() {
    float m = getMagnitude();
    x/=m;
    y/=m;
    z/=m;
  }

  public float getMagnitude() {
    return sqrt(x*x+y*y+z*z);
  }
}

class Vec3f {
  float x;
  float y;
  float p;  // Pressure

  Vec3f() {
    set(0, 0, 0);
  }
  
  Vec3f(float ix, float iy, float ip) {
    set(ix, iy, ip);
  }

  void set(float ix, float iy, float ip) {
    x = ix;
    y = iy;
    p = ip;
  }
}

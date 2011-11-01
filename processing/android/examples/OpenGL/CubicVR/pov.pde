// my basic Point Of View camera class.

class POV{
  float x,y,z;

  float elevation;
  float azimuth;
  float distance;

  POV (float sentDistance){
    distance     = sentDistance;
    x            = xMid;
    y            = yMid;
    azimuth      = 0;
    elevation    = 0;
  }

  void exist(){
    setPosition();
    setCamera();
  }

  void setPosition(){
    x -= (x - xMid) * .2;
    y -= (y - yMid) * .2;
    if (outside){
      z -= (z + distance) * .2;
    } else {
      z -= (z - distance) * .2;
    }
  }

  void setCamera(){
    translate(x, y, z);
    rotateY(elevation);
    rotateZ(-azimuth);
  }
}

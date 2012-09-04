
void renderImage(PImage img, Vec3D _loc, float _diam, color _col, float _alpha ) {
  pushMatrix();
  translate( _loc.x, _loc.y, _loc.z );
  pov.glReverseCamera();
  tint(red(_col), green(_col), blue(_col), _alpha);
  imageMode(CENTER);
  image(img,0,0,_diam,_diam);
  popMatrix();
}

void renderImageOnFloor(PImage img,  Vec3D _loc, float _diam, color _col, float _aa ) {
  pushMatrix();
  translate( _loc.x, _loc.y, _loc.z );
  rotateX(PI/2);
  //pov.glReverseCamera();
  tint(red(_col), green(_col), blue(_col), _aa);
  imageMode(CENTER);
  image(img,0,0,_diam,_diam);
  popMatrix();
}

void renderImageAndRotate(PImage img, Vec3D _loc, float _diam, color _col, float _aa, float _rot ) {
  pushMatrix();
  translate( _loc.x, _loc.y, _loc.z );
  pov.glReverseCamera();
  tint(red(_col), green(_col), blue(_col), _aa);
  imageMode(CENTER);
  image(img,0,0,_diam,_diam);
  popMatrix();
}

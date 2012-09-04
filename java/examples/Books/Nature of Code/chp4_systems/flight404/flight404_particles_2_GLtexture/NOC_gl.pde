
int squareList;
void initGL(){
  pgl.beginGL();
  squareList = gl.glGenLists(1);
  gl.glNewList(squareList, GL.GL_COMPILE);
  gl.glBegin(GL.GL_POLYGON);
  gl.glTexCoord2f(0, 0);    gl.glVertex2f(-.5, -.5);
  gl.glTexCoord2f(1, 0);    gl.glVertex2f( .5, -.5);
  gl.glTexCoord2f(1, 1);    gl.glVertex2f( .5,  .5);
  gl.glTexCoord2f(0, 1);    gl.glVertex2f(-.5,  .5);
  gl.glEnd();
  gl.glEndList();
  pgl.endGL();
}

void renderImage( Vec3D _loc, float _diam, color _col, float _alpha ){
  gl.glPushMatrix();
  gl.glTranslatef( _loc.x, -_loc.y, _loc.z );
  pov.glReverseCamera();
  gl.glScalef( _diam, _diam, _diam );
  gl.glColor4f( red(_col), green(_col), blue(_col), _alpha );
  gl.glCallList( squareList );
  gl.glPopMatrix();
}

// This will allow you to draw images that are oriented to the floor plane.
void renderImageOnFloor( Vec3D _loc, float _diam, color _col, float _aa ){
  gl.glPushMatrix();
  gl.glTranslatef( _loc.x, -_loc.y, _loc.z );
  gl.glScalef( _diam, _diam, _diam );
  gl.glRotatef( 90, 1.0, 0.0, 0.0 );
  gl.glColor4f( red(_col), green(_col), blue(_col), _aa );
  gl.glCallList( squareList );
  gl.glPopMatrix();
}

// This will allow you to specify a rotation for images that are oriented perpendicular to the eyeNormal 
// which is the vector pointing from the camera's eye to the camera's point of interest.
void renderImageAndRotate( Vec3D _loc, float _diam, color _col, float _aa, float _rot ){
  gl.glPushMatrix();
  gl.glTranslatef( _loc.x, -_loc.y, _loc.z );
  gl.glRotatef( degrees( _rot ), pov.eyeNormal.x, pov.eyeNormal.y, pov.eyeNormal.z );
  pov.glReverseCamera();
  gl.glScalef( _diam, _diam, _diam );
  gl.glColor4f( red(_col), green(_col), blue(_col), _aa );
  gl.glCallList( squareList );
  gl.glPopMatrix();
}

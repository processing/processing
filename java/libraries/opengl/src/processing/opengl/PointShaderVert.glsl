attribute vec2 vertDisp;
 
void main() {
  vec4 pos = gl_ModelViewMatrix * gl_Vertex;
  pos.xy += vertDisp.xy;
  gl_Position = gl_ProjectionMatrix * pos;
  gl_FrontColor = gl_Color;
}
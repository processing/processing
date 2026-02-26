attribute vec3 position;
attribute vec4 color;
attribute vec2 texCoord;
attribute float texFactor;

varying vec4 vertColor;
varying vec2 vertTexCoord;
varying float vertTexFactor;

uniform mat4 transform;
uniform vec2 texScale;

void main() {
  gl_Position = transform * vec4(position, 1);

  //we avoid affecting the Z component by the transform
  //because it would mess up our depth testing
  gl_Position.z = position.z;

  vertColor = color.zyxw;
  vertTexCoord = texCoord * texScale;
  vertTexFactor = texFactor;
}

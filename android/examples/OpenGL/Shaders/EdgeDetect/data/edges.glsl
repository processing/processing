// Edge detection shader

precision mediump float;

uniform sampler2D textureSampler;

// The inverse of the texture dimensions along X and Y
uniform vec2 texcoordOffset;

varying vec4 vertColor;
varying vec4 vertTexcoord;

void main() {
  vec4 sum = vec4(0);

  float kernel[9];
  kernel[0] = -1.0; kernel[1] = -1.0; kernel[2] = -1.0;
  kernel[3] = -1.0; kernel[4] = +8.0; kernel[5] = -1.0;
  kernel[6] = -1.0; kernel[7] = -1.0; kernel[8] = -1.0;

  vec2 offset[9];
  offset[0] = vec2(-texcoordOffset.s, -texcoordOffset.t);
  offset[1] = vec2(              0.0, -texcoordOffset.t);
  offset[2] = vec2(+texcoordOffset.s, -texcoordOffset.t);

  offset[3] = vec2(-texcoordOffset.s, 0.0);
  offset[4] = vec2(              0.0, 0.0);
  offset[5] = vec2(+texcoordOffset.s, 0.0);

  offset[6] = vec2(-texcoordOffset.s, +texcoordOffset.t);
  offset[7] = vec2(              0.0, +texcoordOffset.t);
  offset[8] = vec2(+texcoordOffset.s, +texcoordOffset.t);

  for (int i = 0; i < 9; i++) {
    vec4 tmp = texture2D(textureSampler, vertTexcoord.st + offset[i]);
    sum += tmp * kernel[i];
  }

  gl_FragColor = vec4(sum.rgb, 1.0);
}
uniform sampler2D textureSampler;
uniform sampler2D maskSampler;

uniform vec2 texcoordOffset;
varying vec4 vertColor;
varying vec4 vertTexcoord;

void main() {
  vec4 texColor = texture2D(textureSampler, vertTexcoord.st).rgba;
  vec4 maskColor = texture2D(maskSampler, vec2(vertTexcoord.s, 1.0 - vertTexcoord.t)).rgba;
  gl_FragColor = mix(texColor, vec4(0, 0, 0, 0), 1.0 - maskColor.r);  
}
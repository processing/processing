#ifdef GL_ES
precision mediump float;
precision mediump int;
#endif

varying vec4 vertColor;
varying vec2 vertTexCoord;
varying float vertTexFactor;

uniform sampler2D texture;

void main() {
  gl_FragColor = mix(vertColor, vertColor * texture2D(texture, vertTexCoord), vertTexFactor);
}

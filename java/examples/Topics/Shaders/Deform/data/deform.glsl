#ifdef GL_ES
precision mediump float;
precision mediump int;
#endif

#define PROCESSING_TEXTURE_SHADER

uniform sampler2D texture;

uniform float time;
uniform vec2 resolution;
uniform vec2 mouse;

void main(void) {
  vec2 p = -1.0 + 2.0 * gl_FragCoord.xy / resolution.xy;
  vec2 m = -1.0 + 2.0 * mouse.xy / resolution.xy;

  float a1 = atan(p.y - m.y, p.x - m.x);
  float r1 = sqrt(dot(p - m, p - m));
  float a2 = atan(p.y + m.y, p.x + m.x);
  float r2 = sqrt(dot(p + m, p + m));

  vec2 uv;
  uv.x = 0.2 * time + (r1 - r2) * 0.25;
  uv.y = sin(2.0 * (a1 - a2));

  float w = r1 * r2 * 0.8;
  vec3 col = texture2D(texture, 0.5 - 0.495 * uv).xyz;

  gl_FragColor = vec4(col / (0.1 + w), 1.0);
}
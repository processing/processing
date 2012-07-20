// Copyright (C) 2007 Dave Griffiths
// Licence: GPLv2 (see COPYING)
// Fluxus Shader Library
// ---------------------
// BadPrint NPR Shader
// This shader tries to emulate the effect
// of a bad printing process. Can be controlled
// with different settings for RGB

uniform vec3 Scale;
uniform vec3 Offset;
uniform vec3 Register;
uniform vec3 Size;

varying vec3 N;
varying vec3 P;
varying vec4 S;
varying vec3 L;

void main() { 
  vec3 l = normalize(L);
  vec3 n = normalize(N);
  vec2 p = S.xy;

  vec2 sr = p * Size.r + Register.r;
  vec2 sg = p * Size.g + Register.g;
  vec2 sb = p * Size.b + Register.b;

  float diffuse = dot(l,n);
    
  float r = (sin(sr.x) + cos(sr.y)) * Scale.r + Offset.r + diffuse;
  float g = (sin(sg.x) + cos(sg.y)) * Scale.g + Offset.g + diffuse;
  float b = (sin(sb.x) + cos(sb.y)) * Scale.b + Offset.b + diffuse;
 
  gl_FragColor = vec4(step(0.5, r), step(0.5, g), step(0.5, b), 1);
}

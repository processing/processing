/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-15 The Processing Foundation
  Copyright (c) 2004-12 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation, version 2.1.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
*/

uniform mat4 projectionMatrix;
uniform mat4 modelviewMatrix;
 
uniform vec4 viewport;
uniform int perspective; 
 
attribute vec4 position;
attribute vec4 color;
attribute vec2 offset;

varying vec4 vertColor;

vec4 windowToClipVector(vec2 window, vec4 viewport, float clipw) {
  vec2 xypos = (window / viewport.zw) * 2.0;
  return vec4(xypos, 0.0, 0.0) * clipw;
}  

void main() {
  vec4 pos = modelviewMatrix * position;
  vec4 clip = projectionMatrix * pos;
  
  if (0 < perspective) {
    // Perspective correction (points will look thiner as they move away 
    // from the view position).
    gl_Position = clip + projectionMatrix * vec4(offset.xy, 0, 0);
  } else {
    // No perspective correction.	
    vec4 offset = windowToClipVector(offset.xy, viewport, clip.w);
    gl_Position = clip + offset;
  }
  
  vertColor = color;
}
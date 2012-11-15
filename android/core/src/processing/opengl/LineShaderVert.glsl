/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2011-12 Ben Fry and Casey Reas

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License version 2.1 as published by the Free Software Foundation.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
 */

uniform mat4 modelviewMatrix;
uniform mat4 projectionMatrix;

uniform vec4 viewport;
uniform int perspective;
uniform vec3 scale;

attribute vec4 inVertex;
attribute vec4 inColor;
attribute vec4 inLine;

varying vec4 vertColor;

vec3 clipToWindow(vec4 clip, vec4 viewport) {
  vec3 post_div = clip.xyz / clip.w;
  vec2 xypos = (post_div.xy + vec2(1.0, 1.0)) * 0.5 * viewport.zw;
  return vec3(xypos, post_div.z * 0.5 + 0.5);
}
  
vec4 windowToClipVector(vec2 window, vec4 viewport, float clip_w) {
  vec2 xypos = (window / viewport.zw) * 2.0;
  return vec4(xypos, 0.0, 0.0) * clip_w;
}  
  
void main() {
  vec4 pos_p = inVertex;
  vec4 v_p = modelviewMatrix * pos_p;  
  // Moving vertices slightly toward the camera
  // to avoid depth-fighting with the fill triangles.
  // Discussed here:
  // http://www.opengl.org/discussion_boards/ubbthreads.php?ubb=showflat&Number=252848  
  v_p.xyz = v_p.xyz * scale;
  vec4 clip_p = projectionMatrix * v_p;
  float thickness = inLine.w;
  
  if (thickness != 0.0) {  
    vec4 pos_q = vec4(inLine.xyz, 1);
    vec4 v_q = modelviewMatrix * pos_q;
    v_q.xyz = v_q.xyz * scale;  
    vec4 clip_q = projectionMatrix * v_q; 
  
    vec3 window_p = clipToWindow(clip_p, viewport); 
    vec3 window_q = clipToWindow(clip_q, viewport); 
    vec3 tangent = window_q - window_p;
    
    vec2 perp = normalize(vec2(-tangent.y, tangent.x));
    vec2 window_offset = perp * thickness;

    if (0 < perspective) {
      // Perspective correction (lines will look thiner as they move away 
      // from the view position).  
      gl_Position.xy = clip_p.xy + window_offset.xy;
      gl_Position.zw = clip_p.zw;
    } else {
      // No perspective correction.	
      float clip_p_w = clip_p.w;
      vec4 offset_p = windowToClipVector(window_offset, viewport, clip_p_w);
      gl_Position = clip_p + offset_p;
    }  
  } else {
    gl_Position = clip_p;
  }
  
  vertColor = inColor;
}
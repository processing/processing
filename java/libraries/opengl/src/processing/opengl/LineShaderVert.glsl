attribute vec4 attribs;   
uniform vec4 viewport;

vec3 clipToWindow(vec4 clip, vec4 viewport) {
  vec3 post_div = clip.xyz / clip.w;
  vec2 xypos = (post_div.xy + vec2(1.0, 1.0)) * 0.5 * viewport.zw;
  return vec3(xypos, post_div.z * 0.5 + 0.5);
}
  
void main() {
  vec4 pos_p = gl_Vertex;
  vec4 pos_q = vec4(attribs.xyz, 1);  
  vec4 v_p = gl_ModelViewMatrix * pos_p;
  v_p.xyz = v_p.xyz * 0.99;   
  
  vec4 clip_p = gl_ProjectionMatrix * v_p; 
  vec4 v_q = gl_ModelViewMatrix * pos_q;
  v_q.xyz = v_q.xyz * 0.99;
  
  vec4 clip_q = gl_ProjectionMatrix * v_q; 
  vec3 window_p = clipToWindow(clip_p, viewport); 
  vec3 window_q = clipToWindow(clip_q, viewport); 
  vec3 tangent = window_q - window_p;
  
  float segment_length = length(tangent.xy);  
  vec2 perp = normalize(vec2(-tangent.y, tangent.x));
  float thickness = attribs.w;
  vec2 window_offset = perp * thickness;
  
  gl_Position.xy = clip_p.xy + window_offset.xy;
  gl_Position.zw = clip_p.zw;
  gl_FrontColor = gl_Color;
}
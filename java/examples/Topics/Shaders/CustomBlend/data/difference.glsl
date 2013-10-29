uniform sampler2D destSampler;
uniform sampler2D srcSampler;

uniform ivec2 destSize;
uniform ivec4 destRect;

uniform ivec2 srcSize;
uniform ivec4 srcRect;

varying vec4 vertTexCoord;

void main() {
  vec2 st = vertTexCoord.st;   
  
  vec2 dest = vec2(destRect.xy) / vec2(destSize) + st * vec2(destRect.zw) / vec2(destSize);
  vec2 src = vec2(srcRect.xy) / vec2(srcSize) + st * vec2(srcRect.zw) / vec2(srcSize); 
  
  vec3 destColor = texture2D(destSampler, dest).rgb;
  vec3 srcColor = texture2D(srcSampler, src).rgb;
  
  gl_FragColor = vec4(abs(srcColor - destColor), 1.0);  
}
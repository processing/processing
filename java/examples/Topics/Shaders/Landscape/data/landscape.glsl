 // Some hills...
// rotwang: @mod* lowered cam for better flight feeling
// @mod+ mouse y controls flight height
// @mod* some color tests
// @mod+ Canyon
// @emackey: Simple sky blue (no clouds...)
// @rotwang: mod* sky gradient, different terrain front and backcolor
// @mod* stripes texture
// @mod* terrain variation
// @psonice flyover adjusted so height depends on terrain. WOuld be better with spline interpolation, if you have the time go do it :)
#ifdef GL_ES
precision highp float;
#endif


uniform vec2 resolution;
uniform float time;
uniform vec2 mouse;
//Simple raymarching sandbox with camera

//Raymarching Distance Fields
//About http://www.iquilezles.org/www/articles/raymarchingdf/raymarchingdf.htm
//Also known as Sphere Tracing
//Original seen here: http://twitter.com/#!/paulofalcao/statuses/134807547860353024

//Util Start

mat2 m = mat2( 0.80,  0.60, -0.60,  0.80 );

float hash( float n )
{
    return fract(sin(n)*43758.5453);
}

float noise( in vec2 x )
{
    vec2 p = floor(x);
    vec2 f = fract(x);
    f = f*f*(3.0-2.0*f);
    float n = p.x + p.y*57.0;
    float res = mix(mix( hash(n+  0.0), hash(n+  1.0),f.x), mix( hash(n+ 57.0), hash(n+ 58.0),f.x),f.y);
    return res;
}

float fbm_5oct( vec2 p )
{
    float f = 0.0;
    f += 0.50000*noise( p ); p = m*p*2.02;
    f += 0.25000*noise( p ); p = m*p*2.03;
    f += 0.12500*noise( p ); p = m*p*2.01;
    f += 0.06250*noise( p ); p = m*p*2.04;
    f += 0.03125*noise( p );
    return f/0.984375;
}

float fbm_3oct( vec2 p )
{
    float f = 0.0;
    f += 0.50000*noise( p ); p = m*p*2.02;
    f += 0.25000*noise( p ); p = m*p*2.03;
    f += 0.12500*noise( p ); p = m*p*2.01;

    return f/0.875;
}

vec2 ObjUnion(in vec2 obj_floor,in vec2 obj_roundBox){
  if (obj_floor.x<obj_roundBox.x)
  	return obj_floor;
  else
  	return obj_roundBox;
}
//Util End

//Scene Start

//Torus
float torus(in vec3 p, in vec2 t){
	vec2 q = vec2(length(vec2(p.x,p.z))-t.x, p.y);
	return length(q) - t.y;
}

//Sphere
float sphere(in vec3 p, float radius){
	float length = sqrt(p.x*p.x + p.y*p.y + p.z*p.z);
	return length-radius;
}


vec2 terrain(in vec3 p){
	

	float da = sin(0.9*p.y)*fbm_3oct(p.xz); 
  	vec2 vd = vec2(p.y+fbm_3oct(p.xz / 9.0) * 9.33 ,0);
	vd.x += da;
 	
	return vd;
}

vec3 hsv2rgb(float h,float s,float v) {
	return mix(vec3(1.),clamp((abs(fract(h+vec3(3.,2.,1.)/3.)*6.-3.)-1.),0.,1.),s)*v;
}

/**
@rotwang:
smoothes between 2 vectors a and b (eg colors)
using a source value and a smooth amount
about the base as center.
*/
vec3 smoothmix(vec3 a, vec3 b, float base, float smooth, float source)
{
	float f = smoothstep(base-smooth, base+smooth, source );
	vec3 vec = mix(a, b, f);
	return vec;
}


//Terrain Color 
vec3 terrain_clr(in vec3 p){
	
	
	vec3 clr_a = vec3(0.4, 0.5, 0.2);
	vec3 clr_b = vec3(0.3, 0.3, 0.3);
	float m = fract(p.y*4.0);
	float g = fbm_3oct(p.xz * 28.0);
	clr_b += g;
	vec3 clr =  smoothmix(clr_a, clr_b,0.76, 0.3, m );
	clr = mix( clr, g*clr, 0.6);

	
 	return clr; 
}

//IQs RoundBox (try other objects http://www.iquilezles.org/www/articles/distfunctions/distfunctions.htm)
vec2 obj_roundBox(in vec3 p){
  return vec2(length(max(abs(p)-vec3(1,1,1),0.0))-0.25,1);
}

vec2 obj_sphere(in vec3 p){
  return vec2(length(p)-2.0);
}

//RoundBox with simple solid color
vec3 obj_roundBox_c(in vec3 p){
	return vec3(1.0,0.5,0.2);
}

//Objects union
vec2 inObj(in vec3 p){
  return terrain(p);
}

//Scene End

void main(void){
  //Camera animation
  vec3 U=vec3(0,1,0);//Camera Up Vector

	float speed = time*2.0;
	float my =  sin(time/8.0); // mouse.y*4.0;
	float camy = -1.0+my; 
	float tary = -1.0+my; 
  vec3 E=vec3(0., camy, speed);//vec3(-sin(time/4.0)*8.0,4,cos(time/4.0)*8.0); //Camera location; Change camera path position here
	E.y += .2;
  vec3 viewDir=vec3(E.x-sin(mouse.x*0.0),tary,E.z+cos(mouse.x*0.0)); //Change camere view vector here
  //vec3 E=vec3(mouse.x*0.0, 4, mouse.y*4.0); //Camera location; Change camera path position here
	
  //Camera setup
  vec3 C=normalize(viewDir-E);
  vec3 A=cross(C, U);
  vec3 B=cross(A, C);
  vec3 M=(E+C);
  
  vec2 vPos=2.0*gl_FragCoord.xy/resolution.xy - 1.0; // (2*Sx-1) where Sx = x in screen space (between 0 and 1)
  vec3 scrCoord=M + vPos.x*A*resolution.x/resolution.y + vPos.y*B; //normalize resolution in either x or y direction (ie resolution.x/resolution.y)
  vec3 scp=normalize(scrCoord-E);
 // scp.z += 1.;
  scp.y -=0.25;
  E.y -= (terrain(E).x + terrain(E + vec3(0., 0., 0.1)).x + terrain(E + vec3(0., 0., -0.1)).x) / 3.;
	E.y += 2.;
  //Raymarching
  const vec3 e=vec3(0.1,0,0);
  const float MAX_DEPTH=60.0; //Max depth0.

  vec2 s=vec2(0.1,0.0);
  vec3 c,p,n;

  float f=1.0;
  for(int i=0;i<100;i++){
    if (abs(s.x)<.01||f>MAX_DEPTH) break;
    f+=s.x;
    p=E+scp*f;
    s=inObj(p);
  }
  
  if (f<MAX_DEPTH){
    if (s.y==0.0)
      c=terrain_clr(p);
vec3 cc = vec3(0.7, c.g, c.b);
	 
	float m = smoothstep(3.0, 20.0, f);
	 c = mix(c,cc, 1.0-m);

	  
    n=normalize(
      vec3(s.x-inObj(p-e.xyy).x,
           s.x-inObj(p-e.yxy).x,
           s.x-inObj(p-e.yyx).x));
    float b=dot(n,normalize(E-p));
	  
    gl_FragColor=vec4(b*c*(2.0-f*.01),1.0);//simple phong LightPosition=CameraPosition
  }
	else {
		float invy = 1.0-vPos.y;
		float r = invy*0.8;
		float g = 0.2 + invy*0.7;
		vec3 sky_clr = vec3(r,g,0.9);
		gl_FragColor=vec4( sky_clr,1.0); //background color
	}
	
}
// Elevated shader
// https://www.shadertoy.com/view/MdX3Rr by inigo quilez

// Created by inigo quilez - iq/2013
// License Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.

// Processing port by Raphaël de Courville.

#ifdef GL_ES
precision highp float;
#endif

// Type of shader expected by Processing
#define PROCESSING_COLOR_SHADER

// Processing specific input
uniform float time;
uniform vec2 resolution;
uniform vec2 mouse;

// Layer between Processing and Shadertoy uniforms
vec3 iResolution = vec3(resolution,0.0);
float iGlobalTime = time;
vec4 iMouse = vec4(mouse,0.0,0.0); // zw would normally be the click status

// ------- Below is the unmodified Shadertoy code ----------
// Created by inigo quilez - iq/2013
// License Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.

//stereo thanks to Croqueteer
//#define STEREO 

mat3 m = mat3( 0.00,  0.80,  0.60,
              -0.80,  0.36, -0.48,
              -0.60, -0.48,  0.64 );

float hash( float n )
{
    return fract(sin(n)*43758.5453123);
}


float noise( in vec3 x )
{
    vec3 p = floor(x);
    vec3 f = fract(x);

    f = f*f*(3.0-2.0*f);

    float n = p.x + p.y*57.0 + 113.0*p.z;

    float res = mix(mix(mix( hash(n+  0.0), hash(n+  1.0),f.x),
                        mix( hash(n+ 57.0), hash(n+ 58.0),f.x),f.y),
                    mix(mix( hash(n+113.0), hash(n+114.0),f.x),
                        mix( hash(n+170.0), hash(n+171.0),f.x),f.y),f.z);
    return res;
}




vec3 noised( in vec2 x )
{
    vec2 p = floor(x);
    vec2 f = fract(x);

    vec2 u = f*f*(3.0-2.0*f);

    float n = p.x + p.y*57.0;

    float a = hash(n+  0.0);
    float b = hash(n+  1.0);
    float c = hash(n+ 57.0);
    float d = hash(n+ 58.0);
	return vec3(a+(b-a)*u.x+(c-a)*u.y+(a-b-c+d)*u.x*u.y,
				30.0*f*f*(f*(f-2.0)+1.0)*(vec2(b-a,c-a)+(a-b-c+d)*u.yx));

}

float noise( in vec2 x )
{
    vec2 p = floor(x);
    vec2 f = fract(x);

    f = f*f*(3.0-2.0*f);

    float n = p.x + p.y*57.0;

    float res = mix(mix( hash(n+  0.0), hash(n+  1.0),f.x),
                    mix( hash(n+ 57.0), hash(n+ 58.0),f.x),f.y);

    return res;
}

float fbm( vec3 p )
{
    float f = 0.0;

    f += 0.5000*noise( p ); p = m*p*2.02;
    f += 0.2500*noise( p ); p = m*p*2.03;
    f += 0.1250*noise( p ); p = m*p*2.01;
    f += 0.0625*noise( p );

    return f/0.9375;
}

mat2 m2 = mat2(1.6,-1.2,1.2,1.6);
	
float fbm( vec2 p )
{
    float f = 0.0;

    f += 0.5000*noise( p ); p = m2*p*2.02;
    f += 0.2500*noise( p ); p = m2*p*2.03;
    f += 0.1250*noise( p ); p = m2*p*2.01;
    f += 0.0625*noise( p );

    return f/0.9375;
}

float terrain( in vec2 x )
{
	vec2  p = x*0.003;
    float a = 0.0;
    float b = 1.0;
	vec2  d = vec2(0.0);
    for(int i=0;i<5; i++)
    {
        vec3 n = noised(p);
        d += n.yz;
        a += b*n.x/(1.0+dot(d,d));
		b *= 0.5;
        p=mat2(1.6,-1.2,1.2,1.6)*p;
    }

    return 140.0*a;
}

float terrain2( in vec2 x )
{
	vec2  p = x*0.003;
    float a = 0.0;
    float b = 1.0;
	vec2  d = vec2(0.0);
    for(int i=0;i<14; i++)
    {
        vec3 n = noised(p);
        d += n.yz;
        a += b*n.x/(1.0+dot(d,d));
		b *= 0.5;
        p=m2*p;
    }

    return 140.0*a;
}


float map( in vec3 p )
{
	float h = terrain(p.xz);
	
	float ss = 0.03;
	float hh = h*ss;
	float fh = fract(hh);
	float ih = floor(hh);
	fh = mix( sqrt(fh), fh, smoothstep(50.0,140.0,h) );
	h = (ih+fh)/ss;
	
    return p.y - h;
}

float map2( in vec3 p )
{
	float h = terrain2(p.xz);

	
	float ss = 0.03;
	float hh = h*ss;
	float fh = fract(hh);
	float ih = floor(hh);
	fh = mix( sqrt(fh), fh, smoothstep(50.0,140.0,h) );
	h = (ih+fh)/ss;
	
    return p.y - h;
}

bool jinteresct(in vec3 rO, in vec3 rD, out float resT )
{
    float h = 0.0;
    float t = 0.0;
	for( int j=0; j<120; j++ )
	{
        //if( t>2000.0 ) break;
		
	    vec3 p = rO + t*rD;
if( p.y>300.0 ) break;
        h = map( p );

		if( h<0.1 )
		{
			resT = t; 
			return true;
		}
		t += max(0.1,0.5*h);

	}

	if( h<5.0 )
    {
	    resT = t;
	    return true;
	}
	return false;
}

float sinteresct(in vec3 rO, in vec3 rD )
{
    float res = 1.0;
    float t = 0.0;
	for( int j=0; j<50; j++ )
	{
        //if( t>1000.0 ) break;
	    vec3 p = rO + t*rD;

        float h = map( p );

		if( h<0.1 )
		{
			return 0.0;
		}
		res = min( res, 16.0*h/t );
		t += h;

	}

	return clamp( res, 0.0, 1.0 );
}

vec3 calcNormal( in vec3 pos, float t )
{
	float e = 0.001;
	e = 0.001*t;
    vec3  eps = vec3(e,0.0,0.0);
    vec3 nor;
    nor.x = map2(pos+eps.xyy) - map2(pos-eps.xyy);
    nor.y = map2(pos+eps.yxy) - map2(pos-eps.yxy);
    nor.z = map2(pos+eps.yyx) - map2(pos-eps.yyx);
    return normalize(nor);
}

vec3 camPath( float time )
{
    vec2 p = 600.0*vec2( cos(1.4+0.37*time), 
                         cos(3.2+0.31*time) );

	return vec3( p.x, 0.0, p.y );
}

void main(void)
{
    vec2 xy = -1.0 + 2.0*gl_FragCoord.xy / iResolution.xy;

	vec2 s = xy*vec2(1.75,1.0);

	#ifdef STEREO
	float isCyan = mod(gl_FragCoord.x + mod(gl_FragCoord.y,2.0),2.0);
    #endif
	
    float time = iGlobalTime*.15;

	vec3 light1 = normalize( vec3(  0.4, 0.22,  0.6 ) );
	vec3 light2 = vec3( -0.707, 0.000, -0.707 );


	vec3 campos = camPath( time );
	vec3 camtar = camPath( time + 3.0 );
	campos.y = terrain( campos.xz ) + 15.0;
	camtar.y = campos.y*0.5;

	float roll = 0.1*cos(0.1*time);
	vec3 cw = normalize(camtar-campos);
	vec3 cp = vec3(sin(roll), cos(roll),0.0);
	vec3 cu = normalize(cross(cw,cp));
	vec3 cv = normalize(cross(cu,cw));
	vec3 rd = normalize( s.x*cu + s.y*cv + 1.6*cw );

	#ifdef STEREO
	campos += 2.0*cu*isCyan; // move camera to the right - the rd vector is still good
    #endif

	float sundot = clamp(dot(rd,light1),0.0,1.0);
	vec3 col;
    float t;
    if( !jinteresct(campos,rd,t) )
    {
     	col = 0.9*vec3(0.97,.99,1.0)*(1.0-0.3*rd.y);
		col += 0.2*vec3(0.8,0.7,0.5)*pow( sundot, 4.0 );
	}
	else
	{
		vec3 pos = campos + t*rd;

        vec3 nor = calcNormal( pos, t );

		float dif1 = clamp( dot( light1, nor ), 0.0, 1.0 );
		float dif2 = clamp( 0.2 + 0.8*dot( light2, nor ), 0.0, 1.0 );
		float sh = 1.0;
		if( dif1>0.001 ) 
			sh = sinteresct(pos+light1*20.0,light1);
		
		vec3 dif1v = vec3(dif1);
		dif1v *= vec3( sh, sh*sh*0.5+0.5*sh, sh*sh );

		float r = noise( 7.0*pos.xz );

        col = (r*0.25+0.75)*0.9*mix( vec3(0.10,0.05,0.03), vec3(0.13,0.10,0.08), clamp(terrain2( vec2(pos.x,pos.y*48.0))/200.0,0.0,1.0) );
		col = mix( col, 0.17*vec3(0.5,.23,0.04)*(0.50+0.50*r),smoothstep(0.70,0.9,nor.y) );
        col = mix( col, 0.10*vec3(0.2,.30,0.00)*(0.25+0.75*r),smoothstep(0.95,1.0,nor.y) );
  	    col *= 0.75;
         // snow
        #if 1
		float h = smoothstep(55.0,80.0,pos.y + 25.0*fbm(0.01*pos.xz) );
        float e = smoothstep(1.0-0.5*h,1.0-0.1*h,nor.y);
        float o = 0.3 + 0.7*smoothstep(0.0,0.1,nor.x+h*h);
        float s = h*e*o;
        s = smoothstep( 0.1, 0.9, s );
        col = mix( col, 0.4*vec3(0.6,0.65,0.7), s );
        #endif

		
		vec3 brdf  = 2.0*vec3(0.17,0.19,0.20)*clamp(nor.y,0.0,1.0);
		     brdf += 6.0*vec3(1.00,0.95,0.80)*dif1v;
		     brdf += 2.0*vec3(0.20,0.20,0.20)*dif2;

		col *= brdf;
		
		float fo = 1.0-exp(-pow(0.0015*t,1.5));
		vec3 fco = vec3(0.7) + 0.6*vec3(0.8,0.7,0.5)*pow( sundot, 4.0 );
		col = mix( col, fco, fo );
	}

	col = sqrt(col);

	vec2 uv = xy*0.5+0.5;
	col *= 0.7 + 0.3*pow(16.0*uv.x*uv.y*(1.0-uv.x)*(1.0-uv.y),0.1);
	
    #ifdef STEREO	
    col *= vec3( isCyan, 1.0-isCyan, 1.0-isCyan );	
	#endif
	
	gl_FragColor=vec4(col,1.0);
}

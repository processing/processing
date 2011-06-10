/**
 *    This example shows you how to find and use the browsers geo
 *    location services.<br />
 *    <br />
 *    A message should pop up asking for permission to access your 
 *    location data.
 */
 
GeoLocation loc = null;
float pos_x, pos_y;

boolean glNotAvail = false;
String errorMessage = "";

void setup ()
{
    size(300, 200);
    textFont(createFont("Arial", 12));
}

void draw ()
{
    background(255);
    
    if ( glNotAvail ) // failed
    {
        textAlign(CENTER);
        fill( 255, 0, 0 );
        text( "Something went wrong:\n" + errorMessage, 
              width/4, height/4, width/2, height/2 );
    }
    else if ( loc == null ) // waiting for location to come in ..
    {
        float w = 5 + (sin(frameCount/60.0) + 1) * (height / 3);
        ellipse( width/2, height/2, w, w );
    }
    else // we have a location! yay!
    {
        noStroke();
        fill( 255, 0,0 );
        ellipse(pos_x, pos_y, 7, 7);
        boolean onRightHalf = pos_x > width/2;
        textAlign( onRightHalf ? RIGHT : LEFT );
        text( "HI! You are here!", 
              pos_x + (onRightHalf ? -10 : 10), 
              pos_y + 3 );
    }
}

/* these two functions are called from plain javascript, see .js tab */

void setGeoLocation ( GeoLocation position )
{
    loc = position;
    
    // this is a really simplistic (course) "projection",
    // see: http://en.wikipedia.org/wiki/Map_projection
    pos_x = (180.0 + loc.coords.longitude) * (width  / 360.0);
    pos_y = ( 90.0 - loc.coords.latitude ) * (height / 180.0);
}

void geoLocationError ( String message )
{
    glNotAvail = true; // bummer!
    errorMessage = message;
}

/* these classes define how to access the data coming in from the browser */

class Coordinates
{
    float latitude;
    float longitude;
}

class GeoLocation
{
    Coordinates coords;
}

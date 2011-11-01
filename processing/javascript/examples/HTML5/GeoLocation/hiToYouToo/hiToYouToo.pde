/* @pjs preload="map.png"; */

/**
 *    This example shows you how to use the browsers geo
 *    location services.<br />
 *    <br />
 *    Normally the browser should ask you to permit access to
 *    your location data (if you haven't already earlier).
 */
 
/**
 *    Note that the map alignment is not very accurate. If you find
 *    yourself positioned off the map don't worry, you probably are safe.
 */
 
float pos_x, pos_y;

boolean locationFound = false;
boolean locationServiceNotAvailable = false;
String errorMessage = "", addressString = "";
PImage worldMap;

void setup ()
{
    size(454, 200);
    textFont(createFont("Arial", 12));
    worldMap = loadImage("map.png");
}

void draw ()
{
    background(worldMap);
    
    if ( locationServiceNotAvailable ) // failed
    {
        textAlign(CENTER);
        fill( 255, 0, 0 );
        text( "Something went wrong:\n" + errorMessage, 
              width/4, height/4, width/2, height/2 );
    }
    else if ( !locationFound ) // waiting for location to come in ..
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
        text( "HI! You are (about) here ..\n"+addressString, 
              pos_x + (onRightHalf ? -10 : 10), 
              pos_y + 3 );
    }
}

/* the following functions are called from plain javascript, see .js tab */

void setGeoLocation ( float latitude, float longitude )
{
    // this is a really simplistic (course) "projection",
    // see: http://en.wikipedia.org/wiki/Map_projection
    pos_x = (180.0 + longitude) * (width  / 360.0);
    pos_y = ( 90.0 - latitude ) * (height / 180.0);
    
    locationFound = true;
}

void geoLocationError ( String message )
{
    locationServiceNotAvailable = true; // bummer!
    errorMessage = message;
}

void setAddressString ( String address )
{
    addressString = "(" + address + ")";
}

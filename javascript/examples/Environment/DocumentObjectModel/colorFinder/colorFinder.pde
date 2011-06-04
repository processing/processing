/**
 *    <p>This example shows you how to manipulate the DOM of a
 *    HTML page that this sketch is placed in.</p>
 *
 *    <p>Click and drag inside the sketch area to change the 
 *    text color of the page</p> 
 */

color mColor = 255;

void setup ()
{
    size( 600, 100 );
}

void draw ()
{
    background( mColor );
}

void mouseDragged ()
{
    colorMode(HSB);
    mColor = color( map(mouseX, 0,width, 0,255), 200, map(mouseY, 0, height, 255, 0) );
    colorMode(RGB);
    
    if ( js )
    {
        js.setColor(red(mColor), green(mColor), blue(mColor));
    }
}

/**
 *    Define an interface that will act as glue between this sketch
 *    and "real" JavaScript running in the HTML page. The name does not matter.
 *
 *    The interface must define any functions that one intends to call
 *    from inside the sketch.
 */
interface JavaScriptInterface
{
    void setColor( int r, int g, int b );
}

/* A variable to hold whatever implements the interface. */
JavaScriptInterface js;

/**
 *    A setter function to be called from outside (JavaScript)
 *    to set the variable above.
 */
void setInterfaceLink ( JavaScriptInterface jsin )
{
    js = jsin;
}

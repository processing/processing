/**
 *    <p>This example shows you how to manipulate the DOM of a
 *    HTML page from a sketch.</p>
 */

color mColor = 255;

void setup ()
{
    size( 600, 100 );
}

void draw ()
{
    background( mColor );
    
    textAlign(CENTER); fill(0);
    text("<<  click and drag here  >>", width/2, height/2);
}

void mouseDragged ()
{
    colorMode(HSB);
    mColor = color( map(mouseX, 0,width, 0,255), 200, map(mouseY, 0, height, 255, 0) );
    colorMode(RGB);
    
    if ( js )
    {
        // call JavaScript function, see "jsinterface.js"
        js.setColor(red(mColor), green(mColor), blue(mColor));
    }
}

/**
 *    Define an interface that will act as glue between this sketch
 *    and "real" JavaScript running in the HTML page.
 *
 *    The interface must define all functions that you intend to call
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

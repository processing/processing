/**
 *    A Box2D (by Erin Catto) example based on a port and examples by Ando Yasushi. <br />
 *    Click & drag to create balls, hold alt/option to create boxes. <br />
 *
 *    <ul>
 *      <li>Erin Catto http://code.google.com/p/box2d/</li>
 *      <li>Ando Yasushi http://box2d-js.sourceforge.net/</li>
 *    </ul>
 */

/**
 *    Note that this uses a rather old port of a (ActionScript) Box2D library.
 */
 
Box2DInterface b2d;

void setup ()
{
    size( 500, 300 );
    
    colorMode(HSB);
}

void draw ()
{
    background( 100 );
    
    if ( isDragged )
    {
        if ( !keyPressed )
        {
            float d = dist(mouseX,mouseY,pressedX,pressedY)*2;
            fill(120);
            ellipse( pressedX, pressedY, d, d );
        }
        else if ( key == CODED && keyCode == ALT ) 
        {
            float w = mouseX-pressedX;
            float h = mouseY-pressedY;
            rect( pressedX-w, pressedY-h, w*2, h*2 );
        }
    }
    
    if ( b2d != null )
    {
        b2d.update();
        b2d.draw();
    }
}

float pressedX, pressedY;
boolean isDragged = false;
void mousePressed ()
{
    pressedX = mouseX; pressedY = mouseY;
    isDragged = false;
}

void mouseDragged ()
{
    isDragged = true;
}

void mouseReleased ()
{
    color rc = color(random(255), 190, 140);
    
    if ( !keyPressed )
    {
        if ( !isDragged )
            b2d.createBall( rc, mouseX, mouseY, 20 );
        else
            b2d.createBall( rc, pressedX,pressedY, dist(mouseX,mouseY,pressedX,pressedY) );
    }
    else if ( key == CODED && keyCode == ALT )
    {
        if ( !isDragged )
            b2d.createBox( rc, mouseX-20, mouseY-20, 40, 40 );
        else
        {
            b2d.createBox( rc, pressedX,pressedY, mouseX-pressedX, mouseY-pressedY );
        }
    }
    isDragged = false;
}

// these three drawing functions are being called by the
// Box2D interface in the .js tab

void drawJoints ( float[] points ) 
{
    for ( int i = 0; i < points.length-2; i+=2 )
        line( points[i], points[i+1], points[i+2], points[i+3] );
}

void drawPolygon ( color c, float[] points )
{
    fill( c == null ? 255 : c );
    noStroke();
    beginShape();
    for ( int i = 0; i < points.length; i+=2 )
        vertex( points[i], points[i+1] );
    endShape();
}

void drawCircle ( color c, float x, float y, float r )
{
    fill( c == null ? 255 : c );
    noStroke();
    ellipse( x, y, r*2, r*2 );
}

// this is being called from JavaScript to set the Box2D interface
void setBox2DInterface ( Box2DInterface b )
{
    b2d = b;
}

// explain Processing how the interface is set up
interface Box2DInterface
{
    void createBall( color c, float x, float y, float r );
    void createBox( color c, float x, float y, float w, float h );
    
    void update();
    void draw();
}


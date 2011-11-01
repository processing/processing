/**
 *  This example demonstrates how to promt for user input. It first asks
 *  for password which then is used to unlock the sketch.
 */
 
String password = null;
boolean locked = false;
int nextLock = 0;
 
void setup ()
{
    size( 300, 200 );
    
    textFont(createFont("Arial", 22));
    textAlign(CENTER);
    
    rectMode(CENTER);    
    noStroke();
}

void draw ()
{
    if ( password == null )
    {        
        background( 255 );
        
        fill( 0 );
        text( "Please set a password \n(click here)", width/2, height/2);
    }
    else if ( locked )
    {
        background( 100 );
        
        fill(255,0,0);
        pushMatrix();
            translate(width/2,height/3);
            rotate(HALF_PI/2); rect(0,0,40,10);
            rotate(HALF_PI); rect(0,0,40,10);
        popMatrix();
        
        fill( 255 );
        text( "LOCKED, click to unlock", width/2, 2*(height/3));
    }
    else
    {
        background( 255 );
        
        fill(0,255,0);
        pushMatrix();
            translate(width/2,height/3+10);
            rotate(-HALF_PI/2); rect(15,0,40,10);
            rotate(-HALF_PI); rect(5,0,20,10);
        popMatrix();
        
        fill( 0 );
        text( "UNLOCKED\nwill lock in "+int(ceil((nextLock-millis())/1000))+" secs", width/2, 2*(height/3));
        
        if ( nextLock-millis() < 0 ) locked = true;
    }
}

/*void mouseMoved ()
{
    if ( password != null && !locked )
        nextLock = millis() + 5000;
}*/

void mousePressed ()
{
    if ( js != null ) 
    {
        if ( password == null )
        {
            password = js.promtForInput( "Please set and remember a password", "***********" );
            nextLock = millis() + 5000;
        }
        else if ( locked )
        {
            String passTry = js.promtForInput( "Enter your password", "" );
            while ( testLocked(passTry) )
            {
                passTry = js.promtForInput( "Nope, try again", "" );
                if ( passTry == null ) break;
            }
            locked = testLocked(passTry);
            if ( !locked )
                nextLock = millis() + 5000;
        }
    }
}

boolean testLocked ( String passTry )
{
    return passTry == null || !passTry.equals(password);
}


/* this interface is needed to explain Processing what the
properties of the object are that is handed in from JS */
interface JavaScript
{
    String promtForInput( String message, String defaultAnswer );
}

// a variable
JavaScript js;

// called from JavaScript to hand in the object
void setJS ( JavaScript jsi )
{
    js = jsi;
}

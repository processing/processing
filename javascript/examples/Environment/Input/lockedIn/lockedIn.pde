/**
 *  Promt for user input
 */
 
String password = null;
boolean locked = false;
int nextLock = 0;
 
void setup ()
{
    size( 300, 200 );
    
    textFont(createFont("Arial", 22));
    textAlign(CENTER);
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
        
        fill( 255 );
        text( "LOCKED, click to unlock", width/2, height/2);
    }
    else
    {
        background( 255 );
        
        fill( 0 );
        text( "UNLOCKED\nwill lock in "+int(ceil((nextLock-millis())/1000))+" secs", width/2, height/2);
        
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
            while ( passTry != null && !passTry.equals(password) )
            {
                passTry = js.promtForInput( "Nope, try again", "" );
                if ( passTry == null ) break;
            }
            locked = passTry != null && !passTry.equals(password);
            if ( !locked )
                nextLock = millis() + 5000;
        }
    }
}


// this is needed to define a way for us to be able to call out

interface JavaScript
{
    String promtForInput( String message, String defaultAnswer );
}

JavaScript js;

void setJS ( JavaScript jsi )
{
    js = jsi;
}

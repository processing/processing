/**
 *    <p>This example shows you how to get the currently selected text 
 *    from this HTML page.</p>
 *
 *    <p>Just select some text somewhere on this page and see it be transformed
 *    into a ... graph.</p>
 *
 *    <p>Heavily inspired by <a href="http://bit.ly/jHvvWX">Boris Müller</a>.</p>
 */

// a directive to set the background transparent
/* @pjs transparent=true; */

String selectedText = "";

void setup ()
{
    size( 300, 300 );
}

void draw ()
{
    background( 100,0 );
    fill( 255 );
    
    if ( !selectedText.equals("") )
    {
        translate( width/2, height/2 );

        float tw = selectedText.length() * 10;
        float[] x = new float[selectedText.length()];
        float[] y = new float[selectedText.length()];
        
        for ( int i = 0, n = 0; i < tw; n++ )
        {
            char cr = new Character(selectedText.charAt(n));
            float c = cr;
            
            float r = -HALF_PI + map( i, 0, tw, 0, TWO_PI );
            
            x[n] = cos(r)*c;
            y[n] = sin(r)*c;
            
            i += 10;
        }
        
        fill( 100 );
        
        if ( x.length > 0 )
        {
            beginShape();
                for ( int i = 0; i < x.length; i++ )
                {
                    vertex( x[i], y[i] );
                }
            endShape( CLOSE );
        }
        
        smooth();
        fill( 255 );
        stroke( 150 );
        
        for ( int i = 0; i < x.length; i++ )
            text( selectedText.charAt(i), x[i]*1.15, y[i]*1.15+6 );
    }
    else
    {
        fill( map( sin(frameCount/12.0),-1,1,100,200 ) );
        text("Select some text (not this) to start.", 2, height/2);
    }
}

// called from JavaScript
void setSelectionText ( String txt )
{
    selectedText = txt;
}

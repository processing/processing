/**
 *   This example shows how to render a waveform from an audio file.
 *
 *    Currently Firefox and Chrome only.
 */
 
int[] audioBytes;
boolean noAudioDataApi;

void setup ()
{
    size( 800, 200 );
    
    sketchStarted( this );
}

void draw ()
{
    background( 100 );
    
    if ( noAudioDataApi )
    {
        background( 100, 0, 0 );
        fill( 255, 0, 0 );
        text( "Your browser does not support the AudioData API. Try FF 4+ or recent Chrome", 20, 40 );
    }
    else if ( audioBytes != null )
    {
        noFill();
        stroke( 255 );
        beginShape();
        for ( int i = 0; i < audioBytes.length; i++ )
        {
            vertex( map( i, 0, audioBytes.length, 0, width ), map( audioBytes[i], -1, 1, height, 0 ) );
        }
        endShape();
    }
}

void audioDataApiNotAvailable ()
{
    noAudioDataApi = true;
}

void audioMetaData ( int sampleRate, int channels, int bufferLength )
{
}

void audioData ( int[] buffer )
{
    if ( audioBytes == null )
        audioBytes = buffer;
    else
    {
        int[] tmp = new int[ audioBytes.length + buffer.length ];
        
        for ( int i = 0; i <  audioBytes.length; i++ )
            tmp[i] = audioBytes[i];
            
        for ( int i = 0; i < buffer.length; i++ )
            tmp[audioBytes.length+i] = buffer[i];
        
        audioBytes = tmp;
    }
}

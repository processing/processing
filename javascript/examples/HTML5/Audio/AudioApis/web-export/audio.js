
var sketchStarted = function ( sketch )
{
    if ( navigator.userAgent.match(/Firefox/i) )
        AudioDataFF.load( ["BD.wav","BD.mp3"], sketch );
    else if ( navigator.userAgent.match(/Chrome/i) )
        AudioDataWebKit.load( "BD.wav", sketch );
    else
        sketch.audioDataApiNotAvailable();
}

// https://dvcs.w3.org/hg/audio/raw-file/tip/webaudio/specification.html#AudioBuffer-section
// http://www.html5rocks.com/en/tutorials/webaudio/intro/
// http://chromium.googlecode.com/svn/trunk/samples/audio/index.html
var AudioDataWebKit = {
    context : (function(){
        if ('webkitAudioContext' in window) 
            return new webkitAudioContext(); 
        })(),
    buffers : [],
    listener : null,
    load : function ( url, listener ) {
        this.listener = listener;
        if ( !( url instanceof Array ) ) {
            url = [url];
        } 
        for ( u in url ) {
            var request = new XMLHttpRequest();
            request.open('GET', url[u], true);
            request.responseType = 'arraybuffer';
            request.onload = (function(ad){ return function() {
                ad.context.decodeAudioData( request.response, function(buffer) {
                    ad.buffers.push( buffer );
                    ad.listener.audioMetaData( buffer.sampleRate, buffer.numberOfChannels, buffer.length );
                    ad.listener.audioData( buffer.getChannelData(0) );
                }, onError);
            }})(this);
            var onError = function (err) {
                console.log( err );
            }
            request.send();
        }
    }
};

// https://wiki.mozilla.org/Audio_Data_API
var AudioDataFF = {
    audioElement : null,
    listener : null,
    load : function ( url, listener ) {
        this.listener = listener;
        if ( this.audioElement == null ) {
            this.audioElement = document.createElement( "audio" );
            this.audioElement.addEventListener('loadedmetadata', (function(ad){ return function ( ev ) {
                ad.audioMetaData( ev, this );
            }})(this), false);
            this.audioElement.addEventListener('MozAudioAvailable', (function(ad){return function(ev){
                ad.audioAvailable( ev, this );
            }})(this), false);
            if ( !( url instanceof Array ) ) {
                url = [ url ];
            }
            for ( u in url ) {
                var src = document.createElement( "source" );
                src.setAttribute( "src", url[u] );
                this.audioElement.appendChild( src );
            }
            document.body.appendChild( this.audioElement );
        }
    },
    audioAvailable : function ( ev ) {
        this.listener.audioData( ev.frameBuffer );
    },
    audioMetaData : function ( ev ) {
        if ( this.audioElement ) {
            this.audioElement.volume = 0;
            this.audioElement.play();
            this.listener.audioMetaData( 
                this.audioElement.mozSampleRate, 
                this.audioElement.mozChannels, 
                this.audioElement.mozFrameBufferLength );
        }
    }
};

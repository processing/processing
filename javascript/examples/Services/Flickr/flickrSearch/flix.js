/******************************
* YOU NEED A FLICKR API KEY !!
*******************************/

// g't it here:
var flickrAPIPage = "http://www.flickr.com/services/api/keys/";
// then paste here:
var yourFlickrAPIKey = "";

window.onload = function () {
    if ( !yourFlickrAPIKey.match(/^[0-9a-f]{32}$/) ) {
        alert("You need a Flickr API key!");
        return;
    }
    tryFindSketch();
}

var sendImage = null;

function tryFindSketch () {
    var sketch = Processing.getInstanceById(getProcessingSketchId());
    if ( sketch == undefined )
        return setTimeout(tryFindSketch, 200); //retry later
    
    // curry sketch into function .. yum.
    sendImage = (function(){
        return function ( img ) {
            sketch.newFlickrImage( img );
        }
    })();
    
    var input = document.getElementById("form-input");
    var submit = document.getElementById("form-submit");
    submit.onclick = function (event) {
        sketch.resetFlickrImages();
        searchFlickr(input.value);
        return false;
    }
    submit.onclick();
}

function searchFlickr ( query ) {
    // see:
    // http://www.flickr.com/services/api/flickr.photos.search.html
    loadUrl( 'http://api.flickr.com/services/rest/?'+
             'method=flickr.photos.search'+
             '&api_key=' + yourFlickrAPIKey +
             '&text=' + encodeURIComponent( query ) +
             '&format=json'+
             '&extras=owner_name,url_sq,url_t,url_s,url_o'+
             '&jsoncallback=handleNewImages'
    );
}

function handleNewImages ( response ) {
    if ( response.stat !== "ok" ) {
        alert( response.message );
    } else if ( response.photos && response.photos.photo.length > 0 ) {
        for ( var i = 0; i < response.photos.photo.length; i++ ) {
            sendImage( response.photos.photo[i] );
        }
    }
}

function loadUrl ( url ) {
    var script = document.createElement('script');
    script.src = url;
    document.body.appendChild(script);
}

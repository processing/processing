
window.onload = function () {
    tryFindSketch();
}

function tryFindSketch () {
    var sketch = Processing.getInstanceById("hiToYouToo");
    if ( sketch == undefined )
        return setTimeout(tryFindSketch, 200); // try again in 0.2 secs
    
    if ( navigator.geolocation ) {
        navigator.geolocation.getCurrentPosition( function(position) {
            /*success*/
            sketch.setGeoLocation(position);
        }, function( position_error ) {
            /*error*/
            sketch.geoLocationError(position_error.message);
        });
    } else {
        sketch.geoLocationError( "Your browser does not support location services." );
    }
}

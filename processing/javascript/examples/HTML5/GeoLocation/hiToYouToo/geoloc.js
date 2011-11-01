
// lazy load the google geocoding api
document.write( "<script src=\"http://maps.google.com/maps/api/js?sensor=false\" "+
                "type=\"text/javascript\"></script>");

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
            sketch.setGeoLocation(position.coords.latitude, position.coords.longitude);
            tryReverseGeocoding( sketch, position.coords );
        }, function( position_error ) {
            /*error*/
            sketch.geoLocationError(position_error.message);
        });
    } else {
        sketch.geoLocationError( "Your browser does not support location services." );
    }
}

function tryReverseGeocoding ( sketch, latlng ) {
    var geocoder = new google.maps.Geocoder();
    geocoder.geocode({ 
        'latLng': new google.maps.LatLng(latlng.latitude,latlng.longitude) 
    },
    function (data, status) {
        if (status == google.maps.GeocoderStatus.OK) {
            if ( data.length > 0 ) {
                for ( var d in data ) {
                    for ( var t in data[d].types ) {
                        if ( data[d].types[t] == 'political' ) {
                            sketch.setAddressString( data[d].formatted_address );
                            return; // done
                        }
                    }
                }
                sketch.setAddressString( data[0].formatted_address ); // fallback
            }
        } else {
            // ignore ..
        }
    });
}

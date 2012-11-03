
window.onload = function () {
    tryFindSketch();
}

var sendTweet = null;

function tryFindSketch () {
    var sketch = Processing.getInstanceById(getProcessingSketchId());
    if ( sketch == undefined )
        return setTimeout(tryFindSketch, 200); //retry later
        
    sendTweet = (function(){
        return function ( tweet ) {
            sketch.newTweet( tweet );
        }
    })();
    
    var input = document.getElementById("form-input");
    var submit = document.getElementById("form-submit");
    submit.onclick = function (event) {
        sketch.resetTweets();
        searchTweets(input.value);
        return false;
    }
    submit.onclick();
}

// var url = 'http://twitter.com/statuses/user_timeline/' + 
// encodeURIComponent(user) + 
// '.json?callback=html5demoTweets.loadTweets';
function searchTweets ( query ) {
    var script = document.createElement('script');
    var url = 'http://search.twitter.com/search.json?'+
              'rpp=20'+
              '&callback=handleNewTweets'+
              '&q=' + encodeURIComponent( query )+
              '&_cb=' + Math.random();
    script.src = url;
    script.id = "twitterJSON";
    document.body.appendChild(script);
}

function handleNewTweets ( tweets ) {
    if ( tweets.results.length > 0 ) {
        for ( var i in tweets.results ) {
            var result = tweets.results[i];
            sendTweet(result);
        }
    }
}

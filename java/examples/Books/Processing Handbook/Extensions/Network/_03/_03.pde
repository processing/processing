// Download the Yahoo! Search SDK from http://developer.yahoo.com/download
// Inside the download, find the yahoo_search-2.X.X.jar file somewhere inside
// the "Java" subdirectory. Drag the jar file to your sketch and it will be
// added to your 'code' folder for use.
// This example is based on the based on yahoo api example
// Replace this with a developer key from http://developer.yahoo.com
String appid = "YOUR_DEVELOPER_KEY_HERE";
SearchClient client = new SearchClient(appid);
String query = "processing.org";
WebSearchRequest request = new WebSearchRequest(query);

// (Optional) Set the maximum number of results to download
//request.setResults(30);

try {
  WebSearchResults results = client.webSearch(request);
  // Print out how many hits were found
  println("Displaying " + results.getTotalResultsReturned() +
          " out of " + results.getTotalResultsAvailable() + " hits.");
  println();
  // Get a list of the search results
  WebSearchResult[] resultList = results.listResults();
  // Loop through the results and print them to the console

  for (int i = 0; i < resultList.length; i++) {
    // Print out the document title and URL.
    println((i + 1) + ".");
    println(resultList[i].getTitle());
    println(resultList[i].getUrl());
    println();
  }

// Error handling below, see the documentation of the Yahoo! API for details
} catch (IOException e) {
  println("Error calling Yahoo! Search Service: " + e.toString());
  e.printStackTrace();
} catch (SearchException e) {
  println("Error calling Yahoo! Search Service: " + e.toString());
  e.printStackTrace();
}

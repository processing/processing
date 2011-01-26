// Code from Visualizing Data, First Edition, Copyright 2008 Ben Fry.


class Slurper implements Runnable {

  Slurper() {
    Thread thread = new Thread(this);
    thread.start();
  }

  public void run() {
    try {
      InputStream input = openStream("zips.gz");
      BufferedReader reader = createReader(input);
      
      // first get the info line
      String line = reader.readLine();
      parseInfo(line);

      places = new Place[totalCount];

      // parse each of the rest of the lines
      while ((line = reader.readLine()) != null) {
        places[placeCount] = parsePlace(line);
        placeCount++;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}

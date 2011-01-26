Record[] records;
int recordCount;

void setup() {
  String[] lines = loadStrings("cars2.tsv");
  records = new Record[lines.length];
  for (int i = 0; i < lines.length; i++) {
    String[] pieces = split(lines[i], '\t'); // Load data into array
    if (pieces.length == 9) {
      records[recordCount] = new Record(pieces);
      recordCount++;
    }
  }
  for (int i = 0; i < recordCount; i++) {
    println(i + " -> " + records[i].name); // Print name to console
  }
}

class Record {
  String name;
  float mpg;
  int cylinders;
  float displacement;
  float horsepower;
  float weight;
  float acceleration;
  int year;
  float origin;
  public Record(String[] pieces) {
    name = pieces[0];
    mpg = float(pieces[1]);
    cylinders = int(pieces[2]);
    displacement = float(pieces[3]);
    horsepower = float(pieces[4]);
    weight = float(pieces[5]);
    acceleration = float(pieces[6]);
    year = int(pieces[7]);
    origin = float(pieces[8]);
  }
}

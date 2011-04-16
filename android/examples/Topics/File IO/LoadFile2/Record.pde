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

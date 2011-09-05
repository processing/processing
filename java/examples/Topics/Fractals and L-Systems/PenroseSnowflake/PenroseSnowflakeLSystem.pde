class PenroseSnowflakeLSystem extends LSystem {

  String ruleF;

  PenroseSnowflakeLSystem() {
    axiom = "F3-F3-F3-F3-F";
    ruleF = "F3-F3-F45-F++F3-F";
    startLength = 450.0;
    theta = radians(18); 
    reset();
  }

  void useRule(String r_) {
    rule = r_;
  }

  void useAxiom(String a_) {
    axiom = a_;
  }

  void useLength(float l_) {
    startLength = l_;
  }

  void useTheta(float t_) {
    theta = radians(t_);
  }

  void reset() {
    production = axiom;
    drawLength = startLength;
    generations = 0;
  }

  int getAge() {
    return generations;
  }

  void render() {
    translate(width, height);
    int repeats = 1;

    steps += 3;          
    if (steps > production.length()) {
      steps = production.length();
    }

    for (int i = 0; i < steps; i++) {
      char step = production.charAt(i);
      if (step == 'F') {
        for (int j = 0; j < repeats; j++) {
          line(0,0,0, -drawLength);
          translate(0, -drawLength);
        }
        repeats = 1;
      } 
      else if (step == '+') {
        for (int j = 0; j < repeats; j++) {
          rotate(theta);
        }
        repeats = 1;
      } 
      else if (step == '-') {
        for (int j =0; j < repeats; j++) {
          rotate(-theta);
        }
        repeats = 1;
      } 
      else if (step == '[') {
        pushMatrix();
      } 
      else if (step == ']') {
        popMatrix();
      } 
      else if ( (step >= 48) && (step <= 57) ) {
        repeats += step - 48;
      }
    }
  }


  String iterate(String prod_, String rule_) {
    String newProduction = "";
    for (int i = 0; i < prod_.length(); i++) {
      char step = production.charAt(i);
      if (step == 'F') {
        newProduction = newProduction + ruleF;
      } 
      else {
        if (step != 'F') {
          newProduction = newProduction + step;
        }
      }
    }
    drawLength = drawLength * 0.4;
    generations++;
    return newProduction;
  }

}

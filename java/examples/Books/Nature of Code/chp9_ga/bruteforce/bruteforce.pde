// The Nature of Code
// Daniel Shiffman
// http://natureofcode.com

int now = millis();

int passedTime = millis() - now;
int count = 0;
while (passedTime < 1000) {
  for (int i = 0; i < 33; i++) {
    float r = random(27);
  }
  count++;
  passedTime = millis() - now;
}
println(count);



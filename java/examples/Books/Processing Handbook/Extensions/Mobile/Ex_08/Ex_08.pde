import processing.phone.*;
Phone p;

void setup() {
  p = new Phone(this);
  noLoop(); // No drawing in this sketch, so we don't need to run the draw() loop
}

void keyPressed() {
  switch (key) {
	  case '1':
       // Vibrate the phone for 200 milliseconds
	    p.vibrate(200);
	    break;

	  case '2':
       // Flash the backlight for 200 milliseconds
	    p.flash(200);
	    break;

	  case '3':
       // Dial 411 on the phone
	    p.call("411");
	    break;
	  case '4':
       // Launch the Web browser
	    p.launch("http://mobile.processing.org/");
	    break;
  }
}

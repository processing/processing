// Example 04-09 from "Getting Started with Processing" 
// by Reas & Fry. O'Reilly / Make 2010

size(480, 120);
smooth();
strokeWeight(2);
for (int i = 20; i < 400; i += 20) {
  line(i, 0, i + i/2, 80);
  line(i + i/2, 80, i*1.2, 120);
}


// The init is "int i = 20", the test is "i < 80",
// and the update is "i += 5". Notice the semicolon
// terminating the first two elements
for (int i = 20; i < 80; i += 5) {
  // This line will continue to run until "i"
  // is greater than or equal to 80
  line(20, i, 80, i+15);
}
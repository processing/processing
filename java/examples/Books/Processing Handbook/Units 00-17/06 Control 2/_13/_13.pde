int x = 50;

if (x > 100) {
line(20, 20, 80, 80);
} else {
line(80, 20, 20, 80);
}

for (int i=0; i<100; i+=2) {
line(20, i, 80, i);
}

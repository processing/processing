String[] lines = loadStrings("positions.txt");

for (int i = 0; i < lines.length; i++) {
// Split this line into pieces at each tab character
  String[] pieces = split(lines[i], '\t');
// Take action only if there are two values on the line
// (this will avoid blank or incomplete lines)
  if (pieces.length == 2) {
    int x = int(pieces[0]);
    int y = int(pieces[1]);
    point(x, y);
  }
}

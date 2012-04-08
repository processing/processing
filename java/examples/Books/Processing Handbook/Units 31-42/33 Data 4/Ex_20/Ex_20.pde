String[] north = { "OH", "IN", "MI" };
String[] south = { "GA", "FL", "NC" };
arraycopy(north, south); // Copy from north array to south array
print(south); // Prints "OH IN MI"
println();
String[] east = { "MA", "NY", "RI" };
String[] west = new String[east.length]; // Create a new array
arraycopy(east, west); // Copy from east array to west array
print(west); // Prints "MA NY RI"
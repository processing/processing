String s = "a, b c ,,d "; // Despite the bad formatting,
String[] p = splitTokens(s, ", "); // the data is parsed correctly
println(p[0]); // Prints "a"
println(p[1]); // Prints "b"
println(p[2]); // Prints "c"
println(p[3]); // Prints "d"
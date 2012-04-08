String[] trees = { "ash", "oak" };
append(trees, "maple"); // INCORRECT! Does not change the array
print(trees); // Prints "ash oak"
println();
trees = append(trees, "maple"); // Add "maple" to the end
print(trees); // Prints "ash oak maple"
println();
// Add "beech" to the end of the trees array, and creates a new
// array to store the change
String[] moretrees = append(trees, "beech");
print(moretrees); // Prints "ash oak maple beech"
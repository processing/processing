String[] lines = loadStrings("2895.txt");
int totalCount = 0; // Total word count for entire book
boolean started = false; // Ignore lines until the *** START line

for (int i = 0; i < lines.length; i++) {
  if (lines[i].startsWith("*** START")) { // Start parsing text
    started = true;
  } else if (lines[i].startsWith("*** END")) { // Stop parsing text
    started = false;
  } else if (started == true) { // If we're in the useful region
    // List of characters and punctuation to ignore between
    // letters. WHITESPACE is all the whitespace characters
    String separators = WHITESPACE + ",;.:?()\"-";
    // Split the line anywhere that we see one or more of
    // these separators
    String[] words = splitTokens(lines[i], separators);
    // Add this number to the total
    totalCount += words.length;
    // Go through the list of words on the line
    for (int j = 0; j < words.length; j++) {
      String word = words[j].toLowerCase();
      if (word.length() > 10) {
        println(word); // Print word if longer than ten letters
      }
    }
  }
}

// How many words are in the entire book?
println("This book has " + totalCount + " total words.");
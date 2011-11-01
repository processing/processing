// Code from Visualizing Data, First Edition, Copyright 2008 Ben Fry.


class Node {
  File file;
  
  Node[] children;
  int childCount;

  Node(File file) {
    this.file = file;    
    if (file.isDirectory()) {
      addFolder(this);
    }
  }
  
  void check() {
    String[] contents = file.list();
    if (contents != null) {
      // Sort the file names in case insensitive order
      contents = sort(contents);

      children = new Node[contents.length];
      for (int i = 0 ; i < contents.length; i++) {
        // Skip the . and .. directory entries on Unix systems
        if (contents[i].equals(".") || contents[i].equals("..")) {
          continue;
        }
        File childFile = new File(file, contents[i]);
        // Skip any file that appears to be a symbolic link
        try {
          String absPath = childFile.getAbsolutePath();
          String canPath = childFile.getCanonicalPath();
          if (!absPath.equals(canPath)) {
            continue;
          }
        } catch (IOException e) { }

        Node child = new Node(childFile);
        children[childCount++] = child;
      }
    }
  }
  

  void printList(int depth) {
  // print four spaces for each level of depth;
  for (int i = 0; i < depth; i++) {
      print("    ");  
    }
    println(file.getName());

    // now handle the children, if any
    for (int i = 0; i < childCount; i++) {
      children[i].printList(depth + 1);
    }
  }
}

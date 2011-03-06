/*
This book is here to help you get your job done. In general, you may use the 
code in this book in your programs and documentation. You do not need to contact 
us for permission unless you’re reproducing a significant portion of the code. 
For example, writing a program that uses several chunks of code from this book 
does not require permission. Selling or distributing a CD-ROM of examples from 
O’Reilly books does require permission. Answering a question by citing this book 
and quoting example code does not require permission. Incorporating a significant
amount of example code from this book into your product’s documentation does 
require permission.

We appreciate, but do not require, attribution. An attribution usually includes
the title, author, publisher, and ISBN. For example: “Visualizing Data, First 
Edition by Ben Fry. Copyright 2008 Ben Fry, 9780596514556.”

If you feel your use of code examples falls outside fair use or the permission
given above, feel free to contact us at permissions@oreilly.com.
*/
import treemap.*;

import javax.swing.*;

FolderItem rootItem;
FileItem rolloverItem;
FolderItem taggedItem;

BoundsIntegrator zoomBounds;
FolderItem zoomItem;

RankedLongArray modTimes = new RankedLongArray();

PFont font;


void setup() {
  size(1024, 768);
  zoomBounds = new BoundsIntegrator(0, 0, width, height);
  
  cursor(CROSS);
  rectMode(CORNERS);
  smooth();
  noStroke();

  font = createFont("SansSerif", 13);

  selectRoot();
}


void selectRoot() {
  SwingUtilities.invokeLater(new Runnable() {
    public void run() {
      JFileChooser fc = new JFileChooser();
      fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      fc.setDialogTitle("Choose a folder to browse...");

      int returned = fc.showOpenDialog(frame);
      if (returned == JFileChooser.APPROVE_OPTION) {
        File file = fc.getSelectedFile();
        setRoot(file);
      }
    }
  });
}

  
void setRoot(File folder) {
  FolderItem tm = new FolderItem(null, folder, 0, 0);
  tm.setBounds(0, 0, width, height);
  tm.contentsVisible = true;
    
  rootItem = tm;
  rootItem.zoomIn();
  rootItem.updateColors();
}


void draw() {
  background(0);
  textFont(font);
  
  frameRate(30);
  zoomBounds.update();

  rolloverItem = null;
  taggedItem = null;

  if (rootItem != null) {
    rootItem.draw();
  }
  if (rolloverItem != null) {
    rolloverItem.drawTitle();
  }
  if (taggedItem != null) {
    taggedItem.drawTag();
  }
}


void mousePressed() {
  if (zoomItem != null) {
    zoomItem.mousePressed();
  }
}


/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2011 Ben Fry and Casey Reas

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License version 2.1 as published by the Free Software Foundation.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
 */

package processing.core;

import java.io.*;
import java.util.*;

//import com.benfry.util.StringIntPairs;

import processing.core.PApplet;

// no column max/min functions since it needs to be per-datatype
// better to use float mx = max(float(getColumn(3)));
// *** but what to do with null entries?

// todo: need a method to reset the row/column indices after add/remove
// or just make sure that it's covered for all cases


/**
 * <p>Generic class for handling tabular data, typically from a CSV, TSV, or 
 * other sort of spreadsheet file.</p> 
 * <p>CSV files are 
 * <a href="http://en.wikipedia.org/wiki/Comma-separated_values">comma separated values</a>, 
 * often with the data in quotes. TSV files use tabs as separators, and usually
 * don't bother with the quotes.</p> 
 * <p>File names should end with .csv if they're comma separated.</p>
 * <p>By default, empty rows are skipped and so are lines that start with the
 * # character. Using # at the beginning of a line indicates a comment.</p>
 */
public class Table implements Iterable<TableRow> {
  protected int rowCount;
//  protected int columnCount;
  
  protected boolean skipEmptyRows = true;
  protected boolean skipCommentLines = true;
  protected boolean commaSeparatedValues = false;
  protected boolean awfulCSV = false;
  
  protected String missingString = null;
  protected int missingInt = 0;
  protected long missingLong = 0;
  protected float missingFloat = Float.NaN;
  protected double missingDouble = Double.NaN;

  String[] columnTitles;
  HashMap<String, Integer> columnIndices;

//  static final int TSV = 1;
//  static final int CSV = 2;
//  static final int AWFUL_CSV = 3;

//  boolean typed; 

  // untyped data
//  protected String[][] data;
//  protected Object[] data;  // [row][column]
  protected Object[] columns;  // [column]

  // typed data
  static final int STRING = 0;
  static final int INT = 1;
  static final int LONG = 2;
  static final int FLOAT = 3;
  static final int DOUBLE = 4;
//  static final int TIME = 5;
  int[] columnTypes;

//  int[][] intData;  // [column][row]
//  long[][] longData;
//  float[][] floatData;
//  double[][] doubleData;
//  Object[][] objectData;


  /** 
   * Creates a new, empty table. Use addRow() to add additional rows.
   */
  public Table() {
  }

  
  public Table(File file) {
    this(PApplet.createReader(file));
  }

  
  /**
   * Can handle TSV or CSV files.
   * @param parent
   * @param filename
   */
  public Table(PApplet parent, String filename) {
    this(parent.createReader(filename));
  }
  
  
  public Table(BufferedReader reader) {
    try {
      boolean csv = peekCSV(reader);
      if (csv) {
        parseCSV(reader);
      } else {
        parseTSV(reader);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  
  /**
   * Guess whether this file is tab separated or comma separated by checking
   * whether there are more tabs or commas in the first 100 characters. 
   */
  protected boolean peekCSV(BufferedReader reader) throws IOException {
    char[] buffer = new char[100];
    int remaining = buffer.length;
    reader.mark(remaining);
//    int count = 0;
    int commas = 0;
    int tabs = 0;
    for (int i = 0; i < remaining; i++) {
      int c = reader.read();
      if (c == -1) break;
      if (c == ',') commas++;
      if (c == '\t') tabs++;
    }
    reader.reset();
    return (commas > tabs);
  }


  protected void parse(BufferedReader reader) throws IOException {
    if (commaSeparatedValues) {
      if (awfulCSV) {
        parseAwfulCSV(reader);
      } else {
        parseCSV(reader);
      }
    } else {
      parseTSV(reader);
    }
  }
  
  
  protected void parseTSV(BufferedReader reader) throws IOException {
    String line = null;
    int row = 0; 
    setRowCount(10);
    while ((line = reader.readLine()) != null) {
      if (row == getRowCount()) {
        setRowCount(row << 1);
      }
      setStringRow(row, PApplet.split(line, '\t'));
      row++;
    }
    // shorten or lengthen based on what's left
    if (row != getRowCount()) {
      setRowCount(row);
    }
  }
  
  
  protected void parseCSV(BufferedReader reader) throws IOException {
    String line = null;
    int row = 0; 
    setRowCount(10);
    while ((line = reader.readLine()) != null) {
      if (row == getRowCount()) {
        setRowCount(row << 1);
      }
      setStringRow(row, splitLineCSV(line));
      row++;
    }
    // shorten or lengthen based on what's left
    if (row != getRowCount()) {
      setRowCount(row);
    }
  }
  
  
  protected void parseAwfulCSV(BufferedReader reader) throws IOException {
    char[] c = new char[100];
    int count = 0;
    boolean insideQuote = false;
    int row = 0;
    int col = 0;
    int ch;
    while ((ch = reader.read()) != -1) {
      if (insideQuote) {
        if (ch == '\"') {
          // this is either the end of a quoted entry, or a quote character
          reader.mark(1);
          if (reader.read() == '\"') {
            // it's "", which means a quote character 
            if (count == c.length) {
              c = PApplet.expand(c);
            }
            c[count++] = '\"';
          } else {
            // nope, just the end of a quoted csv entry 
            reader.reset();
            insideQuote = false;
            // TODO nothing here that prevents bad csv data from showing up 
            // after the quote and before the comma...
//            setString(row, col, new String(c, 0, count));
//            count = 0;
//            col++;
//            insideQuote = false;
          }
        } else {  // inside a quote, but the character isn't a quote
          if (count == c.length) {
            c = PApplet.expand(c);
          }
          c[count++] = (char) ch;
        }        
      } else {  // not inside a quote
        if (ch == '\"') {
          insideQuote = true;

        } else if (ch == '\r') {
          // check to see if next is a '\n'
          reader.mark(1);
          if (reader.read() != '\n') {
            reader.reset();
          }
          setString(row, col, new String(c, 0, count));
          count = 0;
          row++;
          col = 0;

        } else if (ch == '\n') {
          setString(row, col, new String(c, 0, count));
          count = 0;
          row++;
          col = 0;

        } else if (ch == ',') {
          setString(row, col, new String(c, 0, count));
          count = 0;
          // starting a new column, make sure we have room
          col++;
          checkColumn(col);
          
        } else {  // just a regular character, add it
          if (count == c.length) {
            c = PApplet.expand(c);
          }
          c[count++] = (char) ch;
        }
      }
    }
    // catch any leftovers
    if (count > 0) {
      setString(row, col, new String(c, 0, count));
    }
  }
  

  protected String[] splitLine(String line) {
    return commaSeparatedValues ? splitLineCSV(line) : PApplet.split(line, '\t');
  }

  
  /**
   * Parse a line of text as comma-separated values, returning each value as 
   * one entry in an array of String objects. Remove quotes from entries that
   * begin and end with them, and convert 'escaped' quotes to actual quotes.
   * @param line line of text to be parsed
   * @return an array of the individual values formerly separated by commas
   */
  static public String[] splitLineCSV(String line) {
    char[] c = line.toCharArray();
    int rough = 1;  // at least one
    boolean quote = false;
    for (int i = 0; i < c.length; i++) {
      if (!quote && (c[i] == ',')) {
        rough++;
      } else if (c[i] == '\"') {
        quote = !quote;
      }
    }
    String[] pieces = new String[rough];
    int pieceCount = 0;
    int offset = 0; 
    while (offset < c.length) {
      int start = offset;
      int stop = nextComma(c, offset);
      offset = stop + 1;  // next time around, need to step over the comment
      if (c[start] == '\"' && c[stop-1] == '\"') {
        start++;
        stop--;
      }
      int i = start;
      int ii = start;
      while (i < stop) {
        if (c[i] == '\"') {
          i++;  // skip over pairs of double quotes become one
        }
        if (i != ii) {
          c[ii] = c[i];
        }
        i++;
        ii++;
      }
      String s = new String(c, start, ii - start);
      pieces[pieceCount++] = s;
    }
    // make any remaining entries blanks instead of nulls
    for (int i = pieceCount; i < pieces.length; i++) {
      pieces[i] = "";
      
    }
    return pieces;
  }
  
  
  static int nextComma(char[] c, int index) {
    boolean quote = false;
    for (int i = index; i < c.length; i++) {
      if (!quote && (c[i] == ',')) {
        return i;
      } else if (c[i] == '\"') {
        quote = !quote;
      }
    }
    return c.length;
  }


  public void writeTSV(PrintWriter writer) {
    if (columnTitles != null) {
      for (int col = 0; col < columns.length; col++) {
        if (col != 0) {
          writer.print('\t');
        }
        if (columnTitles[col] != null) {
          writer.print(columnTitles[col]);
        }
      }
      writer.println();
    }
    for (int row = 0; row < rowCount; row++) {
      for (int col = 0; col < getColumnCount(); col++) {
        if (col != 0) {
          writer.print('\t');
        }
        String entry = getString(row, col);
        // just write null entries as blanks, rather than spewing 'null' 
        // all over the spreadsheet file.
        if (entry != null) {
          writer.print(entry);
        }
      }
      writer.println();
    }
    writer.flush();
  }
  
  
  public void writeCSV(PrintWriter writer) {
    if (columnTitles != null) {
      for (int col = 0; col < columns.length; col++) {
        if (col != 0) {
          writer.print(',');
        }
        if (columnTitles[col] != null) {
          writeEntryCSV(writer, columnTitles[col]);
        }
      }
      writer.println();
    }
    for (int row = 0; row < rowCount; row++) {
      for (int col = 0; col < getColumnCount(); col++) {
        if (col != 0) {
          writer.print(',');
        }
        String entry = getString(row, col);
        // just write null entries as blanks, rather than spewing 'null' 
        // all over the spreadsheet file.
        if (entry != null) {
          writeEntryCSV(writer, entry);
        }
      }
      // Prints the newline for the row, even if it's missing
      writer.println();
    }
    writer.flush();
  }
  
  
  protected void writeEntryCSV(PrintWriter writer, String entry) {
    if (entry != null) {
      if (entry.indexOf('\"') != -1) {  // convert quotes to double quotes
        char[] c = entry.toCharArray();
        writer.print('\"');
        for (int i = 0; i < c.length; i++) {
          if (c[i] == '\"') {
            writer.print("\"\"");
          } else {
            writer.print(c[i]);
          }
        }
        writer.print('\"');
        
        // add quotes if commas or CR/LF are in the entry
      } else if (entry.indexOf(',') != -1  || 
                 entry.indexOf('\n') != -1 || 
                 entry.indexOf('\r') != -1) {
        writer.print('\"');
        writer.print(entry);
        writer.print('\"');
        
         
        // add quotes if leading or trailing space
      } else if ((entry.length() > 0) && 
                 (entry.charAt(0) == ' ' || 
                  entry.charAt(entry.length() - 1) == ' ')) {
        writer.print('\"');
        writer.print(entry);
        writer.print('\"');

      } else {
        writer.print(entry);
      }
    }
  }
  
  
  public void writeHTML(PrintWriter writer) {
    writer.println("<table>");
    for (int row = 0; row < getRowCount(); row++) {
      writer.println("  <tr>");
      for (int col = 0; col < getColumnCount(); col++) {
        String entry = getString(row, col);
        writer.print("    <td>");
        writeEntryHTML(writer, entry);
//        String clean = (entry == null) ? "" : HTMLFairy.encodeEntities(entry);
//        writer.println("    <td>" + clean + "</td>");
        writer.println("</td>");
      }
      writer.println("  </tr>");
    }
    writer.println("</table>");
    writer.flush();
  }
  
  
  protected void writeEntryHTML(PrintWriter writer, String entry) {
    //char[] chars = entry.toCharArray();
    for (char c : entry.toCharArray()) {  //chars) {
      if (c < 32 || c > 127) {
        writer.print("&#");
        writer.print((int) c);
        writer.print(';');
      } else {
        writer.print(c);
      }
    }
  }


  /**
   * Write this table as a TSV file. 
   * Exceptions will be printed, but not thrown.
   * @param file the location to write to. 
   * @return true if written successfully
   */
  public boolean writeCSV(File file) {
    try {
      writeCSV(new PrintWriter(new FileWriter(file)));
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  
  public boolean writeTSV(File file) {
    try {
      writeTSV(new PrintWriter(new FileWriter(file)));
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }


  public int getColumnCount() {
    return columns.length;
  }


  /**
   * Change the number of columns in this table. Resizes all rows to ensure
   * the same number of columns in each row. Entries in the additional (empty) 
   * columns will be set to null.
   * @param newCount
   */
  public void setColumnCount(int newCount) {
    if (columns.length != newCount) {
      columns = (Object[]) PApplet.expand(columns, newCount);
      if (columnTitles != null) {
        columnTitles = PApplet.expand(columnTitles, newCount);
      }
      if (columnTypes != null) {
        columnTypes = PApplet.expand(columnTypes, newCount);
      }
      //columnCount = newCount; 
    }
  }


  public void setColumnTitle(int col, String title) {
    checkSize(0, col);
    if (columnTitles == null) {
      columnTitles = new String[getColumnCount()];
    }
    columnTitles[col] = title;
    if (columnIndices != null) {
      columnIndices.put(title, col);
    }
  }
  
  
  public String getColumnTitle(int col) {
    return (columnTitles == null) ? null : columnTitles[col];
  }


  public int getColumnIndex(String name) {
    if (columnTitles == null) {
      System.err.println("Can't get column indices because no column titles are set.");
      return -1;
    }
    // only create this on first get(). subsequent calls to set the title will
    // also update this array, but only if it exists. 
    if (columnIndices == null) {
      columnIndices = new HashMap<String, Integer>();
      for (int col = 0; col < columnCount; col++) {
        //columnIndices.put(data[0][col], new Integer(col));
        columnIndices.put(columnTitles[col], col);
      }
    }
    Integer index = (Integer) columnIndices.get(name);
    if (index == null) {
//      System.err.println("No column named '" + name + "' was found.");
      return -1;
    }
    return index.intValue();
  }
  
  
  /**
   * Same as getColumnIndex(), but creates the column if it doesn't exist.
   * @param title column title
   * @return index of a new or existing column
   */
  public int checkColumnIndex(String title) {
    int cols = getColumnCount();
    if (columnTitles == null) {
      //setColumnCount(cols + 1);
      setColumnTitle(cols, title);
      return cols;
      
    } else {
      if (columnIndices == null) {
        columnIndices = new HashMap<String, Integer>();
        for (int col = 0; col < columnCount; col++) {
          //columnIndices.put(data[0][col], new Integer(col));
          columnIndices.put(columnTitles[col], col);
        }
      }
      Integer index = (Integer) columnIndices.get(title);
      if (index != null) {
        return index.intValue();
      }
      //setColumnCount(cols + 1);
      System.out.println("adding column " + title); 
      setColumnTitle(cols, title);
      return cols;
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  
  protected RowIterator rowIterator;

  public Iterator<TableRow> iterator() {
    if (rowIterator == null) {
      rowIterator = new RowIterator();
    }
    rowIterator.reset();
    return rowIterator;
  }


  public Iterator<TableRow> createIterator() {
    return new RowIterator();
  }


  class RowIterator implements Iterator<TableRow> {
    int row;
    TableRow tableRow = new TableRow() {
      public String getString(int column) {
        return Table.this.getString(row, column);
      }

      public String getString(String columnName) {
        return Table.this.getString(row, columnName);
      }

      public int getInt(int column) {
        return Table.this.getInt(row, column);
      }

      public int getInt(String columnName) {
        return Table.this.getInt(row, columnName);
      }

      public long getLong(int column) {
        return Table.this.getLong(row, column);
      }

      public long getLong(String columnName) {
        return Table.this.getLong(row, columnName);
      }

      public float getFloat(int column) {
        return Table.this.getFloat(row, column);
      }

      public float getFloat(String columnName) {
        return Table.this.getFloat(row, columnName);
      }    

      public double getDouble(int column) {
        return Table.this.getDouble(row, column);
      }

      public double getDouble(String columnName) {
        return Table.this.getDouble(row, columnName);
      }    
    };

    public void remove() {
      removeRow(row);
    }

    public TableRow next() {
      ++row;
//      iteratorRow.setRow(row);
//      return iteratorRow;
      return tableRow;
    }

    public boolean hasNext() {
      return row+1 < getRowCount();
    }
    
    public void reset() {
      row = -1;
    }
  }

  
  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  
  public int getRowCount() {
    return rowCount;
  }


  public void setRowCount(int newCount) {
    if (newCount != rowCount) {
      for (int c = 0; c < columns.length; c++) { 
        
      }
//      if (data == null) {
//        data = new String[newCount][getColumnCount()];
//
//      } else {
//        data = (String[][]) PApplet.expand(data, newCount);
//        // Create a new array of null Strings for the empty row 
//        for (int row = rowCount; row < newCount; row++) {
//          data[row] = new String[columnCount];
//        }
//        rowCount = newCount;
//      }
    }
  }


  public void addRow() {
    setRowCount(rowCount + 1);
  }


  public void addRow(String[] columns) {
    addRow();
    setColumnCount(columns.length);
    if (!typed) {
      PApplet.arrayCopy(columns, data[rowCount-1]);
    } else {
      for (int i = 0; i < columns.length; i++) {
        
      }
    }
  }


  public void removeRow(int dead) {
    for (int col = 0; col < columnCount; col++) {
      switch (columnTypes[col]) {
      case INT:
        int[] intTemp = new int[rowCount-1];
        int[] intData = (int[]) columns[col];
        System.arraycopy(intData[col], 0, intTemp, 0, dead);
        System.arraycopy(intData[col], dead+1, intTemp, dead, (rowCount - dead) + 1);
        columns[col] = intTemp;
        break;
      case LONG:
        long[] ltemp = new long[rowCount-1];
        System.arraycopy(longData[col], 0, ltemp, 0, dead);
        System.arraycopy(longData[col], dead+1, ltemp, dead, (rowCount - dead) + 1);
        longData[col] = ltemp;
        break;
      case FLOAT:
        float[] ftemp = new float[rowCount-1];
        System.arraycopy(floatData[col], 0, ftemp, 0, dead);
        System.arraycopy(floatData[col], dead+1, ftemp, dead, (rowCount - dead) + 1);
        floatData[col] = ftemp;
        break;
      case DOUBLE:
        double[] dtemp = new double[rowCount-1];
        System.arraycopy(doubleData[col], 0, dtemp, 0, dead);
        System.arraycopy(doubleData[col], dead+1, dtemp, dead, (rowCount - dead) + 1);
        doubleData[col] = dtemp;
        break;
      }
    }
    rowCount--;
  }


  public void insertRow(int row) {
    throw new RuntimeException("not yet implemented");
  }
  
  
  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public void insertColumn(int index) {
    if (columnTitles != null) {
      columnTitles = PApplet.splice(columnTitles, (String) null, index);
    }
    for (int row = 0; row < rowCount; row++) {
      data[row] = PApplet.splice(data[row], (String) null, index);
    }
    columnCount++;
  }


  public String[] removeColumn(String dead) {
    return removeColumn(getColumnIndex(dead));
  }


  public String[] removeColumn(int dead) {
    if (!typed) {
    String[] removed = new String[rowCount];
    for (int row = 0; row < rowCount; row++) {
      removed[row] = data[row][dead];
      for (int col = dead; col < columnCount - 1; col++) {
        data[row][col] = data[row][col+1];
      }
      data[row][columnCount-1] = null;
    }
    columnCount--;
//    if (swingModel != null) {
//      swingModel.fireTableStructureChanged();
//    }
    return removed;
    } else {
      
    }
  }
  

  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public String[] getUniqueEntries(String column) {
    return getUniqueEntries(getColumnIndex(column));
  }

  
  class HashMapSucks extends HashMap<String,Integer> {
    void increment(String what) {
      Integer value = get(what);
      if (value == null) {
        put(what, 1);
      } else {
        put(what, value + 1);
      }
    }
    
//    void check(String what) {
//      if (get(what) == null) {
//        put(what, new Object());
//      }
//    }
  }
  
  
  public String[] getUniqueEntries(int column) {
    HashMap<String,Object> found = new HashMap<String, Object>();
    for (int row = 0; row < getRowCount(); row++) {
      String s = getString(row, column);
      if (found.get(s) == null) {
        found.put(s, new Object());
      }
    }
    String[] outgoing = new String[found.size()];
    found.keySet().toArray(outgoing);
    return outgoing;
//    for (int i = 0; i < outgoing.length; i++) {
//      outgoing[i] = found.keySet().toArray(outgoing);
//    }
//    
//    return sc.keys();
  }


  public HashMap<String,Integer> getStringCount(String columnName) {
    return getStringCount(getColumnIndex(columnName));
  }


  public HashMap<String,Integer> getStringCount(int column) {
    HashMapSucks outgoing = new HashMapSucks();
    for (int row = 0; row < rowCount; row++) {
      String entry = data[row][column];
      if (entry != null) {
        outgoing.increment(entry);
      }
    }
    return outgoing;
  }


  /**
   * Return an object that maps the String values in one column back to the 
   * row from which they came. For instance, if the "name" of each row is 
   * found in the first column, getColumnRowLookup(0) would return an object
   * that would map each name back to its row. 
   */
  public HashMap<String,Integer> getLookup(int col) {
    HashMap<String,Integer> outgoing = new HashMap<String, Integer>();
    for (int row = 0; row < getRowCount(); row++) {
      outgoing.put(getString(row, col), row);
    }
    return outgoing;
  }
  
  
 //  public StringIntPairs getColumnRowLookup(int col) {
//    StringIntPairs sc = new StringIntPairs();
//    String[] column = getStringColumn(col);
//    for (int i = 0; i < column.length; i++) {
//      sc.set(column[i], i);
//    }
//    return sc;
//  }


//  public String[] getUniqueEntries(int column) {
////    HashMap indices = new HashMap();
////    for (int row = 0; row < rowCount; row++) {
////      indices.put(data[row][column], this);  // 'this' is a dummy
////    }
//    StringIntPairs sc = getStringCount(column);
//    return sc.keys();
//  }
//  
//  
//  public StringIntPairs getStringCount(String columnName) {
//    return getStringCount(getColumnIndex(columnName));
//  }
//  
//  
//  public StringIntPairs getStringCount(int column) {
//    StringIntPairs outgoing = new StringIntPairs();
//    for (int row = 0; row < rowCount; row++) {
//      String entry = data[row][column];
//      if (entry != null) {
//        outgoing.increment(entry);
//      }
//    }
//    return outgoing;
//  }
//
//  
//  /**
//   * Return an object that maps the String values in one column back to the 
//   * row from which they came. For instance, if the "name" of each row is 
//   * found in the first column, getColumnRowLookup(0) would return an object
//   * that would map each name back to its row. 
//   */
//  public StringIntPairs getColumnRowLookup(int col) {
//    StringIntPairs sc = new StringIntPairs();
//    String[] column = getStringColumn(col);
//    for (int i = 0; i < column.length; i++) {
//      sc.set(column[i], i);
//    }
//    return sc;
//  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  
  public void trim() {
    for (int row = 0; row < rowCount; row++) {
      for (int col = 0; col < columnCount; col++) {
        if (data[row][col] != null) {
          data[row][col] = PApplet.trim(data[row][col]);
        }
      }
    }
  }
  
  
  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public void setStringRow(int row, String[] what) {
    checkSize(row, what.length-1);
    data[row] = what;
  }
  
  
  public String[] getStringRow(int row) {
    String[] outgoing = new String[columnCount];
    for (int i = 0; i < columnCount; i++) {
      outgoing[i] = getString(row, i);
    }
    return outgoing;
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  
  public String[] getStringColumn(String name) {
    int col = getColumnIndex(name);
    return (col == -1) ? null : getStringColumn(col);
  }
  

  public String[] getStringColumn(int col) {
    String[] outgoing = new String[rowCount];
    for (int i = 0; i < rowCount; i++) {
      outgoing[i] = getString(i, col);
    }
    return outgoing;
  }
  

  public int[] getIntColumn(String name) {
    int col = getColumnIndex(name);
    return (col == -1) ? null : getIntColumn(col);
  }
  

  public int[] getIntColumn(int col) {
    int[] outgoing = new int[rowCount];
    for (int row = 0; row < rowCount; row++) {
      outgoing[row] = getInt(row, col);
    }
    return outgoing;
  }
  

  public long[] getLongColumn(String name) {
    int col = getColumnIndex(name);
    return (col == -1) ? null : getLongColumn(col);
  }


  public long[] getLongColumn(int col) {
    long[] outgoing = new long[rowCount];
    for (int row = 0; row < rowCount; row++) {
      outgoing[row] = getLong(row, col);
    }
    return outgoing;
  }

  
  public float[] getFloatColumn(String name) {
    int col = getColumnIndex(name);
    return (col == -1) ? null : getFloatColumn(col);
  }
  
  
  public float[] getFloatColumn(int col) {
    float[] outgoing = new float[rowCount];
    for (int row = 0; row < rowCount; row++) {
      outgoing[row] = getFloat(row, col);
    }
    return outgoing;
  }


  public double[] getDoubleColumn(String name) {
    int col = getColumnIndex(name);
    return (col == -1) ? null : getDoubleColumn(col);
  }
  
  
  public double[] getDoubleColumn(int col) {
    double[] outgoing = new double[rowCount];
    for (int row = 0; row < rowCount; row++) {
      outgoing[row] = getDouble(row, col);
    }
    return outgoing;
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public void setMissingString(String value) {
    missingString = value;
  }


  public String getString(int row, int column) {
    if (row >= getRowCount()) {
      return null;
    }
    if (!typed) {
      if (data[row] == null) {
        return null;
      }
      if (column >= data[row].length) {
        return null;
      }
      return data[row][column];
    } else {
      
    }
  }


  public String getString(int row, String columnName) {
    return getString(row, getColumnIndex(columnName));
  }

  
  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public void setMissingInt(int value) {
    missingInt = value;
  }
  
  
  public int getInt(int row, int column) {
    String str = getString(row, column);
    return (str == null) ? missingInt : PApplet.parseInt(str, missingInt);
  }
  
  
  public int getInt(int row, String columnName) {
    return getInt(row, getColumnIndex(columnName));
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  
  public void setMissingLong(long value) {
    missingLong = value;
  }

  
  public long getLong(int row, int column) {
    String str = getString(row, column);
    return (str == null) ? missingLong : Long.parseLong(str);
  }

  
//  public long getLong(String rowName, int column) {
//    return getLong(getRowIndex(rowName), column);
//  }


  public long getLong(int row, String columnName) {
    return getLong(row, getColumnIndex(columnName));
  }


//  public long getLong(String rowName, String columnName) {
//    return getLong(getRowIndex(rowName), getColumnIndex(columnName));
//  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public void setMissingFloat(float value) {
    missingFloat = value;
  }

  
  /**
   * Get a float value from the specified row and column. If the value is null
   * or not parseable as a float, the "missing" value is returned. By default, 
   * this is Float.NaN, but can be controlled with setMissingFloat().
   */
  public float getFloat(int row, int column) {
    String str = getString(row, column);
    return (str == null) ? missingFloat : PApplet.parseFloat(str, missingFloat);
  }
  

//  public float getFloat(String rowName, int column) {
//    return getFloat(getRowIndex(rowName), column);
//  }


  public float getFloat(int row, String columnName) {
    return getFloat(row, getColumnIndex(columnName));
  }


//  public float getFloat(String rowName, String columnName) {
//    return getFloat(getRowIndex(rowName), getColumnIndex(columnName));
//  }

  
  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public void setMissingDouble(double value) {
    missingDouble = value;
  }

  
  public double getDouble(int row, int column) {
    String str = getString(row, column);
    return (str == null) ? missingDouble : Double.parseDouble(str);
  }


//  public double getDouble(String rowName, int column) {
//    return getDouble(getRowIndex(rowName), column);
//  }


  public double getDouble(int row, String columnName) {
    return getDouble(row, getColumnIndex(columnName));
  }


//  public double getDouble(String rowName, String columnName) {
//    return getDouble(getRowIndex(rowName), getColumnIndex(columnName));
//  }

  
  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  
//public long getTimestamp(String rowName, int column) {
//return getTimestamp(getRowIndex(rowName), column);
//}


  /**
   * Returns the time in milliseconds by parsing a SQL Timestamp at this cell.
   */
//  public long getTimestamp(int row, int column) {
//    String str = getString(row, column);
//    java.sql.Timestamp timestamp = java.sql.Timestamp.valueOf(str);
//    return timestamp.getTime();
//  }
  
  
//  public long getExcelTimestamp(int row, int column) {
//    return parseExcelTimestamp(getString(row, column));
//  }
  
  
//  static protected DateFormat excelDateFormat;

//  static public long parseExcelTimestamp(String timestamp) {
//    if (excelDateFormat == null) {
//      excelDateFormat = new SimpleDateFormat("MM/dd/yy HH:mm");
//    }
//    try {
//      return excelDateFormat.parse(timestamp).getTime();
//    } catch (ParseException e) {
//      e.printStackTrace();
//      return -1;
//    }
//  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


//  public void setRowName(int row, String what) {
//    checkSize(row, 0);
//    if (rowTitles == null) {
//      rowTitles = new String[getRowCount()];
//    }
//    //setString(row, 0, what);
//    rowTitles[row] = what;
//  }


  public void setColumnName(int column, String what) {
    checkSize(0, column);
    if (columnTitles == null) {
      columnTitles = new String[getColumnCount()];
    }
    columnTitles[column] = what;
    if (columnIndices != null) {
      columnIndices.put(what, column);
    }
  }


  public void setString(int row, int column, String what) {
    checkSize(row, column);
    data[row][column] = what;
//    if (swingModel != null) {
//      swingModel.fireTableCellUpdated(row, column);
//    }
  }


  public void setString(int row, String columnName, String what) {
    int column = getColumnIndex(columnName);
    setString(row, column, what);
  }

  
//  public void setString(String rowName, int column, String what) {
//    int row = getRowIndex(rowName);
//    setString(row, column, what);
//  }


  public void setInt(int row, int column, int what) {
    setString(row, column, PApplet.str(what));
  }


//  public void setInt(String rowName, int column, int what) {
//    setString(rowName, column, PApplet.str(what));
//  }


  public void setFloat(int row, int column, float what) {
    setString(row, column, PApplet.str(what));
  }


//  public void setFloat(String rowName, int column, float what) {
//    setString(rowName, column, PApplet.str(what));
//  }


  public void setObject(int row, int column, Object value) {
    if (value == null) {
      data[row][column] = null;
    } else if (value instanceof String) {
      setString(row, column, (String) value);
    } else if (value instanceof Float) { 
      setFloat(row, column, ((Float) value).floatValue());
    } else if (value instanceof Integer) { 
      setInt(row, column, ((Integer) value).intValue());
    } else {
      setString(row, column, value.toString());
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  
  /** 
   * Set all 'null' entries to "" (zero length String objects).
   */
  public void makeNullEmpty() {
    for (int row = 0; row < rowCount; row++) {
      for (int col = 0; col < columnCount; col++) {
        if (data[row][col] == null) {
          data[row][col] = "";
        }
      }
    }
  }

  
  /** 
   * Set all "" entries (zero length String objects) to null values.
   */
  public void makeEmptyNull() {
    for (int row = 0; row < rowCount; row++) {
      for (int col = 0; col < columnCount; col++) {
        String entry = data[row][col]; 
        if (entry != null && entry.length() == 0) {
          data[row][col] = null;
        }
      }
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  
  /**
   * Searches the entire table for float values.
   * Returns missing float (Float.NaN by default) if no valid numbers found.
   */
  public float getMaxFloat() {
    boolean found = false;
    float max = missingFloat;
    for (int row = 0; row < getRowCount(); row++) {
      for (int col = 0; col < getColumnCount(); col++) {
        float value = getFloat(row, col);
        if (!Float.isNaN(value)) {  // TODO no, this should be comparing to the missing value
          if (!found) {
            max = value;
            found = true;
          } else if (value > max) {
            max = value;
          }
        }
      }
    }
    return max;
  }
  
  
  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  
  /**
   * Remove any of the specified characters from the entire table.
   */
  public void removeTokens(String tokens) {
    for (int col = 0; col < getColumnCount(); col++) {
      removeTokens(tokens, col);
    }
  }
  
  
  /**
   * Removed any of the specified characters from a column. For instance,
   * the following code removes dollar signs and commas from column 2: 
   * <pre>
   * table.removeTokens(",$", 2);
   * </pre>
   */
  public void removeTokens(String tokens, int column) {
    for (int row = 0; row < rowCount; row++) {
      String s = getString(row, column);
      if (s != null) {
        char[] c = s.toCharArray();
        int index = 0;
        for (int j = 0; j < c.length; j++) {
          if (tokens.indexOf(c[j]) == -1) {
            if (index != j) {
              c[index] = c[j];
            }
            index++;
          }
        }
        if (index != c.length) {
          setString(row, column, new String(c, 0, index));
        }
      }
    }
  }
  
  
  public void removeTokens(String tokens, String column) {
    removeTokens(tokens, getColumnIndex(column));
  }
  
  
  // TODO this isn't i18n correct, and it's a dumb implementation
//  public void removeLetters(int column) {
//    String alphabet = "abcdefghijklmnopqrstuvwxyz";
//    removeTokens(alphabet + alphabet.toUpperCase(), column);
//  }

  
  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  
  // get lists of strings that match in either a row or a column 
  

  /**
   * Return the row that contains the first String that matches.
   * @param what the String to match
   * @param column the column to search
   */
  public int findString(String what, int column) {
    if (what == null) {
      for (int row = 0; row < rowCount; row++) {
        if (data[row][column] == null) return row;
      }
    } else {
      for (int row = 0; row < rowCount; row++) {
        String value = data[row][column];
        if (value != null && value.equals(what)) {
          return row;
        }
      }
    }
    return -1;
  }
  
  
  /**
   * Return the row that contains the first String that matches.
   * @param what the String to match
   * @param columnName the column to search
   */
  public int findString(String what, String columnName) {
    return findString(what, getColumnIndex(columnName));
  }


  /**
   * Return a list of rows that contain the String passed in.
   * @param what the String to match
   * @param column the column to search
   */
  public int[] findStrings(String what, int column) {
    int[] outgoing = new int[rowCount];
    int count = 0;
    if (what == null) {
      for (int row = 0; row < rowCount; row++) {
        if (data[row][column] == null) {
          outgoing[count++] = row;
        }
      }
    } else {
      for (int row = 0; row < rowCount; row++) {
        String value = data[row][column];
        if (value != null && value.equals(what)) {
          outgoing[count++] = row;
        }
      }
    }
    return PApplet.subset(outgoing, 0, count);
  }
  
  
  /**
   * Return a list of rows that contain the String passed in.
   * @param what the String to match
   * @param columnName the column to search
   */
  public int[] findStrings(String what, String columnName) {
    return findStrings(what, getColumnIndex(columnName));
  }
  
  
  /**
   * Return the column that contains the first String that matches in a row.
   * @param row the row to search
   * @param what the String to match
   */
//  public int findString(int row, String what) {
//    if (what == null) {
//      for (int column = 0; column < columnCount; column++) {
//        if (data[row][column] == null) return column;
//      }
//    } else {
//      for (int column = 0; column < columnCount; column++) {
//        String value = data[row][column];
//        if (value != null && value.equals(what)) {
//          return column;
//        }
//      }
//    }
//    return -1;
//  }


  /**
   * Return a list of columns that contain the String passed in.
   * @param row the row to search
   * @param what the String to match
   */
//  public int[] findStrings(int row, String what) {
//    int[] outgoing = new int[columnCount];
//    int count = 0;
//    if (what == null) {
//      for (int column = 0; column < columnCount; column++) {
//        if (data[row][column] == null) {
//          outgoing[count++] = row;
//        }
//      }
//    } else {
//      for (int column = 0; column < columnCount; column++) {
//        String value = data[row][column];
//        if (value != null && value.equals(what)) {
//          outgoing[count++] = column;
//        }
//      }
//    }
//    return PApplet.subset(outgoing, 0, count);
//  }
  

  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  protected void checkColumn(int col) {
    if (col >= columnCount) {
//      if (expanding) {
//        int dubble = columnCount << 1; 
//        if (dubble > col) {
//          // if big enough, just continue the doubling
//          setColumnCount(dubble);
//        } else {
//          // just go arbitrarily large
//          setColumnCount(col << 1);
//        }
//      } else {
      setColumnCount(col + 1);
//      }
    }
  }
  
  
  protected void checkRow(int row) {
    if (row >= rowCount) {
//      if (expanding) {
//        int dubble = rowCount << 1; 
//        if (dubble > row) {
//          // if big enough, just continue the doubling
//          setRowCount(dubble);
//        } else {
//          // just go arbitrarily large
//          setRowCount(row << 1);
//        }
//      } else {
      setRowCount(row + 1);
//      }
    }
  }
  
  
  protected void checkSize(int row, int col) {
//    System.out.println("checking size to set row " + row + ", col " + col);
    if (rowCount == 0 && columnCount == 0) {
      rowCount = row + 1;
      columnCount = col + 1;
      data = new String[rowCount][columnCount];
    } else {
      checkRow(row);
      checkColumn(col);
    }
//    if (row >= data.length) {
//      data = (String[][]) PApplet.expand(data, row+1);
//    }
//    if (data[row] == null) {
//      data[row] = new String[columnCount];
//    }
//    if (col >= data[row].length) {
//      data[row] = (String[]) PApplet.expand(data[row], col+1);
//    }
  }

//  private void checkSize(int row, int col) {
//    if (row >= data.length) {
//      data = (String[][]) PApplet.expand(data, row+1);
//    }
//    if (col >= data[row].length) {
//      data[row] = (String[]) PApplet.expand(data[row], col+1);
//    }
//  }
  
  
  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public Table subsetRows(int[] rows) {
    Table newbie = new Table();
    for (int row : rows) {
      newbie.addRow(getStringRow(row));
    }
    return newbie;
  }
  
  
  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


//  public void hasColumnTitles(boolean titles) {
//    boolean titledAlready = (columnTitles != null);
//    if (titles && !titledAlready) {
//      
//
//    } else if (!titles && titledAlready) {
//      // take the column titles and put them back
//    }
//    if (hasTitleRow != (titleRow != null)) {
//      titleRow = hasTitleRow;
//      if (titleRow) {
//        // grab the titles and set them aside in another array
//        
//      }
//    }
//  }
  

  /*
  public void hasTitles(boolean row, boolean column) {
    if (row && column) {
      columnTitles = PApplet.subset(removeRow(0), 1);
      rowTitles = removeColumn(0);

    } else if (column) {
      columnTitles = removeRow(0);

    } else if (row) {
      rowTitles = removeColumn(0);
    }
  }
  */


  public void setColumnTitles(String[] titles) {
    if (titles != null) {
      // don't add any rows, (using 0 instead of -1 below would add row 0)
      checkSize(-1, titles.length - 1);
    }
    columnTitles = titles;
    columnIndices = null;  // remove the cache
  }
  
  
//  public void setRowTitles(String[] titles) {
//    rowTitles = titles;
//  }


//  public void setTitles() {
//    setTitles(0, 0);
//  }
  
  
//  public void setTitles(int row, int column) {
//    columnTitleRow = 0;
//    rowTitleColumn = 0;
//  }
  

//  /**
//   * Remove the first row and column from the data set, and use them as 
//   * the column and row titles.
//   */
//  public void removeTitles() {
//    columnTitles = PApplet.subset(removeRow(0), 1);
//    rowTitles = removeColumn(0);
//  }
  
  
//  public void setTitleRow(int row) {
//    columnTitles = PApplet.subset(removeRow(0), 1);
//  }


  /**
   * Remove the first row from the data set, and use it as the column titles.
   */
  public String[] removeTitleRow() {
    return removeTitleRow(0);
  }
  
  
  public String[] removeTitleRow(int row) {
    columnTitles = removeRow(row);
    columnIndices = null;
    return columnTitles;
  }


//  public String[] removeTitleColumn() {
//    return removeTitleColumn(0);
//  }
  
  
//  /**
//   * Remove the specified column from the data set, and use it as the row titles.
//   */
//  public String[] removeTitleColumn(int column) {
//    rowTitles = removeColumn(column);
//    rowIndices = null;
//    return rowTitles;
//  }


  public String[] getColumnTitles() {
    return columnTitles;
  }
  
  
//  public String[] getRowTitles() {
//    return rowTitles;
//  }
}

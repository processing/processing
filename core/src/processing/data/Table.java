/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2011-12 Ben Fry and Casey Reas
  Copyright (c) 2006-11 Ben Fry

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

package processing.data;

import java.io.*;
import java.lang.reflect.Array;
import java.sql.*;
import java.util.*;

import processing.core.PApplet;
import processing.core.PConstants;

// function that will convert awful CSV to TSV.. or something else?
//   maybe to write binary instead? then read the binary file once it's ok?

// if loading from a File object (or PApplet is passed in and we can check online)
// then check the (probable) size of the file before loading

// implement binary tables

// no column max/min functions since it needs to be per-datatype
// better to use float mx = max(float(getColumn(3)));
// *** but what to do with null entries?

// todo: need a method to reset the row/column indices after add/remove
// or just make sure that it's covered for all cases

// no longer the case, ja?
// <p>By default, empty rows are skipped and so are lines that start with the
// # character. Using # at the beginning of a line indicates a comment.</p>


/**
 * <p>Generic class for handling tabular data, typically from a CSV, TSV, or
 * other sort of spreadsheet file.</p>
 * <p>CSV files are
 * <a href="http://en.wikipedia.org/wiki/Comma-separated_values">comma separated values</a>,
 * often with the data in quotes. TSV files use tabs as separators, and usually
 * don't bother with the quotes.</p>
 * <p>File names should end with .csv if they're comma separated.</p>
 *
 * @webref data:composite
 */
public class Table implements Iterable<TableRow> {
  protected int rowCount;

//  protected boolean skipEmptyRows = true;
//  protected boolean skipCommentLines = true;
  protected boolean commaSeparatedValues = false;
  protected boolean awfulCSV = false;

  protected String missingString = null;
  protected int missingInt = 0;
  protected long missingLong = 0;
  protected float missingFloat = Float.NaN;
  protected double missingDouble = Double.NaN;
  protected int missingCategory = -1;

  String[] columnTitles;
  HashMapBlows[] columnCategories;
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
  static final int CATEGORICAL = 5;
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
    columns = new Object[0];
    columnTypes = new int[0];
    columnCategories = new HashMapBlows[0];
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
    columns = new Object[0];
    columnTypes = new int[0];
    columnCategories = new HashMapBlows[0];
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


  public Table(ResultSet rs) {
    this();
    try {
      ResultSetMetaData rsmd = rs.getMetaData();

      int columnCount = rsmd.getColumnCount();
      setColumnCount(columnCount);

      for (int col = 0; col < columnCount; col++) {
        setColumnTitle(col, rsmd.getColumnName(col + 1));

        int type = rsmd.getColumnType(col + 1);
        switch (type) {  // TODO these aren't tested. nor are they complete.
        case Types.INTEGER:
        case Types.TINYINT:
        case Types.SMALLINT:
          setColumnType(col, INT);
          break;
        case Types.BIGINT:
          setColumnType(col, LONG);
          break;
        case Types.FLOAT:
          setColumnType(col, FLOAT);
          break;
        case Types.DECIMAL:
        case Types.DOUBLE:
        case Types.REAL:
          setColumnType(col, DOUBLE);
          break;
        }
      }

      int row = 0;
      while (rs.next()) {
        for (int col = 0; col < columnCount; col++) {
          switch (columnTypes[col]) {
          case STRING: setString(row, col, rs.getString(col+1)); break;
          case INT: setInt(row, col, rs.getInt(col+1)); break;
          case LONG: setLong(row, col, rs.getLong(col+1)); break;
          case FLOAT: setFloat(row, col, rs.getFloat(col+1)); break;
          case DOUBLE: setDouble(row, col, rs.getDouble(col+1)); break;
          default: throw new IllegalArgumentException("column type " + columnTypes[col] + " not supported.");
          }
        }
        row++;
//        String[] row = new String[columnCount];
//        for (int col = 0; col < columnCount; col++) {
//          row[col] = rs.get(col + 1);
//        }
//        addRow(row);
      }

    } catch (SQLException s) {
      throw new RuntimeException(s);
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


  public void parse(BufferedReader reader) throws IOException {
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


  public void parseTSV(BufferedReader reader) throws IOException {
    parseBasic(reader, true);
//    String line = null;
//    int row = 0;
//    if (rowCount == 0) {
//      setRowCount(10);
//    }
//    while ((line = reader.readLine()) != null) {
//      if (row == getRowCount()) {
//        setRowCount(row << 1);
//      }
//      setRow(row, PApplet.split(line, '\t'));
//      row++;
//    }
//    // shorten or lengthen based on what's left
//    if (row != getRowCount()) {
//      setRowCount(row);
//    }
  }


  public void parseCSV(BufferedReader reader) throws IOException {
    parseBasic(reader, false);
//    String line = null;
//    int row = 0;
//    if (rowCount == 0) {
//      setRowCount(10);
//    }
//    while ((line = reader.readLine()) != null) {
//      if (row == getRowCount()) {
//        setRowCount(row << 1);
//      }
//      setRow(row, splitLineCSV(line));
//      row++;
//    }
//    // shorten or lengthen based on what's left
//    if (row != getRowCount()) {
//      setRowCount(row);
//    }
  }


  protected void parseBasic(BufferedReader reader, boolean tsv) throws IOException {
    String line = null;
    int row = 0;
    if (rowCount == 0) {
      setRowCount(10);
    }
    int prev = 0;  //-1;
    while ((line = reader.readLine()) != null) {
      if (row == getRowCount()) {
        setRowCount(row << 1);
      }
      setRow(row, tsv ? PApplet.split(line, '\t') : splitLineCSV(line));
      row++;

      if (row % 10000 == 0) {
        if (row < rowCount) {
          int pct = (100 * row) / rowCount;
          if (pct != prev) {  // also prevents "0%" from showing up
            System.out.println(pct + "%");
            prev = pct;
          }
        }
        try {
          Thread.sleep(5);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
    // shorten or lengthen based on what's left
    if (row != getRowCount()) {
      setRowCount(row);
    }
  }


  public void convertTSV(BufferedReader reader, File outputFile) throws IOException {
    convertBasic(reader, true, outputFile);
  }


  protected void convertBasic(BufferedReader reader, boolean tsv,
                              File outputFile) throws IOException {
    FileOutputStream fos = new FileOutputStream(outputFile);
    BufferedOutputStream bos = new BufferedOutputStream(fos, 16384);
    DataOutputStream output = new DataOutputStream(bos);
    output.writeInt(0);  // come back for row count
    output.writeInt(getColumnCount());
    if (columnTitles != null) {
      output.writeBoolean(true);
      for (String title : columnTitles) {
        output.writeUTF(title);
      }
    } else {
      output.writeBoolean(false);
    }
    for (int type : columnTypes) {
      output.writeInt(type);
    }

    String line = null;
    //setRowCount(1);
    int prev = -1;
    int row = 0;
    while ((line = reader.readLine()) != null) {
      convertRow(output, tsv ? PApplet.split(line, '\t') : splitLineCSV(line));
      row++;

      if (row % 10000 == 0) {
        if (row < rowCount) {
          int pct = (100 * row) / rowCount;
          if (pct != prev) {
            System.out.println(pct + "%");
            prev = pct;
          }
        }
//        try {
//          Thread.sleep(5);
//        } catch (InterruptedException e) {
//          e.printStackTrace();
//        }
      }
    }
    // shorten or lengthen based on what's left
//    if (row != getRowCount()) {
//      setRowCount(row);
//    }

    // has to come afterwards, since these tables get built out during the conversion
    int col = 0;
    for (HashMapBlows hmb : columnCategories) {
      if (hmb == null) {
        output.writeInt(0);
      } else {
        hmb.write(output);
        hmb.writeln(PApplet.createWriter(new File(columnTitles[col] + ".categories")));
//        output.writeInt(hmb.size());
//        for (Map.Entry<String,Integer> e : hmb.entrySet()) {
//          output.writeUTF(e.getKey());
//          output.writeInt(e.getValue());
//        }
      }
      col++;
    }

    output.flush();
    output.close();

    // come back and write the row count
    RandomAccessFile raf = new RandomAccessFile(outputFile, "rw");
    raf.writeInt(rowCount);
    raf.close();
  }


  public void parseAwfulCSV(BufferedReader reader) throws IOException {
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
//            set(row, col, new String(c, 0, count));
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


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public void addColumn() {
    addColumn(null, STRING);
  }


  public void addColumn(String title) {
    addColumn(title, STRING);
  }


  public void addColumn(String title, int type) {
    insertColumn(columns.length, title, type);
  }


  public void insertColumn(int index) {
    insertColumn(index, null, STRING);
  }


  public void insertColumn(int index, String title) {
    insertColumn(index, title, STRING);
  }


  public void insertColumn(int index, String title, int type) {
    if (title != null && columnTitles == null) {
      columnTitles = new String[columns.length];
    }
    if (columnTitles != null) {
      columnTitles = PApplet.splice(columnTitles, title, index);
      columnIndices = null;
    }
    columnTypes = PApplet.splice(columnTypes, type, index);

//    columnCategories = (HashMapBlows[])
//      PApplet.splice(columnCategories, new HashMapBlows(), index);
    HashMapBlows[] catTemp = new HashMapBlows[columns.length + 1];
    // Faster than arrayCopy for a dozen or so entries
    for (int i = 0; i < index; i++) {
      catTemp[i] = columnCategories[i];
    }
    catTemp[index] = new HashMapBlows();
    for (int i = index; i < columns.length; i++) {
      catTemp[i+1] = columnCategories[i];
    }
    columnCategories = catTemp;

    Object[] temp = new Object[columns.length + 1];
    System.arraycopy(columns, 0, temp, 0, index);
    System.arraycopy(columns, index, temp, index+1, columns.length - index);
    columns = temp;

    switch (type) {
      case INT: columns[index] = new int[rowCount]; break;
      case LONG: columns[index] = new long[rowCount]; break;
      case FLOAT: columns[index] = new float[rowCount]; break;
      case DOUBLE: columns[index] = new double[rowCount]; break;
      case STRING: columns[index] = new String[rowCount]; break;
      case CATEGORICAL: columns[index] = new int[rowCount]; break;
    }
  }


  public void removeColumn(String dead) {
    removeColumn(getColumnIndex(dead));
  }


  public void removeColumn(int index) {
    Object[] temp = new Object[columns.length + 1];
    System.arraycopy(columns, 0, temp, 0, index);
    System.arraycopy(columns, index+1, temp, index, (columns.length - index) + 1);
    columns = temp;
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
    int oldCount = columns.length;
    if (oldCount != newCount) {
      columns = (Object[]) PApplet.expand(columns, newCount);
      // create new columns, default to String as the data type
      for (int c = oldCount; c < newCount; c++) {
        columns[c] = new String[rowCount];
      }

      if (columnTitles != null) {
        columnTitles = PApplet.expand(columnTitles, newCount);
      }
      columnTypes = PApplet.expand(columnTypes, newCount);
      columnCategories = (HashMapBlows[])
        PApplet.expand(columnCategories, newCount);
    }
  }


  public void setColumnType(String columnName, String columnType) {
    setColumnType(checkColumnIndex(columnName), columnType);
  }


  /**
   * Set the data type for a column so that using it is more efficient.
   * @param column the column to change
   * @param columnType One of int, long, float, double, or String.
   */
  public void setColumnType(int column, String columnType) {
    int type = -1;
    if (columnType.equals("String")) {
      type = STRING;
    } else if (columnType.equals("int")) {
      type = INT;
    } else if (columnType.equals("long")) {
      type = LONG;
    } else if (columnType.equals("float")) {
      type = FLOAT;
    } else if (columnType.equals("double")) {
      type = DOUBLE;
    } else if (columnType.equals("categorical")) {
      type = CATEGORICAL;
    } else {
      throw new IllegalArgumentException("'" + columnType + "' is not a valid column type.");
    }
    setColumnType(column, type);
  }


  protected void setColumnType(String columnName, int newType) {
    setColumnType(checkColumnIndex(columnName), newType);
  }


  /**
   * Sets the column type. If data already exists, then it'll be converted to
   * the new type.
   * @param column the column whose type should be changed
   * @param newType something fresh, maybe try an int or a float for size?
   */
  protected void setColumnType(int column, int newType) {
    switch (newType) {
      case INT: {
        int[] intData = new int[rowCount];
        for (int row = 0; row < rowCount; row++) {
          String s = getString(row, column);
          intData[row] = PApplet.parseInt(s, missingInt);
        }
        columns[column] = intData;
        break;
      }
      case LONG: {
        long[] longData = new long[rowCount];
        for (int row = 0; row < rowCount; row++) {
          String s = getString(row, column);
          try {
            longData[row] = Long.parseLong(s);
          } catch (NumberFormatException nfe) {
            longData[row] = missingLong;
          }
        }
        columns[column] = longData;
        break;
      }
      case FLOAT: {
        float[] floatData = new float[rowCount];
        for (int row = 0; row < rowCount; row++) {
          String s = getString(row, column);
          floatData[row] = PApplet.parseFloat(s, missingFloat);
        }
        columns[column] = floatData;
        break;
      }
      case DOUBLE: {
        double[] doubleData = new double[rowCount];
        for (int row = 0; row < rowCount; row++) {
          String s = getString(row, column);
          try {
            doubleData[row] = Double.parseDouble(s);
          } catch (NumberFormatException nfe) {
            doubleData[row] = missingDouble;
          }
        }
        columns[column] = doubleData;
        break;
      }
      case STRING: {
        if (columnTypes[column] != STRING) {
          String[] stringData = new String[rowCount];
          for (int row = 0; row < rowCount; row++) {
            stringData[row] = getString(row, column);
          }
          columns[column] = stringData;
        }
        break;
      }
      case CATEGORICAL: {
        int[] indexData = new int[rowCount];
        HashMapBlows categories = new HashMapBlows();
        for (int row = 0; row < rowCount; row++) {
          String s = getString(row, column);
          indexData[row] = categories.index(s);
        }
        columnCategories[column] = categories;
        columns[column] = indexData;
        break;
      }
      default: {
        throw new IllegalArgumentException("That's not a valid column type.");
      }
    }
//    System.out.println("new type is " + newType);
    columnTypes[column] = newType;
  }


  /**
   * Set the entire table to a specific data type.
   */
  public void setTableType(String type) {
    for (int col = 0; col < columns.length; col++) {
      setColumnType(col, type);
    }
  }


  /**
   * Set the titles (and if a second column is present) the data types for
   * this table based on a file loaded separately.
   * @param dictionary
   */
  public void setColumnTypes(Table dictionary) {
    setColumnTitles(dictionary.getStringColumn(0));
    if (dictionary.getColumnCount() > 1) {
      for (int i = 0; i < dictionary.getRowCount(); i++) {
        setColumnType(i, dictionary.getString(i, 1));
      }
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Remove the first row from the data set, and use it as the column titles.
   */
  public String[] removeTitleRow() {
    String[] titles = getStringRow(0);
    removeRow(0);
    setColumnTitles(titles);
    return titles;
  }


  public void setColumnTitles(String[] titles) {
    if (titles != null) {
      checkColumn(titles.length - 1);
    }
    columnTitles = titles;
    columnIndices = null;  // remove the cache
  }


  public void setColumnTitle(int column, String title) {
    checkColumn(column);
    if (columnTitles == null) {
      columnTitles = new String[getColumnCount()];
    }
    columnTitles[column] = title;
    columnIndices = null;  // reset these fellas
  }


  public String[] getColumnTitles() {
    return columnTitles;
  }


  public String getColumnTitle(int col) {
    return (columnTitles == null) ? null : columnTitles[col];
  }


  public int getColumnIndex(String name) {
    return getColumnIndex(name, true);
  }


  /**
   * Get the index of a column.
   * @param name Name of the column.
   * @param report Whether to print to System.err if the column wasn't found.
   * @return index of the found column, or -1 if not found.
   */
  protected int getColumnIndex(String name, boolean report) {
    if (columnTitles == null) {
      if (report) {
        System.err.println("Can't get column indices because no column titles are set.");
      }
      return -1;
    }
    // only create this on first get(). subsequent calls to set the title will
    // also update this array, but only if it exists.
    if (columnIndices == null) {
      columnIndices = new HashMap<String, Integer>();
      for (int col = 0; col < columns.length; col++) {
        columnIndices.put(columnTitles[col], col);
      }
    }
    Integer index = columnIndices.get(name);
    if (index == null) {
      if (report) {
        System.err.println("No column named '" + name + "' was found.");
      }
      return -1;
    }
    return index.intValue();
  }


  /**
   * Same as getColumnIndex(), but creates the column if it doesn't exist.
   * Named this way to not conflict with checkColumn(), an internal function
   * used to ensure that a columns exists, and also to denote that it returns
   * an int for the column index.
   * @param title column title
   * @return index of a new or previously existing column
   */
  public int checkColumnIndex(String title) {
    int index = getColumnIndex(title, false);
    if (index != -1) {
      return index;
    }
    addColumn(title);
    return getColumnCount() - 1;
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public int getRowCount() {
    return rowCount;
  }


  public void setRowCount(int newCount) {
    if (newCount != rowCount) {
      if (newCount > 1000000) {
        System.out.println("setting row count to " + PApplet.nfc(newCount));
      }
      long t = System.currentTimeMillis();
      for (int col = 0; col < columns.length; col++) {
        switch (columnTypes[col]) {
          case INT: columns[col] = PApplet.expand((int[]) columns[col], newCount); break;
          case LONG: columns[col] = PApplet.expand((long[]) columns[col], newCount); break;
          case FLOAT: columns[col] = PApplet.expand((float[]) columns[col], newCount); break;
          case DOUBLE: columns[col] = PApplet.expand((double[]) columns[col], newCount); break;
          case STRING: columns[col] = PApplet.expand((String[]) columns[col], newCount); break;
          case CATEGORICAL: columns[col] = PApplet.expand((int[]) columns[col], newCount); break;
        }
        if (newCount > 1000000) {
          try {
            Thread.sleep(10);  // gc time!
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
      if (newCount > 1000000) {
        int ms = (int) (System.currentTimeMillis() - t);
        System.out.println("  resize took " + PApplet.nfc(ms) + " ms");
      }
    }
    rowCount = newCount;
  }


  public void addRow() {
    setRowCount(rowCount + 1);
  }


  public void addRow(String[] columns) {
    setRow(getRowCount(), columns);
  }


  public void insertRow(int insert, String[] data) {
    for (int col = 0; col < columns.length; col++) {
      switch (columnTypes[col]) {
        case CATEGORICAL:
        case INT: {
          int[] intTemp = new int[rowCount+1];
          System.arraycopy(columns[col], 0, intTemp, 0, insert);
          System.arraycopy(columns[col], insert, intTemp, insert+1, (rowCount - insert) + 1);
          columns[col] = intTemp;
          break;
        }
        case LONG: {
          long[] longTemp = new long[rowCount+1];
          System.arraycopy(columns[col], 0, longTemp, 0, insert);
          System.arraycopy(columns[col], insert, longTemp, insert+1, (rowCount - insert) + 1);
          columns[col] = longTemp;
          break;
        }
        case FLOAT: {
          float[] floatTemp = new float[rowCount+1];
          System.arraycopy(columns[col], 0, floatTemp, 0, insert);
          System.arraycopy(columns[col], insert, floatTemp, insert+1, (rowCount - insert) + 1);
          columns[col] = floatTemp;
          break;
        }
        case DOUBLE: {
          double[] doubleTemp = new double[rowCount+1];
          System.arraycopy(columns[col], 0, doubleTemp, 0, insert);
          System.arraycopy(columns[col], insert, doubleTemp, insert+1, (rowCount - insert) + 1);
          columns[col] = doubleTemp;
          break;
        }
        case STRING: {
          String[] stringTemp = new String[rowCount+1];
          System.arraycopy(columns[col], 0, stringTemp, 0, insert);
          System.arraycopy(columns[col], insert, stringTemp, insert+1, (rowCount - insert) + 1);
          columns[col] = stringTemp;
          break;
        }
      }
    }
    setRow(insert, data);
    rowCount++;
  }


  public void removeRow(int dead) {
    for (int col = 0; col < columns.length; col++) {
      switch (columnTypes[col]) {
        case CATEGORICAL:
        case INT: {
          int[] intTemp = new int[rowCount-1];
//          int[] intData = (int[]) columns[col];
//          System.arraycopy(intData, 0, intTemp, 0, dead);
//          System.arraycopy(intData, dead+1, intTemp, dead, (rowCount - dead) + 1);
          System.arraycopy(columns[col], 0, intTemp, 0, dead);
          System.arraycopy(columns[col], dead+1, intTemp, dead, (rowCount - dead) - 1);
          columns[col] = intTemp;
          break;
        }
        case LONG: {
          long[] longTemp = new long[rowCount-1];
//          long[] longData = (long[]) columns[col];
//          System.arraycopy(longData, 0, longTemp, 0, dead);
//          System.arraycopy(longData, dead+1, longTemp, dead, (rowCount - dead) + 1);
          System.arraycopy(columns[col], 0, longTemp, 0, dead);
          System.arraycopy(columns[col], dead+1, longTemp, dead, (rowCount - dead) - 1);
          columns[col] = longTemp;
          break;
        }
        case FLOAT: {
          float[] floatTemp = new float[rowCount-1];
//          float[] floatData = (float[]) columns[col];
//          System.arraycopy(floatData, 0, floatTemp, 0, dead);
//          System.arraycopy(floatData, dead+1, floatTemp, dead, (rowCount - dead) + 1);
          System.arraycopy(columns[col], 0, floatTemp, 0, dead);
          System.arraycopy(columns[col], dead+1, floatTemp, dead, (rowCount - dead) - 1);
          columns[col] = floatTemp;
          break;
        }
        case DOUBLE: {
          double[] doubleTemp = new double[rowCount-1];
//          double[] doubleData = (double[]) columns[col];
//          System.arraycopy(doubleData, 0, doubleTemp, 0, dead);
//          System.arraycopy(doubleData, dead+1, doubleTemp, dead, (rowCount - dead) + 1);
          System.arraycopy(columns[col], 0, doubleTemp, 0, dead);
          System.arraycopy(columns[col], dead+1, doubleTemp, dead, (rowCount - dead) - 1);
          columns[col] = doubleTemp;
          break;
        }
        case STRING: {
          String[] stringTemp = new String[rowCount-1];
          System.arraycopy(columns[col], 0, stringTemp, 0, dead);
          System.arraycopy(columns[col], dead+1, stringTemp, dead, (rowCount - dead) - 1);
          columns[col] = stringTemp;
        }
      }
    }
    rowCount--;
  }


  public void setRow(int row, String[] pieces) {
    checkSize(row, pieces.length - 1);
    // pieces.length may be less than columns.length, so loop over pieces
    for (int col = 0; col < pieces.length; col++) {
      setRowCol(row, col, pieces[col]);
    }
  }


  protected void setRowCol(int row, int col, String piece) {
    switch (columnTypes[col]) {
      case STRING:
        String[] stringData = (String[]) columns[col];
        stringData[row] = piece;
        break;
      case INT:
        int[] intData = (int[]) columns[col];
        intData[row] = PApplet.parseInt(piece, missingInt);
        break;
      case LONG:
        long[] longData = (long[]) columns[col];
        try {
          longData[row] = Long.parseLong(piece);
        } catch (NumberFormatException nfe) {
          longData[row] = missingLong;
        }
        break;
      case FLOAT:
        float[] floatData = (float[]) columns[col];
        floatData[row] = PApplet.parseFloat(piece, missingFloat);
        break;
      case DOUBLE:
        double[] doubleData = (double[]) columns[col];
        try {
          doubleData[row] = Double.parseDouble(piece);
        } catch (NumberFormatException nfe) {
          doubleData[row] = missingDouble;
        }
        break;
      case CATEGORICAL:
        int[] indexData = (int[]) columns[col];
        indexData[row] = columnCategories[col].index(piece);
        break;
      default:
        throw new IllegalArgumentException("That's not a valid column type.");
    }
  }


  public void convertRow(DataOutputStream output, String[] pieces) throws IOException {
    if (pieces.length > getColumnCount()) {
      throw new IllegalArgumentException("Row with too many columns: " +
                                         PApplet.join(pieces, ","));
    }
    // pieces.length may be less than columns.length, so loop over pieces
    for (int col = 0; col < pieces.length; col++) {
      switch (columnTypes[col]) {
      case STRING:
        output.writeUTF(pieces[col]);
        break;
      case INT:
        output.writeInt(PApplet.parseInt(pieces[col], missingInt));
        break;
      case LONG:
        try {
          output.writeLong(Long.parseLong(pieces[col]));
        } catch (NumberFormatException nfe) {
          output.writeLong(missingLong);
        }
        break;
      case FLOAT:
        output.writeFloat(PApplet.parseFloat(pieces[col], missingFloat));
        break;
      case DOUBLE:
        try {
          output.writeDouble(Double.parseDouble(pieces[col]));
        } catch (NumberFormatException nfe) {
          output.writeDouble(missingDouble);
        }
        break;
      case CATEGORICAL:
        output.writeInt(columnCategories[col].index(pieces[col]));
        break;
      }
    }
    for (int col = pieces.length; col < getColumnCount(); col++) {
      switch (columnTypes[col]) {
      case STRING:
        output.writeUTF("");
        break;
      case INT:
        output.writeInt(missingInt);
        break;
      case LONG:
        output.writeLong(missingLong);
        break;
      case FLOAT:
        output.writeFloat(missingFloat);
        break;
      case DOUBLE:
        output.writeDouble(missingDouble);
        break;
      case CATEGORICAL:
        output.writeInt(missingCategory);
        break;

      }
    }
  }


  protected void convertRowCol(DataOutputStream output, int row, int col, String piece) {
    switch (columnTypes[col]) {
      case STRING:
        String[] stringData = (String[]) columns[col];
        stringData[row] = piece;
        break;
      case INT:
        int[] intData = (int[]) columns[col];
        intData[row] = PApplet.parseInt(piece, missingInt);
        break;
      case LONG:
        long[] longData = (long[]) columns[col];
        try {
          longData[row] = Long.parseLong(piece);
        } catch (NumberFormatException nfe) {
          longData[row] = missingLong;
        }
        break;
      case FLOAT:
        float[] floatData = (float[]) columns[col];
        floatData[row] = PApplet.parseFloat(piece, missingFloat);
        break;
      case DOUBLE:
        double[] doubleData = (double[]) columns[col];
        try {
          doubleData[row] = Double.parseDouble(piece);
        } catch (NumberFormatException nfe) {
          doubleData[row] = missingDouble;
        }
        break;
      default:
        throw new IllegalArgumentException("That's not a valid column type.");
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  protected RowIterator rowIterator;

  /**
   * Note that this one iterator instance is shared by any calls to iterate the
   * rows of this table. This is very efficient, but not very thread-safe. If
   * you want to iterate in a multi-threaded manner, use createIterator().
   */
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


  // temporary objects inside loop! garbage collection! argh!
//  public Iterator<TableRow> iterator() {
//    return new RowIterator();
//  }


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


  static public Iterator<TableRow> createIterator(final ResultSet rs) {
    return new Iterator<TableRow>() {
      boolean already;

      public boolean hasNext() {
        already = true;
        try {
          return rs.next();
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
      }


      public TableRow next() {
        if (!already) {
          try {
            rs.next();
          } catch (SQLException e) {
            throw new RuntimeException(e);
          }
        } else {
          already = false;
        }

        return new TableRow() {
          public double getDouble(int column) {
            try {
              return rs.getDouble(column);
            } catch (SQLException e) {
              throw new RuntimeException(e);
            }
          }

          public double getDouble(String columnName) {
            try {
              return rs.getDouble(columnName);
            } catch (SQLException e) {
              throw new RuntimeException(e);
            }
          }

          public float getFloat(int column) {
            try {
              return rs.getFloat(column);
            } catch (SQLException e) {
              throw new RuntimeException(e);
            }
          }

          public float getFloat(String columnName) {
            try {
              return rs.getFloat(columnName);
            } catch (SQLException e) {
              throw new RuntimeException(e);
            }
          }

          public int getInt(int column) {
            try {
              return rs.getInt(column);
            } catch (SQLException e) {
              throw new RuntimeException(e);
            }
          }

          public int getInt(String columnName) {
            try {
              return rs.getInt(columnName);
            } catch (SQLException e) {
              throw new RuntimeException(e);
            }
          }

          public long getLong(int column) {
            try {
              return rs.getLong(column);
            } catch (SQLException e) {
              throw new RuntimeException(e);
            }
          }

          public long getLong(String columnName) {
            try {
              return rs.getLong(columnName);
            } catch (SQLException e) {
              throw new RuntimeException(e);
            }
          }

          public String getString(int column) {
            try {
              return rs.getString(column);
            } catch (SQLException e) {
              throw new RuntimeException(e);
            }
          }

          public String getString(String columnName) {
            try {
              return rs.getString(columnName);
            } catch (SQLException e) {
              throw new RuntimeException(e);
            }
          }
        };
      }

      public void remove() {
        throw new IllegalArgumentException("remove() not supported");
      }
    };
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public int getInt(int row, int column) {
    checkBounds(row, column);
    if (columnTypes[column] == INT) {
      int[] intData = (int[]) columns[column];
      return intData[row];
    }
    String str = getString(row, column);
    return (str == null || str.equals(missingString)) ?
      missingInt : PApplet.parseInt(str, missingInt);
  }


  public int getInt(int row, String columnName) {
    return getInt(row, getColumnIndex(columnName));
  }


  public void setMissingInt(int value) {
    missingInt = value;
  }


  public void setInt(int row, int column, int what) {
    if (columnTypes[column] == STRING) {
      setString(row, column, String.valueOf(what));

    } else {
      checkSize(row, column);
      if (columnTypes[column] != INT) {
        throw new IllegalArgumentException("Column " + column + " is not an int column.");
      }
      int[] intData = (int[]) columns[column];
      intData[row] = what;
    }
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


  public int[] getIntRow(int row) {
    int[] outgoing = new int[columns.length];
    for (int col = 0; col < columns.length; col++) {
      outgoing[col] = getInt(row, col);
    }
    return outgoing;
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public long getLong(int row, int column) {
    checkBounds(row, column);
    if (columnTypes[column] == LONG) {
      long[] longData = (long[]) columns[column];
      return longData[row];
    }
    String str = getString(row, column);
    if (str == null || str.equals(missingString)) {
      return missingLong;
    }
    try {
      return Long.parseLong(str);
    } catch (NumberFormatException nfe) {
      return missingLong;
    }
  }


  public long getLong(int row, String columnName) {
    return getLong(row, getColumnIndex(columnName));
  }


  public void setMissingLong(long value) {
    missingLong = value;
  }


  public void setLong(int row, int column, long what) {
    if (columnTypes[column] == STRING) {
      setString(row, column, String.valueOf(what));

    } else {
      checkSize(row, column);
      if (columnTypes[column] != LONG) {
        throw new IllegalArgumentException("Column " + column + " is not a 'long' column.");
      }
      long[] longData = (long[]) columns[column];
      longData[row] = what;
    }
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


  public long[] getLongRow(int row) {
    long[] outgoing = new long[columns.length];
    for (int col = 0; col < columns.length; col++) {
      outgoing[col] = getLong(row, col);
    }
    return outgoing;
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Get a float value from the specified row and column. If the value is null
   * or not parseable as a float, the "missing" value is returned. By default,
   * this is Float.NaN, but can be controlled with setMissingFloat().
   */
  public float getFloat(int row, int column) {
    checkBounds(row, column);
    if (columnTypes[column] == FLOAT) {
      float[] floatData = (float[]) columns[column];
      return floatData[row];
    }
    String str = getString(row, column);
    if (str == null || str.equals(missingString)) {
      return missingFloat;
    }
    return PApplet.parseFloat(str, missingFloat);
  }


  public float getFloat(int row, String columnName) {
    return getFloat(row, getColumnIndex(columnName));
  }


  public void setMissingFloat(float value) {
    missingFloat = value;
  }


  public void setFloat(int row, int column, float what) {
    if (columnTypes[column] == STRING) {
      setString(row, column, String.valueOf(what));

    } else {
      checkSize(row, column);
      if (columnTypes[column] != FLOAT) {
        throw new IllegalArgumentException("Column " + column + " is not a float column.");
      }
      float[] longData = (float[]) columns[column];
      longData[row] = what;
    }
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


  public float[] getFloatRow(int row) {
    float[] outgoing = new float[columns.length];
    for (int col = 0; col < columns.length; col++) {
      outgoing[col] = getFloat(row, col);
    }
    return outgoing;
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public double getDouble(int row, int column) {
    checkBounds(row, column);
    if (columnTypes[column] == DOUBLE) {
      double[] doubleData = (double[]) columns[column];
      return doubleData[row];
    }
    String str = getString(row, column);
    if (str == null || str.equals(missingString)) {
      return missingDouble;
    }
    try {
      return Double.parseDouble(str);
    } catch (NumberFormatException nfe) {
      return missingDouble;
    }
  }


  public double getDouble(int row, String columnName) {
    return getDouble(row, getColumnIndex(columnName));
  }


  public void setMissingDouble(double value) {
    missingDouble = value;
  }


  public void setDouble(int row, int column, double what) {
    if (columnTypes[column] == STRING) {
      setString(row, column, String.valueOf(what));

    } else {
      checkSize(row, column);
      if (columnTypes[column] != DOUBLE) {
        throw new IllegalArgumentException("Column " + column + " is not a 'double' column.");
      }
      double[] doubleData = (double[]) columns[column];
      doubleData[row] = what;
    }
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


  public double[] getDoubleRow(int row) {
    double[] outgoing = new double[columns.length];
    for (int col = 0; col < columns.length; col++) {
      outgoing[col] = getDouble(row, col);
    }
    return outgoing;
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


//public long getTimestamp(String rowName, int column) {
//return getTimestamp(getRowIndex(rowName), column);
//}


  /**
   * Returns the time in milliseconds by parsing a SQL Timestamp at this cell.
   */
//  public long getTimestamp(int row, int column) {
//    String str = get(row, column);
//    java.sql.Timestamp timestamp = java.sql.Timestamp.valueOf(str);
//    return timestamp.getTime();
//  }


//  public long getExcelTimestamp(int row, int column) {
//    return parseExcelTimestamp(get(row, column));
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


//  public void setObject(int row, int column, Object value) {
//    if (value == null) {
//      data[row][column] = null;
//    } else if (value instanceof String) {
//      set(row, column, (String) value);
//    } else if (value instanceof Float) {
//      setFloat(row, column, ((Float) value).floatValue());
//    } else if (value instanceof Integer) {
//      setInt(row, column, ((Integer) value).intValue());
//    } else {
//      set(row, column, value.toString());
//    }
//  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Get a String value from the table. If the row is longer than the table
   * @param row
   * @param col
   * @return
   */
  public String getString(int row, int col) {
    checkBounds(row, col);
    if (columnTypes[col] == STRING) {
      String[] stringData = (String[]) columns[col];
      return stringData[row];
    } else if (columnTypes[col] == CATEGORICAL) {
      int index = getInt(row, col);
      return columnCategories[col].key(index);
    } else {
      return String.valueOf(Array.get(columns[col], row));
    }
  }


  public String getString(int row, String columnName) {
    return getString(row, getColumnIndex(columnName));
  }


  public void setMissingString(String value) {
    missingString = value;
  }


  public void setString(int row, int column, String what) {
    checkSize(row, column);
    if (columnTypes[column] != STRING) {
      throw new IllegalArgumentException("Column " + column + " is not a String column.");
    }
    String[] stringData = (String[]) columns[column];
    stringData[row] = what;
  }


  public void setString(int row, String columnName, String what) {
    int column = checkColumnIndex(columnName);
    setString(row, column, what);
  }


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


  public String[] getStringRow(int row) {
    String[] outgoing = new String[columns.length];
    for (int col = 0; col < columns.length; col++) {
      outgoing[col] = getString(row, col);
    }
    return outgoing;
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Set all 'null' entries to "" (zero length String objects).
   * If columns are typed, then this will only apply to String columns.
   */
  public void makeNullEmpty() {
    for (int col = 0; col < columns.length; col++) {
      if (columnTypes[col] == STRING) {
        String[] stringData = (String[]) columns[col];
        for (int row = 0; row < rowCount; row++) {
          if (stringData[row] == null) {
            stringData[row] = "";
          }
        }
      }
    }
  }


  /**
   * Set all "" entries (zero length String objects) to null values.
   * If columns are typed, then this will only apply to String columns.
   */
  public void makeEmptyNull() {
    for (int col = 0; col < columns.length; col++) {
      if (columnTypes[col] == STRING) {
        String[] stringData = (String[]) columns[col];
        for (int row = 0; row < rowCount; row++) {
          if (stringData[row] != null && stringData[row].length() == 0) {
            stringData[row] = null;
          }
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
    float max = PConstants.MIN_FLOAT;
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
    return found ? max : missingFloat;
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


  /**
   * Return the row that contains the first String that matches.
   * @param what the String to match
   * @param column the column to search
   */
  public int findRow(String what, int column) {
    checkBounds(-1, column);
    if (columnTypes[column] == STRING) {
      String[] stringData = (String[]) columns[column];
      if (what == null) {
        for (int row = 0; row < rowCount; row++) {
          if (stringData[row] == null) return row;
        }
      } else {
        for (int row = 0; row < rowCount; row++) {
          if (stringData[row] != null && stringData[row].equals(what)) {
            return row;
          }
        }
      }
    } else {  // less efficient, includes conversion as necessary
      for (int row = 0; row < rowCount; row++) {
        String str = getString(row, column);
        if (str == null) {
          if (what == null) {
            return row;
          }
        } else if (str.equals(what)) {
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
  public int findRow(String what, String columnName) {
    return findRow(what, getColumnIndex(columnName));
  }


  /**
   * Return a list of rows that contain the String passed in. If there are no
   * matches, a zero length array will be returned (not a null array).
   * @param what the String to match
   * @param column the column to search
   */
  public int[] findRows(String what, int column) {
    int[] outgoing = new int[rowCount];
    int count = 0;

    checkBounds(-1, column);
    if (columnTypes[column] == STRING) {
      String[] stringData = (String[]) columns[column];
      if (what == null) {
        for (int row = 0; row < rowCount; row++) {
          if (stringData[row] == null) {
            outgoing[count++] = row;
          }
        }
      } else {
        for (int row = 0; row < rowCount; row++) {
          if (stringData[row] != null && stringData[row].equals(what)) {
            outgoing[count++] = row;
          }
        }
      }
    } else {  // less efficient, includes conversion as necessary
      for (int row = 0; row < rowCount; row++) {
        String str = getString(row, column);
        if (str == null) {
          if (what == null) {
            outgoing[count++] = row;
          }
        } else if (str.equals(what)) {
          outgoing[count++] = row;
        }
      }
    }
    return PApplet.subset(outgoing, 0, count);
  }


  /**
   * Return a list of rows that contain the String passed in. If there are no
   * matches, a zero length array will be returned (not a null array).
   * @param what the String to match
   * @param columnName the column to search
   */
  public int[] findRows(String what, String columnName) {
    return findRows(what, getColumnIndex(columnName));
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Return the row that contains the first String that matches.
   * @param what the String to match
   * @param column the column to search
   */
  public int matchRow(String regexp, int column) {
    checkBounds(-1, column);
    if (columnTypes[column] == STRING) {
      String[] stringData = (String[]) columns[column];
      for (int row = 0; row < rowCount; row++) {
        if (stringData[row] != null &&
            PApplet.match(stringData[row], regexp) != null) {
          return row;
        }
      }
    } else {  // less efficient, includes conversion as necessary
      for (int row = 0; row < rowCount; row++) {
        String str = getString(row, column);
        if (str != null &&
            PApplet.match(str, regexp) != null) {
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
  public int matchRow(String what, String columnName) {
    return matchRow(what, getColumnIndex(columnName));
  }


  /**
   * Return a list of rows that contain the String passed in. If there are no
   * matches, a zero length array will be returned (not a null array).
   * @param what the String to match
   * @param column the column to search
   */
  public int[] matchRows(String regexp, int column) {
    int[] outgoing = new int[rowCount];
    int count = 0;

    checkBounds(-1, column);
    if (columnTypes[column] == STRING) {
      String[] stringData = (String[]) columns[column];
      for (int row = 0; row < rowCount; row++) {
        if (stringData[row] != null &&
            PApplet.match(stringData[row], regexp) != null) {
          outgoing[count++] = row;
        }
      }
    } else {  // less efficient, includes conversion as necessary
      for (int row = 0; row < rowCount; row++) {
        String str = getString(row, column);
        if (str != null &&
            PApplet.match(str, regexp) != null) {
          outgoing[count++] = row;
        }
      }
    }
    return PApplet.subset(outgoing, 0, count);
  }


  /**
   * Return a list of rows that match the regex passed in. If there are no
   * matches, a zero length array will be returned (not a null array).
   * @param what the String to match
   * @param columnName the column to search
   */
  public int[] matchRows(String what, String columnName) {
    return matchRows(what, getColumnIndex(columnName));
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Return a list of rows that contain the String passed in. If there are no
   * matches, a zero length array will be returned (not a null array).
   * @param what the String to match
   * @param column the column to search
   */
  public void replaceAll(String regex, String replacement, int column) {
    checkBounds(-1, column);
    if (columnTypes[column] == STRING) {
      String[] stringData = (String[]) columns[column];
      for (int row = 0; row < rowCount; row++) {
        if (stringData[row] != null) {
          stringData[row] = stringData[row].replaceAll(regex, replacement);
        }
      }
    } else {
      throw new IllegalArgumentException("replaceAll() can only be used on String columns");
    }
  }


  /**
   * Run String.replaceAll() on all entries in a column.
   * Only works with columns that are already String values.
   * @param what the String to match
   * @param columnName the column to search
   */
  public void replaceAll(String regex, String replacement, String columnName) {
    replaceAll(regex, replacement, getColumnIndex(columnName));
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  protected void checkColumn(int col) {
    if (col >= columns.length) {
      setColumnCount(col + 1);
    }
  }


  protected void checkRow(int row) {
    if (row >= rowCount) {
      setRowCount(row + 1);
    }
  }


  protected void checkSize(int row, int col) {
    checkRow(row);
    checkColumn(col);
  }


  protected void checkBounds(int row, int column) {
    if (row < 0 || row >= rowCount) {
      throw new ArrayIndexOutOfBoundsException("Row " + row + " does not exist.");
    }
    if (column < 0 || column >= columns.length) {
      throw new ArrayIndexOutOfBoundsException("Column " + column + " does not exist.");
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public Table createSubset(int[] rowSubset) {
    Table newbie = new Table();
    newbie.setColumnTitles(columnTitles);  // also sets columns.length
    newbie.columnTypes = columnTypes;
    newbie.setRowCount(rowSubset.length);

    for (int i = 0; i < rowSubset.length; i++) {
      int row = rowSubset[i];
      for (int col = 0; col < columns.length; col++) {
        switch (columnTypes[col]) {
          case STRING: newbie.setString(i, col, getString(row, col)); break;
          case INT: newbie.setInt(i, col, getInt(row, col)); break;
          case LONG: newbie.setLong(i, col, getLong(row, col)); break;
          case FLOAT: newbie.setFloat(i, col, getFloat(row, col)); break;
          case DOUBLE: newbie.setDouble(i, col, getDouble(row, col)); break;
        }
      }
    }
    return newbie;
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  class HashMapBlows {
    HashMap<String,Integer> dataToIndex = new HashMap<String, Integer>();
    ArrayList<String> indexToData = new ArrayList<String>();

    int index(String key) {
      Integer value = dataToIndex.get(key);
      if (value != null) {
        return value;
      }

      int v = dataToIndex.size();
      dataToIndex.put(key, v);
      indexToData.add(key);
      return v;
    }

    String key(int index) {
      return indexToData.get(index);
    }

    int size() {
      return dataToIndex.size();
    }

    void write(DataOutputStream output) throws IOException {
      output.writeInt(size());
      for (String str : indexToData) {
        output.writeUTF(str);
      }
    }

    void writeln(PrintWriter writer) throws IOException {
      for (String str : indexToData) {
        writer.println(str);
      }
      writer.flush();
      writer.close();
    }

    void read(DataInputStream input) throws IOException {
      int count = input.readInt();
      dataToIndex = new HashMap<String, Integer>(count);
      for (int i = 0; i < count; i++) {
        String str = input.readUTF();
        dataToIndex.put(str, i);
        indexToData.add(str);
      }
    }
  }

//  class HashMapBlows extends HashMap<String,Integer> {
//
//    int index(String what) {
//      Integer value = get(what);
//      if (value != null) {
//        return value;
//      }
//
//      int v = size();
//      put(what, v);
//      return v;
//    }
//  }


  class HashMapSucks extends HashMap<String,Integer> {

    void increment(String what) {
      Integer value = get(what);
      if (value == null) {
        put(what, 1);
      } else {
        put(what, value + 1);
      }
    }

    void check(String what) {
      if (get(what) == null) {
        put(what, 0);
      }
    }
  }


  public String[] getUnique(String column) {
    return getUnique(getColumnIndex(column));
  }


  public String[] getUnique(int column) {
    HashMapSucks found = new HashMapSucks();
    for (int row = 0; row < getRowCount(); row++) {
      found.check(getString(row, column));
    }
    String[] outgoing = new String[found.size()];
    found.keySet().toArray(outgoing);
    return outgoing;
  }


  public HashMap<String,Integer> getUniqueCount(String columnName) {
    return getUniqueCount(getColumnIndex(columnName));
  }


  public HashMap<String,Integer> getUniqueCount(int column) {
    HashMapSucks outgoing = new HashMapSucks();
    for (int row = 0; row < rowCount; row++) {
      String entry = getString(row, column);
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
  public HashMap<String,Integer> getRowLookup(int col) {
    HashMap<String,Integer> outgoing = new HashMap<String, Integer>();
    for (int row = 0; row < getRowCount(); row++) {
      outgoing.put(getString(row, col), row);
    }
    return outgoing;
  }


  // incomplete, basically this is silly to write all this repetitive code when
  // it can be implemented in ~3 lines of code...
//  /**
//   * Return an object that maps the data from one column to the data of found
//   * in another column.
//   */
//  public HashMap<?,?> getLookup(int col1, int col2) {
//    HashMap outgoing = null;
//
//    switch (columnTypes[col1]) {
//      case INT: {
//        if (columnTypes[col2] == INT) {
//          outgoing = new HashMap<Integer, Integer>();
//          for (int row = 0; row < getRowCount(); row++) {
//            outgoing.put(getInt(row, col1), getInt(row, col2));
//          }
//        } else if (columnTypes[col2] == LONG) {
//          outgoing = new HashMap<Integer, Long>();
//          for (int row = 0; row < getRowCount(); row++) {
//            outgoing.put(getInt(row, col1), getLong(row, col2));
//          }
//        } else if (columnTypes[col2] == FLOAT) {
//          outgoing = new HashMap<Integer, Float>();
//          for (int row = 0; row < getRowCount(); row++) {
//            outgoing.put(getInt(row, col1), getFloat(row, col2));
//          }
//        } else if (columnTypes[col2] == DOUBLE) {
//          outgoing = new HashMap<Integer, Double>();
//          for (int row = 0; row < getRowCount(); row++) {
//            outgoing.put(getInt(row, col1), getDouble(row, col2));
//          }
//        } else if (columnTypes[col2] == STRING) {
//          outgoing = new HashMap<Integer, String>();
//          for (int row = 0; row < getRowCount(); row++) {
//            outgoing.put(getInt(row, col1), get(row, col2));
//          }
//        }
//        break;
//      }
//    }
//    return outgoing;
//  }


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
    for (int col = 0; col < columns.length; col++) {
      String[] stringData = (String[]) columns[col];
      for (int row = 0; row < rowCount; row++) {
        if (stringData[row] != null) {
          stringData[row] = PApplet.trim(stringData[row]);
        }
      }
    }
  }
}

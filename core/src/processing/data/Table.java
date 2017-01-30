/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2011-13 Ben Fry and Casey Reas
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
import java.lang.reflect.*;
import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import processing.core.PApplet;
import processing.core.PConstants;


/**
 * <p>Generic class for handling tabular data, typically from a CSV, TSV, or
 * other sort of spreadsheet file.</p>
 * <p>CSV files are
 * <a href="http://en.wikipedia.org/wiki/Comma-separated_values">comma separated values</a>,
 * often with the data in quotes. TSV files use tabs as separators, and usually
 * don't bother with the quotes.</p>
 * <p>File names should end with .csv if they're comma separated.</p>
 * <p>A rough "spec" for CSV can be found <a href="http://tools.ietf.org/html/rfc4180">here</a>.</p>
 *
 * @webref data:composite
 * @see PApplet#loadTable(String)
 * @see PApplet#saveTable(Table, String)
 * @see TableRow
 */
public class Table {
  protected int rowCount;
  protected int allocCount;

//  protected boolean skipEmptyRows = true;
//  protected boolean skipCommentLines = true;
//  protected String extension = null;
//  protected boolean commaSeparatedValues = false;
//  protected boolean awfulCSV = false;

  protected String missingString = null;
  protected int missingInt = 0;
  protected long missingLong = 0;
  protected float missingFloat = Float.NaN;
  protected double missingDouble = Double.NaN;
  protected int missingCategory = -1;

  String[] columnTitles;
  HashMapBlows[] columnCategories;
  HashMap<String, Integer> columnIndices;

  protected Object[] columns;  // [column]

  // accessible for advanced users
  static public final int STRING = 0;
  static public final int INT = 1;
  static public final int LONG = 2;
  static public final int FLOAT = 3;
  static public final int DOUBLE = 4;
  static public final int CATEGORY = 5;
  int[] columnTypes;

  protected RowIterator rowIterator;

  // 0 for doubling each time, otherwise the number of rows to increment on
  // each expansion.
  protected int expandIncrement;


  /**
   * Creates a new, empty table. Use addRow() to add additional rows.
   */
  public Table() {
    init();
  }

  /**
   * @nowebref
   */
  public Table(File file) throws IOException {
    this(file, null);
  }


  /**
   * version that uses a File object; future releases (or data types)
   * may include additional optimizations here
   *
   * @nowebref
   */
  public Table(File file, String options) throws IOException {
    // uses createInput() to handle .gz (and eventually .bz2) files
    init();
    parse(PApplet.createInput(file),
          extensionOptions(true, file.getName(), options));
  }

  /**
   * @nowebref
   */
  public Table(InputStream input) throws IOException {
    this(input, null);
  }


  /**
   * Read the table from a stream. Possible options include:
   * <ul>
   * <li>csv - parse the table as comma-separated values
   * <li>tsv - parse the table as tab-separated values
   * <li>newlines - this CSV file contains newlines inside individual cells
   * <li>header - this table has a header (title) row
   * </ul>
   *
   * @nowebref
   * @param input
   * @param options
   * @throws IOException
   */
  public Table(InputStream input, String options) throws IOException {
    init();
    parse(input, options);
  }


  public Table(Iterable<TableRow> rows) {
    init();

    int row = 0;
    int alloc = 10;

    for (TableRow incoming : rows) {
      if (row == 0) {
        setColumnTypes(incoming.getColumnTypes());
        setColumnTitles(incoming.getColumnTitles());
        // Do this after setting types, otherwise it'll attempt to parse the
        // allocated but empty rows, and drive CATEGORY columns nutso.
        setRowCount(alloc);
        // sometimes more columns than titles (and types?)
        setColumnCount(incoming.getColumnCount());

      } else if (row == alloc) {
        // Far more efficient than re-allocating all columns and doing a copy
        alloc *= 2;
        setRowCount(alloc);
      }

      //addRow(row);
//      try {
      setRow(row++, incoming);
//      } catch (ArrayIndexOutOfBoundsException aioobe) {
//        for (int i = 0; i < incoming.getColumnCount(); i++) {
//          System.out.format("[%d] %s%n", i, incoming.getString(i));
//        }
//        throw aioobe;
//      }
    }
    // Shrink the table to only the rows that were used
    if (row != alloc) {
      setRowCount(row);
    }
  }


  /**
   * @nowebref
   */
  public Table(ResultSet rs) {
    init();
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


  public Table typedParse(InputStream input, String options) throws IOException {
    Table table = new Table();
    table.setColumnTypes(this);
    table.parse(input, options);
    return table;
  }


  protected void init() {
    columns = new Object[0];
    columnTypes = new int[0];
    columnCategories = new HashMapBlows[0];
  }


  /*
  protected String checkOptions(File file, String options) throws IOException {
    String extension = null;
    String filename = file.getName();
    int dotIndex = filename.lastIndexOf('.');
    if (dotIndex != -1) {
      extension = filename.substring(dotIndex + 1).toLowerCase();
      if (!extension.equals("csv") &&
          !extension.equals("tsv") &&
          !extension.equals("html") &&
          !extension.equals("bin")) {
        // ignore extension
        extension = null;
      }
    }
    if (extension == null) {
      if (options == null) {
        throw new IOException("This table filename has no extension, and no options are set.");
      }
    } else {  // extension is not null
      if (options == null) {
        options = extension;
      } else {
        // prepend the extension, it will be overridden if there's an option for it.
        options = extension + "," + options;
      }
    }
    return options;
  }
  */


  static final String[] loadExtensions = { "csv", "tsv", "ods", "bin" };
  static final String[] saveExtensions = { "csv", "tsv", "ods", "bin", "html" };

  static public String extensionOptions(boolean loading, String filename, String options) {
    String extension = PApplet.checkExtension(filename);
    if (extension != null) {
      for (String possible : loading ? loadExtensions : saveExtensions) {
        if (extension.equals(possible)) {
          if (options == null) {
            return extension;
          } else {
            // prepend the extension to the options (will be replaced by other
            // options that override it later in the load loop)
            return extension + "," + options;
          }
        }
      }
    }
    return options;
  }


  protected void parse(InputStream input, String options) throws IOException {
//    boolean awfulCSV = false;
    boolean header = false;
    String extension = null;
    boolean binary = false;
    String encoding = "UTF-8";

    String worksheet = null;
    final String sheetParam = "worksheet=";

    String[] opts = null;
    if (options != null) {
      opts = PApplet.trim(PApplet.split(options, ','));
      for (String opt : opts) {
        if (opt.equals("tsv")) {
          extension = "tsv";
        } else if (opt.equals("csv")) {
          extension = "csv";
        } else if (opt.equals("ods")) {
          extension = "ods";
        } else if (opt.equals("newlines")) {
          //awfulCSV = true;
          //extension = "csv";
          throw new IllegalArgumentException("The 'newlines' option is no longer necessary.");
        } else if (opt.equals("bin")) {
          binary = true;
          extension = "bin";
        } else if (opt.equals("header")) {
          header = true;
        } else if (opt.startsWith(sheetParam)) {
          worksheet = opt.substring(sheetParam.length());
        } else if (opt.startsWith("dictionary=")) {
          // ignore option, this is only handled by PApplet
        } else if (opt.startsWith("encoding=")) {
          encoding = opt.substring(9);
        } else {
          throw new IllegalArgumentException("'" + opt + "' is not a valid option for loading a Table");
        }
      }
    }

    if (extension == null) {
      throw new IllegalArgumentException("No extension specified for this Table");
    }

    if (binary) {
      loadBinary(input);

    } else if (extension.equals("ods")) {
      odsParse(input, worksheet, header);

    } else {
      InputStreamReader isr = new InputStreamReader(input, encoding);
      BufferedReader reader = new BufferedReader(isr);
      /*
       if (awfulCSV) {
        parseAwfulCSV(reader, header);
      } else if ("tsv".equals(extension)) {
        parseBasic(reader, header, true);
      } else if ("csv".equals(extension)) {
        parseBasic(reader, header, false);
      }
      */
      parseBasic(reader, header, "tsv".equals(extension));
    }
  }


  protected void parseBasic(BufferedReader reader,
                            boolean header, boolean tsv) throws IOException {
    String line = null;
    int row = 0;
    if (rowCount == 0) {
      setRowCount(10);
    }
    //int prev = 0;  //-1;
    try {
      while ((line = reader.readLine()) != null) {
        if (row == getRowCount()) {
          setRowCount(row << 1);
        }
        if (row == 0 && header) {
          setColumnTitles(tsv ? PApplet.split(line, '\t') : splitLineCSV(line, reader));
          header = false;
        } else {
          setRow(row, tsv ? PApplet.split(line, '\t') : splitLineCSV(line, reader));
          row++;
        }

        if (row % 10000 == 0) {
        /*
        // this is problematic unless we're going to calculate rowCount first
        if (row < rowCount) {
          int pct = (100 * row) / rowCount;
          if (pct != prev) {  // also prevents "0%" from showing up
            System.out.println(pct + "%");
            prev = pct;
          }
        }
         */
          try {
            // Sleep this thread so that the GC can catch up
            Thread.sleep(10);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Error reading table on line " + row, e);
    }
    // shorten or lengthen based on what's left
    if (row != getRowCount()) {
      setRowCount(row);
    }
  }


//  public void convertTSV(BufferedReader reader, File outputFile) throws IOException {
//    convertBasic(reader, true, outputFile);
//  }


  /*
  protected void parseAwfulCSV(BufferedReader reader,
                               boolean header) throws IOException {
    char[] c = new char[100];
    int count = 0;
    boolean insideQuote = false;

    int alloc = 100;
    setRowCount(100);

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

        } else if (ch == '\r' || ch == '\n') {
          if (ch == '\r') {
            // check to see if next is a '\n'
            reader.mark(1);
            if (reader.read() != '\n') {
              reader.reset();
            }
          }
          setString(row, col, new String(c, 0, count));
          count = 0;
          row++;
          if (row == 1 && header) {
            // Use internal row removal (efficient because only one row).
            removeTitleRow();
            // Un-set the header variable so that next time around, we don't
            // just get stuck into a loop, removing the 0th row repeatedly.
            header = false;
            // Reset the number of rows (removeTitleRow() won't reset our local 'row' counter)
            row = 0;
          }
//          if (row % 1000 == 0) {
//            PApplet.println(PApplet.nfc(row));
//          }
          if (row == alloc) {
            alloc *= 2;
            setRowCount(alloc);
          }
          col = 0;

        } else if (ch == ',') {
          setString(row, col, new String(c, 0, count));
          count = 0;
          // starting a new column, make sure we have room
          col++;
          ensureColumn(col);

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
    row++;  // set row to row count (the current row index + 1)
    if (alloc != row) {
      setRowCount(row);  // shrink to the actual size
    }
  }
  */


  static class CommaSeparatedLine {
    char[] c;
    String[] pieces;
    int pieceCount;

//    int offset;
    int start; //, stop;

    String[] handle(String line, BufferedReader reader) throws IOException {
//      PApplet.println("handle() called for: " + line);
      start = 0;
      pieceCount = 0;
      c = line.toCharArray();

      // get tally of number of columns and allocate the array
      int cols = 1;  // the first comma indicates the second column
      boolean quote = false;
      for (int i = 0; i < c.length; i++) {
        if (!quote && (c[i] == ',')) {
          cols++;
        } else if (c[i] == '\"') {
          // double double quotes (escaped quotes like "") will simply toggle
          // this back and forth, so it should remain accurate
          quote = !quote;
        }
      }
      pieces = new String[cols];

//      while (offset < c.length) {
//        start = offset;
      while (start < c.length) {
        boolean enough = ingest();
        while (!enough) {
          // found a newline inside the quote, grab another line
          String nextLine = reader.readLine();
//          System.out.println("extending to " + nextLine);
          if (nextLine == null) {
//            System.err.println(line);
            throw new IOException("Found a quoted line that wasn't terminated properly.");
          }
          // for simplicity, not bothering to skip what's already been read
          // from c (and reset the offset to 0), opting to make a bigger array
          // with both lines.
          char[] temp = new char[c.length + 1 + nextLine.length()];
          PApplet.arrayCopy(c, temp, c.length);
          // NOTE: we're converting to \n here, which isn't perfect
          temp[c.length] = '\n';
          nextLine.getChars(0, nextLine.length(), temp, c.length + 1);
//          c = temp;
          return handle(new String(temp), reader);
          //System.out.println("  full line is now " + new String(c));
          //stop = nextComma(c, offset);
          //System.out.println("stop is now " + stop);
          //enough = ingest();
        }
      }

      // Make any remaining entries blanks instead of nulls. Empty columns from
      // CSV are always "" not null, so this handles successive commas in a line
      for (int i = pieceCount; i < pieces.length; i++) {
        pieces[i] = "";
      }
//      PApplet.printArray(pieces);
      return pieces;
    }

    protected void addPiece(int start, int stop, boolean quotes) {
      if (quotes) {
        int dest = start;
        for (int i = start; i < stop; i++) {
          if (c[i] == '\"') {
            ++i;  // step over the quote
          }
          if (i != dest) {
            c[dest] = c[i];
          }
          dest++;
        }
        pieces[pieceCount++] = new String(c, start, dest - start);

      } else {
        pieces[pieceCount++] = new String(c, start, stop - start);
      }
    }

    /**
     * Returns the next comma (not inside a quote) in the specified array.
     * @param c array to search
     * @param index offset at which to start looking
     * @return index of the comma, or -1 if line ended inside an unclosed quote
     */
    protected boolean ingest() {
      boolean hasEscapedQuotes = false;
      // not possible
//      if (index == c.length) {  // we're already at the end
//        return c.length;
//      }
      boolean quoted = c[start] == '\"';
      if (quoted) {
        start++; // step over the quote
      }
      int i = start;
      while (i < c.length) {
//        PApplet.println(c[i] + " i=" + i);
        if (c[i] == '\"') {
          // if this fella started with a quote
          if (quoted) {
            if (i == c.length-1) {
              // closing quote for field; last field on the line
              addPiece(start, i, hasEscapedQuotes);
              start = c.length;
              return true;

            } else if (c[i+1] == '\"') {
              // an escaped quote inside a quoted field, step over it
              hasEscapedQuotes = true;
              i += 2;

            } else if (c[i+1] == ',') {
              // that was our closing quote, get outta here
              addPiece(start, i, hasEscapedQuotes);
              start = i+2;
              return true;

            } else {
              // This is a lone-wolf quote, occasionally seen in exports.
              // It's a single quote in the middle of some other text,
              // and not escaped properly. Pray for the best!
              i++;
            }

          } else {  // not a quoted line
            if (i == c.length-1) {
              // we're at the end of the line, can't have an unescaped quote
              throw new RuntimeException("Unterminated quote at end of line");

            } else if (c[i+1] == '\"') {
              // step over this crummy quote escape
              hasEscapedQuotes = true;
              i += 2;

            } else {
              throw new RuntimeException("Unterminated quoted field mid-line");
            }
          }
        } else if (!quoted && c[i] == ',') {
          addPiece(start, i, hasEscapedQuotes);
          start = i+1;
          return true;

        } else if (!quoted && i == c.length-1) {
          addPiece(start, c.length, hasEscapedQuotes);
          start = c.length;
          return true;

        } else {  // nothing all that interesting
          i++;
        }
      }
//      if (!quote && (c[i] == ',')) {
//        // found a comma, return this location
//        return i;
//      } else if (c[i] == '\"') {
//        // if it's a quote, then either the next char is another quote,
//        // or if this is a quoted entry, it better be a comma
//        quote = !quote;
//      }
//    }

      // if still inside a quote, indicate that another line should be read
      if (quoted) {
        return false;
      }

//    // made it to the end of the array with no new comma
//    return c.length;

      throw new RuntimeException("not sure how...");
    }
  }


  CommaSeparatedLine csl;

  /**
   * Parse a line of text as comma-separated values, returning each value as
   * one entry in an array of String objects. Remove quotes from entries that
   * begin and end with them, and convert 'escaped' quotes to actual quotes.
   * @param line line of text to be parsed
   * @return an array of the individual values formerly separated by commas
   */
  protected String[] splitLineCSV(String line, BufferedReader reader) throws IOException {
    if (csl == null) {
      csl = new CommaSeparatedLine();
    }
    return csl.handle(line, reader);
  }


  /**
   * Returns the next comma (not inside a quote) in the specified array.
   * @param c array to search
   * @param index offset at which to start looking
   * @return index of the comma, or -1 if line ended inside an unclosed quote
   */
  /*
  static protected int nextComma(char[] c, int index) {
    if (index == c.length) {  // we're already at the end
      return c.length;
    }
    boolean quoted = c[index] == '\"';
    if (quoted) {
      index++; // step over the quote
    }
    for (int i = index; i < c.length; i++) {
      if (c[i] == '\"') {
        // if this fella started with a quote
        if (quoted) {
          if (i == c.length-1) {
            //return -1;  // ran out of chars
            // closing quote for field; last field on the line
            return c.length;
          } else if (c[i+1] == '\"') {
            // an escaped quote inside a quoted field, step over it
            i++;
          } else if (c[i+1] == ',') {
            // that's our closing quote, get outta here
            return i+1;
          }

        } else {  // not a quoted line
          if (i == c.length-1) {
            // we're at the end of the line, can't have an unescaped quote
            //return -1;  // ran out of chars
            throw new RuntimeException("Unterminated quoted field at end of line");
          } else if (c[i+1] == '\"') {
            // step over this crummy quote escape
            ++i;
          } else {
            throw new RuntimeException("Unterminated quoted field mid-line");
          }
        }
      } else if (!quoted && c[i] == ',') {
        return i;
      }
      if (!quote && (c[i] == ',')) {
        // found a comma, return this location
        return i;
      } else if (c[i] == '\"') {
        // if it's a quote, then either the next char is another quote,
        // or if this is a quoted entry, it better be a comma
        quote = !quote;
      }
    }
    // if still inside a quote, indicate that another line should be read
    if (quote) {
      return -1;
    }
    // made it to the end of the array with no new comma
    return c.length;
  }
  */


  /**
   * Read a .ods (OpenDoc spreadsheet) zip file from an InputStream, and
   * return the InputStream for content.xml contained inside.
   */
  private InputStream odsFindContentXML(InputStream input) {
    ZipInputStream zis = new ZipInputStream(input);
    ZipEntry entry = null;
    try {
      while ((entry = zis.getNextEntry()) != null) {
        if (entry.getName().equals("content.xml")) {
          return zis;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }


  protected void odsParse(InputStream input, String worksheet, boolean header) {
    try {
      InputStream contentStream = odsFindContentXML(input);
      XML xml = new XML(contentStream);

      // table files will have multiple sheets..
      // <table:table table:name="Sheet1" table:style-name="ta1" table:print="false">
      // <table:table table:name="Sheet2" table:style-name="ta1" table:print="false">
      // <table:table table:name="Sheet3" table:style-name="ta1" table:print="false">
      XML[] sheets =
        xml.getChildren("office:body/office:spreadsheet/table:table");

      boolean found = false;
      for (XML sheet : sheets) {
//        System.out.println(sheet.getAttribute("table:name"));
        if (worksheet == null || worksheet.equals(sheet.getString("table:name"))) {
          odsParseSheet(sheet, header);
          found = true;
          if (worksheet == null) {
            break;  // only read the first sheet
          }
        }
      }
      if (!found) {
        if (worksheet == null) {
          throw new RuntimeException("No worksheets found in the ODS file.");
        } else {
          throw new RuntimeException("No worksheet named " + worksheet +
                                     " found in the ODS file.");
        }
      }
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    } catch (SAXException e) {
      e.printStackTrace();
    }
  }


  /**
   * Parses a single sheet of XML from this file.
   * @param The XML object for a single worksheet from the ODS file
   */
  private void odsParseSheet(XML sheet, boolean header) {
    // Extra <p> or <a> tags inside the text tag for the cell will be stripped.
    // Different from showing formulas, and not quite the same as 'save as
    // displayed' option when saving from inside OpenOffice. Only time we
    // wouldn't want this would be so that we could parse hyperlinks and
    // styling information intact, but that's out of scope for the p5 version.
    final boolean ignoreTags = true;

    XML[] rows = sheet.getChildren("table:table-row");
    //xml.getChildren("office:body/office:spreadsheet/table:table/table:table-row");

    int rowIndex = 0;
    for (XML row : rows) {
      int rowRepeat = row.getInt("table:number-rows-repeated", 1);
//      if (rowRepeat != 1) {
//          System.out.println(rowRepeat + " " + rowCount + " " + (rowCount + rowRepeat));
//      }
      boolean rowNotNull = false;
      XML[] cells = row.getChildren();
      int columnIndex = 0;

      for (XML cell : cells) {
        int cellRepeat = cell.getInt("table:number-columns-repeated", 1);

//        <table:table-cell table:formula="of:=SUM([.E7:.E8])" office:value-type="float" office:value="4150">
//        <text:p>4150.00</text:p>
//        </table:table-cell>

        String cellData = ignoreTags ? cell.getString("office:value") : null;

        // if there's an office:value in the cell, just roll with that
        if (cellData == null) {
          int cellKids = cell.getChildCount();
          if (cellKids != 0) {
            XML[] paragraphElements = cell.getChildren("text:p");
            if (paragraphElements.length != 1) {
              for (XML el : paragraphElements) {
                System.err.println(el.toString());
              }
              throw new RuntimeException("found more than one text:p element");
            }
            XML textp = paragraphElements[0];
            String textpContent = textp.getContent();
            // if there are sub-elements, the content shows up as a child element
            // (for which getName() returns null.. which seems wrong)
            if (textpContent != null) {
              cellData = textpContent;  // nothing fancy, the text is in the text:p element
            } else {
              XML[] textpKids = textp.getChildren();
              StringBuilder cellBuffer = new StringBuilder();
              for (XML kid : textpKids) {
                String kidName = kid.getName();
                if (kidName == null) {
                  odsAppendNotNull(kid, cellBuffer);

                } else if (kidName.equals("text:s")) {
                  int spaceCount = kid.getInt("text:c", 1);
                  for (int space = 0; space < spaceCount; space++) {
                    cellBuffer.append(' ');
                  }
                } else if (kidName.equals("text:span")) {
                  odsAppendNotNull(kid, cellBuffer);

                } else if (kidName.equals("text:a")) {
                  // <text:a xlink:href="http://blah.com/">blah.com</text:a>
                  if (ignoreTags) {
                    cellBuffer.append(kid.getString("xlink:href"));
                  } else {
                    odsAppendNotNull(kid, cellBuffer);
                  }

                } else {
                  odsAppendNotNull(kid, cellBuffer);
                  System.err.println(getClass().getName() + ": don't understand: " + kid);
                  //throw new RuntimeException("I'm not used to this.");
                }
              }
              cellData = cellBuffer.toString();
            }
            //setString(rowIndex, columnIndex, c); //text[0].getContent());
            //columnIndex++;
          }
        }
        for (int r = 0; r < cellRepeat; r++) {
          if (cellData != null) {
            //System.out.println("setting " + rowIndex + "," + columnIndex + " to " + cellData);
            setString(rowIndex, columnIndex, cellData);
          }
          columnIndex++;
          if (cellData != null) {
//            if (columnIndex > columnMax) {
//              columnMax = columnIndex;
//            }
            rowNotNull = true;
          }
        }
      }
      if (header) {
        removeTitleRow();  // efficient enough on the first row
        header = false;  // avoid infinite loop

      } else {
        if (rowNotNull && rowRepeat > 1) {
          String[] rowStrings = getStringRow(rowIndex);
          for (int r = 1; r < rowRepeat; r++) {
            addRow(rowStrings);
          }
        }
        rowIndex += rowRepeat;
      }
    }
  }


  private void odsAppendNotNull(XML kid, StringBuilder buffer) {
    String content = kid.getContent();
    if (content != null) {
      buffer.append(content);
    }
  }


  // A 'Class' object is used here, so the syntax for this function is:
  // Table t = loadTable("cars3.tsv", "header");
  // Record[] records = (Record[]) t.parse(Record.class);
  // While t.parse("Record") might be nicer, the class is likely to be an
  // inner class (another tab in a PDE sketch) or even inside a package,
  // so additional information would be needed to locate it. The name of the
  // inner class would be "SketchName$Record" which isn't acceptable syntax
  // to make people use. Better to just introduce the '.class' syntax.

  // Unlike the Table class itself, this accepts char and boolean fields in
  // the target class, since they're much more prevalent, and don't require
  // a zillion extra methods and special cases in the rest of the class here.

  // since this is likely an inner class, needs a reference to its parent,
  // because that's passed to the constructor parameter (inserted by the
  // compiler) of an inner class by the runtime.

  /** incomplete, do not use */
  public void parseInto(Object enclosingObject, String fieldName) {
    Class<?> target = null;
    Object outgoing = null;
    Field targetField = null;
    try {
      // Object targetObject,
      // Class target -> get this from the type of fieldName
//      Class sketchClass = sketch.getClass();
      Class<?> sketchClass = enclosingObject.getClass();
      targetField = sketchClass.getDeclaredField(fieldName);
//      PApplet.println("found " + targetField);
      Class<?> targetArray = targetField.getType();
      if (!targetArray.isArray()) {
        // fieldName is not an array
      } else {
        target = targetArray.getComponentType();
        outgoing = Array.newInstance(target, getRowCount());
      }
    } catch (NoSuchFieldException e) {
      e.printStackTrace();
    } catch (SecurityException e) {
      e.printStackTrace();
    }

//    Object enclosingObject = sketch;
//    PApplet.println("enclosing obj is " + enclosingObject);
    Class<?> enclosingClass = target.getEnclosingClass();
    Constructor<?> con = null;

    try {
      if (enclosingClass == null) {
        con = target.getDeclaredConstructor();  //new Class[] { });
//        PApplet.println("no enclosing class");
      } else {
        con = target.getDeclaredConstructor(new Class[] { enclosingClass });
//        PApplet.println("enclosed by " + enclosingClass.getName());
      }
      if (!con.isAccessible()) {
//        System.out.println("setting constructor to public");
        con.setAccessible(true);
      }
    } catch (SecurityException e) {
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    }

    Field[] fields = target.getDeclaredFields();
    ArrayList<Field> inuse = new ArrayList<Field>();
    for (Field field : fields) {
      String name = field.getName();
      if (getColumnIndex(name, false) != -1) {
//        System.out.println("found field " + name);
        if (!field.isAccessible()) {
//          PApplet.println("  changing field access");
          field.setAccessible(true);
        }
        inuse.add(field);
      } else {
//        System.out.println("skipping field " + name);
      }
    }

    int index = 0;
    try {
      for (TableRow row : rows()) {
        Object item = null;
        if (enclosingClass == null) {
          //item = target.newInstance();
          item = con.newInstance();
        } else {
          item = con.newInstance(new Object[] { enclosingObject });
        }
        //Object item = defaultCons.newInstance(new Object[] { });
        for (Field field : inuse) {
          String name = field.getName();
          //PApplet.println("gonna set field " + name);

          if (field.getType() == String.class) {
            field.set(item, row.getString(name));

          } else if (field.getType() == Integer.TYPE) {
            field.setInt(item, row.getInt(name));

          } else if (field.getType() == Long.TYPE) {
            field.setLong(item, row.getLong(name));

          } else if (field.getType() == Float.TYPE) {
            field.setFloat(item, row.getFloat(name));

          } else if (field.getType() == Double.TYPE) {
            field.setDouble(item, row.getDouble(name));

          } else if (field.getType() == Boolean.TYPE) {
            String content = row.getString(name);
            if (content != null) {
              // Only bother setting if it's true,
              // otherwise false by default anyway.
              if (content.toLowerCase().equals("true") ||
                  content.equals("1")) {
                field.setBoolean(item, true);
              }
            }
//            if (content == null) {
//              field.setBoolean(item, false);  // necessary?
//            } else if (content.toLowerCase().equals("true")) {
//              field.setBoolean(item, true);
//            } else if (content.equals("1")) {
//              field.setBoolean(item, true);
//            } else {
//              field.setBoolean(item, false);  // necessary?
//            }
          } else if (field.getType() == Character.TYPE) {
            String content = row.getString(name);
            if (content != null && content.length() > 0) {
              // Otherwise set to \0 anyway
              field.setChar(item, content.charAt(0));
            }
          }
        }
//        list.add(item);
        Array.set(outgoing, index++, item);
      }
      if (!targetField.isAccessible()) {
//        PApplet.println("setting target field to public");
        targetField.setAccessible(true);
      }
      // Set the array in the sketch
//      targetField.set(sketch, outgoing);
      targetField.set(enclosingObject, outgoing);

    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }
  }


  public boolean save(File file, String options) throws IOException {
    return save(PApplet.createOutput(file),
                Table.extensionOptions(false, file.getName(), options));
  }


  public boolean save(OutputStream output, String options) {
    PrintWriter writer = PApplet.createWriter(output);
    String extension = null;
    if (options == null) {
      throw new IllegalArgumentException("No extension specified for saving this Table");
    }

    String[] opts = PApplet.trim(PApplet.split(options, ','));
    // Only option for save is the extension, so we can safely grab the last
    extension = opts[opts.length - 1];
    boolean found = false;
    for (String ext : saveExtensions) {
      if (extension.equals(ext)) {
        found = true;
        break;
      }
    }
    // Not providing a fallback; let's make users specify an extension
    if (!found) {
      throw new IllegalArgumentException("'" + extension + "' not available for Table");
    }

    if (extension.equals("csv")) {
      writeCSV(writer);
    } else if (extension.equals("tsv")) {
      writeTSV(writer);
    } else if (extension.equals("ods")) {
      try {
        saveODS(output);
      } catch (IOException e) {
        e.printStackTrace();
        return false;
      }
    } else if (extension.equals("html")) {
      writeHTML(writer);
    } else if (extension.equals("bin")) {
      try {
        saveBinary(output);
      } catch (IOException e) {
        e.printStackTrace();
        return false;
      }
    }
    writer.flush();
    writer.close();
    return true;
  }


  protected void writeTSV(PrintWriter writer) {
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


  protected void writeCSV(PrintWriter writer) {
    if (columnTitles != null) {
      for (int col = 0; col < getColumnCount(); col++) {
        if (col != 0) {
          writer.print(',');
        }
        try {
          if (columnTitles[col] != null) {  // col < columnTitles.length &&
            writeEntryCSV(writer, columnTitles[col]);
          }
        } catch (ArrayIndexOutOfBoundsException e) {
          PApplet.printArray(columnTitles);
          PApplet.printArray(columns);
          throw e;
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


  protected void writeHTML(PrintWriter writer) {
    writer.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 3.2//EN\">");
//    writer.println("<!DOCTYPE html>");
//    writer.println("<meta charset=\"utf-8\">");

    writer.println("<html>");
    writer.println("<head>");
    writer.println("  <meta http-equiv=\"content-type\" content=\"text/html;charset=utf-8\" />");
    writer.println("</head>");

    writer.println("<body>");
    writer.println("  <table>");

    if (hasColumnTitles()) {
      writer.println("  <tr>");
      for (String entry : getColumnTitles()) {
        writer.print("      <th>");
        if (entry != null) {
          writeEntryHTML(writer, entry);
        }
        writer.println("</th>");
      }
      writer.println("  </tr>");
    }

    for (int row = 0; row < getRowCount(); row++) {
      writer.println("    <tr>");
      for (int col = 0; col < getColumnCount(); col++) {
        String entry = getString(row, col);
        writer.print("      <td>");
        if (entry != null) {
          // probably not a great idea to mess w/ the export
//          if (entry.startsWith("<") && entry.endsWith(">")) {
//            writer.print(entry);
//          } else {
          writeEntryHTML(writer, entry);
//          }
        }
        writer.println("</td>");
      }
      writer.println("    </tr>");
    }
    writer.println("  </table>");
    writer.println("</body>");

    writer.println("</html>");
    writer.flush();
  }


  protected void writeEntryHTML(PrintWriter writer, String entry) {
    //char[] chars = entry.toCharArray();
    for (char c : entry.toCharArray()) {  //chars) {
      if (c == '<') {
        writer.print("&lt;");
      } else if (c == '>') {
        writer.print("&gt;");
      } else if (c == '&') {
        writer.print("&amp;");
//      } else if (c == '\'') {  // only in XML
//        writer.print("&apos;");
      } else if (c == '"') {
        writer.print("&quot;");

      } else if (c < 32 || c > 127) {  // keep in ASCII or Tidy complains
        writer.print("&#");
        writer.print((int) c);
        writer.print(';');

      } else {
        writer.print(c);
      }
    }
  }


  protected void saveODS(OutputStream os) throws IOException {
    ZipOutputStream zos = new ZipOutputStream(os);

    final String xmlHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    ZipEntry entry = new ZipEntry("META-INF/manifest.xml");
    String[] lines = new String[] {
      xmlHeader,
      "<manifest:manifest xmlns:manifest=\"urn:oasis:names:tc:opendocument:xmlns:manifest:1.0\">",
      "  <manifest:file-entry manifest:media-type=\"application/vnd.oasis.opendocument.spreadsheet\" manifest:version=\"1.2\" manifest:full-path=\"/\"/>",
      "  <manifest:file-entry manifest:media-type=\"text/xml\" manifest:full-path=\"content.xml\"/>",
      "  <manifest:file-entry manifest:media-type=\"text/xml\" manifest:full-path=\"styles.xml\"/>",
      "  <manifest:file-entry manifest:media-type=\"text/xml\" manifest:full-path=\"meta.xml\"/>",
      "  <manifest:file-entry manifest:media-type=\"text/xml\" manifest:full-path=\"settings.xml\"/>",
      "</manifest:manifest>"
    };
    zos.putNextEntry(entry);
    zos.write(PApplet.join(lines, "\n").getBytes());
    zos.closeEntry();

    /*
    entry = new ZipEntry("meta.xml");
    lines = new String[] {
      xmlHeader,
      "<office:document-meta office:version=\"1.0\"" +
      " xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\" />"
    };
    zos.putNextEntry(entry);
    zos.write(PApplet.join(lines, "\n").getBytes());
    zos.closeEntry();

    entry = new ZipEntry("meta.xml");
    lines = new String[] {
      xmlHeader,
      "<office:document-settings office:version=\"1.0\"" +
      " xmlns:config=\"urn:oasis:names:tc:opendocument:xmlns:config:1.0\"" +
      " xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\"" +
      " xmlns:ooo=\"http://openoffice.org/2004/office\"" +
      " xmlns:xlink=\"http://www.w3.org/1999/xlink\" />"
    };
    zos.putNextEntry(entry);
    zos.write(PApplet.join(lines, "\n").getBytes());
    zos.closeEntry();

    entry = new ZipEntry("settings.xml");
    lines = new String[] {
      xmlHeader,
      "<office:document-settings office:version=\"1.0\"" +
      " xmlns:config=\"urn:oasis:names:tc:opendocument:xmlns:config:1.0\"" +
      " xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\"" +
      " xmlns:ooo=\"http://openoffice.org/2004/office\"" +
      " xmlns:xlink=\"http://www.w3.org/1999/xlink\" />"
    };
    zos.putNextEntry(entry);
    zos.write(PApplet.join(lines, "\n").getBytes());
    zos.closeEntry();

    entry = new ZipEntry("styles.xml");
    lines = new String[] {
      xmlHeader,
      "<office:document-styles office:version=\"1.0\"" +
      " xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\" />"
    };
    zos.putNextEntry(entry);
    zos.write(PApplet.join(lines, "\n").getBytes());
    zos.closeEntry();
    */

    final String[] dummyFiles = new String[] {
      "meta.xml", "settings.xml", "styles.xml"
    };
    lines = new String[] {
      xmlHeader,
      "<office:document-meta office:version=\"1.0\"" +
      " xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\" />"
    };
    byte[] dummyBytes = PApplet.join(lines, "\n").getBytes();
    for (String filename : dummyFiles) {
      entry = new ZipEntry(filename);
      zos.putNextEntry(entry);
      zos.write(dummyBytes);
      zos.closeEntry();
    }

    //

    entry = new ZipEntry("mimetype");
    zos.putNextEntry(entry);
    zos.write("application/vnd.oasis.opendocument.spreadsheet".getBytes());
    zos.closeEntry();

    //

    entry = new ZipEntry("content.xml");
    zos.putNextEntry(entry);
    //lines = new String[] {
    writeUTF(zos, new String[] {
      xmlHeader,
      "<office:document-content" +
        " xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\"" +
        " xmlns:text=\"urn:oasis:names:tc:opendocument:xmlns:text:1.0\"" +
        " xmlns:table=\"urn:oasis:names:tc:opendocument:xmlns:table:1.0\"" +
        " office:version=\"1.2\">",
     "  <office:body>",
     "    <office:spreadsheet>",
     "      <table:table table:name=\"Sheet1\" table:print=\"false\">"
    });
    //zos.write(PApplet.join(lines, "\n").getBytes());

    byte[] rowStart = "        <table:table-row>\n".getBytes();
    byte[] rowStop = "        </table:table-row>\n".getBytes();

    if (hasColumnTitles()) {
      zos.write(rowStart);
      for (int i = 0; i < getColumnCount(); i++) {
        saveStringODS(zos, columnTitles[i]);
      }
      zos.write(rowStop);
    }

    for (TableRow row : rows()) {
      zos.write(rowStart);
      for (int i = 0; i < getColumnCount(); i++) {
        if (columnTypes[i] == STRING || columnTypes[i] == CATEGORY) {
          saveStringODS(zos, row.getString(i));
        } else {
          saveNumberODS(zos, row.getString(i));
        }
      }
      zos.write(rowStop);
    }

    //lines = new String[] {
    writeUTF(zos, new String[] {
      "      </table:table>",
      "    </office:spreadsheet>",
      "  </office:body>",
      "</office:document-content>"
    });
    //zos.write(PApplet.join(lines, "\n").getBytes());
    zos.closeEntry();

    zos.flush();
    zos.close();
  }


  void saveStringODS(OutputStream output, String text) throws IOException {
    // At this point, I should have just used the XML library. But this does
    // save us from having to create the entire document in memory again before
    // writing to the file. So while it's dorky, the outcome is still useful.
    StringBuilder sanitized = new StringBuilder();
    if (text != null) {
      char[] array = text.toCharArray();
      for (char c : array) {
        if (c == '&') {
          sanitized.append("&amp;");
        } else if (c == '\'') {
          sanitized.append("&apos;");
        } else if (c == '"') {
          sanitized.append("&quot;");
        } else if (c == '<') {
          sanitized.append("&lt;");
        } else if (c == '>') {
          sanitized.append("&rt;");
        } else if (c < 32 || c > 127) {
          sanitized.append("&#" + ((int) c) + ";");
        } else {
          sanitized.append(c);
        }
      }
    }

    writeUTF(output,
             "          <table:table-cell office:value-type=\"string\">",
             "            <text:p>" + sanitized + "</text:p>",
             "          </table:table-cell>");
  }


  void saveNumberODS(OutputStream output, String text) throws IOException {
    writeUTF(output,
             "          <table:table-cell office:value-type=\"float\" office:value=\"" + text + "\">",
             "            <text:p>" + text + "</text:p>",
             "          </table:table-cell>");
  }


  static Charset utf8;

  static void writeUTF(OutputStream output, String... lines) throws IOException {
    if (utf8 == null) {
      utf8 = Charset.forName("UTF-8");
    }
    for (String str : lines) {
      output.write(str.getBytes(utf8));
      output.write('\n');
    }
  }


  protected void saveBinary(OutputStream os) throws IOException {
    DataOutputStream output = new DataOutputStream(new BufferedOutputStream(os));
    output.writeInt(0x9007AB1E);  // version
    output.writeInt(getRowCount());
    output.writeInt(getColumnCount());
    if (columnTitles != null) {
      output.writeBoolean(true);
      for (String title : columnTitles) {
        output.writeUTF(title);
      }
    } else {
      output.writeBoolean(false);
    }
    for (int i = 0; i < getColumnCount(); i++) {
      //System.out.println(i + " is " + columnTypes[i]);
      output.writeInt(columnTypes[i]);
    }

    for (int i = 0; i < getColumnCount(); i++) {
      if (columnTypes[i] == CATEGORY) {
        columnCategories[i].write(output);
      }
    }
    if (missingString == null) {
      output.writeBoolean(false);
    } else {
      output.writeBoolean(true);
      output.writeUTF(missingString);
    }
    output.writeInt(missingInt);
    output.writeLong(missingLong);
    output.writeFloat(missingFloat);
    output.writeDouble(missingDouble);
    output.writeInt(missingCategory);

    for (TableRow row : rows()) {
      for (int col = 0; col < getColumnCount(); col++) {
        switch (columnTypes[col]) {
        case STRING:
          String str = row.getString(col);
          if (str == null) {
            output.writeBoolean(false);
          } else {
            output.writeBoolean(true);
            output.writeUTF(str);
          }
          break;
        case INT:
          output.writeInt(row.getInt(col));
          break;
        case LONG:
          output.writeLong(row.getLong(col));
          break;
        case FLOAT:
          output.writeFloat(row.getFloat(col));
          break;
        case DOUBLE:
          output.writeDouble(row.getDouble(col));
          break;
        case CATEGORY:
          String peace = row.getString(col);
          if (peace.equals(missingString)) {
            output.writeInt(missingCategory);
          } else {
            output.writeInt(columnCategories[col].index(peace));
          }
          break;
        }
      }
    }

    output.flush();
    output.close();
  }


  protected void loadBinary(InputStream is) throws IOException {
    DataInputStream input = new DataInputStream(new BufferedInputStream(is));

    int magic = input.readInt();
    if (magic != 0x9007AB1E) {
      throw new IOException("Not a compatible binary table (magic was " + PApplet.hex(magic) + ")");
    }
    int rowCount = input.readInt();
    setRowCount(rowCount);
    int columnCount = input.readInt();
    setColumnCount(columnCount);

    boolean hasTitles = input.readBoolean();
    if (hasTitles) {
      columnTitles = new String[getColumnCount()];
      for (int i = 0; i < columnCount; i++) {
        //columnTitles[i] = input.readUTF();
        setColumnTitle(i, input.readUTF());
      }
    }
    for (int column = 0; column < columnCount; column++) {
      int newType = input.readInt();
      columnTypes[column] = newType;
      switch (newType) {
      case INT:
        columns[column] = new int[rowCount];
        break;
      case LONG:
        columns[column] = new long[rowCount];;
        break;
      case FLOAT:
        columns[column] = new float[rowCount];;
        break;
      case DOUBLE:
        columns[column] = new double[rowCount];;
        break;
      case STRING:
        columns[column] = new String[rowCount];;
        break;
      case CATEGORY:
        columns[column] = new int[rowCount];;
        break;
      default:
        throw new IllegalArgumentException(newType + " is not a valid column type.");
      }
    }

    for (int i = 0; i < columnCount; i++) {
      if (columnTypes[i] == CATEGORY) {
        columnCategories[i] = new HashMapBlows(input);
      }
    }

    if (input.readBoolean()) {
      missingString = input.readUTF();
    } else {
      missingString = null;
    }
    missingInt = input.readInt();
    missingLong = input.readLong();
    missingFloat = input.readFloat();
    missingDouble = input.readDouble();
    missingCategory = input.readInt();

    for (int row = 0; row < rowCount; row++) {
      for (int col = 0; col < columnCount; col++) {
        switch (columnTypes[col]) {
        case STRING:
          String str = null;
          if (input.readBoolean()) {
            str = input.readUTF();
          }
          setString(row, col, str);
          break;
        case INT:
          setInt(row, col, input.readInt());
          break;
        case LONG:
          setLong(row, col, input.readLong());
          break;
        case FLOAT:
          setFloat(row, col, input.readFloat());
          break;
        case DOUBLE:
          setDouble(row, col, input.readDouble());
          break;
        case CATEGORY:
          int index = input.readInt();
          //String name = columnCategories[col].key(index);
          setInt(row, col, index);
          break;
        }
      }
    }

    input.close();
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * @webref table:method
   * @brief Adds a new column to a table
   * @see Table#removeColumn(String)
   */
  public void addColumn() {
    addColumn(null, STRING);
  }


  /**
   * @param title the title to be used for the new column
   */
  public void addColumn(String title) {
    addColumn(title, STRING);
  }


  /**
   * @param type the type to be used for the new column: INT, LONG, FLOAT, DOUBLE, or STRING
   */
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
      case CATEGORY: columns[index] = new int[rowCount]; break;
    }
  }

 /**
   * @webref table:method
   * @brief Removes a column from a table
   * @param columnName the title of the column to be removed
   * @see Table#addColumn()
   */
  public void removeColumn(String columnName) {
    removeColumn(getColumnIndex(columnName));
  }

 /**
   * @param column the index number of the column to be removed
   */
  public void removeColumn(int column) {
    int newCount = columns.length - 1;

    Object[] columnsTemp = new Object[newCount];
    HashMapBlows[] catTemp = new HashMapBlows[newCount];

    for (int i = 0; i < column; i++) {
      columnsTemp[i] = columns[i];
      catTemp[i] = columnCategories[i];
    }
    for (int i = column; i < newCount; i++) {
      columnsTemp[i] = columns[i+1];
      catTemp[i] = columnCategories[i+1];
    }

    columns = columnsTemp;
    columnCategories = catTemp;

    if (columnTitles != null) {
      String[] titlesTemp = new String[newCount];
      for (int i = 0; i < column; i++) {
        titlesTemp[i] = columnTitles[i];
      }
      for (int i = column; i < newCount; i++) {
        titlesTemp[i] = columnTitles[i+1];
      }
      columnTitles = titlesTemp;
      columnIndices = null;
    }
  }


  /**
   * @webref table:method
   * @brief Gets the number of columns in a table
   * @see Table#getRowCount()
   */
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


  static int parseColumnType(String columnType) {
    columnType = columnType.toLowerCase();
    int type = -1;
    if (columnType.equals("string")) {
      type = STRING;
    } else if (columnType.equals("int")) {
      type = INT;
    } else if (columnType.equals("long")) {
      type = LONG;
    } else if (columnType.equals("float")) {
      type = FLOAT;
    } else if (columnType.equals("double")) {
      type = DOUBLE;
    } else if (columnType.equals("category")) {
      type = CATEGORY;
    } else {
      throw new IllegalArgumentException("'" + columnType + "' is not a valid column type.");
    }
    return type;
  }


  /**
   * Set the data type for a column so that using it is more efficient.
   * @param column the column to change
   * @param columnType One of int, long, float, double, string, or category.
   */
  public void setColumnType(int column, String columnType) {
    setColumnType(column, parseColumnType(columnType));
  }


  public void setColumnType(String columnName, int newType) {
    setColumnType(checkColumnIndex(columnName), newType);
  }


  /**
   * Sets the column type. If data already exists, then it'll be converted to
   * the new type.
   * @param column the column whose type should be changed
   * @param newType something fresh, maybe try an int or a float for size?
   */
  public void setColumnType(int column, int newType) {
    switch (newType) {
      case INT: {
        int[] intData = new int[rowCount];
        for (int row = 0; row < rowCount; row++) {
          String s = getString(row, column);
          intData[row] = (s == null) ? missingInt : PApplet.parseInt(s, missingInt);
        }
        columns[column] = intData;
        break;
      }
      case LONG: {
        long[] longData = new long[rowCount];
        for (int row = 0; row < rowCount; row++) {
          String s = getString(row, column);
          try {
            longData[row] = (s == null) ? missingLong : Long.parseLong(s);
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
          floatData[row] = (s == null) ? missingFloat : PApplet.parseFloat(s, missingFloat);
        }
        columns[column] = floatData;
        break;
      }
      case DOUBLE: {
        double[] doubleData = new double[rowCount];
        for (int row = 0; row < rowCount; row++) {
          String s = getString(row, column);
          try {
            doubleData[row] = (s == null) ? missingDouble : Double.parseDouble(s);
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
      case CATEGORY: {
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
    for (int col = 0; col < getColumnCount(); col++) {
      setColumnType(col, type);
    }
  }


  public void setColumnTypes(int[] types) {
    ensureColumn(types.length - 1);
    for (int col = 0; col < types.length; col++) {
      setColumnType(col, types[col]);
    }
  }


  /**
   * Set the titles (and if a second column is present) the data types for
   * this table based on a file loaded separately. This will look for the
   * title in column 0, and the type in column 1. Better yet, specify a
   * column named "title" and another named "type" in the dictionary table
   * to future-proof the code.
   * @param dictionary
   */
  public void setColumnTypes(final Table dictionary) {
    ensureColumn(dictionary.getRowCount() - 1);
    int titleCol = 0;
    int typeCol = 1;
    if (dictionary.hasColumnTitles()) {
      titleCol = dictionary.getColumnIndex("title", true);
      typeCol = dictionary.getColumnIndex("type", true);
    }
    setColumnTitles(dictionary.getStringColumn(titleCol));
    final String[] typeNames = dictionary.getStringColumn(typeCol);

    if (dictionary.getColumnCount() > 1) {
      if (getRowCount() > 1000) {
        int proc = Runtime.getRuntime().availableProcessors();
        ExecutorService pool = Executors.newFixedThreadPool(proc/2);
        for (int i = 0; i < dictionary.getRowCount(); i++) {
          final int col = i;
          pool.execute(new Runnable() {
            public void run() {
              setColumnType(col, typeNames[col]);
            }
          });
        }
        pool.shutdown();
        while (!pool.isTerminated()) {
          Thread.yield();
        }

      } else {
        for (int col = 0; col < dictionary.getRowCount(); col++) {
//          setColumnType(i, dictionary.getString(i, typeCol));
          setColumnType(col, typeNames[col]);
        }
      }
    }
  }


  public int getColumnType(String columnName) {
    return getColumnType(getColumnIndex(columnName));
  }


  /** Returns one of Table.STRING, Table.INT, etc... */
  public int getColumnType(int column) {
    return columnTypes[column];
  }


  public int[] getColumnTypes() {
    return columnTypes;
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Remove the first row from the data set, and use it as the column titles.
   * Use loadTable("table.csv", "header") instead.
   */
  @Deprecated
  public String[] removeTitleRow() {
    String[] titles = getStringRow(0);
    removeRow(0);
    setColumnTitles(titles);
    return titles;
  }


  public void setColumnTitles(String[] titles) {
    if (titles != null) {
      ensureColumn(titles.length - 1);
    }
    columnTitles = titles;
    columnIndices = null;  // remove the cache
  }


  public void setColumnTitle(int column, String title) {
    ensureColumn(column);
    if (columnTitles == null) {
      columnTitles = new String[getColumnCount()];
    }
    columnTitles[column] = title;
    columnIndices = null;  // reset these fellas
  }


  public boolean hasColumnTitles() {
    return columnTitles != null;
  }


  public String[] getColumnTitles() {
    return columnTitles;
  }


  public String getColumnTitle(int col) {
    return (columnTitles == null) ? null : columnTitles[col];
  }


  public int getColumnIndex(String columnName) {
    return getColumnIndex(columnName, true);
  }


  /**
   * Get the index of a column.
   * @param name Name of the column.
   * @param report Whether to throw an exception if the column wasn't found.
   * @return index of the found column, or -1 if not found.
   */
  protected int getColumnIndex(String name, boolean report) {
    if (columnTitles == null) {
      if (report) {
        throw new IllegalArgumentException("This table has no header, so no column titles are set.");
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
        // Throws an exception here because the name is known and therefore most useful.
        // (Rather than waiting for it to fail inside, say, getInt())
        throw new IllegalArgumentException("This table has no column named '" + name + "'");
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

  /**
   * @webref table:method
   * @brief Gets the number of rows in a table
   * @see Table#getColumnCount()
   */
  public int getRowCount() {
    return rowCount;
  }


  public int lastRowIndex() {
    return getRowCount() - 1;
  }


  /**
   * @webref table:method
   * @brief Removes all rows from a table
   * @see Table#addRow()
   * @see Table#removeRow(int)
   */
  public void clearRows() {
    setRowCount(0);
  }


  public void setRowCount(int newCount) {
    if (newCount != rowCount) {
      if (newCount > 1000000) {
        System.out.print("Note: setting maximum row count to " + PApplet.nfc(newCount));
      }
      long t = System.currentTimeMillis();
      for (int col = 0; col < columns.length; col++) {
        switch (columnTypes[col]) {
          case INT: columns[col] = PApplet.expand((int[]) columns[col], newCount); break;
          case LONG: columns[col] = PApplet.expand((long[]) columns[col], newCount); break;
          case FLOAT: columns[col] = PApplet.expand((float[]) columns[col], newCount); break;
          case DOUBLE: columns[col] = PApplet.expand((double[]) columns[col], newCount); break;
          case STRING: columns[col] = PApplet.expand((String[]) columns[col], newCount); break;
          case CATEGORY: columns[col] = PApplet.expand((int[]) columns[col], newCount); break;
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
        System.out.println(" (resize took " + PApplet.nfc(ms) + " ms)");
      }
    }
    rowCount = newCount;
  }


 /**
   * @webref table:method
   * @brief Adds a row to a table
   * @see Table#removeRow(int)
   * @see Table#clearRows()
   */
  public TableRow addRow() {
    //if (rowIncrement == 0) {
    setRowCount(rowCount + 1);
    return new RowPointer(this, rowCount - 1);
  }


 /**
   * @param source a reference to the original row to be duplicated
   */
  public TableRow addRow(TableRow source) {
    return setRow(rowCount, source);
  }


  public TableRow setRow(int row, TableRow source) {
    // Make sure there are enough columns to add this data
    ensureBounds(row, source.getColumnCount() - 1);

    for (int col = 0; col < Math.min(source.getColumnCount(), columns.length); col++) {
      switch (columnTypes[col]) {
      case INT:
        setInt(row, col, source.getInt(col));
        break;
      case LONG:
        setLong(row, col, source.getLong(col));
        break;
      case FLOAT:
        setFloat(row, col, source.getFloat(col));
        break;
      case DOUBLE:
        setDouble(row, col, source.getDouble(col));
        break;
      case STRING:
        setString(row, col, source.getString(col));
        break;
      case CATEGORY:
        int index = source.getInt(col);
        setInt(row, col, index);
        if (!columnCategories[col].hasCategory(index)) {
          columnCategories[col].setCategory(index, source.getString(col));
        }
        break;

      default:
        throw new RuntimeException("no types");
      }
    }
    return new RowPointer(this, row);
  }


 /**
   * @nowebref
   */
  public TableRow addRow(Object[] columnData) {
    setRow(getRowCount(), columnData);
    return new RowPointer(this, rowCount - 1);
  }


  public void addRows(Table source) {
    int index = getRowCount();
    setRowCount(index + source.getRowCount());
    for (TableRow row : source.rows()) {
      setRow(index++, row);
    }
  }


  public void insertRow(int insert, Object[] columnData) {
    for (int col = 0; col < columns.length; col++) {
      switch (columnTypes[col]) {
        case CATEGORY:
        case INT: {
          int[] intTemp = new int[rowCount+1];
          System.arraycopy(columns[col], 0, intTemp, 0, insert);
          System.arraycopy(columns[col], insert, intTemp, insert+1, rowCount - insert);
          columns[col] = intTemp;
          break;
        }
        case LONG: {
          long[] longTemp = new long[rowCount+1];
          System.arraycopy(columns[col], 0, longTemp, 0, insert);
          System.arraycopy(columns[col], insert, longTemp, insert+1, rowCount - insert);
          columns[col] = longTemp;
          break;
        }
        case FLOAT: {
          float[] floatTemp = new float[rowCount+1];
          System.arraycopy(columns[col], 0, floatTemp, 0, insert);
          System.arraycopy(columns[col], insert, floatTemp, insert+1, rowCount - insert);
          columns[col] = floatTemp;
          break;
        }
        case DOUBLE: {
          double[] doubleTemp = new double[rowCount+1];
          System.arraycopy(columns[col], 0, doubleTemp, 0, insert);
          System.arraycopy(columns[col], insert, doubleTemp, insert+1, rowCount - insert);
          columns[col] = doubleTemp;
          break;
        }
        case STRING: {
          String[] stringTemp = new String[rowCount+1];
          System.arraycopy(columns[col], 0, stringTemp, 0, insert);
          System.arraycopy(columns[col], insert, stringTemp, insert+1, rowCount - insert);
          columns[col] = stringTemp;
          break;
        }
      }
    }
    setRow(insert, columnData);
    rowCount++;
  }

  /**
   * @webref table:method
   * @brief Removes a row from a table
   * @param row ID number of the row to remove
   * @see Table#addRow()
   * @see Table#clearRows()
   */
  public void removeRow(int row) {
    for (int col = 0; col < columns.length; col++) {
      switch (columnTypes[col]) {
        case CATEGORY:
        case INT: {
          int[] intTemp = new int[rowCount-1];
//          int[] intData = (int[]) columns[col];
//          System.arraycopy(intData, 0, intTemp, 0, dead);
//          System.arraycopy(intData, dead+1, intTemp, dead, (rowCount - dead) + 1);
          System.arraycopy(columns[col], 0, intTemp, 0, row);
          System.arraycopy(columns[col], row+1, intTemp, row, (rowCount - row) - 1);
          columns[col] = intTemp;
          break;
        }
        case LONG: {
          long[] longTemp = new long[rowCount-1];
//          long[] longData = (long[]) columns[col];
//          System.arraycopy(longData, 0, longTemp, 0, dead);
//          System.arraycopy(longData, dead+1, longTemp, dead, (rowCount - dead) + 1);
          System.arraycopy(columns[col], 0, longTemp, 0, row);
          System.arraycopy(columns[col], row+1, longTemp, row, (rowCount - row) - 1);
          columns[col] = longTemp;
          break;
        }
        case FLOAT: {
          float[] floatTemp = new float[rowCount-1];
//          float[] floatData = (float[]) columns[col];
//          System.arraycopy(floatData, 0, floatTemp, 0, dead);
//          System.arraycopy(floatData, dead+1, floatTemp, dead, (rowCount - dead) + 1);
          System.arraycopy(columns[col], 0, floatTemp, 0, row);
          System.arraycopy(columns[col], row+1, floatTemp, row, (rowCount - row) - 1);
          columns[col] = floatTemp;
          break;
        }
        case DOUBLE: {
          double[] doubleTemp = new double[rowCount-1];
//          double[] doubleData = (double[]) columns[col];
//          System.arraycopy(doubleData, 0, doubleTemp, 0, dead);
//          System.arraycopy(doubleData, dead+1, doubleTemp, dead, (rowCount - dead) + 1);
          System.arraycopy(columns[col], 0, doubleTemp, 0, row);
          System.arraycopy(columns[col], row+1, doubleTemp, row, (rowCount - row) - 1);
          columns[col] = doubleTemp;
          break;
        }
        case STRING: {
          String[] stringTemp = new String[rowCount-1];
          System.arraycopy(columns[col], 0, stringTemp, 0, row);
          System.arraycopy(columns[col], row+1, stringTemp, row, (rowCount - row) - 1);
          columns[col] = stringTemp;
        }
      }
    }
    rowCount--;
  }


  /*
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
    case CATEGORY:
      int[] indexData = (int[]) columns[col];
      indexData[row] = columnCategories[col].index(piece);
      break;
    default:
      throw new IllegalArgumentException("That's not a valid column type.");
    }
  }
  */


  public void setRow(int row, Object[] pieces) {
    ensureBounds(row, pieces.length - 1);
    // pieces.length may be less than columns.length, so loop over pieces
    for (int col = 0; col < pieces.length; col++) {
      setRowCol(row, col, pieces[col]);
    }
  }


  protected void setRowCol(int row, int col, Object piece) {
    switch (columnTypes[col]) {
      case STRING:
        String[] stringData = (String[]) columns[col];
        if (piece == null) {
          stringData[row] = null;
//        } else if (piece instanceof String) {
//          stringData[row] = (String) piece;
        } else {
          // Calls toString() on the object, which is 'return this' for String
          stringData[row] = String.valueOf(piece);
        }
        break;
      case INT:
        int[] intData = (int[]) columns[col];
        //intData[row] = PApplet.parseInt(piece, missingInt);
        if (piece == null) {
          intData[row] = missingInt;
        } else if (piece instanceof Integer) {
          intData[row] = (Integer) piece;
        } else {
          intData[row] = PApplet.parseInt(String.valueOf(piece), missingInt);
        }
        break;
      case LONG:
        long[] longData = (long[]) columns[col];
        if (piece == null) {
          longData[row] = missingLong;
        } else if (piece instanceof Long) {
          longData[row] = (Long) piece;
        } else {
          try {
            longData[row] = Long.parseLong(String.valueOf(piece));
          } catch (NumberFormatException nfe) {
            longData[row] = missingLong;
          }
        }
        break;
      case FLOAT:
        float[] floatData = (float[]) columns[col];
        if (piece == null) {
          floatData[row] = missingFloat;
        } else if (piece instanceof Float) {
          floatData[row] = (Float) piece;
        } else {
          floatData[row] = PApplet.parseFloat(String.valueOf(piece), missingFloat);
        }
        break;
      case DOUBLE:
        double[] doubleData = (double[]) columns[col];
        if (piece == null) {
          doubleData[row] = missingDouble;
        } else if (piece instanceof Double) {
          doubleData[row] = (Double) piece;
        } else {
          try {
            doubleData[row] = Double.parseDouble(String.valueOf(piece));
          } catch (NumberFormatException nfe) {
            doubleData[row] = missingDouble;
          }
        }
        break;
      case CATEGORY:
        int[] indexData = (int[]) columns[col];
        if (piece == null) {
          indexData[row] = missingCategory;
        } else {
          String peace = String.valueOf(piece);
          if (peace.equals(missingString)) {  // missingString might be null
            indexData[row] = missingCategory;
          } else {
            indexData[row] = columnCategories[col].index(peace);
          }
        }
        break;
      default:
        throw new IllegalArgumentException("That's not a valid column type.");
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  /**
   * @webref table:method
   * @brief Gets a row from a table
   * @param row ID number of the row to get
   * @see Table#rows()
   * @see Table#findRow(String, int)
   * @see Table#findRows(String, int)
   * @see Table#matchRow(String, int)
   * @see Table#matchRows(String, int)
   */
  public TableRow getRow(int row) {
    return new RowPointer(this, row);
  }


  /**
   * Note that this one iterator instance is shared by any calls to iterate
   * the rows of this table. This is very efficient, but not thread-safe.
   * If you want to iterate in a multi-threaded manner, don't use the iterator.
   *
   * @webref table:method
   * @brief Gets multiple rows from a table
   * @see Table#getRow(int)
   * @see Table#findRow(String, int)
   * @see Table#findRows(String, int)
   * @see Table#matchRow(String, int)
   * @see Table#matchRows(String, int)
   */
  public Iterable<TableRow> rows() {
    return new Iterable<TableRow>() {
      public Iterator<TableRow> iterator() {
        if (rowIterator == null) {
          rowIterator = new RowIterator(Table.this);
        } else {
          rowIterator.reset();
        }
        return rowIterator;
      }
    };
  }

  /**
   * @nowebref
   */
  public Iterable<TableRow> rows(final int[] indices) {
    return new Iterable<TableRow>() {
      public Iterator<TableRow> iterator() {
        return new RowIndexIterator(Table.this, indices);
      }
    };
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  static class RowPointer implements TableRow {
    Table table;
    int row;

    public RowPointer(Table table, int row) {
      this.table = table;
      this.row = row;
    }

    public void setRow(int row) {
      this.row = row;
    }

    public String getString(int column) {
      return table.getString(row, column);
    }

    public String getString(String columnName) {
      return table.getString(row, columnName);
    }

    public int getInt(int column) {
      return table.getInt(row, column);
    }

    public int getInt(String columnName) {
      return table.getInt(row, columnName);
    }

    public long getLong(int column) {
      return table.getLong(row, column);
    }

    public long getLong(String columnName) {
      return table.getLong(row, columnName);
    }

    public float getFloat(int column) {
      return table.getFloat(row, column);
    }

    public float getFloat(String columnName) {
      return table.getFloat(row, columnName);
    }

    public double getDouble(int column) {
      return table.getDouble(row, column);
    }

    public double getDouble(String columnName) {
      return table.getDouble(row, columnName);
    }

    public void setString(int column, String value) {
      table.setString(row, column, value);
    }

    public void setString(String columnName, String value) {
      table.setString(row, columnName, value);
    }

    public void setInt(int column, int value) {
      table.setInt(row, column, value);
    }

    public void setInt(String columnName, int value) {
      table.setInt(row, columnName, value);
    }

    public void setLong(int column, long value) {
      table.setLong(row, column, value);
    }

    public void setLong(String columnName, long value) {
      table.setLong(row, columnName, value);
    }

    public void setFloat(int column, float value) {
      table.setFloat(row, column, value);
    }

    public void setFloat(String columnName, float value) {
      table.setFloat(row, columnName, value);
    }

    public void setDouble(int column, double value) {
      table.setDouble(row, column, value);
    }

    public void setDouble(String columnName, double value) {
      table.setDouble(row, columnName, value);
    }

    public int getColumnCount() {
      return table.getColumnCount();
    }

    public int getColumnType(String columnName) {
      return table.getColumnType(columnName);
    }

    public int getColumnType(int column) {
      return table.getColumnType(column);
    }

    public int[] getColumnTypes() {
      return table.getColumnTypes();
    }

    public String getColumnTitle(int column) {
      return table.getColumnTitle(column);
    }

    public String[] getColumnTitles() {
      return table.getColumnTitles();
    }

    public void print() {
      write(new PrintWriter(System.out));
    }

    public void write(PrintWriter writer) {
      for (int i = 0 ; i < getColumnCount(); i++) {
        if (i != 0) {
          writer.print('\t');
        }
        writer.print(getString(i));
      }
    }
  }


  static class RowIterator implements Iterator<TableRow> {
    Table table;
    RowPointer rp;
    int row;

    public RowIterator(Table table) {
      this.table = table;
      row = -1;
      rp = new RowPointer(table, row);
    }

    public void remove() {
      table.removeRow(row);
    }

    public TableRow next() {
      rp.setRow(++row);
      return rp;
    }

    public boolean hasNext() {
      return row+1 < table.getRowCount();
    }

    public void reset() {
      row = -1;
    }
  }


  static class RowIndexIterator implements Iterator<TableRow> {
    Table table;
    RowPointer rp;
    int[] indices;
    int index;

    public RowIndexIterator(Table table, int[] indices) {
      this.table = table;
      this.indices = indices;
      index = -1;
      // just set to something arbitrary
      rp = new RowPointer(table, -1);
    }

    public void remove() {
      table.removeRow(indices[index]);
    }

    public TableRow next() {
      rp.setRow(indices[++index]);
      return rp;
    }

    public boolean hasNext() {
      //return row+1 < table.getRowCount();
      return index + 1 < indices.length;
    }

    public void reset() {
      index = -1;
    }
  }


  /*
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

          public void setString(int column, String value) { immutable(); }
          public void setString(String columnName, String value) { immutable(); }
          public void setInt(int column, int value) { immutable(); }
          public void setInt(String columnName, int value) { immutable(); }
          public void setLong(int column, long value) { immutable(); }
          public void setLong(String columnName, long value) { immutable(); }
          public void setFloat(int column, float value) { immutable(); }
          public void setFloat(String columnName, float value) { immutable(); }
          public void setDouble(int column, double value) { immutable(); }
          public void setDouble(String columnName, double value) { immutable(); }

          private void immutable() {
            throw new IllegalArgumentException("This TableRow cannot be modified.");
          }

          public int getColumnCount() {
            try {
              return rs.getMetaData().getColumnCount();
            } catch (SQLException e) {
              e.printStackTrace();
              return -1;
            }
          }


          public int getColumnType(String columnName) {
            // unimplemented
          }


          public int getColumnType(int column) {
            // unimplemented
          }

        };
      }

      public void remove() {
        throw new IllegalArgumentException("remove() not supported");
      }
    };
  }
  */


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * @webref table:method
   * @brief Get an integer value from the specified row and column
   * @param row ID number of the row to reference
   * @param column ID number of the column to reference
   * @see Table#getFloat(int, int)
   * @see Table#getString(int, int)
   * @see Table#getStringColumn(String)
   * @see Table#setInt(int, int, int)
   * @see Table#setFloat(int, int, float)
   * @see Table#setString(int, int, String)
   */
  public int getInt(int row, int column) {
    checkBounds(row, column);
    if (columnTypes[column] == INT ||
        columnTypes[column] == CATEGORY) {
      int[] intData = (int[]) columns[column];
      return intData[row];
    }
    String str = getString(row, column);
    return (str == null || str.equals(missingString)) ?
      missingInt : PApplet.parseInt(str, missingInt);
  }

  /**
   * @param columnName title of the column to reference
   */
  public int getInt(int row, String columnName) {
    return getInt(row, getColumnIndex(columnName));
  }


  public void setMissingInt(int value) {
    missingInt = value;
  }


  /**
   * @webref table:method
   * @brief Store an integer value in the specified row and column
   * @param row ID number of the target row
   * @param column ID number of the target column
   * @param value value to assign
   * @see Table#setFloat(int, int, float)
   * @see Table#setString(int, int, String)
   * @see Table#getInt(int, int)
   * @see Table#getFloat(int, int)
   * @see Table#getString(int, int)
   * @see Table#getStringColumn(String)
   */
  public void setInt(int row, int column, int value) {
    if (columnTypes[column] == STRING) {
      setString(row, column, String.valueOf(value));

    } else {
      ensureBounds(row, column);
      if (columnTypes[column] != INT &&
          columnTypes[column] != CATEGORY) {
        throw new IllegalArgumentException("Column " + column + " is not an int column.");
      }
      int[] intData = (int[]) columns[column];
      intData[row] = value;
    }
  }

  /**
   * @param columnName title of the target column
   */
  public void setInt(int row, String columnName, int value) {
    setInt(row, getColumnIndex(columnName), value);
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


  public void setLong(int row, int column, long value) {
    if (columnTypes[column] == STRING) {
      setString(row, column, String.valueOf(value));

    } else {
      ensureBounds(row, column);
      if (columnTypes[column] != LONG) {
        throw new IllegalArgumentException("Column " + column + " is not a 'long' column.");
      }
      long[] longData = (long[]) columns[column];
      longData[row] = value;
    }
  }


  public void setLong(int row, String columnName, long value) {
    setLong(row, getColumnIndex(columnName), value);
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
   *
   * @webref table:method
   * @brief Get a float value from the specified row and column
   * @param row ID number of the row to reference
   * @param column ID number of the column to reference
   * @see Table#getInt(int, int)
   * @see Table#getString(int, int)
   * @see Table#getStringColumn(String)
   * @see Table#setInt(int, int, int)
   * @see Table#setFloat(int, int, float)
   * @see Table#setString(int, int, String)
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

  /**
   * @param columnName title of the column to reference
   */
  public float getFloat(int row, String columnName) {
    return getFloat(row, getColumnIndex(columnName));
  }


  public void setMissingFloat(float value) {
    missingFloat = value;
  }


  /**
   * @webref table:method
   * @brief Store a float value in the specified row and column
   * @param row ID number of the target row
   * @param column ID number of the target column
   * @param value value to assign
   * @see Table#setInt(int, int, int)
   * @see Table#setString(int, int, String)
   * @see Table#getInt(int, int)
   * @see Table#getFloat(int, int)
   * @see Table#getString(int, int)
   * @see Table#getStringColumn(String)
   */
  public void setFloat(int row, int column, float value) {
    if (columnTypes[column] == STRING) {
      setString(row, column, String.valueOf(value));

    } else {
      ensureBounds(row, column);
      if (columnTypes[column] != FLOAT) {
        throw new IllegalArgumentException("Column " + column + " is not a float column.");
      }
      float[] longData = (float[]) columns[column];
      longData[row] = value;
    }
  }

  /**
   * @param columnName title of the target column
   */
  public void setFloat(int row, String columnName, float value) {
    setFloat(row, getColumnIndex(columnName), value);
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


  public void setDouble(int row, int column, double value) {
    if (columnTypes[column] == STRING) {
      setString(row, column, String.valueOf(value));

    } else {
      ensureBounds(row, column);
      if (columnTypes[column] != DOUBLE) {
        throw new IllegalArgumentException("Column " + column + " is not a 'double' column.");
      }
      double[] doubleData = (double[]) columns[column];
      doubleData[row] = value;
    }
  }


  public void setDouble(int row, String columnName, double value) {
    setDouble(row, getColumnIndex(columnName), value);
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
   *
   * @webref table:method
   * @brief Get an String value from the specified row and column
   * @param row ID number of the row to reference
   * @param column ID number of the column to reference
   * @see Table#getInt(int, int)
   * @see Table#getFloat(int, int)
   * @see Table#getStringColumn(String)
   * @see Table#setInt(int, int, int)
   * @see Table#setFloat(int, int, float)
   * @see Table#setString(int, int, String)
   */
  public String getString(int row, int column) {
    checkBounds(row, column);
    if (columnTypes[column] == STRING) {
      String[] stringData = (String[]) columns[column];
      return stringData[row];
    } else if (columnTypes[column] == CATEGORY) {
      int cat = getInt(row, column);
      if (cat == missingCategory) {
        return missingString;
      }
      return columnCategories[column].key(cat);
    } else if (columnTypes[column] == FLOAT) {
      if (Float.isNaN(getFloat(row, column))) {
        return null;
      }
    } else if (columnTypes[column] == DOUBLE) {
      if (Double.isNaN(getFloat(row, column))) {
        return null;
      }
    }
    return String.valueOf(Array.get(columns[column], row));
  }


  /**
   * @param columnName title of the column to reference
   */
  public String getString(int row, String columnName) {
    return getString(row, getColumnIndex(columnName));
  }


  /**
   * Treat entries with this string as "missing". Also used for categorial.
   */
  public void setMissingString(String value) {
    missingString = value;
  }


  /**
   * @webref table:method
   * @brief Store a String value in the specified row and column
   * @param row ID number of the target row
   * @param column ID number of the target column
   * @param value value to assign
   * @see Table#setInt(int, int, int)
   * @see Table#setFloat(int, int, float)
   * @see Table#getInt(int, int)
   * @see Table#getFloat(int, int)
   * @see Table#getString(int, int)
   * @see Table#getStringColumn(String)
   */
  public void setString(int row, int column, String value) {
    ensureBounds(row, column);
    if (columnTypes[column] != STRING) {
      throw new IllegalArgumentException("Column " + column + " is not a String column.");
    }
    String[] stringData = (String[]) columns[column];
    stringData[row] = value;
  }

  /**
   * @param columnName title of the target column
   */
  public void setString(int row, String columnName, String value) {
    int column = checkColumnIndex(columnName);
    setString(row, column, value);
  }

  /**
   * @webref table:method
   * @brief Gets all values in the specified column
   * @param columnName title of the column to search
   * @see Table#getInt(int, int)
   * @see Table#getFloat(int, int)
   * @see Table#getString(int, int)
   * @see Table#setInt(int, int, int)
   * @see Table#setFloat(int, int, float)
   * @see Table#setString(int, int, String)
   */
  public String[] getStringColumn(String columnName) {
    int col = getColumnIndex(columnName);
    return (col == -1) ? null : getStringColumn(col);
  }


  /**
   * @param column ID number of the column to search
   */
  public String[] getStringColumn(int column) {
    String[] outgoing = new String[rowCount];
    for (int i = 0; i < rowCount; i++) {
      outgoing[i] = getString(i, column);
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
   * Return the row that contains the first String that matches.
   * @param value the String to match
   * @param column ID number of the column to search
   */
  public int findRowIndex(String value, int column) {
    checkColumn(column);
    if (columnTypes[column] == STRING) {
      String[] stringData = (String[]) columns[column];
      if (value == null) {
        for (int row = 0; row < rowCount; row++) {
          if (stringData[row] == null) return row;
        }
      } else {
        for (int row = 0; row < rowCount; row++) {
          if (stringData[row] != null && stringData[row].equals(value)) {
            return row;
          }
        }
      }
    } else {  // less efficient, includes conversion as necessary
      for (int row = 0; row < rowCount; row++) {
        String str = getString(row, column);
        if (str == null) {
          if (value == null) {
            return row;
          }
        } else if (str.equals(value)) {
          return row;
        }
      }
    }
    return -1;
  }


  /**
   * Return the row that contains the first String that matches.
   * @param value the String to match
   * @param columnName title of the column to search
   */
  public int findRowIndex(String value, String columnName) {
    return findRowIndex(value, getColumnIndex(columnName));
  }


  /**
   * Return a list of rows that contain the String passed in. If there are no
   * matches, a zero length array will be returned (not a null array).
   * @param value the String to match
   * @param column ID number of the column to search
   */
  public int[] findRowIndices(String value, int column) {
    int[] outgoing = new int[rowCount];
    int count = 0;

    checkColumn(column);
    if (columnTypes[column] == STRING) {
      String[] stringData = (String[]) columns[column];
      if (value == null) {
        for (int row = 0; row < rowCount; row++) {
          if (stringData[row] == null) {
            outgoing[count++] = row;
          }
        }
      } else {
        for (int row = 0; row < rowCount; row++) {
          if (stringData[row] != null && stringData[row].equals(value)) {
            outgoing[count++] = row;
          }
        }
      }
    } else {  // less efficient, includes conversion as necessary
      for (int row = 0; row < rowCount; row++) {
        String str = getString(row, column);
        if (str == null) {
          if (value == null) {
            outgoing[count++] = row;
          }
        } else if (str.equals(value)) {
          outgoing[count++] = row;
        }
      }
    }
    return PApplet.subset(outgoing, 0, count);
  }


  /**
   * Return a list of rows that contain the String passed in. If there are no
   * matches, a zero length array will be returned (not a null array).
   * @param value the String to match
   * @param columnName title of the column to search
   */
  public int[] findRowIndices(String value, String columnName) {
    return findRowIndices(value, getColumnIndex(columnName));
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  /**
   * @webref table:method
   * @brief Finds a row that contains the given value
   * @param value the value to match
   * @param column ID number of the column to search
   * @see Table#getRow(int)
   * @see Table#rows()
   * @see Table#findRows(String, int)
   * @see Table#matchRow(String, int)
   * @see Table#matchRows(String, int)
   */
  public TableRow findRow(String value, int column) {
    int row = findRowIndex(value, column);
    return (row == -1) ? null : new RowPointer(this, row);
  }


  /**
   * @param columnName title of the column to search
   */
  public TableRow findRow(String value, String columnName) {
    return findRow(value, getColumnIndex(columnName));
  }


  /**
   * @webref table:method
   * @brief Finds multiple rows that contain the given value
   * @param value the value to match
   * @param column ID number of the column to search
   * @see Table#getRow(int)
   * @see Table#rows()
   * @see Table#findRow(String, int)
   * @see Table#matchRow(String, int)
   * @see Table#matchRows(String, int)
   */
  public Iterable<TableRow> findRows(final String value, final int column) {
    return new Iterable<TableRow>() {
      public Iterator<TableRow> iterator() {
        return findRowIterator(value, column);
      }
    };
  }


  /**
   * @param columnName title of the column to search
   */
  public Iterable<TableRow> findRows(final String value, final String columnName) {
    return findRows(value, getColumnIndex(columnName));
  }


  /**
   * @brief Finds multiple rows that contain the given value
   * @param value the value to match
   * @param column ID number of the column to search
   */
  public Iterator<TableRow> findRowIterator(String value, int column) {
    return new RowIndexIterator(this, findRowIndices(value, column));
  }


  /**
   * @param columnName title of the column to search
   */
  public Iterator<TableRow> findRowIterator(String value, String columnName) {
    return findRowIterator(value, getColumnIndex(columnName));
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Return the row that contains the first String that matches.
   * @param regexp the String to match
   * @param column ID number of the column to search
   */
  public int matchRowIndex(String regexp, int column) {
    checkColumn(column);
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
   * @param columnName title of the column to search
   */
  public int matchRowIndex(String what, String columnName) {
    return matchRowIndex(what, getColumnIndex(columnName));
  }


  /**
   * Return a list of rows that contain the String passed in. If there are no
   * matches, a zero length array will be returned (not a null array).
   * @param regexp the String to match
   * @param column ID number of the column to search
   */
  public int[] matchRowIndices(String regexp, int column) {
    int[] outgoing = new int[rowCount];
    int count = 0;

    checkColumn(column);
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
   * @param columnName title of the column to search
   */
  public int[] matchRowIndices(String what, String columnName) {
    return matchRowIndices(what, getColumnIndex(columnName));
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  /**
   * @webref table:method
   * @brief Finds a row that matches the given expression
   * @param regexp the regular expression to match
   * @param column ID number of the column to search
   * @see Table#getRow(int)
   * @see Table#rows()
   * @see Table#findRow(String, int)
   * @see Table#findRows(String, int)
   * @see Table#matchRows(String, int)
   */
  public TableRow matchRow(String regexp, int column) {
    int row = matchRowIndex(regexp, column);
    return (row == -1) ? null : new RowPointer(this, row);
  }


  /**
   * @param columnName title of the column to search
   */
  public TableRow matchRow(String regexp, String columnName) {
    return matchRow(regexp, getColumnIndex(columnName));
  }


  /**
   * @webref table:method
   * @brief Finds multiple rows that match the given expression
   * @param regexp the regular expression to match
   * @param column ID number of the column to search
   * @see Table#getRow(int)
   * @see Table#rows()
   * @see Table#findRow(String, int)
   * @see Table#findRows(String, int)
   * @see Table#matchRow(String, int)
   */
  public Iterable<TableRow> matchRows(final String regexp, final int column) {
    return new Iterable<TableRow>() {
      public Iterator<TableRow> iterator() {
        return matchRowIterator(regexp, column);
      }
    };
  }


  /**
   * @param columnName title of the column to search
   */
  public Iterable<TableRow> matchRows(String regexp, String columnName) {
    return matchRows(regexp, getColumnIndex(columnName));
  }


  /**
   * @webref table:method
   * @brief Finds multiple rows that match the given expression
   * @param value the regular expression to match
   * @param column ID number of the column to search
   */
  public Iterator<TableRow> matchRowIterator(String value, int column) {
    return new RowIndexIterator(this, matchRowIndices(value, column));
  }


  /**
   * @param columnName title of the column to search
   */
  public Iterator<TableRow> matchRowIterator(String value, String columnName) {
    return matchRowIterator(value, getColumnIndex(columnName));
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Replace a String with another. Set empty entries null by using
   * replace("", null) or use replace(null, "") to go the other direction.
   * If this is a typed table, only String columns will be modified.
   * @param orig
   * @param replacement
   */
  public void replace(String orig, String replacement) {
    for (int col = 0; col < columns.length; col++) {
      replace(orig, replacement, col);
    }
  }


  public void replace(String orig, String replacement, int col) {
    if (columnTypes[col] == STRING) {
      String[] stringData = (String[]) columns[col];

      if (orig != null) {
        for (int row = 0; row < rowCount; row++) {
          if (orig.equals(stringData[row])) {
            stringData[row] = replacement;
          }
        }
      } else {  // null is a special case (and faster anyway)
        for (int row = 0; row < rowCount; row++) {
          if (stringData[row] == null) {
            stringData[row] = replacement;
          }
        }
      }
    }
  }


  public void replace(String orig, String replacement, String colName) {
    replace(orig, replacement, getColumnIndex(colName));
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public void replaceAll(String regex, String replacement) {
    for (int col = 0; col < columns.length; col++) {
      replaceAll(regex, replacement, col);
    }
  }


  public void replaceAll(String regex, String replacement, int column) {
    checkColumn(column);
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
   * @param regex the String to match
   * @param columnName title of the column to search
   */
  public void replaceAll(String regex, String replacement, String columnName) {
    replaceAll(regex, replacement, getColumnIndex(columnName));
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Remove any of the specified characters from the entire table.
   *
   * @webref table:method
   * @brief Removes characters from the table
   * @param tokens a list of individual characters to be removed
   * @see Table#trim()
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
   *
   * @param column ID number of the column to process
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

  /**
   * @param columnName title of the column to process
   */
  public void removeTokens(String tokens, String columnName) {
    removeTokens(tokens, getColumnIndex(columnName));
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  /**
   * @webref table:method
   * @brief Trims whitespace from values
   * @see Table#removeTokens(String)
   */
  public void trim() {
    columnTitles = PApplet.trim(columnTitles);
    for (int col = 0; col < getColumnCount(); col++) {
      trim(col);
    }
  }

  /**
   * @param column ID number of the column to trim
   */
  public void trim(int column) {
    if (columnTypes[column] == STRING) {
      String[] stringData = (String[]) columns[column];
      for (int row = 0; row < rowCount; row++) {
        if (stringData[row] != null) {
          stringData[row] = PApplet.trim(stringData[row]);
        }
      }
    }
  }

  /**
   * @param columnName title of the column to trim
   */
  public void trim(String columnName) {
    trim(getColumnIndex(columnName));
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /** Make sure this is a legit column, and if not, expand the table. */
  protected void ensureColumn(int col) {
    if (col >= columns.length) {
      setColumnCount(col + 1);
    }
  }


  /** Make sure this is a legit row, and if not, expand the table. */
  protected void ensureRow(int row) {
    if (row >= rowCount) {
      setRowCount(row + 1);
    }
  }


  /** Make sure this is a legit row and column. If not, expand the table. */
  protected void ensureBounds(int row, int col) {
    ensureRow(row);
    ensureColumn(col);
  }


  /** Throw an error if this row doesn't exist. */
  protected void checkRow(int row) {
    if (row < 0 || row >= rowCount) {
      throw new ArrayIndexOutOfBoundsException("Row " + row + " does not exist.");
    }
  }


  /** Throw an error if this column doesn't exist. */
  protected void checkColumn(int column) {
    if (column < 0 || column >= columns.length) {
      throw new ArrayIndexOutOfBoundsException("Column " + column + " does not exist.");
    }
  }


  /** Throw an error if this entry is out of bounds. */
  protected void checkBounds(int row, int column) {
    checkRow(row);
    checkColumn(column);
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  static class HashMapBlows {
    HashMap<String,Integer> dataToIndex = new HashMap<String, Integer>();
    ArrayList<String> indexToData = new ArrayList<String>();

    HashMapBlows() { }

    HashMapBlows(DataInputStream input) throws IOException {
      read(input);
    }

    /** gets the index, and creates one if it doesn't already exist. */
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

    boolean hasCategory(int index) {
      return index < size() && indexToData.get(index) != null;
    }

    void setCategory(int index, String name) {
      while (indexToData.size() <= index) {
        indexToData.add(null);
      }
      indexToData.set(index, name);
      dataToIndex.put(name, index);
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

    private void writeln(PrintWriter writer) throws IOException {
      for (String str : indexToData) {
        writer.println(str);
      }
      writer.flush();
      writer.close();
    }

    void read(DataInputStream input) throws IOException {
      int count = input.readInt();
      //System.out.println("found " + count + " entries in category map");
      dataToIndex = new HashMap<String, Integer>(count);
      for (int i = 0; i < count; i++) {
        String str = input.readUTF();
        //System.out.println(i + " " + str);
        dataToIndex.put(str, i);
        indexToData.add(str);
      }
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


//  class HashMapSucks extends HashMap<String,Integer> {
//
//    void increment(String what) {
//      Integer value = get(what);
//      if (value == null) {
//        put(what, 1);
//      } else {
//        put(what, value + 1);
//      }
//    }
//
//    void check(String what) {
//      if (get(what) == null) {
//        put(what, 0);
//      }
//    }
//  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  /**
   * Sorts (orders) a table based on the values in a column.
   *
   * @webref table:method
   * @brief Orders a table based on the values in a column
   * @param columnName the name of the column to sort
   * @see Table#trim()
   */
  public void sort(String columnName) {
    sort(getColumnIndex(columnName), false);
  }

  /**
   * @param column the column ID, e.g. 0, 1, 2
   */
  public void sort(int column) {
    sort(column, false);
  }


  public void sortReverse(String columnName) {
    sort(getColumnIndex(columnName), true);
  }


  public void sortReverse(int column) {
    sort(column, true);
  }


  protected void sort(final int column, final boolean reverse) {
    final int[] order = IntList.fromRange(getRowCount()).array();
    Sort s = new Sort() {

      @Override
      public int size() {
        return getRowCount();
      }

      @Override
      public float compare(int index1, int index2) {
        int a = reverse ? order[index2] : order[index1];
        int b = reverse ? order[index1] : order[index2];

        switch (getColumnType(column)) {
        case INT:
          return getInt(a, column) - getInt(b, column);
        case LONG:
          return getLong(a, column) - getLong(b, column);
        case FLOAT:
          return getFloat(a, column) - getFloat(b, column);
        case DOUBLE:
          return (float) (getDouble(a, column) - getDouble(b, column));
        case STRING:
          return getString(a, column).compareToIgnoreCase(getString(b, column));
        case CATEGORY:
          return getInt(a, column) - getInt(b, column);
        default:
          throw new IllegalArgumentException("Invalid column type: " + getColumnType(column));
        }
      }

      @Override
      public void swap(int a, int b) {
        int temp = order[a];
        order[a] = order[b];
        order[b] = temp;
      }

    };
    s.run();

    //Object[] newColumns = new Object[getColumnCount()];
    for (int col = 0; col < getColumnCount(); col++) {
      switch (getColumnType(col)) {
      case INT:
      case CATEGORY:
        int[] oldInt = (int[]) columns[col];
        int[] newInt = new int[rowCount];
        for (int row = 0; row < getRowCount(); row++) {
          newInt[row] = oldInt[order[row]];
        }
        columns[col] = newInt;
        break;
      case LONG:
        long[] oldLong = (long[]) columns[col];
        long[] newLong = new long[rowCount];
        for (int row = 0; row < getRowCount(); row++) {
          newLong[row] = oldLong[order[row]];
        }
        columns[col] = newLong;
        break;
      case FLOAT:
        float[] oldFloat = (float[]) columns[col];
        float[] newFloat = new float[rowCount];
        for (int row = 0; row < getRowCount(); row++) {
          newFloat[row] = oldFloat[order[row]];
        }
        columns[col] = newFloat;
        break;
      case DOUBLE:
        double[] oldDouble = (double[]) columns[col];
        double[] newDouble = new double[rowCount];
        for (int row = 0; row < getRowCount(); row++) {
          newDouble[row] = oldDouble[order[row]];
        }
        columns[col] = newDouble;
        break;
      case STRING:
        String[] oldString = (String[]) columns[col];
        String[] newString = new String[rowCount];
        for (int row = 0; row < getRowCount(); row++) {
          newString[row] = oldString[order[row]];
        }
        columns[col] = newString;
        break;
      }
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public String[] getUnique(String columnName) {
    return getUnique(getColumnIndex(columnName));
  }


  public String[] getUnique(int column) {
    StringList list = new StringList(getStringColumn(column));
    return list.getUnique();
  }


  public IntDict getTally(String columnName) {
    return getTally(getColumnIndex(columnName));
  }


  public IntDict getTally(int column) {
    StringList list = new StringList(getStringColumn(column));
    return list.getTally();
  }


  public IntDict getOrder(String columnName) {
    return getOrder(getColumnIndex(columnName));
  }


  public IntDict getOrder(int column) {
    StringList list = new StringList(getStringColumn(column));
    return list.getOrder();
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public IntList getIntList(String columnName) {
    return new IntList(getIntColumn(columnName));
  }


  public IntList getIntList(int column) {
    return new IntList(getIntColumn(column));
  }


  public FloatList getFloatList(String columnName) {
    return new FloatList(getFloatColumn(columnName));
  }


  public FloatList getFloatList(int column) {
    return new FloatList(getFloatColumn(column));
  }


  public StringList getStringList(String columnName) {
    return new StringList(getStringColumn(columnName));
  }


  public StringList getStringList(int column) {
    return new StringList(getStringColumn(column));
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public IntDict getIntDict(String keyColumnName, String valueColumnName) {
    return new IntDict(getStringColumn(keyColumnName),
                       getIntColumn(valueColumnName));
  }


  public IntDict getIntDict(int keyColumn, int valueColumn) {
    return new IntDict(getStringColumn(keyColumn),
                       getIntColumn(valueColumn));
  }


  public FloatDict getFloatDict(String keyColumnName, String valueColumnName) {
    return new FloatDict(getStringColumn(keyColumnName),
                         getFloatColumn(valueColumnName));
  }


  public FloatDict getFloatDict(int keyColumn, int valueColumn) {
    return new FloatDict(getStringColumn(keyColumn),
                         getFloatColumn(valueColumn));
  }


  public StringDict getStringDict(String keyColumnName, String valueColumnName) {
    return new StringDict(getStringColumn(keyColumnName),
                          getStringColumn(valueColumnName));
  }


  public StringDict getStringDict(int keyColumn, int valueColumn) {
    return new StringDict(getStringColumn(keyColumn),
                          getStringColumn(valueColumn));
  }


  public Map<String, TableRow> getRowMap(String columnName) {
    int col = getColumnIndex(columnName);
    return (col == -1) ? null : getRowMap(col);
  }


  /**
   * Return a mapping that connects the entry from a column back to the row
   * from which it came. For instance:
   * <pre>
   * Table t = loadTable("country-data.tsv", "header");
   * // use the contents of the 'country' column to index the table
   * Map<String, TableRow> lookup = t.getRowMap("country");
   * // get the row that has "us" in the "country" column:
   * TableRow usRow = lookup.get("us");
   * // get an entry from the 'population' column
   * int population = usRow.getInt("population");
   * </pre>
   */
  public Map<String, TableRow> getRowMap(int column) {
    Map<String, TableRow> outgoing = new HashMap<>();
    for (int row = 0; row < getRowCount(); row++) {
      String id = getString(row, column);
      outgoing.put(id, new RowPointer(this, row));
    }
//    for (TableRow row : rows()) {
//      String id = row.getString(column);
//      outgoing.put(id, row);
//    }
    return outgoing;
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


//  /**
//   * Return an object that maps the String values in one column back to the
//   * row from which they came. For instance, if the "name" of each row is
//   * found in the first column, getColumnRowLookup(0) would return an object
//   * that would map each name back to its row.
//   */
//  protected HashMap<String,Integer> getRowLookup(int col) {
//    HashMap<String,Integer> outgoing = new HashMap<String, Integer>();
//    for (int row = 0; row < getRowCount(); row++) {
//      outgoing.put(getString(row, col), row);
//    }
//    return outgoing;
//  }


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


  // TODO naming/whether to include
  protected Table createSubset(int[] rowSubset) {
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


  /**
   * Searches the entire table for float values.
   * Returns missing float (Float.NaN by default) if no valid numbers found.
   */
  protected float getMaxFloat() {
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


  // converts a TSV or CSV file to binary.. do not use
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
      convertRow(output, tsv ? PApplet.split(line, '\t') : splitLineCSV(line, reader));
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


  protected void convertRow(DataOutputStream output, String[] pieces) throws IOException {
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
      case CATEGORY:
        String peace = pieces[col];
        if (peace.equals(missingString)) {
          output.writeInt(missingCategory);
        } else {
          output.writeInt(columnCategories[col].index(peace));
        }
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
      case CATEGORY:
        output.writeInt(missingCategory);
        break;

      }
    }
  }


  /*
  private void convertRowCol(DataOutputStream output, int row, int col, String piece) {
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
  */


  /** Make a copy of the current table */
  public Table copy() {
    return new Table(rows());
  }


  public void write(PrintWriter writer) {
    writeTSV(writer);
  }


  public void print() {
    writeTSV(new PrintWriter(System.out));
  }
}

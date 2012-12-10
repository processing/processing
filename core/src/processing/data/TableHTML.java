package processing.data;

import java.io.*;
import java.util.*;

import javax.swing.text.*;
import javax.swing.text.html.*;

import processing.core.PApplet;
import processing.data.Table;


class HTMLTableScraper {
  ArrayList<Table> tables;
  TableHTML currentTable;


  public HTMLTableScraper(PApplet parent, String where) {
    this(parent.createReader(where));
  }


  public HTMLTableScraper(File file) {
    this(PApplet.createReader(file));
  }


  public HTMLTableScraper(String html) {
    this(new StringReader(html));
  }


  public HTMLTableScraper(Reader reader) {
    tables = new ArrayList<Table>();
    TableHandler handler = new TableHandler();
    parse(reader, handler);
  }


  // The actual class doing some of the work:
  // javax.swing.text.html.parser.ParserDelegator pd;

  void parse(Reader reader, HTMLEditorKit.ParserCallback handler) {
    HTMLEditorKit.Parser parser = new HTMLEditorKit() {
      @Override
      public HTMLEditorKit.Parser getParser() {
        return super.getParser();
      }
    }.getParser();
    try {
      parser.parse(reader, handler, true);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  public int getTableCount() {
    return tables.size();
  }


  public Table getTable(int index) {
    return tables.get(index);
  }


  /**
   * Get the list of tables as an array of Table objects.
   */
  public Table[] getTables() {
    TableHTML[] outgoing = new TableHTML[tables.size()];
    tables.toArray(outgoing);
    return outgoing;
  }


  /**
   * Get the list of tables as an ArrayList of Table objects.
   */
  public ArrayList<Table> getTableList() {
    return tables;
  }


  public void writeTables(PApplet parent, String prefix) {
    int digits = 0;
    int num = getTableCount();
    while (num > 0) {
      num /= 10;
      digits++;
    }
    for (int i = 0; i < getTableCount(); i++) {
      String name = prefix + PApplet.nf(i, digits);
      //tables.get(i).writeCSV(parent.createWriter(name + ".csv"));
      parent.saveTable(tables.get(i), name + ".csv");
    }
  }


  ////////////////////////////////////////////////////////////////////////////////


  class TableHandler extends HTMLEditorKit.ParserCallback {

    @Override
    public void handleStartTag(HTML.Tag tag, MutableAttributeSet a, int pos) {
      if (tag == HTML.Tag.TABLE) {
        currentTable = new TableHTML(currentTable);
        tables.add(currentTable);

      } else if (currentTable != null) {
        if (tag == HTML.Tag.TR) {
          currentTable.beginTableRow();

        } else if (tag == HTML.Tag.TD || tag == HTML.Tag.TH) {
          int advance = 1;
          String colspanStr = (String) a.getAttribute(HTML.Attribute.COLSPAN);
          if (colspanStr != null) {
            advance = Integer.parseInt(colspanStr);
          }
          currentTable.beginTableData(advance);

          if (a.getAttribute(HTML.Attribute.ROWSPAN) != null) {
            System.err.println("rowspan attribute in this table is being ignored");
          }
        }
      }
    }


    @Override
    public void handleText(char[] c,int pos) {
      if (currentTable != null) {
        String data = new String(c).trim();
        currentTable.setContent(data);
      }
    }


    @Override
    public void handleEndTag(HTML.Tag tag, int pos) {
      if (currentTable != null) {
        if (tag == HTML.Tag.TABLE) {
          currentTable = currentTable.parent;

        } else if (tag == HTML.Tag.TR) {
          currentTable.endTableRow();

        } else if (tag == HTML.Tag.TD || tag == HTML.Tag.TH) {
          currentTable.endTableData();
        }
      }
    }
  }
}


////////////////////////////////////////////////////////////////////////////////


public class TableHTML extends Table {
  // used during parse to capture state
  TableHTML parent;
  int colAdvance;
  int rowIndex, colIndex;
  int colCount;

  TableHTML(TableHTML parent) {
    super();
    this.parent = parent;
  }

  void beginTableRow() {
    // make sure we have enough room for these rows
    addRow();
//    if (rowCount == data.length) {
//      String[][] temp = new String[data.length << 1][];
//      System.arraycopy(data, 0, temp, 0, rowCount);
//      data = temp;
//      for (int j = rowCount; j < data.length; j++) {
//        data[j] = new String[data[0].length];
//      }
//    }
  }

  void beginTableData(int advance) {
    colAdvance = advance;
    // expand the number of columns if necessary
    checkColumn(colIndex + colAdvance - 1);
//    if (colIndex + colAdvance > data[0].length) {
//      int needed = (colIndex + colAdvance) * 2;
//      for (int i = 0; i < data.length; i++) {
//        String[] temp = new String[needed];
//        System.arraycopy(data[i], 0, temp, 0, colCount);
//        data[i] = temp;
//      }
//    }
  }


  void setContent(String what) {
    //data[rowIndex][colIndex] = what;
    String cell = getString(rowIndex, colIndex);
    if (cell != null) {
      cell += what;
    } else {
      cell = what;
    }
//    setString(rowIndex, colIndex, what);
    setString(rowIndex, colIndex, cell);
  }


  void endTableData() {
    colIndex += colAdvance;
    colCount = Math.max(colIndex, colCount);
  }


  void endTableRow() {
    rowIndex++;
    rowCount = rowIndex;
    colIndex = 0;
  }
}

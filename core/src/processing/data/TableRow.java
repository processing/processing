package processing.data;

import java.io.PrintWriter;

/**
 * @webref data:composite
 * @see Table
 * @see Table#addRow()
 * @see Table#removeRow(int)
 * @see Table#clearRows()
 * @see Table#getRow(int)
 * @see Table#rows()
 */
public interface TableRow {

  /**
   * @webref tablerow:method
   * @brief Get an String value from the specified column
   * @param column ID number of the column to reference
   * @see TableRow#getInt(int)
   * @see TableRow#getFloat(int)
   */
  String getString(int column);

  /**
   * @param columnName title of the column to reference
   */
  String getString(String columnName);

  /**
   * @webref tablerow:method
   * @brief Get an integer value from the specified column
   * @param column ID number of the column to reference
   * @see TableRow#getFloat(int)
   * @see TableRow#getString(int)
   */
  int getInt(int column);

  /**
   * @param columnName title of the column to reference
   */
  int getInt(String columnName);

  /**
   * @brief Get a long value from the specified column
   * @param column ID number of the column to reference
   * @see TableRow#getFloat(int)
   * @see TableRow#getString(int)
   */

  long getLong(int column);

  /**
   * @param columnName title of the column to reference
   */
  long getLong(String columnName);

  /**
   * @webref tablerow:method
   * @brief Get a float value from the specified column
   * @param column ID number of the column to reference
   * @see TableRow#getInt(int)
   * @see TableRow#getString(int)
   */
  float getFloat(int column);

  /**
   * @param columnName title of the column to reference
   */
  float getFloat(String columnName);
  
  /**
   * @brief Get a double value from the specified column
   * @param column ID number of the column to reference
   * @see TableRow#getInt(int)
   * @see TableRow#getString(int)
   */
  double getDouble(int column);
  
  /**
   * @param columnName title of the column to reference
   */
  double getDouble(String columnName);

  /**
   * @webref tablerow:method
   * @brief Store a String value in the specified column
   * @param column ID number of the target column
   * @param value value to assign
   * @see TableRow#setInt(int, int)
   * @see TableRow#setFloat(int, float)
   */
  void setString(int column, String value);
  /**
   * @param columnName title of the target column
   */
  void setString(String columnName, String value);

  /**
   * @webref tablerow:method
   * @brief Store an integer value in the specified column
   * @param column ID number of the target column
   * @param value value to assign
   * @see TableRow#setFloat(int, float)
   * @see TableRow#setString(int, String)
   */
  void setInt(int column, int value);

  /**
   * @param columnName title of the target column
   */
  void setInt(String columnName, int value);
  
  /**
   * @brief Store a long value in the specified column
   * @param column ID number of the target column
   * @param value value to assign
   * @see TableRow#setFloat(int, float)
   * @see TableRow#setString(int, String)
   */
  void setLong(int column, long value);
  
  /**
   * @param columnName title of the target column
   */
  void setLong(String columnName, long value);

  /**
   * @webref tablerow:method
   * @brief Store a float value in the specified column
   * @param column ID number of the target column
   * @param value value to assign
   * @see TableRow#setInt(int, int)
   * @see TableRow#setString(int, String)
   */
  void setFloat(int column, float value);
  
  /**
   * @param columnName title of the target column
   */
  void setFloat(String columnName, float value);

  /**
   * @brief Store a double value in the specified column
   * @param column ID number of the target column
   * @param value value to assign
   * @see TableRow#setFloat(int, float)
   * @see TableRow#setString(int, String)
   */
  void setDouble(int column, double value);
  
  /**
   * @param columnName title of the target column
   */
  void setDouble(String columnName, double value);

  /**
   * @webref tablerow:method
   * @brief Get the column count.
   * @return count of all columns
   */
  int getColumnCount();
  
  /**
   * @brief Get the column type.
   * @param columnName title of the target column
   * @return type of the column
   */
  int getColumnType(String columnName);
  
  /**
   * @param column ID number of the target column
   */
  int getColumnType(int column);
  
  /**
   * @brief Get the all column types
   * @return list of all column types
   */
  int[] getColumnTypes();

  /**
   * @webref tablerow:method
   * @brief Get the column title.
   * @param column ID number of the target column
   * @return title of the column
   */
  String getColumnTitle(int column);

  /**
   * @brief Get the all column titles
   * @return list of all column titles
   */
  String[] getColumnTitles();

  void write(PrintWriter writer);
  void print();
}

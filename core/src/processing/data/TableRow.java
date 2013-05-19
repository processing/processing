package processing.data;

/**
 * @webref data:composite
 */
public interface TableRow {

  /**
   * @webref tablerow:method
   * @brief Get the String value from a column
   */
  public String getString(int column);
  public String getString(String columnName);

  /**
   * @webref tablerow:method
   * @brief Get the int value from a column
   */
  public int getInt(int column);
  public int getInt(String columnName);
  
  public long getLong(int column);
  public long getLong(String columnName);

  /**
   * @webref tablerow:method
   * @brief Get the float value from a column
   */
  public float getFloat(int column);
  public float getFloat(String columnName);
  
  public double getDouble(int column);
  public double getDouble(String columnName);

  /**
   * @webref tablerow:method
   * @brief Set the String value in a column
   */
  public void setString(int column, String value);
  public void setString(String columnName, String value);

  /**
   * @webref tablerow:method
   * @brief Set the int value in a column
   */
  public void setInt(int column, int value);
  public void setInt(String columnName, int value);

  public void setLong(int column, long value);
  public void setLong(String columnName, long value);

  /**
   * @webref tablerow:method
   * @brief Set the float value in a column
   */
  public void setFloat(int column, float value);
  public void setFloat(String columnName, float value);

  public void setDouble(int column, double value);
  public void setDouble(String columnName, double value);

  public int getColumnCount();
  public int getColumnType(String columnName);
  public int getColumnType(int column);
}

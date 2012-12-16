package processing.data;


public interface TableRow {
  public String getString(int column);
  public String getString(String columnName);
  public int getInt(int column);
  public int getInt(String columnName);
  public long getLong(int column);
  public long getLong(String columnName);
  public float getFloat(int column);
  public float getFloat(String columnName);
  public double getDouble(int column);
  public double getDouble(String columnName);

  public void setString(int column, String value);
  public void setString(String columnName, String value);
  public void setInt(int column, int value);
  public void setInt(String columnName, int value);
  public void setLong(int column, long value);
  public void setLong(String columnName, long value);
  public void setFloat(int column, float value);
  public void setFloat(String columnName, float value);
  public void setDouble(int column, double value);
  public void setDouble(String columnName, double value);
}

/*

Copyright (c) 2002 JSON.org

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

The Software shall be used for Good, not Evil.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

package processing.data;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * A JSONArray is an ordered sequence of values. Its external text form is a
 * string wrapped in square brackets with commas separating the values. The
 * internal form is an object having <code>get</code> and <code>opt</code>
 * methods for accessing the values by index, and <code>put</code> methods for
 * adding or replacing values. The values can be any of these types:
 * <code>Boolean</code>, <code>JSONArray</code>, <code>JSONObject</code>,
 * <code>Number</code>, <code>String</code>, or the
 * <code>JSONObject.NULL object</code>.
 * <p>
 * The constructor can convert a JSON text into a Java object. The
 * <code>toString</code> method converts to JSON text.
 * <p>
 * A <code>get</code> method returns a value if one can be found, and throws an
 * exception if one cannot be found. An <code>opt</code> method returns a
 * default value instead of throwing an exception, and so is useful for
 * obtaining optional values.
 * <p>
 * The generic <code>get()</code> and <code>opt()</code> methods return an
 * object which you can cast or query for type. There are also typed
 * <code>get</code> and <code>opt</code> methods that do type checking and type
 * coercion for you.
 * <p>
 * The texts produced by the <code>toString</code> methods strictly conform to
 * JSON syntax rules. The constructors are more forgiving in the texts they will
 * accept:
 * <ul>
 * <li>An extra <code>,</code>&nbsp;<small>(comma)</small> may appear just
 * before the closing bracket.</li>
 * <li>The <code>null</code> value will be inserted when there is <code>,</code>
 * &nbsp;<small>(comma)</small> elision.</li>
 * <li>Strings may be quoted with <code>'</code>&nbsp;<small>(single
 * quote)</small>.</li>
 * <li>Strings do not need to be quoted at all if they do not begin with a quote
 * or single quote, and if they do not contain leading or trailing spaces, and
 * if they do not contain any of these characters:
 * <code>{ } [ ] / \ : , = ; #</code> and if they do not look like numbers and
 * if they are not the reserved words <code>true</code>, <code>false</code>, or
 * <code>null</code>.</li>
 * <li>Values can be separated by <code>;</code> <small>(semicolon)</small> as
 * well as by <code>,</code> <small>(comma)</small>.</li>
 * </ul>
 *
 * @author JSON.org
 * @version 2012-11-13
 */
public class JSONArray {


  /**
   * The arrayList where the JSONArray's properties are kept.
   */
  private final ArrayList<Object> myArrayList;


  /**
   * Construct an empty JSONArray.
   */
  public JSONArray() {
    this.myArrayList = new ArrayList<Object>();
  }

  /**
   * Construct a JSONArray from a JSONTokener.
   * @param x A JSONTokener
   * @throws JSONException If there is a syntax error.
   */
  private JSONArray(JSONTokener x) {
    this();
    if (x.nextClean() != '[') {
      throw new RuntimeException("A JSONArray text must start with '['");
    }
    if (x.nextClean() != ']') {
      x.back();
      for (;;) {
        if (x.nextClean() == ',') {
          x.back();
          myArrayList.add(JSONObject.NULL);
        } else {
          x.back();
          myArrayList.add(x.nextValue());
        }
        switch (x.nextClean()) {
        case ';':
        case ',':
          if (x.nextClean() == ']') {
            return;
          }
          x.back();
          break;
        case ']':
          return;
        default:
          throw new RuntimeException("Expected a ',' or ']'");
        }
      }
    }
  }


  /**
   * Construct a JSONArray from a source JSON text.
   * @param source     A string that begins with
   * <code>[</code>&nbsp;<small>(left bracket)</small>
   *  and ends with <code>]</code>&nbsp;<small>(right bracket)</small>.
   *  @throws JSONException If there is a syntax error.
   */
  public JSONArray(String source) {
    this(new JSONTokener(source));
  }


  /**
   * Construct a JSONArray from a Collection.
   * @param collection     A Collection.
   */
  public JSONArray(Collection collection) {
    myArrayList = new ArrayList<Object>();
    if (collection != null) {
      Iterator iter = collection.iterator();
      while (iter.hasNext()) {
        myArrayList.add(JSONObject.wrap(iter.next()));
      }
    }
  }


  /**
   * Construct a JSONArray from an array
   * @throws JSONException If not an array.
   */
  public JSONArray(Object array) {
    this();
    if (array.getClass().isArray()) {
      int length = Array.getLength(array);
      for (int i = 0; i < length; i += 1) {
        this.append(JSONObject.wrap(Array.get(array, i)));
      }
    } else {
      throw new RuntimeException("JSONArray initial value should be a string or collection or array.");
    }
  }


  /**
   * Get the optional object value associated with an index.
   * @param index The index must be between 0 and length() - 1.
   * @return      An object value, or null if there is no
   *              object at that index.
   */
  private Object opt(int index) {
    if (index < 0 || index >= this.size()) {
      return null;
    }
    return myArrayList.get(index);
  }


  /**
   * Get the object value associated with an index.
   * @param index The index must be between 0 and length() - 1.
   * @return An object value.
   * @throws JSONException If there is no value for the index.
   */
  private Object get(int index) {
    Object object = opt(index);
    if (object == null) {
      throw new RuntimeException("JSONArray[" + index + "] not found.");
    }
    return object;
  }


  /**
   * Get the string associated with an index.
   * @param index The index must be between 0 and length() - 1.
   * @return      A string value.
   * @throws JSONException If there is no string value for the index.
   */
  public String getString(int index) {
    Object object = this.get(index);
    if (object instanceof String) {
      return (String)object;
    }
    throw new RuntimeException("JSONArray[" + index + "] not a string.");
  }


  /**
   * Get the int value associated with an index.
   *
   * @param index The index must be between 0 and length() - 1.
   * @return      The value.
   * @throws   JSONException If the key is not found or if the value is not a number.
   */
  public int getInt(int index) {
    Object object = this.get(index);
    try {
      return object instanceof Number
        ? ((Number)object).intValue()
          : Integer.parseInt((String)object);
    } catch (Exception e) {
      throw new RuntimeException("JSONArray[" + index + "] is not a number.");
    }
  }


  /**
   * Get the long value associated with an index.
   *
   * @param index The index must be between 0 and length() - 1.
   * @return      The value.
   * @throws   JSONException If the key is not found or if the value cannot
   *  be converted to a number.
   */
  public long getLong(int index) {
    Object object = this.get(index);
    try {
      return object instanceof Number
        ? ((Number)object).longValue()
          : Long.parseLong((String)object);
    } catch (Exception e) {
      throw new RuntimeException("JSONArray[" + index + "] is not a number.");
    }
  }


  /**
   * Get a value from an index as a float. JSON uses 'double' values
   * internally, so this is simply getDouble() cast to a float.
   */
  public float getFloat(int index) {
    return (float) getDouble(index);
  }


  /**
   * Get the double value associated with an index.
   *
   * @param index The index must be between 0 and length() - 1.
   * @return      The value.
   * @throws   JSONException If the key is not found or if the value cannot
   *  be converted to a number.
   */
  public double getDouble(int index) {
    Object object = this.get(index);
    try {
      return object instanceof Number
        ? ((Number)object).doubleValue()
          : Double.parseDouble((String)object);
    } catch (Exception e) {
      throw new RuntimeException("JSONArray[" + index + "] is not a number.");
    }
  }


  /**
   * Get the boolean value associated with an index.
   * The string values "true" and "false" are converted to boolean.
   *
   * @param index The index must be between 0 and length() - 1.
   * @return      The truth.
   * @throws JSONException If there is no value for the index or if the
   *  value is not convertible to boolean.
   */
  public boolean getBoolean(int index) {
    Object object = this.get(index);
    if (object.equals(Boolean.FALSE) ||
      (object instanceof String &&
        ((String)object).equalsIgnoreCase("false"))) {
      return false;
    } else if (object.equals(Boolean.TRUE) ||
      (object instanceof String &&
        ((String)object).equalsIgnoreCase("true"))) {
      return true;
    }
    throw new RuntimeException("JSONArray[" + index + "] is not a boolean.");
  }


  /**
   * Get the JSONArray associated with an index.
   * @param index The index must be between 0 and length() - 1.
   * @return      A JSONArray value.
   * @throws JSONException If there is no value for the index. or if the
   * value is not a JSONArray
   */
  public JSONArray getArray(int index) {
    Object object = this.get(index);
    if (object instanceof JSONArray) {
      return (JSONArray)object;
    }
    throw new RuntimeException("JSONArray[" + index + "] is not a JSONArray.");
  }


  /**
   * Get the JSONObject associated with an index.
   * @param index subscript
   * @return      A JSONObject value.
   * @throws JSONException If there is no value for the index or if the
   * value is not a JSONObject
   */
  public JSONObject getObject(int index) {
    Object object = this.get(index);
    if (object instanceof JSONObject) {
      return (JSONObject)object;
    }
    throw new RuntimeException("JSONArray[" + index + "] is not a JSONObject.");
  }


//  /**
//   * Get the optional boolean value associated with an index.
//   * It returns false if there is no value at that index,
//   * or if the value is not Boolean.TRUE or the String "true".
//   *
//   * @param index The index must be between 0 and length() - 1.
//   * @return      The truth.
//   */
//  public boolean optBoolean(int index)  {
//    return this.optBoolean(index, false);
//  }
//
//
//  /**
//   * Get the optional boolean value associated with an index.
//   * It returns the defaultValue if there is no value at that index or if
//   * it is not a Boolean or the String "true" or "false" (case insensitive).
//   *
//   * @param index The index must be between 0 and length() - 1.
//   * @param defaultValue     A boolean default.
//   * @return      The truth.
//   */
//  public boolean optBoolean(int index, boolean defaultValue)  {
//    try {
//      return this.getBoolean(index);
//    } catch (Exception e) {
//      return defaultValue;
//    }
//  }
//
//
//  /**
//   * Get the optional double value associated with an index.
//   * NaN is returned if there is no value for the index,
//   * or if the value is not a number and cannot be converted to a number.
//   *
//   * @param index The index must be between 0 and length() - 1.
//   * @return      The value.
//   */
//  public double optDouble(int index) {
//    return this.optDouble(index, Double.NaN);
//  }
//
//
//  /**
//   * Get the optional double value associated with an index.
//   * The defaultValue is returned if there is no value for the index,
//   * or if the value is not a number and cannot be converted to a number.
//   *
//   * @param index subscript
//   * @param defaultValue     The default value.
//   * @return      The value.
//   */
//  public double optDouble(int index, double defaultValue) {
//    try {
//      return this.getDouble(index);
//    } catch (Exception e) {
//      return defaultValue;
//    }
//  }
//
//
//  /**
//   * Get the optional int value associated with an index.
//   * Zero is returned if there is no value for the index,
//   * or if the value is not a number and cannot be converted to a number.
//   *
//   * @param index The index must be between 0 and length() - 1.
//   * @return      The value.
//   */
//  public int optInt(int index) {
//    return this.optInt(index, 0);
//  }
//
//
//  /**
//   * Get the optional int value associated with an index.
//   * The defaultValue is returned if there is no value for the index,
//   * or if the value is not a number and cannot be converted to a number.
//   * @param index The index must be between 0 and length() - 1.
//   * @param defaultValue     The default value.
//   * @return      The value.
//   */
//  public int optInt(int index, int defaultValue) {
//    try {
//      return this.getInt(index);
//    } catch (Exception e) {
//      return defaultValue;
//    }
//  }
//
//
//  /**
//   * Get the optional JSONArray associated with an index.
//   * @param index subscript
//   * @return      A JSONArray value, or null if the index has no value,
//   * or if the value is not a JSONArray.
//   */
//  public JSONArray optJSONArray(int index) {
//    Object o = this.opt(index);
//    return o instanceof JSONArray ? (JSONArray)o : null;
//  }
//
//
//  /**
//   * Get the optional JSONObject associated with an index.
//   * Null is returned if the key is not found, or null if the index has
//   * no value, or if the value is not a JSONObject.
//   *
//   * @param index The index must be between 0 and length() - 1.
//   * @return      A JSONObject value.
//   */
//  public JSON optJSONObject(int index) {
//    Object o = this.opt(index);
//    return o instanceof JSON ? (JSON)o : null;
//  }
//
//
//  /**
//   * Get the optional long value associated with an index.
//   * Zero is returned if there is no value for the index,
//   * or if the value is not a number and cannot be converted to a number.
//   *
//   * @param index The index must be between 0 and length() - 1.
//   * @return      The value.
//   */
//  public long optLong(int index) {
//    return this.optLong(index, 0);
//  }
//
//
//  /**
//   * Get the optional long value associated with an index.
//   * The defaultValue is returned if there is no value for the index,
//   * or if the value is not a number and cannot be converted to a number.
//   * @param index The index must be between 0 and length() - 1.
//   * @param defaultValue     The default value.
//   * @return      The value.
//   */
//  public long optLong(int index, long defaultValue) {
//    try {
//      return this.getLong(index);
//    } catch (Exception e) {
//      return defaultValue;
//    }
//  }
//
//
//  /**
//   * Get the optional string value associated with an index. It returns an
//   * empty string if there is no value at that index. If the value
//   * is not a string and is not null, then it is coverted to a string.
//   *
//   * @param index The index must be between 0 and length() - 1.
//   * @return      A String value.
//   */
//  public String optString(int index) {
//    return this.optString(index, "");
//  }
//
//
//  /**
//   * Get the optional string associated with an index.
//   * The defaultValue is returned if the key is not found.
//   *
//   * @param index The index must be between 0 and length() - 1.
//   * @param defaultValue     The default value.
//   * @return      A String value.
//   */
//  public String optString(int index, String defaultValue) {
//    Object object = this.opt(index);
//    return JSON.NULL.equals(object)
//      ? defaultValue
//        : object.toString();
//  }


  /**
   * Append an int value. This increases the array's length by one.
   *
   * @param value An int value.
   * @return this.
   */
  public JSONArray append(int value) {
    this.append(new Integer(value));
    return this;
  }


  /**
   * Append an long value. This increases the array's length by one.
   *
   * @param value A long value.
   * @return this.
   */
  public JSONArray append(long value) {
    this.append(new Long(value));
    return this;
  }


  /**
   * Append a float value. This increases the array's length by one.
   * This will store the value as a double, since there are no floats in JSON.
   *
   * @param value A float value.
   * @throws JSONException if the value is not finite.
   * @return this.
   */
  public JSONArray append(float value) {
    return append((double) value);
  }


  /**
   * Append a double value. This increases the array's length by one.
   *
   * @param value A double value.
   * @throws JSONException if the value is not finite.
   * @return this.
   */
  public JSONArray append(double value) {
    Double d = new Double(value);
    JSONObject.testValidity(d);
    this.append(d);
    return this;
  }


  /**
   * Append a boolean value. This increases the array's length by one.
   *
   * @param value A boolean value.
   * @return this.
   */
  public JSONArray append(boolean value) {
    this.append(value ? Boolean.TRUE : Boolean.FALSE);
    return this;
  }


//  /**
//   * Put a value in the JSONArray, where the value will be a
//   * JSONArray which is produced from a Collection.
//   * @param value A Collection value.
//   * @return      this.
//   */
//  public JSONArray append(Collection value) {
//    this.append(new JSONArray(value));
//    return this;
//  }


//  /**
//   * Put a value in the JSONArray, where the value will be a
//   * JSONObject which is produced from a Map.
//   * @param value A Map value.
//   * @return      this.
//   */
//  public JSONArray append(Map value) {
//    this.append(new JSONObject(value));
//    return this;
//  }


  /**
   * Append an object value. This increases the array's length by one.
   * @param value An object value.  The value should be a
   *  Boolean, Double, Integer, JSONArray, JSONObject, Long, or String, or the
   *  JSONObject.NULL object.
   * @return this.
   */
  public JSONArray append(Object value) {
    this.myArrayList.add(value);
    return this;
  }


//  /**
//   * Put a value in the JSONArray, where the value will be a
//   * JSONArray which is produced from a Collection.
//   * @param index The subscript.
//   * @param value A Collection value.
//   * @return      this.
//   * @throws JSONException If the index is negative or if the value is
//   * not finite.
//   */
//  public JSONArray set(int index, Collection value) {
//    this.set(index, new JSONArray(value));
//    return this;
//  }


  /**
   * Put or replace an int value. If the index is greater than the length of
   *  the JSONArray, then null elements will be added as necessary to pad
   *  it out.
   * @param index The subscript.
   * @param value An int value.
   * @return this.
   * @throws JSONException If the index is negative.
   */
  public JSONArray setInt(int index, int value) {
    this.set(index, new Integer(value));
    return this;
  }


  /**
   * Put or replace a long value. If the index is greater than the length of
   *  the JSONArray, then null elements will be added as necessary to pad
   *  it out.
   * @param index The subscript.
   * @param value A long value.
   * @return this.
   * @throws JSONException If the index is negative.
   */
  public JSONArray setLong(int index, long value) {
    return set(index, new Long(value));
  }


  /**
   * Put or replace a float value. If the index is greater than the length
   * of the JSONArray, then null elements will be added as necessary to pad
   * it out. There are no 'double' values in JSON, so this is passed to
   * setDouble(value).
   * @param index The subscript.
   * @param value A float value.
   * @return this.
   * @throws RuntimeException If the index is negative or if the value is
   * not finite.
   */
  public JSONArray setFloat(int index, float value) {
    return setDouble(index, value);
  }


  /**
   * Put or replace a double value. If the index is greater than the length of
   *  the JSONArray, then null elements will be added as necessary to pad
   *  it out.
   * @param index The subscript.
   * @param value A double value.
   * @return this.
   * @throws JSONException If the index is negative or if the value is
   * not finite.
   */
  public JSONArray setDouble(int index, double value) {
    return set(index, new Double(value));
  }


  /**
   * Put or replace a boolean value in the JSONArray. If the index is greater
   * than the length of the JSONArray, then null elements will be added as
   * necessary to pad it out.
   * @param index The subscript.
   * @param value A boolean value.
   * @return this.
   * @throws JSONException If the index is negative.
   */
  public JSONArray setBoolean(int index, boolean value) {
    return set(index, value ? Boolean.TRUE : Boolean.FALSE);
  }


//  /**
//   * Put a value in the JSONArray, where the value will be a
//   * JSONObject that is produced from a Map.
//   * @param index The subscript.
//   * @param value The Map value.
//   * @return      this.
//   * @throws JSONException If the index is negative or if the the value is
//   *  an invalid number.
//   */
//  public JSONArray set(int index, Map value) {
//    this.set(index, new JSONObject(value));
//    return this;
//  }


  /**
   * Put or replace an object value in the JSONArray. If the index is greater
   *  than the length of the JSONArray, then null elements will be added as
   *  necessary to pad it out.
   * @param index The subscript.
   * @param value The value to put into the array. The value should be a
   *  Boolean, Double, Integer, JSONArray, JSONObject, Long, or String, or the
   *  JSONObject.NULL object.
   * @return this.
   * @throws JSONException If the index is negative or if the the value is
   *  an invalid number.
   */
  private JSONArray set(int index, Object value) {
    JSONObject.testValidity(value);
    if (index < 0) {
      throw new RuntimeException("JSONArray[" + index + "] not found.");
    }
    if (index < this.size()) {
      this.myArrayList.set(index, value);
    } else {
      while (index != this.size()) {
        this.append(JSONObject.NULL);
      }
      this.append(value);
    }
    return this;
  }


  /**
   * Get the number of elements in the JSONArray, included nulls.
   *
   * @return The length (or size).
   */
  public int size() {
    return myArrayList.size();
  }


  /**
   * Determine if the value is null.
   * @param index The index must be between 0 and length() - 1.
   * @return true if the value at the index is null, or if there is no value.
   */
  public boolean isNull(int index) {
    return JSONObject.NULL.equals(this.opt(index));
  }


  /**
   * Remove an index and close the hole.
   * @param index The index of the element to be removed.
   * @return The value that was associated with the index,
   * or null if there was no value.
   */
  public Object removeIndex(int index) {
    Object o = this.opt(index);
    this.myArrayList.remove(index);
    return o;
  }


//  /**
//   * Produce a JSONObject by combining a JSONArray of names with the values
//   * of this JSONArray.
//   * @param names A JSONArray containing a list of key strings. These will be
//   * paired with the values.
//   * @return A JSONObject, or null if there are no names or if this JSONArray
//   * has no values.
//   * @throws JSONException If any of the names are null.
//   */
//  public JSON toJSONObject(JSONArray names) {
//    if (names == null || names.length() == 0 || this.length() == 0) {
//      return null;
//    }
//    JSON jo = new JSON();
//    for (int i = 0; i < names.length(); i += 1) {
//      jo.put(names.getString(i), this.opt(i));
//    }
//    return jo;
//  }


  /**
   * Make a JSON text of this JSONArray. For compactness, no
   * unnecessary whitespace is added. If it is not possible to produce a
   * syntactically correct JSON text then null will be returned instead. This
   * could occur if the array contains an invalid number.
   * <p>
   * Warning: This method assumes that the data structure is acyclical.
   *
   * @return a printable, displayable, transmittable
   *  representation of the array.
   */
  @Override
  public String toString() {
    try {
      return this.toString(0);
    } catch (Exception e) {
      return null;
    }
  }


  /**
   * Make a prettyprinted JSON text of this JSONArray.
   * Warning: This method assumes that the data structure is acyclical.
   * @param indentFactor The number of spaces to add to each level of
   *  indentation.
   * @return a printable, displayable, transmittable
   *  representation of the object, beginning
   *  with <code>[</code>&nbsp;<small>(left bracket)</small> and ending
   *  with <code>]</code>&nbsp;<small>(right bracket)</small>.
   * @throws JSONException
   */
  public String toString(int indentFactor) {
    StringWriter sw = new StringWriter();
    synchronized (sw.getBuffer()) {
      return this.write(sw, indentFactor, 0).toString();
    }
  }

  /**
   * Write the contents of the JSONArray as JSON text to a writer. For
   * compactness, no whitespace is added.
   * <p>
   * Warning: This method assumes that the data structure is acyclical.
   *
   * @return The writer.
   * @throws JSONException
   */
  public Writer write(Writer writer) {
    return this.write(writer, 0, 0);
  }

  /**
   * Write the contents of the JSONArray as JSON text to a writer. For
   * compactness, no whitespace is added.
   * <p>
   * Warning: This method assumes that the data structure is acyclical.
   *
   * @param indentFactor
   *            The number of spaces to add to each level of indentation.
   * @param indent
   *            The indention of the top level.
   * @return The writer.
   * @throws JSONException
   */
  Writer write(Writer writer, int indentFactor, int indent) {
    try {
      boolean commanate = false;
      int length = this.size();
      writer.write('[');

      if (length == 1) {
        JSONObject.writeValue(writer, this.myArrayList.get(0),
                              indentFactor, indent);
      } else if (length != 0) {
        final int newindent = indent + indentFactor;

        for (int i = 0; i < length; i += 1) {
          if (commanate) {
            writer.write(',');
          }
          if (indentFactor > 0) {
            writer.write('\n');
          }
          JSONObject.indent(writer, newindent);
          JSONObject.writeValue(writer, this.myArrayList.get(i),
                                indentFactor, newindent);
          commanate = true;
        }
        if (indentFactor > 0) {
          writer.write('\n');
        }
        JSONObject.indent(writer, indent);
      }
      writer.write(']');
      return writer;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  /**
   * Make a string from the contents of this JSONArray. The
   * <code>separator</code> string is inserted between each element.
   * Warning: This method assumes that the data structure is acyclical.
   * @param separator A string that will be inserted between the elements.
   * @return a string.
   * @throws JSONException If the array contains an invalid number.
   */
  public String join(String separator) {
    int len = this.size();
    StringBuffer sb = new StringBuffer();

    for (int i = 0; i < len; i += 1) {
      if (i > 0) {
        sb.append(separator);
      }
      sb.append(JSONObject.valueToString(this.myArrayList.get(i)));
    }
    return sb.toString();
  }
}

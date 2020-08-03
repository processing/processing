package processing.data;

// This code has been modified heavily to more closely match the rest of the
// Processing API. In the spirit of the rest of the project, where we try to
// keep the API as simple as possible, we have erred on the side of being
// conservative in choosing which functions to include, since we haven't yet
// decided what's truly necessary. Power users looking for a full-featured
// version can use the original version from json.org, or one of the many
// other APIs that are available. As with all Processing API, if there's a
// function that should be added, please let use know, and have others vote:
// http://code.google.com/p/processing/issues/list

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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import processing.core.PApplet;

/**
 * A JSONObject is an unordered collection of name/value pairs. Its external
 * form is a string wrapped in curly braces with colons between the names and
 * values, and commas between the values and names. The internal form is an
 * object having <code>get</code> and <code>opt</code> methods for accessing the
 * values by name, and <code>put</code> methods for adding or replacing values
 * by name. The values can be any of these types: <code>Boolean</code>,
 * <code>JSONArray</code>, <code>JSONObject</code>, <code>Number</code>,
 * <code>String</code>, or the <code>JSONObject.NULL</code> object. A JSONObject
 * constructor can be used to convert an external form JSON text into an
 * internal form whose values can be retrieved with the <code>get</code> and
 * <code>opt</code> methods, or to convert values into a JSON text using the
 * <code>put</code> and <code>toString</code> methods. A <code>get</code> method
 * returns a value if one can be found, and throws an exception if one cannot be
 * found. An <code>opt</code> method returns a default value instead of throwing
 * an exception, and so is useful for obtaining optional values.
 * <p>
 * The generic <code>get()</code> and <code>opt()</code> methods return an
 * object, which you can cast or query for type. There are also typed
 * <code>get</code> and <code>opt</code> methods that do type checking and type
 * coercion for you. The opt methods differ from the get methods in that they do
 * not throw. Instead, they return a specified value, such as null.
 * <p>
 * The <code>put</code> methods add or replace values in an object. For example,
 *
 * <pre>
 * myString = new JSONObject().put(&quot;JSON&quot;, &quot;Hello, World!&quot;).toString();
 * </pre>
 *
 * produces the string <code>{"JSON": "Hello, World"}</code>.
 * <p>
 * The texts produced by the <code>toString</code> methods strictly conform to
 * the JSON syntax rules. The constructors are more forgiving in the texts they
 * will accept:
 * <ul>
 * <li>An extra <code>,</code>&nbsp;<small>(comma)</small> may appear just
 * before the closing brace.</li>
 * <li>Strings may be quoted with <code>'</code>&nbsp;<small>(single
 * quote)</small>.</li>
 * <li>Strings do not need to be quoted at all if they do not begin with a quote
 * or single quote, and if they do not contain leading or trailing spaces, and
 * if they do not contain any of these characters:
 * <code>{ } [ ] / \ : , = ; #</code> and if they do not look like numbers and
 * if they are not the reserved words <code>true</code>, <code>false</code>, or
 * <code>null</code>.</li>
 * <li>Keys can be followed by <code>=</code> or {@code =>} as well as by
 * <code>:</code>.</li>
 * <li>Values can be followed by <code>;</code> <small>(semicolon)</small> as
 * well as by <code>,</code> <small>(comma)</small>.</li>
 * </ul>
 *
 * @author JSON.org
 * @version 2012-12-01
 * @webref data:composite
 * @see JSONArray
 * @see PApplet#loadJSONObject(String)
 * @see PApplet#loadJSONArray(String)
 * @see PApplet#saveJSONObject(JSONObject, String)
 * @see PApplet#saveJSONArray(JSONArray, String)
 */
public class JSONObject {
  /**
   * The maximum number of keys in the key pool.
   */
  private static final int keyPoolSize = 100;

  /**
   * Key pooling is like string interning, but without permanently tying up
   * memory. To help conserve memory, storage of duplicated key strings in
   * JSONObjects will be avoided by using a key pool to manage unique key
   * string objects. This is used by JSONObject.put(string, object).
   */
  private static HashMap<String, Object> keyPool =
    new HashMap<>(keyPoolSize);


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * JSONObject.NULL is equivalent to the value that JavaScript calls null,
   * whilst Java's null is equivalent to the value that JavaScript calls
   * undefined.
   */
  private static final class Null {
    /**
     * There is only intended to be a single instance of the NULL object,
     * so the clone method returns itself.
     * @return     NULL.
     */
    @Override
    protected final Object clone() {
      return this;
    }

    /**
     * A Null object is equal to the null value and to itself.
     * @param object    An object to test for nullness.
     * @return true if the object parameter is the JSONObject.NULL object
     *  or null.
     */
    @Override
    public boolean equals(Object object) {
      return object == null || object == this;
    }

    /**
     * Get the "null" string value.
     * @return The string "null".
     */
    @Override
    public String toString() {
      return "null";
    }

    @Override
    public int hashCode() {
      // TODO Auto-generated method stub
      return super.hashCode();
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * The map where the JSONObject's properties are kept.
   */
//  private final Map map;
  private final HashMap<String, Object> map;


  /**
   * It is sometimes more convenient and less ambiguous to have a
   * <code>NULL</code> object than to use Java's <code>null</code> value.
   * <code>JSONObject.NULL.equals(null)</code> returns <code>true</code>.
   * <code>JSONObject.NULL.toString()</code> returns <code>"null"</code>.
   */
  public static final Object NULL = new Null();


  /**
   * Construct an empty JSONObject.
   * @nowebref
   */
  public JSONObject() {
    this.map = new HashMap<>();
  }


//  /**
//   * Construct a JSONObject from a subset of another JSONObject.
//   * An array of strings is used to identify the keys that should be copied.
//   * Missing keys are ignored.
//   * @param jo A JSONObject.
//   * @param names An array of strings.
//   */
//  public JSONObject(JSONObject jo, String[] names) {
//    this();
//    for (int i = 0; i < names.length; i += 1) {
//      try {
//        this.putOnce(names[i], jo.opt(names[i]));
//      } catch (Exception ignore) {
//      }
//    }
//  }


  /**
   * @nowebref
   */
  public JSONObject(Reader reader) {
    this(new JSONTokener(reader));
  }


  /**
   * Construct a JSONObject from a JSONTokener.
   * @param x A JSONTokener object containing the source string.
   * @throws RuntimeException If there is a syntax error in the source string
   *  or a duplicated key.
   */
  protected JSONObject(JSONTokener x) {
    this();
    char c;
    String key;

    if (x.nextClean() != '{') {
      throw new RuntimeException("A JSONObject text must begin with '{'");
    }
    for (;;) {
      c = x.nextClean();
      switch (c) {
      case 0:
        throw new RuntimeException("A JSONObject text must end with '}'");
      case '}':
        return;
      default:
        x.back();
        key = x.nextValue().toString();
      }

      // The key is followed by ':'. We will also tolerate '=' or '=>'.

      c = x.nextClean();
      if (c == '=') {
        if (x.next() != '>') {
          x.back();
        }
      } else if (c != ':') {
        throw new RuntimeException("Expected a ':' after a key");
      }
      this.putOnce(key, x.nextValue());

      // Pairs are separated by ','. We will also tolerate ';'.

      switch (x.nextClean()) {
      case ';':
      case ',':
        if (x.nextClean() == '}') {
          return;
        }
        x.back();
        break;
      case '}':
        return;
      default:
        throw new RuntimeException("Expected a ',' or '}'");
      }
    }
  }


  /**
   * Construct a JSONObject from a Map.
   *
   * @param map A map object that can be used to initialize the contents of
   *  the JSONObject.
   */
  protected JSONObject(HashMap<String, Object> map) {
    this.map = new HashMap<>();
    if (map != null) {
      Iterator i = map.entrySet().iterator();
      while (i.hasNext()) {
        Map.Entry e = (Map.Entry) i.next();
        Object value = e.getValue();
        if (value != null) {
          map.put((String) e.getKey(), wrap(value));
        }
      }
    }
  }


  /**
   * @nowebref
   */
  public JSONObject(IntDict dict) {
    map = new HashMap<>();
    for (int i = 0; i < dict.size(); i++) {
      setInt(dict.key(i), dict.value(i));
    }
  }


  /**
   * @nowebref
   */
  public JSONObject(FloatDict dict) {
    map = new HashMap<>();
    for (int i = 0; i < dict.size(); i++) {
      setFloat(dict.key(i), dict.value(i));
    }
  }


  /**
   * @nowebref
   */
  public JSONObject(StringDict dict) {
    map = new HashMap<>();
    for (int i = 0; i < dict.size(); i++) {
      setString(dict.key(i), dict.value(i));
    }
  }


  /**
   * Construct a JSONObject from an Object using bean getters.
   * It reflects on all of the public methods of the object.
   * For each of the methods with no parameters and a name starting
   * with <code>"get"</code> or <code>"is"</code> followed by an uppercase letter,
   * the method is invoked, and a key and the value returned from the getter method
   * are put into the new JSONObject.
   *
   * The key is formed by removing the <code>"get"</code> or <code>"is"</code> prefix.
   * If the second remaining character is not upper case, then the first
   * character is converted to lower case.
   *
   * For example, if an object has a method named <code>"getName"</code>, and
   * if the result of calling <code>object.getName()</code> is <code>"Larry Fine"</code>,
   * then the JSONObject will contain <code>"name": "Larry Fine"</code>.
   *
   * @param bean An object that has getter methods that should be used
   * to make a JSONObject.
   */
  protected JSONObject(Object bean) {
    this();
    this.populateMap(bean);
  }


  // holding off on this method until we decide on how to handle reflection
//  /**
//   * Construct a JSONObject from an Object, using reflection to find the
//   * public members. The resulting JSONObject's keys will be the strings
//   * from the names array, and the values will be the field values associated
//   * with those keys in the object. If a key is not found or not visible,
//   * then it will not be copied into the new JSONObject.
//   * @param object An object that has fields that should be used to make a
//   * JSONObject.
//   * @param names An array of strings, the names of the fields to be obtained
//   * from the object.
//   */
//  public JSONObject(Object object, String names[]) {
//    this();
//    Class c = object.getClass();
//    for (int i = 0; i < names.length; i += 1) {
//      String name = names[i];
//      try {
//        this.putOpt(name, c.getField(name).get(object));
//      } catch (Exception ignore) {
//      }
//    }
//  }


  /**
   * Construct a JSONObject from a source JSON text string.
   * This is the most commonly used JSONObject constructor.
   * @param source    A string beginning
   *  with <code>{</code>&nbsp;<small>(left brace)</small> and ending
   *  with <code>}</code>&nbsp;<small>(right brace)</small>.
   * @exception RuntimeException If there is a syntax error in the source
   *  string or a duplicated key.
   */
  static public JSONObject parse(String source) {
    return new JSONObject(new JSONTokener(source));
  }


//  /**
//   * Construct a JSONObject from a ResourceBundle.
//   * @param baseName The ResourceBundle base name.
//   * @param locale The Locale to load the ResourceBundle for.
//   * @throws JSONException If any JSONExceptions are detected.
//   */
//  public JSON(String baseName, Locale locale) {
//    this();
//    ResourceBundle bundle = ResourceBundle.getBundle(baseName, locale,
//                                                     Thread.currentThread().getContextClassLoader());
//
//    // Iterate through the keys in the bundle.
//
//    Enumeration keys = bundle.getKeys();
//    while (keys.hasMoreElements()) {
//      Object key = keys.nextElement();
//      if (key instanceof String) {
//
//        // Go through the path, ensuring that there is a nested JSONObject for each
//        // segment except the last. Add the value using the last segment's name into
//        // the deepest nested JSONObject.
//
//        String[] path = ((String)key).split("\\.");
//        int last = path.length - 1;
//        JSON target = this;
//        for (int i = 0; i < last; i += 1) {
//          String segment = path[i];
//          JSON nextTarget = target.optJSONObject(segment);
//          if (nextTarget == null) {
//            nextTarget = new JSON();
//            target.put(segment, nextTarget);
//          }
//          target = nextTarget;
//        }
//        target.put(path[last], bundle.getString((String)key));
//      }
//    }
//  }


//  /**
//   * Accumulate values under a key. It is similar to the put method except
//   * that if there is already an object stored under the key then a
//   * JSONArray is stored under the key to hold all of the accumulated values.
//   * If there is already a JSONArray, then the new value is appended to it.
//   * In contrast, the put method replaces the previous value.
//   *
//   * If only one value is accumulated that is not a JSONArray, then the
//   * result will be the same as using put. But if multiple values are
//   * accumulated, then the result will be like append.
//   * @param key   A key string.
//   * @param value An object to be accumulated under the key.
//   * @return this.
//   * @throws JSONException If the value is an invalid number
//   *  or if the key is null.
//   */
//  public JSONObject accumulate(
//                               String key,
//                               Object value
//    ) throws JSONException {
//    testValidity(value);
//    Object object = this.opt(key);
//    if (object == null) {
//      this.put(key, value instanceof JSONArray
//               ? new JSONArray().put(value)
//                 : value);
//    } else if (object instanceof JSONArray) {
//      ((JSONArray)object).put(value);
//    } else {
//      this.put(key, new JSONArray().put(object).put(value));
//    }
//    return this;
//  }


//  /**
//   * Append values to the array under a key. If the key does not exist in the
//   * JSONObject, then the key is put in the JSONObject with its value being a
//   * JSONArray containing the value parameter. If the key was already
//   * associated with a JSONArray, then the value parameter is appended to it.
//   * @param key   A key string.
//   * @param value An object to be accumulated under the key.
//   * @return this.
//   * @throws JSONException If the key is null or if the current value
//   *  associated with the key is not a JSONArray.
//   */
//  public JSONObject append(String key, Object value) throws JSONException {
//    testValidity(value);
//    Object object = this.opt(key);
//    if (object == null) {
//      this.put(key, new JSONArray().put(value));
//    } else if (object instanceof JSONArray) {
//      this.put(key, ((JSONArray)object).put(value));
//    } else {
//      throw new JSONException("JSONObject[" + key +
//        "] is not a JSONArray.");
//    }
//    return this;
//  }


  /**
   * Produce a string from a double. The string "null" will be returned if
   * the number is not finite.
   * @param  d A double.
   * @return A String.
   */
  static protected String doubleToString(double d) {
    if (Double.isInfinite(d) || Double.isNaN(d)) {
      return "null";
    }

    // Shave off trailing zeros and decimal point, if possible.

    String string = Double.toString(d);
    if (string.indexOf('.') > 0 && string.indexOf('e') < 0 &&
      string.indexOf('E') < 0) {
      while (string.endsWith("0")) {
        string = string.substring(0, string.length() - 1);
      }
      if (string.endsWith(".")) {
        string = string.substring(0, string.length() - 1);
      }
    }
    return string;
  }


  /**
   * Get the value object associated with a key.
   *
   * @param key   A key string.
   * @return      The object associated with the key.
   * @throws      RuntimeException if the key is not found.
   */
  public Object get(String key) {
    if (key == null) {
      throw new RuntimeException("JSONObject.get(null) called");
    }
    Object object = this.opt(key);
    if (object == null) {
      // Adding for rev 0257 in line with other p5 api
      return null;
    }
    if (object == null) {
      throw new RuntimeException("JSONObject[" + quote(key) + "] not found");
    }
    return object;
  }


  /**
   * Gets the String associated with a key
   *
   * @webref jsonobject:method
   * @brief Gets the string value associated with a key
   * @param key a key string
   * @return A string which is the value.
   * @throws RuntimeException if there is no string value for the key.
   * @see JSONObject#getInt(String)
   * @see JSONObject#getFloat(String)
   * @see JSONObject#getBoolean(String)
   */
  public String getString(String key) {
    Object object = this.get(key);
    if (object == null) {
      // Adding for rev 0257 in line with other p5 api
      return null;
    }
    if (object instanceof String) {
      return (String)object;
    }
    throw new RuntimeException("JSONObject[" + quote(key) + "] is not a string");
  }


  /**
   * Get an optional string associated with a key.
   * It returns the defaultValue if there is no such key.
   *
   * @param key   A key string.
   * @param defaultValue     The default.
   * @return      A string which is the value.
   */
  public String getString(String key, String defaultValue) {
    Object object = this.opt(key);
    return NULL.equals(object) ? defaultValue : object.toString();
  }


  /**
   * Gets the int value associated with a key
   *
   * @webref jsonobject:method
   * @brief Gets the int value associated with a key
   * @param key A key string.
   * @return The integer value.
   * @throws RuntimeException if the key is not found or if the value cannot
   *  be converted to an integer.
   * @see JSONObject#getFloat(String)
   * @see JSONObject#getString(String)
   * @see JSONObject#getBoolean(String)
   */
  public int getInt(String key) {
    Object object = this.get(key);
    if (object == null) {
      throw new RuntimeException("JSONObject[" + quote(key) + "] not found");
    }
    try {
      return object instanceof Number ?
        ((Number)object).intValue() : Integer.parseInt((String)object);
    } catch (Exception e) {
      throw new RuntimeException("JSONObject[" + quote(key) + "] is not an int.");
    }
  }


  /**
   * Get an optional int value associated with a key,
   * or the default if there is no such key or if the value is not a number.
   * If the value is a string, an attempt will be made to evaluate it as
   * a number.
   *
   * @param key   A key string.
   * @param defaultValue     The default.
   * @return      An object which is the value.
   */
  public int getInt(String key, int defaultValue) {
    try {
      return this.getInt(key);
    } catch (Exception e) {
      return defaultValue;
    }
  }


  /**
   * Get the long value associated with a key.
   *
   * @param key   A key string.
   * @return      The long value.
   * @throws   RuntimeException if the key is not found or if the value cannot
   *  be converted to a long.
   */
  public long getLong(String key) {
    Object object = this.get(key);
    try {
      return object instanceof Number
        ? ((Number)object).longValue()
          : Long.parseLong((String)object);
    } catch (Exception e) {
      throw new RuntimeException("JSONObject[" + quote(key) + "] is not a long.", e);
    }
  }


  /**
   * Get an optional long value associated with a key,
   * or the default if there is no such key or if the value is not a number.
   * If the value is a string, an attempt will be made to evaluate it as
   * a number.
   *
   * @param key          A key string.
   * @param defaultValue The default.
   * @return             An object which is the value.
   */
  public long getLong(String key, long defaultValue) {
    try {
      return this.getLong(key);
    } catch (Exception e) {
      return defaultValue;
    }
  }


  /**
   * @webref jsonobject:method
   * @brief Gets the float value associated with a key
   * @param key a key string
   * @see JSONObject#getInt(String)
   * @see JSONObject#getString(String)
   * @see JSONObject#getBoolean(String)
   */
  public float getFloat(String key) {
    return (float) getDouble(key);
  }


  public float getFloat(String key, float defaultValue) {
    try {
      return getFloat(key);
    } catch (Exception e) {
      return defaultValue;
    }
  }


  /**
   * Get the double value associated with a key.
   * @param key   A key string.
   * @return      The numeric value.
   * @throws RuntimeException if the key is not found or
   *  if the value is not a Number object and cannot be converted to a number.
   */
  public double getDouble(String key) {
    Object object = this.get(key);
    try {
      return object instanceof Number
        ? ((Number)object).doubleValue()
          : Double.parseDouble((String)object);
    } catch (Exception e) {
      throw new RuntimeException("JSONObject[" + quote(key) + "] is not a number.");
    }
  }


  /**
   * Get an optional double associated with a key, or the
   * defaultValue if there is no such key or if its value is not a number.
   * If the value is a string, an attempt will be made to evaluate it as
   * a number.
   *
   * @param key   A key string.
   * @param defaultValue     The default.
   * @return      An object which is the value.
   */
  public double getDouble(String key, double defaultValue) {
    try {
      return this.getDouble(key);
    } catch (Exception e) {
      return defaultValue;
    }
  }


  /**
   * Get the boolean value associated with a key.
   *
   * @webref jsonobject:method
   * @brief Gets the boolean value associated with a key
   * @param key a key string
   * @return The truth.
   * @throws RuntimeException if the value is not a Boolean or the String "true" or "false".
   * @see JSONObject#getInt(String)
   * @see JSONObject#getFloat(String)
   * @see JSONObject#getString(String)
   */
  public boolean getBoolean(String key) {
    Object object = this.get(key);
    if (object.equals(Boolean.FALSE) ||
      (object instanceof String &&
        ((String)object).equalsIgnoreCase("false"))) {
      return false;
    } else if (object.equals(Boolean.TRUE) ||
      (object instanceof String &&
        ((String)object).equalsIgnoreCase("true"))) {
      return true;
    }
    throw new RuntimeException("JSONObject[" + quote(key) + "] is not a Boolean.");
  }


  /**
   * Get an optional boolean associated with a key.
   * It returns the defaultValue if there is no such key, or if it is not
   * a Boolean or the String "true" or "false" (case insensitive).
   *
   * @param key              A key string.
   * @param defaultValue     The default.
   * @return      The truth.
   */
  public boolean getBoolean(String key, boolean defaultValue) {
    try {
      return this.getBoolean(key);
    } catch (Exception e) {
      return defaultValue;
    }
  }


  /**
   * Get the JSONArray value associated with a key.
   *
   * @webref jsonobject:method
   * @brief Gets the JSONArray value associated with a key
   * @param key a key string
   * @return A JSONArray which is the value, or null if not present
   * @throws RuntimeException if the value is not a JSONArray.
   * @see JSONObject#getJSONObject(String)
   * @see JSONObject#setJSONObject(String, JSONObject)
   * @see JSONObject#setJSONArray(String, JSONArray)
   */
  public JSONArray getJSONArray(String key) {
    Object object = this.get(key);
    if (object == null) {
      return null;
    }
    if (object instanceof JSONArray) {
      return (JSONArray)object;
    }
    throw new RuntimeException("JSONObject[" + quote(key) + "] is not a JSONArray.");
  }


  /**
   * Get the JSONObject value associated with a key.
   *
   * @webref jsonobject:method
   * @brief Gets the JSONObject value associated with a key
   * @param key a key string
   * @return A JSONObject which is the value or null if not available.
   * @throws RuntimeException if the value is not a JSONObject.
   * @see JSONObject#getJSONArray(String)
   * @see JSONObject#setJSONObject(String, JSONObject)
   * @see JSONObject#setJSONArray(String, JSONArray)
   */
  public JSONObject getJSONObject(String key) {
    Object object = this.get(key);
    if (object == null) {
      return null;
    }
    if (object instanceof JSONObject) {
      return (JSONObject)object;
    }
    throw new RuntimeException("JSONObject[" + quote(key) + "] is not a JSONObject.");
  }


//  /**
//   * Get an array of field names from a JSONObject.
//   *
//   * @return An array of field names, or null if there are no names.
//   */
//  public static String[] getNames(JSONObject jo) {
//    int length = jo.length();
//    if (length == 0) {
//      return null;
//    }
//    Iterator iterator = jo.keys();
//    String[] names = new String[length];
//    int i = 0;
//    while (iterator.hasNext()) {
//      names[i] = (String)iterator.next();
//      i += 1;
//    }
//    return names;
//  }
//
//
//  /**
//   * Get an array of field names from an Object.
//   *
//   * @return An array of field names, or null if there are no names.
//   */
//  public static String[] getNames(Object object) {
//    if (object == null) {
//      return null;
//    }
//    Class klass = object.getClass();
//    Field[] fields = klass.getFields();
//    int length = fields.length;
//    if (length == 0) {
//      return null;
//    }
//    String[] names = new String[length];
//    for (int i = 0; i < length; i += 1) {
//      names[i] = fields[i].getName();
//    }
//    return names;
//  }


  /**
   * Determine if the JSONObject contains a specific key.
   * @param key   A key string.
   * @return      true if the key exists in the JSONObject.
   */
  public boolean hasKey(String key) {
    return map.containsKey(key);
  }


//  /**
//   * Increment a property of a JSONObject. If there is no such property,
//   * create one with a value of 1. If there is such a property, and if
//   * it is an Integer, Long, Double, or Float, then add one to it.
//   * @param key  A key string.
//   * @return this.
//   * @throws JSONException If there is already a property with this name
//   * that is not an Integer, Long, Double, or Float.
//   */
//  public JSON increment(String key) {
//    Object value = this.opt(key);
//    if (value == null) {
//      this.put(key, 1);
//    } else if (value instanceof Integer) {
//      this.put(key, ((Integer)value).intValue() + 1);
//    } else if (value instanceof Long) {
//      this.put(key, ((Long)value).longValue() + 1);
//    } else if (value instanceof Double) {
//      this.put(key, ((Double)value).doubleValue() + 1);
//    } else if (value instanceof Float) {
//      this.put(key, ((Float)value).floatValue() + 1);
//    } else {
//      throw new RuntimeException("Unable to increment [" + quote(key) + "].");
//    }
//    return this;
//  }


  /**
   * Determine if the value associated with the key is null or if there is
   * no value.
   *
   * @webref
   * @param key   A key string.
   * @return      true if there is no value associated with the key or if
   *  the value is the JSONObject.NULL object.
   */
  public boolean isNull(String key) {
    return JSONObject.NULL.equals(this.opt(key));
  }


  /**
   * Get an enumeration of the keys of the JSONObject.
   *
   * @return An iterator of the keys.
   */
  public Iterator keyIterator() {
//    return this.keySet().iterator();
    return map.keySet().iterator();
  }


  /**
   * Get a set of keys of the JSONObject.
   *
   * @return A keySet.
   */
  public Set keys() {
    return this.map.keySet();
  }


  /**
   * Get the number of keys stored in the JSONObject.
   *
   * @return The number of keys in the JSONObject.
   */
  public int size() {
    return this.map.size();
  }


//  /**
//   * Produce a JSONArray containing the names of the elements of this
//   * JSONObject.
//   * @return A JSONArray containing the key strings, or null if the JSONObject
//   * is empty.
//   */
//  public JSONArray names() {
//    JSONArray ja = new JSONArray();
//    Iterator  keys = this.keys();
//    while (keys.hasNext()) {
//      ja.append(keys.next());
//    }
//    return ja.size() == 0 ? null : ja;
//  }


  /**
   * Produce a string from a Number.
   * @param  number A Number
   * @return A String.
   * @throws RuntimeException If number is null or a non-finite number.
   */
  private static String numberToString(Number number) {
    if (number == null) {
      throw new RuntimeException("Null pointer");
    }
    testValidity(number);

    // Shave off trailing zeros and decimal point, if possible.

    String string = number.toString();
    if (string.indexOf('.') > 0 && string.indexOf('e') < 0 &&
      string.indexOf('E') < 0) {
      while (string.endsWith("0")) {
        string = string.substring(0, string.length() - 1);
      }
      if (string.endsWith(".")) {
        string = string.substring(0, string.length() - 1);
      }
    }
    return string;
  }


  /**
   * Get an optional value associated with a key.
   * @param key   A key string.
   * @return      An object which is the value, or null if there is no value.
   */
  private Object opt(String key) {
    return key == null ? null : this.map.get(key);
  }


//  /**
//   * Get an optional boolean associated with a key.
//   * It returns false if there is no such key, or if the value is not
//   * Boolean.TRUE or the String "true".
//   *
//   * @param key   A key string.
//   * @return      The truth.
//   */
//  private boolean optBoolean(String key) {
//    return this.optBoolean(key, false);
//  }


//  /**
//   * Get an optional double associated with a key,
//   * or NaN if there is no such key or if its value is not a number.
//   * If the value is a string, an attempt will be made to evaluate it as
//   * a number.
//   *
//   * @param key   A string which is the key.
//   * @return      An object which is the value.
//   */
//  private double optDouble(String key) {
//    return this.optDouble(key, Double.NaN);
//  }


//  /**
//   * Get an optional int value associated with a key,
//   * or zero if there is no such key or if the value is not a number.
//   * If the value is a string, an attempt will be made to evaluate it as
//   * a number.
//   *
//   * @param key   A key string.
//   * @return      An object which is the value.
//   */
//  private int optInt(String key) {
//    return this.optInt(key, 0);
//  }


//  /**
//   * Get an optional JSONArray associated with a key.
//   * It returns null if there is no such key, or if its value is not a
//   * JSONArray.
//   *
//   * @param key   A key string.
//   * @return      A JSONArray which is the value.
//   */
//  private JSONArray optJSONArray(String key) {
//    Object o = this.opt(key);
//    return o instanceof JSONArray ? (JSONArray)o : null;
//  }


//  /**
//   * Get an optional JSONObject associated with a key.
//   * It returns null if there is no such key, or if its value is not a
//   * JSONObject.
//   *
//   * @param key   A key string.
//   * @return      A JSONObject which is the value.
//   */
//  private JSONObject optJSONObject(String key) {
//    Object object = this.opt(key);
//    return object instanceof JSONObject ? (JSONObject)object : null;
//  }


//  /**
//   * Get an optional long value associated with a key,
//   * or zero if there is no such key or if the value is not a number.
//   * If the value is a string, an attempt will be made to evaluate it as
//   * a number.
//   *
//   * @param key   A key string.
//   * @return      An object which is the value.
//   */
//  public long optLong(String key) {
//    return this.optLong(key, 0);
//  }


//  /**
//   * Get an optional string associated with a key.
//   * It returns an empty string if there is no such key. If the value is not
//   * a string and is not null, then it is converted to a string.
//   *
//   * @param key   A key string.
//   * @return      A string which is the value.
//   */
//  public String optString(String key) {
//    return this.optString(key, "");
//  }


  private void populateMap(Object bean) {
    Class klass = bean.getClass();

    // If klass is a System class then set includeSuperClass to false.

    boolean includeSuperClass = klass.getClassLoader() != null;

    Method[] methods = includeSuperClass
      ? klass.getMethods()
        : klass.getDeclaredMethods();
      for (int i = 0; i < methods.length; i += 1) {
        try {
          Method method = methods[i];
          if (Modifier.isPublic(method.getModifiers())) {
            String name = method.getName();
            String key = "";
            if (name.startsWith("get")) {
              if ("getClass".equals(name) ||
                "getDeclaringClass".equals(name)) {
                key = "";
              } else {
                key = name.substring(3);
              }
            } else if (name.startsWith("is")) {
              key = name.substring(2);
            }
            if (key.length() > 0 &&
              Character.isUpperCase(key.charAt(0)) &&
              method.getParameterTypes().length == 0) {
              if (key.length() == 1) {
                key = key.toLowerCase();
              } else if (!Character.isUpperCase(key.charAt(1))) {
                key = key.substring(0, 1).toLowerCase() +
                  key.substring(1);
              }

              Object result = method.invoke(bean, (Object[])null);
              if (result != null) {
                this.map.put(key, wrap(result));
              }
            }
          }
        } catch (Exception ignore) {
        }
      }
  }


  /**
   * @webref jsonobject:method
   * @brief Put a key/String pair in the JSONObject
   * @param key a key string
   * @param value the value to assign
   * @see JSONObject#setInt(String, int)
   * @see JSONObject#setFloat(String, float)
   * @see JSONObject#setBoolean(String, boolean)
   */
  public JSONObject setString(String key, String value) {
    return put(key, value);
  }


  /**
   * Put a key/int pair in the JSONObject.
   *
   * @webref jsonobject:method
   * @brief Put a key/int pair in the JSONObject
   * @param key a key string
   * @param value the value to assign
   * @return this.
   * @throws RuntimeException If the key is null.
   * @see JSONObject#setFloat(String, float)
   * @see JSONObject#setString(String, String)
   * @see JSONObject#setBoolean(String, boolean)
   */
  public JSONObject setInt(String key, int value) {
    this.put(key, Integer.valueOf(value));
    return this;
  }


  /**
   * Put a key/long pair in the JSONObject.
   *
   * @param key   A key string.
   * @param value A long which is the value.
   * @return this.
   * @throws RuntimeException If the key is null.
   */
  public JSONObject setLong(String key, long value) {
    this.put(key, Long.valueOf(value));
    return this;
  }

  /**
   * @webref jsonobject:method
   * @brief Put a key/float pair in the JSONObject
   * @param key a key string
   * @param value the value to assign
   * @throws RuntimeException If the key is null or if the number is NaN or infinite.
   * @see JSONObject#setInt(String, int)
   * @see JSONObject#setString(String, String)
   * @see JSONObject#setBoolean(String, boolean)
   */
  public JSONObject setFloat(String key, float value) {
    this.put(key, Double.valueOf(value));
    return this;
  }


  /**
   * Put a key/double pair in the JSONObject.
   *
   * @param key   A key string.
   * @param value A double which is the value.
   * @return this.
   * @throws RuntimeException If the key is null or if the number is NaN or infinite.
   */
  public JSONObject setDouble(String key, double value) {
    this.put(key, Double.valueOf(value));
    return this;
  }


  /**
   * Put a key/boolean pair in the JSONObject.
   *
   * @webref jsonobject:method
   * @brief Put a key/boolean pair in the JSONObject
   * @param key a key string
   * @param value the value to assign
   * @return this.
   * @throws RuntimeException If the key is null.
   * @see JSONObject#setInt(String, int)
   * @see JSONObject#setFloat(String, float)
   * @see JSONObject#setString(String, String)
   */
  public JSONObject setBoolean(String key, boolean value) {
    this.put(key, value ? Boolean.TRUE : Boolean.FALSE);
    return this;
  }

  /**
   * @webref jsonobject:method
   * @brief Sets the JSONObject value associated with a key
   * @param key a key string
   * @param value value to assign
   * @see JSONObject#setJSONArray(String, JSONArray)
   * @see JSONObject#getJSONObject(String)
   * @see JSONObject#getJSONArray(String)
   */
  public JSONObject setJSONObject(String key, JSONObject value) {
    return put(key, value);
  }

  /**
   * @webref jsonobject:method
   * @brief Sets the JSONArray value associated with a key
   * @param key a key string
   * @param value value to assign
   * @see JSONObject#setJSONObject(String, JSONObject)
   * @see JSONObject#getJSONObject(String)
   * @see JSONObject#getJSONArray(String)
   */
  public JSONObject setJSONArray(String key, JSONArray value) {
    return put(key, value);
  }


//  /**
//   * Put a key/value pair in the JSONObject, where the value will be a
//   * JSONArray which is produced from a Collection.
//   * @param key   A key string.
//   * @param value A Collection value.
//   * @return      this.
//   * @throws JSONException
//   */
//  public JSONObject put(String key, Collection value) {
//    this.put(key, new JSONArray(value));
//    return this;
//  }


//  /**
//   * Put a key/value pair in the JSONObject, where the value will be a
//   * JSONObject which is produced from a Map.
//   * @param key   A key string.
//   * @param value A Map value.
//   * @return      this.
//   * @throws JSONException
//   */
//  //public JSONObject put(String key, HashMap<String, Object> value) {
//  public JSONObject put(String key, Map value) {
//    this.put(key, new JSONObject(value));
//    return this;
//  }


  /**
   * Put a key/value pair in the JSONObject. If the value is null,
   * then the key will be removed from the JSONObject if it is present.
   * @param key   A key string.
   * @param value An object which is the value. It should be of one of these
   *  types: Boolean, Double, Integer, JSONArray, JSONObject, Long, String,
   *  or the JSONObject.NULL object.
   * @return this.
   * @throws RuntimeException If the value is non-finite number
   *  or if the key is null.
   */
  public JSONObject put(String key, Object value) {
    String pooled;
    if (key == null) {
      throw new RuntimeException("Null key.");
    }
    if (value != null) {
      testValidity(value);
      pooled = (String)keyPool.get(key);
      if (pooled == null) {
        if (keyPool.size() >= keyPoolSize) {
          keyPool = new HashMap<>(keyPoolSize);
        }
        keyPool.put(key, key);
      } else {
        key = pooled;
      }
      this.map.put(key, value);
    } else {
      this.remove(key);
    }
    return this;
  }


  /**
   * Put a key/value pair in the JSONObject, but only if the key and the
   * value are both non-null, and only if there is not already a member
   * with that name.
   * @param key
   * @param value
   * @return {@code this}.
   * @throws RuntimeException if the key is a duplicate, or if
   * {@link #put(String,Object)} throws.
   */
  private JSONObject putOnce(String key, Object value) {
    if (key != null && value != null) {
      if (this.opt(key) != null) {
        throw new RuntimeException("Duplicate key \"" + key + "\"");
      }
      this.put(key, value);
    }
    return this;
  }


//  /**
//   * Put a key/value pair in the JSONObject, but only if the
//   * key and the value are both non-null.
//   * @param key   A key string.
//   * @param value An object which is the value. It should be of one of these
//   *  types: Boolean, Double, Integer, JSONArray, JSONObject, Long, String,
//   *  or the JSONObject.NULL object.
//   * @return this.
//   * @throws JSONException If the value is a non-finite number.
//   */
//  public JSONObject putOpt(String key, Object value) {
//    if (key != null && value != null) {
//      this.put(key, value);
//    }
//    return this;
//  }


  /**
   * Produce a string in double quotes with backslash sequences in all the
   * right places. A backslash will be inserted within </, producing <\/,
   * allowing JSON text to be delivered in HTML. In JSON text, a string
   * cannot contain a control character or an unescaped quote or backslash.
   * @param string A String
   * @return  A String correctly formatted for insertion in a JSON text.
   */
  static public String quote(String string) {
    StringWriter sw = new StringWriter();
    synchronized (sw.getBuffer()) {
      try {
        return quote(string, sw).toString();
      } catch (IOException ignored) {
        // will never happen - we are writing to a string writer
        return "";
      }
    }
  }

  static public Writer quote(String string, Writer w) throws IOException {
    if (string == null || string.length() == 0) {
      w.write("\"\"");
      return w;
    }

    char b;
    char c = 0;
    String hhhh;
    int i;
    int len = string.length();

    w.write('"');
    for (i = 0; i < len; i += 1) {
      b = c;
      c = string.charAt(i);
      switch (c) {
      case '\\':
      case '"':
        w.write('\\');
        w.write(c);
        break;
      case '/':
        if (b == '<') {
          w.write('\\');
        }
        w.write(c);
        break;
      case '\b':
        w.write("\\b");
        break;
      case '\t':
        w.write("\\t");
        break;
      case '\n':
        w.write("\\n");
        break;
      case '\f':
        w.write("\\f");
        break;
      case '\r':
        w.write("\\r");
        break;
      default:
        if (c < ' ' || (c >= '\u0080' && c < '\u00a0')
          || (c >= '\u2000' && c < '\u2100')) {
          w.write("\\u");
          hhhh = Integer.toHexString(c);
          w.write("0000", 0, 4 - hhhh.length());
          w.write(hhhh);
        } else {
          w.write(c);
        }
      }
    }
    w.write('"');
    return w;
  }


  /**
   * Remove a name and its value, if present.
   * @param key The name to be removed.
   * @return The value that was associated with the name,
   * or null if there was no value.
   */
  public Object remove(String key) {
    return this.map.remove(key);
  }


  /**
   * Try to convert a string into a number, boolean, or null. If the string
   * can't be converted, return the string.
   * @param string A String.
   * @return A simple JSON value.
   */
  static protected Object stringToValue(String string) {
    Double d;
    if (string.equals("")) {
      return string;
    }
    if (string.equalsIgnoreCase("true")) {
      return Boolean.TRUE;
    }
    if (string.equalsIgnoreCase("false")) {
      return Boolean.FALSE;
    }
    if (string.equalsIgnoreCase("null")) {
      return JSONObject.NULL;
    }

    /*
     * If it might be a number, try converting it.
     * If a number cannot be produced, then the value will just
     * be a string. Note that the plus and implied string
     * conventions are non-standard. A JSON parser may accept
     * non-JSON forms as long as it accepts all correct JSON forms.
     */

    char b = string.charAt(0);
    if ((b >= '0' && b <= '9') || b == '.' || b == '-' || b == '+') {
      try {
        if (string.indexOf('.') > -1 ||
          string.indexOf('e') > -1 || string.indexOf('E') > -1) {
          d = Double.valueOf(string);
          if (!d.isInfinite() && !d.isNaN()) {
            return d;
          }
        } else {
          Long myLong = Long.valueOf(string);
          if (myLong.longValue() == myLong.intValue()) {
            return Integer.valueOf(myLong.intValue());
          } else {
            return myLong;
          }
        }
      }  catch (Exception ignore) {
      }
    }
    return string;
  }


  /**
   * Throw an exception if the object is a NaN or infinite number.
   * @param o The object to test. If not Float or Double, accepted without
   *    exceptions.
   * @throws RuntimeException If o is infinite or NaN.
   */
  static protected void testValidity(Object o) {
    if (o != null) {
      if (o instanceof Double) {
        if (((Double)o).isInfinite() || ((Double)o).isNaN()) {
          throw new RuntimeException(
            "JSON does not allow non-finite numbers.");
        }
      } else if (o instanceof Float) {
        if (((Float)o).isInfinite() || ((Float)o).isNaN()) {
          throw new RuntimeException(
            "JSON does not allow non-finite numbers.");
        }
      }
    }
  }


//  /**
//   * Produce a JSONArray containing the values of the members of this
//   * JSONObject.
//   * @param names A JSONArray containing a list of key strings. This
//   * determines the sequence of the values in the result.
//   * @return A JSONArray of values.
//   * @throws JSONException If any of the values are non-finite numbers.
//   */
//  public JSONArray toJSONArray(JSONArray names) {
//    if (names == null || names.size() == 0) {
//      return null;
//    }
//    JSONArray ja = new JSONArray();
//    for (int i = 0; i < names.size(); i += 1) {
//      ja.append(this.opt(names.getString(i)));
//    }
//    return ja;
//  }


//  protected boolean save(OutputStream output) {
//    return save(PApplet.createWriter(output));
//  }


  public boolean save(File file, String options) {
    PrintWriter writer = PApplet.createWriter(file);
    boolean success = write(writer, options);
    writer.close();
    return success;
  }


  public boolean write(PrintWriter output) {
    return write(output, null);
  }


  public boolean write(PrintWriter output, String options) {
    int indentFactor = 2;
    if (options != null) {
      String[] opts = PApplet.split(options, ',');
      for (String opt : opts) {
        if (opt.equals("compact")) {
          indentFactor = -1;
        } else if (opt.startsWith("indent=")) {
          indentFactor = PApplet.parseInt(opt.substring(7), -2);
          if (indentFactor == -2) {
            throw new IllegalArgumentException("Could not read a number from " + opt);
          }
        } else {
          System.err.println("Ignoring " + opt);
        }
      }
    }
    output.print(format(indentFactor));
    output.flush();
    return true;
  }


  /**
   * Return the JSON data formatted with two spaces for indents.
   * Chosen to do this since it's the most common case (e.g. with println()).
   * Same as format(2). Use the format() function for more options.
   */
  @Override
  public String toString() {
    try {
      return format(2);
    } catch (Exception e) {
      return null;
    }
  }


  /**
   * Make a prettyprinted JSON text of this JSONObject.
   * <p>
   * Warning: This method assumes that the data structure is acyclical.
   * @param indentFactor The number of spaces to add to each level of
   *  indentation.
   * @return a printable, displayable, portable, transmittable
   *  representation of the object, beginning
   *  with <code>{</code>&nbsp;<small>(left brace)</small> and ending
   *  with <code>}</code>&nbsp;<small>(right brace)</small>.
   * @throws RuntimeException If the object contains an invalid number.
   */
  public String format(int indentFactor) {
    StringWriter w = new StringWriter();
    synchronized (w.getBuffer()) {
      return this.writeInternal(w, indentFactor, 0).toString();
    }
  }

  /**
   * Make a JSON text of an Object value. If the object has an
   * value.toJSONString() method, then that method will be used to produce
   * the JSON text. The method is required to produce a strictly
   * conforming text. If the object does not contain a toJSONString
   * method (which is the most common case), then a text will be
   * produced by other means. If the value is an array or Collection,
   * then a JSONArray will be made from it and its toJSONString method
   * will be called. If the value is a MAP, then a JSONObject will be made
   * from it and its toJSONString method will be called. Otherwise, the
   * value's toString method will be called, and the result will be quoted.
   *
   * <p>
   * Warning: This method assumes that the data structure is acyclical.
   * @param value The value to be serialized.
   * @return a printable, displayable, transmittable
   *  representation of the object, beginning
   *  with <code>{</code>&nbsp;<small>(left brace)</small> and ending
   *  with <code>}</code>&nbsp;<small>(right brace)</small>.
   * @throws RuntimeException If the value is or contains an invalid number.
   */
  static protected String valueToString(Object value) {
    if (value == null || value.equals(null)) {
      return "null";
    }
//    if (value instanceof JSONString) {
//      Object object;
//      try {
//        object = ((JSONString)value).toJSONString();
//      } catch (Exception e) {
//        throw new RuntimeException(e);
//      }
//      if (object instanceof String) {
//        return (String)object;
//      }
//      throw new RuntimeException("Bad value from toJSONString: " + object);
//    }
    if (value instanceof Number) {
      return numberToString((Number) value);
    }
    if (value instanceof Boolean || value instanceof JSONObject ||
      value instanceof JSONArray) {
      return value.toString();
    }
    if (value instanceof Map) {
      return new JSONObject(value).toString();
    }
    if (value instanceof Collection) {
      return new JSONArray(value).toString();
    }
    if (value.getClass().isArray()) {
      return new JSONArray(value).toString();
    }
    return quote(value.toString());
  }

  /**
   * Wrap an object, if necessary. If the object is null, return the NULL
   * object. If it is an array or collection, wrap it in a JSONArray. If
   * it is a map, wrap it in a JSONObject. If it is a standard property
   * (Double, String, et al) then it is already wrapped. Otherwise, if it
   * comes from one of the java packages, turn it into a string. And if
   * it doesn't, try to wrap it in a JSONObject. If the wrapping fails,
   * then null is returned.
   *
   * @param object The object to wrap
   * @return The wrapped value
   */
  static protected Object wrap(Object object) {
    try {
      if (object == null) {
        return NULL;
      }
      if (object instanceof JSONObject || object instanceof JSONArray  ||
        NULL.equals(object)      || /*object instanceof JSONString ||*/
        object instanceof Byte   || object instanceof Character  ||
        object instanceof Short  || object instanceof Integer    ||
        object instanceof Long   || object instanceof Boolean    ||
        object instanceof Float  || object instanceof Double     ||
        object instanceof String) {
        return object;
      }

      if (object instanceof Collection) {
        return new JSONArray(object);
      }
      if (object.getClass().isArray()) {
        return new JSONArray(object);
      }
      if (object instanceof Map) {
        return new JSONObject(object);
      }
      Package objectPackage = object.getClass().getPackage();
      String objectPackageName = objectPackage != null
        ? objectPackage.getName()
          : "";
        if (
          objectPackageName.startsWith("java.") ||
          objectPackageName.startsWith("javax.") ||
          object.getClass().getClassLoader() == null
          ) {
          return object.toString();
        }
        return new JSONObject(object);
    } catch(Exception exception) {
      return null;
    }
  }


//  /**
//   * Write the contents of the JSONObject as JSON text to a writer.
//   * For compactness, no whitespace is added.
//   * <p>
//   * Warning: This method assumes that the data structure is acyclical.
//   *
//   * @return The writer.
//   * @throws JSONException
//   */
//  protected Writer write(Writer writer) {
//    return this.write(writer, 0, 0);
//  }


  static final Writer writeValue(Writer writer, Object value,
                                 int indentFactor, int indent) throws IOException {
    if (value == null || value.equals(null)) {
      writer.write("null");
    } else if (value instanceof JSONObject) {
      ((JSONObject) value).writeInternal(writer, indentFactor, indent);
    } else if (value instanceof JSONArray) {
      ((JSONArray) value).writeInternal(writer, indentFactor, indent);
    } else if (value instanceof Map) {
      new JSONObject(value).writeInternal(writer, indentFactor, indent);
    } else if (value instanceof Collection) {
      new JSONArray(value).writeInternal(writer, indentFactor,
                                              indent);
    } else if (value.getClass().isArray()) {
      new JSONArray(value).writeInternal(writer, indentFactor, indent);
    } else if (value instanceof Number) {
      writer.write(numberToString((Number) value));
    } else if (value instanceof Boolean) {
      writer.write(value.toString());
      /*
    } else if (value instanceof JSONString) {
      Object o;
      try {
        o = ((JSONString) value).toJSONString();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      writer.write(o != null ? o.toString() : quote(value.toString()));
      */
    } else {
      quote(value.toString(), writer);
    }
    return writer;
  }


  static final void indent(Writer writer, int indent) throws IOException {
    for (int i = 0; i < indent; i += 1) {
      writer.write(' ');
    }
  }

  /**
   * Write the contents of the JSONObject as JSON text to a writer.
   * <p>
   * Warning: This method assumes that the data structure is acyclical.
   *
   * @return The writer.
   * @throws RuntimeException
   */
  protected Writer writeInternal(Writer writer, int indentFactor, int indent) {
    try {
      boolean commanate = false;
      final int length = this.size();
      Iterator keys = this.keyIterator();
      writer.write('{');

      int actualFactor = (indentFactor == -1) ? 0 : indentFactor;

      if (length == 1) {
        Object key = keys.next();
        writer.write(quote(key.toString()));
        writer.write(':');
        if (actualFactor > 0) {
          writer.write(' ');
        }
        //writeValue(writer, this.map.get(key), actualFactor, indent);
        writeValue(writer, this.map.get(key), indentFactor, indent);
      } else if (length != 0) {
        final int newIndent = indent + actualFactor;
        while (keys.hasNext()) {
          Object key = keys.next();
          if (commanate) {
            writer.write(',');
          }
          if (indentFactor != -1) {
            writer.write('\n');
          }
          indent(writer, newIndent);
          writer.write(quote(key.toString()));
          writer.write(':');
          if (actualFactor > 0) {
            writer.write(' ');
          }
          //writeValue(writer, this.map.get(key), actualFactor, newIndent);
          writeValue(writer, this.map.get(key), indentFactor, newIndent);
          commanate = true;
        }
        if (indentFactor != -1) {
          writer.write('\n');
        }
        indent(writer, indent);
      }
      writer.write('}');
      return writer;
    } catch (IOException exception) {
      throw new RuntimeException(exception);
    }
  }


//  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
//
//
//  class JSONException extends RuntimeException {
//
//    public JSONException(String message) {
//      super(message);
//    }
//
//    public JSONException(Throwable throwable) {
//      super(throwable);
//    }
//  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


//  /**
//   * Get the hex value of a character (base16).
//   * @param c A character between '0' and '9' or between 'A' and 'F' or
//   * between 'a' and 'f'.
//   * @return  An int between 0 and 15, or -1 if c was not a hex digit.
//   */
//  static protected int dehexchar(char c) {
//    if (c >= '0' && c <= '9') {
//      return c - '0';
//    }
//    if (c >= 'A' && c <= 'F') {
//      return c - ('A' - 10);
//    }
//    if (c >= 'a' && c <= 'f') {
//      return c - ('a' - 10);
//    }
//    return -1;
//  }


//  static class JSONTokener {
//    private long    character;
//    private boolean eof;
//    private long    index;
//    private long    line;
//    private char    previous;
//    private Reader  reader;
//    private boolean usePrevious;
//
//
//    /**
//     * Construct a JSONTokener from a Reader.
//     *
//     * @param reader     A reader.
//     */
//    public JSONTokener(Reader reader) {
//      this.reader = reader.markSupported()
//        ? reader
//          : new BufferedReader(reader);
//      this.eof = false;
//      this.usePrevious = false;
//      this.previous = 0;
//      this.index = 0;
//      this.character = 1;
//      this.line = 1;
//    }
//
//
//    /**
//     * Construct a JSONTokener from an InputStream.
//     */
//    public JSONTokener(InputStream inputStream) {
//      this(new InputStreamReader(inputStream));
//    }
//
//
//    /**
//     * Construct a JSONTokener from a string.
//     *
//     * @param s     A source string.
//     */
//    public JSONTokener(String s) {
//      this(new StringReader(s));
//    }
//
//
//    /**
//     * Back up one character. This provides a sort of lookahead capability,
//     * so that you can test for a digit or letter before attempting to parse
//     * the next number or identifier.
//     */
//    public void back() {
//      if (this.usePrevious || this.index <= 0) {
//        throw new RuntimeException("Stepping back two steps is not supported");
//      }
//      this.index -= 1;
//      this.character -= 1;
//      this.usePrevious = true;
//      this.eof = false;
//    }
//
//
//    public boolean end() {
//      return this.eof && !this.usePrevious;
//    }
//
//
//    /**
//     * Determine if the source string still contains characters that next()
//     * can consume.
//     * @return true if not yet at the end of the source.
//     */
//    public boolean more() {
//      this.next();
//      if (this.end()) {
//        return false;
//      }
//      this.back();
//      return true;
//    }
//
//
//    /**
//     * Get the next character in the source string.
//     *
//     * @return The next character, or 0 if past the end of the source string.
//     */
//    public char next() {
//      int c;
//      if (this.usePrevious) {
//        this.usePrevious = false;
//        c = this.previous;
//      } else {
//        try {
//          c = this.reader.read();
//        } catch (IOException exception) {
//          throw new RuntimeException(exception);
//        }
//
//        if (c <= 0) { // End of stream
//          this.eof = true;
//        c = 0;
//        }
//      }
//      this.index += 1;
//      if (this.previous == '\r') {
//        this.line += 1;
//        this.character = c == '\n' ? 0 : 1;
//      } else if (c == '\n') {
//        this.line += 1;
//        this.character = 0;
//      } else {
//        this.character += 1;
//      }
//      this.previous = (char) c;
//      return this.previous;
//    }
//
//
//    /**
//     * Consume the next character, and check that it matches a specified
//     * character.
//     * @param c The character to match.
//     * @return The character.
//     * @throws JSONException if the character does not match.
//     */
//    public char next(char c) {
//      char n = this.next();
//      if (n != c) {
//        throw new RuntimeException("Expected '" + c + "' and instead saw '" + n + "'");
//      }
//      return n;
//    }
//
//
//    /**
//     * Get the next n characters.
//     *
//     * @param n     The number of characters to take.
//     * @return      A string of n characters.
//     * @throws JSONException
//     *   Substring bounds error if there are not
//     *   n characters remaining in the source string.
//     */
//    public String next(int n) {
//      if (n == 0) {
//        return "";
//      }
//
//      char[] chars = new char[n];
//      int pos = 0;
//
//      while (pos < n) {
//        chars[pos] = this.next();
//        if (this.end()) {
//          throw new RuntimeException("Substring bounds error");
//        }
//        pos += 1;
//      }
//      return new String(chars);
//    }
//
//
//    /**
//     * Get the next char in the string, skipping whitespace.
//     * @throws JSONException
//     * @return  A character, or 0 if there are no more characters.
//     */
//    public char nextClean() {
//      for (;;) {
//        char c = this.next();
//        if (c == 0 || c > ' ') {
//          return c;
//        }
//      }
//    }
//
//
//    /**
//     * Return the characters up to the next close quote character.
//     * Backslash processing is done. The formal JSON format does not
//     * allow strings in single quotes, but an implementation is allowed to
//     * accept them.
//     * @param quote The quoting character, either
//     *      <code>"</code>&nbsp;<small>(double quote)</small> or
//     *      <code>'</code>&nbsp;<small>(single quote)</small>.
//     * @return      A String.
//     * @throws JSONException Unterminated string.
//     */
//    public String nextString(char quote) {
//      char c;
//      StringBuffer sb = new StringBuffer();
//      for (;;) {
//        c = this.next();
//        switch (c) {
//        case 0:
//        case '\n':
//        case '\r':
//          throw new RuntimeException("Unterminated string");
//        case '\\':
//          c = this.next();
//          switch (c) {
//          case 'b':
//            sb.append('\b');
//            break;
//          case 't':
//            sb.append('\t');
//            break;
//          case 'n':
//            sb.append('\n');
//            break;
//          case 'f':
//            sb.append('\f');
//            break;
//          case 'r':
//            sb.append('\r');
//            break;
//          case 'u':
//            sb.append((char)Integer.parseInt(this.next(4), 16));
//            break;
//          case '"':
//          case '\'':
//          case '\\':
//          case '/':
//            sb.append(c);
//            break;
//          default:
//            throw new RuntimeException("Illegal escape.");
//          }
//          break;
//        default:
//          if (c == quote) {
//            return sb.toString();
//          }
//          sb.append(c);
//        }
//      }
//    }
//
//
//    /**
//     * Get the text up but not including the specified character or the
//     * end of line, whichever comes first.
//     * @param  delimiter A delimiter character.
//     * @return   A string.
//     */
//    public String nextTo(char delimiter) {
//      StringBuffer sb = new StringBuffer();
//      for (;;) {
//        char c = this.next();
//        if (c == delimiter || c == 0 || c == '\n' || c == '\r') {
//          if (c != 0) {
//            this.back();
//          }
//          return sb.toString().trim();
//        }
//        sb.append(c);
//      }
//    }
//
//
//    /**
//     * Get the text up but not including one of the specified delimiter
//     * characters or the end of line, whichever comes first.
//     * @param delimiters A set of delimiter characters.
//     * @return A string, trimmed.
//     */
//    public String nextTo(String delimiters) {
//      char c;
//      StringBuffer sb = new StringBuffer();
//      for (;;) {
//        c = this.next();
//        if (delimiters.indexOf(c) >= 0 || c == 0 ||
//          c == '\n' || c == '\r') {
//          if (c != 0) {
//            this.back();
//          }
//          return sb.toString().trim();
//        }
//        sb.append(c);
//      }
//    }
//
//
//    /**
//     * Get the next value. The value can be a Boolean, Double, Integer,
//     * JSONArray, JSONObject, Long, or String, or the JSONObject.NULL object.
//     * @throws JSONException If syntax error.
//     *
//     * @return An object.
//     */
//    public Object nextValue() {
//      char c = this.nextClean();
//      String string;
//
//      switch (c) {
//      case '"':
//      case '\'':
//        return this.nextString(c);
//      case '{':
//        this.back();
//        return new JSONObject(this);
//      case '[':
//        this.back();
//        return new JSONArray(this);
//      }
//
//      /*
//       * Handle unquoted text. This could be the values true, false, or
//       * null, or it can be a number. An implementation (such as this one)
//       * is allowed to also accept non-standard forms.
//       *
//       * Accumulate characters until we reach the end of the text or a
//       * formatting character.
//       */
//
//      StringBuffer sb = new StringBuffer();
//      while (c >= ' ' && ",:]}/\\\"[{;=#".indexOf(c) < 0) {
//        sb.append(c);
//        c = this.next();
//      }
//      this.back();
//
//      string = sb.toString().trim();
//      if ("".equals(string)) {
//        throw new RuntimeException("Missing value");
//      }
//      return JSONObject.stringToValue(string);
//    }
//
//
//    /**
//     * Skip characters until the next character is the requested character.
//     * If the requested character is not found, no characters are skipped.
//     * @param to A character to skip to.
//     * @return The requested character, or zero if the requested character
//     * is not found.
//     */
//    public char skipTo(char to) {
//      char c;
//      try {
//        long startIndex = this.index;
//        long startCharacter = this.character;
//        long startLine = this.line;
//        this.reader.mark(1000000);
//        do {
//          c = this.next();
//          if (c == 0) {
//            this.reader.reset();
//            this.index = startIndex;
//            this.character = startCharacter;
//            this.line = startLine;
//            return c;
//          }
//        } while (c != to);
//      } catch (IOException exc) {
//        throw new RuntimeException(exc);
//      }
//
//      this.back();
//      return c;
//    }
//
//
//    /**
//     * Make a printable string of this JSONTokener.
//     *
//     * @return " at {index} [character {character} line {line}]"
//     */
//    @Override
//    public String toString() {
//      return " at " + this.index + " [character " + this.character + " line " +
//        this.line + "]";
//    }
//  }
}

package com.oracle.appbundler;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Stack;


/**
 * Class that handles Info.plist files. Started because of Java's brain dead 
 * XML implementation doesn't have simple indentation support, but evolved
 * from there to hide some of the Info.plist innards for cleaner code in the
 * AppBundlerTask object.  
 * @author fry at processing dot org
 */
class PropertyLister {
  static private final String XML_HEADER = "<?xml version=\"1.0\" ?>";
  static private final String PLIST_DTD = "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">";
  static private final String PLIST_TAG = "plist";
  static private final String PLIST_VERSION_ATTRIBUTE = "version";
  static private final String DICT_TAG = "dict";
  static private final String KEY_TAG = "key";
  static private final String ARRAY_TAG = "array";
  static private final String STRING_TAG = "string";

  PrintWriter writer;
  String indentSpaces = "  ";
  Stack<String> elements = new Stack<>();

  
  public PropertyLister(OutputStream output) throws UnsupportedEncodingException {
    OutputStreamWriter osw = new OutputStreamWriter(output, "UTF-8");
    writer = new PrintWriter(osw);
  }
  

  void writeStartDocument() {
    writer.println(XML_HEADER);
    writer.println(PLIST_DTD);

    // Begin root 'plist' element
    writeStartElement(PLIST_TAG, PLIST_VERSION_ATTRIBUTE, "1.0");
  }

  
  void writeEndDocument() {
    // End root 'plist' element
    writeEndElement();

    writer.flush();
    writer.close();
  }
  

  void writeStartElement(String element, String... args) {
    emitIndent();
    writer.print("<" + element);

    for (int i = 0; i < args.length; i += 2) {
      String attr = args[i];
      String value = args[i+1];
      writer.print(" " + attr + "=\"" + value + "\"");
    }
    writer.println(">");
    elements.push(element);
  }

  
  void writeStartElement(String element) {
    emitIndent();
    writer.println("<" + element + ">");
    elements.push(element);
  }

  
  void writeStartDictElement() {
    writeStartElement(DICT_TAG);
  }

  
  void writeStartArrayElement() {
    writeStartElement(ARRAY_TAG);
  }

  
  void writeEndElement() {
    emitOutdent();
    writer.println("</" + elements.pop() + ">");
  }

  
  void writeKey(String key) {
    emitSingle(KEY_TAG, key);
  }

  
  void writeString(String value) {
    emitSingle(STRING_TAG, value);
  }

  
  void writeBoolean(boolean value) {
    emitIndent();
    writer.println("<" + (value ? "true" : "false") + "/>");
  }

  
  void writeProperty(String property, String value) {
    writeKey(property);
    writeString(value);
  }

  
  private void emitSingle(String tag, String content) {
    emitIndent();
    writer.println("<" + tag + ">" + content + "</" + tag + ">");
  }

  
  private void emitIndent() {
    for (int i = 0; i < elements.size(); i++) {
      writer.print(indentSpaces);
    }
  }

  
  private void emitOutdent() {
    for (int i = 0; i < elements.size() - 1; i++) {
      writer.print(indentSpaces);
    }
  }
}
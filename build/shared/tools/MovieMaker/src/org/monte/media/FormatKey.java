/*
 * @(#)FormatKey.java  
 * 
 * Copyright (c) 2011 Werner Randelshofer, Goldau, Switzerland.
 * All rights reserved.
 * 
 * You may not use, copy or modify this file, except in compliance with the
 * license agreement you entered into with Werner Randelshofer.
 * For details see accompanying license terms.
 */
package org.monte.media;

import java.io.Serializable;

/**
 * A <em>FormatKey</em> provides type-safe access to an attribute of
 * a {@link Format}.
 * <p>
 * A format key has a name, a type and a value.
 * 
 * @author Werner Randelshofer
 * @version $Id: FormatKey.java 299 2013-01-03 07:40:18Z werner $
 */
public class FormatKey<T> implements Serializable, Comparable {

    public static final long serialVersionUID = 1L;
    /**
     * Holds a String representation of the attribute key.
     */
    private String key;
    /**
     * Holds a pretty name. This can be null, if the value is self-explaining.
     */
    private String name;
    /** This variable is used as a "type token" so that we can check for
     * assignability of attribute values at runtime.
     */
    private Class<T> clazz;
    
    /** Comment keys are ignored when matching two media formats with each other. */
    private boolean comment;

    /** Creates a new instance with the specified attribute key, type token class,
     * default value null, and allowing null values. */
    public FormatKey(String key, Class<T> clazz) {
        this(key, key, clazz);
    }

    /** Creates a new instance with the specified attribute key, type token class,
     * default value null, and allowing null values. */
    public FormatKey(String key, String name, Class<T> clazz) {
        this(key,name,clazz,false);
    }
    /** Creates a new instance with the specified attribute key, type token class,
     * default value null, and allowing null values. */
    public FormatKey(String key, String name, Class<T> clazz, boolean comment) {
        this.key = key;
        this.name = name;
        this.clazz = clazz;
        this.comment=comment;
    }

    /**
     * Returns the key string.
     * @return key string.
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the pretty name string.
     * @return name string.
     */
    public String getName() {
        return name;
    }

    /** Returns the key string. */
    @Override
    public String toString() {
        return key;
    }

    /**
     * Returns true if the specified value is assignable with this key.
     *
     * @param value
     * @return True if assignable.
     */
    public boolean isAssignable(Object value) {
        return clazz.isInstance(value);
    }

    public boolean isComment() {
        return comment;
    }
    

    public Class getValueClass() {
        return clazz;
    }

    @Override
    public int compareTo(Object o) {
        return compareTo((FormatKey) o);
    }

    public int compareTo(FormatKey that) {
        return this.key.compareTo(that.key);
    }
}

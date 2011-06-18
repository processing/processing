/**
 * Part of the GSVideo library: http://gsvideo.sourceforge.net/
 * Copyright (c) 2008-11 Andres Colubri 
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation, version 2.1.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General
 * Public License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307  USA
 */
 
package codeanticode.gsvideo;

import com.sun.jna.Library;
import com.sun.jna.Native;

/**
 * This JNA interface provides access to the environment variable-related functions in the C library.
 * How to use:
 * CLibrary clib = CLibrary.INSTANCE;		    	
 * String s = clib.getenv("DYLD_LIBRARY_PATH");
 */    
public interface CLibrary extends Library {
  CLibrary INSTANCE = (CLibrary)Native.loadLibrary("c", CLibrary.class);
    
  int setenv(String name, String value, int overwrite);
  String getenv(String name);
  int unsetenv(String name);
  int putenv(String string);
}

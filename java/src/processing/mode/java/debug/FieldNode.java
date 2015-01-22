/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org
  Copyright (c) 2012-15 The Processing Foundation

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License version 2
  as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation, Inc.
  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
*/

package processing.mode.java.debug;

import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.Field;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.Value;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Specialized {@link VariableNode} for representing fields. Overrides
 * {@link #setValue} to properly change the value of the encapsulated field.
 *
 * @author Martin Leopold <m@martinleopold.com>
 */
public class FieldNode extends VariableNode {
    protected Field field;
    protected ObjectReference obj;

    
    /**
     * Construct a {@link FieldNode}.
     *
     * @param name the name
     * @param type the type
     * @param value the value
     * @param field the field
     * @param obj a reference to the object containing the field
     */
    public FieldNode(String name, String type, Value value, Field field, ObjectReference obj) {
        super(name, type, value);
        this.field = field;
        this.obj = obj;
    }

    
    @Override
    public void setValue(Value value) {
        try {
            obj.setValue(field, value);
        } catch (InvalidTypeException ex) {
            Logger.getLogger(FieldNode.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotLoadedException ex) {
            Logger.getLogger(FieldNode.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.value = value;
    }
}

/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-16 The Processing Foundation

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  version 2, as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.mode.java.debug;

import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.Field;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.Value;

import processing.app.Messages;


/**
 * Specialized {@link VariableNode} for representing fields. Overrides
 * {@link #setValue} to properly change the value of the encapsulated field.
 */
public class FieldNode extends VariableNode {
  protected Field field;
  protected ObjectReference obj;


  /**
   * Construct a {@link FieldNode}.
   * @param obj a reference to the object containing the field
   */
  public FieldNode(String name, String type, Value value, Field field,
                   ObjectReference obj) {
    super(name, type, value);
    this.field = field;
    this.obj = obj;
  }


  @Override
  public void setValue(Value value) {
    try {
      obj.setValue(field, value);
    } catch (InvalidTypeException ite) {
      Messages.loge(null, ite);
    } catch (ClassNotLoadedException cnle) {
      Messages.loge(null, cnle);
    }
    this.value = value;
  }
}

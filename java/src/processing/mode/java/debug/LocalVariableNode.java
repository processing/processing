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
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.StackFrame;
import com.sun.jdi.Value;

import processing.app.Messages;


/**
 * Specialized {@link VariableNode} for representing local variables.
 * Overrides {@link #setValue} to properly change the value of the
 * encapsulated local variable.
 */
public class LocalVariableNode extends VariableNode {
  protected LocalVariable var;
  protected StackFrame frame;


  public LocalVariableNode(String name, String type, Value value, LocalVariable var, StackFrame frame) {
    super(name, type, value);
    this.var = var;
    this.frame = frame;
  }


  @Override
  public void setValue(Value value) {
    try {
      frame.setValue(var, value);
    } catch (InvalidTypeException | ClassNotLoadedException ex) {
      Messages.loge(null, ex);
    }
    this.value = value;
  }
}

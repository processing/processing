/*
 * Copyright (C) 2012 Martin Leopold <m@martinleopold.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package processing.mode.java2;

import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.StackFrame;
import com.sun.jdi.Value;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Specialized {@link VariableNode} for representing local variables. Overrides
 * {@link #setValue} to properly change the value of the encapsulated local
 * variable.
 *
 * @author Martin Leopold <m@martinleopold.com>
 */
public class LocalVariableNode extends VariableNode {

    protected LocalVariable var;
    protected StackFrame frame;

    /**
     * Construct a {@link LocalVariableNode}.
     *
     * @param name the name
     * @param type the type
     * @param value the value
     * @param var the local variable
     * @param frame the stack frame containing the local variable
     */
    public LocalVariableNode(String name, String type, Value value, LocalVariable var, StackFrame frame) {
        super(name, type, value);
        this.var = var;
        this.frame = frame;
    }

    @Override
    public void setValue(Value value) {
        try {
            frame.setValue(var, value);
        } catch (InvalidTypeException ex) {
            Logger.getLogger(LocalVariableNode.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotLoadedException ex) {
            Logger.getLogger(LocalVariableNode.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.value = value;
    }
}

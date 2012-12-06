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

import com.sun.jdi.ArrayReference;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.Value;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Specialized {@link VariableNode} for representing single fields in an array.
 * Overrides {@link #setValue} to properly change the value of the encapsulated
 * array field.
 *
 * @author Martin Leopold <m@martinleopold.com>
 */
public class ArrayFieldNode extends VariableNode {

    protected ArrayReference array;
    protected int index;

    /**
     * Construct an {@link ArrayFieldNode}.
     *
     * @param name the name
     * @param type the type
     * @param value the value
     * @param array a reference to the array
     * @param index the index inside the array
     */
    public ArrayFieldNode(String name, String type, Value value, ArrayReference array, int index) {
        super(name, type, value);
        this.array = array;
        this.index = index;
    }

    @Override
    public void setValue(Value value) {
        try {
            array.setValue(index, value);
        } catch (InvalidTypeException ex) {
            Logger.getLogger(ArrayFieldNode.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotLoadedException ex) {
            Logger.getLogger(ArrayFieldNode.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.value = value;
    }
}

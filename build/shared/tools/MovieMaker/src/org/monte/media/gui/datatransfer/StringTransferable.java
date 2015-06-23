/*
 * @(#)StringTransferable.java  1.0  22. August 2007
 *
 * Copyright (c) 2007 Werner Randelshofer, Goldau, Switzerland.
 * All rights reserved.
 *
 * You may not use, copy or modify this file, except in compliance with the
 * license agreement you entered into with Werner Randelshofer.
 * For details see accompanying license terms.
 */

package org.monte.media.gui.datatransfer;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.IOException;
/**
 * StringTransferable.
 * <p>
 * Note: This transferable should always be used in conjunction with 
 * PlainTextTransferable.
 * <p>
 * Usage:
 * <pre>
 * String text = "bla";
 * CompositeTransfer t = new CompositeTransferable();
 * t.add(new StringTransferable(text));
 * t.add(new PlainTextTransferable(text));
 * </pre>
 *
 * @author Werner Randelshofer
 * @version 1.0 22. August 2007 Created.
 */
public class StringTransferable extends AbstractTransferable {
    private String string;
    
    public StringTransferable(String string) {
        this(getDefaultFlavors(), string);
    }
    public StringTransferable(DataFlavor flavor, String string) {
        this(new DataFlavor[] { flavor }, string);
    }
    public StringTransferable(DataFlavor[] flavors, String string) {
        super(flavors);
        this.string = string;
    }
    
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (! isDataFlavorSupported(flavor)) {
            throw new UnsupportedFlavorException(flavor);
        }
        return string;
    }
    
    protected static DataFlavor[] getDefaultFlavors() {
        try {
            return new DataFlavor[] {
                new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType+";class=java.lang.String"),
                DataFlavor.stringFlavor
            };
        } catch (ClassNotFoundException cle) {
            InternalError ie = new InternalError(
                    "error initializing StringTransferable");
            ie.initCause(cle);
            throw ie;
        }
    }
}

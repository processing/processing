/*
 * @(#)PlainTextTransferable.java  1.1  2009-09-01
 *
 * Copyright (c) 2007-2009 Werner Randelshofer, Goldau, Switzerland.
 * All rights reserved.
 *
 * You may not use, copy or modify this file, except in compliance with the
 * license agreement you entered into with Werner Randelshofer.
 * For details see accompanying license terms.
 */

package org.monte.media.gui.datatransfer;

import java.awt.datatransfer.*;
import java.io.*;
/**
 * PlainTextTransferable.
 * <p>
 * Note: This transferable should (almost) always be used in conjunction with
 * PlainTextTransferable.
 *
 * @author Werner Randelshofer
 * @version 1.1 2009-09-01 Replaced use of deprecated class StringBufferInputStream.
 * <br>1.0 22. August 2007 Created.
 */
public class PlainTextTransferable extends AbstractTransferable {
    private String plainText;

    public PlainTextTransferable(String plainText) {
        this(getDefaultFlavors(), plainText);
    }
    public PlainTextTransferable(DataFlavor flavor, String plainText) {
        this(new DataFlavor[] { flavor }, plainText);
    }
    public PlainTextTransferable(DataFlavor[] flavors, String plainText) {
        super(flavors);
        this.plainText = plainText;
    }

    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (! isDataFlavorSupported(flavor)) {
            throw new UnsupportedFlavorException(flavor);
        }
        plainText = (plainText == null) ? "" : plainText;
        if (String.class.equals(flavor.getRepresentationClass())) {
            return plainText;
        } else if (Reader.class.equals(flavor.getRepresentationClass())) {
            return new StringReader(plainText);
        } else if (InputStream.class.equals(flavor.getRepresentationClass())) {
            String charsetName = flavor.getParameter("charset");
            return new ByteArrayInputStream(plainText.getBytes(charsetName==null?"UTF-8":charsetName));
            //return new StringBufferInputStream(plainText);
        } // fall through to unsupported

	throw new UnsupportedFlavorException(flavor);
    }

    protected static DataFlavor[] getDefaultFlavors() {
        try {
            return new DataFlavor[] {
                new DataFlavor("text/plain;class=java.lang.String"),
                new DataFlavor("text/plain;class=java.io.Reader"),
                new DataFlavor("text/plain;charset=unicode;class=java.io.InputStream")
            };
        } catch (ClassNotFoundException cle) {
            InternalError ie = new InternalError(
                    "error initializing PlainTextTransferable");
            ie.initCause(cle);
            throw ie;
        }
    }
}

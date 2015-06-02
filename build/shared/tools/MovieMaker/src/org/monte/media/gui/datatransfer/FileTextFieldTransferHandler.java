/*
 * @(#)FileTextFieldTransferHandler.java  1.2  2010-10-02
 *
 * Copyright (c) 2011 Werner Randelshofer, Goldau, Switzerland.
 * All rights reserved.
 * 
 * You may not use, copy or modify this file, except in compliance with the
 * license agreement you entered into with Werner Randelshofer.
 * For details see accompanying license terms.
 */
package org.monte.media.gui.datatransfer;

import java.awt.datatransfer.*;
import java.awt.im.InputContext;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.text.*;

/**
 * The FileTextFieldTransferHandler can be used to add drag and drop
 * support for JTextFields, which contain the path to a file.
 *
 * @author Werner Randelshofer
 * @version 1.2 2010-10-03 Adds support for file filter.
 * <br>1.1 2008-12-03 Added file selection mode.
 * <br>1.0 September 8, 2007 Created.
 */
public class FileTextFieldTransferHandler extends TransferHandler {

    private boolean shouldRemove;
    private JTextComponent exportComp;
    private int p0;
    private int p1;
    private int fileSelectionMode;
    private FileFilter fileFilter;

    /** Creates a new instance. */
    public FileTextFieldTransferHandler() {
        this(JFileChooser.FILES_ONLY);
    }

    /** Creates a new instance.
     * @param fileSelectionMode JFileChooser file selection mode.
     */
    public FileTextFieldTransferHandler(int fileSelectionMode) {
        this(fileSelectionMode, null);
    }

    /** Creates a new instance.
     * @param fileSelectionMode JFileChooser file selection mode.
     */
    public FileTextFieldTransferHandler(int fileSelectionMode, FileFilter filter) {
        this.fileFilter = filter;
        if (fileSelectionMode != JFileChooser.FILES_AND_DIRECTORIES
                && fileSelectionMode != JFileChooser.FILES_ONLY
                && fileSelectionMode != JFileChooser.DIRECTORIES_ONLY) {
            throw new IllegalArgumentException("illegal file selection mode:" + fileSelectionMode);
        }
        this.fileSelectionMode = fileSelectionMode;
    }

    @Override
    public boolean importData(JComponent comp, Transferable t) {
        JTextComponent c = (JTextComponent) comp;

        // if we are importing to the same component that we exported from
        // then don't actually do anything if the drop location is inside
        // the drag location and set shouldRemove to false so that exportDone
        // knows not to remove any data
        if (c == exportComp && c.getCaretPosition() >= p0 && c.getCaretPosition() <= p1) {
            shouldRemove = false;
            return true;
        }

        boolean imported = false;
        if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            InputContext ic = c.getInputContext();
            if (ic != null) {
                ic.endComposition();
            }

            try {
                java.util.List list = (List) t.getTransferData(DataFlavor.javaFileListFlavor);
                if (list.size() > 0) {
                    File file = (File) list.get(0);

                    switch (fileSelectionMode) {
                        case JFileChooser.FILES_AND_DIRECTORIES:
                            break;
                        case JFileChooser.FILES_ONLY:
                            if (file.isDirectory()) {
                                return false;
                            }
                            break;
                        case JFileChooser.DIRECTORIES_ONLY:
                            if (!file.isDirectory()) {
                                return false;
                            }
                            break;
                    }
                    if (fileFilter != null && !fileFilter.accept(file)) {
                        return false;
                    }
                    c.setText(file.getPath());
                }
                imported = true;
            } catch (UnsupportedFlavorException ex) {
                //   ex.printStackTrace();
            } catch (IOException ex) {
                //   ex.printStackTrace();
            }
        }

        if (!imported) {
            DataFlavor importFlavor = getImportFlavor(t.getTransferDataFlavors(), c);
            if (importFlavor != null) {
                InputContext ic = c.getInputContext();
                if (ic != null) {
                    ic.endComposition();
                }
                try {
                    String text = (String) t.getTransferData(DataFlavor.stringFlavor);
                    Reader r = importFlavor.getReaderForText(t);
                    boolean useRead = false;
                    handleReaderImport(r, c, useRead);
                    imported = true;
                } catch (UnsupportedFlavorException ex) {
                    //   ex.printStackTrace();
                } catch (BadLocationException ex) {
                    //   ex.printStackTrace();
                } catch (IOException ex) {
                    //   ex.printStackTrace();
                }
            }
        }
        return imported;
    }

    @Override
    protected Transferable createTransferable(JComponent comp) {

        CompositeTransferable t;

        JTextComponent c = (JTextComponent) comp;
        shouldRemove = true;
        p0 = c.getSelectionStart();
        p1 = c.getSelectionEnd();
        if (p0 != p1) {
            t = new CompositeTransferable();

            String text = c.getSelectedText();

            //LinkedList fileList = new LinkedList();
            //fileList.add(new File(text));
            //t.add(new JVMLocalObjectTransferable(DataFlavor.javaFileListFlavor, fileList));
            t.add(new StringTransferable(text));
            t.add(new PlainTextTransferable(text));
        } else {
            t = null;
        }


        return t;
    }

    @Override
    public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
        JTextComponent c = (JTextComponent) comp;
        if (!(c.isEditable() && c.isEnabled())) {
            return false;
        }

        for (DataFlavor flavor : transferFlavors) {
            if (flavor.isFlavorJavaFileListType()
                    || flavor.isFlavorTextType()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Try to find a flavor that can be used to import a Transferable.
     * The set of usable flavors are tried in the following order:
     * <ol>
     *     <li>First, an attempt to find a text/plain flavor is made.
     *     <li>Second, an attempt to find a flavor representing a String reference
     *         in the same VM is made.
     *     <li>Lastly, DataFlavor.stringFlavor is searched for.
     * </ol>
     */
    protected DataFlavor getImportFlavor(DataFlavor[] flavors, JTextComponent c) {
        DataFlavor plainFlavor = null;
        DataFlavor refFlavor = null;
        DataFlavor stringFlavor = null;

        for (int i = 0; i < flavors.length; i++) {
            String mime = flavors[i].getMimeType();
            if (mime.startsWith("text/plain")) {
                return flavors[i];
            } else if (refFlavor == null && mime.startsWith("application/x-java-jvm-local-objectref") && flavors[i].getRepresentationClass() == java.lang.String.class) {
                refFlavor = flavors[i];
            } else if (stringFlavor == null && flavors[i].equals(DataFlavor.stringFlavor)) {
                stringFlavor = flavors[i];
            }
        }
        if (refFlavor != null) {
            return refFlavor;
        } else if (stringFlavor != null) {
            return stringFlavor;
        }
        return null;
    }

    /**
     * Import the given stream data into the text component.
     */
    protected void handleReaderImport(Reader in, JTextComponent c, boolean useRead)
            throws BadLocationException, IOException {
        if (useRead) {
            int startPosition = c.getSelectionStart();
            int endPosition = c.getSelectionEnd();
            int length = endPosition - startPosition;
            EditorKit kit = c.getUI().getEditorKit(c);
            Document doc = c.getDocument();
            if (length > 0) {
                doc.remove(startPosition, length);
            }
            kit.read(in, doc, startPosition);
        } else {
            char[] buff = new char[1024];
            int nch;
            boolean lastWasCR = false;
            int last;
            StringBuffer sbuff = null;

            // Read in a block at a time, mapping \r\n to \n, as well as single
            // \r to \n.
            while ((nch = in.read(buff, 0, buff.length)) != -1) {
                if (sbuff == null) {
                    sbuff = new StringBuffer(nch);
                }
                last = 0;
                for (int counter = 0; counter < nch; counter++) {
                    switch (buff[counter]) {
                        case '\r':
                            if (lastWasCR) {
                                if (counter == 0) {
                                    sbuff.append('\n');
                                } else {
                                    buff[counter - 1] = '\n';
                                }
                            } else {
                                lastWasCR = true;
                            }
                            break;
                        case '\n':
                            if (lastWasCR) {
                                if (counter > (last + 1)) {
                                    sbuff.append(buff, last, counter - last - 1);
                                }
                                // else nothing to do, can skip \r, next write will
                                // write \n
                                lastWasCR = false;
                                last = counter;
                            }
                            break;
                        default:
                            if (lastWasCR) {
                                if (counter == 0) {
                                    sbuff.append('\n');
                                } else {
                                    buff[counter - 1] = '\n';
                                }
                                lastWasCR = false;
                            }
                            break;
                    }
                }
                if (last < nch) {
                    if (lastWasCR) {
                        if (last < (nch - 1)) {
                            sbuff.append(buff, last, nch - last - 1);
                        }
                    } else {
                        sbuff.append(buff, last, nch - last);
                    }
                }
            }
            if (lastWasCR) {
                sbuff.append('\n');
            }
            System.out.println("FileTextTransferHandler " + c.getSelectionStart() + ".." + c.getSelectionEnd());
            c.replaceSelection(sbuff != null ? sbuff.toString() : "");
        }
    }

    // --- TransferHandler methods ------------------------------------
    /**
     * This is the type of transfer actions supported by the source.  Some models are
     * not mutable, so a transfer operation of COPY only should
     * be advertised in that case.
     *
     * @param comp  The component holding the data to be transfered.  This
     *  argument is provided to enable sharing of TransferHandlers by
     *  multiple components.
     * @return  This is implemented to return NONE if the component is a JPasswordField
     *  since exporting data via user gestures is not allowed.  If the text component is
     *  editable, COPY_OR_MOVE is returned, otherwise just COPY is allowed.
     */
    @Override
    public int getSourceActions(JComponent comp) {
        JTextComponent c = (JTextComponent) comp;

        if (c instanceof JPasswordField
                && c.getClientProperty("JPasswordField.cutCopyAllowed")
                != Boolean.TRUE) {
            return NONE;
        }

        return c.isEditable() ? COPY_OR_MOVE : COPY;
    }

    /**
     * This method is called after data has been exported.  This method should remove
     * the data that was transfered if the action was MOVE.
     *
     * @param comp The component that was the source of the data.
     * @param data   The data that was transferred or possibly null
     *               if the action is <code>NONE</code>.
     * @param action The actual action that was performed.
     */
    @Override
    protected void exportDone(JComponent comp, Transferable data, int action) {
        JTextComponent c = (JTextComponent) comp;

        // only remove the text if shouldRemove has not been set to
        // false by importData and only if the action is a move
        if (shouldRemove && action == MOVE) {
            try {
                Document doc = c.getDocument();
                doc.remove(p0, p1 - p0);
            } catch (BadLocationException e) {
            }
        }
        exportComp = null;
    }

    /**
     * @return the fileFilter
     */
    public FileFilter getFileFilter() {
        return fileFilter;
    }

    /**
     * @param fileFilter the fileFilter to set
     */
    public void setFileFilter(FileFilter fileFilter) {
        this.fileFilter = fileFilter;
    }
}


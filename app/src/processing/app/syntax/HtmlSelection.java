package processing.app.syntax;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class HtmlSelection implements Transferable {

  private static List<DataFlavor> flavors = new ArrayList<DataFlavor>();

  static {
    try {
      flavors.add(DataFlavor.stringFlavor);
      flavors.add(new DataFlavor("text/html;class=java.lang.String"));
      flavors.add(new DataFlavor("text/html;class=java.io.Reader"));
      flavors
        .add(new DataFlavor(
                            "text/html;charset=unicode;class=java.io.InputStream"));
    } catch (ClassNotFoundException ex) {
      ex.printStackTrace();
    }
  }

  private String html;

  public HtmlSelection(String html) {
    this.html = html;
  }

  public DataFlavor[] getTransferDataFlavors() {
    return flavors.toArray(new DataFlavor[flavors.size()]);
  }

  public boolean isDataFlavorSupported(DataFlavor flavor) {
    return flavors.contains(flavor);
  }

  public Object getTransferData(DataFlavor flavor)
    throws UnsupportedFlavorException {
    if (flavor.equals(DataFlavor.stringFlavor)) {
      return html;
    } else if (String.class.equals(flavor.getRepresentationClass())) {
      return html;
    } else if (Reader.class.equals(flavor.getRepresentationClass())) {
      return new StringReader(html);
    } else if (InputStream.class.equals(flavor.getRepresentationClass())) {
      return new StringReader(html);
    }
    throw new UnsupportedFlavorException(flavor);
  }
}

import java.awt.*;
import java.io.*;


// might be nice to have option to save this to a file

// debugging this class is tricky.. if it's throwing 
// exceptions, don't take over System.err, and debug 
// while watching just System.out
// or just write directly to systemOut or systemErr

public class PdeEditorConsole extends Component {
  PdeEditor editor;

  static final byte CR = (byte)'\r';
  static final byte LF = (byte)'\n';

  int lineCount;
  int maxLineCount;
  String lines[];
  boolean isError[];
  int firstLine;

  byte cline[] = new byte[1024];
  byte clength;
  boolean cerror;

  Color bgColor;
  Color fgColorErr;
  Color fgColorOut;

  Font font;
  FontMetrics metrics;
  int ascent;
  int leading;

  Image offscreen;
  int sizeW, sizeH;
  int imageW, imageH;

  static final int HINSET = 6;
  static final int VINSET = 6;

  static PrintStream systemOut;
  static PrintStream systemErr;

  PrintStream consoleOut;
  PrintStream consoleErr;


  public PdeEditorConsole(PdeEditor editor) {
    this.editor = editor;

    lineCount = PdeBase.getInteger("editor.console.lines", 6);

    if (systemOut == null) {
      systemOut = System.out;
      systemErr = System.err;

      consoleOut = new PrintStream(new PdeEditorConsoleStream(this, false));
      consoleErr = new PrintStream(new PdeEditorConsoleStream(this, true));

      System.setOut(consoleOut);
      System.setErr(consoleErr);
    }

    maxLineCount = 1000;
    lines = new String[maxLineCount];
    isError = new boolean[maxLineCount];
    for (int i = 0; i < maxLineCount; i++) {
      lines[i] = "";
      isError[i] = false;
    }
    firstLine = 0;
  }


  public void update() {
    Graphics g = this.getGraphics();
    if (g != null) paint(g);
  }

  public void update(Graphics g) {
    paint(g);
  }

  public void paint(Graphics screen) {
    //systemOut.println("paint()");
    if (bgColor == null) {
      bgColor = PdeBase.getColor("editor.console.bgcolor", 
				   new Color(26, 26, 26));
      fgColorOut = PdeBase.getColor("editor.console.fgcolor.output", 
				      new Color(153, 153, 153));
      fgColorErr = PdeBase.getColor("editor.console.fgcolor.error", 
				      new Color(153, 0, 0));
      screen.setFont(font);
      metrics = screen.getFontMetrics();
      ascent = metrics.getAscent();
      leading = ascent + metrics.getDescent();
      //System.out.println(ascent + " " + leading);
    }

    Dimension size = getSize();
    if ((size.width != sizeW) || (size.height != sizeH)) {
      // component has been resized

      if ((size.width > imageW) || (size.height > imageH)) {
	// nix the image and recreate, it's too small
	offscreen = null;

      } else {
	// who cares, just resize
	sizeW = size.width; 
	sizeH = size.height;
	//setButtonBounds();
      }
    }
    //systemErr.println("size h, w = " + sizeW + " " + sizeH);

    if (offscreen == null) {
      sizeW = size.width;
      sizeH = size.height;
      //setButtonBounds();
      imageW = sizeW;
      imageH = sizeH;
      offscreen = createImage(imageW, imageH);
    }

    if (offscreen == null) return;
    Graphics g = offscreen.getGraphics();
    /*
    if (font == null) {
      font = PdeBase.getFont("editor.console.font", 
			       new Font("Monospaced", Font.PLAIN, 11));
      //font = new Font("SansSerif", Font.PLAIN, 10);
      g.setFont(font);
      metrics = g.getFontMetrics();
      ascent = metrics.getAscent();
    }
    */
    g.setFont(font);

    g.setColor(bgColor);
    g.fillRect(0, 0, imageW, imageH);

    for (int i = 0; i < lineCount; i++) {
      int ii = (firstLine + i) % maxLineCount;
      g.setColor(isError[ii] ? fgColorErr : fgColorOut);
      //System.out.println(leading);
      g.drawString(lines[ii], HINSET, VINSET + ascent + i*ascent);
    }

    screen.drawImage(offscreen, 0, 0, null);
  }

  public void write(byte b[], int offset, int length, boolean err) {
    if ((clength > 0) && (err != cerror)) {
      // advance the line because switching between err/out streams
      message(new String(cline, 0, clength), cerror, true);
      clength = 0;
    }
    int last = offset+length - 1;
    // starting a new line, so set its output type to out or err
    if (clength == 0) cerror = err;
    for (int i = offset; i <= last; i++) {
      if (b[i] == CR) {  // mac CR or win CRLF
	if ((i != last) && (b[i+1] == LF)) {
	  // if windows CRLF, skip the LF too
	  i++;
	}
	message(new String(cline, 0, clength), cerror, true);
	clength = 0;

      } else if (b[i] == LF) {  // unix LF only
	message(new String(cline, 0, clength), cerror, true);
	clength = 0;

      } else {
	if (cline.length == clength) {
	  byte temp[] = new byte[clength * 2];
	  System.arraycopy(cline, 0, temp, 0, clength);
	  cline = temp;
	}
	cline[clength++] = b[i];
      }
    }
    if (clength != 0) {
      message(new String(cline, 0, clength), cerror, false);
    }
  }

  public void message(String what, boolean err, boolean advance) {
    int currentLine = (firstLine + lineCount) % maxLineCount;
    lines[currentLine] = what;
    isError[currentLine] = err;

    if (advance) {
      firstLine = (firstLine + 1) % maxLineCount;
      //systemOut.println((err ? "ERR: " : "OUT: ") + what);
      systemOut.println(what);
    }
    update();
  }

  public Dimension getPreferredSize() {
    //systemOut.println("pref'd sizde");
    if (font == null) {
      font = PdeBase.getFont("editor.console.font", 
			       new Font("Monospaced", Font.PLAIN, 11));
      //font = new Font("SansSerif", Font.PLAIN, 10);
      //g.setFont(font);
      //metrics = g.getFontMetrics();
      metrics = Toolkit.getDefaultToolkit().getFontMetrics(font);
      ascent = metrics.getAscent();
    }

    //if (ascent == 0) {
      // no useful font information yet
      //System.out.println("PdeEditorConsole: setting size w/o metrics");
    //return new Dimension(300, 84);
    //} else {
      //System.out.println("PdeEditorConsole: got metrics, setting size" + 
      //		 new Dimension(300 + HINSET*2, 
      //			       leading*lineCount + VINSET*2));
    return new Dimension(300 + HINSET*2, 
			 ascent*lineCount + VINSET*2);
    //}
  }
}


class PdeEditorConsoleStream extends OutputStream {
  PdeEditorConsole parent;
  boolean err; // whether stderr or stdout
  byte single[] = new byte[1];

  public PdeEditorConsoleStream(PdeEditorConsole parent, boolean err) {
    this.parent = parent;
    this.err = err;
  }

  public void close() { }

  public void flush() { }

  public void write(byte b[]) {  // appears never to be used
    parent.write(b, 0, b.length, err);
  }

  public void write(byte b[], int offset, int length) {
    parent.write(b, offset, length, err);
    /*
    //System.out.println("leech2:");
    if (length >= 1) {
      int lastchar = b[offset + length - 1];
      if (lastchar == '\r') {
	length--;
      } else if (lastchar == '\n') {
	if (length >= 2) {
	  int secondtolastchar = b[offset + length - 2];
	  if (secondtolastchar == '\r') {
	    length -= 2;
	  } else { 
	    length--;
	  }
	} else {
	  length--;
	}
      }
      //if ((lastchar = '\r') || (lastchar == '\n')) length--;
    }
    //if (b[offset + length - 1] == '\r'
    parent.message("2: " + length + " " + new String(b, offset, length), err);
    */
  }

  public void write(int b) {
    single[0] = (byte)b;
    parent.write(single, 0, 1, err);
    //parent.message(String.valueOf((char)b), err);
  }
}

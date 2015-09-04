package processing.opengl;

import processing.opengl.PGraphicsOpenGL.GLResourceVertexBuffer;

// TODO: need to combine with PGraphicsOpenGL.VertexAttribute
public class VertexBuffer {
  static protected final int INIT_VERTEX_BUFFER_SIZE  = 256;
  static protected final int INIT_INDEX_BUFFER_SIZE   = 512;

  public int glId;
  int target;
  int elementSize;
  int ncoords;
  boolean index;

  protected PGL pgl;                // The interface between Processing and OpenGL.
  protected int context;            // The context that created this texture.
  private GLResourceVertexBuffer glres;

  VertexBuffer(PGraphicsOpenGL pg, int target, int ncoords, int esize) {
    this(pg, target, ncoords, esize, false);
  }

  VertexBuffer(PGraphicsOpenGL pg, int target, int ncoords, int esize, boolean index) {
    pgl = pg.pgl;
    context = pgl.createEmptyContext();

    this.target = target;
    this.ncoords = ncoords;
    this.elementSize = esize;
    this.index = index;
    create();
    init();
  }

  protected void create() {
    context = pgl.getCurrentContext();
    glres = new GLResourceVertexBuffer(this);
  }

  protected void init() {
    int size = index ? ncoords * INIT_INDEX_BUFFER_SIZE * elementSize :
                       ncoords * INIT_VERTEX_BUFFER_SIZE * elementSize;
    pgl.bindBuffer(target, glId);
    pgl.bufferData(target, size, null, PGL.STATIC_DRAW);
  }

  protected void dispose() {
    if (glres != null) {
      glres.dispose();
      glId = 0;
      glres = null;
    }
  }

  protected boolean contextIsOutdated() {
    boolean outdated = !pgl.contextIsCurrent(context);
    if (outdated) {
      dispose();
    }
    return outdated;
  }

}

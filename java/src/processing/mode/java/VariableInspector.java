/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org
  Copyright (c) 2012-15 The Processing Foundation

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License version 2
  as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation, Inc.
  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
*/

package processing.mode.java;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.table.TableColumn;
import javax.swing.tree.*;

import org.netbeans.swing.outline.*;

import com.sun.jdi.Value;

import processing.app.Language;
import processing.app.Messages;
import processing.app.Mode;
import processing.mode.java.debug.VariableNode;


public class VariableInspector extends JDialog {
  // The tray will be placed at this amount from the top of the editor window,
  // and extend to this amount from the bottom of the editor window.
  static final int VERTICAL_OFFSET = 64;
  static final int HORIZONTAL_OFFSET = 16;

  static final int DEFAULT_WIDTH = 300;
  static final int DEFAULT_HEIGHT = 400;

  /// the root node (invisible)
  protected DefaultMutableTreeNode rootNode;

  /// node for Processing built-in variables
  protected DefaultMutableTreeNode builtins;

  /// data model for the tree column
  protected DefaultTreeModel treeModel;

//  private JScrollPane scrollPane;

  protected Outline tree;
  protected OutlineModel model;

  protected List<DefaultMutableTreeNode> callStack;

  /// current local variables
  protected List<VariableNode> locals;

  /// all fields of the current this-object
  protected List<VariableNode> thisFields;

  /// declared i.e. non-inherited fields of this
  protected List<VariableNode> declaredThisFields;

  protected JavaEditor editor;
//  protected Debugger dbg;

  /// list of expanded tree paths. (using list to maintain the order of expansion)
  protected List<TreePath> expandedNodes = new ArrayList<>();


  public VariableInspector(final JavaEditor editor) {
    // As a JDialog, the menu bar comes from the Editor
    super(editor, "Variables");
    this.editor = editor;

    // Use the small toolbar style (at least on OS X)
    // https://developer.apple.com/library/mac/technotes/tn2007/tn2196.html#WINDOWS
    getRootPane().putClientProperty("Window.style", "small");

    // When clicking this window, keep the focus on the Editor window.
    // Slightly awkward on OS X, but less weird than the Editor losing focus?
    setFocusableWindowState(false);

    //setUndecorated(true);
    //editor.addComponentListener(new EditorFollower());

    //setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    Box box = Box.createVerticalBox();
//    box.add(createToolbar());
    box.add(createScrollPane());
    getContentPane().add(box);
    pack();

    int x = editor.getX() + editor.getWidth() + HORIZONTAL_OFFSET;
    if (x + getWidth() > getToolkit().getScreenSize().width) {
      // If it doesn't fit on screen, place it inside the editor window,
      // per the OS rules for window placement
      setLocationRelativeTo(editor);
    } else {
      // If it'll fit, place it to the right of the editor window
      setLocation(x, editor.getY() + VERTICAL_OFFSET);
    }

    /*
    bgColor = mode.getColor("buttons.bgcolor");
    statusFont = mode.getFont("buttons.status.font");
    statusColor = mode.getColor("buttons.status.color");
//    modeTitle = mode.getTitle().toUpperCase();
    modeTitle = mode.getTitle();
    modeTextFont = mode.getFont("mode.button.font");
    modeButtonColor = mode.getColor("mode.button.color");
    */
  }


  /*
  Container createToolbar() {
    final Mode mode = editor.getMode();
    Box box = Box.createHorizontalBox();

    continueButton =
      new EditorButton(mode, "theme/debug/continue",
                       Language.text("toolbar.debug.continue")) {
      @Override
      public void actionPerformed(ActionEvent e) {
        Logger.getLogger(VariableInspector.class.getName()).log(Level.INFO, "Invoked 'Continue' toolbar button");
        editor.debugger.continueDebug();
      }
    };
    box.add(continueButton);
    box.add(Box.createHorizontalStrut(GAP));

    stepButton =
      new EditorButton(mode, "theme/debug/step",
                       Language.text("toolbar.debug.step"),
                       Language.text("toolbar.debug.step_into")) {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (isShiftDown()) {
          Logger.getLogger(VariableInspector.class.getName()).log(Level.INFO, "Invoked 'Step Into' toolbar button");
          editor.debugger.stepInto();
        } else {
          Logger.getLogger(VariableInspector.class.getName()).log(Level.INFO, "Invoked 'Step' toolbar button");
          editor.debugger.stepOver();
        }
      }
    };
    box.add(stepButton);
    box.add(Box.createHorizontalStrut(GAP));

    breakpointButton =
      new EditorButton(mode, "theme/debug/breakpoint",
                       Language.text("toolbar.debug.toggle_breakpoints")) {
      @Override
      public void actionPerformed(ActionEvent e) {
        Logger.getLogger(VariableInspector.class.getName()).log(Level.INFO, "Invoked 'Toggle Breakpoint' toolbar button");
        editor.debugger.toggleBreakpoint();
      }
    };
    box.add(breakpointButton);
    box.add(Box.createHorizontalStrut(GAP));

    JLabel label = new JLabel();
    box.add(label);
    continueButton.setRolloverLabel(label);
    stepButton.setRolloverLabel(label);
    breakpointButton.setRolloverLabel(label);

    // the rest is all gaps
    box.add(Box.createHorizontalGlue());
    box.setBorder(new EmptyBorder(GAP, GAP, GAP, GAP));

    // prevent the toolbar from getting taller than its default
    box.setMaximumSize(new Dimension(getMaximumSize().width, getPreferredSize().height));
    return box;
  }
  */


  Container createScrollPane() {
    JScrollPane scrollPane = new JScrollPane();
    tree = new Outline();
    scrollPane.setViewportView(tree);

    /*
    GroupLayout layout = new GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                              .addGap(0, 400, Short.MAX_VALUE)
                              .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(scrollPane,
                                                      GroupLayout.DEFAULT_SIZE,
                                                      400, Short.MAX_VALUE)));
    layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addGap(0, 300, Short.MAX_VALUE)
                            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                      .addComponent(scrollPane,
                                                    GroupLayout.Alignment.TRAILING,
                                                    GroupLayout.DEFAULT_SIZE,
                                                    300, Short.MAX_VALUE)));
    pack();
    */

    // setup Outline
    rootNode = new DefaultMutableTreeNode("root");
    builtins = new DefaultMutableTreeNode("Processing");
    treeModel = new DefaultTreeModel(rootNode); // model for the tree column
    // model for all columns
    model =
      DefaultOutlineModel.createOutlineModel(treeModel, new VariableRowModel(),
                                             true,
                                             Language.text("debugger.name"));

    ExpansionHandler expansionHandler = new ExpansionHandler();
    model.getTreePathSupport().addTreeWillExpandListener(expansionHandler);
    model.getTreePathSupport().addTreeExpansionListener(expansionHandler);
    tree.setModel(model);
    tree.setRootVisible(false);
    tree.setRenderDataProvider(new OutlineRenderer());
    tree.setColumnHidingAllowed(false); // disable visible columns button (shows by default when right scroll bar is visible)
    tree.setAutoscrolls(false);

    // set custom renderer and editor for value column, since we are using a custom class for values (VariableNode)
    TableColumn valueColumn = tree.getColumnModel().getColumn(1);
    valueColumn.setCellRenderer(new ValueCellRenderer());
    valueColumn.setCellEditor(new ValueCellEditor());

    //System.out.addEmptyLine("renderer: " + tree.getDefaultRenderer(String.class).getClass());
    //System.out.addEmptyLine("editor: " + tree.getDefaultEditor(String.class).getClass());

    callStack = new ArrayList<>();
    locals = new ArrayList<>();
    thisFields = new ArrayList<>();
    declaredThisFields = new ArrayList<>();

    // Remove ugly (and unused) focus border on OS X
    scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
    scrollPane.setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
    return scrollPane;
  }


  /*
  protected void activateContinue() {
  }


  protected void deactivateContinue() {
  }


  protected void activateStep() {
  }


  protected void deactivateStep() {
  }
  */


  /*
  // Keeps the debug window adjacent the editor at all times.
  class EditorFollower implements ComponentListener {

    @Override
    public void componentShown(ComponentEvent e) {
      if (editor.isDebuggerEnabled()) {
//        updateBounds();
        setVisible(true);
      }
    }

    @Override
    public void componentHidden(ComponentEvent e) {
      if (isVisible()) {
        setVisible(false);
      }
    }

    @Override
    public void componentResized(ComponentEvent e) {
      if (isVisible()) {
        updateBounds();
      }
    }

    @Override
    public void componentMoved(ComponentEvent e) {
      if (isVisible()) {
        updateBounds();
      }
    }
  }


  private void updateBounds() {
    setBounds(editor.getX() + editor.getWidth(),
              editor.getY() + VERTICAL_OFFSET,
              getPreferredSize().width,
              editor.getHeight() - VERTICAL_OFFSET*2);
  }


  public void setVisible(boolean visible) {
    if (visible) {
      updateBounds();
    }
    super.setVisible(visible);
  }
  */


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Model for a Outline Row (excluding the tree column). Column 0 is "Value".
   * Column 1 is "Type". Handles setting and getting values. TODO: Maybe use a
   * TableCellRenderer instead of this to also have a different icon based on
   * expanded state. See:
   * http://kickjava.com/src/org/netbeans/swing/outline/DefaultOutlineCellRenderer.java.htm
   */
  protected class VariableRowModel implements RowModel {
    final String column0 = Language.text("debugger.value");
    final String column1 = Language.text("debugger.type");
    final String[] columnNames = { column0, column1 };
    final int[] editableTypes = {
      VariableNode.TYPE_BOOLEAN,
      VariableNode.TYPE_FLOAT,
      VariableNode.TYPE_INTEGER,
      VariableNode.TYPE_STRING,
      VariableNode.TYPE_FLOAT,
      VariableNode.TYPE_DOUBLE,
      VariableNode.TYPE_LONG,
      VariableNode.TYPE_SHORT,
      VariableNode.TYPE_CHAR
    };

    @Override
    public int getColumnCount() {
      return 1;
    }

    @Override
    public Object getValueFor(Object obj, int column) {
      if (obj instanceof VariableNode) {
        VariableNode var = (VariableNode) obj;
        if (column == 0) {
          // will be converted to an appropriate String by ValueCellRenderer
          return var;
        } else if (column == 1) {
          return var.getTypeName();
        }
      }
      return "";
    }

    @Override
    public Class<?> getColumnClass(int column) {
      if (column == 0) {
        return VariableNode.class;
      }
      return String.class;
    }

    @Override
    public boolean isCellEditable(Object o, int i) {
      if (i == 0 && o instanceof VariableNode) {
        VariableNode var = (VariableNode) o;
        //System.out.addEmptyLine("type: " + var.getTypeName());
        for (int type : editableTypes) {
          if (var.getType() == type) {
            return true;
          }
        }
      }
      return false;
    }

    @Override
    public void setValueFor(Object o, int i, Object o1) {
      VariableNode var = (VariableNode) o;
      String stringValue = (String) o1;
      Debugger dbg = editor.getDebugger();

      Value value = null;
      try {
        switch (var.getType()) {
        case VariableNode.TYPE_INTEGER:
          value = dbg.vm().mirrorOf(Integer.parseInt(stringValue));
          break;
        case VariableNode.TYPE_BOOLEAN:
          value = dbg.vm().mirrorOf(Boolean.parseBoolean(stringValue));
          break;
        case VariableNode.TYPE_FLOAT:
          value = dbg.vm().mirrorOf(Float.parseFloat(stringValue));
          break;
        case VariableNode.TYPE_STRING:
          value = dbg.vm().mirrorOf(stringValue);
          break;
        case VariableNode.TYPE_LONG:
          value = dbg.vm().mirrorOf(Long.parseLong(stringValue));
          break;
        case VariableNode.TYPE_BYTE:
          value = dbg.vm().mirrorOf(Byte.parseByte(stringValue));
          break;
        case VariableNode.TYPE_DOUBLE:
          value = dbg.vm().mirrorOf(Double.parseDouble(stringValue));
          break;
        case VariableNode.TYPE_SHORT:
          value = dbg.vm().mirrorOf(Short.parseShort(stringValue));
          break;
        case VariableNode.TYPE_CHAR:
          // TODO: better char support
          if (stringValue.length() > 0) {
            value = dbg.vm().mirrorOf(stringValue.charAt(0));
          }
        break;
        }
      } catch (NumberFormatException ex) {
        Messages.log(getClass().getName() + " invalid value entered for " +
                     var.getName() + " -> " + stringValue);
      }
      if (value != null) {
        var.setValue(value);
        Messages.log(getClass().getName() + " new value set: " + var.getStringValue());
      }
    }

    @Override
    public String getColumnName(int i) {
      return columnNames[i];
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Renderer for the tree portion of the outline component.
   * Handles icons, text color and tool tips.
   */
  class OutlineRenderer implements RenderDataProvider {
    Icon[][] icons;
    static final int ICON_SIZE = 16;

    OutlineRenderer() {
      icons = loadIcons("theme/variables-1x.png");
    }

    /**
     * Load multiple icons (horizontal) with multiple states (vertical) from
     * a single file.
     *
     * @param fileName file path in the mode folder.
     * @return a nested array (first index: icon, second index: state) or
     * null if the file wasn't found.
     */
    private ImageIcon[][] loadIcons(String fileName) {
      Mode mode = editor.getMode();
      File file = mode.getContentFile(fileName);
      if (!file.exists()) {
        Messages.log(getClass().getName(), "icon file not found: " + file.getAbsolutePath());
        return null;
      }
      Image allIcons = mode.loadImage(fileName);
      int cols = allIcons.getWidth(null) / ICON_SIZE;
      int rows = allIcons.getHeight(null) / ICON_SIZE;
      ImageIcon[][] iconImages = new ImageIcon[cols][rows];

      for (int i = 0; i < cols; i++) {
        for (int j = 0; j < rows; j++) {
          Image image = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
          Graphics g = image.getGraphics();
          g.drawImage(allIcons, -i * ICON_SIZE, -j * ICON_SIZE, null);
          iconImages[i][j] = new ImageIcon(image);
        }
      }
      return iconImages;
    }


    protected Icon getIcon(int type, int state) {
      if (type < 0 || type > icons.length - 1) {
        return null;
      }
      return icons[type][state];
    }


    protected VariableNode toVariableNode(Object o) {
      return (o instanceof VariableNode) ? (VariableNode) o : null;
    }


    protected Icon toGray(Icon icon) {
      if (icon instanceof ImageIcon) {
        Image grayImage = GrayFilter.createDisabledImage(((ImageIcon) icon).getImage());
        return new ImageIcon(grayImage);
      }
      // Cannot convert
      return icon;
    }


    @Override
    public String getDisplayName(Object o) {
      return o.toString();
    }


    @Override
    public boolean isHtmlDisplayName(Object o) {
      return false;
    }


    @Override
    public Color getBackground(Object o) {
      return null;
    }


    @Override
    public Color getForeground(Object o) {
      if (tree.isEnabled()) {
        return null; // default
      } else {
        return Color.GRAY;
      }
    }


    @Override
    public String getTooltipText(Object o) {
      VariableNode var = toVariableNode(o);
      if (var != null) {
        return var.getDescription();
      } else {
        return "";
      }
    }


    @Override
    public Icon getIcon(Object o) {
      VariableNode var = toVariableNode(o);
      if (var != null) {
        return getIcon(var.getType(), tree.isEnabled() ? 0 : 1);
      }
      if (o instanceof TreeNode) {
        UIDefaults defaults = UIManager.getDefaults();

        boolean isLeaf = model.isLeaf(o);
        Icon icon;
        if (isLeaf) {
          icon = defaults.getIcon("Tree.leafIcon");
        } else {
          icon = defaults.getIcon("Tree.closedIcon");
        }

        if (!tree.isEnabled()) {
          return toGray(icon);
        }
        return icon;
      }
      return null; // use standard icon
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  // TODO: could probably extend the simpler DefaultTableCellRenderer here
  /**
   * Renderer for the value column. Uses an italic font for null values and
   * Object values ("instance of ..."). Uses a gray color when tree is not
   * enabled.
   */
  protected class ValueCellRenderer extends DefaultOutlineCellRenderer {

    public ValueCellRenderer() {
      super();
    }

    protected void setItalic(boolean on) {
      setFont(new Font(getFont().getName(),
                       on ? Font.ITALIC : Font.PLAIN,
                       getFont().getSize()));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      setForeground(tree.isEnabled() ? Color.BLACK : Color.GRAY);

      if (value instanceof VariableNode) {
        VariableNode var = (VariableNode) value;

        setItalic(var.getValue() == null ||
                  var.getType() == VariableNode.TYPE_OBJECT);
        value = var.getStringValue();
      }
      setValue(value);
      return c;
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Editor for the value column. Will show an empty string when editing
   * String values that are null.
   */
  protected class ValueCellEditor extends DefaultCellEditor {

    public ValueCellEditor() {
      super(new JTextField());
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected,
                                                 int row, int column) {
      if (!(value instanceof VariableNode)) {
        return super.getTableCellEditorComponent(table, value, isSelected, row, column);
      }
      VariableNode var = (VariableNode) value;

      String strValue =
        (var.getType() == VariableNode.TYPE_STRING &&
         var.getValue() == null) ? "" : var.getStringValue();
      return super.getTableCellEditorComponent(table, strValue, isSelected, row, column);
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Handler for expanding and collapsing tree nodes.
   * Implements lazy loading of tree data (on expand).
   */
  protected class ExpansionHandler implements ExtTreeWillExpandListener, TreeExpansionListener {

    @Override
    public void treeWillExpand(TreeExpansionEvent tee) throws ExpandVetoException {
      //System.out.addEmptyLine("will expand");
      Object last = tee.getPath().getLastPathComponent();
      if (!(last instanceof VariableNode)) {
        return;
      }
      VariableNode var = (VariableNode) last;
      var.removeAllChildren(); // TODO: should we only load it once?
      var.addChildren(filterNodes(editor.getDebugger().getFields(var.getValue(), 0, true), new ThisFilter()));
    }

    @Override
    public void treeWillCollapse(TreeExpansionEvent tee) throws ExpandVetoException {
      //throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void treeExpanded(TreeExpansionEvent tee) {
      //System.out.addEmptyLine("expanded: " + tee.getPath());
      if (!expandedNodes.contains(tee.getPath())) {
        expandedNodes.add(tee.getPath());
      }
    }

    @Override
    public void treeCollapsed(TreeExpansionEvent tee) {
      //System.out.addEmptyLine("collapsed: " + tee.getPath());

      // first remove all children of collapsed path
      // this makes sure children do not appear before parents in the list.
      // (children can't be expanded before their parents)
      List<TreePath> removalList = new ArrayList<>();
      for (TreePath path : expandedNodes) {
        if (path.getParentPath().equals(tee.getPath())) {
          removalList.add(path);
        }
      }
      for (TreePath path : removalList) {
        expandedNodes.remove(path);
      }
      // remove collapsed path
      expandedNodes.remove(tee.getPath());
    }

    @Override
    public void treeExpansionVetoed(TreeExpansionEvent tee, ExpandVetoException eve) {
      //System.out.addEmptyLine("expansion vetoed");
      // nop
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  // removed in 3.0a9, doesn't seem to be used?
//  protected static void run(final VariableInspector vi) {
//    EventQueue.invokeLater(new Runnable() {
//      @Override
//      public void run() {
//        vi.setVisible(true);
//      }
//    });
//  }


  /*
  public DefaultMutableTreeNode getRootNode() {
    return rootNode;
  }
  */


  /**
   * Unlock the inspector window. Rebuild after this to avoid ... dots in the
   * trees labels
   */
  public void unlock() {
    tree.setEnabled(true);
  }


  /**
   * Lock the inspector window. Cancels open edits.
   */
  public void lock() {
    if (tree.getCellEditor() != null) {
      //tree.getCellEditor().stopCellEditing(); // force quit open edit
      tree.getCellEditor().cancelCellEditing(); // cancel an open edit
    }
    tree.setEnabled(false);
  }


  /**
   * Reset the inspector windows data. Rebuild after this to make changes
   * visible.
   */
  public void reset() {
    rootNode.removeAllChildren();
    // clear local data for good measure (in case someone rebuilds)
    callStack.clear();
    locals.clear();
    thisFields.clear();
    declaredThisFields.clear();
    expandedNodes.clear();
    // update
    treeModel.nodeStructureChanged(rootNode);
  }


  /**
   * Update call stack data.
   *
   * @param nodes a list of nodes that represent the call stack.
   * @param title the title to be used when labeling or otherwise grouping
   * call stack data.
   */
  public void updateCallStack(List<DefaultMutableTreeNode> nodes, String title) {
    callStack = nodes;
  }


  /**
   * Update locals data.
   *
   * @param nodes a list of {@link VariableNode} to be shown as local
   * variables in the inspector.
   * @param title the title to be used when labeling or otherwise grouping
   * locals data.
   */
  public void updateLocals(List<VariableNode> nodes, String title) {
    locals = nodes;
  }


  /**
   * Update this-fields data.
   *
   * @param nodes a list of {@link VariableNode}s to be shown as this-fields
   * in the inspector.
   * @param title the title to be used when labeling or otherwise grouping
   * this-fields data.
   */
  public void updateThisFields(List<VariableNode> nodes, String title) {
    thisFields = nodes;
  }


  /**
   * Update declared (non-inherited) this-fields data.
   *
   * @param nodes a list of {@link VariableNode}s to be shown as declared
   * this-fields in the inspector.
   * @param title the title to be used when labeling or otherwise grouping
   * declared this-fields data.
   */
  public void updateDeclaredThisFields(List<VariableNode> nodes, String title) {
    declaredThisFields = nodes;
  }


  /**
   * Rebuild the outline tree from current data. Uses the data provided by
   * {@link #updateCallStack}, {@link #updateLocals}, {@link #updateThisFields}
   * and {@link #updateDeclaredThisFields}
   */
  public void rebuild() {
    rootNode.removeAllChildren();

    // add all locals to root
    addAllNodes(rootNode, locals);

    // add non-inherited this fields
    addAllNodes(rootNode, filterNodes(declaredThisFields, new LocalHidesThisFilter(locals, LocalHidesThisFilter.MODE_PREFIX)));

    // add p5 builtins in a new folder
    builtins.removeAllChildren();
    addAllNodes(builtins, filterNodes(thisFields, new P5BuiltinsFilter()));
    if (builtins.getChildCount() > 0) { // skip builtins in certain situations e.g. in pure java tabs.
      rootNode.add(builtins);
    }

    // notify tree (using model) changed a node and its children
    // http://stackoverflow.com/questions/2730851/how-to-update-jtree-elements
    // needs to be done before expanding paths!
    treeModel.nodeStructureChanged(rootNode);

    // handle node expansions
    for (TreePath path : expandedNodes) {
      //System.out.addEmptyLine("re-expanding: " + path);
      path = synthesizePath(path);
      if (path != null) {
        tree.expandPath(path);
      } else {
        //System.out.addEmptyLine("couldn't synthesize path");
      }
    }

    // this expansion causes problems when sorted and stepping
    //tree.expandPath(new TreePath(new Object[]{rootNode, builtins}));
  }


  /**
   * Re-build a {@link TreePath} from a previous path using equals-checks
   * starting at the root node. This is used to use paths from previous trees
   * for use on the current tree.
   * @param path the path to synthesize.
   * @return the rebuilt path, usable on the current tree.
   */
  protected TreePath synthesizePath(TreePath path) {
    //System.out.addEmptyLine("synthesizing: " + path);
    if (path.getPathCount() == 0 || !rootNode.equals(path.getPathComponent(0))) {
      return null;
    }
    Object[] newPath = new Object[path.getPathCount()];
    newPath[0] = rootNode;
    TreeNode currentNode = rootNode;
    for (int i = 0; i < path.getPathCount() - 1; i++) {
      // get next node
      for (int j = 0; j < currentNode.getChildCount(); j++) {
        TreeNode nextNode = currentNode.getChildAt(j);
        if (nextNode.equals(path.getPathComponent(i + 1))) {
          currentNode = nextNode;
          newPath[i + 1] = nextNode;
          //System.out.addEmptyLine("found node " + (i+1) + ": " + nextNode);
          break;
        }
      }
      if (newPath[i + 1] == null) {
        //System.out.addEmptyLine("didn't find node");
        return null;
      }
    }
    return new TreePath(newPath);
  }


  /**
   * Filter a list of nodes using a {@link VariableNodeFilter}.
   * @param nodes the list of nodes to filter.
   * @param filter the filter to be used.
   * @return the filtered list.
   */
  protected List<VariableNode> filterNodes(List<VariableNode> nodes, VariableNodeFilter filter) {
    List<VariableNode> filtered = new ArrayList<>();
    for (VariableNode node : nodes) {
      if (filter.accept(node)) {
        filtered.add(node);
      }
    }
    return filtered;
  }


  protected void addAllNodes(DefaultMutableTreeNode root, List<? extends MutableTreeNode> nodes) {
    for (MutableTreeNode node : nodes) {
      root.add(node);
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public interface VariableNodeFilter {

    /** Check whether the filter accepts a {@link VariableNode}. */
    public boolean accept(VariableNode var);
  }


  /**
   * A {@link VariableNodeFilter} that accepts Processing built-in variable
   * names.
   */
  public class P5BuiltinsFilter implements VariableNodeFilter {

    protected String[] p5Builtins = {
      "focused",
      "frameCount",
      "frameRate",
      "height",
      "online",
      "screen",
      "width",
      "mouseX",
      "mouseY",
      "pmouseX",
      "pmouseY",
      "key",
      "keyCode",
      "keyPressed"
    };

    @Override
    public boolean accept(VariableNode var) {
      return Arrays.asList(p5Builtins).contains(var.getName());
    }
  }


  /**
   * A {@link VariableNodeFilter} that rejects implicit this references.
   * (Names starting with "this$")
   */
  public class ThisFilter implements VariableNodeFilter {

    @Override
    public boolean accept(VariableNode var) {
      return !var.getName().startsWith("this$");
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * A {@link VariableNodeFilter} that either rejects this-fields if hidden by
   * a local, or prefixes its name with "this."
   */
  public class LocalHidesThisFilter implements VariableNodeFilter {
    // Reject a this-field if hidden by a local.
    public static final int MODE_HIDE = 0; // don't show hidden this fields

    // Prefix a this-fields name with "this." if hidden by a local.
    public static final int MODE_PREFIX = 1;

    protected List<VariableNode> locals;
    protected int mode;

    /**
     * Construct a {@link LocalHidesThisFilter}.
     * @param locals a list of locals to check against
     * @param mode either {@link #MODE_HIDE} or {@link #MODE_PREFIX}
     */
    public LocalHidesThisFilter(List<VariableNode> locals, int mode) {
      this.locals = locals;
      this.mode = mode;
    }

    @Override
    public boolean accept(VariableNode var) {
      // check if the same name appears in the list of locals i.e. the local hides the field
      for (VariableNode local : locals) {
        if (var.getName().equals(local.getName())) {
          switch (mode) {
          case MODE_PREFIX:
            var.setName("this." + var.getName());
            return true;
          case MODE_HIDE:
            return false;
          }
        }
      }
      return true;
    }
  }
}

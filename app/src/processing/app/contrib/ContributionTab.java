/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2013 The Processing Foundation
  Copyright (c) 2011-12 Ben Fry and Casey Reas

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License version 2
  as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License along
  with this program; if not, write to the Free Software Foundation, Inc.
  59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
package processing.app.contrib;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import java.awt.*;

import javax.swing.border.*;

import processing.app.*;
import processing.app.ui.Editor;
import processing.app.ui.Toolkit;


public class ContributionTab {
  static final String ANY_CATEGORY = Language.text("contrib.all");

  ContributionType contributionType;
  ContributionManagerDialog contributionManagerDialog;
  JPanel panel;
  String title;
  ContributionFilter filter;
  JComboBox<String> categoryChooser;
  JScrollPane scrollPane;
  ContributionListPanel contributionListPanel;
  StatusPanel statusPanel;
  FilterField filterField;
  JButton restartButton;
  JLabel categoryLabel;
  JLabel loaderLabel;
  
  JPanel errorPanel;
  JTextPane errorMessage;
  JButton tryAgainButton;
  JButton closeButton;

  // the calling editor, so updates can be applied
  Editor editor;
  String category;
  ContributionListing contribListing;
  
  JProgressBar progressBar;


  public ContributionTab(ContributionType type,ContributionManagerDialog contributionManagerDialog) {
    if (type == null) {
      title = Language.text("contrib.manager_title.update");
      filter = ContributionType.createUpdateFilter();
    } else {
      if (type == ContributionType.MODE) {
        title = Language.text("contrib.manager_title.mode");
      }
      else if (type == ContributionType.TOOL) {
        title = Language.text("contrib.manager_title.tool");
      }
      else if (type == ContributionType.LIBRARY) {
        title = Language.text("contrib.manager_title.library");
      }
      else if (type == ContributionType.EXAMPLES) {
        title = Language.text("contrib.manager_title.examples");
      }

      filter = type.createFilter();
    }
    this.contributionType = type;
    this.contributionManagerDialog = contributionManagerDialog;
    contribListing = ContributionListing.getInstance();
    if (contributionType == null) {
      contributionListPanel = new UpdateContribListingPanel(this, filter);
      statusPanel = new UpdateStatusPanel(450, this);
    } else {
      statusPanel = new StatusPanel(450,this);
      contributionListPanel = new ContributionListPanel(this, filter);
    }
    contribListing.addContributionListener(contributionListPanel);
  }


  public boolean hasUpdates() {
    return contribListing.hasUpdates();
  }


  public boolean hasUpdates(Base base) {
    return contribListing.hasUpdates(base);
  }

//  protected JPanel makeTextPanel(String text) {
//    JPanel panel = new JPanel(false);
//    JLabel filler = new JLabel(text);
//    filler.setHorizontalAlignment(JLabel.CENTER);
//    panel.setLayout(new GridLayout(1, 1));
//    panel.add(filler);
//    return panel;
//  }
  
  


  public void showFrame(final Editor editor, boolean activateErrorPanel,
                        final boolean isLoading) {
    this.editor = editor;
    if (panel == null) {
      setLayout(editor, activateErrorPanel, isLoading);
    }
    contributionListPanel.setVisible(!isLoading);
    loaderLabel.setVisible(isLoading);
    errorPanel.setVisible(activateErrorPanel);
    panel.validate();
    panel.repaint();
  }


  public void setLayout(final Editor editor, boolean activateErrorPanel, boolean isLoading) {
    if(panel == null){
      progressBar = new JProgressBar();
      progressBar.setVisible(false);
      createComponents();
      panel = new JPanel(false);
      loaderLabel = new JLabel(Toolkit.getLibIcon("icons/loader.gif"));
      loaderLabel.setOpaque(false);
      loaderLabel.setBackground(Color.WHITE);
    }
    
    /*restartButton = new JButton(Language.text("contrib.restart"));
    restartButton.setVisible(false);
    restartButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent arg0) {

        Iterator<Editor> iter = editor.getBase().getEditors().iterator();
        while (iter.hasNext()) {
          Editor ed = iter.next();
          if (ed.getSketch().isModified()) {
            int option = Base
              .showYesNoQuestion(editor, title,
                                 Language.text("contrib.unsaved_changes"),
                                 Language.text("contrib.unsaved_changes.prompt"));

            if (option == JOptionPane.NO_OPTION)
              return;
            else
              break;
          }
        }

        // Thanks to http://stackoverflow.com/a/4160543
        StringBuilder cmd = new StringBuilder();
        cmd.append(System.getProperty("java.home") + File.separator + "bin"
          + File.separator + "java ");
        for (String jvmArg : ManagementFactory.getRuntimeMXBean()
          .getInputArguments()) {
          cmd.append(jvmArg + " ");
        }
        cmd.append("-cp ")
          .append(ManagementFactory.getRuntimeMXBean().getClassPath())
          .append(" ");
        cmd.append(Base.class.getName());

        try {
          Runtime.getRuntime().exec(cmd.toString());
          System.exit(0);
        } catch (IOException e) {
          e.printStackTrace();
        }

      }

    });*/

 
    GroupLayout layout = new GroupLayout(panel);
    panel.setLayout(layout);
    layout.setAutoCreateContainerGaps(true);
    layout.setAutoCreateGaps(true);
    layout.setHorizontalGroup(layout
      .createParallelGroup(GroupLayout.Alignment.CENTER)
      .addGroup(layout.createSequentialGroup()
                  .addComponent(filterField)
                  .addComponent(categoryChooser, GroupLayout.PREFERRED_SIZE,
                                GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
      .addComponent(loaderLabel).addComponent(contributionListPanel)
      .addComponent(errorPanel).addComponent(statusPanel));
    
    layout.setVerticalGroup(layout
      .createSequentialGroup()
      .addGroup(layout
                  .createParallelGroup(GroupLayout.Alignment.CENTER)
                  .addComponent(categoryChooser, GroupLayout.PREFERRED_SIZE,
                                GroupLayout.DEFAULT_SIZE,
                                GroupLayout.PREFERRED_SIZE)
                  .addComponent(filterField))
      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                  .addComponent(loaderLabel)
                  .addComponent(contributionListPanel))
      .addComponent(errorPanel)
      .addComponent(statusPanel, GroupLayout.PREFERRED_SIZE,
                    GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE));
    layout.linkSize(SwingConstants.VERTICAL, categoryChooser, filterField);
    layout.setHonorsVisibility(contributionListPanel, false);
 }


  /** Creates and arranges the Swing components in the dialog. 
   */
  private void createComponents() {

      categoryLabel = new JLabel(Language.text("contrib.category"));

      categoryChooser = new JComboBox<String>();
      categoryChooser.setMaximumRowCount(20);
      
      updateCategoryChooser();
      
      categoryChooser.addItemListener(new ItemListener() {
        public void itemStateChanged(ItemEvent e) {
          category = (String) categoryChooser.getSelectedItem();
          if (ContributionManagerDialog.ANY_CATEGORY.equals(category)) {
            category = null;
          }
          filterLibraries(category, filterField.filters);
          contributionListPanel.updateColors();
        }
      });

      filterField = new FilterField();
      
      buildErrorPanel();
     
  }
  private void buildErrorPanel(){
    errorPanel = new JPanel();
    GroupLayout layout = new GroupLayout(errorPanel);
    layout.setAutoCreateGaps(true);
    layout.setAutoCreateContainerGaps(true);
    errorPanel.setLayout(layout);
    errorMessage = new JTextPane();
    errorMessage.setEditable(false);
    errorMessage.setText("Could not connect to the Processing server. "
      + "Contributions cannot be installed or updated without an Internet connection. "
      + "Please verify your network connection again, then try connecting again.");
    errorMessage.setMaximumSize(new Dimension(450, 50));
    errorMessage.setOpaque(false);
    
    StyledDocument doc = errorMessage.getStyledDocument();
    SimpleAttributeSet center = new SimpleAttributeSet();
    StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
    doc.setParagraphAttributes(0, doc.getLength(), center, false);
    
    
    closeButton = new JButton("X");
    closeButton.setOpaque(false);
    closeButton.addActionListener(new ActionListener() {
      
      @Override
      public void actionPerformed(ActionEvent e) {
        contributionManagerDialog.makeAndShowTab(false, false);
      }
    });
    tryAgainButton = new JButton("Try Again");
    tryAgainButton.addActionListener(new ActionListener() {
      
      @Override
      public void actionPerformed(ActionEvent e) {
        contributionManagerDialog.makeAndShowTab(false, true);
        contributionManagerDialog.downloadAndUpdateContributionListing(editor.getBase());
      }
    });
    layout.setHorizontalGroup(layout
      .createSequentialGroup()
      .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,
                       GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                  .addComponent(errorMessage).addComponent(tryAgainButton))
      .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,
                       GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
      .addComponent(closeButton));
    layout.setVerticalGroup(layout
      .createSequentialGroup()
      .addGroup(layout.createParallelGroup().addComponent(errorMessage)
                  .addComponent(closeButton)).addComponent(tryAgainButton));
    errorPanel.setBackground(Color.BLUE);
  }



  protected void updateCategoryChooser() {
    if (categoryChooser != null) {
      ArrayList<String> categories;
      categoryChooser.removeAllItems();
      categories = new ArrayList<String>(contribListing.getCategories(filter));
//      for (int i = 0; i < categories.size(); i++) {
//        System.out.println(i + " category: " + categories.get(i));
//      }
      Collections.sort(categories);
//    categories.add(0, ContributionManagerDialog.ANY_CATEGORY);
      boolean categoriesFound = false;
      categoryChooser.addItem(ContributionManagerDialog.ANY_CATEGORY);
      for (String s : categories) {
        categoryChooser.addItem(s);
        if (!s.equals("Unknown")) {
          categoriesFound = true;
        }
      }
      categoryChooser.setEnabled(categoriesFound);
    }
  }


  protected void filterLibraries(String category, List<String> filters) {
    List<Contribution> filteredLibraries =
      contribListing.getFilteredLibraryList(category, filters);
    contributionListPanel.filterLibraries(filteredLibraries);
  }

/*
  protected void filterLibraries(String category, List<String> filters, boolean isCompatibilityFilter) {
    List<Contribution> filteredLibraries =
      contribListing.getFilteredLibraryList(category, filters);
    filteredLibraries = contribListing.getCompatibleContributionList(filteredLibraries, isCompatibilityFilter);
    contributionListPanel.filterLibraries(filteredLibraries);
  }*/


  protected void updateContributionListing() {
    if (editor != null) {
      List<Contribution> contributions = new ArrayList<Contribution>();

      List<Library> libraries =
        new ArrayList<Library>(editor.getMode().contribLibraries);
      contributions.addAll(libraries);

      //ArrayList<ToolContribution> tools = editor.contribTools;
      List<ToolContribution> tools = editor.getToolContribs();
      contributions.addAll(tools);

      List<ModeContribution> modes = editor.getBase().getModeContribs();
      contributions.addAll(modes);

      List<ExamplesContribution> examples = editor.getBase().getExampleContribs();
      contributions.addAll(examples);

//    ArrayList<LibraryCompilation> compilations = LibraryCompilation.list(libraries);
//
//    // Remove libraries from the list that are part of a compilations
//    for (LibraryCompilation compilation : compilations) {
//      Iterator<Library> it = libraries.iterator();
//      while (it.hasNext()) {
//        Library current = it.next();
//        if (compilation.getFolder().equals(current.getFolder().getParentFile())) {
//          it.remove();
//        }
//      }
//    }

      contribListing.updateInstalledList(contributions);
    }
  }


 

  protected void setFilterText(String filter) {
    if (filter == null || filter.isEmpty()) {
      filterField.setText("");
      filterField.showingHint = true;
    } else {
      filterField.setText(filter);
      filterField.showingHint = false;
    }
    filterField.applyFilter();
  }


//  private JPanel getPlaceholder() {
//    return contributionListPanel.statusPlaceholder;
//  }

  //TODO: this is causing a lot of bugs as the hint is wrongly firing applyFilter()
  class FilterField extends JPanel {
    JTextField searchField;
    JButton searchButton;
    String filterHint;
    boolean showingHint;
    List<String> filters;

    public FilterField () {

      searchField = new JTextField(Language.text("contrib.filter_your_search"));
      filterHint = Language.text("contrib.filter_your_search");
      searchButton = new JButton("S");

      ComponentBorder cb = new ComponentBorder(searchButton,"LEFT");
      cb.install( searchField);
      GroupLayout layout = new GroupLayout(this);
      setLayout(layout);
      
      layout.setHorizontalGroup(layout.createSequentialGroup().addComponent(searchField));
      layout.setVerticalGroup(layout.createParallelGroup().addComponent(searchField));
      
      showingHint = true;
      filters = new ArrayList<String>();
      updateStyle();

      addFocusListener(new FocusListener() {
        public void focusLost(FocusEvent focusEvent) {
          if (filterField.getText().isEmpty()) {
            showingHint = true;
          }
          updateStyle();
        }

        public void focusGained(FocusEvent focusEvent) {
          if (showingHint) {
            showingHint = false;
            filterField.setText("");
          }
          updateStyle();
        }
      });

      searchField.getDocument().addDocumentListener(new DocumentListener() {
        public void removeUpdate(DocumentEvent e) {
          applyFilter();
        }

        public void insertUpdate(DocumentEvent e) {
          applyFilter();
        }

        public void changedUpdate(DocumentEvent e) {
          applyFilter();
        }
      });
    }
    
    public void applyFilter() {
      String filter = filterField.getFilterText();
      filter = filter.toLowerCase();

      // Replace anything but 0-9, a-z, or : with a space
      filter = filter.replaceAll("[^\\x30-\\x39^\\x61-\\x7a^\\x3a]", " ");
      filters = Arrays.asList(filter.split(" "));
      filterLibraries(category, filters);

      contributionListPanel.updateColors();
    }

    public String getFilterText() {
      return showingHint ? "" : getText();
    }

    public String getText() {
      return searchField.getText();
    }
    
    public void setText(String text) {
      searchField.setText(text);
    }

    public void updateStyle() {
      if (showingHint) {
        setText(filterHint);
        // setForeground(UIManager.getColor("TextField.light")); // too light
        setForeground(Color.gray);
        setFont(getFont().deriveFont(Font.ITALIC));
      } else {
        setForeground(UIManager.getColor("TextField.foreground"));
        setFont(getFont().deriveFont(Font.PLAIN));
      }
    }
  }


  public boolean hasAlreadyBeenOpened() {
    return panel != null;
  }

  public void updateStatusPanel(ContributionPanel contributionPanel) {
    statusPanel.update(contributionPanel);
  }
  /**
   *  The ComponentBorder class allows you to place a real component in
   *  the space reserved for painting the Border of a component.
   *
   *  This class takes advantage of the knowledge that all Swing components are
   *  also Containers. By default the layout manager is null, so we should be
   *  able to place a child component anywhere in the parent component. In order
   *  to prevent the child component from painting over top of the parent
   *  component a Border is added to the parent componet such that the insets of
   *  the Border will reserve space for the child component to be painted without
   *  affecting the parent component.
   */
  public class ComponentBorder implements Border
  {
    static final String TOP = "TOP";
    static final String LEFT = "LEFT";
    static final String BOTTOM = "BOTTOM";
    static final String RIGHT = "RIGHT";

    public static final float LEADING  = 0.0f;
    public static final float CENTER   = 0.5f;
    public static final float TRAILING = 1.0f;

    private JComponent parent;
    private JComponent component;
    private String edge;
    private float alignment;
    private int gap = 5;
    private boolean adjustInsets = true;
    private Insets borderInsets = new Insets(0, 0, 0, 0);

    /**
     *  Convenience constructor that uses the default edge (Edge.RIGHT) and
     *  alignment (CENTER).
     *
     *  @param component the component to be added in the Border area
     */
    public ComponentBorder(JComponent component)
    {
      this(component, RIGHT);
    }

    /**
     *  Convenience constructor that uses the default alignment (CENTER).
     *
     *  @param component the component to be added in the Border area
     *  @param edge a valid Edge enum of TOP, LEFT, BOTTOM, RIGHT
     */
    public ComponentBorder(JComponent component, String edge)
    {
      this(component, edge, CENTER);
    }

    /**
     *  Main constructor to create a ComponentBorder.
     *
     *  @param component the component to be added in the Border area
     *  @param edge  a valid Edge enum of TOP, LEFT, BOTTOM, RIGHT
     *  @param alignment the alignment of the component along the
     *                   specified Edge. Must be in the range 0 - 1.0.
     */
    public ComponentBorder(JComponent component, String edge, float alignment )
    {
      this.component = component;
      component.setSize( component.getPreferredSize() );
      component.setCursor(Cursor.getDefaultCursor());
      setEdge( edge );
      setAlignment( alignment );
    }

    public boolean isAdjustInsets()
    {
      return adjustInsets;
    }

    public void setAdjustInsets(boolean adjustInsets)
    {
      this.adjustInsets = adjustInsets;
    }

    /**
     *  Get the component alignment along the Border Edge
     *
     *  @return the alignment
     */
    public float getAlignment()
    {
      return alignment;
    }

    /**
     *  Set the component alignment along the Border Edge
     *
     *  @param alignment a value in the range 0 - 1.0. Standard values would be
     *                   CENTER (default), LEFT and RIGHT.
     */
    public void setAlignment(float alignment)
    {
      this.alignment = alignment > 1.0f ? 1.0f : alignment < 0.0f ? 0.0f : alignment;
    }

    /**
     *  Get the Edge the component is positioned along
     *
     *  @return the Edge
     */
    public String getEdge()
    {
      return edge;
    }

    /**
     *  Set the Edge the component is positioned along
     *
     *  @param edge the Edge the component is position on.
     */
    public void setEdge(String edge)
    {
      this.edge = edge;
    }

    /**
     *  Get the gap between the border component and the parent component
     *
     *  @return the gap in pixels.
     */
    public int getGap()
    {
      return gap;
    }

    /**
     *  Set the gap between the border component and the parent component
     *
     *  @param gap the gap in pixels (default is 5)
     */
    public void setGap(int gap)
    {
      this.gap = gap;
    }

  //
//    Implement the Border interface
  //

    public Insets getBorderInsets(Component c)
    {
      return borderInsets;
    }

    public boolean isBorderOpaque()
    {
      return false;
    }

    /**
     *  In this case a real component is to be painted. Setting the location
     *  of the component will cause it to be painted at that location.
     */
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height)
    {
      float x2 = (width  - component.getWidth())  * component.getAlignmentX() + x;
      float y2 = (height - component.getHeight()) * component.getAlignmentY() + y;
      component.setLocation((int)x2, (int)y2);
    }

    /*
     *  Install this Border on the specified component by replacing the
     *  existing Border with a CompoundBorder containing the original Border
     *  and our ComponentBorder
     *
     *  This method should only be invoked once all the properties of this
     *  class have been set. Installing the Border more than once will cause
     *  unpredictable results.
     */
    public void install(JComponent parent)
    {
      this.parent = parent;

      determineInsetsAndAlignment();

      //  Add this Border to the parent

      Border current = parent.getBorder();

      if (current == null)
      {
        parent.setBorder(this);
      }
      else
      {
        CompoundBorder compound = new CompoundBorder(current, this);
        parent.setBorder(compound);
      }

      //  Add component to the parent

      parent.add(component);
    }

    /**
     *  The insets need to be determined so they are included in the preferred
     *  size of the component the Border is attached to.
     *
     *  The alignment of the component is determined here so it doesn't need
     *  to be recalculated every time the Border is painted.
     */
    private void determineInsetsAndAlignment()
    {
      borderInsets = new Insets(0, 0, 0, 0);

      //  The insets will only be updated for the edge the component will be
      //  diplayed on.
      //
      //  The X, Y alignment of the component is controlled by both the edge
      //  and alignment parameters

      if (edge.equals("TOP"))
      {
        borderInsets.top = component.getPreferredSize().height + gap;
        component.setAlignmentX(alignment);
        component.setAlignmentY(0.0f);
      }
      else if (edge.equals("BOTTOM"))
      {
        borderInsets.bottom = component.getPreferredSize().height + gap;
        component.setAlignmentX(alignment);
        component.setAlignmentY(1.0f);
      }
      else if (edge.equals("LEFT"))
      {
        borderInsets.left = component.getPreferredSize().width + gap;
        component.setAlignmentX(0.0f);
        component.setAlignmentY(alignment);
      }
      else if (edge.equals("RIGHT"))
      {
        borderInsets.right = component.getPreferredSize().width + gap;
        component.setAlignmentX(1.0f);
        component.setAlignmentY(alignment);
      }

      if (adjustInsets)
        adjustBorderInsets();
    }

    /*
     *  The complimentary edges of the Border may need to be adjusted to allow
     *  the component to fit completely in the bounds of the parent component.
     */
    private void adjustBorderInsets()
    {
      Insets parentInsets = parent.getInsets();

      //  May need to adust the height of the parent component to fit
      //  the component in the Border

      if (edge.equals("RIGHT") || edge.equals("LEFT"))
      {
        int parentHeight = parent.getPreferredSize().height - parentInsets.top - parentInsets.bottom;
        int diff = component.getHeight() - parentHeight;

        if (diff > 0)
        {
          int topDiff = (int)(diff * alignment);
          int bottomDiff = diff - topDiff;
          borderInsets.top += topDiff;
          borderInsets.bottom += bottomDiff;
        }
      }

      //  May need to adust the width of the parent component to fit
      //  the component in the Border

      if (edge.equals("TOP") || edge.equals("BOTTOM"))
      {
        int parentWidth = parent.getPreferredSize().width - parentInsets.left - parentInsets.right;
        int diff = component.getWidth() - parentWidth;

        if (diff > 0)
        {
          int leftDiff = (int)(diff * alignment);
          int rightDiff = diff - leftDiff;
          borderInsets.left += leftDiff;
          borderInsets.right += rightDiff;
        }
      }
    }
  }
}

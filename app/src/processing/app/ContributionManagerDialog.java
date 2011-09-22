/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
 Part of the Processing project - http://processing.org

 Copyright (c) 2004-11 Ben Fry and Casey Reas
 Copyright (c) 2001-04 Massachusetts Institute of Technology

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software Foundation,
 Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package processing.app;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.CropImageFilter;
import java.awt.image.FilteredImageSource;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.*;

import processing.app.contribution.*;

public class ContributionManagerDialog {
  
  static final String ANY_CATEGORY = "All";
  
  /** Width of each contribution icon. */
  static final int ICON_WIDTH = 25;

  /** Height of each contribution icon. */
  static final int ICON_HEIGHT = 20;

  JFrame dialog;
  
  private String title;
  
  FilterField filterField;
  
  JScrollPane scrollPane;
  
  ContributionListPanel contributionListPanel;
  
  StatusPanel statusBar;
  
  JComboBox categoryChooser;
  
  Image[] contributionIcons;
  
  // the calling editor, so updates can be applied
  Editor editor;
  
  String category;
  
  ContributionListing contribListing;
  
  public ContributionManagerDialog(String title,
                                   ContributionListing.Filter filter) {
    
    this.title = title;
    
    contribListing = ContributionListing.getInstance();
    
    contributionListPanel = new ContributionListPanel(this, filter);
    contribListing.addContributionListener(contributionListPanel);
  }
  
  protected void showFrame(Editor editor) {
    this.editor = editor;
    
    if (dialog == null) {
      dialog = new JFrame(title);
  
      Base.setIcon(dialog);
      
      createComponents();
  
      registerDisposeListeners();
  
      dialog.pack();
      Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
      dialog.setLocation((screen.width - dialog.getWidth()) / 2,
                         (screen.height - dialog.getHeight()) / 2);
  
      contributionListPanel.grabFocus();
    }
    
    dialog.setVisible(true);
    
    if (!contribListing.hasDownloadedLatestList()) {
      contribListing.getAdvertisedContributions(new AbstractProgressMonitor() {
        public void startTask(String name, int maxValue) {
        }

        public void finished() {
          super.finished();

          updateContributionListing();
          updateCategoryChooser();
          if (isError()) {
            statusBar.setErrorMessage("An error occured when downloading " + 
                                      "the list of available contributions.");
          }
        }
      });
    }
    
    updateContributionListing();
    
    if (contributionIcons == null) {
      try {
        Image allButtons = ImageIO.read(Base.getLibStream("contributions.gif"));
        int count = allButtons.getHeight(dialog) / ContributionManagerDialog.ICON_HEIGHT;
        contributionIcons = new Image[count];
        contributionIcons[0]  = allButtons;
        contributionIcons[1]  = allButtons;
        contributionIcons[2]  = allButtons;
        contributionIcons[3]  = allButtons;
        
        for (int i = 0; i < count; i++) {
          Image image = dialog.createImage(
                            new FilteredImageSource(allButtons.getSource(),
                            new CropImageFilter(0, i * ContributionManagerDialog.ICON_HEIGHT,
                                                ContributionManagerDialog.ICON_WIDTH,
                                                ContributionManagerDialog.ICON_HEIGHT)));
          contributionIcons[i] = image;
        }
        
        contributionListPanel.updateColors();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }
  
  public Image getContributionIcon(Contribution.Type type) {
    
    if (contributionIcons == null)
      return null;
      
    switch (type) {
    case LIBRARY:
      return contributionIcons[0];
    case TOOL:
      return contributionIcons[1];
    case MODE:
      return contributionIcons[2];
    case LIBRARY_COMPILATION:
      return contributionIcons[3];
    }
    return null;
  }
  
  /**
   * Close the window after an OK or Cancel.
   */
  protected void disposeFrame() {
    dialog.dispose();
    editor = null;
  }
  
  /** Creates and arranges the Swing components in the dialog. */
  private void createComponents() {
    dialog.setResizable(true);
    
    Container pane = dialog.getContentPane();
    pane.setLayout(new GridBagLayout());
    
    { // The filter text area
      GridBagConstraints c = new GridBagConstraints();
      c.gridx = 0;
      c.gridy = 0;
      c.gridwidth = 2;
      c.weightx = 1;
      c.fill = GridBagConstraints.HORIZONTAL;
      filterField = new FilterField();

      pane.add(filterField, c);
    }
    
    { // The scroll area containing the contribution listing and the status bar.
      GridBagConstraints c = new GridBagConstraints();
      c.fill = GridBagConstraints.BOTH;
      c.gridx = 0;
      c.gridy = 1;
      c.gridwidth = 2;
      c.weighty = 1;
      c.weightx = 1;
      
      scrollPane = new JScrollPane();
      scrollPane.setPreferredSize(new Dimension(300, 300));
      scrollPane.setViewportView(contributionListPanel);
      scrollPane.getViewport().setOpaque(true);
      scrollPane.getViewport().setBackground(contributionListPanel.getBackground());
      scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
      scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      
      statusBar = new StatusPanel();
      statusBar.setBorder(BorderFactory.createEtchedBorder());
      
      final JLayeredPane layeredPane = new JLayeredPane();
      layeredPane.add(scrollPane, JLayeredPane.DEFAULT_LAYER);
      layeredPane.add(statusBar, JLayeredPane.PALETTE_LAYER);
      
      layeredPane.addComponentListener(new ComponentAdapter() {
        
        void resizeLayers() {
          scrollPane.setSize(layeredPane.getSize());
          scrollPane.updateUI();
        }
        
        public void componentShown(ComponentEvent e) {
          resizeLayers();
        }
        
        public void componentResized(ComponentEvent arg0) {
          resizeLayers();
        }
      });
      
      final JViewport viewport = scrollPane.getViewport();
      viewport.addComponentListener(new ComponentAdapter() {
        void resizeLayers() {
          statusBar.setLocation(0, viewport.getHeight() - 18);
          
          Dimension d = viewport.getSize();
          d.height = 20;
          d.width += 3;
          statusBar.setSize(d);
        }
        public void componentShown(ComponentEvent e) {
          resizeLayers();
        }
        public void componentResized(ComponentEvent e) {
          resizeLayers();
        }
      });
      
      pane.add(layeredPane, c);
    }
    
    { // Shows "Category:"
      GridBagConstraints c = new GridBagConstraints();
      c.gridx = 0;
      c.gridy = 2;
      pane.add(new Label("Category:"), c);
    }
    
    { // Combo box for selecting a category
      GridBagConstraints c = new GridBagConstraints();
      c.fill = GridBagConstraints.HORIZONTAL;
      c.gridx = 1;
      c.gridy = 2;

      categoryChooser = new JComboBox();
      updateCategoryChooser();
      pane.add(categoryChooser, c);
      categoryChooser.addItemListener(new ItemListener() {

        public void itemStateChanged(ItemEvent e) {
          category = (String) categoryChooser.getSelectedItem();
          if (ContributionManagerDialog.ANY_CATEGORY.equals(category)) {
            category = null;
          }

          filterLibraries(category, filterField.filters);
        }
      });
    }
    
    dialog.setMinimumSize(new Dimension(550, 400));
  }

  private void updateCategoryChooser() {
    if (categoryChooser == null)
      return;
    
    ArrayList<String> categories;
    categoryChooser.removeAllItems();
    categories = new ArrayList<String>(contribListing.getCategories());
    Collections.sort(categories);
    categories.add(0, ContributionManagerDialog.ANY_CATEGORY);
    for (String s : categories) {
      categoryChooser.addItem(s);
    }
  }

  private void registerDisposeListeners() {
    dialog.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        disposeFrame();
      }
    });
    ActionListener disposer = new ActionListener() {
      public void actionPerformed(ActionEvent actionEvent) {
        disposeFrame();
      }
    };
    Base.registerWindowCloseKeys(dialog.getRootPane(), disposer);
    
    // handle window closing commands for ctrl/cmd-W or hitting ESC.
    
    dialog.getContentPane().addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        //System.out.println(e);
        KeyStroke wc = Base.WINDOW_CLOSE_KEYSTROKE;
        if ((e.getKeyCode() == KeyEvent.VK_ESCAPE) ||
            (KeyStroke.getKeyStrokeForEvent(e).equals(wc))) {
          disposeFrame();
        }
      }
    });
  }

  public void filterLibraries(String category, List<String> filters) {

    List<Contribution> filteredLibraries = contribListing
        .getFilteredLibraryList(category, filters);

    contributionListPanel.filterLibraries(filteredLibraries);
  }

  protected void updateContributionListing() {
    if (editor == null)
      return;
    
    ArrayList<Library> libraries = editor.getMode().contribLibraries;
    ArrayList<LibraryCompilation> compilations = LibraryCompilation.list(libraries);

    // Remove libraries from the list that are part of a compilations
    for (LibraryCompilation compilation : compilations) {
      Iterator<Library> it = libraries.iterator();
      while (it.hasNext()) {
        Library current = it.next();
        if (compilation.getFolder().equals(current.getFolder().getParentFile())) {
          it.remove();
        }
      }
    }
    
    ArrayList<Contribution> contributions = new ArrayList<Contribution>();
    contributions.addAll(editor.contribTools);
    contributions.addAll(libraries);
    contributions.addAll(compilations);
    
    contribListing.updateInstalledList(contributions);
  }
  
  public void setFilterText(String filter) {
    if (filter == null || filter.isEmpty()) {
      filterField.setText("");
      filterField.isShowingHint = true;
    } else {
      filterField.setText(filter);
      filterField.isShowingHint = false;
    }
    filterField.applyFilter();
    
  }
  
  class FilterField extends JTextField {
    
    final static String filterHint = "Filter your search...";

    boolean isShowingHint;
    
    List<String> filters;
    
    public FilterField () {
      super(filterHint);
      
      isShowingHint = true;
      
      filters = new ArrayList<String>();
      
      addFocusListener(new FocusListener() {
        
        public void focusLost(FocusEvent focusEvent) {
          if (filterField.getText().isEmpty()) {
            isShowingHint = true;
          }
          
          updateStyle();
        }
        
        public void focusGained(FocusEvent focusEvent) {
          if (isShowingHint) {
            isShowingHint = false;
            filterField.setText("");
          }
          
          updateStyle();
        }
      });
      
      getDocument().addDocumentListener(new DocumentListener() {
        
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
    }
    
    public String getFilterText() {
      return isShowingHint ? "" : getText();
    }

    public void updateStyle() {
      if (isShowingHint) {
        filterField.setText(filterHint);
        
        // setForeground(UIManager.getColor("TextField.light")); // too light
        setForeground(Color.gray);
      } else {
        setForeground(UIManager.getColor("TextField.foreground"));
      }
    }
  }

  public boolean hasAlreadyBeenOpened() {
    return dialog != null;
  }
  
  class StatusPanel extends JPanel implements ErrorWidget {
    
    String errorMessage;
    
    StatusPanel() {
      addMouseListener(new MouseAdapter() {
        
        public void mousePressed(MouseEvent e) {
          clearErrorMessage();
        }
      });
    }
    
    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      
      g.setFont(new Font("SansSerif", Font.PLAIN, 10));
      int baseline = (getSize().height + g.getFontMetrics().getAscent()) / 2;
      
      if (contribListing.isDownloadingListing()) {
        g.setColor(Color.black);
        g.drawString("Downloading software listing...", 2, baseline);
        setVisible(true);
      } else if (errorMessage != null) {
        g.setColor(Color.red);
        g.drawString(errorMessage, 2, baseline);
        setVisible(true);
      } else {
        setVisible(false);
      }
    }
    
    public void setErrorMessage(String message) {
      errorMessage = message;
      setVisible(true);
      
      JPanel placeholder = ContributionManagerDialog.this.contributionListPanel.statusPlaceholder;
      Dimension d = getPreferredSize();
      if (Base.isWindows()) {
        d.height += 5;
        placeholder.setPreferredSize(d);
      }
      placeholder.setVisible(true);
      
//      Rectangle rect = scrollPane.getViewport().getViewRect();
//      rect.x += d.height;
//      scrollPane.getViewport().scrollRectToVisible(rect);
    }
    
    void clearErrorMessage() {
      errorMessage = null;
      repaint();
      
      ContributionManagerDialog.this.contributionListPanel.statusPlaceholder
          .setVisible(false);
    }
  }
  
}

abstract class JProgressMonitor extends AbstractProgressMonitor {
  JProgressBar progressBar;
  
  public JProgressMonitor(JProgressBar progressBar) {
    this.progressBar = progressBar;
  }
  
  public void startTask(String name, int maxValue) {
    isFinished = false;
    progressBar.setString(name);
    progressBar.setIndeterminate(maxValue == UNKNOWN);
    progressBar.setMaximum(maxValue);
  }
  
  public void setProgress(int value) {
    super.setProgress(value);
    progressBar.setValue(value);
  }
  
  @Override
  public void finished() {
    super.finished();
    finishedAction();
  }

  public abstract void finishedAction();
  
}
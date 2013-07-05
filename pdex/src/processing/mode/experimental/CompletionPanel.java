package processing.mode.experimental;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.text.BadLocationException;

import processing.app.syntax.JEditTextArea;

public class CompletionPanel {
  private JList completionList;

  private JPopupMenu popupMenu;

  private String subWord;

  private int insertionPosition;

  private TextArea textarea;

  private JScrollPane scrollPane;

  public CompletionPanel(JEditTextArea textarea, int position, String subWord,
                         DefaultListModel items, Point location) {
    this.textarea = (TextArea) textarea;
    this.insertionPosition = position;
    if (subWord.indexOf('.') != -1)
      this.subWord = subWord.substring(subWord.lastIndexOf('.') + 1);
    else
      this.subWord = subWord;
    popupMenu = new JPopupMenu();
    popupMenu.removeAll();
    popupMenu.setOpaque(false);
    popupMenu.setBorder(null);
    scrollPane = new JScrollPane();
    scrollPane.setViewportView(completionList = createSuggestionList(position, items));
    popupMenu.add(scrollPane, BorderLayout.CENTER);
    this.textarea.errorCheckerService.astGenerator
        .updateJavaDoc((CompletionCandidate) completionList.getSelectedValue());
    popupMenu.show(textarea, location.x, textarea.getBaseline(0, 0)
        + location.y);
    System.out.println("Suggestion shown");
  }

  public boolean isVisible() {
    return popupMenu.isVisible();
  }

  public JList createSuggestionList(final int position,
                                    final DefaultListModel items) {

    JList list = new JList(items);
    list.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.setSelectedIndex(0);
    list.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
          insertSelection();
          hideSuggestion();
        }
      }
    });
    return list;
  }
  
  public boolean updateList(final DefaultListModel items, String newSubword, int position){    
    completionList.setModel(items);
    completionList.validate();
    completionList.repaint();
    completionList.setSelectedIndex(0);
    this.subWord = new String(newSubword);
    if (subWord.indexOf('.') != -1)
      this.subWord = subWord.substring(subWord.lastIndexOf('.') + 1);
    insertionPosition = position;
    return true;
  }

  public boolean insertSelection() {
    if (completionList.getSelectedValue() != null) {
      try {
        final String selectedSuggestion = ((CompletionCandidate) completionList
            .getSelectedValue()).getCompletionString().substring(subWord.length());
        textarea.getDocument().insertString(insertionPosition,
                                            selectedSuggestion, null);
        textarea.setCaretPosition(insertionPosition
            + selectedSuggestion.length());
        return true;
      } catch (BadLocationException e1) {
        e1.printStackTrace();
      }
      hideSuggestion();
    }
    return false;
  }

  public void hideSuggestion() {
    popupMenu.setVisible(false);
    System.out.println("Suggestion hidden");
    //textarea.errorCheckerService.astGenerator.jdocWindowVisible(false);
  }

  public void moveUp() {
    if (completionList.getSelectedIndex() == 0) {
      scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
      selectIndex(completionList.getModel().getSize() - 1);
      return;
    } else {
      int index = Math.max(completionList.getSelectedIndex() - 1, 0);
      selectIndex(index);
    }
    int step = scrollPane.getVerticalScrollBar().getMaximum()
        / completionList.getModel().getSize();
    scrollPane.getVerticalScrollBar().setValue(scrollPane
                                                   .getVerticalScrollBar()
                                                   .getValue()
                                                   - step);
    textarea.errorCheckerService.astGenerator
        .updateJavaDoc((CompletionCandidate) completionList.getSelectedValue());

  }

  public void moveDown() {
    if (completionList.getSelectedIndex() == completionList.getModel().getSize() - 1) {
      scrollPane.getVerticalScrollBar().setValue(0);
      selectIndex(0);
      return;
    } else {
      int index = Math.min(completionList.getSelectedIndex() + 1, completionList.getModel()
          .getSize() - 1);
      selectIndex(index);
    }
    textarea.errorCheckerService.astGenerator
        .updateJavaDoc((CompletionCandidate) completionList.getSelectedValue());
    int step = scrollPane.getVerticalScrollBar().getMaximum()
        / completionList.getModel().getSize();
    scrollPane.getVerticalScrollBar().setValue(scrollPane
                                                   .getVerticalScrollBar()
                                                   .getValue()
                                                   + step);
  }

  private void selectIndex(int index) {
    completionList.setSelectedIndex(index);
//      final int position = textarea.getCaretPosition();
//      SwingUtilities.invokeLater(new Runnable() {
//        @Override
//        public void run() {
//          textarea.setCaretPosition(position);
//        };
//      });
  }
}
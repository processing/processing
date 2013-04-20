package processing.mode.experimental;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;
import javax.swing.text.BadLocationException;

import processing.app.syntax.JEditTextArea;

public class CompletionPanel {
  private JList list;

  private JPopupMenu popupMenu;

  private String subWord;

  private final int insertionPosition;

  private TextArea textarea;

  public CompletionPanel(JEditTextArea textarea, int position, String subWord,
                         CompletionCandidate[] items, Point location) {
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
    popupMenu.add(list = createSuggestionList(position, items),
                  BorderLayout.CENTER);
    popupMenu.show(textarea, location.x, textarea.getBaseline(0, 0)
        + location.y);
    this.textarea.errorCheckerService.astGenerator.updateJavaDoc((CompletionCandidate) list
                                                            .getSelectedValue());
  }

  public void hide() {
    popupMenu.setVisible(false);
  }

  public boolean isVisible() {
    return popupMenu.isVisible();
  }

  public JList createSuggestionList(final int position, final CompletionCandidate[] items) {

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

  public boolean insertSelection() {
    if (list.getSelectedValue() != null) {
      try {
        final String selectedSuggestion = ((CompletionCandidate) list.getSelectedValue()).toString()
            .substring(subWord.length());
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
    hide();
  }

  public void moveUp() {
    if (list.getSelectedIndex() == 0) {
      selectIndex(list.getModel().getSize() - 1);
    } else {
      int index = Math.max(list.getSelectedIndex() - 1, 0);
      selectIndex(index);
    }
    textarea.errorCheckerService.astGenerator.updateJavaDoc((CompletionCandidate) list
        .getSelectedValue());

  }

  public void moveDown() {
    if (list.getSelectedIndex() == list.getModel().getSize() - 1) {
      selectIndex(0);
    } else {
      int index = Math.min(list.getSelectedIndex() + 1, list.getModel()
          .getSize() - 1);
      selectIndex(index);
    }
    textarea.errorCheckerService.astGenerator.updateJavaDoc((CompletionCandidate) list
                                                            .getSelectedValue());
  }

  private void selectIndex(int index) {
    list.setSelectedIndex(index);
//      final int position = textarea.getCaretPosition();
//      SwingUtilities.invokeLater(new Runnable() {
//        @Override
//        public void run() {
//          textarea.setCaretPosition(position);
//        };
//      });
  }
}
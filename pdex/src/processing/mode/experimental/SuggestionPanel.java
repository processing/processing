package processing.mode.experimental;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;

import processing.app.syntax.JEditTextArea;

public class SuggestionPanel {
    private JList list;

    private JPopupMenu popupMenu;

    private String subWord;

    private final int insertionPosition;
    private JTextArea textarea;
    
    public SuggestionPanel(JEditTextArea textarea, int position, String subWord,
                           Point location) {
      this.insertionPosition = position;
      this.subWord = subWord;
      popupMenu = new JPopupMenu();
      popupMenu.removeAll();
      popupMenu.setOpaque(false);
      popupMenu.setBorder(null);
      popupMenu.add(list = createSuggestionList(position, subWord),
                    BorderLayout.CENTER);
      popupMenu.show(textarea, location.x, textarea.getBaseline(0, 0)
          + location.y);
    }

    public void hide() {
      popupMenu.setVisible(false);
//      if (suggestion == this) {
//        suggestion = null;
//      }
    }

    private JList createSuggestionList(final int position, final String subWord) {
      Object[] data = new Object[10];
      for (int i = 0; i < data.length; i++) {
        data[i] = subWord + i * 10;
      }
      JList list = new JList(data);
      list.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
      list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      list.setSelectedIndex(0);
      list.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
          if (e.getClickCount() == 2) {
            insertSelection();
          }
        }
      });
      return list;
    }

    public boolean insertSelection() {
      if (list.getSelectedValue() != null) {
        try {
          final String selectedSuggestion = ((String) list.getSelectedValue())
              .substring(subWord.length());
          textarea.getDocument().insertString(insertionPosition,
                                              selectedSuggestion, null);
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
      int index = Math.min(list.getSelectedIndex() - 1, 0);
      selectIndex(index);
    }

    public void moveDown() {
      int index = Math.min(list.getSelectedIndex() + 1, list.getModel()
          .getSize() - 1);
      selectIndex(index);
    }

    private void selectIndex(int index) {
      final int position = textarea.getCaretPosition();
      list.setSelectedIndex(index);
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          textarea.setCaretPosition(position);
        };
      });
    }
  }
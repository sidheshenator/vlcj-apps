/*
 * This file is part of VLCJ.
 *
 * VLCJ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * VLCJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with VLCJ.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright 2009, 2010 Caprica Software Limited.
 */

package uk.co.caprica.vlcj.radio.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;
import uk.co.caprica.vlcj.radio.model.Directory;
import uk.co.caprica.vlcj.radio.model.DirectoryEntry;
import uk.co.caprica.vlcj.radio.service.DirectoryService;
import uk.co.caprica.vlcj.radio.service.icecast.IcecastDirectoryService;
import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.matchers.AbstractMatcherEditor;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.swing.EventTableModel;
import ca.odell.glazedlists.swing.TableComparatorChooser;

/**
 * Main directory view.
 * <p>
 * This view provides a table of directory entries that can be sorted and 
 * filtered.
 * <p>
 * Double-clicking a directory entry plays that station.
 */
public class DirectoryView extends JPanel {

  private static final long serialVersionUID = 1L;

  public static final String ACTIVATE_COMMAND = "activate";
  
  private final EventList<DirectoryEntry> directoryEventList = new BasicEventList<DirectoryEntry>();
  private final SortedList<DirectoryEntry> directorySortedList = new SortedList<DirectoryEntry>(directoryEventList);
  private final DirectoryMatcherEditor directoryMatcherEditor = new DirectoryMatcherEditor();
  private final FilterList<DirectoryEntry> directoryFilterList = new FilterList<DirectoryEntry>(directorySortedList, (MatcherEditor<? super DirectoryEntry>)directoryMatcherEditor);
  
  private final JPanel topPanel;
  
  private final FilterPanel filterPanel;
  private final JPanel playPanel;
  private final StatusPanel statusPanel;
  
  private final JButton playButton;
  private final JScrollPane directoryTableScrollPane;
  private final JTable directoryTable;
  
  private final ExecutorService executorService = Executors.newCachedThreadPool();
  
  public DirectoryView() {
    setLayout(new BorderLayout());

    topPanel = new JPanel();
    topPanel.setBorder(new EmptyBorder(8, 2, 0, 2));
    topPanel.setLayout(new BorderLayout(2, 2));
    add(topPanel, BorderLayout.NORTH);
    
    filterPanel = new FilterPanel(directoryMatcherEditor);
    topPanel.add(filterPanel, BorderLayout.CENTER);
    
    playButton = new JButton("Play");
    playButton.setMnemonic('p');
    playButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        fireActivateEvent();
      }
    });
    
    playPanel = new JPanel();
    playPanel.setBorder(new TitledBorder(""));
    playPanel.setLayout(new FlowLayout());
    playPanel.add(playButton);
    topPanel.add(playPanel, BorderLayout.EAST);
    
    statusPanel = new StatusPanel();
    add(statusPanel, BorderLayout.SOUTH);
    
    directoryTable = new JTable();
    directoryTable.setModel(new DirectoryTableModel(directoryFilterList));
    directoryTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    
    directoryTableScrollPane = new JScrollPane();
    directoryTableScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    directoryTableScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    directoryTableScrollPane.setViewportView(directoryTable);
    
    add(directoryTableScrollPane, BorderLayout.CENTER);

    TableComparatorChooser.install(directoryTable, directorySortedList, TableComparatorChooser.MULTIPLE_COLUMN_MOUSE);
    
    directoryTable.addMouseListener(new TableMouseListener());
  }
  
  public void addActionListener(ActionListener listener) {
    listenerList.add(ActionListener.class, listener);
  }
  
  public void addDirectoryListSelectionListener(ListSelectionListener listener) {
    directoryTable.getSelectionModel().addListSelectionListener(listener);
  }
  
  /**
   * Invoked after the application main view has been created and shown.
   * <p>
   * Start a modal dialog box with an indeterminate progress bar while the
   * media directory is loaded and parsed in a background thread.
   */
  public void start() {
    executorService.execute(new Runnable() {
      @Override
      public void run() {
        JFrame parentFrame = (JFrame)SwingUtilities.getAncestorOfClass(JFrame.class, DirectoryView.this);
        final JDialog dlg = new JDialog(parentFrame, "Please wait...", true);
        JPanel cp = new JPanel();
        cp.setLayout(new MigLayout("fill, insets 16", "[c]", ""));
        cp.add(new JLabel("Please wait, loading station directory..."), "wrap");
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        cp.add(progressBar, "growx");
        dlg.setContentPane(cp);
        dlg.setUndecorated(true);
        dlg.setResizable(false);
        dlg.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dlg.pack();
        dlg.setLocationRelativeTo(parentFrame);
        
        executorService.submit(new Runnable() {
          @Override
          public void run() {
            dlg.setVisible(true);
          }
        });
        
        try {
          DirectoryService directoryService = new IcecastDirectoryService();
          Directory directory = directoryService.directory();
          directoryEventList.addAll(directory.entries());
        }
        catch(Throwable t) {
          // Like this to make sure that the dialog is closed
        }
        
        dlg.setVisible(false);
        dlg.dispose();
      }
    });
  }
  
  public DirectoryEntry getSelectedEntry() {
    int selectedRow = directoryTable.getSelectedRow();
    if(selectedRow != -1) {
      return directoryFilterList.get(selectedRow);
    }
    else {
      return null;
    }
  }
  
  public void setNowPlaying(DirectoryEntry selectedEntry) {
    statusPanel.setModel(getSelectedEntry());
  }

  private void fireActivateEvent() {
    ActionListener[] listeners = listenerList.getListeners(ActionListener.class);
    if(listeners.length > 0) {
      ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ACTIVATE_COMMAND);
      for(int i = listeners.length - 1; i >= 0; i--) {
        listeners[i].actionPerformed(event);
      }
    }
  }
  
  private class TableMouseListener extends MouseAdapter {
    @Override
    public void mouseClicked(MouseEvent e) {
      if(e.getClickCount() == 2) {
        fireActivateEvent();
      }
    }
  }

  class DirectoryMatcherEditor extends AbstractMatcherEditor<DirectoryEntry> implements Matcher<DirectoryEntry> {
    
    private String name;
    private String address;
    private String type;
    private String genre;
    
    @Override
    public Matcher<DirectoryEntry> getMatcher() {
      return this;
    }
    
    @Override
    public boolean matches(DirectoryEntry entry) {
      return match(name   , entry.getName ()) &&
             match(address, entry.getUrl  ()) &&
             match(type   , entry.getType ()) &&
             match(genre  , entry.getGenre()
      );
    }

    void setName(String name) {
      this.name = name;
      fireChanged(this);
    }
    
    void setAddress(String address) {
      this.address = address;
      fireChanged(this);
    }
    
    void setType(String type) {
      this.type = type;
      fireChanged(this);
    }
    
    void setGenre(String genre) {
      this.genre = genre;
      fireChanged(this);
    }
    
    void clear() {
      name = address = type = genre = null;
      fireMatchAll();
    }
    
    private boolean match(String s, String value) {
      return s == null || s.trim().length() == 0 || value.toLowerCase().contains(s);
    }
  }

  @SuppressWarnings("unchecked")
  private static class DirectoryTableModel extends EventTableModel<DirectoryEntry> {

    private static final long serialVersionUID = 1L;

    private static final String[] PROPERTY_NAMES = {"name", "genre", "url", "type"};
    private static final String[] COLUMN_LABELS = {"Name", "Genre", "Address", "Type"};
    private static final boolean[] WRITABLE = {false, false, false, false};
    
    public DirectoryTableModel(EventList<DirectoryEntry> source) {
      super(source, PROPERTY_NAMES, COLUMN_LABELS, WRITABLE);
    }
  }
}
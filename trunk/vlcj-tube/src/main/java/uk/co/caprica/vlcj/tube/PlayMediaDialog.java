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
 * Copyright 2009, 2010, 2011 Caprica Software Limited.
 */

package uk.co.caprica.vlcj.tube;

import java.io.File;

import net.miginfocom.swt.MigLayout;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 *
 */
public class PlayMediaDialog extends Dialog {

  private static final String DEFAULT_AUDIO_BIT_RATE = "192";
  
  private Shell shell;

  private File lastDirectory;

  private Group saveMediaGroup;
  private Button saveAudioButton;
  private Label saveAudioFileNameLabel;
  private Text saveAudioFileNameText;
  private Button saveAudioFileNameButton;
  private Label audioBitRateLabel;
  private Text audioBitRateText;
  
  private Composite buttonPanel;
  private Button okButton;
  private Button cancelButton;
  
  private PlayMediaOptions playMediaOptions;
  
  public PlayMediaDialog(Shell parent, File lastDirectory) {
    this(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL, lastDirectory);
  }

  public PlayMediaDialog(Shell parent, int style, File lastDirectory) {
    super(parent, style);
    this.lastDirectory = lastDirectory;
  }
 
  public PlayMediaOptions open() {
    shell = createDialog();
    shell.pack();
    shell.open();
    Display display = getParent().getDisplay();
    while(!shell.isDisposed()) {
      if(!display.readAndDispatch()) {
        display.sleep();
      }
    }
    return playMediaOptions;
  }
  
  public void close() {
    shell.close();
  }
  
  private Shell createDialog() {
    // Prepare the dialog...
    
    setText("Play Media");
    
    // Create controls...
    
    shell = new Shell(getParent(), getStyle());
    shell.setText(getText());
    
    saveMediaGroup = new Group(shell, SWT.SHADOW_ETCHED_IN);
    saveMediaGroup.setText("Save Media");
    
    saveAudioButton = new Button(saveMediaGroup, SWT.CHECK);
    saveAudioButton.setText("Save &Audio");
    
    saveAudioFileNameLabel = new Label(saveMediaGroup, SWT.NONE);
    saveAudioFileNameLabel.setText("Audio File &Name");
    
    saveAudioFileNameText = new Text(saveMediaGroup, SWT.BORDER);
    saveAudioFileNameText.setEnabled(false);
    
    saveAudioFileNameButton = new Button(saveMediaGroup, SWT.PUSH);
    saveAudioFileNameButton.setText("C&hoose...");
    saveAudioFileNameButton.setEnabled(false);

    audioBitRateLabel = new Label(saveMediaGroup, SWT.NONE);
    audioBitRateLabel.setText("&Bit Rate");
    
    audioBitRateText = new Text(saveMediaGroup, SWT.BORDER);
    audioBitRateText.setText(DEFAULT_AUDIO_BIT_RATE);
    audioBitRateText.setEnabled(false);
    
    buttonPanel = new Composite(shell, SWT.NONE);
    
    okButton = new Button(buttonPanel, SWT.PUSH);
    okButton.setText("&OK");
    
    cancelButton = new Button(buttonPanel, SWT.PUSH);
    cancelButton.setText("&Cancel");
    
    shell.setDefaultButton(okButton);
    
    // Add listeners...
    
    saveAudioButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent evt) {
        boolean saveAudio = saveAudioButton.getSelection();
        saveAudioFileNameText.setEnabled(saveAudio);
        saveAudioFileNameButton.setEnabled(saveAudio);
        audioBitRateText.setEnabled(saveAudio);
      }
    });
    
    saveAudioFileNameButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent evt) {
        String result = getSaveAudioFileName(shell);
        if(result != null) {
          saveAudioFileNameText.setText(result);
        }
      }
    });
    
    okButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent evt) {
        // Validate...
        boolean saveAudio = saveAudioButton.getSelection();
        if(saveAudio) {
          String audioFileName = saveAudioFileNameText.getText().trim();
          if(audioFileName.length() == 0) {
            showValidationError("You must specify a file name to save the audio file.");
            return;
          }
          try {
            int bitRate = Integer.parseInt(audioBitRateText.getText());
            if(bitRate <= 0) {
              showValidationError("You must specify a valid audio bit rate.");
              return;
            }
          }
          catch(NumberFormatException e) {
            showValidationError("You must specify a valid audio bit rate.");
            return;
          }
        }
        
        // Valid...
        playMediaOptions = new PlayMediaOptions();
        playMediaOptions.setSaveAudio(saveAudioButton.getSelection());
        playMediaOptions.setAudioFileName(saveAudioFileNameText.getText());
        playMediaOptions.setAudioBitRate(Integer.parseInt(audioBitRateText.getText()));
        
        close();
      }
    });
    
    cancelButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent evt) {
        close();
      }
    });
    
    // Do layout...
    
    shell.setLayout(new MigLayout("", "", ""));

    saveMediaGroup.setLayoutData("wrap");
    
    saveMediaGroup.setLayout(new MigLayout("", "", ""));

    saveAudioButton.setLayoutData("wrap");
    
    saveAudioFileNameLabel.setLayoutData("align right");
    saveAudioFileNameText.setLayoutData("growx, width min:500");
    saveAudioFileNameButton.setLayoutData("wrap");
    
    audioBitRateLabel.setLayoutData("align right");
    audioBitRateText.setLayoutData("width min:50");
    
    buttonPanel.setLayoutData("span 4, align right");
    
    buttonPanel.setLayout(new MigLayout("insets 0", "", ""));
    
    okButton.setLayoutData("tag ok");
    cancelButton.setLayoutData("tag cancel");
    
    return shell;
  }

  private String getSaveAudioFileName(Shell shell) {
    FileDialog dialog = new FileDialog (shell, SWT.SAVE);
    String [] filterNames = new String [] {"Audio Files", "All Files (*)"};
    String [] filterExtensions = new String [] {"*.mp3", "*"};
    String filterPath = lastDirectory != null ? lastDirectory.getAbsolutePath() : new File(System.getProperty("user.home")).getAbsolutePath();
    dialog.setFilterNames(filterNames);
    dialog.setFilterExtensions(filterExtensions);
    dialog.setFilterPath(filterPath);
    String result = dialog.open();
    lastDirectory = new File(dialog.getFilterPath());
    return result;
  }
  
  private void showValidationError(String message) {
    MessageBox messageBox = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
    messageBox.setText("Validation Error");
    messageBox.setMessage(message);
    messageBox.open();
  }
}

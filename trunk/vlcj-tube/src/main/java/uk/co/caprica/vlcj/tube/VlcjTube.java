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

import java.awt.Canvas;
import java.awt.Frame;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.miginfocom.swt.MigLayout;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.TitleEvent;
import org.eclipse.swt.browser.TitleListener;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

import uk.co.caprica.vlcj.binding.internal.libvlc_media_t;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.CanvasVideoSurface;
import uk.co.caprica.vlcj.x.LibXUtil;

/**
 * An application that embeds a native Web Browser component and a native
 * media player component.
 * <p>
 * Activating a video hyperlink causes the video to be played by the embedded
 * native media player.
 * <p>
 * There are three modes possible when clicking a hyperlink:
 * <ol>
 *   <li>Ordinary click, play the video directly</li>
 *   <li>Control-click, activate the play options dialog box (e.g. to save audio)</li>
 *   <li>Shift-click, follow the hyperlink as normal</li>
 * </ol>
 * <p>
 * Press the F11 key to toggle full-screen mode.
 * <p>
 * Press the ESCAPE key to quit the current video and return to the browser.
 * <p> 
 * WebKit is a better choice than Mozilla for the embedded browser.
 * <pre>
 *   -Dorg.eclipse.swt.browser.UseWebKitGTK=true
 * </pre>
 * With SWT 3.7, WebKit (if available) is the default implementation and the 
 * above property is not required. 
 */
public class VlcjTube {

  /**
   * Initial URL.
   */
  private static final String HOME_URL = "http://www.youtube.com";
  
  /**
   * URLs matching this pattern will be intercepted and played by the embedded
   * media player.
   */
  private static final String WATCH_VIDEO_PATTERN = "http://www.youtube.com/watch\\?v=.*";
  
  /**
   * Pre-compiled regular expression pattern.
   */
  private Pattern watchLinkPattern;

  /**
   * UI components.
   */
  private Display display;
  private Shell shell;
  private StackLayout stackLayout;
  private Composite browserPanel;
  private Browser browser;
  private Composite videoPanel;
  private Composite videoComposite;
  private Frame videoFrame;
  private Canvas videoSurfaceCanvas;
  private CanvasVideoSurface videoSurface;
  private Composite encoderPanel;
  private ProgressBar positionProgressBar;
  private Label positionPercentageLabel;
  private Cursor emptyCursor;

  /**
   * Native media player components.
   */
  private MediaPlayerFactory mediaPlayerFactory;
  private EmbeddedMediaPlayer mediaPlayer;
  
  /**
   * Track the state of the control key.
   */
  private boolean controlKeyDown;
  
  /**
   * Track the state of the shift key.
   */
  private boolean shiftKeyDown;
  
  /**
   * True if encoding audio, false if playing normally.
   */
  private boolean encodeMode;
  
  /**
   * Application entry point.
   * 
   * @param args command-line arguments
   * @throws Exception if an error occurs
   */
  public static void main(String[] args) throws Exception {
    LibXUtil.initialise();
    new VlcjTube().start();
  }
  
  /**
   * Create an application.
   * 
   * @throws Exception if an error occurs
   */
  public VlcjTube() throws Exception {
    watchLinkPattern = Pattern.compile(WATCH_VIDEO_PATTERN);
    
    createUserInterface();
    createEmptyCursor();
    createMediaPlayer();
  }
  
  /**
   * Create the user interface controls.
   */
  private void createUserInterface() {
    display = new Display();
    
    stackLayout = new StackLayout();

    shell = new Shell(display);
    shell.setLayout(stackLayout);
    shell.setSize(1200, 900);
    
    browserPanel = new Composite(shell, SWT.NONE);
    browserPanel.setLayout(new FillLayout());

    browser = new Browser(browserPanel, SWT.NONE);
    browser.setJavascriptEnabled(true);
    
    videoPanel = new Composite(shell, SWT.NONE);
    videoPanel.setLayout(new FillLayout());
    videoPanel.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
    
    videoComposite = new Composite(videoPanel, SWT.EMBEDDED | SWT.NO_BACKGROUND);
    videoComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
    videoFrame = SWT_AWT.new_Frame(videoComposite);
    videoSurfaceCanvas = new Canvas();
    videoSurfaceCanvas.setBackground(java.awt.Color.black);
    videoFrame.add(videoSurfaceCanvas);

    encoderPanel = new Composite(shell, SWT.NONE);
    encoderPanel.setLayout(new MigLayout("center, center", "", ""));
    
    positionProgressBar = new ProgressBar(encoderPanel, SWT.SMOOTH);
    positionProgressBar.setMinimum(0);
    positionProgressBar.setMaximum(100);
    positionProgressBar.setLayoutData("center, width min:400, wrap");
    
    positionPercentageLabel = new Label(encoderPanel, SWT.NONE);
    positionPercentageLabel.setAlignment(SWT.CENTER);
    positionPercentageLabel.setText("0%");
    positionPercentageLabel.setLayoutData("center, width min:400");
    
    showBrowser();
    
    display.addFilter(SWT.KeyDown, new Listener() {
      public void handleEvent(Event evt) {
        switch(evt.keyCode) {
          case SWT.ESC:
            if(stackLayout.topControl == videoPanel || stackLayout.topControl == encoderPanel) {
              mediaPlayer.stop();
              showBrowser();
            }
            else if(stackLayout.topControl == browserPanel) {
              browser.back();
            }
            break;
          
          case SWT.F11:
            shell.setFullScreen(!shell.getFullScreen());
            break;
            
          case SWT.CONTROL:
            controlKeyDown = true;
            break;
          
          case SWT.SHIFT:
            shiftKeyDown = true;
            break;
        }
      }
    });
    
    display.addFilter(SWT.KeyUp, new Listener() {
      public void handleEvent(Event evt) {
        switch(evt.keyCode) {
          case SWT.CONTROL:
            controlKeyDown = false;
            break;

          case SWT.SHIFT:
            shiftKeyDown = false;
            break;
        }
      }
    });
    
    shell.addShellListener(new ShellAdapter() {
      @Override
      public void shellClosed(ShellEvent evt) {
        mediaPlayer.release();
        mediaPlayerFactory.release();
      }
    });
    
    browser.addTitleListener(new TitleListener() {
      public void changed(TitleEvent e) {
        shell.setText("vlcj - " + e.title);
      }
    });
    
    browser.addLocationListener(new LocationAdapter() {
      File lastDirectory;
      
      @Override
      public void changing(LocationEvent evt) {
        Matcher matcher = watchLinkPattern.matcher(evt.location);
        if(matcher.matches()) {
          if(!shiftKeyDown) {
            // Cancel the normal link navigation
            evt.doit = false;
            
            if(controlKeyDown == true) {
              PlayMediaDialog dialog = new PlayMediaDialog(shell, lastDirectory);
              PlayMediaOptions playMediaOptions = dialog.open();
              
              if(playMediaOptions != null) {
                lastDirectory = new File(playMediaOptions.getAudioFileName()).getParentFile();
                
                if(playMediaOptions.isSaveAudio()) {
                  encodeMode = true;
                  positionProgressBar.setSelection(0);
                  positionPercentageLabel.setText("0%");
                  showEncoder();
                  mediaPlayer.playMedia(evt.location, playMediaOptions.getMediaOptions());
                }
                else {
                  encodeMode = false;
                  showVideo();
                  mediaPlayer.playMedia(evt.location);
                }
              }
            }
            else {
              encodeMode = false;
              showVideo();
              mediaPlayer.playMedia(evt.location);
            }
          }
        }
      }
    });
    
    browser.addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent e) {
        switch(e.keyCode) {
        }
      }
    });
    
    videoComposite.addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent e) {
        switch(e.keyCode) {
        }
      }
    });
    
    shell.open();
  }
  
  /**
   * Create an empty cursor.
   */
  private void createEmptyCursor() {
    Color white = display.getSystemColor(SWT.COLOR_WHITE);
    Color black = display.getSystemColor(SWT.COLOR_BLACK);
    PaletteData palette = new PaletteData(new RGB[] { white.getRGB(), black.getRGB() });
    ImageData sourceData = new ImageData(16, 16, 1, palette);
    sourceData.transparentPixel = 0;
    emptyCursor = new Cursor(display, sourceData, 0, 0);
  }
  
  /**
   * Create the native media player components.
   */
  private void createMediaPlayer() {
    mediaPlayerFactory = new MediaPlayerFactory("--no-video-title-show");
    mediaPlayer = mediaPlayerFactory.newEmbeddedMediaPlayer();
    mediaPlayer.setPlaySubItems(true);
    videoSurface = mediaPlayerFactory.newVideoSurface(videoSurfaceCanvas);
    mediaPlayer.setVideoSurface(videoSurface);
    
    mediaPlayer.addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
      int expected;
      
      @Override
      public void newMedia(MediaPlayer mediaPlayer) {
        System.out.println("newMedia");
        expected = 0;
      }

      @Override
      public void opening(MediaPlayer mediaPlayer) {
        System.out.println("opening");
        // Similar to Swing, obey the SWT threading model...
        display.asyncExec(new Runnable() {
          public void run() {
            if(encodeMode) {
              showEncoder();
            }
            else {
              showVideo();
            }
          }
        });
      }
      
      @Override
      public void finished(MediaPlayer mediaPlayer) {
        System.out.println("finished");
        if(expected == 0) {
          // Similar to Swing, obey the SWT threading model...
          display.asyncExec(new Runnable() {
            public void run() {
              showBrowser();
            }
          });
        }
        else {
          expected--;
        }
      }

      @Override
      public void positionChanged(MediaPlayer mediaPlayer, final float newPosition) {
        if(!positionProgressBar.isDisposed()) {
          // Similar to Swing, obey the SWT threading model...
          display.asyncExec(new Runnable() {
            public void run() {
              int value = Math.min(100, Math.round(newPosition * 100.0f));
              positionProgressBar.setSelection(value);
              positionPercentageLabel.setText(value + "%");
            }
          });
        }
      }

      @Override
      public void stopped(MediaPlayer mediaPlayer) {
        expected = 0;
      }

      @Override
      public void mediaSubItemAdded(MediaPlayer mediaPlayer, libvlc_media_t subItem) {
        System.out.println("subItemAdded");
        expected++;
      }

      @Override
      public void subItemPlayed(MediaPlayer mediaPlayer, int subItemIndex) {
        System.out.println("subItemPlayed: " + subItemIndex);
      }

      @Override
      public void subItemFinished(MediaPlayer mediaPlayer, int subItemIndex) {
        System.out.println("subItemFinished: " + subItemIndex);
      }

      @Override
      public void endOfSubItems(MediaPlayer mediaPlayer) {
        System.out.println("endOfSubItems");
      }

      @Override
      public void error(MediaPlayer mediaPlayer) {
        System.out.println("error");
      }
    });
  }
  
  /**
   * Start the application.
   * <p>
   * Execute the SWT message loop.
   */
  private void start() {
    browser.setUrl(HOME_URL);

    while(!shell.isDisposed()) {
      if(!display.readAndDispatch()) {
        display.sleep();
      }
    }
    display.dispose();
  }
  
  private void showBrowser() {
    showView(browserPanel);
  }
  
  private void showVideo() {
    showView(videoPanel);
  }

  private void showEncoder() {
    showView(encoderPanel);
  }
  
  private void showView(Composite view) {
    stackLayout.topControl = view;
    shell.layout();
  }
}

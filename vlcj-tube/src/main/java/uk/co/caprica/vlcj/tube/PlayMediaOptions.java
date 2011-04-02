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

/**
 *
 */
public class PlayMediaOptions {

  private boolean saveAudio;

  private String audioFileName;

  private int audioBitRate;

  public boolean isSaveAudio() {
    return saveAudio;
  }

  public void setSaveAudio(boolean saveAudio) {
    this.saveAudio = saveAudio;
  }

  public String getAudioFileName() {
    return audioFileName;
  }

  public void setAudioFileName(String audioFileName) {
    this.audioFileName = audioFileName;
  }

  public int getAudioBitRate() {
    return audioBitRate;
  }

  public void setAudioBitRate(int audioBitRate) {
    this.audioBitRate = audioBitRate;
  }
  
  // FIXME with saving audio - either there's a problem with 1.1.x and duplicate display (plays locally but saved audio is corrupted)
  //       or with 1.2.x there's a problem with duplicate=display generally (doesn't play locally) but at least the audio is not corrupted
  //       don't know yet if 1.1.x works without duplicate=display
  //       so, at the moment you can either play locally or save, but not both

  public String[] getMediaOptions() {
    StringBuilder sb = new StringBuilder(200);
    sb.append("sout=#transcode{acodec=mp3,channels=2,ab=");
    sb.append(audioBitRate);
    sb.append(",samplerate=44100,vcodec=dummy}:standard{mux=raw,access=file,dst=");
    sb.append(audioFileName);
    sb.append("}}");
    return new String[] {sb.toString()};
  }
}

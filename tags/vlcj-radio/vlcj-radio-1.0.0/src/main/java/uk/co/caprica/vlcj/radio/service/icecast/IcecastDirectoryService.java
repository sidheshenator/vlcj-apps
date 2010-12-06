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

package uk.co.caprica.vlcj.radio.service.icecast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.simpleframework.xml.core.Persister;

import uk.co.caprica.vlcj.radio.model.Directory;
import uk.co.caprica.vlcj.radio.service.DirectoryService;

/**
 * Implementation of a streaming media station directory service that gets the
 * IceCast server directory from xiph.org.
 * <p>
 * This implementation will work off a locally cached directory file unless it
 * detects that the cache is missing or the remote directory has changed.
 * <p>
 * This optimisation is of limited use since the remote directory tracks the
 * currently playing media and this will of course change frequently.
 */
public class IcecastDirectoryService implements DirectoryService {

  /**
   * Remote directory URL. 
   */
  private static final String DIRECTORY_URL = "http://dir.xiph.org/yp.xml";
  
  /**
   * XML binding parser.
   */
  private final Persister persister;
  
  /**
   * Create a new directory service component.
   */
  public IcecastDirectoryService() {
    this.persister = new Persister();
  }

  @Override
  public Directory directory() {
    URLConnection conn = null;
    InputStream in = null;
    try {
      URL url = new URL(DIRECTORY_URL);
      conn = url.openConnection();
      long remoteLastModified = conn.getLastModified();
      File localCacheFile = getCachedDirectoryFile();
      long localLastModified = localCacheFile.lastModified();
      if(remoteLastModified > localLastModified) {
        retrieveRemoteDirectoryFile(conn);
      }
      in = new BufferedInputStream(new FileInputStream(localCacheFile));
      return persister.read(IcecastDirectory.class, in);
    }
    catch(Exception e) {
      throw new RuntimeException("Failed to get directory", e);
    }
    finally {
      if(in != null) {
        try {
          in.close();
        }
        catch(IOException e) {
        }
      }
    }
  }
  
  /**
   * Get the file reference to the locally cached directory file.
   * 
   * @return file
   */
  private File getCachedDirectoryFile() {
    File userHomeDirectory = new File(System.getProperty("user.home"));
    File cacheDirectory = new File(userHomeDirectory, "vlcj-radio");
    File file = new File(cacheDirectory, "directory.xml");
    if(!cacheDirectory.exists()) {
      cacheDirectory.mkdirs();
    }
    return file;
  }

  /**
   * Get the remote directory resource and cache it locally.
   * 
   * @param conn URL connection to the resource resource
   */
  private void retrieveRemoteDirectoryFile(URLConnection conn) {
    BufferedInputStream in = null;
    BufferedOutputStream out = null;
    try {
      File localFile = getCachedDirectoryFile();
      in = new BufferedInputStream(conn.getInputStream());
      out = new BufferedOutputStream(new FileOutputStream(localFile));
      byte[] buff = new byte[10240];
      int read;
      for(;;) {
        read = in.read(buff);
        if(read != -1) {
          out.write(buff, 0, read);
        }
        else {
          break;
        }
      }
    }
    catch(IOException e) {
      throw new RuntimeException("Failed to get remote directory", e);
    }
    finally {
      if(out != null) {
        try {
          out.close();
        }
        catch(IOException e) {
        }
      }
      if(in != null) {
        try {
          in.close();
        }
        catch(IOException e) {
        }
      }
    }
  }
}
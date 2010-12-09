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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.simpleframework.xml.core.Persister;

import uk.co.caprica.vlcj.radio.model.Directory;
import uk.co.caprica.vlcj.radio.service.DirectoryService;

/**
 * Implementation of a streaming media station directory service that gets the
 * IceCast server directory from xiph.org.
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
    InputStream in = null;
    try {
      URL url = new URL(DIRECTORY_URL);
      return persister.read(IcecastDirectory.class, url.openStream());
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
}
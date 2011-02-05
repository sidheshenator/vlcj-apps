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

package uk.co.caprica.vlcj.radio.service.bbcstreams;

import java.util.ArrayList;
import java.util.List;

import uk.co.caprica.vlcj.radio.model.Directory;
import uk.co.caprica.vlcj.radio.service.DirectoryService;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlListItem;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * Implementation of a streaming media station directory service that gets the
 * directory of BBC iPhone streams from the BBCStreams web-site (not affiliated
 * with the BBC).
 * <p>
 * There is web-service API or XML feed so this implementation scrapes the page
 * directly.
 */
public class BbcStreamsDirectoryService implements DirectoryService {

  @Override
  public Directory directory() {
    try {
      WebClient webClient = new WebClient();
      webClient.setCssEnabled(false);
      webClient.setJavaScriptEnabled(false);
      HtmlPage page = webClient.getPage("http://www.bbcstreams.com");
  
      List<?> elements = page.getByXPath("//div[@class='page-post']/h3/following-sibling::ul/li");
      List<BbcStreamsDirectoryEntry> entries = new ArrayList<BbcStreamsDirectoryEntry>(50);
      for(Object element : elements) {
        HtmlListItem li = (HtmlListItem)element;
        
        String itemText = li.asText();
        
        int urlStart = itemText.indexOf("http:");
        int urlEnd = itemText.indexOf(' ', urlStart);

        String descriptionText = itemText.substring(0, urlStart).trim();
        if(descriptionText.endsWith("-")) {
          descriptionText = descriptionText.substring(0, descriptionText.length()-1).trim();
        }
        String urlText = itemText.substring(urlStart, urlEnd != -1 ? urlEnd : itemText.length());
        
        entries.add(new BbcStreamsDirectoryEntry(descriptionText, urlText));
      }
      
      webClient.closeAllWindows();
      return new BbcStreamsDirectory(entries);
    }
    catch(Exception e) {
      throw new RuntimeException("Failed to get directory", e);
    }
  }
}

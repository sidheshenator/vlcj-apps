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

package uk.co.caprica.vlcj.radio.service.indymedia;

import java.util.ArrayList;
import java.util.List;

import uk.co.caprica.vlcj.radio.model.Directory;
import uk.co.caprica.vlcj.radio.service.DirectoryService;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTableCell;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow;

/**
 * Implementation of a streaming media station directory service that gets the
 * directory of streams from the Indymedia web-site.
 * <p>
 * There is no web-service API or XML feed so this implementation scrapes all 
 * of the pages directly.
 */
public class IndymediaDirectoryService implements DirectoryService {

  @Override
  public Directory directory() {
    try {
      WebClient webClient = new WebClient();
      webClient.setCssEnabled(false);
      webClient.setJavaScriptEnabled(false);

      boolean finished = false;
      
      int pageIndex = 0;
      
      List<IndymediaDirectoryEntry> entries = new ArrayList<IndymediaDirectoryEntry>(200);
      while(!finished) {
        HtmlPage page = webClient.getPage("http://radio.indymedia.org/en/yp?page=" + pageIndex++);

        List<?> elements = page.getByXPath("//div[@id='block-system-main']//div[@class='view-content']//table[1]/tbody/tr");
        for(Object element : elements) {
          HtmlTableRow tr = (HtmlTableRow)element;
          
          @SuppressWarnings("unchecked")
          List<HtmlTableCell> tds = (List<HtmlTableCell>)tr.getByXPath("td");
          
          String name = tds.get(0).asText();
          String genre = tds.get(1).asText();
          String type = tds.get(3).asText();
          String bitRate = tds.get(4).asText();
  
          HtmlTableCell listenCell = tds.get(6);
          HtmlAnchor a = (HtmlAnchor)listenCell.getByXPath("a[1]").get(0);
          String url = a.getAttribute("href");
          
          entries.add(new IndymediaDirectoryEntry(name, url, type, bitRate, genre)); 
        }
        
        List<?> nextElement = page.getByXPath("//div[@class='item-list']/ul[@class='pager']/li[contains(@class,'pager-next')]");
        if(nextElement == null || nextElement.isEmpty()) {
          finished = true;
        }
      }

      webClient.closeAllWindows();
      return new IndymediaDirectory(entries);
    }
    catch(Exception e) {
      throw new RuntimeException("Failed to get directory", e);
    }
  }
}

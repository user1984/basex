package org.basex.gui.view.map;

import java.util.ArrayList;

import org.basex.gui.GUI;
import org.basex.gui.view.ViewData;
import org.basex.gui.view.ViewRect;
import org.basex.util.IntList;

/**
 * Uses a Squarified Algorithm to divide Rectangles.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-08, ISC License
 * @author Joerg Hauser
 */
public class SquarifiedLayout extends MapLayout {
  
  @Override
  void calcMap(final ViewRect r, final ArrayList<ViewRect> mainRects, 
      final IntList l, final int ns, final int ne, final int level) {
    // one rectangle left.. continue with this child
    if(ne - ns == 1) {
      // calculate rectangle sizes
      final ViewRect t = new ViewRect(r.x, r.y, r.w, r.h, l.list[ns], r.level);
      // position, with and height are calculated using split sizes of 
      // former recursion level
      final int x = t.x + layout.x;
      final int y = t.y + layout.y;
      final int w = t.w - layout.w;
      final int h = t.h - layout.h;
      mainRects.add(t);
      // skip too small rectangles and leaf nodes (= meta data in deepfs)
      if(w > 0 && h > 0 && !ViewData.isLeaf(GUI.context.data(), t.pre)) {
        final IntList ch = children(t.pre);
        if(ch.size >= 0)
          calcMap(new ViewRect(x, y, w, h, l.list[ns], t.level + 1),
              mainRects, ch, 0, ch.size - 1, level + 1);
      }
    } else {      
      // number of nodes used to calculate rect size
      int nn = l.list[ne] - l.list[ns];
      int ni = ns;
      
      // determine direction
      final boolean v = (r.w > r.h) ? false : true;
      
      // setting initial proportions
      double xx = r.x;
      double yy = r.y;
      double ww = r.w;
      double hh = r.h;
      
      if(v) {
        ArrayList<ViewRect> row = new ArrayList<ViewRect>();
        double height = 0;
        while(ni < ne) {
          // height of row adjusted with current rect
          height = (l.list[ni + 1] - l.list[ns]) * hh / nn;
          ArrayList<ViewRect> tmp = new ArrayList<ViewRect>();
          // create temporary row including current rectangle
          double x = xx;
          
          for(int i = ns; i <= ni; i++) {
            double w = (l.list[i + 1] - l.list[i]) * ww / 
              (l.list[ni + 1] - l.list[ns]);
            tmp.add(new ViewRect((int) x, (int) yy, (int) w, (int) height, 
                l.list[i], level));
            x += w;
          }
          
          // if ar has increased discard tmp and add row
          if(aspectRatio(tmp) > aspectRatio(row)) break;
          row = tmp;
          ni++;
        }
        height = row.get(0).h;
        
        // add rects of row using recursion
        for(int i = 0; i < row.size(); i++) {
          IntList newl = new IntList(1);
          newl.add(row.get(i).pre);
          calcMap(row.get(i), mainRects, newl, 0, 1, level);
        }
        // call recursion for left nodes
        if (ni < ne) calcMap(new ViewRect((int) xx, (int) (yy + height), 
            (int) ww, (int) (hh - height), 0, level), 
            mainRects, l, ni, ne, level);
      } else {
        ArrayList<ViewRect> row = new ArrayList<ViewRect>();
        double width = 0;
        while(ni < ne) {
          // width of row adjusted with current rect
          width = (l.list[ni + 1] - l.list[ns]) * ww / nn;
          ArrayList<ViewRect> tmp = new ArrayList<ViewRect>();
          // create temporary row including current rectangle
          double y = yy;
          for(int i = ns; i <= ni; i++) {
            double h = (l.list[i + 1] - l.list[i]) * hh / 
              (l.list[ni + 1] - l.list[ns]);
            tmp.add(new ViewRect((int) xx, (int) y, (int) width, (int) h, 
                l.list[i], level));
            y += h;
          }
          
          // if ar has increased discard tmp and add row
          if(aspectRatio(tmp) > aspectRatio(row)) break;
          row = tmp;
          ni++;
        }
        
        width = row.get(0).w;
        // add rects of row using recursion
        for(int i = 0; i < row.size(); i++) {
          IntList newl = new IntList(1);
          newl.add(row.get(i).pre);
          calcMap(row.get(i), mainRects, newl, 0, 1, level);
        }
        // call recursion for left nodes
        if(ni < ne) calcMap(new ViewRect((int) (xx + width), (int) yy, 
            (int) (ww - width), (int) hh, 0, level), 
            mainRects, l, ni, ne, level);
      }
    }
  }
}
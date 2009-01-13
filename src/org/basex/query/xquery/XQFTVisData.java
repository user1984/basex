package org.basex.query.xquery;

import org.basex.util.Array;
import org.basex.util.IntList;

/**
 * This class provides a container for XQuery fulltext data 
 * for any visualization.
 * The data is stored as follows:
 * prepos[i][pre, pos0, ..., posn]
 * poi[i][poiMax, poi0, ..., poin]
 * For each pos values, theres a poi value stored. poiMax is the
 * max value of all pos0, ..., poin
 *
 * @author Workgroup DBIS, University of Konstanz 2005-08, ISC License
 * @author Christian Gruen
 * @author Sebastian Gath
 */
public final class XQFTVisData {
  /** Flag for GUI. */
  public boolean gui = false;
  /** Number of pre values. */
  public int size = 0;
  /** Pre values and its pos values. */
  public int[][] prepos = new int[1][];
  /** Pointer values for the pos values.*/
  public int[][] poi = new int[1][];

  /** Real position list for text. */
  private IntList tpos = new IntList();
  /** Corresponding color for the real position list for text. */
  private IntList tcol = new IntList();
  
  /** Number of pre values. */
  private int osize = 0;
  /** Pre values and its pos values. */
  private int[][] oprepos = new int[1][];
  /** Pointer values for the pos values.*/
  private int[][] opoi = new int[1][];

/**
   * Constructor.
   * @param o old data
   */
  public XQFTVisData(final XQFTVisData o) {
    if(o != null) {
      osize = o.size;
      oprepos = o.prepos;
      opoi = o.poi;
    }
    tpos = new IntList();
    tcol = new IntList();
    size = 0;
    prepos = new int[1][];
    poi = new int[1][];
  }

  /**
   * Constructor.
   */
  public XQFTVisData() {
    if(gui) initT();
  }
  
  /**
   * Init container for textview.
   */
  public void initT() {
    tpos = new IntList();
    tcol = new IntList();
  }
  
  /**
   * Add a node.
   * pp : [pre, pos0, ..., posn]
   * p : [poiMax, poi0, ..., poin]
   * @param pp int[] pre and positions
   * @param p int[] pointer
   */
  public void add(final int[] pp, final int[] p) {
    if (!gui || pp == null || p == null) return;
    if (size + 1 > prepos.length) {
      prepos = Array.extend(prepos);
      poi = Array.extend(poi);
    }
    
    int i = 0;
    while (i < size) {
      if (prepos[i][0] < pp[0]) i++;
      else {
        add(i, pp, p);
        return;
      }
    }
    
    prepos[i] = pp;
    poi[i] = p;
    size++;
  }
  
  
  /**
   * Add pp and p at position i.
   * @param i int position where to add
   * @param pp int[] pre and position values
   * @param p int[] pointer values
   */
  private void add(final int i, final int[] pp, final int[] p) {
    if(prepos[i][0] == pp[0]) {
      // check if equal or merge
      if (Array.eq(pp, prepos[i]) && Array.eq(p, poi[i])) return;
      final int[] tpp = new int[pp.length + prepos[i].length - 1];
      final int[] tp = new int[tpp.length];
      tpp[0] = pp[0];
      tp[0] = Math.max(p[0], poi[i][0]);
      int p0 = 1, p1 = 1;
      for (int j = 1; j < tpp.length; j++) {
        if (p1 >= pp.length || p0 < prepos[i].length && 
            prepos[i][p0] < pp[p1]) {
          tpp[j] = prepos[i][p0];
          tp[j] = poi[i][p0++];
        } else {
          tpp[j] = pp[p1];
          tp[j] = p[p1++];          
        }
      }
      prepos[i] = tpp;
      poi[i] = tp;
      return;
    }
    
    if (i < size) {
      Array.move(prepos, i, 1, size - i);
      Array.move(poi, i, 1, size - i);
    }
    prepos[i] = pp;
    poi[i] = p;
    size++;
  }

  /**
   * Get fulltext data from the container.
   * If no data is stored for a pre value,
   * null is returned.
   * int[0] : [pos0, ..., posn]
   * int[1] : [poi0, ..., poin] 
   * 
   * @param pre int pre value
   * @return int[2][n] fulltext data or null
   */
  public int[][] get(final int pre) {
    if (!gui || size == 0) return null;
    int l = 0, r = size;
    // binary search
    while(l < r) {
      final int m = l + (r - l) / 2;
      final int c = prepos[m][0] - pre;
      if(c == 0)  return getFTData(m);
      else if(c < 0) l = m + 1;
      else r = m - 1;
    }
    return r != size && l == r && prepos[l][0] == pre ? getFTData(l) : null;
  }

  /**
   * Convert data to.
   * int[0] : [pos0, ..., posn]
   * int[1] : [poi0, ..., poin] 
   * 
   * @param i pointer on the data
   * @return int[2][n] fulltext data or null
   */
  private int[][] getFTData(final int i) {
    final int[][] r = new int[2][prepos[i].length - 1];
    System.arraycopy(prepos[i], 1, r[0], 0, r[0].length);
    System.arraycopy(poi[i], 1, r[1], 0, r[1].length);
    return r;
  }
  
  /**
   * Keeps all ftdata specified in pres.
   * All other ftdata is deleted.
   * 
   * @param pres int[] pre values to keep
   */
  public void keep(final int[] pres) {
    if(!gui || size == 0 || pres == null) return;
    final int[][] tpp = new int[pres.length][];
    final int[][] tp = new int[pres.length][];
    int p = 0, t = 0;
    for (int i = 0; i < size; i++) {
      if (prepos[i][0] == pres[p]) {
        tpp[t] = prepos[i];
        tp[t++] = poi[i];
        p++;
      }
    }
    prepos = tpp;
    poi = tp;
    size = pres.length;
  }
  
  /**
   * Remove ftdata for this node.
   * 
   * @param pre int pre value
   */
  public void remove(final int pre) {
    if (!gui || size == 0) return;
    int l = 0, r = size;
    // binary search
    while(l < r) {
      final int m = l + (r - l) / 2;
      final int c = prepos[m][0] - pre;
      if(c == 0)  { 
        removeFTData(m);
        return;
      } else if(c < 0) l = m + 1;
      else r = m - 1;
    }
    if(r != size && l == r && prepos[l][0] == pre) 
      removeFTData(l);
  }
  
  /**
   * Remove ftdata for this node.
   * @param i pointer on node entry
   */
  private void removeFTData(final int i) {
    System.out.println(size);
    System.arraycopy(prepos, i, prepos, i, size - i - 1);
    System.arraycopy(poi, i, poi, i, size - i - 1);
    size--;
  }
  
  /**
   * Converts data from sequential ftcontains processing.
   * 
   * @param d IntList[] ftcontains data
   * @param pre int pre value
   * @param div int div to add for each pointer
   */
  public void addConvSeqData(final IntList[] d, final int pre, 
      final int div) {
    if (!gui  || d.length == 0) return;
    
    int[] pp;
    int[] p;
    pp = new int[d[0].size + 1];
    p = new int[pp.length];
    pp[0] = pre;
    final int[] tmp = d[0].finish();
    System.arraycopy(tmp, 0, pp, 1, tmp.length);
    for (int i = 0; i < p.length; i++) p[i] = div;
    
    for(int c = 1; c < d.length; c++) {
      int p0 = 1, p1 = 0, p2 = 1;
      int[] ppn = new int[d[c].size + pp.length];
      int[] pn = new int[d[c].size + p.length];
      
      ppn[0] = pp[0];
      pn[0] = Math.max(c + div, p[0]);
      
      while(p0 < pp.length && p1 < d[c].size) {
        if(d[c].list[p1] < pp[p0]) {
          ppn[p2] = d[c].list[p1++];
          pn[p2++] = c + div;
        } else {
          ppn[p2] = pp[p0];
          pn[p2++] = p[p0++];    
        }
      }
      while(p1 < d[c].size) {
        ppn[p2] = d[c].list[p1++];
        pn[p2++] = c + div;
      }    
      while(p0 < pp.length) {
        ppn[p2] = pp[p0];
        pn[p2++] = p[p0++];
      }    
      
      pp = ppn;
      p = pn;
      c++;
    }
    add(pp, p);
  }
  
  /**
   * Compares current and last ftdata for equality.
   * @return boolean same()
   */
  public boolean same() {
    if (!gui) return true;
    if (osize != size) return false;
    for (int i = 0; i < osize; i++) {
      if (!Array.eq(prepos[i], oprepos[i]) 
          || Array.eq(poi[i], opoi[i])) return false;
    }

    return true;
  }
  
  /**
   * Add a text position and color value.
   * Used to color text in the text view.
   * 
   * @param pos int real position value of the token
   * @param col int color 
   */
  public void addTextPos(final int pos, final int col) {
    tpos.add(pos);
    tcol.add(col);    
  }
  
  /**
   * Get real text position and color for a token.
   * Used to color text in the text view.
   * 
   * @param pos int real position value of the token
   * @return col int color
   */
  public int getTextCol(final int pos) {
    int tc = 0;
    while (tpos.size > 0 && tc < tcol.size && tpos.list[tc] < pos) tc++; 
    return tpos.size > 0 && tpos.list[tc] == pos ? tcol.list[tc++] : -1;  
  }

  /*
  public static String toString(final int[] p) {
    if (p == null) return new String("[null]");
    StringBuffer sb = new StringBuffer();
    sb.append("[" + p[0]);
    for (int i = 1; i < p.length; i++) sb.append(", " + p[i]);
    sb.append("]");
    return sb.toString();
  }
  */
}
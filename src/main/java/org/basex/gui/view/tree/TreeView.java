package org.basex.gui.view.tree;

import static org.basex.gui.GUIConstants.*;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Transparency;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import javax.swing.SwingUtilities;
import org.basex.data.Data;
import org.basex.data.Nodes;
import org.basex.gui.GUIConstants;
import org.basex.gui.GUIProp;
import org.basex.gui.layout.BaseXLayout;
import org.basex.gui.layout.BaseXPopup;
import org.basex.gui.view.View;
import org.basex.gui.view.ViewData;
import org.basex.gui.view.ViewNotifier;
import org.basex.gui.view.ViewRect;
import org.basex.util.IntList;
import org.basex.util.Token;

/**
 * This class offers a real tree view.
 * 
 * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
 * @author Wolfgang Miller
 */
public final class TreeView extends View implements TreeViewOptions {
  /** Current font height. */
  private int fontHeight;
  /** TreeCaching Object, contains cached pre values. */
  private TreeCaching cache;
  /** Current mouse position x. */
  private int mousePosX = -1;
  /** Current mouse position y. */
  private int mousePosY = -1;
  /** Window width. */
  private int wwidth = -1;
  /** Window height. */
  private int wheight = -1;
  /** Currently focused rectangle. */
  private TreeRect focusedRect;
  /** Level of currently focused rectangle. */
  private int focusedRectLevel = -1;
  /** Current Image of visualization. */
  private BufferedImage treeImage;
  /** Notified focus flag. */
  private boolean refreshedFocus;
  /** Distance between the levels. */
  private static int levelDistance;
  /** Image of the current marked nodes. */
  private BufferedImage markedImage;
  /** If something is selected. */
  private boolean selection;
  /** The selection rectangle. */
  private ViewRect selectRect;
  /** The node height. */
  private int nodeHeight;
  /** Top margin. */
  private int topMargin;
  /** Distance between multiple trees. */
  private double treedist;
  /** Focused root number. */
  private int frn;
  /** Focused index. */
  private int fix;
  /** Number of roots. */
  private int numRoots;

  /**
   * Default Constructor.
   * @param man view manager
   */
  public TreeView(final ViewNotifier man) {
    super(TREEVIEW, null, man);
    new BaseXPopup(this, GUIConstants.POPUP);
  }

  @Override
  public void refreshContext(final boolean more, final boolean quick) {
    wwidth = -1;
    wheight = -1;
    repaint();
  }

  @Override
  public void refreshFocus() {
    refreshedFocus = true;
    repaint();
  }

  @Override
  public void refreshInit() {
    if(!visible()) return;
    wwidth = -1;
    wheight = -1;
    cache = new TreeCaching(gui.context.data, gui.prop);
    repaint();
  }

  @Override
  public void refreshLayout() {
    wwidth = -1;
    wheight = -1;
    repaint();
  }

  @Override
  public void refreshMark() {
    markNodes();
    repaint();
  }

  @Override
  public void refreshUpdate() {
    repaint();
  }

  @Override
  public boolean visible() {
    return gui.prop.is(GUIProp.SHOWTREE);
  }

  @Override
  protected boolean db() {
    return true;
  }

  @Override
  public void paintComponent(final Graphics g) {
    super.paintComponent(g);

    // timer
    // final Performance perf = new Performance();
    // perf.initTimer();

    // initializes sizes
    nodeHeight = MAX_NODE_HEIGHT;
    smooth(g);
    g.setFont(font);
    fontHeight = g.getFontMetrics().getHeight();

    if(windowSizeChanged()) {
      if(markedImage != null) {
        markedImage = null;
      }
      focusedRect = null;
      if((treedist = cache.generateBordersAndRects(g, gui.context, getWidth())) 
      == -1) return;
      setLevelDistance();
      createNewMainImage();
      if(gui.context.marked.size() > 0) markNodes();

    } else setLevelDistance();
    g.drawImage(treeImage, 0, 0, getWidth(), getHeight(), this);

    if(selection) markSelektedNodes(g);

    if(markedImage != null) g.drawImage(markedImage, 0, 0, getWidth(),
        getHeight(), this);

    // highlights the focused node

    if(focus()) {
      final int focused = gui.context.focused;

      highlightNode(g, frn, focused, focusedRect, focusedRectLevel, -1,
          DRAW_HIGHLIGHT);

      if(SHOW_EXTRA_INFO) {
        g.setColor(new Color(0xeeeeee));
        g.fillRect(0, getHeight() - fontHeight - 6, getWidth(), fontHeight + 6);
        final Data d = gui.context.data;
        final int k = d.kind(focused);
        final int s = d.size(focused, k);
        final int as = d.attSize(focused, k);
        g.setColor(Color.BLACK);
        g.drawString("pre: " + focused + "level: " + focusedRectLevel
            + "  level-size: " + cache.getLevelSize(frn, focusedRectLevel)
            + "  node-size: " + (s - as) + "  node: "
            + Token.string(ViewData.tag(gui.prop, d, focused)), 2,
            getHeight() - 6);
      }
    }
  }

  /**
   * Creates new image and draws rectangles in it.
   */
  private void createNewMainImage() {
    treeImage = createImage();
    final Graphics tg = treeImage.getGraphics();
    tg.setFont(font);
    smooth(tg);
    final int[] roots = gui.context.current.nodes;
    numRoots = roots.length;

    for(int rn = 0; rn < numRoots; rn++) {

      final int h = cache.getHeight(rn);

      for(int lv = 0; lv < h; lv++) {

        final boolean br = cache.isBigRectangle(rn, lv);
        final TreeRect[] lr = cache.getTreeRectsPerLevel(rn, lv);

        for(int i = 0; i < lr.length; i++) {
          final TreeRect r = lr[i];
          final int pre = cache.getPrePerIndex(rn, lv, i);
          drawRectangle(tg, rn, lv, r, pre, DRAW_RECTANGLE);
        }

        if(br) {
          final TreeRect r = lr[0];
          int nh = nodeHeight;
          final int w = r.x + r.w - 1;
          int x = r.x + 1;
          final int y = getYperLevel(lv);
          int box = 4;

          tg.setColor(GUIConstants.back);
          while(nh > 0) {
            nh = nh - box;
            tg.drawLine(x, y + nh, w, y + nh);
          }

          while(x < w) {
            x = x + box;
            tg.drawLine(x, y, x, y + nodeHeight);
          }
        }
      }
      if(SHOW_CONN_MI) highlightNode(tg, rn, roots[rn],
          cache.getTreeRectPerIndex(rn, 0, 0), 0, -1, DRAW_CONN);
    }
  }

  /**
   * Draws Rectangles.
   * @param g graphics reference
   * @param rn root
   * @param lv level
   * @param r rectangle
   * @param pre pre
   * @param type use type
   */
  private void drawRectangle(final Graphics g, final int rn, final int lv,
      final TreeRect r, final int pre, final byte type) {

    final int y = getYperLevel(lv);
    final int h = nodeHeight;
    final boolean br = cache.isBigRectangle(rn, lv);
    boolean txt = !br && fontHeight <= h;
    boolean fill = true;
    boolean border = true;

    Color borderColor = null;
    Color fillColor = null;
    Color textColor = Color.BLACK;

    switch(type) {
      case DRAW_RECTANGLE:
        borderColor = getColorPerLevel(lv, false);
        fillColor = getColorPerLevel(lv, true);
        txt = txt && DRAW_NODE_TEXT;
        border = BORDER_RECTANGLES;
        fill = FILL_RECTANGLES;
        break;
      case DRAW_HIGHLIGHT:
        borderColor = color6;
        final int alpha = 0xDD000000;
        final int rgb = GUIConstants.COLORCELL.getRGB();
        fillColor = new Color(rgb + alpha, true);
        border = true;
        fill = !br;
        break;
      case DRAW_MARK:
        borderColor = h > 2 && r.w > 4 ? colormarkA : colormark1;
        fillColor = colormark1;
        break;
      case DRAW_DESCENDANTS:
        final int alphaD = 0xDD000000;
        final int rgbD = COLORS[6].getRGB();
        fillColor = new Color(rgbD + alphaD, true);
        borderColor = COLORS[8];
        textColor = Color.WHITE;
        fill = true;
        border = true;
        if(h < 2 || r.w < 4) {
          borderColor = fillColor;
          txt = false;
        }
        break;
      case DRAW_PARENT:
        fillColor = COLORS[6];
        textColor = Color.WHITE;
        fill = !br;
        if(h < 2 || r.w < 4) {
          borderColor = COLORS[6];
          border = true;
          txt = false;
        }
        break;
    }

    final int xx = r.x;
    final int ww = r.w;

    if(border) {
      g.setColor(borderColor);
      g.drawRect(xx, y, ww, h);
      // g.drawRoundRect(xx, y, ww, h, 20, 20);
      // if(type == DRAW_HIGHLIGHT)
      // g.drawRect(xx + 1, y + 1, ww - 2, h - 2);
    }
    if(fill) {
      g.setColor(fillColor);
      g.fillRect(xx + 1, y + 1, ww - 1, h - 1);
      // g.fillRoundRect(xx, y, ww, h, 20, 20);
    }

    if(txt && (fill || !FILL_RECTANGLES)) {
      g.setColor(textColor);
      drawRectangleText(g, rn, lv, r, pre);
    }
  }

  /**
   * Draws text into rectangle.
   * @param g graphics reference
   * @param rn root
   * @param lv level
   * @param r rectangle
   * @param pre pre
   */
  private void drawRectangleText(final Graphics g, final int rn, final int lv,
      final TreeRect r, final int pre) {

    if(r.w < MIN_TXT_SPACE) return;

    final int x = r.x;
    final int y = getYperLevel(lv);
    final int rm = x + (int) (r.w / 2f);

    String s = Token.string(cache.getText(gui.context, rn, pre));

    int tw = 0;
    while((tw = BaseXLayout.width(g, s)) + 4 > r.w) {
      s = s.substring(0, s.length() / 2).concat("*");
    }
    final int yy = (int) (y + (nodeHeight + fontHeight - 4) / 2d);
    g.drawString(s, (int) (rm - tw / 2d + BORDER_PADDING), yy);
  }

  /**
   * Returns draw Color.
   * @param l the current level
   * @param fill if true it returns fill color, rectangle color else
   * @return draw color
   */
  private Color getColorPerLevel(final int l, final boolean fill) {
    final int till = l < CHANGE_COLOR_TILL ? l : CHANGE_COLOR_TILL;
    return fill ? COLORS[till] : COLORS[till + 2];
  }

  /**
   * Draws the dragged selection and marks the nodes inside.
   * @param g the graphics reference
   */
  private void markSelektedNodes(final Graphics g) {
    final int x = selectRect.w < 0 ? selectRect.x + selectRect.w : selectRect.x;
    final int y = selectRect.h < 0 ? selectRect.y + selectRect.h : selectRect.y;
    final int w = Math.abs(selectRect.w);
    final int h = Math.abs(selectRect.h);

    // draw selection
    g.setColor(Color.RED);
    g.drawRect(x, y, w, h);

    final int t = y + h;
    final int size = cache.maxLevel;
    final IntList list = new IntList();

    for(int i = 0; i < size; i++) {
      final int yL = getYperLevel(i);

      if(i < cache.getHeight(frn) && (yL >= y || yL + nodeHeight >= y)
          && (yL <= t || yL + nodeHeight <= t)) {

        final TreeRect[] rlv = cache.getTreeRectsPerLevel(frn, i);
        final int s = cache.getLevelSize(frn, i);

        if(cache.isBigRectangle(frn, i)) {
          final TreeRect mRect = rlv[0];
          int sPrePos = (int) (s * (x / (double) mRect.w));
          int ePrePos = (int) (s * ((x + w) / (double) mRect.w));

          if(sPrePos < 0) sPrePos = 0;
          if(ePrePos >= s) ePrePos = s - 1;

          while(sPrePos++ < ePrePos)
            list.add(cache.getPrePerIndex(frn, i, sPrePos));
        } else {
          for(int j = 0; j < s; j++) {
            final TreeRect tr = rlv[j];
            if(tr.contains(x, w)) list.add(cache.getPrePerIndex(frn, i, j));
          }
        }
        gui.notify.mark(new Nodes(list.finish(), gui.context.data), this);
        markNodes();
      }
    }
  }

  /**
   * Creates a new translucent BufferedImage.
   * @return new translucent BufferedImage
   */
  private BufferedImage createImage() {
    return new BufferedImage(Math.max(1, getWidth()), Math.max(1, getHeight()),
        Transparency.TRANSLUCENT);
  }

  /**
   * Highlights the marked nodes.
   */
  private void markNodes() {
    markedImage = createImage();
    final Graphics mg = markedImage.getGraphics();
    smooth(mg);

    final int size = gui.context.marked.size();
    if(size == 0) return;

    final int[] marked = Arrays.copyOf(gui.context.marked.nodes, size);

    int rn = 0;

    while(rn < numRoots) {

      for(int i = 0; i < cache.getHeight(rn); i++) {

        final int y = getYperLevel(i);

        if(cache.isBigRectangle(rn, i)) {

          for(int j = 0; j < size; j++) {
            final int pre = marked[j];

            final int ix = cache.getPreIndex(rn, i, pre);

            if(ix > -1) {
              final int x = (int) (getWidth() * ix / 
              (double) cache.getLevelSize(
                  rn, i));
              mg.setColor(Color.RED);
              mg.drawRect(x, y, 2, nodeHeight);
            }
          }
        } else {

          for(int j = 0; j < size; j++) {
            final int pre = marked[j];

            final TreeRect rect = cache.searchRect(rn, i, pre);

            if(rect != null) {
              drawRectangle(mg, rn, i, rect, pre, DRAW_MARK);
            }
          }
        }
      }
      rn++;
    }
  }

  /**
   * Highlights nodes.
   * @param g the graphics reference
   * @param rn root
   * @param pre pre
   * @param r rectangle to highlight
   * @param l level
   * @param cx child's x value
   * @param t highlight type
   */
  private void highlightNode(final Graphics g, final int rn, final int pre,
      final TreeRect r, final int l, final int cx, final byte t) {

    if(l == -1) return;

    final int y = getYperLevel(l);
    final int h = nodeHeight;
    final boolean br = cache.isBigRectangle(rn, l);
    final boolean root = gui.context.current.nodes[rn] == pre;
    final int height = cache.getHeight(rn);

//    System.out.println("rn: " + rn + " t: " + t + " br: " + br + " root "
//        + root);

    final Data d = gui.context.data;
    final int k = d.kind(pre);
    final int size = d.size(pre, k);
    // big rectangle x
    int brx = -1;

    if(t != DRAW_CONN) drawRectangle(g, rn, l, r, pre, t);

    if(br) {
      final int index = cache.getPreIndex(rn, l, pre);
      if(t == DRAW_HIGHLIGHT) fix = index;
      final double ratio = index / (double) (cache.getLevelSize(rn, l) - 1);

//      System.out.println("index:" + index + " lsize: "
//          + cache.getLevelSize(rn, l) + "w:" + r.w + " ratio:" + ratio);

      brx = r.x + (int) (r.w * ratio);
      g.setColor(COLORS[7]);
      g.drawLine(brx, y, brx, y + nodeHeight);
    }

    if(cx > -1 && MIN_NODE_DIST_CONN <= levelDistance) {
      g.setColor(COLORS[7]);
      g.drawLine(cx, getYperLevel(l + 1) - 1, brx == -1 ? (2 * r.x + r.w) / 2
          : brx, y + nodeHeight + 1);
    }

    if((t == DRAW_HIGHLIGHT || t == DRAW_PARENT) && !root) {
      final int par = d.parent(pre, k);
      final int lv = l - 1;

//    System.out.println("brx: " + brx + "   level: " + lv + "   pre: " + pre);

      final TreeRect parRect = cache.searchRect(rn, lv, par);
      if(parRect == null) return;
      highlightNode(g, rn, par, parRect, lv, brx == -1 ? (2 * r.x + r.w) / 2
          : brx, DRAW_PARENT);
    }

    if((t == DRAW_CONN || t == DRAW_HIGHLIGHT || t == DRAW_DESCENDANTS)
        && size > 1 && l + 1 < height) highlightDescendants(g, rn, pre, r, l,
        brx, t);

    if(t != DRAW_HIGHLIGHT) return;

    final String s = Token.string(cache.getText(gui.context, rn, pre));
    final int w = BaseXLayout.width(g, s);

    g.setColor(COLORS[l + 5]);

    if(root) {
      g.fillRect(r.x, y + h + 2, w + 2, fontHeight);
      g.setColor(Color.WHITE);
      g.drawString(s, r.x + 1, (int) (y + h + (float) fontHeight) - 2);
    } else {
      g.fillRect(r.x, y - fontHeight, w + 2, fontHeight);
      g.setColor(Color.WHITE);
      g.drawString(s, r.x + 1, (int) (y - h / (float) fontHeight) - 2);
    }

  }

  /**
   * Highlights descendants.
   * @param g the graphics reference
   * @param rn root
   * @param pre pre
   * @param r rectangle to highlight
   * @param l level
   * @param px parent's x value
   * @param t highlight type
   */
  private void highlightDescendants(final Graphics g, final int rn,
      final int pre, final TreeRect r, final int l, 
      final int px, final byte t) {

    final Data d = gui.context.current.data;
    if(!cache.isBigRectangle(rn, l) && t != DRAW_CONN) drawRectangle(g, rn, l,
        r, pre, t);
    final int lv = l + 1;

    final TreeBorder[] sbo = cache.generateSubtreeBorders(d, pre);

    if(cache.getHeight(rn) <= lv || sbo.length < 2) return;
    final int parc = px == -1 ? (2 * r.x + r.w) / 2 : px;

    if(cache.isBigRectangle(rn, lv)) {
      drawBigRectDescendants(g, rn, lv, sbo, parc, t);

    } else {
      final TreeBorder bo = sbo[1];
      final TreeBorder bos = cache.getTreeBorder(rn, lv);

      for(int j = 0; j < bo.size; j++) {
        int pi = cache.getPrePerIndex(bo, j);
//        if(gui.context.current.nodes[0] > 0) System.out.println("rn:" + rn
//            + " lv:" + lv + " bo-size:" + bo.size + " bo-start:" + (bo.start)
//            + " bos:" + bos.start + " rl:" + cache.rects.length + " rlvl:"
//            + cache.rects[rn].length + " r:" + cache.rects[rn][lv].length);

        final int start = bo.start >= bos.start ? bo.start - bos.start
            : bo.start;
        final TreeRect sr = cache.getTreeRectPerIndex(rn, lv, j + start);

        if(SHOW_DESCENDANTS_CONN && levelDistance >= MIN_NODE_DIST_CONN) {
          drawDescendantsConn(g, lv, sr, parc, t);
        }

        highlightDescendants(g, rn, pi, sr, lv, -1, t == DRAW_CONN ? DRAW_CONN
            : DRAW_DESCENDANTS);
      }
    }
  }

  /**
   * Draws descendants for big rectangles.
   * @param g graphics reference
   * @param rn root
   * @param lv level
   * @param subt subtree
   * @param parc parent center
   * @param t type
   */
  private void drawBigRectDescendants(final Graphics g, final int rn,
      final int lv, final TreeBorder[] subt, final int parc, final byte t) {

    final int h = cache.getHeight(rn);
    int lvv = lv;
    int cen = parc;
    int i;

    for(i = 1; i < subt.length && cache.isBigRectangle(rn, lvv); i++) {

      final TreeBorder bos = cache.getTreeBorder(rn, lvv);
      final TreeBorder bo = subt[i];

      final TreeRect r = cache.getTreeRectPerIndex(rn, lvv, 0);
      final int start = bo.start - bos.start;
      final double sti = start / (double) bos.size;
      final double eni = (start + bo.size) / (double) bos.size;

      final int df = r.x + (int) ((r.w) * sti);
      final int dt = r.x + (int) ((r.w) * eni);
      final int ww = Math.max(dt - df, 2);

      if(MIN_NODE_DIST_CONN <= levelDistance) drawDescendantsConn(g, lvv,
          new TreeRect(df, ww), cen, t);
      cen = (2 * df + ww) / 2;
      switch(t) {
        case DRAW_CONN:
          break;
        default:
          // g.setColor(COLORS[1]);
          // g.drawRect(df, getYperLevel(lvv), ww, nodeHeight);
          final int rgb = COLORS[7].getRGB();
          final int alpha = 0x33000000;
          g.setColor(new Color(rgb + alpha, false));
          g.fillRect(df, getYperLevel(lvv), ww, nodeHeight);
      }

      if(lvv + 1 < h
          && !cache.isBigRectangle(rn, lvv + 1)) {
        final Data d = gui.context.current.data;
        for(int j = 0; j < subt[i].size; j++) {
          final int pre = cache.getPrePerIndex(rn, lvv, j);
          final int k = d.kind(pre);
          final int s = d.size(pre, k) - d.attSize(pre, k);
          if(s > 0) highlightDescendants(g, rn, pre, r, lvv, cen,
              t == DRAW_HIGHLIGHT || t == DRAW_DESCENDANTS ? DRAW_DESCENDANTS
                  : DRAW_CONN);
        }
        return;
      }
      lvv++;
    }
  }

  /**
   * Draws descendants connection.
   * @param g graphics reference
   * @param lv level
   * @param r TreeRect
   * @param parc parent center
   * @param t type
   */
  private void drawDescendantsConn(final Graphics g, final int lv,
      final TreeRect r, final int parc, final byte t) {

    final int pary = getYperLevel(lv - 1) + nodeHeight;
    final int prey = getYperLevel(lv);
    final int boRight = r.x + r.w + BORDER_PADDING - 2;
    final int boLeft = r.x + BORDER_PADDING;
    final int boBottom = prey + nodeHeight + 1;
    final int boTop = prey + 1;
    final int parmx = r.x + (int) ((boRight - boLeft) / 2d);
    Color c = null;
    int alpha;
    int rgb;

    switch(t) {
      case DRAW_CONN:
        alpha = 0x20000000;
        rgb = COLORS[4].getRGB();
        c = new Color(rgb + alpha, true);
        break;
      default:
      case DRAW_DESCENDANTS:
        alpha = 0x60000000;
        rgb = COLORS[8].getRGB();
        c = new Color(rgb + alpha, true);
        break;
    }

    if(SHOW_3D_CONN) {
      final int dis = 0x111111;
      final Color ca = new Color(rgb + alpha - dis, false);
      final Color cb = new Color(rgb + alpha + dis, false);
      // g.setColor(new Color(0x444444, false));

      g.setColor(cb);
      // g.setColor(new Color(0x666666, false));
      g.drawPolygon(new int[] { parc, boRight, boRight}, new int[] { pary,
          boBottom, boTop}, 3);

      // g.setColor(new Color(0x666666, false));
      g.drawPolygon(new int[] { parc, boLeft, boLeft}, new int[] { pary,
          boBottom, boTop}, 3);

      g.setColor(c);
      // g.setColor(new Color(0x555555, false));
      g.drawPolygon(new int[] { parc, boLeft, boRight}, new int[] { pary,
          boTop, boTop}, 3);

      g.setColor(Color.BLACK);

      if(parmx < parc) g.drawLine(boRight, boBottom, parc, pary);
      else if(parmx > parc) g.drawLine(boLeft, boBottom, parc, pary);

      // g.setColor(cb);
      // // g.setColor(new Color(0x666666, false));
      // g.drawLine(boRight, boTop, parc, pary);
      // g.drawLine(boLeft, boTop, parc, pary);
    } else {
      g.setColor(c);
      if(boRight - boLeft > 2) {
        g.fillPolygon(new int[] { parc, boRight, boLeft}, new int[] { pary,
            boTop, boTop}, 3);
      } else {
        g.drawLine((boRight + boLeft) / 2, boTop, parc, pary);
      }
    }

  }

  /**
   * Finds rectangle at cursor position.
   * @return focused rectangle
   */
  private boolean focus() {
    if(refreshedFocus) {
      final int pre = gui.context.focused;

      for(int i = 0; i < cache.getHeight(frn); i++) {

        if(cache.isBigRectangle(frn, i)) {
          final int index = cache.getPreIndex(frn, i, pre);

          if(index > -1) {
            focusedRect = cache.getTreeRectsPerLevel(frn, i)[0];
            focusedRectLevel = i;
            refreshedFocus = false;
            return true;
          }
        } else {

          final TreeRect rect = cache.searchRect(0, i, pre);

          if(rect != null) {
            focusedRect = rect;
            focusedRectLevel = i;
            refreshedFocus = false;
            return true;
          }
        }
      }
    } else {

      final int rn = frn = getTreePerX(mousePosX);
      final int lv = getLevelPerY(mousePosY);

      if(lv < 0 || cache.getHeight(rn) < 0 || lv >= cache.getHeight(rn)) 
      return false;

      final TreeRect[] rL = cache.getTreeRectsPerLevel(rn, lv);

      for(int i = 0; i < rL.length; i++) {
        final TreeRect r = rL[i];

        if(r.contains(mousePosX)) {
          focusedRect = r;
          focusedRectLevel = lv;
          int pre = -1;

          // if multiple pre values, then approximate pre value
          if(cache.isBigRectangle(rn, lv)) {
            pre = cache.getPrePerXPos(rn, lv, mousePosX);
          } else {
            pre = cache.getPrePerIndex(rn, lv, i);
          }
          gui.notify.focus(pre, this);
          refreshedFocus = false;
          return true;
        }
      }
    }
    refreshedFocus = false;
    return false;
  }

  /**
   * Returns the y-axis value for a given level.
   * @param level the level
   * @return the y-axis value
   */
  private int getYperLevel(final int level) {
    return level * nodeHeight + level * levelDistance + topMargin;
  }

  /**
   * Determines tree number.
   * @param x x-axis value
   * @return tree number
   */
  private int getTreePerX(final int x) {
    return (int) (x / treedist);
  }

  /**
   * Determines the level of a y-axis value.
   * @param y the y-axis value
   * @return the level if inside a node rectangle, -1 else
   */
  private int getLevelPerY(final int y) {
    final double f = (y - topMargin) / ((float) levelDistance + nodeHeight);
    final double b = nodeHeight / (float) (levelDistance + nodeHeight);
    return f <= ((int) f + b) ? (int) f : -1;
  }

  /**
   * Sets optimal distance between levels.
   */
  private void setLevelDistance() {
    final int h = getHeight() - BOTTOM_MARGIN;
    final int rl = gui.context.current.nodes.length;
    int lvs = 0;
    for(int i = 0; i < rl; i++) {
      final int th = cache.getHeight(i);
      if(th > lvs) lvs = th;
    }
    int lD;
    while((lD = (int) ((h - lvs * nodeHeight) / (double) (lvs - 1))) < 
    (nodeHeight < CHANGE_NODE_HEIGHT_TILL ? MIN_LEVEL_DISTANCE
        : BEST_LEVEL_DISTANCE)
        && nodeHeight >= MIN_NODE_HEIGHT)
      nodeHeight--;
    levelDistance = lD < MIN_LEVEL_DISTANCE ? MIN_LEVEL_DISTANCE
        : lD > MAX_LEVEL_DISTANCE ? MAX_LEVEL_DISTANCE : lD;
    final int ih = (int) ((h - (levelDistance * (lvs - 1) + lvs * nodeHeight)) 
    / 2d);
    topMargin = ih < TOP_MARGIN ? TOP_MARGIN : ih;
  }

  /**
   * Returns true if window-size has changed.
   * @return window-size has changed
   */
  private boolean windowSizeChanged() {
    if((wwidth > -1 && wheight > -1)
        && (getHeight() == wheight && getWidth() == wwidth)) return false;
    wheight = getHeight();
    wwidth = getWidth();
    return true;
  }

  @Override
  public void mouseMoved(final MouseEvent e) {
    if(gui.updating) return;
    super.mouseMoved(e);
    // refresh mouse focus
    mousePosX = e.getX();
    mousePosY = e.getY();
    repaint();
  }

  @Override
  public void mouseClicked(final MouseEvent e) {
    final boolean left = SwingUtilities.isLeftMouseButton(e);
    final boolean right = !left;
    if(!right && !left || focusedRect == null) return;

    if(left) {

      if(focusedRectLevel >= cache.getHeight(frn)) return;

      if(cache.isBigRectangle(frn, focusedRectLevel)) {
        final Nodes ns = new Nodes(gui.context.data);
        final int w = focusedRect.w;
        final int ls = cache.getLevelSize(frn, focusedRectLevel);
        final int sum = (int) Math.max(ls / (double) w, 1);
        int[] m = new int[sum];
        for(int i = 0; i < sum; i++) {
          m[i] = cache.getPrePerIndex(frn, focusedRectLevel, i + fix);
        }
        ns.union(m);
        gui.notify.mark(ns, null);
      } else {
        gui.notify.mark(0, null);
      }
      if(e.getClickCount() > 1) {
        gui.notify.context(gui.context.marked, false, this);
        refreshContext(false, false);
      }
    }
  }

  @Override
  public void mouseWheelMoved(final MouseWheelEvent e) {
    if(gui.updating || gui.context.focused == -1) return;
    if(e.getWheelRotation() > 0) gui.notify.context(new Nodes(
        gui.context.focused, gui.context.data), false, null);
    else gui.notify.hist(false);
  }

  @Override
  public void mouseDragged(final MouseEvent e) {
    if(gui.updating || e.isShiftDown()) return;

    if(!selection) {
      selection = true;
      selectRect = new ViewRect();
      selectRect.x = e.getX();
      selectRect.y = e.getY();
      selectRect.h = 1;
      selectRect.w = 1;

    } else {
      final int x = e.getX();
      final int y = e.getY();
      selectRect.w = x - selectRect.x;
      selectRect.h = y - selectRect.y;
    }
    repaint();
  }

  @Override
  public void mouseReleased(final MouseEvent e) {
    if(gui.updating || gui.painting) return;
    selection = false;
    repaint();
  }
}
package org.basex.query.xquery.expr;

import static org.basex.query.xquery.XQText.*;
import java.io.IOException;

import org.basex.data.Serializer;
import org.basex.query.FTPos;
import org.basex.query.xquery.XQContext;
import org.basex.query.xquery.XQException;
import org.basex.query.xquery.item.FTNodeItem;
import org.basex.query.xquery.iter.FTNodeIter;
import org.basex.query.xquery.iter.Iter;
import org.basex.query.xquery.util.Err;

/**
 * FTSelect expression.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-08, ISC License
 * @author Christian Gruen
 */
public final class FTSelectIndex extends FTExpr {
  /** Position filter. */
  public FTPos pos;
  /** Window. */
  public Expr window;
  /** Distance occurrences. */
  public Expr[] dist;
  /** Weight. */
  public Expr weight;

  /**
   * Constructor.
   * @param e expression
   * @param u fulltext selections
   * @param w weight
   */
  public FTSelectIndex(final FTExpr e, final FTPos u, final Expr w) {
    super(e);
    pos = u;
    weight = w;
  }

  @Override
  public Iter iter(final XQContext ctx) {
    return new FTNodeIter() {
      @Override
      public FTNodeItem next() throws XQException {
        final FTPos tmp = ctx.ftpos;
        ctx.ftpos = pos;
        pos.init(ctx.ftitem);
        FTNodeItem it = (FTNodeItem) ctx.iter(expr[0]).next();
        
        if (it.ftn.size > 0) {
          init(it);
          while (!posFilter(ctx)) {
            it = (FTNodeItem) ctx.iter(expr[0]).next();
            if(it.ftn.size == 0) {
              ctx.ftpos = tmp;
              return it;
            }
            init(it);
          } 
        }    

        // calculate weight
        final double d = checkDbl(ctx.iter(weight));
        if(d < 0 || d > 1000) Err.or(FTWEIGHT, d);
        if (d != -1) it.setDbl(it.dbl() * d);

        ctx.ftpos = tmp;
        return it;
      }
    };
  }

  /**
   * Init FTPos for next seqEval with index use.
   * @param it current FTNode 
   */
  public void init(final FTNodeItem it) {
    pos.setPos(it.ftn.convertPos(), it.ftn.p.list[0]);
    if (it.ftn.getToken() != null) {
      pos.ft.init(it.data.text(it.ftn.getPre()));
      pos.term = pos.ft.getTokenList();
    }
  }

  
  /**
   * Evaluates the position filters.
   * @param ctx query context
   * @return result of check
   * @throws XQException query exception
   */
  public boolean posFilter(final XQContext ctx) throws XQException {
    if(!pos.valid()) return false;

    // ...distance?
    if(pos.dunit != null) {
      final long mn = checkItr(ctx.iter(dist[0]));
      final long mx = checkItr(ctx.iter(dist[1]));
      if(!pos.distance(mn, mx)) return false;
    }
    // ...window?
    if(pos.wunit != null) {
      final long c = checkItr(ctx.iter(window));
      if(!pos.window(c)) return false;
    }
    return true;
  }

  @Override
  public void plan(final Serializer ser) throws IOException {
    ser.startElement(this);
    pos.plan(ser);
    ser.finishElement();
    expr[0].plan(ser);
    ser.closeElement();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(expr[0]);
    sb.append(pos);
    if(pos.dunit != null) {
      sb.append(" distance(");
      sb.append(dist[0]);
      sb.append(",");
      sb.append(dist[1]);
      sb.append(")");
    }
    return sb.toString();
  }
}
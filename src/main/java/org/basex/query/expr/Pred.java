package org.basex.query.expr;

import static org.basex.query.QueryText.*;
import java.io.IOException;
import org.basex.data.Serializer;
import org.basex.query.QueryContext;
import org.basex.query.QueryException;
import org.basex.query.func.Fun;
import org.basex.query.func.FunDef;
import org.basex.query.item.Bln;
import org.basex.query.item.Item;
import org.basex.query.item.Seq;
import org.basex.query.item.SeqType;
import org.basex.query.iter.Iter;
import org.basex.query.iter.SeqIter;
import org.basex.query.path.AxisPath;
import org.basex.query.util.Var;

/**
 * Predicate expression.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
 * @author Christian Gruen
 */
public class Pred extends Preds {
  /** Expression. */
  Expr root;
  /** Counter flag. */
  private boolean counter;

  /**
   * Constructor.
   * @param r expression
   * @param p predicates
   */
  public Pred(final Expr r, final Expr... p) {
    super(p);
    root = r;
  }

  @Override
  public final Expr comp(final QueryContext ctx) throws QueryException {
    root = checkUp(root, ctx).comp(ctx);

    // if possible, convert filters to axis paths
    if(root instanceof AxisPath && !super.uses(Use.POS, ctx)) {
      AxisPath path = ((AxisPath) root).copy();
      for(final Expr p : pred) path = path.addPred(p);
      return path.comp(ctx);
    }

    final Item tmp = ctx.item;
    ctx.item = null;
    final Expr e = super.comp(ctx);
    ctx.item = tmp;
    if(e != this) return e;

    if(root.e()) {
      ctx.compInfo(OPTPRE, this);
      return Seq.EMPTY;
    }

    // no predicates.. return root
    if(pred.length == 0) return root;
    final Expr p = pred[0];

    // position predicate
    final Pos pos = p instanceof Pos ? (Pos) p : null;
    // last flag
    final boolean last = p instanceof Fun && ((Fun) p).func == FunDef.LAST;
    // use iterative evaluation
    if(pred.length == 1 && (last || pos != null || !uses(Use.POS, ctx))) {
      return new IterPred(root, pred, pos, last);
    }

    // faster runtime evaluation of variable counters (array[$pos] ...)
    counter = pred.length == 1 && p.returned(ctx).num() &&
      !p.uses(Use.CTX, ctx);
    return this;
  }

  @Override
  public Iter iter(final QueryContext ctx) throws QueryException {
    if(counter) {
      final Item it = pred[0].ebv(ctx);
      final long l = it.itr();
      final Expr e = Pos.get(l, l);
      return l != it.dbl() || e == Bln.FALSE ? Iter.EMPTY :
        new IterPred(root, pred, (Pos) e, false).iter(ctx);
    }

    final Iter iter = ctx.iter(root);
    final Item ci = ctx.item;
    final long cs = ctx.size;
    final long cp = ctx.pos;

    // cache results to support last() function
    final SeqIter si = new SeqIter();
    Item i;
    while((i = iter.next()) != null) si.add(i);

    // evaluate predicates
    for(final Expr p : pred) {
      ctx.size = si.size();
      ctx.pos = 1;
      int c = 0;
      final int sl = si.size();
      for(int s = 0; s < sl; s++) {
        ctx.item = si.item[s];
        if(p.test(ctx) != null) si.item[c++] = si.item[s];
        ctx.pos++;
      }
      si.size(c);
    }

    ctx.item = ci;
    ctx.size = cs;
    ctx.pos = cp;
    return si;
  }

  @Override
  public final boolean uses(final Use u, final QueryContext ctx) {
    return root.uses(u, ctx) || super.uses(u, ctx);
  }

  @Override
  public final boolean removable(final Var v, final QueryContext ctx) {
    return root.removable(v, ctx) && super.removable(v, ctx);
  }

  @Override
  public final Expr remove(final Var v) {
    root = root.remove(v);
    return super.remove(v);
  }

  @Override
  public final SeqType returned(final QueryContext ctx) {
    final SeqType ret = root.returned(ctx);
    return ret.single() ? super.returned(ctx) : ret;
  }

  @Override
  public final void plan(final Serializer ser) throws IOException {
    ser.openElement(this);
    root.plan(ser);
    super.plan(ser);
    ser.closeElement();
  }

  @Override
  public final String toString() {
    final StringBuilder sb = new StringBuilder("(" + root.toString());
    sb.append(")" + super.toString());
    return sb.toString();
  }
}

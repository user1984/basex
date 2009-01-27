package org.basex.query.func;

import static org.basex.query.QueryText.*;
import static org.basex.query.item.Type.*;
import org.basex.BaseX;
import org.basex.query.QueryContext;
import org.basex.query.QueryException;
import org.basex.query.expr.Calc;
import org.basex.query.expr.CmpV;
import org.basex.query.expr.Expr;
import org.basex.query.item.Date;
import org.basex.query.item.Dbl;
import org.basex.query.item.Item;
import org.basex.query.item.Itr;
import org.basex.query.item.Seq;
import org.basex.query.item.Type;
import org.basex.query.iter.Iter;
import org.basex.query.path.AxisPath;
import org.basex.query.util.Err;

/**
 * Aggregating functions.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-08, ISC License
 * @author Christian Gruen
 */
public final class FNAggr extends Fun {
  @Override
  public Iter iter(final QueryContext ctx, final Iter[] arg)
      throws QueryException {
    final Iter iter = arg[0];

    switch(func) {
      case COUNT:
        long c = iter.size();
        if(c == -1) do ++c; while(iter.next() != null);
        return Itr.get(c).iter();
      case MIN:
        return minmax(arg, CmpV.Comp.GT, ctx);
      case MAX:
        return minmax(arg, CmpV.Comp.LT, ctx);
      case SUM:
        final Iter zero = arg.length == 2 ? arg[1] : null;
        Item it = iter.next();
        return it == null ? zero != null ? zero : Itr.ZERO.iter() :
          sum(iter, it, false);
      case AVG:
        it = iter.next();
        return it == null ? Iter.EMPTY : sum(iter, it, true);
      default:
        BaseX.notexpected(func); return null;
    }
  }

  @Override
  public Expr c(final QueryContext ctx) {
    switch(func) {
      case AVG:
        return args[0].e() ? Seq.EMPTY : this;
      case COUNT:
        final int c = count(args[0], ctx);
        return c >= 0 ? Itr.get(c) : this;
      default:
        return this;
    }
  }
  
  /**
   * Counts the number of expected results for the specified expression.
   * @param arg argument
   * @param ctx query context
   * @return number of results
   */
  public static int count(final Expr arg, final QueryContext ctx) {
    if(arg.e()) return 0;
    if(arg.i()) return 1;
    if(arg instanceof Seq) return ((Seq) arg).size();
    return arg instanceof AxisPath ? ((AxisPath) arg).count(ctx) : -1;
  }

  /**
   * Sums up the specified item(s).
   * @param iter iterator
   * @param it first item
   * @param avg calculating the average
   * @return summed up item.
   * @throws QueryException thrown if the items can't be compared
   */
  private Iter sum(final Iter iter, final Item it, final boolean avg)
      throws QueryException {
    Item res = it.u() ? Dbl.get(it.str()) : it;
    if(!res.n() && !res.d()) Err.or(FUNNUMDUR, this, res.type);
    final boolean n = res.n();

    int c = 1;
    Item i;
    while((i = iter.next()) != null) {
      final boolean un = i.u() || i.n();
      if(n && !un) Err.or(FUNNUM, this, i.type);
      if(!n && un) Err.or(FUNDUR, this, i.type);
      res = Calc.PLUS.ev(res, i);
      c++;
    }
    return avg ? Calc.DIV.ev(res, Itr.get(c)).iter() : res.iter();
  }

  /**
   * Returns a minimum or maximum item.
   * @param arg arguments
   * @param cmp comparator
   * @param ctx xquery context
   * @return resulting item
   * @throws QueryException thrown if the items can't be compared
   */
  private Iter minmax(final Iter[] arg, final CmpV.Comp cmp,
      final QueryContext ctx) throws QueryException {

    final Iter iter = arg[0];
    if(arg.length == 2) checkColl(arg[1]);

    Item res = iter.next();
    if(res == null) return Iter.EMPTY;

    cmp.e(res, res);
    if(!res.u() && res.s() || res instanceof Date)
      return evalStr(iter, res, cmp);

    Type t = res.u() ? DBL : res.type;
    if(res.type != t) res = t.e(res, ctx);

    Item it;
    while((it = iter.next()) != null) {
      t = type(res, it);
      final double d = it.dbl();
      if(d != d || cmp.e(res, it)) res = it;
      if(res.type != t) res = t.e(res, ctx);
    }
    return res.iter();
  }

  /**
   * Returns the type with the highest precedence.
   * @param a input item
   * @param b result item
   * @return result
   * @throws QueryException thrown if the items can't be compared
   */
  private Type type(final Item a, final Item b) throws QueryException {
    if(b.u()) {
      if(!a.n()) Err.or(FUNCMP, this, a.type, b.type);
      return DBL;
    }
    if(a.n() && !b.u() && b.s()) Err.or(FUNCMP, this, a.type, b.type);
    if(a.type == b.type) return a.type;
    if(a.type == DBL || b.type == DBL) return DBL;
    if(a.type == FLT || b.type == FLT) return FLT;
    if(a.type == DEC || b.type == DEC) return DEC;
    if(a.type == BLN || a.n() && !b.n() || b.n() && !a.n())
      Err.or(FUNCMP, this, a.type, b.type);
    return a.n() || b.n() ? ITR : a.type;
  }

  /**
   * Compares strings.
   * @param iter input iterator
   * @param r first item
   * @param cmp comparator
   * @return resulting item
   * @throws QueryException thrown if the items can't be compared
   */
  private Iter evalStr(final Iter iter, final Item r, final CmpV.Comp cmp)
      throws QueryException {

    Item res = r;
    Item it;
    while((it = iter.next()) != null) {
      if(it.type != res.type) Err.or(FUNCMP, info(), res.type, it.type);
      if(cmp.e(res, it)) res = it;
    }
    return res.iter();
  }
}
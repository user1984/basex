package org.basex.query;

import org.basex.data.Data;
import org.basex.query.expr.Context;
import org.basex.query.expr.Expr;
import org.basex.query.expr.ParseExpr;
import org.basex.query.path.Axis;
import org.basex.query.path.AxisPath;
import org.basex.query.path.AxisStep;
import org.basex.query.path.Path;
import org.basex.util.Array;

/**
 * Container for all information needed to determine whether an index is
 * accessible or not.
 *
 * @author BaseX Team 2005-11, BSD License
 * @author Sebastian Gath
 */
public final class IndexContext {
  /** Query context. */
  public final QueryContext ctx;
  /** Data reference. */
  public final Data data;
  /** Index Step. */
  public final AxisStep step;
  /** Flag for potential duplicates. */
  public final boolean dupl;

  /** Costs of index access: smaller is better, 0 means no results. */
  public int costs = Integer.MAX_VALUE;
  /** Flag for ftnot expressions. */
  public boolean not;
  /** Flag for sequential processing. */
  public boolean seq;

  /**
   * Constructor.
   * @param c query context
   * @param d data reference
   * @param s index step
   * @param l duplicate flag
   */
  public IndexContext(final QueryContext c, final Data d, final AxisStep s,
      final boolean l) {
    ctx = c;
    data = d;
    step = s;
    dupl = l;
  }

  /**
   * Rewrites the specified expression for index access.
   * @param ex expression to be rewritten
   * @param root new root expression
   * @param text text flag
   * @return index access
   */
  public Expr invert(final Expr ex, final ParseExpr root, final boolean text) {
    // handle context node
    if(ex instanceof Context) {
      if(text) return root;
      // add attribute step
      if(step.test.name == null) return root;
      return Path.get(root.input, root,
          AxisStep.get(step.input, Axis.SELF, step.test));
    }

    final AxisPath orig = (AxisPath) ex;
    final AxisPath path = orig.invertPath(root, step);
    if(!text) {
      // add attribute step
      final AxisStep s = orig.step(orig.step.length - 1);
      if(s.test.name != null) {
        Expr[] steps = { AxisStep.get(s.input, Axis.SELF, s.test) };
        for(final Expr e : path.step) steps = Array.add(steps, e);
        path.step = steps;
      }
    }
    return path;
  }
}

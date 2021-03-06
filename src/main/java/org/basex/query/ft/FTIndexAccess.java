package org.basex.query.ft;

import static org.basex.query.QueryTokens.*;
import static org.basex.util.Token.*;
import java.io.IOException;
import org.basex.data.Serializer;
import org.basex.query.IndexContext;
import org.basex.query.QueryContext;
import org.basex.query.QueryException;
import org.basex.query.expr.Simple;
import org.basex.query.item.FTNode;
import org.basex.query.item.ANode;
import org.basex.query.item.NodeType;
import org.basex.query.iter.FTIter;
import org.basex.query.iter.NodeIter;
import org.basex.util.InputInfo;
import org.basex.util.TokenBuilder;

/**
 * FTContains expression with index access.
 *
 * @author BaseX Team 2005-11, BSD License
 * @author Christian Gruen
 */
public final class FTIndexAccess extends Simple {
  /** Full-text expression. */
  private final FTExpr ftexpr;
  /** Index context. */
  private final IndexContext ictx;

  /**
   * Constructor.
   * @param ii input info
   * @param ex contains, select and optional ignore expression
   * @param ic index context
   */
  public FTIndexAccess(final InputInfo ii, final FTExpr ex,
      final IndexContext ic) {

    super(ii);
    ftexpr = ex;
    ictx = ic;
  }

  @Override
  public NodeIter iter(final QueryContext ctx) throws QueryException {
    final FTIter ir = ftexpr.iter(ctx);

    return new NodeIter() {
      @Override
      public ANode next() throws QueryException {
        final FTNode it = ir.next();
        if(it != null) {
          // add entry to visualization
          if(ctx.ftpos != null) ctx.ftpos.add(it.data, it.pre, it.all);
          // assign scoring, if not done yet
          it.score();
          // remove matches reference to save memory
          it.all = null;
        }
        return it;
      }
    };
  }

  @Override
  public void plan(final Serializer ser) throws IOException {
    ser.openElement(this, DATA, token(ictx.data.meta.name));
    ftexpr.plan(ser);
    ser.closeElement();
  }

  @Override
  public boolean duplicates() {
    return ictx.dupl;
  }

  @Override
  public String toString() {
    return new TokenBuilder(NodeType.DOC.nam).add(" { \"").
        add(ictx.data.meta.name).add("\" }/fulltext").add(PAR1).
        add(ftexpr.toString()).add(PAR2).toString();
  }
}

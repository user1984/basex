package org.basex.core.proc;

import org.basex.core.Context;
import org.basex.core.Commands.Cmd;

/**
 * Evaluates the 'xquery' command and processes an XQuery request.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
 * @author Christian Gruen
 */
public final class XQuery extends AQuery {
  /**
   * Default constructor.
   * @param query query to process
   */
  public XQuery(final String query) {
    super(STANDARD, query);
  }

  @Override
  protected boolean run() {
    return query(args[0]);
  }

  @Override
  public boolean updating(final Context ctx) {
    return updating(ctx, args[0]);
  }

  @Override
  public String toString() {
    return Cmd.XQUERY + " " + args[0];
  }
}
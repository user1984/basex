package org.basex.test.query.advanced;

import org.basex.query.QueryException;
import org.basex.query.func.FunDef;
import org.basex.query.util.Err;
import org.junit.Test;

/**
 * This class tests the functions of the <code>FNGen</code> class.
 *
 * @author BaseX Team 2005-11, BSD License
 * @author Christian Gruen
 */
public final class FNGenTest extends AdvancedQueryTest {
  /** Text file. */
  private static final String TEXT = "etc/xml/stopWords";

  /**
   * Test method for the fn:unparsed-text() function.
   * @throws QueryException database exception
   */
  @Test
  public void testUnparsedText() throws QueryException {
    final String fun = check(FunDef.PARSETXT);
    contains(fun + "('" + TEXT + "')", "aboutabove");
    contains(fun + "('" + TEXT + "', 'US-ASCII')", "aboutabove");
    error(fun + "('" + TEXT + "', 'xyz')", Err.WRONGINPUT);
  }

  /**
   * Test method for the fn:parse-xml() function.
   * @throws QueryException database exception
   */
  @Test
  public void testParseXML() throws QueryException {
    final String fun = check(FunDef.PARSEXML);
    contains(fun + "('<x>a</x>')//text()", "a");
  }

  /**
   * Test method for the fn:serialize() function.
   * @throws QueryException database exception
   */
  @Test
  public void testSerialize() throws QueryException {
    final String fun = check(FunDef.SERIALIZE);
    contains(fun + "(<x/>)", "&lt;x/&gt;");
    contains(fun + "(<x/>, " + serialParams("") + ")", "&lt;x/&gt;");
    contains(fun + "(<x>a</x>, " +
        serialParams("<method>text</method>") + ")", "a");
  }
}

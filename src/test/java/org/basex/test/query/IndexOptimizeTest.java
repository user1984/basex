package org.basex.test.query;

import static org.junit.Assert.*;
import java.io.IOException;
import org.basex.core.Context;
import org.basex.core.Prop;
import org.basex.core.cmd.Add;
import org.basex.core.cmd.Close;
import org.basex.core.cmd.CreateDB;
import org.basex.core.cmd.DropDB;
import org.basex.core.cmd.Open;
import org.basex.core.cmd.Optimize;
import org.basex.core.cmd.Set;
import org.basex.data.XMLSerializer;
import org.basex.io.ArrayOutput;
import org.basex.query.QueryException;
import org.basex.query.QueryProcessor;
import org.basex.util.Util;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This class tests if queries are rewritten for index access.
 *
 * @author BaseX Team 2005-11, BSD License
 * @author Christian Gruen
 */
public final class IndexOptimizeTest {
  /** Database context. */
  private static final Context CONTEXT = new Context();
  /** Test database name. */
  private static final String NAME = Util.name(IndexOptimizeTest.class);

  /**
   * Creates a test database.
   * @throws Exception exception
   */
  @BeforeClass
  public static void start() throws Exception {
    new Set(Prop.FTINDEX, true).execute(CONTEXT);
    new CreateDB(NAME, "<xml><a>1</a><a>1 2</a></xml>").execute(CONTEXT);
    new Close().execute(CONTEXT);
  }

  /**
   * Drops the test database.
   * @throws Exception exception
   */
  @AfterClass
  public static void stop() throws Exception {
    new DropDB(NAME).execute(CONTEXT);
  }

  /**
   * Checks the open command.
   * Test method.
   * @throws Exception unexpected exception
   */
  @Test
  public void openDocTest() throws Exception {
    createDoc();
    new Open(NAME).execute(CONTEXT);
    check("//*[text() = '1']");
    check("//*[text() contains text '1']");
  }

  /**
   * Checks the open command.
   * Test method.
   * @throws Exception unexpected exception
   */
  @Test
  public void openCollTest() throws Exception {
    createColl();
    new Open(NAME).execute(CONTEXT);
    check("//*[text() = '1']");
    check("//*[text() contains text '1']");
  }

  /**
   * Checks the XQuery doc() function.
   * @throws Exception unexpected exception
   */
  @Test
  public void docTest() throws Exception {
    createDoc();
    final String doc = "doc('" + NAME + "')";
    check(doc + "//*[text() = '1']");
    check(doc + "//*[text() contains text '2']");
  }

  /**
   * Checks the XQuery collection() function.
   * @throws Exception unexpected exception
   */
  @Test
  public void collTest() throws Exception {
    createColl();
    final String doc = "collection('" + NAME + "')";
    check(doc + "//*[text() = '1']");
    check(doc + "//*[text() contains text '2']");
  }

  /**
   * Checks the XQuery db:open() function.
   * @throws Exception unexpected exception
   */
  @Test
  public void dbOpenTest() throws Exception {
    createColl();
    final String doc = "db:open('" + NAME + "')";
    check(doc + "//*[text() = '1']");
    check(doc + "//*[text() <- '2']");
  }

  /**
   * Checks the XQuery db:open() function, using a specific path.
   * @throws Exception unexpected exception
   */
  @Test
  public void dbOpenExtTest() throws Exception {
    createColl();
    final String doc = "db:open('" + NAME + "/two')";
    check(doc + "//*[text() = '1']", "");
    check(doc + "//*[text() = '4']", "<a>4</a>");
  }

  /**
   * Checks full-text requests.
   * @throws Exception unexpected exception
   */
  @Test
  public void ftTest() throws Exception {
    createDoc();
    new Open(NAME).execute(CONTEXT);
    check("//*[text() <- '1']", "<a>1</a>");
    check("//*[text() <- '1 2' any word]", "<a>1</a><a>2 3</a>");
    check("//*[text() <- {'2','4'} all]", "");
    check("//*[text() <- {'2','3'} all words]", "<a>2 3</a>");
    check("//*[text() <- {'2','4'} all words]", "");
  }

  /**
   * Creates a test database.
   * @throws Exception exception
   */
  private void createDoc() throws Exception {
    new CreateDB(NAME, "<xml><a>1</a><a>2 3</a></xml>").execute(CONTEXT);
    new Close().execute(CONTEXT);
  }

  /**
   * Creates a test collection.
   * @throws Exception exception
   */
  private void createColl() throws Exception {
    new CreateDB(NAME).execute(CONTEXT);
    new Add("<xml><a>1</a><a>2 3</a></xml>", "one").execute(CONTEXT);
    new Add("<xml><a>4</a><a>5 6</a></xml>", "two").execute(CONTEXT);
    new Optimize().execute(CONTEXT);
    new Close().execute(CONTEXT);
  }

  /**
   * Check if specified query was rewritten for index access.
   * @param query query to be tested
   */
  private void check(final String query) {
    check(query, null);
  }

  /**
   * Checks if specified query was rewritten for index access, and checks the
   * query result.
   * @param query query to be tested
   * @param result expected query result
   */
  private void check(final String query, final String result) {
    // compile query
    ArrayOutput plan = null;
    QueryProcessor qp = new QueryProcessor(query, CONTEXT);
    try {
      ArrayOutput ao = new ArrayOutput();
      XMLSerializer xml = qp.getSerializer(ao);
      qp.execute().serialize(xml);
      qp.close();
      if(result != null)
        assertEquals(result, ao.toString().replaceAll("\\r?\\n", ""));

      // fetch query plan
      plan = new ArrayOutput();
      qp.plan(new XMLSerializer(plan));

      qp = new QueryProcessor(plan + "//(IndexAccess|FTIndexAccess)", CONTEXT);
      ao = new ArrayOutput();
      xml = qp.getSerializer(ao);
      qp.execute().serialize(xml);

      // check if IndexAccess is used
      assertTrue("No index used:\nQuery: " + query + "\nPlan: " + plan,
          !ao.toString().isEmpty());
    } catch(final QueryException ex) {
      fail(ex.getMessage() + "\nQuery: " + query + "\nPlan: " + plan);
    } catch(final IOException ex) {
      fail(ex.getMessage());
    }
  }
}

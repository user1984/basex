package org.basex.test.build;

import static org.junit.Assert.*;
import org.basex.core.BaseXException;
import org.basex.core.Context;
import org.basex.core.cmd.Add;
import org.basex.core.cmd.CreateDB;
import org.basex.core.cmd.DropDB;
import org.basex.query.QueryProcessor;
import org.basex.query.item.Item;
import org.basex.util.Util;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests queries on collections.
 *
 * @author BaseX Team 2005-11, BSD License
 * @author Michael Seiferle
 */
public final class CollectionPathTest {
  /** Database context. */
  private static final Context CONTEXT = new Context();

  /** Test database name. */
  private static final String DBNAME = Util.name(CollectionPathTest.class);
  /** Test files. */
  private static final String[] FILES = {
    "etc/xml/input.xml", "etc/xml/xmark.xml", "etc/xml/test.xml"
  };
  /** Test ZIP. */
  private static final String ZIP = "etc/xml/xml.zip";

  /**
   * Creates an initial database.
   * @throws BaseXException exception
   */
  @BeforeClass
  public static void before() throws BaseXException {
    new CreateDB(DBNAME).execute(CONTEXT);
    for(final String file : FILES) {
      new Add(file, null, "etc/xml").execute(CONTEXT);
    }
    new Add(ZIP, null, "test/zipped").execute(CONTEXT);
  }

  /**
   * Drops the initial collection.
   * @throws BaseXException exception
   */
  @AfterClass
  public static void after() throws BaseXException {
    new DropDB(DBNAME).execute(CONTEXT);
  }

  /**
   * Finds single doc.
   * @throws Exception exception
   */
  @Test
  public void testFindDoc() throws Exception {
    final String find =
      "for $x in collection('" + DBNAME + "/etc/xml/xmark.xml') " +
      "where $x//location contains text 'uzbekistan' " +
      "return $x";
    final QueryProcessor qp = new QueryProcessor(find, CONTEXT);
    assertEquals(1, qp.execute().size());
    qp.close();
  }

  /**
   * Finds documents in path.
   * @throws Exception exception
   */
  @Test
  public void testFindDocs() throws Exception {
    final String find = "collection('" + DBNAME + "/test/zipped') ";
    final QueryProcessor qp = new QueryProcessor(find, CONTEXT);
    assertEquals(4, qp.execute().size());
    qp.close();
  }

  /**
   * Checks if the constructed base-uri matches the base-uri of added documents.
   * @throws Exception exception
   */
  @Test
  public void testBaseUri() throws Exception {
    final String find =
      "for $x in collection('" + DBNAME + "/etc/xml/xmark.xml') " +
      "return base-uri($x)";
    final QueryProcessor qp = new QueryProcessor(find, CONTEXT);
    final Item it = qp.iter().next();
    final String expath = '"' + CONTEXT.data.meta.path.url().replace(DBNAME, "")
        + FILES[1] + '"';
    assertEquals(expath, it.toString());
    qp.close();
  }
}

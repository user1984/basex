package org.basex.build.fs;

import java.io.File;

import org.basex.core.Context;
import org.basex.core.Main;
import org.basex.core.Progress;
import org.basex.core.Text;
import org.basex.core.proc.CreateDB;
import org.deepfs.util.FSImporter;
import org.deepfs.util.FSWalker;

/**
 * Creates a database that maps a file system hierarchy.
 * @author Bastian Lemke
 */
public class FSTraversalParser extends Progress {

  /** The FSImporter. */
  private final FSImporter importer;
  /** The database context. */
  private final Context context;
  /** Root file(s). */
  private final File[] roots;
  
  /**
   * Constructor.
   * @param path import root.
   * @param ctx the database context to use.
   * @param dbname the name of the database to create.
   */
  public FSTraversalParser(final String path, final Context ctx,
      final String dbname) {
    context = ctx;
    final CreateDB c = new CreateDB("<" + FSImporter.DOC_NODE + "/>", dbname);
    if(!c.execute(ctx)) Main.notexpected(
        "Failed to create file system database (%).", c.info());
    importer = new FSImporter(context);
    context.data.meta.deepfs = true;
    roots = path.equals("/") ? File.listRoots() : new File[] { new File(path)};
  }

  /** Recursively parses the given path. */
  public void parse() {
    final FSWalker walker = new FSWalker(importer);
    for (final File file : roots)
      walker.traverse(file);
  }

  @Override
  public String tit() {
    return Text.CREATEFSPROG;
  }

  @Override
  public String det() {
    return importer == null ? "" : importer.getCurrentFileName();
  }

  @Override
  public double prog() {
    return 0;
  }

}

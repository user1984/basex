package org.basex.core.cmd;

import static org.basex.core.Text.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.basex.core.Command;
import org.basex.core.Context;
import org.basex.core.Prop;
import org.basex.core.User;
import org.basex.io.IO;
import org.basex.util.StringList;
import org.basex.util.Util;

/**
 * Evaluates the 'restore' command and restores a backup of a database.
 *
 * @author BaseX Team 2005-11, BSD License
 * @author Christian Gruen
 */
public final class Restore extends Command {
  /** Date pattern. */
  private static final String PATTERN =
    "-\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}";

  /** Counter for outstanding files. */
  private int of;
  /** Counter of total files. */
  private int tf;

  /**
   * Default constructor.
   * @param arg optional argument
   */
  public Restore(final String arg) {
    super(User.CREATE, arg);
  }

  @Override
  protected boolean run() {
    String db = args[0];
    if(!validName(db)) return error(NAMEINVALID, db);

    // find backup file with or without date suffix
    File file = new File(prop.get(Prop.DBPATH) + '/' + db + IO.ZIPSUFFIX);
    if(!file.exists()) {
      final StringList list = list(db, context);
      if(list.size() != 0) file = new File(list.get(0));
    } else {
      db = db.replace(PATTERN + '$', "");
    }
    if(!file.exists()) return error(DBBACKNF, db);

    // close database if it's currently opened and not opened by others
    final boolean closed = close(db);

    // check if database is pinned
    if(context.pinned(db)) return error(DBLOCKED, db);

    // try to restore database
    return restore(file, prop) && (!closed || new Open(db).run(context)) ?
        info(DBRESTORE, file.getName(), perf) : error(DBNORESTORE, db);
  }

  /**
   * Restores the specified database.
   * @param file file
   * @param pr database properties
   * @return success flag
   */
  private boolean restore(final File file, final Prop pr) {
    ZipInputStream zis = null;
    try {
      // count number of files
      zis = new ZipInputStream(new BufferedInputStream(
          new FileInputStream(file)));
      while(zis.getNextEntry() != null) tf++;
      zis.close();
      // reopen zip stream
      zis = new ZipInputStream(new BufferedInputStream(
          new FileInputStream(file)));

      final byte[] data = new byte[IO.BLOCKSIZE];
      ZipEntry e;
      while((e = zis.getNextEntry()) != null) {
        of++;
        final String path = pr.get(Prop.DBPATH) + '/' + e.getName();
        if(e.isDirectory()) {
          new File(path).mkdir();
        } else {
          BufferedOutputStream bos = null;
          try {
            bos = new BufferedOutputStream(new FileOutputStream(path));
            int c;
            while((c = zis.read(data)) != -1) bos.write(data, 0, c);
          } finally {
            if(bos != null) try { bos.close(); } catch(final IOException ee) { }
          }
        }
      }
      zis.close();
      return true;
    } catch(final IOException ex) {
      Util.debug(ex);
      return false;
    } finally {
      if(zis != null) try { zis.close(); } catch(final IOException e) { }
    }
  }

  /**
   * Returns all backups of the specified database.
   * @param db database
   * @param ctx database context
   * @return available backups
   */
  public static StringList list(final String db, final Context ctx) {
    final StringList list = new StringList();

    final IO dir = IO.get(ctx.prop.get(Prop.DBPATH));
    if(!dir.exists()) return list;

    for(final IO f : dir.children()) {
      if(f.name().matches(db + PATTERN + IO.ZIPSUFFIX)) list.add(f.path());
    }
    list.sort(false, false);
    return list;
  }

  @Override
  protected String tit() {
    return BUTTONRESTORE;
  }

  @Override
  public boolean supportsProg() {
    return true;
  }

  @Override
  protected double prog() {
    return (double) of / tf;
  }
}
package org.basex.query.item;

import static org.basex.query.QueryTokens.*;
import static org.basex.util.Token.*;
import java.io.IOException;
import org.basex.data.Data;
import org.basex.data.Serializer;
import org.basex.io.IO;
import org.basex.query.ft.Scoring;
import org.basex.query.iter.NodeIter;
import org.basex.query.iter.NodeMore;
import org.basex.query.util.Err;
import org.basex.query.util.NSGlobal;
import org.basex.util.Atts;

/**
 * Disk-based Node item.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
 * @author Christian Gruen
 */
public class DBNode extends Nod {
  /** Node Types. */
  public static final Type[] TYPES = {
    Type.DOC, Type.ELM, Type.TXT, Type.ATT, Type.COM, Type.PI
  };
  /** Data reference. */
  public final Data data;
  /** Root node (constructor). */
  public Nod root;
  /** Namespaces. */
  private Atts nsp;
  /** Pre value. */
  public int pre;

  /**
   * Constructor.
   */
  public DBNode() {
    super(Type.TXT);
    data = null;
  }

  /**
   * Constructor.
   * @param d data reference
   * @param p pre value
   */
  public DBNode(final Data d, final int p) {
    this(d, p, d.kind(p));
  }

  /**
   * Constructor.
   * @param d data reference
   * @param p pre value
   * @param k node kind
   */
  public DBNode(final Data d, final int p, final int k) {
    this(d, p, null, TYPES[k]);
  }

  /**
   * Constructor.
   * @param d data reference
   * @param p pre value
   * @param r parent reference
   * @param t node type
   */
  public DBNode(final Data d, final int p, final Nod r, final Type t) {
    super(t);
    data = d;
    pre = p;
    par = r;
  }

  /**
   * Sets the node type.
   * @param p pre value
   * @param k node kind
   */
  public final void set(final int p, final int k) {
    type = TYPES[k];
    par = null;
    pre = p;
  }

  @Override
  public final byte[] str() {
    return data.atom(pre);
  }

  @Override
  public final void serialize(final Serializer ser) throws IOException {
    ser.node(data, pre);
  }

  @Override
  public final byte[] nname() {
    switch(type) {
      case ELM: case ATT: case PI:
        return data.name(pre, kind(type));
      default:
        return EMPTY;
    }
  }

  @Override
  public final QNm qname() {
    return qname(new QNm());
  }

  @Override
  public final QNm qname(final QNm name) {
    final byte[] nm = nname();
    name.name(nm);
    name.uri = Uri.EMPTY;
    final boolean ns = name.ns();
    if(ns || data.ns.size() != 0) {
      final int n = ns ? data.ns.uri(nm, pre) : data.uri(pre, data.kind(pre));
      final byte[] uri = n > 0 ? data.ns.uri(n) : ns ?
          NSGlobal.uri(pref(nm)) : EMPTY;
      name.uri = uri.length == 0 ? Uri.EMPTY : new Uri(uri);
    }
    return name;
  }

  @Override
  public final Atts ns() {
    if(type == Type.ELM && nsp == null) nsp = data.ns(pre);
    return nsp;
  }

  @Override
  public final byte[] base() {
    if(type != Type.DOC) return EMPTY;
    final IO dir = IO.get(data.meta.file.path());
    return token(dir.merge(string(data.text(pre, true))).url());
  }

  @Override
  public final boolean is(final Nod nod) {
    if(nod == this) return true;
    if(!(nod instanceof DBNode)) return false;
    return data == ((DBNode) nod).data && pre == ((DBNode) nod).pre;
  }

  @Override
  public final int diff(final Nod nod) {
    if(!(nod instanceof DBNode) || data != ((DBNode) nod).data) {
      return id - nod.id;
    }
    return pre - ((DBNode) nod).pre;
  }

  @Override
  public final DBNode copy() {
    // par.finish() ?..
    final DBNode node = new DBNode(data, pre, par, type);
    node.root = root;
    node.score = score;
    return node;
  }

  @Override
  public final DBNode finish() {
    return copy();
  }

  @Override
  public final Nod parent() {
    if(par != null) return par;
    final int p = data.parent(pre, data.kind(pre));
    if(p == -1) return null;

    final DBNode node = copy();
    node.set(p, data.kind(p));
    node.score(Scoring.step(node.score));
    return node;
  }

  @Override
  public final void parent(final Nod p) {
    root = p;
    par = p;
  }

  @Override
  public final NodeIter anc() {
    return new NodeIter() {
      /** Temporary node. */
      private Nod node = copy();

      @Override
      public Nod next() {
        node = node.parent();
        return node;
      }
    };
  }

  @Override
  public final NodeIter attr() {
    return new NodeIter() {
      final DBNode node = copy();
      final int s = pre + data.attSize(pre, data.kind(pre));
      int p = pre + 1;

      @Override
      public Nod next() {
        if(p == s) return null;
        node.set(p++, Data.ATTR);
        return node;
      }
    };
  }

  @Override
  public final NodeMore child() {
    return new NodeMore() {
      int k = data.kind(pre);
      int p = pre + data.attSize(pre, k);
      final int s = pre + data.size(pre, k);
      final DBNode node = copy();
      final double sc = node.score;

      @Override
      public boolean more() {
        return p != s;
      }

      @Override
      public Nod next() {
        if(!more()) return null;
        k = data.kind(p);
        node.set(p, k);
        node.score(Scoring.step(sc));
        p += data.size(p, k);
        return node;
      }
    };
  }

  @Override
  public final NodeIter desc() {
    return new NodeIter() {
      int k = data.kind(pre);
      int p = pre + data.attSize(pre, k);
      final int s = pre + data.size(pre, k);
      final DBNode node = copy();
      final double sc = node.score;

      @Override
      public DBNode next() {
        if(p == s) return null;
        k = data.kind(p);
        node.set(p, k);
        p += data.attSize(p, k);
        node.score(Scoring.step(sc));
        return node;
      }
    };
  }

  @Override
  public final NodeIter descOrSelf() {
    return new NodeIter() {
      final DBNode node = copy();
      final int s = pre + data.size(pre, data.kind(pre));
      int p = pre;

      @Override
      public Nod next() {
        if(p == s) return null;
        final int k = data.kind(p);
        node.set(p, k);
        p += data.attSize(p, k);
        return node;
      }
    };
  }

  @Override
  public final NodeIter par() {
    return new NodeIter() {
      /** First call. */
      private boolean more;

      @Override
      public Nod next() {
        if(more) return null;
        more = true;
        return parent();
      }
    };
  }

  @Override
  public final String color() {
    return "9999FF";
  }

  @Override
  public final void plan(final Serializer ser) throws IOException {
    ser.emptyElement(this, NAM, token(data.meta.file.name()), PRE, token(pre));
  }

  @Override
  public String toString() {
    switch(type) {
      case ATT:
      case PI:
        return type + " " + string(nname()) + " { \"" + string(str()) + "\" }";
      case ELM:
        return type + " " + string(nname()) + " { ... }";
      case DOC:
        return type + " { \"" + string(data.text(pre, true)) + "\" }";
      default:
        return type + " { \"" + Err.chop(str()) + "\" }";
    }
  }
}
package org.basex.query.item;

import static org.basex.query.QueryTokens.*;
import java.io.IOException;
import org.basex.data.Serializer;
import org.basex.query.iter.NodeCache;
import org.basex.util.TokenMap;
import org.basex.util.Util;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Document node fragment.
 *
 * @author BaseX Team 2005-11, BSD License
 * @author Christian Gruen
 */
public final class FDoc extends FNode {
  /** Base URI. */
  private final byte[] base;

  /**
   * Constructor.
   * @param ch children
   * @param b base uri
   */
  public FDoc(final NodeCache ch, final byte[] b) {
    super(NodeType.DOC);
    children = ch;
    base = b;
  }

  /**
   * Constructor for DOM nodes (partial).
   * Provided by Erdal Karaca.
   * @param doc DOM node
   * @param b base uri
   */
  FDoc(final DocumentFragment doc, final byte[] b) {
    this(new NodeCache(), b);
    final Node elem = doc.getFirstChild();
    if(elem != null && elem instanceof Element)
      children.add(new FElem((Element) elem, this, new TokenMap()));
    // [LW] DOM: DocumentFragment != Document, possibly multiple roots
  }

  @Override
  public void serialize(final Serializer ser) throws IOException {
    for(int c = 0; c < children.size(); ++c) children.get(c).serialize(ser);
  }

  @Override
  public byte[] base() {
    return base;
  }

  @Override
  public FDoc copy() {
    return new FDoc(children, base);
  }

  @Override
  public void plan(final Serializer ser) throws IOException {
    ser.emptyElement(this, BASE, base);
  }

  @Override
  public String toString() {
    return Util.info("%(%)", name(), base);
  }
}

package org.basex.query.item;

import static org.basex.query.QueryTokens.*;

import org.basex.query.QueryContext;
import org.basex.query.QueryException;
import org.basex.query.item.map.Map;
import org.basex.query.util.Err;
import org.basex.util.InputInfo;

/**
 * Type for maps.
 *
 * @author BaseX Team 2005-11, BSD License
 * @author Leo Woerteler
 */
public final class MapType extends FunType {
  /** Key type of the map. */
  public final AtomType keyType;
  /** The general map type. */
  public static final MapType ANY_MAP = new MapType(AtomType.AAT,
      SeqType.ITEM_ZM);

  /**
   * Constructor.
   * @param arg argument type
   * @param rt return type
   */
  private MapType(final AtomType arg, final SeqType rt) {
    super(new SeqType[]{ arg.seq() }, rt);
    keyType = arg;
  }

  @Override
  public byte[] nam() {
    return MAP;
  }

  @Override
  public boolean map() {
    return true;
  }

  @Override
  public FItem e(final Item it, final QueryContext ctx, final InputInfo ii)
      throws QueryException {
    if(!it.map() || !((Map) it).hasType(this)) throw Err.cast(ii, this, it);

    return (Map) it;
  }

  @Override
  public boolean instance(final Type t) {
//    if(t instanceof MapType) {
//      final MapType mt = (MapType) t;
//      return mt.keyType.instance(keyType) && ret.instance(mt.ret);
//    }
    return super.instance(t);
  }

  /**
   * Creates a new map type.
   * @param key key type
   * @param val value type
   * @return map type
   */
  public static MapType get(final AtomType key, final SeqType val) {
    if(key == AtomType.AAT && val.eq(SeqType.ITEM_ZM)) return ANY_MAP;
    return new MapType(key, val);
  }

  @Override
  public String toString() {
    return keyType == AtomType.AAT && ret.eq(SeqType.ITEM_ZM) ? "map(*)"
        : "map(" + keyType + ", " + ret + ")";
  }
}

package codec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.SourceModel;
import models.Symbol;
import models.SymbolModel;

import io.BitSink;


public class HuffmanEncoder implements SymbolEncoder {

	private Map<Symbol, String> _code_map;
	private boolean _closed;
	
	public HuffmanEncoder(Map<Symbol, String> code_map) {
		_code_map = code_map;
		_closed = false;
	}

	public HuffmanEncoder(SourceModel source_model, long precision) {
		this(createCodeMapFromModel(source_model, precision));
	}

	public Map<Symbol, String> getCodeMap() {
		return _code_map;
	}

	@Override
	public void encode(Symbol s, BitSink out) throws IOException {
		if (_closed) {
			throw new RuntimeException("Attempt to encode symbol on closed encoder");
		}
		
		if (_code_map.containsKey(s)) {
			out.write(_code_map.get(s));
		} else {
			throw new RuntimeException("Symbol not in code map");
		}
	}

	@Override
	public void close(BitSink out) throws IOException {
		out.padToWord();
		_closed = true;
	}

	public static Map<Symbol, String> createCodeMapFromModel(SourceModel m, long precision) {

		int symbol_count = m.getSymbolCount();

		List<HENode> nodes = new ArrayList<HENode>();
		for (int i=0; i<symbol_count; i++) {
			nodes.add(new HELeafNode(m.getByIndex(i), precision));
		}

		while (nodes.size() > 1) { 
			Collections.sort(nodes, new Comparator<HENode>() {
				@Override
				public int compare(HENode a, HENode b) {
					if (a.probability() < b.probability()) {
						return 1;
					} else if (a.probability() > b.probability()) {
						return -1;
					} else {
						if (a.depthBelow() < b.depthBelow()) {
							return 1;
						} else if (a.depthBelow() > b.depthBelow()) {
							return -1;
						}
					}
					return 0;
				}
			});
			HENode a = nodes.remove(nodes.size()-1);
			HENode b = nodes.remove(nodes.size()-1);
			nodes.add(new HEInternalNode(a, b));
		}

		HENode root = nodes.get(0);

		Map<Symbol, String> code_map = new HashMap<Symbol, String>();
		root.addCodesToMap("", code_map);
		return code_map;
	}

}

abstract class HENode {

	abstract public long probability();
	abstract public int depthBelow();
	abstract public void addCodesToMap(String prefix, Map<Symbol, String> map);

	private long _precision;

	public HENode(long precision) {
		this._precision = precision;
	}

	public long precision() {
		return _precision;
	}

}

class HEInternalNode extends HENode {

	private HENode _node0;
	private HENode _node1;
	private long _probability;
	private int _depth_below;

	public HEInternalNode(HENode a, HENode b) {
		super(a.precision());

		if (a.precision() != b.precision()) {
			throw new RuntimeException("Precision associated with child nodes must be equal");
		}

		_node0 = a;
		_node1 = b;

		_probability = a.probability()+b.probability();
		if (a.depthBelow() > b.depthBelow()) {
			_depth_below = 1 + a.depthBelow();
		} else {
			_depth_below = 1 + b.depthBelow();
		}
	}

	@Override
	public long probability() {
		return _probability;
	}

	@Override
	public int depthBelow() {
		return _depth_below;
	}

	@Override
	public void addCodesToMap(String prefix, Map<Symbol, String> map) {
		_node0.addCodesToMap(prefix+"0", map);
		_node1.addCodesToMap(prefix+"1", map);
	}

}

class HELeafNode extends HENode {

	private SymbolModel _sym_model;

	public HELeafNode(SymbolModel symbol_model, long precision) {
		super(precision);
		_sym_model = symbol_model;
	}


	@Override
	public long probability() {
		return _sym_model.getProbability(precision());
	}

	@Override
	public int depthBelow() {
		return 0;
	}

	@Override
	public void addCodesToMap(String prefix, Map<Symbol, String> map) {
		map.put(_sym_model.getSymbol(), prefix);
	}
}

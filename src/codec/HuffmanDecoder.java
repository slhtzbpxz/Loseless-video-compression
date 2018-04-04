package codec;

import java.io.IOException;
import java.util.Map;
import models.Symbol;
import io.BitSource;
import io.InsufficientBitsLeftException;

public class HuffmanDecoder implements SymbolDecoder {

	private HDNode _root;
	
	public HuffmanDecoder(Map<Symbol, String> code_map) {
		_root = new HDInternalNode();

		for (Symbol symbol : code_map.keySet()) {
			String code = code_map.get(symbol);
			HDNode node = _root;
			for (int i=0; i<code.length()-1; i++) {
				if (code.charAt(i) == '0') {
					node = node.zeroChild();
				} else {
					node = node.oneChild();
				}
			}
			if (code.charAt(code.length()-1) == '0') {
				node.zeroChild(new HDLeafNode(symbol));
			} else {
				node.oneChild(new HDLeafNode(symbol));
			}
		}
		
		if (!_root.isSet()) {
			throw new RuntimeException("Code map provided does not define leafs on all paths through tree");
		}
	}
	
	/* (non-Javadoc)
	 * @see codec.SymbolDecoder#decode(io.BitSource)
	 */
	@Override
	public Symbol decode(BitSource bit_source) throws InsufficientBitsLeftException, IOException {
		HDNode node = _root;
		
		while (!node.isLeaf()) {
			if (bit_source.next(1) == 0x1) {
				node = node.oneChild();
			} else {
				node = node.zeroChild();
			}
		}
		return ((HDLeafNode) node).symbol();
	}
	
	private abstract class HDNode {
		abstract boolean isLeaf();
		abstract boolean isSet();
		abstract HDNode zeroChild();
		abstract HDNode oneChild();
		abstract void zeroChild(HDNode leaf);
		abstract void oneChild(HDNode leaf);
	}
	
	private class HDLeafNode extends HDNode {
		private Symbol _symbol;
		
		public HDLeafNode(Symbol symbol) {
			_symbol = symbol;
		}
		
		public boolean isLeaf() {return true;}
		public boolean isSet() {return true;}
		
		public HDNode zeroChild() {
			throw new RuntimeException("Can't ask for child of leaf");
		}
		public HDNode oneChild() {
			throw new RuntimeException("Can't ask for child of leaf");
		}
		public void zeroChild(HDNode node) {
			throw new RuntimeException("Can't set child of leaf");
		}
		public void oneChild(HDNode node) {
			throw new RuntimeException("Can't set child of leaf");
		}		
		
		public Symbol symbol() {
			return _symbol;
		}
	}
	
	private class HDInternalNode extends HDNode {

		private HDNode _zero;
		private HDNode _one;
		
		public HDInternalNode() {
			_zero = null;
			_one = null;
		}
		
		public boolean isLeaf() { return false;}
		
		public boolean isSet() {
			return ((_zero != null) &&
				    (_one != null) &&
				    (_zero.isSet()) &&
				    (_one.isSet()));
		}
		
		public HDNode zeroChild() {
			if (_zero == null) {
				_zero = new HDInternalNode();
			}
			
			return _zero;
		}
		
		public void zeroChild(HDNode child) {
			if (_zero != null) {
				throw new RuntimeException("Zero child should only be set once.");
			}
			_zero = child;
		}
		
		public HDNode oneChild() {
			if (_one == null) {
				_one = new HDInternalNode();
			}
			return _one;
		}
		
		public void oneChild(HDNode child) {
			if (_one != null) {
				throw new RuntimeException("One child should only be set once.");
			}
			_one = child;
		}

	}
}

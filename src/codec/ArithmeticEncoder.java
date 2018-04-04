package codec;

import java.io.IOException;

import io.BitSink;
import models.SourceModel;
import models.Symbol;
import models.SymbolModel;

public class ArithmeticEncoder implements SymbolEncoder {
	static final long HIGH_BIT_MASK = 0x80000000L;
	static final long RANGE_MAX = 0xffffffffL;
	static final long LOW_WORD_MASK = 0xffffffffL;
	static final long THREE_QUARTER_THRESHOLD = 0xc0000000L;
	static final long ONE_QUARTER_THRESHOLD = 0x3fffffffL;
	
	private SourceModel _model;
	private long _low;
	private long _high;
	private int _pending_bits;
	private boolean _closed;

	public ArithmeticEncoder(SourceModel m) {
		_model = m;
		_low = 0;
		_high = RANGE_MAX;
		_pending_bits = 0;
		_closed = false;
	}

	public SourceModel model() {
		return _model;
	}
	
	public void model(SourceModel m) {
		_model = m;
	}
	
	public void close(BitSink out) throws IOException {
		if (!_closed) {
			_closed = true;
			out.write(0x80000000, 32);
			out.padToWord();
		}
	}
	
	@Override
	public void encode(Symbol s, BitSink out) throws IOException {

		if (_model == null) {
			throw new RuntimeException("No source model installed");
		}
		
		if (_closed) {
			throw new RuntimeException("Arithmetic encoder already closed");
		}
		
		long sym_range_low = _low;
		long sym_range_high = 0;
		
		for (int i=0; i<_model.getSymbolCount(); i++) {
			SymbolModel sym_model = _model.getByIndex(i);
			if (sym_model.getSymbol().equals(s)) {
				sym_range_high = sym_range_low + sym_model.getProbability(range()) - 1;
				break;
			}
			sym_range_low += sym_model.getProbability(range());
		}
		
		if (sym_range_high == 0) {
			// This only happens if we never see the symbol. 
			throw new RuntimeException("Symbol to be encoded not in symbol model.");
		}

		_high = sym_range_high;
		_low = sym_range_low;
		
		while (high_order_bit(_high) == high_order_bit(_low)) {
			out.write(high_order_bit(_high), 1);
			
			while(_pending_bits > 0) {
				out.write(1-high_order_bit(_high), 1);
				_pending_bits--;
			}
			_high = ((_high << 1) | 0x1) & LOW_WORD_MASK;
			_low = (_low << 1) & LOW_WORD_MASK;
		}
		
		while ((_high < THREE_QUARTER_THRESHOLD) && 
			   (_low > ONE_QUARTER_THRESHOLD)) {
			_pending_bits++;
			_high = ((_high << 1) | 0x1 | HIGH_BIT_MASK) & LOW_WORD_MASK;
			_low = ((_low << 1) & (~HIGH_BIT_MASK)) & LOW_WORD_MASK;
		}				
	}

	private static int high_order_bit(long value) {
		if ((value & HIGH_BIT_MASK) == HIGH_BIT_MASK) {
			return 1;
		} else {
			return 0;
		}
	}
	
	private long range() {
		return _high-_low+1;
	}

}

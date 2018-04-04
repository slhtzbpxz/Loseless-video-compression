package codec;

import java.io.IOException;

import io.BitSource;
import io.InsufficientBitsLeftException;
import models.SourceModel;
import models.Symbol;
import models.SymbolModel;

public class ArithmeticDecoder implements SymbolDecoder {
	static final long HIGH_BIT_MASK = 0x80000000L;
	static final long RANGE_MAX = 0xffffffffL;
	static final long LOW_WORD_MASK = 0xffffffffL;
	static final long THREE_QUARTER_THRESHOLD = 0xc0000000L;
	static final long ONE_QUARTER_THRESHOLD = 0x3fffffffL;

	private SourceModel _model;
	private long _low;
	private long _high;
	private long _buffer;
	private int _bits_needed;

	public ArithmeticDecoder(SourceModel m) {
		_model = m;
		_low = 0;
		_high = RANGE_MAX;
		_bits_needed = 32;
		_buffer = 0x0;
	}

	public SourceModel model() {
		return _model;
	}

	public void model(SourceModel m) {
		_model = m;
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

	@Override
	public Symbol decode(BitSource bit_source) throws InsufficientBitsLeftException, IOException {

		if (_model == null) {
			throw new RuntimeException("No source model installed");
		}

		if (_bits_needed > 0) {
			_buffer |= (((long) bit_source.next(_bits_needed)) & LOW_WORD_MASK);
			_bits_needed = 0;
		}

		long sym_range_low = _low;
		long sym_range_high = _high;
		Symbol sym = null;

		if ((sym_range_low > _buffer) ||
				(sym_range_high < _buffer)) {
			throw new RuntimeException("Range error. This should never happen");
		}

		for (int i=0; i<_model.getSymbolCount(); i++) {
			SymbolModel sym_model = _model.getByIndex(i);
			if (_buffer < sym_range_low + sym_model.getProbability(range())) {
				sym_range_high = sym_range_low + sym_model.getProbability(range()) - 1;
				if (sym_range_high < sym_range_low) {
					throw new RuntimeException("This shouldn't happen");
				}
				sym = sym_model.getSymbol();
				break;
			}
			sym_range_low += sym_model.getProbability(range());
		}

		if (sym == null) {
			throw new RuntimeException("Symbol model in error. Sum of probability ranges insufficient to include current value of buffer.");
		}
		
		_high = sym_range_high;
		_low = sym_range_low;

		while (high_order_bit(_high) == high_order_bit(_low)) {
			_buffer = (_buffer << 1) & LOW_WORD_MASK;
			_bits_needed++;

			_high = ((_high << 1) | 0x1) & LOW_WORD_MASK;
			_low = (_low << 1) & LOW_WORD_MASK;
		}

		while ((_high < THREE_QUARTER_THRESHOLD) && 
			   (_low > ONE_QUARTER_THRESHOLD)) {
			int high_bit = high_order_bit(_buffer);
			_buffer = (_buffer << 1) & LOW_WORD_MASK;
			_bits_needed++;
			if (high_bit == 1) {
				_buffer |= HIGH_BIT_MASK;
			} else {
				_buffer &= (~HIGH_BIT_MASK);
			}

			_high = ((_high << 1) | 0x1 | HIGH_BIT_MASK) & LOW_WORD_MASK;
			_low = ((_low << 1) & (~HIGH_BIT_MASK)) & LOW_WORD_MASK;
		}	

		return sym;
	}
}

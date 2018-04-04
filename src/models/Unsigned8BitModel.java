package models;

import java.io.IOException;
import java.io.InputStream;

public class Unsigned8BitModel implements SourceModel {

	public class Unsigned8BitSymbol implements Symbol {

		private int _value;

		public Unsigned8BitSymbol(int value) {
			if (value < 0 || value > 255) {
				throw new IllegalArgumentException("Value out of range");
			}
			_value = value;
		}

		public int getValue() {
			return _value;
		}
		
		@Override
		public int compareTo(Symbol o) {
			if (!(o instanceof Unsigned8BitSymbol)) {
				throw new IllegalArgumentException("Unsigned8BitSymbol only comparable to type of same");
			}
			Unsigned8BitSymbol other = (Unsigned8BitSymbol) o;
			if (other.getValue() > getValue()) {
				return -1;
			} else if (other.getValue() < getValue()) {
				return 1;
			} else {
				return 0;
			}
		}
		
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof Symbol)) {
				return false;
			}
			return (compareTo((Symbol) o) == 0);
		}
		
		@Override
		public int hashCode() {
			return getValue();
		}
		
		@Override
		public String toString() {
			return "" + getValue();
				
		}
		
	}
	
	public class Unsigned8BitSymbolModel implements SymbolModel {
		private Unsigned8BitSymbol _symbol;
		private long _count;
		private Unsigned8BitModel _model;
		
		public Unsigned8BitSymbolModel(int value, long init_count, Unsigned8BitModel model) {
			_symbol = new Unsigned8BitSymbol(value);
			_count = init_count;
			_model = model;
		}
		
		public void incrementCount() {
			_count++;
		}

		@Override
		public long getProbability(long precision) {
			return _count * precision / _model.getCountTotal();
		}

		@Override
		public Symbol getSymbol() {
			return _symbol;
		}

		public long getCount() {
			return _count;
		}
	}
	
	private Unsigned8BitSymbolModel[] _values;
	private long _count_total;

	public Unsigned8BitModel() {
		_values = new Unsigned8BitSymbolModel[256];
		for (int v=0; v<256; v++) {
			_values[v] = new Unsigned8BitSymbolModel(v, 1, this);
		}
		_count_total = 256;
	}
	
	public Unsigned8BitModel(long[] counts) {
		_values = new Unsigned8BitSymbolModel[256];
		_count_total = 0;
		for (int v=0; v<256; v++) {
			_values[v] = new Unsigned8BitSymbolModel(v, counts[v], this);
			_count_total += counts[v];
		}
	}

	public long getCountTotal() {
		return _count_total;
	}

	public void train(InputStream src, long input_count) 
			throws IOException
	{
		while (input_count > 0) {
			_values[src.read()].incrementCount();
			_count_total++;
			input_count--;
		}
	}
	
	public void train(int value) {
		_values[value].incrementCount();
		_count_total++;
	}

	@Override
	public int getSymbolCount() {
		return _values.length;
	}

	@Override
	public SymbolModel getByIndex(int i) {
		return _values[i];
	}
}
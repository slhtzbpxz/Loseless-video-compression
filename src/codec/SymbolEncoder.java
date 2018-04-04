package codec;

import java.io.IOException;
import io.BitSink;
import models.Symbol;

public interface SymbolEncoder {	
	
	/* encode
	 * Given Symbol s, encode the symbol sending any 
	 * output bits to provided BitSink.
	 */
	void encode(Symbol s, BitSink out) throws IOException;

	/* close
	 * Write any necessary pending bits and pad output
	 * to flush to sink target. Once closed, the
	 * SymbolEncoder should not be used to encode any
	 * additional symbols and any attempt to do so should result in a
	 * runtime exception.
	 */
	void close(BitSink out) throws IOException;
}
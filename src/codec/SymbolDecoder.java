package codec;

import java.io.IOException;

import io.BitSource;
import io.InsufficientBitsLeftException;
import models.Symbol;

public interface SymbolDecoder {

	/* decode
	 * Decodes next symbol from bit source provided.
	 */
	Symbol decode(BitSource bit_source) throws InsufficientBitsLeftException, IOException;

}
package apps;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import codec.ArithmeticDecoder;
import codec.ArithmeticEncoder;
import codec.HuffmanDecoder;
import codec.HuffmanEncoder;
import codec.SymbolDecoder;
import codec.SymbolEncoder;
import io.BitSink;
import io.BitSource;
import io.InputStreamBitSource;
import io.InsufficientBitsLeftException;
import io.OutputStreamBitSink;
import models.Symbol;
import models.SymbolModel;
import models.Unsigned8BitModel;
import models.Unsigned8BitModel.Unsigned8BitSymbol;

public class SimpleFileApp {

	public static void main(String[] args) 
			throws FileNotFoundException, IOException
		{
			String filename="/Users/carrotykim/Desktop/bunny/bunny.450p.yuv";
			File file = new File(filename);
			long length = file.length();
			InputStream training_values = new FileInputStream(file);
			
			Unsigned8BitModel model = new Unsigned8BitModel();
			model.train(training_values, length);
			training_values.close();
			
			System.out.println("Model symbol count: " + model.getSymbolCount());
			System.out.println("Model count total: " + model.getCountTotal());

			
//			SymbolEncoder encoder = new HuffmanEncoder(model, model.getCountTotal());
			SymbolEncoder encoder = new ArithmeticEncoder(model);
			
//			Map<Symbol, String> code_map = ((HuffmanEncoder) encoder).getCodeMap();
			
			Symbol[] symbols = new Unsigned8BitSymbol[256];
			for (int v=0; v<256; v++) {
				SymbolModel s = model.getByIndex(v);
				Symbol sym = s.getSymbol();
				symbols[v] = sym;

				long prob = s.getProbability(model.getCountTotal());
//				System.out.println("Symbol: " + sym + " probability: " + prob + "/" + model.getCountTotal() + " code: " + code_map.get(sym));
				System.out.println("Symbol: " + sym + " probability: " + prob + "/" + model.getCountTotal());
			}

			InputStream message = new FileInputStream(file);
			
			File out_file = new File("/Users/carrotykim/Desktop/bunny/test-compressed.dat");
			OutputStream out_stream = new FileOutputStream(out_file);
			BitSink bit_sink = new OutputStreamBitSink(out_stream);
			
			int next_value = message.read();
			
			while (next_value != -1) {
				encoder.encode(symbols[next_value], bit_sink);
				next_value = message.read();
			}
			
			message.close();
			
			encoder.close(bit_sink);

			out_stream.close();
			
			System.out.println("Done compressing");
			
			BitSource bit_source = new InputStreamBitSource(new FileInputStream(out_file));
			OutputStream decoded_file = new FileOutputStream(new File("/Users/carrotykim/Desktop/bunny/test-decompressed.dat"));
			
//			SymbolDecoder decoder = new HuffmanDecoder(((HuffmanEncoder) encoder).getCodeMap());
			SymbolDecoder decoder = new ArithmeticDecoder(model);
			
			int num_decoded = 0;
			while (num_decoded < length) {
				try {
					Symbol sym = decoder.decode(bit_source);
					decoded_file.write(((Unsigned8BitSymbol) sym).getValue());
					num_decoded++;
				} catch (InsufficientBitsLeftException e) {
					System.out.println("At end of bit source");
					break;
				}
			}
			decoded_file.close();			
		}

}

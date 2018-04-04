package apps;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.Unsigned8BitModel;
import models.ProgressBar;
import models.Unsigned16BitModel;
import codec.HuffmanEncoder;
import codec.SymbolDecoder;
import codec.SymbolEncoder;
import models.Symbol;
import models.SymbolModel;
import models.Unsigned8BitModel.Unsigned8BitSymbol;
import models.Unsigned16BitModel.Unsigned16BitSymbol;
import io.InsufficientBitsLeftException;
import io.BitSink;
import io.BitSource;
import codec.ArithmeticDecoder;
import codec.ArithmeticEncoder;
import codec.HuffmanDecoder;
import io.InputStreamBitSource;
import io.OutputStreamBitSink;

public class MyVideoApp {
	static int max_value= 256;

	public static void main(String[] args) throws IOException, InsufficientBitsLeftException {
		String base = "pinwheel";
		String filename = "/Users/carrotykim/Desktop/pinwheel/" + base + ".450p.yuv";
		File file = new File(filename);
		int width = 800;
		int height = 450;
		int num_frames = 150;
		int[] compressed_frame_size = new int [num_frames]; 

		Unsigned16BitModel model = new Unsigned16BitModel();

		InputStream training_values = new FileInputStream(file);
		int[][] current_frame = new int[width][height];
		
		double rate =0.0;

		for (int f = 0; f < num_frames; f++) {
			System.out.println("Training frame difference " + f);
			int[][] prior_frame = current_frame;
			current_frame = readFrame(training_values, width, height);
			int[][] diff_frame = frameDifference(prior_frame, current_frame);
			List<String> uncompressed = frame2list(diff_frame);
			List<Integer> compressed =  LZW_compress(uncompressed);
			compressed_frame_size[f] = compressed.size();
			//System.out.println("uncompressed size is"+uncompressed.size());
			//System.out.println("compressed size is"+compressed.size());
			List<String> decompressed= LZW_decompress(compressed);
			//System.out.println("decompressed size is"+decompressed.size());
			System.out.println(max_value);
		    trainModelWithLzwFrame(model, compressed);
		    rate+=(double)compressed.size()/uncompressed.size();
		    
		}
		
		
		
		//System.out.println("compressed ratio is"+(double)rate/num_frames);
		//return;
		
	

	
		
		training_values.close();

		// HuffmanEncoder encoder = new HuffmanEncoder(model,
		// model.getCountTotal());
		// Map<Symbol, String> code_map = encoder.getCodeMap();

		SymbolEncoder encoder = new ArithmeticEncoder(model);

		Symbol[] symbols = new Unsigned16BitSymbol[max_value];
		for (int v = 0; v < max_value; v++) {
			SymbolModel s = model.getByIndex(v);
			Symbol sym = s.getSymbol();
			symbols[v] = sym;
			long prob = s.getProbability(model.getCountTotal());
			System.out.println("Symbol: " + sym + " probability: " + prob + "/" + model.getCountTotal());
		}

		InputStream message = new FileInputStream(file);

		File out_file = new File("/Users/carrotykim/Desktop/pinwheel/" + base + "-compressed.dat");
		OutputStream out_stream = new FileOutputStream(out_file);
		BitSink bit_sink = new OutputStreamBitSink(out_stream);

		
		current_frame = new int[width][height];
      
		for (int f = 0; f < num_frames; f++) {
			System.out.println("Encoding frame difference " + f);
			int[][] prior_frame = current_frame;
			current_frame = readFrame(message, width, height);
			int[][] diff_frame = frameDifference(prior_frame, current_frame);
			List<String> uncompressed = frame2list(diff_frame);
			List<Integer> compressed =  LZW_compress(uncompressed);
			encodeLzwCompressed(compressed, encoder, bit_sink, symbols);
			//encodeFrameDifference(diff_frame, encoder, bit_sink, symbols);
		}
		

		message.close();
		encoder.close(bit_sink);
		out_stream.close();

		BitSource bit_source = new InputStreamBitSource(new FileInputStream(out_file));
		OutputStream decoded_file = new FileOutputStream(
				new File("/Users/carrotykim/Desktop/pinwheel/" + base + "-decoded.dat"));
		// SymbolDecoder decoder = new HuffmanDecoder(encoder.getCodeMap());
       
		SymbolDecoder decoder = new ArithmeticDecoder(model);

		current_frame = new int[width][height];

		for (int f = 0; f < num_frames; f++) {
			System.out.println("Decoding frame " + f);
			List<Integer> lzw_compressed = LZWdecodeFrame(decoder, bit_source, compressed_frame_size[f]);
			List<String> lzw_decompressed = LZW_decompress(lzw_compressed); 
			int[][] diff_frame = list2frame(lzw_decompressed,width,height);
			int[][] prior_frame = current_frame;
			current_frame = reconstructFrame(prior_frame, diff_frame);
			outputFrame(current_frame, decoded_file);
		}

		decoded_file.close();
	

	}
	


	private static int[][] readFrame(InputStream src, int width, int height) throws IOException {
		int[][] frame_data = new int[width][height];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				frame_data[x][y] = src.read();
			}
		}
		return frame_data;
	}

	
	
	
	//Change the 2D array into a list of string
	private static List<String> frame2list(int[][] frame) {
		List<String> result=new ArrayList();
		int width = frame.length;
		int height = frame[0].length;
		
		for (int y=0; y<height; y++) {
			for (int x=0; x<width; x++) {
				String curPx=("00")+Integer.toString(frame[x][y]);
				result.add(curPx.substring(curPx.length()-3));
			}
		}
		return result;
	}
	
	// Inverse process of frame2list
	private static int[][] list2frame(List<String> lzw_compressed, int width,int height) {
		
		int [][]diff_frame = new int[width][height];
		int index =0;
		List<String> result=new ArrayList();
		for (int y=0; y<height; y++) {
			for (int x=0; x<width; x++) {			
				diff_frame[x][y]= Integer.parseInt(lzw_compressed.get(index++));
			}
		}
		return diff_frame;
	}
	
	
	//Compress using LZW
	private static List<Integer> LZW_compress(List<String> uncompressed) {

      int dictSize = 256;
      Map<String,Integer> dictionary = new HashMap<String,Integer>();
      for (int i = 0; i < 256; i++) {
      		String curStr=(("00")+Integer.toString(i));
          dictionary.put(curStr.substring(curStr.length()-3), i);
      }
      String w = "";
      List<Integer> result = new ArrayList<Integer>();
      for (String c:uncompressed) {
          String wc = w + c;
          if (dictionary.containsKey(wc))
              w = wc;
          else {
              result.add(dictionary.get(w));
              dictionary.put(wc, dictSize++);
              w = "" + c;
          }
      }

      // Output the code for w.
      if (!w.equals(""))
          result.add(dictionary.get(w));
          max_value=Math.max(max_value,dictionary.size());
      return result;
	}

	
	//Decompressing using LZW
	

	private static List<String> LZW_decompress(List<Integer> compressed) {

        int dictSize = 256;
        Map<Integer,String> dictionary = new HashMap<Integer,String>();
        for (int i = 0; i < 256; i++) {
        		String curStr=(("00")+Integer.toString(i));
        		dictionary.put(i,curStr.substring(curStr.length()-3));
        }
        String w = Integer.toString(compressed.remove(0));
        w=("00"+w);
        w=w.substring(w.length()-3);
        List<String> result=new ArrayList<String>();
        result.add(w);
        for (int k : compressed) {
            String entry;
            if (dictionary.containsKey(k)) {
                entry = dictionary.get(k);
            }
            else if (k == dictSize) {
            		entry=w+w.substring(0, Math.min(w.length(), 3));
            		if(entry.length()%3!=0) {System.out.println(result.toString());}
            }
            else
                throw new IllegalArgumentException("Bad compressed k: " + k);
            String[] arr=entry.split("(?<=\\G...)");
            for(String s:arr) {
            		result.add(s);
            }

            	dictionary.put(dictSize++, w + entry.substring(0,Math.min(entry.length(), 3)));
            w = entry;
        }
        return result;
	}
	
	
	private static void encodeLzwCompressed(List<Integer> compressed, SymbolEncoder encoder, BitSink bit_sink, Symbol[] symbols)
			throws IOException {
		    ProgressBar progess = new ProgressBar();
			for (Integer x : compressed) {
				encoder.encode(symbols[x], bit_sink);
				}
			}
	
	private static List<Integer> LZWdecodeFrame(SymbolDecoder decoder, BitSource bit_source, int compressed_size)
			throws InsufficientBitsLeftException, IOException {
		     List<Integer> lzw_compressed = new ArrayList<Integer>();
			for (int x = 0; x <compressed_size ; x++) {
				lzw_compressed.add( ((Unsigned16BitSymbol) decoder.decode(bit_source)).getValue());
			}
		return lzw_compressed;
	}
	
	
	private static int[][] frameDifference(int[][] prior_frame, int[][] current_frame) {
		int width = prior_frame.length;
		int height = prior_frame[0].length;

		int[][] difference_frame = new int[width][height];

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				difference_frame[x][y] = ((current_frame[x][y] - prior_frame[x][y]) + 256) % 256;
			}
		}
		return difference_frame;
	}

	private static void trainModelWithFrame(Unsigned16BitModel model, int[][] frame) {
		int width = frame.length;
		int height = frame[0].length;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				model.train(frame[x][y]);
			}
		}
	} 
	
	private static void trainModelWithLzwFrame(Unsigned16BitModel model, List<Integer> diff_frame_lzw_compressed ) {
			for (int x:diff_frame_lzw_compressed) {
				model.train(x);
			}
	}

	private static void encodeFrameDifference(int[][] frame, SymbolEncoder encoder, BitSink bit_sink, Symbol[] symbols)
			throws IOException {

		int width = frame.length;
		int height = frame[0].length;

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				encoder.encode(symbols[frame[x][y]], bit_sink);
			}
		}
	}

	private static int[][] decodeFrame(SymbolDecoder decoder, BitSource bit_source, int width, int height)
			throws InsufficientBitsLeftException, IOException {
		int[][] frame = new int[width][height];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				frame[x][y] = ((Unsigned8BitSymbol) decoder.decode(bit_source)).getValue();
			}
		}
		return frame;
	}

	private static int[][] reconstructFrame(int[][] prior_frame, int[][] frame_difference) {
		int width = prior_frame.length;
		int height = prior_frame[0].length;

		int[][] frame = new int[width][height];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				frame[x][y] = (prior_frame[x][y] + frame_difference[x][y]) % 256;
			}
		}
		return frame;
	}

	private static void outputFrame(int[][] frame, OutputStream out) throws IOException {
		int width = frame.length;
		int height = frame[0].length;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				out.write(frame[x][y]);
			}
		}
	}
}

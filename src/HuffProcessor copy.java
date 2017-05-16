import java.util.PriorityQueue;

public class HuffProcessor implements Processor{

	String[] codes;

	//count characters in file
	//create huffman tree
	//traverse tree and extract codes
	//write the header
	//compress/write the body
	//write pseudo-EOF

	private void extractCodes(HuffNode current, String path){
		//current = new HuffNode(current.value(), current.weight(), current.left(), current.right());
		if(current.left() == null && current.right() == null){
			codes[current.value()] = path;
			return;
		}
		extractCodes(current.left(), path + 0);
		extractCodes(current.right(), path + 1);
	}



	private void writeHeader(HuffNode current, BitOutputStream out){
		//current = new HuffNode(current.value(), current.weight(), current.left(), current.right());
		if(current.left() == null && current.right() == null){
			out.writeBits(1,1);
			out.writeBits(9, current.value());
			return;
		}
		out.writeBits(1, 0);
		writeHeader(current.left(), out);
		writeHeader(current.right(), out);
	}



	public void compress(BitInputStream in, BitOutputStream out){
		int[] counts = new int[256];
		int let = in.readBits(BITS_PER_WORD);
		while(let != -1){
			counts[let]++;
			let = in.readBits(BITS_PER_WORD);
		}
		in.reset();
		codes = new String[counts.length + 1];

		//priority queue
		PriorityQueue<HuffNode> huffTree = new PriorityQueue<HuffNode>();
		for(int i = 0; i < 256; i++){
			if(counts[i] != 0){
				//add new HuffNode
				HuffNode letter = new HuffNode(i, counts[i]);
				huffTree.add(letter);
			}
		}
		HuffNode pseudo = new HuffNode(PSEUDO_EOF, 0);
		huffTree.add(pseudo);

		while(huffTree.size() > 1){
			HuffNode left = huffTree.poll();
			HuffNode right = huffTree.poll();
			//combine them into one node
			HuffNode newHuff = new HuffNode(-1, left.weight() + right.weight(),left, right);
			huffTree.add(newHuff);
		}

		HuffNode last = huffTree.poll();
		String path = "";
		extractCodes(last, path);

		out.writeBits(BITS_PER_INT, HUFF_NUMBER);

		writeHeader(last, out);

		String chars = "";
		int let2 = in.readBits(BITS_PER_WORD);
		while(let2 != -1){
			chars = codes[let2];
			out.writeBits(chars.length(), Integer.parseInt(chars, 2));
			let2 = in.readBits(BITS_PER_WORD);
		}
		
		chars = codes[PSEUDO_EOF];
		out.writeBits(chars.length(), Integer.parseInt(chars, 2));
		
		int num = 0;
		for(int i = 0; i < counts.length; i++){
			if(counts[i] != 0){
				num++;
			}
		}
		System.out.print(num);
	}


	//check for HUFF_NUMBER
	//recreate tree from header
	//parse body of compressed file

	private HuffNode readHeader(BitInputStream in){
		if(in.readBits(1) == 0){
			HuffNode left = readHeader(in);
			HuffNode right = readHeader(in);
			HuffNode comb = new HuffNode(-1, 0, left, right);
			return comb;
		}
		else{
			HuffNode ret = new HuffNode(in.readBits(9), 0);
			return ret;
		}
	}

	public void decompress(BitInputStream in, BitOutputStream out){
		if(in.readBits(32) != HUFF_NUMBER){
			throw new HuffException(null);
		}

		HuffNode root = readHeader(in);
		HuffNode current = root;
		int let = in.readBits(1);

		while(let != -1){
			if(let == 1){
				current = current.right();
			}
			else{
				current = current.left();
			}

			if(current.value() > -1){
				if(current.value() == PSEUDO_EOF){
					return;
				}
				else{
					out.writeBits(8, current.value());
					current = root;
				}
			}
			let = in.readBits(1);
		}
		throw new HuffException("PSEUDO_EOF error");
	}
}

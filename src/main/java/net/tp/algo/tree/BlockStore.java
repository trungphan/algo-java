package net.tp.algo.tree;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * ref http://opendatastructures.org/ods-java/14_2_B_Trees.html
 * @author Trung
 *
 * @param <N>
 */
public class BlockStore implements Closeable {
	
	private File file;
	private FileInputStream fis;
	private FileOutputStream fos;
	private FileChannel fic;
	private FileChannel foc;
	private int blocksize = 4096;
	private Set<Integer> freeBlocks = new HashSet<>();
	private byte[] freeBlockBits = new byte[4096];
	private int blocks;

	public BlockStore(File file) {
		this.file = file;
		if (!this.file.exists()) {
			try {
				this.file.createNewFile();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
		this.blocks = (int)this.file.length() / 4096 + 1;
		
		try {
			this.fis = new FileInputStream(this.file);
			this.fic = fis.getChannel();
			this.fos = new FileOutputStream(this.file, true);
			this.foc = fos.getChannel();
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		
		readMetaData();
		
	}
	
	@Override
	public void finalize() {
		close();
	}
	
	private void readMetaData() {
		readBlock(0, freeBlockBits);
		for (int x = 0; x < 4096; x++) {
			int i = (x << 8);
			byte bits = freeBlockBits[x];
			while (bits > 0) {
				if ((bits & (byte)1) > 0) {
					freeBlocks.add(i);
				}
				bits >>>= 1;
				i++;
			}
		}
	}
	
	private void writeMetaData() {
		writeBlock(0, freeBlockBits);
	}
	
	public void close() {
		if (this.fis != null) {
			try {
				this.fis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.fis = null;
		}
		if (this.fos != null) {
			try {
				this.fos.flush();
				this.fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.fos = null;
		}
		
	}
	
	public void readBlock(int i, byte[] bytes) {
		try {
			ByteBuffer bb = ByteBuffer.wrap(bytes);
			fic.read(bb, i * blocksize);
			
			System.out.println(Arrays.toString(bytes));
			
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public void writeBlock(int i, byte[] bytes) {
		
		try {
			ByteBuffer bb = ByteBuffer.wrap(bytes);
			foc.write(bb, i * blocksize);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		
	}
	
	public int placeBlock(byte[] bytes) {
		
		int i = freeBlocks.size() > 0 ? freeBlocks.iterator().next() : blocks++;
		
		freeBlockBits[i >>> 8] &= ~((byte)1 << (i % 8));
		freeBlocks.remove(i);
		
		writeMetaData();
		writeBlock(i, bytes);
		
		return i;
	}
	
	public void freeBlock(int i) {
		freeBlockBits[i >>> 8] |= (byte)1 << (i % 8);
		freeBlocks.add(i);
		
		writeMetaData();
	}

	
	
	public static void main(String ... strings) throws UnsupportedEncodingException {

		BlockStore bs = new BlockStore(new File("Test.bin"));
		
		byte[] buf = new byte[4096];

		int b1 = bs.placeBlock("Block 1".getBytes());
		int b2 = bs.placeBlock("Block 2".getBytes());
		int b3 = bs.placeBlock("Block 3".getBytes());
		int b4 = bs.placeBlock("Block 4".getBytes());
		
		bs.freeBlock(b3);
		
		int b5 = bs.placeBlock("Block 5".getBytes());
		
		bs.freeBlock(b2);
		
		bs.close();
	}
	
}

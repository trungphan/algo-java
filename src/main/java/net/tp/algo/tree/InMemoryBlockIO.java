package net.tp.algo.tree;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class InMemoryBlockIO implements BlockIO {

	private final int blocksize;
	private List<byte[]> blocks;
	
	public InMemoryBlockIO(int blocksize) {
		this.blocksize = blocksize;
		this.blocks = new LinkedList<>();
	}

	@Override
	public int blocksize() {
		return this.blocksize;
	}

	@Override
	public void readDiskBlock(int i, byte[] bytes) {
		Arrays.fill(bytes, (byte)0);
		if (i < blocks.size()) {
			byte[] buf = blocks.get(i);
			if (buf != null) {
				System.arraycopy(buf, 0, bytes, 0, Math.min(buf.length, bytes.length));
			}
		}
	}

	@Override
	public void writeDiskBlock(int i, byte[] bytes) {
		while (i >= blocks.size()) {
			blocks.add(null);
		}
		
		byte[] buf = blocks.get(i);
		if (buf == null) {
			buf = new byte[blocksize];
			blocks.set(i, buf);
		}
		Arrays.fill(buf, (byte)0);
		System.arraycopy(bytes, 0, buf, 0, Math.min(bytes.length, buf.length));
		
	}

	@Override
	public void flush() {
	}
	
	
	
	
}

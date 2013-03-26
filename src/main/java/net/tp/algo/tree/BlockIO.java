package net.tp.algo.tree;

public interface BlockIO {
	
	public int blocksize();
	public void readDiskBlock(int i, byte[] bytes);
	public void writeDiskBlock(int i, byte[] bytes);
	public void flush();
}

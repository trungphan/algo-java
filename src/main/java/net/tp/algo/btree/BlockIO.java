package net.tp.algo.btree;

public interface BlockIO {
	
	public int blocksize();
	public void readBlock(int i, byte[] bytes);
	public void writeBlock(int i, byte[] bytes);
	public void flush();
}

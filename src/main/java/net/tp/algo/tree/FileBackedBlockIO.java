package net.tp.algo.tree;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

public class FileBackedBlockIO implements BlockIO, Closeable {

	private final int blocksize;
	private RandomAccessFile raf;
	private FileChannel fc;
	
	public FileBackedBlockIO(int blocksize) {
		this(null, blocksize);
	}
	
	public FileBackedBlockIO(File file, int blocksize) {
		this.blocksize = blocksize;
		try {
			open(file);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void open(File file) throws IOException {
		if (file == null) {
			return;
		}

		close();
		
		if (!file.exists()) {
			file.createNewFile();
		}
		this.raf = new RandomAccessFile(file, "rw");
		this.fc = raf.getChannel();
	}
	
	
	@Override
	public int blocksize() {
		return blocksize;
	}

	@Override
	public void readBlock(int i, byte[] bytes) {
		
		Arrays.fill(bytes, (byte)0);
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		try {
			fc.read(bb, i * blocksize);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void writeBlock(int i, byte[] bytes) {
		byte[] buf;
		if (bytes.length == blocksize) {
			buf = bytes;
		}
		else if (bytes.length < blocksize) {
			buf = Arrays.copyOf(bytes, blocksize);
		}
		else {
			throw new IllegalStateException();
		}
		ByteBuffer bb = ByteBuffer.wrap(buf);
		try {
			fc.write(bb, i * blocksize);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	@Override
	public void flush() {
		try {
			fc.force(false);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	@Override
	public void close() throws IOException {
		try {
			if (this.fc != null) {
				this.fc.close();
			}
		}
		finally {
			this.fc = null;
			if (this.raf != null) {
				try {
					this.raf.close();
				} finally {
					this.raf = null;
				}
			}
		}
	}

}

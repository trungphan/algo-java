package net.tp.algo.tree;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

public class FileBackedBlockIO implements BlockIO, Closeable {

	private final int blocksize;
	private FileInputStream fis;
	private FileChannel fic;
	private FileOutputStream fos;
	private FileChannel foc;
	
	
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
		
		try {
			this.fis = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}

		try {
			this.fic = fis.getChannel();
			this.fos = new FileOutputStream(file);
			this.foc = fos.getChannel();
		} catch (IOException e) {
			try {
				close();
			} catch (IOException e1) {};
			throw e;
		}
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
			fic.read(bb);
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
			foc.write(bb, i * blocksize);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	@Override
	public void flush() {
		try {
			foc.force(false);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	@Override
	public void close() throws IOException {
		
		IOException ioE = null;
		if (foc != null) {
			try {
				foc.close();
				foc = null;
			} catch (IOException e) {
				ioE = e;
			}
		}
		if (fos != null) {
			try {
				fos.close();
				fos = null;
			} catch (IOException e) {
				ioE = e;
			}
		}
		if (fic != null) {
			try {
				fic.close();
				fic = null;
			} catch (IOException e) {
			}
		}
		if (fis != null) {
			try {
				fis.close();
				fis = null;
			} catch (IOException e) {
			}
		}
		
		if (ioE != null) {
			throw ioE;
		}
	}

}

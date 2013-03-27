package net.tp.algo.btree;

import java.nio.ByteBuffer;

public interface Serializer<T> {

	public T read(ByteBuffer bb);
	
	public void write(ByteBuffer bb, T value);
	
}

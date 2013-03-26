package net.tp.algo.tree;

import java.nio.ByteBuffer;

public interface Serializer<T> {

	public T read(ByteBuffer bb);
	
	public void write(ByteBuffer bb, T value);
	
}

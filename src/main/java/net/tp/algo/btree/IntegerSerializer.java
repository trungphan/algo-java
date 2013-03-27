package net.tp.algo.btree;

import java.nio.ByteBuffer;

public class IntegerSerializer implements Serializer<Integer> {

	@Override
	public Integer read(ByteBuffer bb) {
		return bb.getInt();
	}

	@Override
	public void write(ByteBuffer bb, Integer value) {
		bb.putInt(value == null ? 0 : value.intValue());
	}

}

package net.tp.algo.tree;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;

/**
 * ref http://opendatastructures.org/ods-java/14_2_B_Trees.html
 * @author Trung Phan
 *
 * @param <N>
 */
public class BlockStore {
	
	private int addrSeq;
	
	private int prevMaxBlocks;
	private Set<Integer> prevFreeBlocks;
	private Map<Integer, Integer> prevDictMap;
	
	private int maxBlocks;
	private Set<Integer> freeBlocks;
	private Map<Integer, Integer> dictMap;
	
	private boolean changed = false;
	
	/**
	 * safeFreeBlock is the intersection between prevFreeBlocks and freeBlocks.
	 * It's maintained here to guarantee O(1) time to get freeblock
	 */
	private Set<Integer> safeFreeBlocks;
	
	private final BlockIO blockIO;
	
	public BlockStore(BlockIO blockIO) {
		
		this.blockIO = blockIO;
		
		if (blockIO == null || blockIO.blocksize() <= 0) {
			throw new IllegalArgumentException();
		}
		
		this.prevDictMap = new HashMap<>();
		this.prevFreeBlocks = new HashSet<>();
		
		readMetaData();
	}
	
	private void readMetaData() {
		byte[] buf = new byte[blockIO.blocksize()];
		
		blockIO.readBlock(0, buf);
		ByteBuffer bb = ByteBuffer.wrap(buf);
		
		this.prevMaxBlocks = bb.getInt();
		if (this.prevMaxBlocks == 0) {
			this.prevMaxBlocks = 1;
		}
		this.maxBlocks = this.prevMaxBlocks;
		this.addrSeq = bb.getInt();
		
		int dictBlockIndex = bb.getInt();
		int freeBlockIndex = bb.getInt();
		
		this.prevDictMap = l2m(loadIntArray(blockIO, dictBlockIndex));
		this.dictMap = new HashMap<>(this.prevDictMap);
		this.prevFreeBlocks = new HashSet<>(loadIntArray(blockIO, freeBlockIndex));
		this.freeBlocks = new HashSet<>(this.prevFreeBlocks);
		
		this.safeFreeBlocks = new HashSet<>(this.prevFreeBlocks);
	}
	
	private void writeMetaData() {
		
		List<Integer> blocksForFreeBlocks = newLogicalBlocks((this.freeBlocks.size() + 3) / (blockIO.blocksize() / 4 - 1));
		
		List<Integer> blocksForDictBlocks = newLogicalBlocks((this.dictMap.size() + 7) / (blockIO.blocksize() / 8 - 1) );

		int freeIndex = storeIntArray(blockIO, new LinkedList<>(this.freeBlocks), blocksForFreeBlocks);
		int dictIndex = storeIntArray(blockIO, m2l(this.prevDictMap), blocksForDictBlocks);

		byte[] buf = new byte[blockIO.blocksize()];
		ByteBuffer bb = ByteBuffer.wrap(buf);
		bb.putInt(this.maxBlocks);
		bb.putInt(this.addrSeq);
		bb.putInt(dictIndex);
		bb.putInt(freeIndex);
		
		blockIO.writeBlock(0, buf);
		this.prevFreeBlocks = this.freeBlocks;
		this.prevDictMap = this.dictMap;
	}
	
	private static Map<Integer, Integer> l2m(List<Integer> l) {
		Map<Integer, Integer> result = new HashMap<>();
		for (int i = 0; i < l.size(); i += 2) {
			result.put(l.get(i), l.get(i+1));
		}
		return result;
	}
	
	private static List<Integer> m2l(Map<Integer, Integer> m) {
		List<Integer> result = new LinkedList<>();
		for (Map.Entry<Integer, Integer> e : m.entrySet()) {
			result.add(e.getKey());
			result.add(e.getValue());
		}
		return result;
	}
	
	
	private List<Integer> newLogicalBlocks(int count) {
		List<Integer> result = new ArrayList<>();
		while (count > 0) {
			result.add(this.dictMap.get(newLogicalBlock()));
			count--;
		}
		return result;
	}
	
	private int newLogicalBlock() {
		
		int allocBlock = newDiskBlock();

		this.freeBlocks.remove(allocBlock);
		this.dictMap.put(++this.addrSeq, allocBlock);
		this.changed = true;
		
		return this.addrSeq;
	}

	private int newDiskBlock() {		
		int allocBlock = this.safeFreeBlocks.isEmpty() ? this.maxBlocks++ : safeFreeBlocks.iterator().next();
		this.changed = true;
		return allocBlock;
	}
	
	private int relocateLogicalBlock(int i) {
		Integer currDiskAddr = this.dictMap.get(i);
		if (currDiskAddr == null) {
			throw new IllegalStateException();
		}
		
		int newDiskAddr = newDiskBlock();
		this.dictMap.put(i, newDiskAddr);
		this.freeBlocks.remove(currDiskAddr);
		this.changed = true;

		return newDiskAddr;
	}
	
	public int blocksize() {
		return blockIO.blocksize();
	}
	
	public boolean hasBlock(int i) {
		return this.dictMap.containsKey(i);
	}
	
	public byte[] readBlock(int i) {
		byte[] buf = new byte[blockIO.blocksize()];
		readBlock(i, buf);
		return buf;
	}
	
	public void readBlock(int i, byte[] bytes) {
		Integer diskAddr = this.dictMap.get(i);
		if (diskAddr == null || diskAddr == 0) {
			throw new NoSuchElementException();
		}
		blockIO.readBlock(diskAddr, bytes);
	}
	
	public void writeBlock(int i, byte[] bytes) {
		
		Integer diskAddr = this.dictMap.get(i);
		if (diskAddr == null || diskAddr == 0) {
			throw new NoSuchElementException();
		}
		
		if (diskAddr == this.prevDictMap.get(i)) {
			// copy of write
			diskAddr = relocateLogicalBlock(i);
		}
		
		blockIO.writeBlock(diskAddr, bytes);
	}
	
	public int placeBlock(byte[] bytes) {
		int addr = newLogicalBlock();
		int diskAddr = this.dictMap.get(addr);
		blockIO.writeBlock(diskAddr, bytes);
		return addr;
	}
	
	public void freeBlock(int i) {
		Integer diskAddr = this.dictMap.get(i);
		if (diskAddr == null || diskAddr == 0) {
			throw new NoSuchElementException();
		}

		this.dictMap.remove(i);
		this.freeBlocks.add(diskAddr);
		this.changed = true;
		if (diskAddr >= this.prevMaxBlocks) {
			this.safeFreeBlocks.add(diskAddr);
		}
		
	}
	
	public void flush() {
		if (changed) {
			writeMetaData();
			blockIO.flush();
			
			this.prevDictMap = this.dictMap;
			this.dictMap = new HashMap<>(this.prevDictMap);
			this.prevFreeBlocks = this.freeBlocks;
			this.freeBlocks = new HashSet<>(prevFreeBlocks);
			this.prevMaxBlocks = this.maxBlocks;
			this.safeFreeBlocks = new HashSet<>(this.prevFreeBlocks);
			
			changed = false;
		}
	}
	
	private static List<Integer> loadIntArray(BlockIO blockIO, int addr) {
		List<Integer> result = new ArrayList<>();
		byte[] buf = new byte[blockIO.blocksize()];
		
		while (addr > 0) {
			Arrays.fill(buf, (byte)0);
			blockIO.readBlock(addr, buf);
			ByteBuffer bb = ByteBuffer.wrap(buf);

			int n = bb.getInt();
			addr = bb.getInt(); // nextDiskAddr
			
			for (int i = 0; i < n; i++) {
				result.add(bb.getInt());
			}
			
		}
		
		return result;
	}
	
	private static int storeIntArray(BlockIO blockIO, List<Integer> array, List<Integer> blocks) {
		int last = 0;
		int arrayIndex = 0;
		for (int b : blocks) {
			byte[] buf = new byte[blockIO.blocksize()];
			ByteBuffer bb = ByteBuffer.wrap(buf);
			bb.putInt(0); // temp size; will be changed.
			bb.putInt(last);
			last = b;
			
			int count = 0;
			while (bb.remaining() >= 4 && arrayIndex < array.size()) {
				bb.putInt(array.get(arrayIndex++));
				count++;
			}
			
			bb.putInt(0, count);
			blockIO.writeBlock(b, buf);
		}
		return last;
	}
	
	

	

	public static void main(String ... strings) throws UnsupportedEncodingException {

	}
	
	
	
	public static class TestCase {
		
		@Test
		public void storeArray() {
			
			InMemoryBlockIO blockIO = new InMemoryBlockIO(4 * 4); // 4 int
			List<Integer> array = Arrays.asList(101, 102, 103, 104, 105, 106, 107);
			
			int i = storeIntArray(blockIO, array, Arrays.asList(1, 2, 3, 4));
			System.out.println(i);
			
			List<Integer> array2 = loadIntArray(blockIO, i);
			Collections.sort(array2);
			
			Assert.assertEquals(array, array2);
			
		}
		
		
		
	}
	
}

package net.tp.algo.tree;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
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
	
	private transient List<Integer> metaBlocksForFreeList; // list of logical address to manage free list
	private transient List<Integer> metaBlocksForDictMap; // list of logical address to manage dictionary
	
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
		
		if (blockIO.blocksize() < 10 * 4) {
			throw new IllegalStateException("Block size is not enough to manage overhead: " + blockIO.blocksize());
		}
		
		if (blockIO == null || blockIO.blocksize() <= 0) {
			throw new IllegalArgumentException();
		}
		
		this.prevDictMap = new HashMap<>();
		this.prevFreeBlocks = new HashSet<>();
		this.metaBlocksForFreeList = new ArrayList<>();
		this.metaBlocksForDictMap = new ArrayList<>();
		
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
		
		Set<Integer> metaBlocks = new HashSet<>();
		
		this.prevDictMap = l2m(loadIntArray(blockIO, dictBlockIndex, metaBlocks));
		this.dictMap = new HashMap<>(this.prevDictMap);
		for (Map.Entry<Integer, Integer> entry : this.dictMap.entrySet()) {
			if (metaBlocks.contains(entry.getValue())) {
				this.metaBlocksForDictMap.add(entry.getKey());
			}
		}
		
		metaBlocks.clear();
		this.prevFreeBlocks = new HashSet<>(loadIntArray(blockIO, freeBlockIndex, metaBlocks));
		this.freeBlocks = new HashSet<>(this.prevFreeBlocks);

		for (Map.Entry<Integer, Integer> entry : this.dictMap.entrySet()) {
			if (metaBlocks.contains(entry.getValue())) {
				this.metaBlocksForFreeList.add(entry.getKey());
			}
		}
		
		this.safeFreeBlocks = new HashSet<>(this.prevFreeBlocks);
		
		verifyInvariants();
	}
	
	private void verifyInvariants() {

		int diff = maxBlocks - this.dictMap.size() - this.freeBlocks.size() - 1;
		if (diff != 0) {
			throw new IllegalStateException("Diff: " + diff);
		}
		
		for (Map.Entry<Integer, Integer> entry : this.dictMap.entrySet()) {
			int addr = entry.getKey();
			int diskAddr = entry.getValue();
			if (diskAddr >= maxBlocks) {
				throw new IllegalStateException();
			}
			if (addr > addrSeq) {
				throw new IllegalStateException();
			}
		}
		for (Integer diskAddr : this.freeBlocks) {
			if (diskAddr >= maxBlocks) {
				throw new IllegalStateException();
			}
		}
		for (Integer addr : this.metaBlocksForDictMap) {
			if (this.dictMap.get(addr) == null) {
				throw new IllegalStateException();
			}
		}
		for (Integer addr : this.metaBlocksForFreeList) {
			if (this.dictMap.get(addr) == null) {
				throw new IllegalStateException();
			}
		}
	}
	
	
	private void writeMetaData() {
		
		for (int b : metaBlocksForFreeList) {
			safeRemoveBlock(b);
		}
		for (int b : metaBlocksForDictMap) {
			safeRemoveBlock(b);
		}
		
		int capPerBlock = blockIO.blocksize() / 4 - 2; // measured in int. Minus two for the overhead of storing the next pointer and count.
		int nBlocks = (this.freeBlocks.size() + capPerBlock - 1 ) / capPerBlock;
		List<Integer> newMetaBlocksForFreeList = newLogicalBlocks(nBlocks);
		
		nBlocks = (this.dictMap.size() * 2 + this.metaBlocksForFreeList.size() * 2 + this.metaBlocksForDictMap.size() * 2 + capPerBlock - 1 ) / (capPerBlock - 2);
		List<Integer> newMetaBlocksForDictMap = newLogicalBlocks(nBlocks);
		
		int freeIndex = storeIntArray(blockIO, new LinkedList<>(this.freeBlocks), logicalToDiskAddrs(newMetaBlocksForFreeList));
		int dictIndex = storeIntArray(blockIO, m2l(this.dictMap), logicalToDiskAddrs(newMetaBlocksForDictMap));

		byte[] buf = new byte[blockIO.blocksize()];
		ByteBuffer bb = ByteBuffer.wrap(buf);
		bb.putInt(this.maxBlocks);
		bb.putInt(this.addrSeq);
		bb.putInt(dictIndex);
		bb.putInt(freeIndex);
		
		
		
		blockIO.writeBlock(0, buf);
		this.prevDictMap = this.dictMap;
		this.dictMap = new HashMap<>(this.prevDictMap);
		this.prevFreeBlocks = this.freeBlocks;
		this.freeBlocks = new HashSet<>(prevFreeBlocks);
		this.prevMaxBlocks = this.maxBlocks;
		this.safeFreeBlocks = new HashSet<>(this.freeBlocks);
		this.metaBlocksForDictMap = new ArrayList<>(newMetaBlocksForDictMap);
		this.metaBlocksForFreeList = new ArrayList<>(newMetaBlocksForFreeList);
		
		
	}
	
	private List<Integer> logicalToDiskAddrs(List<Integer> addrs) {
		List<Integer> result = new ArrayList<>();
		for (Integer addr : addrs) {
			int diskAddr = this.dictMap.get(addr);
			result.add(diskAddr);
		}
		return result;
	}
	
	private static void checkCommit(BlockIO blockIO, int expectedMaxBlocks, Set<Integer> expectedFreeBlocks, Map<Integer, Integer> expectedDictMap) {
		
		BlockStore newBlockStore = new BlockStore(blockIO);
		Assert.assertEquals(expectedMaxBlocks, newBlockStore.maxBlocks);
		Assert.assertEquals(expectedFreeBlocks, newBlockStore.freeBlocks);
		Assert.assertEquals(expectedDictMap, newBlockStore.dictMap);
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
			result.add(newLogicalBlock());
			count--;
		}
		return result;
	}
	
	private int newLogicalBlock() {
		
		int allocBlock = newDiskBlock();

		this.freeBlocks.remove(allocBlock);
		this.safeFreeBlocks.remove(allocBlock);
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
		this.freeBlocks.remove(newDiskAddr);
		this.safeFreeBlocks.remove(newDiskAddr);
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
			throw new NoSuchElementException("Addr " + i + " does not exists.");
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
		
		int diskAddr = safeRemoveBlock(i);
		if (diskAddr >= this.prevMaxBlocks) {
			this.safeFreeBlocks.add(diskAddr);
		}
		this.changed = true;
	}
	
	/**
	 * Remove block without changing the safeFreeList;
	 * @param i
	 * @return
	 */
	private int safeRemoveBlock(int i) {
		Integer diskAddr = this.dictMap.get(i);
		if (diskAddr == null || diskAddr == 0) {
			throw new NoSuchElementException();
		}

		this.dictMap.remove(i);
		this.freeBlocks.add(diskAddr);
		return diskAddr;
	}
	
	/**
	 * 
	 * Commit change
	 * 
	 */
	public void commit() {
		if (changed) {
			writeMetaData();
			blockIO.flush();			
			changed = false;
		}
	}
	
	public void rollback() {
		if (changed) {
			blockIO.flush();
			this.dictMap = new HashMap<>(this.prevDictMap);
			this.freeBlocks = new HashSet<>(prevFreeBlocks);
			this.maxBlocks = this.prevMaxBlocks;
			this.safeFreeBlocks = new HashSet<>(this.prevFreeBlocks);
			
			changed = false;
		}
	}
	
	private static List<Integer> loadIntArray(BlockIO blockIO, int addr, Collection<Integer> metaBlocks) {
		List<Integer> result = new ArrayList<>();
		byte[] buf = new byte[blockIO.blocksize()];
		
		while (addr > 0) {
			metaBlocks.add(addr);
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
		
		int arrayIndex = 0;
		
		for (int i = 0; i < blocks.size(); i++) {
			int b = blocks.get(i);
			byte[] buf = new byte[blockIO.blocksize()];
			ByteBuffer bb = ByteBuffer.wrap(buf);
			bb.putInt(0); // temp size; will be changed.
			bb.putInt( i < blocks.size() - 1 ? blocks.get(i+1) : 0 );
			
			int count = 0;
			while (bb.remaining() >= 4 && arrayIndex < array.size()) {
				bb.putInt(array.get(arrayIndex++));
				count++;
			}
			
			bb.putInt(0, count);
			blockIO.writeBlock(b, buf);
		}

		if (arrayIndex < array.size()) {
			throw new IllegalStateException("Some values are not stored from index: " + arrayIndex);
		}
		
		return blocks.size() == 0 ? 0 : blocks.get(0);
	}
	
	/**
	 * return number of blocks stored on this blockstore (this already remove the overhead)
	 * @return
	 */
	public int itemsStored() {
		return this.dictMap.size() - this.metaBlocksForDictMap.size() - this.metaBlocksForFreeList.size();
	}
	

	

	public static void main(String ... strings) throws UnsupportedEncodingException {

		
		InMemoryBlockIO blockIO = new InMemoryBlockIO(4096); // 4 int
		BlockStore blockStore = new BlockStore(blockIO);

		Map<Integer, Integer> map = new HashMap<>();
		int seq = 1;
		
		byte[] buf = new byte[blockIO.blocksize()];
		ByteBuffer bb = ByteBuffer.wrap(buf);
		
		Random random = new Random(100);
		Random randomFlush = new Random(7);
		for (int i = 0; i < 10000; i++) {
			
			if (random.nextInt(4) == 1 && map.size() > 0) {
				int addr = map.keySet().iterator().next();
				blockStore.freeBlock(addr);
				map.remove(addr);
//				System.out.println("Freeing " + addr + ". Free: " + blockStore.freeBlocks.size());
			}
			else {
				Arrays.fill(buf, (byte)0);
				bb.position(0);
				bb.putInt(++seq);
				int addr = blockStore.placeBlock(buf);
				map.put(addr, seq);
				int diskAddr = blockStore.dictMap.get(addr);
				if (map.size() != blockStore.itemsStored()) {
					throw new IllegalStateException("Mismatched: " + map.size() + " vs. " + blockStore.itemsStored());
				}
//				System.out.println("Puting " + addr + ". Value: " + seq + ". Size: " + map.size() + ". Free: " + blockStore.freeBlocks.size() + ". Map: " + blockStore.dictMap.size() + ". Max: " + blockStore.maxBlocks + ". Overhead free: " + blockStore.metaBlocksForFreeList.size() + ". Overhead dict: " + blockStore.metaBlocksForDictMap.size());
				
			}
			
			for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
				blockStore.readBlock(entry.getKey(), buf);
				bb.position(0);
				int value= bb.getInt();
				if (value != entry.getValue()) {
					throw new IllegalStateException("Read " + entry.getKey() + ". Get " + value);
				}
			}
			
			if (randomFlush.nextInt(10) == 1) {
				blockStore.commit();
//				System.out.println("Commit. Free: " + blockStore.freeBlocks.size() + ". Map: " + blockStore.dictMap.size());
				BlockStore.checkCommit(blockIO, blockStore.maxBlocks, blockStore.freeBlocks, blockStore.dictMap);
				
//				blockStore = new BlockStore(blockIO);
			}
			
		}
		System.out.println("Size: " + map.size() + ". Free: " + blockStore.freeBlocks.size() + ". Map: " + blockStore.dictMap.size() + ". Max: " + blockStore.maxBlocks + ". Overhead free: " + blockStore.metaBlocksForFreeList.size() + ". Overhead dict: " + blockStore.metaBlocksForDictMap.size());

	}
	
	
	
	public static class TestCase {
		
		@Test
		public void storeArray() {
			
			InMemoryBlockIO blockIO = new InMemoryBlockIO(4 * 4); // 4 int
			List<Integer> array = Arrays.asList(101, 102, 103, 104, 105, 106, 107);
			
			int i = storeIntArray(blockIO, array, Arrays.asList(1, 2, 3, 4));
			System.out.println(i);
			
			List<Integer> array2 = loadIntArray(blockIO, i, new HashSet<Integer>());
			Collections.sort(array2);
			
			Assert.assertEquals(array, array2);
			
		}
		
		
		
	}
	
}

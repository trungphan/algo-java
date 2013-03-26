package net.tp.algo.tree;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Comparator;

import net.tp.algo.util.NaturalComparator;

public class BTree<K> {

	private BlockStore bs;
	private Serializer<K> keySerializer;
	private Comparator<? super K> comparator;
	private BNode<K> root;
	
	private final int innerOrder;
	private final int innerLowWaterMark;
	private final int leafOrder;
	private final int leafLowWaterMark;

	
	// ref JDBM3, LevelDB,
	// Paper D. Comer, "The Ubiquitous B-Tree".
	// http://www.slac.stanford.edu/comp/unix/package/cernroot/22309/src/TBtree.cxx.html
	// http://www.cs.jhu.edu/~nishu/project_reports/Blockstore.pdf
	// http://webhdd.ru/library/files/WAFL.pdf
	// http://www.cs.berkeley.edu/~kamil/teaching/su02/080802.pdf (Threaded Tree)
	public BTree(BlockIO blockIO, Serializer<K> keySerializer, Comparator<? super K> comparator, int innerOrder, int innerLowWaterMark, int leafOrder, int leafLowWaterMark) {
		this.bs = new BlockStore(blockIO);
		this.keySerializer = keySerializer;
		this.comparator = comparator;
		

		this.innerOrder = innerOrder;
		this.innerLowWaterMark = innerLowWaterMark;
		this.leafOrder = leafOrder;
		this.leafLowWaterMark = leafLowWaterMark;
		
		readMetaData();
	}
	
	public int size() {
		return root != null ? root.size() : 0;
	}
	
	private void readMetaData() {
		
		byte[] buf = new byte[bs.blocksize()];
		
		if (bs.hasBlock(1)) {
			bs.readBlock(1, buf);
		}
		else {
			int i = bs.placeBlock(buf);
			if (i != 1) {
				throw new IllegalStateException("DataStore is corrupted.");
			}
			bs.flush();
		}
		
		ByteBuffer bb = ByteBuffer.wrap(buf);
		int ri = bb.getInt(); // rootIndex;
		if (ri > 0) {
			root = new BNode<K>(this, ri);
		}
	}
	
	private void writeMetaData() {
		byte[] buf = new byte[bs.blocksize()];
		ByteBuffer bb = ByteBuffer.wrap(buf);
		bb.putInt(root != null ? root.blockAddr : 0);
		bs.writeBlock(1, buf);
	}
	
	
	public boolean add(K key) {
		
		if (root == null) {
			root = new BNode<>(this, true); // create a leaf node for root
			root.insertKey(0, key);
			root.persistNew();
			writeMetaData();
			bs.flush();
			return true;
		}
		else {
			// using top down methodology
			
			BNode<K> u = root;
			BNode<K> parent = null;
			while (true) {

				if (u.full()) {
					handleOverflow(u, parent, key);
				}
				
				int pos = Arrays.binarySearch(u.keys, 0, u.last, key, this.comparator);
				if (pos >= 0) {
					bs.flush(); // there might be some update before
					return false; // key already exist
				}
				pos = -1 - pos; // pos now becomes the insertion point
				
				if (u.isLeaf) {
					u.insertKey(pos, key);
					u.persist();
					bs.flush();
					return true;
				}
				else {
					parent = u;
					u = u.getChild(pos);
				}
			}
		}
	}
	
	public boolean delete(K key) {
		if (root == null) {
			return false;
		}
		else {
			BNode<K> u = root;
			BNode<K> parent = null;
			BNode<K> found = null;
			
			while (true) {
				if (u.low() || (u == root && u.keysSize() == 1)) {
					handleUnderflow(u, parent, key);
				}

				if (found == null) {
					int pos = Arrays.binarySearch(u.keys, 0, u.last, key, this.comparator);
					if (pos >= 0) {
						found = u;
						if (u.isLeaf) {
							u.deleteKey(pos);
							u.persist();
							if (u == root) {
								writeMetaData();
							}
							bs.flush();
							return true;
						}
						else {
							u = u.getChild(pos + 1);
						}
					}
					else {
						pos = -1 - pos;
						u = u.getChild(pos);
						continue;
					}
				}				
				else if (!u.isLeaf) {
					u = u.getChild(0); // keep going to the left to find the smallest
				}
				else { // u is leaf
					int pos = Arrays.binarySearch(found.keys, 0, found.last, key, this.comparator);
					found.keys[pos] = u.keys[0];
					found.persist();
					if (found == root) {
						writeMetaData();
					}
					
					u.deleteKey(0);
					u.persist();
					bs.flush();
					return true;
				}
				
			}
		}
	}
	
	private void handleUnderflow(BNode<K> u, BNode<K> parent, K key) {
	}

	private void handleOverflow(BNode<K> u, BNode<K> parent, K key) {
		// TODO delay splitting
		split(u, parent, key);
	}

	private void split(BNode<K> u, BNode<K> parent, K key) {
		int mid = (u.last - 1) / 2;
		K kmid = u.keys[mid];
		BNode<K> s = u.split(mid);
		s.persistNew();
		
		if (parent != null) {
			int pos = Arrays.binarySearch(parent.keys, 0, parent.last, key, comparator);
			parent.insertKey(pos, kmid);
			parent.children[pos] = s.blockAddr;
			parent.persist();
		}
		else {
			BNode<K> newRoot = new BNode<>(this, false);
			newRoot.insertKey(0, kmid);
			newRoot.children[0] = root.blockAddr;
			newRoot.children[1] = s.blockAddr;
			newRoot.persistNew();
			root = newRoot;
			writeMetaData();
		}
	}
	
	public K find(K key) {
		
		if (root == null) {
			return null;
		}
		else {
			BNode<K> u = root;
			while (true) {
				int pos = Arrays.binarySearch(u.keys, 0, u.last, key, this.comparator);
				if (pos >= 0) {
					return u.keys[pos];
				}
				pos = -pos - 1;
				
				if (u.isLeaf) {
					return null;
				}
				else {
					u = u.getChild(pos);
				}
			}
		}		
		
	}
	
	
	public void printTree() {
		System.out.println("Tree ....");
		printTree(root);
	}
	
	private void printTree(BNode<K> u) {
		
		if (u == null) {
			System.out.println("null");
			return;
		}

		System.out.print("Index: " + u.blockAddr);
		System.out.print(". Keys : " + Arrays.toString(Arrays.copyOf(u.keys, u.last)));
		if (!u.isLeaf) {
			System.out.print(". Child: " + Arrays.toString(Arrays.copyOf(u.children, u.last + 1)));
		}
		
		System.out.println();
		
		if (u.isLeaf) {
			return;
		}
		
		for (int i = 0; i < u.keysSize() + 1; i++) {
			printTree(u.getChild(i));
		}
		
	}
	
	public static void main(String ... args) throws IOException {
//		BTree btree = new BTree(new FileBackedBlockIO(new File("Test.bin"), 4096));
		// build 2-3-4 tree
		BTree<Integer> btree = new BTree<Integer>(new InMemoryBlockIO(4 * 100), new IntegerSerializer(), new NaturalComparator<Integer>(), 4, 2, 4, 2);

		btree.add(20);
		btree.add(30);
		btree.add(50);
		btree.add(1);
		btree.add(3);
		btree.add(5);
		btree.printTree();

		btree.delete(20);
		btree.printTree();
		
//		System.out.println(btree.find(0));
//		System.out.println(btree.find(1));
//		System.out.println(btree.find(2));
//		System.out.println(btree.find(3));
//		System.out.println(btree.find(4));
//		System.out.println(btree.find(5));
//		System.out.println(btree.find(6));
//		System.out.println(btree.find(20));
//		System.out.println(btree.find(21));
//		System.out.println(btree.find(30));
//		System.out.println(btree.find(31));
//		System.out.println(btree.find(50));
//		System.out.println(btree.find(51));
		
	}
	
	
	
	
	private static class BNode<K> {
		
		private transient int blockAddr;
		
		private int last;
		private K[] keys;
		private int[] children;
		private int[] subtreeSizes;
		private boolean isLeaf;
		private final transient BTree<K> btree;
		
		public BNode(BTree<K> btree, boolean isLeaf) {
			this.btree = btree;
			this.isLeaf = isLeaf;
			this.last = 0;
			this.keys = (K[])(isLeaf ? new Object[btree.leafOrder] : new Object[btree.innerOrder]);
			this.children = isLeaf ? null : new int[this.keys.length + 1];
			this.subtreeSizes = isLeaf ? null : new int[this.keys.length + 1];
		}
		
		public int keysSize() {
			return last;
		}
		
		public int size() {
			if (isLeaf) {
				return last;
			}
			else {
				int sum = last;
				for (int i = 0; i < last; i++) {
					sum += subtreeSizes[i];
				}
				return sum;
			}
		}
		
		public BNode<K> getChild(int pos) {
			if (isLeaf || pos >= children.length || pos < 0) {
				throw new IndexOutOfBoundsException();
			}
			return new BNode<K>(btree, children[pos]);
		}
		
		public BNode(BTree<K> btree, int blockAddr) {
			this.btree = btree;
			this.blockAddr = blockAddr;
			
			ByteBuffer bb = ByteBuffer.wrap(btree.bs.readBlock(blockAddr));
			int checksum = bb.getInt();
			if (checksum != 154) {
				throw new IllegalStateException("Data Store is corrupted");
			}
			this.isLeaf = bb.get() != 0;
			this.last = bb.getInt();
			this.keys = (K[])(isLeaf ? new Object[btree.leafOrder] : new Object[btree.innerOrder]);
			this.children = isLeaf ? null : new int[this.keys.length + 1];
			this.subtreeSizes = isLeaf ? null : new int[this.keys.length + 1];
			
			Serializer<K> keySerializer = btree.keySerializer;
			for (int i = 0; i < keys.length; i++) {
				K k = keySerializer.read(bb);
				if (i < last) {
					this.keys[i] = k;
				}
			}
			
			if (!isLeaf) {
				for (int i = 0; i < this.children.length; i++) {
					int ci = bb.getInt();
					int csz = bb.getInt();
					if (i < last + 1) {
						this.children[i] = ci;
						this.subtreeSizes[i] = csz;
					}
				}
			}
			

		}
		
		private byte[] toBytes() {
			byte[] buf = new byte[this.btree.bs.blocksize()];
			ByteBuffer bb = ByteBuffer.wrap(buf);
			
			int checksum = 154;
			bb.putInt(checksum);
			bb.put(isLeaf ? (byte)1 : (byte)0);
			bb.putInt(last);
			
			Serializer<K> objectIO = this.btree.keySerializer;
			for (int i = 0; i < keys.length; i++) {
				objectIO.write(bb, i < last ? (K)this.keys[i] : null);
			}
			if (!isLeaf) {
				for (int i = 0; i < this.children.length; i++) {
					bb.putInt(i < last + 1 ? this.children[i] : 0);
					bb.putInt(i < last + 1 ? this.subtreeSizes[i] : 0);
				}
			}
			
			return buf;
		}
		
		public int persistNew() {
			if (this.blockAddr != 0) {
				throw new IllegalStateException();
			}
			this.blockAddr = btree.bs.placeBlock(toBytes());
			return blockAddr;
		}
		
		public void persist() {
			if (this.blockAddr <= 0) {
				throw new IllegalStateException();
			}
			btree.bs.writeBlock(this.blockAddr, toBytes());
		}
				
		public void insertKey(int index, K key) {
			if (index < 0 || index > last) {
				throw new IndexOutOfBoundsException();
			}
			
			last++;
			for (int i = last - 1; i > index; i--) {
				this.keys[i] = this.keys[i-1];
			}
			if (!isLeaf) {
				for (int i = last; i > index; i--) {
					this.children[i] = this.children[i-1];
					this.subtreeSizes[i] = this.subtreeSizes[i-1];
				}
			}
			
			this.keys[index] = key;
		}
		
		public void deleteKey(int index) {
			if (index < 0 || index >= last) {
				throw new IndexOutOfBoundsException();
			}

			for (int i = index; i < last - 1; i++) {
				this.keys[i] = this.keys[i+1];
			}
			if (!isLeaf) {
				for (int i = index; i < last; i++) {
					this.children[i] = this.children[i+1];
					this.subtreeSizes[i] = this.subtreeSizes[i+1];
				}
			}
			last--;
		}
		
		public boolean full() {
			return last == keys.length;
		}
		
		public boolean low() {
			return last <= (isLeaf ? btree.leafLowWaterMark : btree.innerLowWaterMark);
		}
		
		
		public BNode<K> split(int mid) {
			
			BNode<K> newSibling = new BNode<>(btree, isLeaf);
			newSibling.last = last - mid - 1;
			last = mid;
			System.arraycopy(keys, mid + 1, newSibling.keys, 0, keys.length - mid - 1);
			Arrays.fill(keys, last, keys.length, 0);
			if (!isLeaf) {
				System.arraycopy(children, mid, newSibling.children, 0, children.length - mid);
				System.arraycopy(subtreeSizes, mid, newSibling.subtreeSizes, 0, subtreeSizes.length - mid);
				
				Arrays.fill(children, last + 1, children.length - last - 1, 0);
				Arrays.fill(subtreeSizes, last + 1, subtreeSizes.length - last - 1, 0);
			}
			
			return newSibling;
		}
		
		
	}	
	
}

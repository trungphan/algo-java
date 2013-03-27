package net.tp.algo.btree;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;

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
			bs.commit();
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
			bs.commit();
			return true;
		}
		else {
			// using top down methodology
			
			BNode<K> u = root;
			BNode<K> parent = null;
			while (true) {
				
				int pos = Arrays.binarySearch(u.keys, 0, u.last, key, this.comparator);
				if (pos >= 0) {
					bs.commit(); // there might be some update before
					return false; // key already exist
				}

				if (u.full()) {
					u = handleOverflow(u, parent, key);
					// update pos as elements would have changed.
					// pos will not be >= 0 because it's checked before that key does not belong to u
					pos = Arrays.binarySearch(u.keys, 0, u.last, key, this.comparator);
				}
				
				pos = -1 - pos; // pos now becomes the insertion point
				
				if (u.isLeaf) {
					u.insertKey(pos, key);
					u.persist();
					bs.commit();
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

		BNode<K> u = root;
		BNode<K> parent = null;
		BNode<K> found = null;
		int foundPos = 0;
		int pos = 0;
		
		while (true) {
			if (u.low() || (u == root && u.keysSize() == 1)) {
				handleUnderflow(u, parent, key);
			}
			
			pos = Arrays.binarySearch(u.keys, 0, u.last, key, this.comparator);
			if (pos >= 0) {
				found = u;
				foundPos = pos;
				if (!u.isLeaf) {
					pos++;
				}
			}
			else if (found != null) {
				pos = 0;
			}
			else {
				pos = -1 - pos;
			}
			
			if (u.isLeaf) {
				if (found == null) {
					bs.commit();
					return false;
				}
				else {
					if (found != u) {
						found.keys[foundPos] = u.keys[pos];
						found.persist();
					}
					
					u.deleteKey(pos, true);
					u.persist();
					if (found == root || u == root) {
						writeMetaData();
					}
					bs.commit();
					return true;
				}
			}
		
			parent = u;
			u = u.getChild(pos);
			
		}

	}
	
	private BNode<K> handleUnderflow(BNode<K> u, BNode<K> parent, K key) {
		
		BNode<K> leftChild = null;
		BNode<K> rightChild = null;
		
		if (parent == null) {
			if (!u.isLeaf && u.last == 1) {
				leftChild = u.getChild(0);
				rightChild = u.getChild(1);
				
				if (leftChild.low() && rightChild.low()) {
					merge(u, leftChild, rightChild, 0, 0);
					leftChild.delete();
					rightChild.delete();
					root.persist();
					writeMetaData();
				}
			}
			
			return u;
		}
		
		int pos = Arrays.binarySearch(parent.keys, 0, parent.last, key, comparator);
		int leftPos = pos >= 0 ? pos : -1 - pos -1;
		int rightPos = pos >= 0 ? pos + 2 : -1 -pos + 1;
		if (pos < 0) {
			pos = -1 - pos;
		}
		
		if (leftPos >= 0) {
			leftChild = parent.getChild(leftPos);
			if (!leftChild.low()) {
				rotateRight(parent, leftChild, u, pos);
				leftChild.persist();
				u.persist();
				parent.persist();
				if (parent == root) {
					writeMetaData();
				}
				return u;
			}
			
		}
		
		if (rightPos <= parent.last) {
			rightChild = parent.getChild(rightPos);
			if (!rightChild.low()) {
				rotateLeft(parent, u, rightChild, pos);
				rightChild.persist();
				u.persist();
				parent.persist();
				if (parent == root) {
					writeMetaData();
				}
				return u;
			}
		}
		
		if (leftChild != null) {
			merge(parent, leftChild, u, leftPos, 2);
			leftChild.delete();
			u.persist();
			parent.persist();
			if (parent == root) {
				writeMetaData();
			}
		}
		else {
			merge(parent, u, rightChild, rightPos, 1);
			rightChild.delete();
			u.persist();
			parent.persist();
			if (parent == root) {
				writeMetaData();
			}
		}
		
		return u;
	}
	
	private static <K> void rotateRight(BNode<K> parent, BNode<K> left, BNode<K> right, int pos) {
		right.insertKey(0, parent.keys[pos]);
		if (!right.isLeaf) {
			right.children[0] = left.children[left.last];
			right.subtreeSizes[0] = left.subtreeSizes[left.last];
		}
		parent.keys[pos] = left.keys[left.last-1];
		left.deleteKey(left.last - 1, false);
	}
	
	private static <K> void rotateLeft(BNode<K> parent, BNode<K> left, BNode<K> right, int pos) {
		left.insertKey(left.last, parent.keys[pos]);
		if (!left.isLeaf) {
			left.children[left.last] = right.children[0];
			left.subtreeSizes[left.last] = right.subtreeSizes[0];			
		}
		parent.keys[pos] = right.keys[0];
		right.deleteKey(0, true);
	}
	
	/**
	 * Keep right child, merge the left child to right child
	 * @param parent
	 * @param left
	 * @param right
	 * @param pos
	 * @param opts 0: keep root, 1: keep left, 2: keep right
	 */
	private static <K> void merge(BNode<K> parent, BNode<K> left, BNode<K> right, int pos, int opts) {
		int newLast = left.last + 1 + right.last;
		K[] newKeys = Arrays.copyOf(left.keys, left.keys.length);
		int[] newChildren = left.isLeaf ? null : Arrays.copyOf(left.children, left.children.length);
		int[] newSubtreeSizes = left.isLeaf ? null : Arrays.copyOf(left.subtreeSizes, left.subtreeSizes.length);
		
		newKeys[left.last] = parent.keys[pos];
		System.arraycopy(right.keys, 0, newKeys, left.last + 1, right.last);
		if (!left.isLeaf) {
			System.arraycopy(right.children, 0, newChildren, left.last + 1, right.last + 1);
			System.arraycopy(right.subtreeSizes, 0, newSubtreeSizes, left.last + 1, right.last + 1);
		}
		
		switch (opts) {
		case 1: // keep left
			parent.deleteKey(pos, false);
			left.keys = newKeys;
			left.children = newChildren;
			left.subtreeSizes = newSubtreeSizes;
			left.last = newLast;
			break;
		case 2: // keep right
			parent.deleteKey(pos, true);
			right.keys = newKeys;
			right.children = newChildren;
			right.subtreeSizes = newSubtreeSizes;
			right.last = newLast;
			break;
		default: // keep parent
			parent.keys = newKeys;
			parent.isLeaf = left.isLeaf;
			parent.children = newChildren;
			parent.subtreeSizes = newSubtreeSizes;
			parent.last = newLast;
			break;
		}
		
	}

	private BNode<K> handleOverflow(BNode<K> u, BNode<K> parent, K searchKey) {
		
		if (parent != null) {
			BNode<K> leftChild = null;
			BNode<K> rightChild = null;
			
			int pos = Arrays.binarySearch(parent.keys, 0, parent.last, searchKey, comparator);
			int leftPos = pos >= 0 ? pos : -1 - pos -1;
			int rightPos = pos >= 0 ? pos + 2 : -1 -pos + 1;
			if (pos < 0) {
				pos = -1 - pos;
			}
			
			if (leftPos >= 0) {
				leftChild = parent.getChild(leftPos);
				if (!leftChild.full()) {
					rotateLeft(parent, leftChild, u, pos - 1);
					leftChild.persist();
					u.persist();
					parent.persist();
					if (parent == root) {
						writeMetaData();
					}
					return u;
				}
				
			}
			
			if (rightPos <= parent.last) {
				rightChild = parent.getChild(rightPos);
				if (!rightChild.full()) {
					rotateRight(parent, u, rightChild, pos);
					rightChild.persist();
					u.persist();
					parent.persist();
					if (parent == root) {
						writeMetaData();
					}
					return u;
				}
			}
			
		}
		
		return split(u, parent, searchKey);
	}

	private BNode<K> split(BNode<K> u, BNode<K> parent, K searchKey) {
		
		int searchPos = Arrays.binarySearch(u.keys, 0, u.last, searchKey, comparator);
		if (searchPos < 0) {
			searchPos = -1 - searchPos;
		}
		
		int mid = (u.last - 1) / 2;
		K kmid = u.keys[mid];
		BNode<K> s = u.split(mid);
		s.persistNew();
		u.persist();
		
		if (parent != null) {
			int pos = Arrays.binarySearch(parent.keys, 0, parent.last, kmid, comparator);
			if (pos < 0) {
				pos = -1 - pos;
			}
			parent.insertKey(pos, kmid);
			parent.children[pos+1] = s.blockAddr;
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
		
		return searchPos > mid ? s : u;
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
		
		if (root == null) {
			System.out.println("Empty");
		}
		
		Queue<BNode<K>> queue = new LinkedList<>();
		queue.add(root);
		while (!queue.isEmpty()) {
			BNode<K> u = queue.poll();
			if (u == null) {
				System.out.println();
			}
			else {
				System.out.print( Arrays.toString(Arrays.copyOf(u.keys, u.keysSize() )));
				System.out.print(" ");
				
				if (!u.isLeaf) {
					queue.add(null);
					for (int i = 0; i < u.keysSize() + 1; i++) {
						queue.add(u.getChild(i));
					}
				}
			}
			
		}
		System.out.println();
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		
		if (root == null) {
			result.append("Empty");
		}
		
		Queue<BNode<K>> queue = new LinkedList<>();
		queue.add(root);
		while (!queue.isEmpty()) {
			BNode<K> u = queue.poll();
			if (u == null) {
				result.append("\n");
			}
			else {
				result.append(Arrays.toString(Arrays.copyOf(u.keys, u.keysSize() ))).append(" ");
				
				if (!u.isLeaf) {
					queue.add(null);
					for (int i = 0; i < u.keysSize() + 1; i++) {
						queue.add(u.getChild(i));
					}
				}
			}
			
		}
		
		return result.toString();
	}
	
	public static void main(String ... args) throws IOException {
		
		
		
		
		// build 2-3-4 tree
		InMemoryBlockIO blockIO = new InMemoryBlockIO(100 * 4);
		BTree<Integer> btree = new BTree<Integer>(blockIO, new IntegerSerializer(), new NaturalComparator<Integer>(), 5, 2, 5, 2);
//		BTree<Integer> btree = new BTree<Integer>(new FileBackedBlockIO(new File("Test.bin"), 4096), new IntegerSerializer(), new NaturalComparator<Integer>(), 4, 2, 4, 2);

		btree.add(20);
		btree.add(30);
		btree.add(50);
		btree.add(1);
		btree.add(3);
		btree.add(5);

		btree.delete(20);
		
		btree.delete(5);
		btree.printTree();
		
		btree.add(5);		
		btree.printTree();
		btree.add(6);
		btree.printTree();
		
		try (Scanner scanner = new Scanner(System.in)) {
			while (true) {
				System.out.print("> ");
				String fullCommand = scanner.nextLine().trim().toLowerCase();
				
				String[] tokens = fullCommand.split(" +");
				String command = tokens[0];
				switch (command) {
				case "quit":
				case "exit":
				case "bye":
					System.out.println("Bye!");
					return;
				case "add": {
					int count = 0;
					for (String token : tokens) {
						try {
							if (btree.add(Integer.parseInt(token))) {
								count++;
							}
						} catch (NumberFormatException e) {}
					}
					System.out.println("Add " + count + " items.");
					break;
				}
				case "delete": {
					int count = 0;
					for (String token : tokens) {
						try {
							if (btree.delete(Integer.parseInt(token))) {
								count++;
							}
						} catch (NumberFormatException e) {}
					}
					System.out.println("Delete " + count + " items.");
					break;
				}
				case "find": {
					boolean found = false;
					try {
						found = tokens.length > 1 && (btree.find(Integer.parseInt(tokens[1])) != null);
					} catch (NumberFormatException e) {}
					System.out.println(found ? "Key found." : "Key not found.");
					break;
				}
				case "print": {
					btree.printTree();
					break;
				}
				default:
					System.out.println("Unknown command");
					break;
				}
			}
		}
		
		
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
		
		public void delete() {
			if (this.blockAddr == 0) {
				throw new IllegalStateException();
			}
			btree.bs.freeBlock(this.blockAddr);
			this.blockAddr = 0;
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
		
		public void deleteKey(int index, boolean removeLeftTree) {
			if (index < 0 || index >= last) {
				throw new IndexOutOfBoundsException();
			}

			for (int i = index; i < last - 1; i++) {
				this.keys[i] = this.keys[i+1];
			}
			if (!isLeaf) {
				if (removeLeftTree) {
					for (int i = index; i < last; i++) {
						this.children[i] = this.children[i+1];
						this.subtreeSizes[i] = this.subtreeSizes[i+1];
					}
				}
				else {
					for (int i = index + 1; i < last; i++) {
						this.children[i] = this.children[i+1];
						this.subtreeSizes[i] = this.subtreeSizes[i+1];
					}
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
		
		@Override
		public String toString() {
			return Arrays.toString(Arrays.copyOf(keys, last));
		}
		
	}	
	
}

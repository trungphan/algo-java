package net.tp.algo.tree;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class BTree {

	private int n;
	private BlockStore bs;
	private int ri;
	private transient byte[] buf;
	
	public BTree(File backedStorage) {
		
		this.bs = new BlockStore(backedStorage);
		this.buf = new byte[4096];
		readMetaData();
	}
	
	private void readMetaData() {
		bs.readBlock(1, buf);
		ByteBuffer bb = ByteBuffer.wrap(buf);
		n = bb.getInt();
		ri = bb.getInt();
		if (n == 0 && ri == 0) {
			ri = -1;
			bb.putInt(ri);
			bs.placeBlock(buf);
		}
	}
	
	private void writeMetaData() {
		Arrays.fill(buf, (byte)0);
		ByteBuffer bb = ByteBuffer.wrap(buf);
		bb.putInt(n); bb.putInt(ri);
		bs.writeBlock(1, buf);
	}
	
	
	public void add(int k) {
		
		if (ri < 0) {
			BNode node = new BNode();
			node.add(k,  -1);
			ri = bs.placeBlock(node.toBytes());
			n++;
			writeMetaData();
		}
		else {
			int i = ri;
			BNode node = getNode(i);
			
			
		}
		
		
		
		
	}
	
    public int find(int x) {
        int z = 0;
        int ui = ri;
        while (ui >= 0) {
            BNode u = getNode(ui);
            int i = findIt(u.keys, x);
            if (i < 0) return u.keys[-(i+1)]; // found it
            z = u.keys[i];
            ui = u.children[i];
        }
        return z;
    }
    
    private int findIt(int[] a, int x) {
        int lo = 0, hi = a.length;
        while (hi != lo) {
            int m = (hi+lo)/2;
            if (a[x] < a[m])
                hi = m;      // look in first half
            else if (a[x] > a[m])
                lo = m+1;    // look in second half
            else
                return -m-1; // found it
        }
        return lo;
    }    
	
	private BNode getNode(int i) {
		bs.readBlock(i, buf);
		BNode node = new BNode(buf);
		return node;
	}
	
	public void close() {
		bs.close();
	}
	
	
	public static void main(String ... args) {
		BTree btree = new BTree(new File("Test.bin"));

		btree.add(20);
		
		btree.close();
		
	}
	
	
	
	
	private static class BNode {
		
		private int size;
		private int[] keys = new int[256];
		private int[] children = new int[256]; // -1 means no children
		
		public BNode() {
			
		}
		
		public BNode(byte[] bytes) {
			
			ByteBuffer bb = ByteBuffer.wrap(bytes);

			for (int i = 0; i < 256; i++) {
				int ci = bb.getInt();
				if (ci < -1) {
					break;
				}
				size++;
				children[i] = ci;
			}
			bb.position(256 * 4);
			for (int i = 0; i < size; i++) {
				keys[i] = bb.getInt();
			}
		}
		
		public byte[] toBytes() {
			
			Arrays.fill(keys, size, 256-size, -2);
			Arrays.fill(children, size, 256 - size, -2);
			
			byte[] result = new byte[4096];
			ByteBuffer bb = ByteBuffer.wrap(result);
			for (int i = 0; i < 256; i++) {
				bb.putInt(children[i]);
			}
			for (int i = 0; i < 256; i++) {
				bb.putInt(keys[i]);
			}
			return result;
		}

		
		public void add(int k, int ci) {
			size++;
			keys[size - 1] = k;
			children[size - 1] = ci;
		}
		
		public boolean full() {
			return size == keys.length;
		}
	}	
	
}

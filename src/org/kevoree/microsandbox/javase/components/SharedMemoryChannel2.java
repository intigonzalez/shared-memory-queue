package org.kevoree.microsandbox.javase.components;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: inti
 * Date: 1/17/14
 * Time: 2:06 AM
 *
 */
public abstract class SharedMemoryChannel2 {

    static {
        System.load("/home/inti/IdeaProjects/TestingSharedMemoryIdea/src/libInterprocessLock.so");
    }

    protected static final int BLOCK_SIZE = 524288; // change the next one if you change this
    protected static final int LOG_2_BLOCK_SIZE = 19;
    private static final int NUMBER_OF_BLOCKS = 128;
    protected static final int MASK_NUMBER_OF_BLOCKS = NUMBER_OF_BLOCKS - 1;

    protected final InterprocessLock lock;

    protected final int size;
    private final FileChannel ch;
    protected final MappedByteBuffer mem;

    private static final int nbDesiredReaders_index = 0;
    private static final int BlockStartAddress_index = 4;

    private static final int NodeSize = BlockStartAddress_index + 4;
    private static final int LOG_NODE_SIZE = 3;

    protected static final int head_pos = 0;
    protected static final int tail_pos = head_pos + 4;
    private static final int first_node_descriptor = tail_pos + 4;
    private static final int end_node_descriptors = first_node_descriptor + NUMBER_OF_BLOCKS*NodeSize;
    protected static final int free_set_position = end_node_descriptors; // an stack of free blocks
    private static final int end_free_list = free_set_position + NUMBER_OF_BLOCKS*4 + 4;
    protected static final int BLOCKS_POSITION = ((end_free_list >> LOG_2_BLOCK_SIZE) << LOG_2_BLOCK_SIZE) + BLOCK_SIZE;

    class Node {

        Node(int index) {
            this.posDescriptor = (index<<LOG_NODE_SIZE) + first_node_descriptor;
        }

        Node(int index, int desiredReaders, int startAddr, int lastAddr) {
            this(index);
            mem.putInt(posDescriptor + nbDesiredReaders_index, desiredReaders);
            mem.putInt(posDescriptor + BlockStartAddress_index, startAddr);
            mem.putInt(startAddr, lastAddr);
        }

        private int posDescriptor;

        int getAddr() {
            return mem.getInt(posDescriptor + BlockStartAddress_index);
        }

        int getLastAddr() {
            int addr = getAddr();
            return mem.getInt(addr);
        }

        void incReaders() {
            int v = mem.getInt(posDescriptor + nbDesiredReaders_index) - 1;
            mem.putInt(posDescriptor + nbDesiredReaders_index, v);
        }

        boolean wasReadForAll() {
            return mem.getInt(posDescriptor + nbDesiredReaders_index) == 0;
        }

        void freeUsedBlocks() {
            int i ;
            Block b = new Block(getAddr(), getLastAddr());
            while (!b.isFinal()) {
                // calculate index
                i = (b.start - BLOCKS_POSITION) >> LOG_2_BLOCK_SIZE;
                addToFreeBlocks(i);
                b = b.nextBlock();
            }
            i = (b.start - BLOCKS_POSITION) >> LOG_2_BLOCK_SIZE;
            addToFreeBlocks(i);
        }
    }

    protected class Block {
        int start;
        int currentPos;
        int lastPos;

        public Block(int start, int size) {
            this.start = start;
            this.currentPos = start + 8; // one int for size of the block, one for next
            this.lastPos = currentPos + size;
            if (lastPos - start > BLOCK_SIZE) {
                lastPos -= (lastPos - start) - BLOCK_SIZE;
            }
        }

        public void setNext(Block next) {
            mem.putInt(start + 4, next.start);
        }

        public void saveLastAddr() {
            mem.putInt(start, lastPos);
        }

        public boolean isFinal() {
            return mem.getInt(start + 4) == -1;
        }

        public void markAsFinal() {
            mem.putInt(start + 4, -1);
        }

        public Block nextBlock() {
            int sNext = mem.getInt(start + 4);
            Block b = new Block(sNext, BLOCK_SIZE);
            b.lastPos = mem.getInt(b.start);
            return b;
        }

        public void calculateLastPosFromStoredInfo() {
            lastPos = mem.getInt(start);
        }
    }


    GlobalResource globalResource;

    protected SharedMemoryChannel2(String channelName) throws IOException {
        globalResource = new GlobalResource(channelName);
        boolean initialize = globalResource.hasToInitialize();
        RandomAccessFile randomAccessFile = new RandomAccessFile("/tmp/mapped" + channelName + ".txt", "rw");
        ch = randomAccessFile.getChannel();
        this.size = BLOCK_SIZE * NUMBER_OF_BLOCKS + BLOCKS_POSITION;
        mem = ch.map(FileChannel.MapMode.READ_WRITE,0, this.size);
//        initialize && !initialized
//        System.out.println("Most Initialize " + channelName + "? " + (initialize));
        lock = new InterprocessLock((channelName + "_lock").getBytes(), initialize);
        if (initialize) {
            mem.putInt(head_pos, 0);
            mem.putInt(tail_pos, 0);
            mem.putInt(free_set_position, 0);
            for (int index = 0 ; index < getQueueCapacity() ; index++)
                addToFreeBlocks(index);
        }
    }

    protected void advanceHead() {
        int head = (mem.getInt(head_pos) + 1) & MASK_NUMBER_OF_BLOCKS;
//        head %= NUMBER_OF_BLOCKS;
        mem.putInt(head_pos, head);
    }

    protected int advanceTail() {
        int tmp = mem.getInt(tail_pos) ;
        int tail = (tmp + 1) & MASK_NUMBER_OF_BLOCKS;
        mem.putInt(tail_pos, tail);
        return tmp;
    }

    protected final int getQueueCapacity() {
        return NUMBER_OF_BLOCKS;
    }

    protected void addToFreeBlocks(int v) {
        int top = mem.getInt(free_set_position);
        mem.putInt(free_set_position + 4 + (top<<2), v);
        mem.putInt(free_set_position, ++top);

    }

    protected boolean isEmptyStackOfEmptyBlock() {
        return mem.getInt(free_set_position) == 0;
    }

    protected int getEmptyBlockFromStack() {
        int top = mem.getInt(free_set_position) - 1;
        mem.putInt(free_set_position, top);
        return mem.getInt(free_set_position + 4 + (top<<2));
    }
}

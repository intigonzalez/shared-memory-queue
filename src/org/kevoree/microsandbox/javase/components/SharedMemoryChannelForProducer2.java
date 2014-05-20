package org.kevoree.microsandbox.javase.components;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

/**
 * Created with IntelliJ IDEA.
 * User: inti
 * Date: 1/17/14
 * Time: 1:57 PM
 * 
 */
public class SharedMemoryChannelForProducer2 extends SharedMemoryChannel2 {

    private final int nbConsumers;

    public SharedMemoryChannelForProducer2(String channelName, int nbConsumers) throws IOException {
        super(channelName);
        this.nbConsumers = nbConsumers;
    }

    public void notifySend(OutputStream s) {
        SharedMemoryOutputStream stream = (SharedMemoryOutputStream)s;
        try {
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            lock.lock();
            int tail = mem.getInt(tail_pos);
            tail = (tail + 1) & MASK_NUMBER_OF_BLOCKS;
            int head = mem.getInt(head_pos);
            while (tail == head) {
                lock.waitInterprocess2();
                tail = mem.getInt(tail_pos);
                tail = (tail + 1) & MASK_NUMBER_OF_BLOCKS;
                head = mem.getInt(head_pos);
            }
            int t = advanceTail();
            new Node(t, nbConsumers, stream.firstBlock.start, stream.firstBlock.lastPos);
            lock.broadcast();
        }
        finally {
            lock.unlock();
        }
    }

    public void notifySend(Pair p) {
        p.currentBlock.lastPos = p.bb.position();
        p.currentBlock.saveLastAddr();
        try {
            lock.lock();
            int tail = mem.getInt(tail_pos);
            tail = (tail + 1) & MASK_NUMBER_OF_BLOCKS;
            int head = mem.getInt(head_pos);
            while (tail == head) {
                lock.waitInterprocess2();
                tail = mem.getInt(tail_pos);
                tail = (tail + 1) & MASK_NUMBER_OF_BLOCKS;
                head = mem.getInt(head_pos);
            }
            int t = advanceTail();
//            System.out.println(p.currentBlock.start + " " + p.currentBlock.lastPos);
            new Node(t, nbConsumers, p.currentBlock.start, p.currentBlock.lastPos);
            lock.broadcast();
        }
        finally {
            lock.unlock();
        }
    }

    public OutputStream getStreamToSendMessage() {
        return new SharedMemoryOutputStream(mem);
    }

    private Block getFreeBlock() {
        int index = -1;
//        try {
//            lock.lock();
//            int counter = 0;
//            while (counter < 10 && isEmptyStackOfEmptyBlock()) {
//                lock.unlock();
//                Thread.yield();
//                counter++;
//                lock.lock();
//            }
//            if (counter < 10)
//                index = getEmptyBlockFromStack();
//        }
//        finally {
//            lock.unlock();
//        }
//        if (index == -1) {
            try {
                lock.lock();
                while (isEmptyStackOfEmptyBlock()) {
                    lock.waitInterprocess2();
                }
                index = getEmptyBlockFromStack();
            } finally {
                lock.unlock();
            }
//        }
        if (index != -1) {
            int p = BLOCKS_POSITION + (index << LOG_2_BLOCK_SIZE);
            Block block = new Block(p, BLOCK_SIZE);
            return block;
        }
        return null;
    }

    public Pair getBufferToSend() {
        return new Pair(mem);
    }


    private class SharedMemoryOutputStream extends OutputStream {

        private ByteBuffer writingTo;

        private int lastPos;

        Block currentBlock;
        Block firstBlock;

        private SharedMemoryOutputStream(MappedByteBuffer mem) {
            firstBlock = currentBlock = getFreeBlock();
            currentBlock.markAsFinal();
//            lastPos = currentBlock.lastPos;
            writingTo = mem.duplicate();
            writingTo.position(currentBlock.currentPos);
            writingTo.limit(currentBlock.lastPos);
        }

        @Override
        public void write(int b) throws IOException {
            if (!writingTo.hasRemaining()) {
                // I need a new block
                iNeedANewBlock();
            }
            writingTo.put((byte) b);
        }

        @Override
        public void write(byte[] b, int off, int l) throws IOException {
            // TODO : some additional checks, for instance: has the array that number of elements?
            int tmp = writingTo.remaining();
            if (l <= tmp) {
                writingTo.put(b,off,l);
            }
            else {
                writingTo.put(b, off, tmp);
                // I need a new block
                iNeedANewBlock();
                write(b, off + tmp, l - tmp);
            }
        }

        @Override
        public void flush() throws IOException {
            currentBlock.lastPos = writingTo.position();
            currentBlock.saveLastAddr();
        }

        @Override
        public void close() throws IOException {
            flush();
        }

        protected void iNeedANewBlock() {
            Block block = getFreeBlock();
            currentBlock.setNext(block);
            currentBlock.lastPos = writingTo.position();
            currentBlock.saveLastAddr();
            currentBlock = block;
            currentBlock.markAsFinal();
//            lastPos = currentBlock.lastPos;
            writingTo.position(currentBlock.currentPos);
            writingTo.limit(currentBlock.lastPos);
        }
    }

    public class Pair {
        private final Block currentBlock;
        public ByteBuffer bb;

        private Pair(MappedByteBuffer mem) {
            currentBlock = getFreeBlock();
            int size = currentBlock.lastPos - currentBlock.currentPos;
            currentBlock.markAsFinal();

            bb = mem.duplicate();
//            int p = mem.position();
            bb.position(currentBlock.currentPos); // Fixme, any access to position must be synchronized
            bb.limit(currentBlock.lastPos);
//            bb = mem.slice();
//            mem.position(p);
//            bb.limit(size);
        }
    }
}

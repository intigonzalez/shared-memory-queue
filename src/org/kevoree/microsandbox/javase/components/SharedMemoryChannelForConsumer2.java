package org.kevoree.microsandbox.javase.components;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

/**
 * Created with IntelliJ IDEA.
 * User: inti
 * Date: 1/17/14
 * Time: 1:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class SharedMemoryChannelForConsumer2 extends SharedMemoryChannel2 {

    private int local_head = 0;

    public SharedMemoryChannelForConsumer2(String channelName) throws IOException {
        super(channelName);
        try {
            lock.lock();
            local_head = mem.getInt(head_pos);
        }finally {
            lock.unlock();
        }
    }

    public int waitForMessage() {
        int n = local_head;
        try {
            lock.lock();
            int tail = mem.getInt(tail_pos);
            while (local_head == tail) {
                lock.waitInterprocess();
                tail = mem.getInt(tail_pos);
            }
            local_head = (local_head + 1) % getQueueCapacity();
        } finally {
            lock.unlock();
        }
        return n;
    }

    public void notifyReception(int id) {
        try {
            lock.lock();
            Node node = new Node(id);
            node.incReaders();
            if (node.wasReadForAll()) {
//                decreaseQueueSize();
                advanceHead();
                node.freeUsedBlocks();
                lock.broadcast2();
                Thread.yield();
            }
        } finally {
            lock.unlock();
        }
    }

    public InputStream getMessageInputStream(int id) {
        return new SharedMemoryInputStream(mem, id);
    }

    public ByteBuffer getByteBuffer(int id) {
        Block currentBlock = getFirstBlock(id);
        currentBlock.calculateLastPosFromStoredInfo();
        int currentPos = currentBlock.currentPos;
        int lastPos = currentBlock.lastPos;
//        int size = lastPos - currentPos;

//        System.out.println("lalala " + currentPos + " " + lastPos);

        ByteBuffer bb = mem.duplicate();
        bb.position(currentPos);
        bb.limit(lastPos);
//        bb = bb.slice();
//        try {
//            bb.limit(size);
//        }
//        catch (Exception e) {
//            System.out.println(bb.limit() + " " + size);
//        }
        return bb;
    }

    private class SharedMemoryInputStream extends InputStream {
        private int lastPos;

        Block currentBlock;

        ByteBuffer readingFrom;

        private SharedMemoryInputStream(MappedByteBuffer mem, int id) {
            currentBlock = getFirstBlock(id);
            lastPos = currentBlock.lastPos;
            readingFrom = mem.duplicate();
            readingFrom.position(currentBlock.currentPos);
            readingFrom.limit(lastPos);
        }

        @Override
        public int read() throws IOException {
            if (!readingFrom.hasRemaining()) {
                if (currentBlock.isFinal())
                    return -1;
                currentBlock = currentBlock.nextBlock();
                lastPos = currentBlock.lastPos;
                readingFrom.position(currentBlock.currentPos);
                readingFrom.limit(lastPos);
            }
//            if (x < 0) {
//                x += 256;
//            }
            return readingFrom.get() & 0xFF;
        }


        @Override
        public int read(byte[] b, int off, int desired) throws IOException {
            if (!readingFrom.hasRemaining()) {
                if (currentBlock.isFinal())
                    return -1;
                currentBlock = currentBlock.nextBlock();
                lastPos = currentBlock.lastPos;
                readingFrom.position(currentBlock.currentPos);
                readingFrom.limit(lastPos);
            }

            int available = readingFrom.remaining();
            int toRead = Math.min(available, desired);
            readingFrom.get(b, off, toRead);
            if (desired > available) {
                int tmp = read(b, off + toRead, desired - toRead);
                if (tmp == -1) {
                    tmp = 0;
                }
                toRead += tmp;
            }
            return toRead;
        }

        @Override
        public boolean markSupported() {
            return false;
        }
    }

    private Block getFirstBlock(int msgId) {
        Node node = new Node(msgId);
        int start = node.getAddr();
        int end = node.getLastAddr();
        return new Block(start, end - start);
    }
}

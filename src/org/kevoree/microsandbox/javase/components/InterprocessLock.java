package org.kevoree.microsandbox.javase.components;

/**
 * Created with IntelliJ IDEA.
 * User: inti
 * Date: 1/20/14
 * Time: 3:57 PM
 *
 */
public class InterprocessLock {
    private int lockId;
    private byte[] id;

    public InterprocessLock(byte[] id, boolean initialize) {
        this.id = id;
        lockId = InterprocessLock.createNew(id, initialize);
    }

    public void lock() {
        InterprocessLock.lock(lockId);
    }

    public void unlock() {
        InterprocessLock.unlock(lockId);
    }

    public void broadcast() {
        InterprocessLock.broadcast(lockId);
    }

    public void waitInterprocess() {
        InterprocessLock.waitInterprocess(lockId);
    }

    public void broadcast2() {
        InterprocessLock.broadcast2(lockId);
    }

    public void waitInterprocess2() {
        InterprocessLock.waitInterprocess2(lockId);
    }

    private native static void waitInterprocess(int lockId);

    private native static void broadcast(int lockId);

    private native static void waitInterprocess2(int lockId);

    private native static void broadcast2(int lockId);

    private native static void lock(int lockId);

    private native static void unlock(int lockId);

    private native static int createNew(byte[] id, boolean initialize);

//    private native static void initTable();
//
//    static {
//        initTable();
//    }
}

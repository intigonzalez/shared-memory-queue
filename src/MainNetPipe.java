import netpipe.*;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.List;

/**
 * Created by inti on 13/05/14.
 */
public class MainNetPipe {
    private static final int CHARSIZE = 8;
    private static final int TRIALS = 7; //Original 7, I was using 27
    private static final int LATENCYREPS = 10000;
    private static final int NSAMP = 8000;
    private static final double STOPTM = 1.0;
    private static final int PERT = 3;
    private static final double RUNTM = 0.25;
    private NetPipeStrategy netpipeProtocol = new TCPNetPipe();
    private int nbConsumers = 1;

    public static void main(String[] args) {
        new MainNetPipe().execute(args);
    }

    public class Configuration {
        boolean tr; // is the producer
        int nbuff; // number of trials

        private int bufflen;
        private ByteBuffer buffer;
        private int maxSize;

        public Configuration() {
            buffer = ByteBuffer.allocateDirect(maxSize = 100*1024*1024);
        }

        public void allocateBuffers(int len) {
            buffer.limit(bufflen = len);
        }

        public int bufflen() {
            return bufflen;
        }
    }

    class Data {
        double t;
        double bps;
        double variance;
        long bits;
        int repeat;
    }

    private void execute(String[] args) {

        PrintWriter out = null;
        Configuration configuration = new Configuration();
        configuration.nbuff = TRIALS;
        configuration.tr = args[0].equals("p");

        List<String> argsL = Arrays.asList(args);

        int index = Arrays.asList(args).indexOf("-shared");
        if (index != -1) {
            netpipeProtocol = new SharedMemNetPipe();
            index = argsL.indexOf("-consumers");
            if (configuration.tr && index != -1) {
                int count = Integer.parseInt(argsL.get(index + 1));
                if (count > 1) {
                    nbConsumers = count;
                    netpipeProtocol = new SharedMemNetPipeMulticast(nbConsumers);
                    System.out.println("Muy loco");
                }
            }
        }
        else {
            index = argsL.indexOf("-consumers");
            if (index != -1) {
                int count = Integer.parseInt(argsL.get(index + 1));
                if (count > 1) {
                    nbConsumers = count;
                    netpipeProtocol = new TCPNetPipeMulticast(nbConsumers);
                }
            }
        }

        Data[] bwdata = setup(configuration);

        String fileName = null;
        index = Arrays.asList(args).indexOf("-out");
        if (index != -1) {
            fileName = args[index+1];
        }

        if (configuration.tr) {
            try {

                out = new PrintWriter(new FileOutputStream(fileName));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                System.exit(1);
            }

        } else
            out = new PrintWriter(System.out);

        configuration.allocateBuffers(1);

        double t0 = When();
        for (int i = 0; i < LATENCYREPS; i++) {
            if (configuration.tr) {
                sendData(configuration);
                recvData(configuration);
            } else {
                recvData(configuration);
                sendData(configuration);
            }
        }
        double latency = (When() - t0) / (2 * LATENCYREPS);

        if (configuration.tr) {
            sendTime(configuration, latency);
        } else {
            latency = recvTime(configuration);
        }
        if (configuration.tr) {
            System.err.printf("Latency: %.12f\n", latency);
            System.err.printf("Now starting main loop\n");
        }
        double tlast = latency;
        int inc = 0;
        int start = 1;
        if (inc == 0) {
            /* Set a starting value for the message size increment. */
            inc = (start > 1) ? start / 2 : 1;
        }

        int end = 65*1024*1024;
        index = Arrays.asList(args).indexOf("-end");
        if (index != -1) {
            end = Integer.parseInt(args[index + 1]);
        }

        /* Main loop of benchmark */
        int nq, n, len, pert;
        int nrepeat;
        for (nq = n = 0, len = start;
             n < NSAMP - 3 && tlast < STOPTM && len <= end;
             len = len + inc, nq++) {
//            if (configuration.tr) {
//                System.out.println();
//                System.out.println("n = " + n);
//                System.out.printf("%.12f <? %.12f\n", tlast, STOPTM);
//                System.out.println("len = " + len);
//            }
            if (nq > 2 /*&& !detailflag*/) {
                /*
                This has the effect of exponentially increasing the block
                size. If detailflag is false, then the block size is
                linearly increased (the increment is not adjusted).
                */
                inc = (nq % 2 == 1) ? (inc + inc) : inc;
            }

            /* This is a perturbation loop to test nearby values */
            for (pert = (inc > PERT + 1) ? -PERT : 0;
                 pert <= PERT;
                 n++, pert += (inc > PERT + 1) ? PERT : PERT + 1) {

                /* Calculate how many times to repeat the experiment. */
                if (configuration.tr) {
                    nrepeat = (int) Math.max((RUNTM / ((double)configuration.bufflen() /
                                    (configuration.bufflen() - inc + 1.0) * tlast)),
                            TRIALS);
                    sendRepeat(configuration, nrepeat);
                } else
                    nrepeat = recvRepeat(configuration);

                /* Allocate the buffer */
                configuration.allocateBuffers(len + pert);
                if (configuration.tr)
                    System.err.printf("%3d: %9d bytes %4d times --> ",
                            n, configuration.bufflen(), nrepeat);

                /* Finally, we get to transmit or receive and time */
                if (configuration.tr) {
                    /*
                    This is the transmitter: send the block TRIALS times, and
                    if we are not streaming, expect the receiver to return each
                    block.
                    */
                    bwdata[n].t = Long.MAX_VALUE;
                    long t2 = 0;
                    long t1 = 0;
                    for (int i = 0; i < TRIALS; i++) {
//                        Sync(&args);
                        t0 = When();
                        for (int j = 0; j < nrepeat; j++) {
                            sendData(configuration);
                            recvData(configuration);
                        }
                        double t = (When() - t0) / (2 * nrepeat);

                        t2 += t * t;
                        t1 += t;
                        bwdata[n].t =
                                Math.min(bwdata[n].t, t);
                    }
                    sendTime(configuration, bwdata[n].t);

                    bwdata[n].variance =
                                t2 / TRIALS -
                                        t1 / TRIALS * t1 / TRIALS;

                } else {
                    /*
                    This is the receiver: receive the block TRIALS times, and
                    if we are not streaming, send the block back to the
                    sender.
                    */
                    bwdata[n].t = Long.MAX_VALUE;
//                    long t2 = 0;
//                    long t1 = 0;
                    for (int i = 0; i < TRIALS; i++) {
//                        t0 = When();
                        for (int j = 0; j < nrepeat; j++) {
                            recvData(configuration);
                            sendData(configuration);
                        }
//                        double t = (When() - t0) / (2 * nrepeat);
                    }
                    bwdata[n].t = recvTime(configuration);
                }
                tlast = bwdata[n].t;
                bwdata[n].bits = (long)(configuration.bufflen() * CHARSIZE ) * nbConsumers;
                bwdata[n].bps =
                        bwdata[n].bits / (bwdata[n].t * 1024 * 1024);
                bwdata[n].repeat = nrepeat;

                if (configuration.tr) {
                    out.printf("%.9f %.9f %d %d %.9f",
                            bwdata[n].t, bwdata[n].bps,
                            bwdata[n].bits, bwdata[n].bits / 8,
                            bwdata[n].variance);
                    out.printf("\n");
                }
                out.flush();

                if (configuration.tr) {
                    System.err.printf(" %6.3f Mbps in %.9f sec",
                            bwdata[n].bps, tlast);
                    System.err.printf("\n");
                }
            }	/* End of perturbation loop */

        }	/* End of main loop */

        if (configuration.tr)
            out.close();
    }

    private double When() {
        return System.nanoTime() / 1000000000.0;
    }

    private int recvRepeat(Configuration configuration) {
        if (!configuration.tr) {
            // it is the consumer
            int tmp = configuration.buffer.limit();
            configuration.buffer.limit(4);
            configuration.buffer.position(0);
            // receive
            // - here
            netpipeProtocol.receive(configuration.buffer);
            configuration.buffer.position(0);
            int n = configuration.buffer.getInt(0);
            configuration.buffer.limit(tmp);
            return n;
        }
        throw new RuntimeException("Why are you executing this in the producer?");
    }

    private double recvTime(Configuration configuration) {
        if (!configuration.tr) {
            // it is the consumer
            int tmp = configuration.buffer.limit();
            configuration.buffer.limit(10);
            configuration.buffer.position(0);
            // receive
            // - here
            netpipeProtocol.receive(configuration.buffer);
            configuration.buffer.position(0);
            double time = configuration.buffer.getDouble(0);
            configuration.buffer.limit(tmp);
            return time;
        }
        throw new RuntimeException("Why are you executing this in the producer?");
    }

    private void sendRepeat(Configuration configuration, int nrepeat) {
        if (configuration.tr) {
            // it is the producer
            int tmp = configuration.buffer.limit();
            configuration.buffer.limit(4);
            configuration.buffer.putInt(0, nrepeat);
            configuration.buffer.position(0);
            // send
            // - here
            netpipeProtocol.send(configuration.buffer);
            configuration.buffer.limit(tmp);
        }
        else {
            throw new RuntimeException("Why are you calling this in the consumer?");
        }
    }

    private void sendTime(Configuration configuration, double latency) {
        if (configuration.tr) {
            // it is the producer
            int tmp = configuration.buffer.limit();
            configuration.buffer.limit(10);
            configuration.buffer.putDouble(0, latency);
            configuration.buffer.position(0);
            // send
            // - here
            netpipeProtocol.send(configuration.buffer);
            configuration.buffer.limit(tmp);
        }
        else {
            throw new RuntimeException("Why are you calling this in the consumer?");
        }
    }

    private void recvData(Configuration configuration) {
        if (configuration.tr) {
            // the producer
        }
        else {
            // the consumer
        }
        configuration.buffer.position(0);
        // receive here
        netpipeProtocol.receive(configuration.buffer);
    }


    private void sendData(Configuration configuration) {
        if (configuration.tr) {
            // the producer
        }
        else {
            // the consumer
        }
        // I am sending whatever is in the buffer
        configuration.buffer.position(0);
        // send here
        netpipeProtocol.send(configuration.buffer);
    }

    private Data[] setup(Configuration configuration) {
        if (configuration.tr) {
            // it is the producer
            netpipeProtocol.setupTransmitter();
        }
        else {
            // it is the consumer
            netpipeProtocol.setupReceiver();
        }

        Data[] data = new Data[NSAMP];
        for (int i = 0; i < data.length; i++) {
            data[i] = new Data();
        }
        return data;
    }
}

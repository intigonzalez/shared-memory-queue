import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Created by inti on 08/05/14.
 */
public class MainTests extends BaseMain {



    static class Result {
        long[] executionsTime;
        int c;

        Result(int c) {
            this.c = 0;
            executionsTime = new long[c];
        }

        synchronized void add(long l) {
            executionsTime[c++] = l;
        }

        double average() {
            double avg = 0;
            for (int i = 0; i < c; i++) {
                avg = executionsTime[i] + avg;
            }
            return avg / c;
        }
    }

    static class ExecutingConsumer extends Thread {
        private final SERIALIZATION serialization;
        private final Result result;
        private final IPC ipc;
        private final DATA dataType;
        private final int msg_size;

        ExecutingConsumer(Result result, IPC ipc, DATA dataType, int msg_size, SERIALIZATION serialization) {
            this.result = result;
            this.ipc = ipc;
            this.dataType = dataType;
            this.msg_size = msg_size;
            this.serialization = serialization;
        }

        @Override
        public void run() {
            long before = System.nanoTime();
            // execute consumer
            ProcessBuilder pb = new ProcessBuilder("java", "-cp" ,".:junixsocket-1.3.jar:fst-1.55-onejar.jar",
                    "Main", "c",
                    "-ipc", ipc.name(), "-data", dataType.name(),
                    "-msg_size", Integer.toString(msg_size),
                    "-marshalling", serialization.name()).inheritIO();
            try {
                Process p = pb.start();
                p.waitFor();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            long after = System.nanoTime();
            result.add(after - before);
        }
    }


    public static void main(String[] args) throws FileNotFoundException {
        int ITERATIONS = 2;
        String resultFileName = "results.txt";
        if (args.length == 1) {
            resultFileName = args[0];
        }
        PrintStream ps = new PrintStream(resultFileName);
        int[] msg_size = new int[] {4000, 8000, 16000, 32000, 64000, 128000};
        int[] consumers_options = new int[] {1, 2 , 3, 4, 5, 6, 7, 8 };
//        for (int consumers : consumers_options) {
//            for (DATA dataType : DATA.values())
//                for (IPC ipc : IPC.values()) {
//                    if (dataType == DATA.RAW)
//                        for (int i = 0; i < msg_size.length; i++) {
//                            Result result = executeExperiment(dataType, ipc, msg_size[i], ITERATIONS, consumers);
//                            ps.printf("%d\t%s\t%s\t%d\t%f\n", consumers,ipc.name(), dataType.name(), msg_size[i], result.average() / 1000000000);
//                            System.out.printf("%d\t%s\t%s\t%d\t%f\n",consumers, ipc.name(), dataType.name(), msg_size[i], result.average() / 1000000000);
//                        }
//                }
//        }
        for (SERIALIZATION serialization: SERIALIZATION.values()) {
            for (int consumers : consumers_options) {
                for (IPC ipc : IPC.values()) {
                    if (ipc == IPC.UNIX_SOCKET) continue;
                    System.out.printf("Doing the trick for %s,%d,%s\n", ipc.name(), consumers, serialization.name());
                    Result result = executeExperiment(DATA.POJO, ipc, 0, ITERATIONS, consumers, serialization);
                    ps.printf("%d\t%s\t%s\t%s\t%d\t%f\n", consumers, ipc.name(), DATA.POJO.name(), serialization.name(), 0, result.average() / 1000000000);
                    System.out.printf("%d\t%s\t%s\t%s\t%d\t%f\n", consumers, ipc.name(), DATA.POJO.name(),serialization.name(), 0, result.average() / 1000000000);
                }
            }
        }
        ps.close();
    }

    private static Result executeExperiment(DATA dataType, IPC ipc, int msg_size,
                                            int iterations, int consumers, SERIALIZATION serialization) {
        try {
            Result result = new Result(iterations);
            for (int it = 0; it < iterations; it++) {
                // execute the producer
                ProcessBuilder pb = new ProcessBuilder("java","-cp" ,".:junixsocket-1.3.jar:fst-1.55-onejar.jar",
                        "Main", "p",
                        "-ipc", ipc.name(), "-data", dataType.name(),
                        "-msg_size", Integer.toString(msg_size), "-marshalling", serialization.name()
                        , Integer.toString(consumers)).inheritIO();
                Process p = pb.start();
                // wait a second to get producer ready
                Thread.sleep(5000);

                ExecutingConsumer[] cons = new ExecutingConsumer[consumers];
                Result partial = new Result(consumers);
                for (int c = 0 ; c < consumers ; ++c) {
                    cons[c] = new ExecutingConsumer(partial, ipc, dataType, msg_size, serialization);
                    cons[c].start();
                }

                // wait for consumers to finish
                for (int i = 0; i < consumers; i++) cons[i].join();
                // wait for producer
                p.waitFor();

                // add average of consumers
                result.add((long)partial.average());
            }
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;

    }
}

import strategies.*;
import strategies.marshalling.BuiltinMarshalling;
import strategies.marshalling.FSTMarshalling;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by inti on 07/05/14.
 */
public abstract class BaseMain {

    public static final int MESSAGE_SIZE = 102400/16;

    static enum IPC {
        UNIX_SOCKET("Use unix domain sockets. Requires a native library: junixsocket."),
        NIO("Use the standard NIO library"),
        SHARED("Use a queue built on top of shared memory"),
//        SOCKETS("Use the old-fashion java sockets"),
//        JFS("Use a library with support for fast channels. It isn't that fast")
        ;

        private IPC(String d) {
            description = d;
        }

        private String description;

        public String description() {
            return description;
        }
    }

    protected static enum DATA {
        RAW ("Copy an array from a process to others"),
        POJO ("Copy a POJO structure from a process to others");

        private DATA(String d) {
            description = d;
        }

        private String description;

        public String description() {
            return description;
        }
    }

    static enum SERIALIZATION {
        BUILTIN("Java Built-in Serialization"),
        FST("Fast Serialization Library")
        ;

        private SERIALIZATION(String d) {
            description = d;
        }

        private String description;

        public String description() {
            return description;
        }
    }

    public static void execute(BaseMain o, String[] arg) {
        Map<String, IPC> ipcMap = new HashMap<String, IPC>(10);
        for (IPC ipc: IPC.values()) {
            ipcMap.put(ipc.name(), ipc);
            ipcMap.put(ipc.name().toLowerCase(), ipc);
        }
        Map<String, DATA> dataMap = new HashMap<String, DATA>(8);
        for (DATA datatype: DATA.values()) {
            dataMap.put(datatype.name(), datatype);
            dataMap.put(datatype.name().toLowerCase(), datatype);
        }
        Map<String, SERIALIZATION> dataSerialization = new HashMap<String, SERIALIZATION>(8);
        for (SERIALIZATION serializationType: SERIALIZATION.values()) {
            dataSerialization.put(serializationType.name(), serializationType);
            dataSerialization.put(serializationType.name().toLowerCase(), serializationType);
        }

        if (!arg[0].equals("c") && !arg[0].equals("p")) {
            printUsageMessage(ipcMap, dataMap);
        }
        else {
            IPC ipcOption = IPC.NIO;
            DATA dataOption = DATA.POJO;
            SERIALIZATION serializationOption = SERIALIZATION.FST;
            int consumers = 1;
            int msg_count = 1000000;
            int msg_size = 1000;
            for (int i = 1 ; i < arg.length ; i++) {
                if (arg[i].equals("-ipc")) {
                    i++;
                    if (ipcMap.containsKey(arg[i])) {
                        ipcOption = ipcMap.get(arg[i]);
                    }
                    else {
                        System.err.printf("Wrong ipc option %s\n", arg[i]);
                        printUsageMessage(ipcMap, dataMap);
                    }
                }
                else if (arg[i].equals("-data")) {
                    i++;
                    if (dataMap.containsKey(arg[i])) {
                        dataOption = dataMap.get(arg[i]);
                    }
                    else {
                        System.err.printf("Wrong data option %s\n", arg[i]);
                        printUsageMessage(ipcMap, dataMap);
                    }
                }
                else if (arg[i].equals("-marshalling")) {
                    i++;
                    if (dataSerialization.containsKey(arg[i])) {
                        serializationOption = dataSerialization.get(arg[i]);
                    }
                    else {
                        System.err.printf("Wrong data option %s\n", arg[i]);
                        printUsageMessage(ipcMap, dataMap);
                    }
                }
                else if (arg[i].equals("-msg_count")) {
                    i++;
                    Integer tmp = getInteger(arg[i]);
                    if (tmp != null) {
                        msg_count = tmp;
                    }
                    else {
                        System.err.printf("Incorrect value %s for message count at %d\n", arg[i], i);
                        printUsageMessage(ipcMap, dataMap);
                    }
                }
                else if (arg[i].equals("-msg_size")) {
                    i++;
                    Integer tmp = getInteger(arg[i]);
                    if (tmp != null) {
                        msg_size = tmp >> 2;
                    }
                    else {
                        System.err.printf("Incorrect value %s for message size at %d\n", arg[i], i);
                        printUsageMessage(ipcMap, dataMap);
                    }
                }
                else if (getInteger(arg[i]) != null) {
                    consumers = getInteger(arg[i]);
                }
                else {
                    System.out.printf("Unexpected argument at %d: %s\n", i, arg[i]);
                    printUsageMessage(ipcMap, dataMap);
                }
            }
            IPCStrategy ipcStrategy = null;

            switch (ipcOption) {
                case SHARED:
                    switch (dataOption) {
                        case RAW:
                            ipcStrategy = new SharedMemoryQueueRawMessage(msg_count,msg_size);
                            break;
                        case POJO:
                            ipcStrategy = new SharedMemoryQueueForPOJO(msg_count);
                            if (serializationOption == SERIALIZATION.FST) {
                                ((IPCForPOJO)ipcStrategy).setSerializer(new FSTMarshalling());
                            }
                            else if (serializationOption == SERIALIZATION.BUILTIN) {
                                ((IPCForPOJO)ipcStrategy).setSerializer(new BuiltinMarshalling());
                            }
                            break;
                        default:
                            throw new RuntimeException("Using wrong data format");
                    }
                    break;
                case UNIX_SOCKET:
                    switch (dataOption) {
                        case RAW:
                            ipcStrategy = new UnixSocketsRawMessage(msg_count,msg_size);
                            break;
                        case POJO:
                            ipcStrategy = new UnixSocketForPOJO(msg_count);
                            break;
                        default:
                            throw new RuntimeException("Using wrong data format");
                    }
                    break;
                case NIO:
                    switch (dataOption) {
                        case RAW:
                            ipcStrategy = new NioRawMessage(msg_count, msg_size);
                            break;
                        case POJO:
                            ipcStrategy = new NIOForPOJO(msg_count);
                            if (serializationOption == SERIALIZATION.FST) {
                                ((IPCForPOJO)ipcStrategy).setSerializer(new FSTMarshalling());
                            }
                            else if (serializationOption == SERIALIZATION.BUILTIN) {
                                ((IPCForPOJO)ipcStrategy).setSerializer(new BuiltinMarshalling());
                            }
                            break;
                        default:
                            throw new RuntimeException("Using wrong data format");
                    }
                    break;
                default:
                    throw new RuntimeException("Using a wrong ipc");
            }


            if (arg[0].equals("c")) {
                // consumer
                ipcStrategy.consumer();
            }
            else {
                // producer
                ipcStrategy.producer(consumers);
            }
        }

    }

    protected static Integer getInteger(String s) {
        try {
            return Integer.valueOf(s);
        }
        catch (Exception e) {
            return null;
        }
    }

    private static void printUsageMessage(Map<String, IPC> ipcMap, Map<String, DATA> dataMap) {
        System.err.println("Usage for producer: java class_name p -ipc IPC_OPTION -raw DATA_OPTION -msg_count NUMBER number_of_consumers");
        System.err.println("Usage for consumer: java class_name c -ipc IPC_OPTION -raw DATA_OPTION");
        System.err.printf("-msg_count - Number of messages the producer will send. " +
                "Default value is a million. Only valid for the producer.\n");
        System.err.printf("-msg_count - Size of raw messages. " +
                "Default value is 4000 bytes. Ignored if the DATA_OPTION is not %s\n", DATA.RAW.name());

        System.err.println("IPC_OPTION:");
        for (IPC ipc: ipcMap.values()) {
            System.out.printf("\t%s -\t%s\n", ipc.name(), ipc.description());
        }

        System.err.println("DATA_OPTION:");
        for (DATA data: dataMap.values()) {
            System.out.printf("\t%s -\t%s\n", data.name(), data.description());
        }

        System.err.println("SERIALIZATION_OPTION: Ignored if data type is other than POJO");
        for (SERIALIZATION data: SERIALIZATION.values()) {
            System.out.printf("\t%s -\t%s\n", data.name(), data.description());
        }
        System.exit(0);
    }

}

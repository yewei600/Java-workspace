package a1;

import java.lang.Integer;
import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Array;
import java.util.*;

import org.apache.thrift.server.*;
import org.apache.thrift.server.TServer.*;
import org.apache.thrift.transport.*;
import org.apache.thrift.protocol.*;
import org.apache.thrift.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.nio.ByteBuffer;
import java.lang.Thread;
import java.lang.InterruptedException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


public class Test {
	public static void main(String [] args) throws IOException, TException, InterruptedException {
		if (args.length != 1) {
			System.err.println("Usage: java config_file");
			System.exit(-1);
		}
		System.out.println("args[0] is " + args[0]);
		BufferedReader br = new BufferedReader(new FileReader(args[0]));
		HashMap<Integer, String> hosts = new HashMap<Integer, String>();
		HashMap<Integer, Integer> ports  = new HashMap<Integer, Integer>();
		String line;
		int i = 0;
		while ((line = br.readLine()) != null) {
			String[] parts = line.split(" ");
			hosts.put(i, parts[0]);
			ports.put(i, Integer.parseInt(parts[1]));
			i++;
		}

		TestHost(hosts, ports, "main");
	}


	private static void TestHost(HashMap<Integer, String> hosts, HashMap<Integer, Integer> ports, String label)
            throws IOException, TException, InterruptedException {
		int serverNum = hosts.size();
        Random r = new Random(0);

        Hashtable<String, ByteBuffer> ref = new Hashtable<String, ByteBuffer>();



        int time = 0;
        int listLength = 10;

        Object[] keyValues = genKeyValues(listLength, r, time);
        List<String> keys = (ArrayList<String>) keyValues[0];
        List<ByteBuffer> values = (ArrayList<ByteBuffer>) keyValues[1];

        for (int i = 0; i < keys.size(); i++) {
            ref.put(keys.get(i), values.get(i));
        }

        int crtServer = -1;
		while (true) {
            crtServer++;
            crtServer = crtServer % serverNum;
            String host = hosts.get(crtServer);
            int port = ports.get(crtServer);

            System.out.println(String.format("host: %s, port: %d, %d th time", host, port, time));
            TSocket sock = new TSocket(host, port);
            TTransport transport = new TFramedTransport(sock);
            TProtocol protocol = new TBinaryProtocol(transport);
            KeyValueService.Client client = new KeyValueService.Client(protocol);

            transport.open();
            List<ByteBuffer> result = client.multiGet(keys);

            boolean flag = check(ref, keys, result);
            if (flag)
                System.out.println(String.format("The %dth multiGet is correct", time));

            System.out.println("***************keys&values generation************");
            keyValues = genKeyValues(listLength, r, time);
            keys = (ArrayList<String>) keyValues[0];
            values = (ArrayList<ByteBuffer>) keyValues[1];
            for (int i = 0; i< keys.size(); i++) {
                String key = keys.get(i);
                byte[] ba = new byte[values.get(i).remaining()];
                values.get(i).get(ba);
                System.out.println(String.format("KEY: %s, VALUE: %s", key, new String(ba)));
            }
            System.out.println("******************end****************");


            result = client.multiPut(keys, values);
            flag = check(ref, keys, result);
            if (flag)
                System.out.println(String.format("The %dth multiPut is correct", time));

            for (int i = 0; i < keys.size(); i++) {
                ref.put(keys.get(i), values.get(i));
            }
            transport.close();

            System.out.println("*********************Record***************");
            for (String key : ref.keySet()) {
                byte[] ba = new byte[ref.get(key).remaining()];
                ref.get(key).get(ba);
                System.out.println(String.format("KEY: %s, VALUE: %s", key, new String(ba)));
            }
            System.out.println("******************end****************");


            Thread.sleep(1000);
		}
	}

    private static boolean check(Hashtable<String, ByteBuffer> ref, List<String> keys, List<ByteBuffer> ret) {
        boolean c = true;
        Iterator<ByteBuffer> iret = ret.iterator();
        for (int i=0; i < keys.size(); i++) {
            ByteBuffer b1 = ref.get(keys.get(i));
            ByteBuffer b2 = iret.next();
            if (b1.compareTo(b2) != 0) {
                c = false;
                byte[] ba1 = new byte[b1.remaining()];
                byte[] ba2 = new byte[b2.remaining()];
                System.out.println(String.format("multiGet error. Receive \"%s\", expected \"%s\"",
                        new String(ba1), new String(ba2)));
            }
        }
        return c;
    }

    private static Object[] genKeyValues(int listLength, Random r, int fixIndex) {
        List<String> keys = new ArrayList<String>();
        List<ByteBuffer> values = new ArrayList<ByteBuffer>();
        for (int i = 0; i < listLength; i++) {
            String key = String.format("Key#%d-%d", i, r.nextInt(listLength * 2));
            DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
            String datestr = dateFormat.format(new Date());
            String value = String.format("value#%d-%d : %s : %d", fixIndex, i, datestr, r.nextInt());

            keys.add(key);
            values.add(ByteBuffer.wrap(new byte[0]));
        }

        Object[] ret = new Object[2];
        ret[0] = keys;
        ret[1] = values;
        return ret;
    }
}



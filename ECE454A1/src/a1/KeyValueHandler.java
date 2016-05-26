package a1;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.List;
import org.apache.thrift.TException;

import org.apache.thrift.server.*;
import org.apache.thrift.transport.*;
import org.apache.thrift.protocol.*;
import org.apache.thrift.*; 

public class KeyValueHandler implements KeyValueService.Iface {
    
    HashMap<String, byte[]> storage = new HashMap<String, byte[]>();
    final HashMap<Integer, String> hosts;
    final HashMap<Integer, Integer> ports;
    final int myNum;

    public KeyValueHandler(int myNum, HashMap<Integer, String> hosts, HashMap<Integer, Integer> ports) {
        this.hosts = hosts;
        this.ports = ports;
        this.myNum = myNum;
    }

    public List<String> getGroupMembers()
    {
	    List<String> ret = new ArrayList<String>();
	    ret.add("y52wei");
	    ret.add("zjian");
	    return ret;
    }

   private Object[] partite(List<String> keys) {
    	int serverNum = hosts.size();
        int l = keys.size(); 
        ArrayList<LinkedList<String>> keyDis = new ArrayList<LinkedList<String>>(serverNum);
        ArrayList<LinkedList<Integer>> inDis = new ArrayList<LinkedList<Integer>>(serverNum);
        for (int i = 0; i < serverNum; i++) {
            keyDis.add(i, new LinkedList<String>());
            inDis.add(i, new LinkedList<Integer>());
        }
        int index = 0;
        for (String key: keys ) {
            keyDis.get(key.hashCode() % serverNum).add(key);
            inDis.get(key.hashCode() % serverNum).add(index);
            index++;
        }

        Object[] ret = new Object[2];
        ret[0] = keyDis;
        ret[1] = inDis;
        return ret;
   }



    public List<ByteBuffer> multiGet(List<String> keys) {
        int serverNum = hosts.size();
        int l = keys.size(); 

        Object[] partition = partite(keys);
        ArrayList<LinkedList<String>> keyDis = (ArrayList<LinkedList<String>>) partition[0];
        ArrayList<LinkedList<Integer>> inDis = (ArrayList<LinkedList<Integer>>) partition[1];
        ArrayList<LinkedList<byte[]>> rDis = new ArrayList<LinkedList<byte[]>>(serverNum);
        
        ArrayList<byte[]> r = new ArrayList<byte[]>(l);
        for (int serverIndex = 0; serverIndex < serverNum; serverIndex++) {
            if (serverIndex == myNum) {
                rDis.add(serverIndex, getLocalStorage(keyDis.get(serverIndex)));
            } else {
                if (keyDis.get(serverIndex).size() > 0)  {
                    rDis.add(serverIndex, getRemoteStorage(keyDis.get(serverIndex), hosts.get(serverIndex), ports.get(serverIndex)));
                } else {
                   rDis.add(serverIndex, new LinkedList<byte[]>());
                } 
            }
        }
        
        for (int serverIndex = 0; serverIndex < serverNum; serverIndex++) {
            for (int listIndex : inDis.get(serverIndex)) {
                r.add(listIndex, rDis.get(serverIndex).pop());
            }
        }

        ArrayList<ByteBuffer> ret = new ArrayList<ByteBuffer>(l);
        for (byte[] b : r) {
            ret.add(ByteBuffer.wrap(b));
        }

        log();
        return ret;
    }

    public List<ByteBuffer> multiPut(List<String> keys, List<ByteBuffer> values) throws IllegalArgument {
        System.out.println("executing multiPut");
    	if (keys.size() != values.size()) 
		    throw new IllegalArgument("the length of keys and values are different!");

    	int serverNum = hosts.size();
        int l = keys.size(); 
        System.out.println("executing partition");
        Object[] partition = partite(keys);
        ArrayList<LinkedList<String>> keyDis = (ArrayList<LinkedList<String>>) partition[0];
        ArrayList<LinkedList<Integer>> inDis = (ArrayList<LinkedList<Integer>>) partition[1];
        ArrayList<LinkedList<ByteBuffer>> vDis = new ArrayList<LinkedList<ByteBuffer>>(serverNum);

        for (int serverIndex = 0; serverIndex < serverNum; serverIndex++) {
        	vDis.add(serverIndex, new LinkedList<ByteBuffer>());
        	for (int index : inDis.get(serverIndex)) {
        		vDis.get(serverIndex).add(values.get(index));
        	}
        }

        ArrayList<LinkedList<ByteBuffer>> rDis = new ArrayList<LinkedList<ByteBuffer>>(serverNum);
        for (int serverIndex = 0; serverIndex < serverNum; serverIndex++) {
            if (serverIndex == myNum) {
                rDis.add(serverIndex, storeLocal(keyDis.get(serverIndex), vDis.get(serverIndex)));
            } else {
                if (keyDis.get(serverIndex).size() > 0)  {
                    rDis.add(serverIndex, storeRemote(keyDis.get(serverIndex), vDis.get(serverIndex), hosts.get(serverIndex), ports.get(serverIndex)));
                } else {
                    rDis.add(serverIndex, new LinkedList<ByteBuffer>());
                }
            }
        }

		ArrayList<ByteBuffer> ret = new ArrayList<ByteBuffer>(l);
        for (int serverIndex = 0; serverIndex < serverNum; serverIndex++) {
            for (int listIndex : inDis.get(serverIndex)) {
                ret.add(listIndex, rDis.get(serverIndex).pop());
            }
        }
        return ret;

    }


    private LinkedList<byte[]> getLocalStorage(LinkedList<String> keys) {
        System.out.println("getLocalStorage");
        LinkedList<byte[]> ret = new LinkedList<byte[]>();
        for (String key : keys) {
		        if (storage.containsKey(key)) {
			    ret.add(storage.get(key));
		    } else {
			    ret.add(new byte[0]);
		    }
	    }
	    return ret;
    }

    private LinkedList<byte[]> getRemoteStorage(LinkedList<String> keys, String remoteHost, int remotePort) {
    	try {
	        System.out.println(String.format("getRemoteStorage from %s - %d", remoteHost, remotePort));
	        TSocket sock = new TSocket(remoteHost, remotePort);
	        TTransport transport = new TFramedTransport(sock);
	        TProtocol protocol = new TBinaryProtocol(transport);
	        KeyValueService.Client client = new KeyValueService.Client(protocol);
	        
	        List<ByteBuffer> remoteResult = new LinkedList<ByteBuffer>();
	        
	        transport.open();
	        remoteResult = client.multiGet(keys);
	    	transport.close();
	        
	        LinkedList<byte[]> ret = new LinkedList<byte[]>();
	        for (ByteBuffer b: remoteResult) {
	            byte[] tmp = new byte[b.remaining()];
	            b.get(tmp);
	            ret.add(tmp);
	        }
	        return ret;
	    } catch (TException x) {
	    	x.printStackTrace();
	    }

	    LinkedList<byte[]> ret = new LinkedList<byte[]>();
	    for (String key : keys) {
	    	ret.add("TException".getBytes());
	    }
	    return ret;

    }

    
    private LinkedList<ByteBuffer> storeLocal(List<String> keys, List<ByteBuffer> values) {
        System.out.println("storeLocal");
	    LinkedList<ByteBuffer> ret = new LinkedList<ByteBuffer>();
	    for(int i=0; i<keys.size(); i++) {
		    String key = keys.get(i);
		    ByteBuffer v = values.get(i);
            byte[] value = new byte[v.remaining()];
            v.get(value);
		    if (storage.containsKey(key)) {
			    ret.add(ByteBuffer.wrap(storage.get(key)));
	    		storage.put(key, value);
		    } else {
			    storage.put(key, value);
			    ret.add(ByteBuffer.wrap(new byte[0]));
		    }
	    }   
        return ret;
    }

    private LinkedList<ByteBuffer> storeRemote(List<String> keys, List<ByteBuffer> values, String remoteHost, int remotePort) {
        try {
	        System.out.print(String.format("storeRemote to %s - %d", remoteHost, remotePort));
	   		TSocket sock = new TSocket(remoteHost, remotePort);
	        TTransport transport = new TFramedTransport(sock);
	        TProtocol protocol = new TBinaryProtocol(transport);
	        KeyValueService.Client client = new KeyValueService.Client(protocol);
	        
	        List<ByteBuffer> remoteResult = new LinkedList<ByteBuffer>();
	        transport.open();
	        remoteResult = client.multiPut(keys, values);
	        transport.close();
	        
	        LinkedList<ByteBuffer> ret = new LinkedList<ByteBuffer>();
	        for (ByteBuffer b: remoteResult) {
	            ret.add(b);
	        }
	        return ret;
        }catch (TException x) {
	    	x.printStackTrace();
	    }

	    LinkedList<ByteBuffer> ret = new LinkedList<ByteBuffer>();
	    for (String key : keys) {
	    	ret.add(ByteBuffer.wrap("TException".getBytes()));
	    }
	    return ret;
    }

    private void log() {
        System.out.println(String.format("myNumber: %d\n host: %s\n port: %d\n", myNum, hosts.get(myNum), ports.get(myNum)));
        for (String key : storage.keySet()) {
            System.out.println(String.format(
                        "RECORD - KEY: %s, VALUE: %s",
                        key, new String(storage.get(key))));
        }
        System.out.println(" ************Log Ends***********");
    }
}
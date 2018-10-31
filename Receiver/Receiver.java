package Receiver;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class Receiver {
    static Integer send_port;            // on which the emulator receive data from sender
    static Integer receive_port;         // on which the sender listen from the emulator
    static InetAddress emulator_addr;    // the ip address of the emulator
    static String file_name;             // the file name written to the current directory


    private static int receive_Seq;
    private static int expected_SeqNum;
    private static int type;
    private static byte[] buf;
    private static DatagramSocket ds;
    private static DatagramPacket dp_receive;
    private static DatagramPacket dp_send;
    public static ArrayList<Integer> receivedSeqNums = new ArrayList<Integer>();


    public static void main(String args[])throws IOException{
        emulator_addr = InetAddress.getByName(args[0]);
        send_port = Integer.parseInt(args[1]);
        receive_port = Integer.parseInt(args[2]);
        file_name = args[3];

        expected_SeqNum = 0;
        buf = new byte[500];
        ds = new DatagramSocket(receive_port);
        dp_receive = new DatagramPacket(buf, 500);
        dp_send = new DatagramPacket(buf,500);
        while (true) {
            receive();
        }
    }
    public static void receive(){
        try {
            // get the received packet number and type
            ds.receive(dp_receive);
            packet packetReceived= packet.parseUDPdata(dp_receive.getData());
            receive_Seq = packetReceived.getSeqNum();
            receivedSeqNums.add(receive_Seq);
            type = packetReceived.getType();
            System.out.println("SeqNum received: "+receive_Seq + "  Acknowledged: "+(expected_SeqNum -1)+  " Type: "+type );
            if(receive_Seq == expected_SeqNum) {
                // if received as expected, response the received number
                randomDrop();
                response(receive_Seq, type);
                expected_SeqNum = (expected_SeqNum + 1) % 32;
                // if received the last packet as expected, close the data socket
                if (type == 2) {
                    LogWriter.LogWriter logWriter = new LogWriter.LogWriter();
                    try{
                        logWriter.writeFile(file_name,receivedSeqNums);
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                    ds.close();
                    System.exit(0);
                }
            }
            else if(expected_SeqNum != 0){
                // if not as expected, return the up-to-now correct SeqNum
                response(expected_SeqNum-1, type);
            }
            // reset the receive buffer
            dp_receive.setLength(500);
        }catch (Exception e){
            System.out.println("Fail to resolve the packet");
        }
        dp_receive.setLength(500);
    }

    public static void response(int Seqnum, int Type) throws IOException{
        // generate a new packet to acknowledge the received packet
        try{
        packet packetAck;
        if(Type==1){
            packetAck = packet.createACK(Seqnum);
        }else{
            packetAck = packet.createEOT(Seqnum);
        }

        byte[] sendBuff= packetAck.getUDPdata();
        dp_send= new DatagramPacket(sendBuff,sendBuff.length,emulator_addr,send_port);
        ds.send(dp_send);}catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void randomDrop() throws InterruptedException{
        if(Math.random()<0.5){
            Thread.sleep(500);
        }
    }
}

class packet{

    // constants
    private final int maxDataLength = 500;
    private final int SeqNumModulo = 32;

    // data members
    private int type;
    private int seqnum;
    private String data;

    //////////////////////// CONSTRUCTORS //////////////////////////////////////////

    // hidden constructor to prevent creation of invalid packets
    private packet(int Type, int SeqNum, String strData) throws Exception {
        // if data seqment larger than allowed, then throw exception
        if (strData.length() > maxDataLength)
            throw new Exception("data too large (max 500 chars)");

        type = Type;
        seqnum = SeqNum % SeqNumModulo;
        data = strData;
    }

    // special packet constructors to be used in place of hidden constructor
    public static packet createACK(int SeqNum) throws Exception {
        return new packet(0, SeqNum, new String());
    }

    public static packet createPacket(int SeqNum, String data) throws Exception {
        return new packet(1, SeqNum, data);
    }

    public static packet createEOT(int SeqNum) throws Exception {
        return new packet(2, SeqNum, new String());
    }

    ///////////////////////// PACKET DATA //////////////////////////////////////////

    public int getType() {
        return type;
    }

    public int getSeqNum() {
        return seqnum;
    }

    public int getLength() {
        return data.length();
    }

    public byte[] getData() {
        return data.getBytes();
    }

    //////////////////////////// UDP HELPERS ///////////////////////////////////////

    public byte[] getUDPdata() {
        ByteBuffer buffer = ByteBuffer.allocate(512);
        buffer.putInt(type);
        buffer.putInt(seqnum);
        buffer.putInt(data.length());
        buffer.put(data.getBytes(),0,data.length());
        return buffer.array();
    }

    public static packet parseUDPdata(byte[] UDPdata) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(UDPdata);
        int type = buffer.getInt();
        int seqnum = buffer.getInt();
        int length = buffer.getInt();
        byte data[] = new byte[length];
        buffer.get(data, 0, length);
        return new packet(type, seqnum, new String(data));
    }
}
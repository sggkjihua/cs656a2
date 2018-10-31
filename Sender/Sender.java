package Sender;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


public class Sender{
    public static final int TIMEOUT = 100;
    public static final int Sequence = 32;
    public static int curr_Sequence = 0;
    public static int expected_Sequence;
    private static int windowSize = 10;
    private static Boolean complete = false;
    private static DatagramSocket ds;
    public static Timer timer;
    public static Boom boom;
    public static ArrayList<Integer> sentSeqNums = new ArrayList<Integer>();
    public static ArrayList<Integer> ackSeqNums = new ArrayList<Integer>();

    static Integer send_port;            // on which the emulator receive data from sender
    static Integer receive_port;         // on which the sender listen from the emulator
    static InetAddress emulator_addr;    // the ip address of the emulator
    static String file_name;             // the file name written to the current directory


    public static void main(String args[])throws IOException {
        emulator_addr = InetAddress.getByName(args[0]);
        send_port = Integer.parseInt(args[1]);
        receive_port = Integer.parseInt(args[2]);
        file_name = args[3];
        try{
            ds = new DatagramSocket(receive_port);
            SenderUDP senderUDP = new SenderUDP(emulator_addr,send_port,windowSize,ds);
            TimerReceiver receiver = new TimerReceiver();
            new Thread(receiver).start();
            new Thread(senderUDP).start();

            } catch (Exception e){
                e.printStackTrace();
            }
    }

    public static void moveBackExpectedSeq()
        {
            curr_Sequence = expected_Sequence;
        }

    public static void moveToNextSeq(){
        if(expected_Sequence < Sequence ){
            expected_Sequence = (expected_Sequence + 1)%Sequence;
        }
    }

    public static void setComplete(){
        complete = true;
    }

    public static void addCurrSeq(){
        if(curr_Sequence < Sequence+1){
        curr_Sequence = curr_Sequence + 1;
        }
    }

    public static Boolean getComplete(){
        return complete;
    }

    public static DatagramSocket getDs(){
        return ds;
    }
}



class SenderUDP implements Runnable{
    // send package thread
    byte[] buf = new byte[500];
    private InetAddress emulator_addr;
    private int send_port;
    private int windowSize;
    private DatagramSocket ds;
    private int type;
    private String send_str;


    public SenderUDP(InetAddress emulator_addr, int send_port, int windowSize, DatagramSocket datagramSocket){
        this.emulator_addr = emulator_addr;
        this.send_port = send_port;
        this.windowSize = windowSize;
        this.ds =  datagramSocket;
    }

    @Override
    public void run(){
        while(!Sender.getComplete()){
            sendPackage();
            System.out.print("");
         }
        System.out.println("Transmitted successfully!");
        ds.close();
        LogWriter.LogWriter logWriter = new LogWriter.LogWriter();
        try{
            logWriter.writeFile(Sender.file_name, Sender.sentSeqNums);
            logWriter.writeFile("ackSeqNums.log",Sender.ackSeqNums);
        }catch (IOException e){
            e.printStackTrace();
        }
        System.exit(0);
    }

    public void sendPackage(){
        if((Sender.curr_Sequence - Sender.expected_Sequence) < windowSize && Sender.curr_Sequence < Sender.Sequence+1){
           // check if it is still available to send package within window size
           try{
               packet packetSent;
            if(Sender.curr_Sequence == Sender.Sequence){
                send_str = "EOT with length 0";
                packetSent = packet.createEOT(Sender.curr_Sequence);
            }else{
                send_str = "Normal file chunk";
                packetSent = packet.createPacket(Sender.curr_Sequence ,send_str);
            }

            Sender.sentSeqNums.add(packetSent.getSeqNum());
            byte[] sendBuff=packetSent.getUDPdata();
            DatagramPacket data_sent= new DatagramPacket(sendBuff,sendBuff.length,emulator_addr,send_port);
            ds.send(data_sent);
            if(Sender.curr_Sequence == Sender.expected_Sequence) {
                Sender.timer = new Timer();
                Sender.boom = new Boom(Sender.expected_Sequence);
                Sender.timer.schedule(Sender.boom,Sender.TIMEOUT);
            }
            System.out.println("The current sent seq number is:"+ Sender.curr_Sequence);
            Sender.addCurrSeq();
           }catch (Exception e){
               e.printStackTrace();
           }
        }
    }
}


class Boom extends TimerTask {

    // only when the oldest packet was timed out will it work
    // when received as expected, start again for the next expected seqNum
    private int packet_SeqNum;
    public long startTime;
    // Run some code;

    public Boom(int packet_SeqNum){
        this.packet_SeqNum = packet_SeqNum;
        this.startTime = System.currentTimeMillis();
    }
    @Override
    public void run() {
        if(packet_SeqNum==Sender.expected_Sequence){
            System.out.println("Time out from "+ Sender.curr_Sequence +" back to "+Sender.expected_Sequence);
            Sender.moveBackExpectedSeq();
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

class TimerReceiver implements Runnable{
    private DatagramSocket ds;
    private int receive_Seq;
    private int receive_type;
    private byte[] buf;
    private DatagramPacket dp_receive;

    public TimerReceiver(){
        buf = new byte[500];
        ds = Sender.getDs();
        dp_receive = new DatagramPacket(buf, 500);
    }

    public void receive(){

        try {
            ds.receive(dp_receive);
            packet packetReceived =  packet.parseUDPdata(dp_receive.getData());
            receive_Seq = packetReceived.getSeqNum();
            receive_type = packetReceived.getType();
            Sender.ackSeqNums.add(receive_Seq);
            System.out.println("SeqNum acknowledged by receiver: "+receive_Seq +" Expected: "+Sender.expected_Sequence + "  Type: "+ receive_type);
            if(receive_type == 2){
                Sender.setComplete();
            }else if(receive_Seq == Sender.expected_Sequence){
                Sender.moveToNextSeq();
                Sender.timer.cancel();
                //System.out.println("Boom for " + (Sender.expected_Sequence-1)+" canceled  Time remaining: "+ (System.currentTimeMillis()-Sender.boom.startTime ));
                Sender.timer = new java.util.Timer();
                Sender.boom = new Boom(Sender.expected_Sequence);
                Sender.timer.schedule(Sender.boom,Sender.TIMEOUT);
            }
            dp_receive.setLength(500);
        }catch (Exception e){
            System.out.println("Unknown Exception2222");
        }
    }


    @Override
    public void run() {
        while(!Sender.getComplete()) {
            receive();
        }
    }
}


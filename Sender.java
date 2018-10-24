import com.sun.tools.javac.Main;
import javafx.concurrent.Task;

import javax.sound.midi.Receiver;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;



public class Sender{
    public static final int TIMEOUT = 100;
    public static final int Sequence = 32;
    public static int curr_Sequence = 0;
    public static int expected_Sequence;
    public static TasksLinkedList linkedlist;
    private static int windowSize = 10;
    private static Boolean complete = false;


    static Integer send_port;            // on which the emulator receive data from sender
    static Integer receive_port;         // on which the sender listen from the emulator
    static InetAddress emulator_addr;    // the ip address of the emulator
    static String file_name;             // the file name written to the current directory


    public static void main(String args[])throws IOException {
        linkedlist = new TasksLinkedList();
            emulator_addr = InetAddress.getByName(args[0]);
            send_port = Integer.parseInt(args[1]);
            receive_port = Integer.parseInt(args[2]);
            file_name = args[3];
            try{
            SenderUDP senderUDP = new SenderUDP(emulator_addr,send_port,windowSize);
            TimerReceive receiver = new TimerReceive(receive_port);
            senderUDP.run();
            receiver.run();
            } catch (Exception e){
                e.printStackTrace();
            }


    }
        //dp_receive.setLength(1024);
    public static void moveBackExpectedSeq()
        {
        curr_Sequence = expected_Sequence;
        }

    public static void moveToNextSeq(){
        if(expected_Sequence < Sequence-1){
            expected_Sequence = expected_Sequence + 1;
        }
    }

    public static void addCurrSeq(){
        curr_Sequence = curr_Sequence + 1;
    }

    public static Boolean getComplete(){
        return complete;
    }
}



class SenderUDP implements Runnable{
    // send package thread
    byte[] buf = new byte[500];
    private InetAddress emulator_addr;
    private int send_port;
    private int windowSize;
    private DatagramSocket ds;
    private Boolean complete = false;

    public SenderUDP(InetAddress emulator_addr, int send_port, int windowSize) throws IOException{
        this.emulator_addr = emulator_addr;
        this.send_port = send_port;
        this.windowSize = windowSize;
        this.ds =  new DatagramSocket(send_port);
    }

    @Override
    public void run(){
        while(!complete){
            try{
            sendPackage();
        }catch (IOException e) {
                e.printStackTrace();            }
        }
        ds.close();
    }

    public void sendPackage() throws IOException{
        while((Sender.curr_Sequence - Sender.expected_Sequence) < windowSize-1){
            // check if it is still available to send package within window size
            String test_str = "This is only for testing";
            Packet packet = new Packet(0,Sender.curr_Sequence,test_str.length(),test_str);
            ByteArrayOutputStream bout=new ByteArrayOutputStream();
            ObjectOutputStream oout=new ObjectOutputStream(bout);
            oout.writeObject(packet);
            oout.flush();
            byte[] sendBuff=bout.toByteArray();
            DatagramPacket data_sent= new DatagramPacket(sendBuff,sendBuff.length,emulator_addr,send_port);
            ds.send(data_sent);
            Sender.addCurrSeq();
            System.out.println("The current seq number is:"+ Sender.curr_Sequence);
        }
    }
}


import com.sun.tools.javac.Main;
import javafx.concurrent.Task;

import javax.sound.midi.Receiver;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

// 先检查是否在window 10个以内，在里面的话就发送curr_Sequence
// 此时另外一个thread 需要从未confirm的那一个开始， 在timeout之内检查expected和receive的是否一致
// 这个timeout是在发送某个包的时候同时开始的，因此要判断目前是否还是within timeout，不是的话就需要将curr_Seq变成expected_Seq
// 实际上应该每次发送包裹之后立刻要重新生成某个timeout ? 针对每个receive进行
//


public class Sender{
    public static final int TIMEOUT = 100;  //设置接收数据的超时时间
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
        //由于dp_receive在接收了数据之后，其内部消息长度值会变为实际接收的消息的字节数，
        //        //所以这里要将dp_receive的内部消息长度重新置为1024
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
        //客户端在9000端口监听接收到的数据
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


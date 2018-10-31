package Sender;

import Packet.packet;

import javax.management.timer.Timer;
import javax.management.timer.TimerMBean;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;

public class TimerReceiver implements Runnable{
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

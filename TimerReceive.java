import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Timer;
import java.util.TimerTask;

public class TimerReceive implements Runnable{
    private int receive_port;
    private DatagramSocket ds;
    private int receive_Seq;
    private byte[] buf;
    private DatagramPacket dp_receive;

    public TimerReceive(int receive_port) throws Exception {
        this.receive_port = receive_port;
        buf = new byte[500];
        ds = new DatagramSocket(receive_port);
        dp_receive = new DatagramPacket(buf, 500);
    }

    public void receive(){
        try {
            ds.receive(dp_receive);
            ByteArrayInputStream bin = new ByteArrayInputStream(dp_receive.getData());
            ObjectInputStream oin = new ObjectInputStream(bin);
            Packet packet = (Packet) oin.readObject();
            receive_Seq = packet.getSeqnum();
            if(!compare(receive_Seq,Sender.expected_Sequence)){
                Sender.moveBackExpectedSeq();

            }else{
                Sender.moveToNextSeq();
            }
            dp_receive.setLength(500);
        }catch (Exception e){
            System.out.println("Unknown Exception");
        }
    }

    public Boolean compare(int receive, int expected){
        return receive==expected;
    }

    @Override
    public void run() {
        while(!Sender.getComplete()){
            try{
                receive();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}

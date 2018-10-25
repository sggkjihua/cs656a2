import javax.sound.midi.Receiver;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class ReceiverTest {
    static Integer send_port;            // on which the emulator receive data from sender
    static Integer receive_port;         // on which the sender listen from the emulator
    static InetAddress emulator_addr;    // the ip address of the emulator
    static String file_name;             // the file name written to the current directory


    private static DatagramSocket ds;
    private static int receive_Seq;
    private static byte[] buf;
    private static DatagramPacket dp_receive;
    private static DatagramPacket dp_send;

    public static void main(String args[])throws IOException{
        emulator_addr = InetAddress.getByName(args[0]);
        send_port = Integer.parseInt(args[1]);
        receive_port = Integer.parseInt(args[2]);
        file_name = args[3];

        buf = new byte[500];
        ds = new DatagramSocket(receive_port);
        dp_receive = new DatagramPacket(buf, 500);
        dp_send = new DatagramPacket(buf,500);
        while (true) {
            System.out.println("I am here!!!");
            receive();
        }
    }
    public static void receive(){
        try {
            ds.receive(dp_receive);
            ByteArrayInputStream bin = new ByteArrayInputStream(dp_receive.getData());
            ObjectInputStream oin = new ObjectInputStream(bin);
            Packet packet = (Packet) oin.readObject();
            receive_Seq = packet.getSeqnum();
            System.out.println("SeqNum received is "+receive_Seq);
            response(receive_Seq);
            dp_receive.setLength(500);
        }catch (Exception e){
            System.out.println("Unknown Exception");
        }
        dp_receive.setLength(500);
    }

    public static void response(int Seqnum) throws IOException{
        String test_str = "This is only for response";
        Packet packet = new Packet(0,Seqnum,test_str.length(),test_str);
        ByteArrayOutputStream bout=new ByteArrayOutputStream();
        ObjectOutputStream oout=new ObjectOutputStream(bout);
        oout.writeObject(packet);
        oout.flush();
        byte[] sendBuff=bout.toByteArray();
        dp_send= new DatagramPacket(sendBuff,sendBuff.length,emulator_addr,send_port);
        ds.send(dp_send);
    }
}

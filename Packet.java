import java.io.Serializable;
import java.util.Timer;
import java.util.TimerTask;

public class Packet implements Serializable{
    private int type; // 0: ACK, 1: Data, 2: EOT
    private int seqnum; // Modulo 32
    private int length; // Length of the String variable ‘data’
    private String data; // String with Max Length 500
    private Boolean correctUpToNow = true;
    public Packet(int type, int seqnum, int length, String data) {
        // constructor including sequence number and type
        this.type = type;
        this.seqnum = seqnum;
        this.length = length;
        this.data = data;
        // schedule the timer once the packet is initialized
        new Timer().schedule(new Acknowledge(), Sender.TIMEOUT);
    }
    public int getType() {
        return type;
    }

    public int getSeqnum() {
        return seqnum;
    }

    public void stopWaiting(){
        // 一旦检查正确了或者前面已经有的fail了的话就需要让这个计时器停止
        correctUpToNow = false;
    }


    class Acknowledge extends TimerTask{
        @Override
        public void run() {
            // once it is time out, it should move the current Seq to the expected Seq
            if (correctUpToNow) {
                Sender.moveBackExpectedSeq();
            }
        }
    }
}

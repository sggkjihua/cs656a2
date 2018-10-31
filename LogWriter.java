package LogWriter;
import java.io.*;
import java.util.ArrayList;
public class LogWriter {
    public void writeFile(String fileName, ArrayList<Integer> SeqNums) throws IOException {
        File fout = new File(fileName);
        FileOutputStream fos = new FileOutputStream(fout);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

        for (int i = 0; i < SeqNums.size(); i++) {
            bw.write(SeqNums.get(i).toString());
            bw.newLine();
        }
        bw.close();
    }
}
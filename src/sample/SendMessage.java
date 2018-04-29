package sample;
import java.io.DataOutputStream;
import java.net.Socket;

public class SendMessage implements Runnable {
    private String mMsg;

    public SendMessage(String msg) {
        mMsg = msg;
    }

    public void run() {
        try {
            Socket socket = new Socket("185.128.97.145", 1755);
            DataOutputStream DOS = new DataOutputStream(socket.getOutputStream());
            DOS.writeUTF(mMsg);
            socket.close();
        } catch (Exception e) {
            System.out.println("Couldn't connect:" + e);
        }
    }
}


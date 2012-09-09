package bndtools.ace.launch;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AceLaunchSocket {
    private ServerSocket m_serverSocket;
    private Socket m_socket;
    private OutputStream m_outputStream;

    public int openSocket() {
        try {
            m_serverSocket = new ServerSocket(0);
            return m_serverSocket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void start() {
        ExecutorService threadPool = Executors.newFixedThreadPool(1);
        threadPool.execute(new Runnable() {

            public void run() {
                try {
                    m_socket = m_serverSocket.accept();
                    m_outputStream = m_socket.getOutputStream();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public void sendUpdated() {
        System.out.println("sending update");
        PrintWriter out = new PrintWriter(m_outputStream, true);
        String output = "UPDATE";
        out.println(output);
        System.out.println("sent update");
    }
}

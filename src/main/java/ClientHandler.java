import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));
            while (true) {
                String line = br.readLine();
                if (line == null) {
                    break;
                }

                if (line.equalsIgnoreCase("PING")) {
                    System.out.println("line: " + line);
                    clientSocket.getOutputStream().write("+PONG\r\n".getBytes());
                }
            }
        } catch (IOException e) {
            System.out.println("IOException:: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
                System.out.println("Closed socket " + clientSocket.toString());
            } catch (IOException e) {
                System.out.println("IOException:: " + e.getMessage());
            }
        }
    }

}
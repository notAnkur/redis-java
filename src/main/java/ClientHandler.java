import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler implements Runnable {

    private Socket clientSocket;
    ConcurrentHashMap<String, String> map;

    public ClientHandler(Socket socket, ConcurrentHashMap<String, String> map) {
        this.clientSocket = socket;
        this.map = map;
    }

    @Override
    public void run() {
        try {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));

            /*
             * client message format
             * *<ARR_SIZE> [crlf] $<LENGTH>[crlf] <data>[crlf] $<LENGTH>[crlf] <data>[crlf]
             */

            String message = "";

            while (true) {
                String line = br.readLine();

                if (line != null && line.startsWith("*")) {
                    System.out.println("line: " + line);
                    // start of new command
                    String strLength = line.substring(1);
                    int length = (Integer.parseInt(strLength) * 2);

                    message = line;

                    for (int i = 0; i < length; i++) {
                        line = br.readLine();
                        message += line;
                    }

                    String[] args = parseClientMessage(message);
                    String output = processMessage(args);
                    String formattedOutput = String.format("+%s\r\n", output);
                    clientSocket.getOutputStream().write(formattedOutput.getBytes());
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

    private String processMessage(String[] args) {
        String output = null;
        for (String ss : args) {
            System.out.println("arg: " + ss);
        }
        // idx 0 -> command
        String keyIn = null;
        switch (args[0]) {
            case "PING":
                // Format -> PING
                output = "PONG";
                break;
            case "ECHO":
                // Format -> ECHO s1 s2 ....
                String[] modifiedArr = Arrays.copyOfRange(args, 1, args.length);
                output = String.join(" ", modifiedArr);
                break;
            case "GET":
                // Format -> GET <KEY>
                keyIn = args[1];
                String val = map.getOrDefault(keyIn, null);
                output = val;
                break;
            case "SET":
                // Format -> SET <KEY> <VALUE>....
                keyIn = args[1];
                String[] valInArr = Arrays.copyOfRange(args, 2, args.length);
                if (valInArr.length > 0) {
                    map.put(keyIn, String.join(" ", valInArr));
                    output = "OK";
                } else {
                    output = "No value";
                }
            default:
                String.format("Command %s is not available!", args[0]);
                break;
        }
        return output;
    }

    private String[] parseClientMessage(String message) {
        System.out.println(message);

        /**
         * step 1: get array length
         *
         */

        String messageTemp = message;
        String[] args = null;

        int arrayLength = 0;
        int offset = 0;

        if (messageTemp.startsWith("*")) {
            messageTemp = messageTemp.substring(1);
            for (char c : messageTemp.toCharArray()) {
                if (Character.isDigit(c)) {
                    offset++;
                    arrayLength = (arrayLength * 10) + Character.getNumericValue(c);
                } else {
                    break;
                }
            }

            messageTemp = messageTemp.substring(offset);
            offset = 0;

            args = new String[arrayLength];

            for (int arrayIdx = 0; arrayIdx < arrayLength; arrayIdx++) {
                int bulkStringLength = 0;
                if (messageTemp.startsWith("$")) {
                    messageTemp = messageTemp.substring(1);
                    for (char c : messageTemp.toCharArray()) {
                        if (Character.isDigit(c)) {
                            offset++;
                            bulkStringLength = (bulkStringLength * 10) + Character.getNumericValue(c);
                        } else {
                            break;
                        }
                    }

                    messageTemp = messageTemp.substring(offset);

                    if (bulkStringLength > 0) {
                        args[arrayIdx] = messageTemp.substring(0, bulkStringLength);

                        messageTemp = messageTemp.substring(bulkStringLength);
                    }

                    offset = 0;
                }
            }
        }

        return args;
    }

}

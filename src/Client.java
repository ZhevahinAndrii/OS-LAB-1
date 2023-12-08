import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) throws IOException {

        Scanner scanner = new Scanner(System.in);
        String host = "localhost";
        int port = 8888;

        try {
            Socket clientsocket = new Socket(host, port);
            System.out.println("Connected to " + host + ":" + port);
            try {
                OutputStream output = clientsocket.getOutputStream();
                InputStream input_from_server = clientsocket.getInputStream();
                BufferedReader input = new BufferedReader(new InputStreamReader(input_from_server));

                System.out.println("Enter x:");
                String x = scanner.nextLine() + "\n";

                Double.valueOf(x);
                output.write(x.getBytes());

                char[] answer = new char[1024];
                int read = input.read(answer);
                answer = Arrays.copyOfRange(answer,0,read);
                String message = new String(answer);
                System.out.println(message);
                if(message.contains("error")){
                    System.out.println("Cause of error, please enter x again(retry):");
                    x = scanner.nextLine()+"\n";
                    Double.valueOf(x);
                    output.write(x.getBytes());

                    read = input.read(answer);
                    answer = Arrays.copyOfRange(answer,0,read);
                    message = new String(answer);
                    System.out.println(message);
                }
            } catch (NumberFormatException exception) {
                System.out.println("You entered not a number.Closing your socket...");
                clientsocket.close();
            } catch (SocketException exception) {
                System.out.println("Connection with server socket is not available anymore.Closing your socket...");
                clientsocket.close();
            }
        } catch (ConnectException exception) {
            System.out.println("Can not connect to the given host and port.Check if this server is available.");
        }
        scanner.close();
    }
}

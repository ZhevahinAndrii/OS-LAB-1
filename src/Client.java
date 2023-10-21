import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        String host = "localhost";
        int port = 8080;

        Socket clientsocket = new Socket(host, port);
        System.out.println("Connected to " + host + ":" + port);

        OutputStream output = clientsocket.getOutputStream();
        InputStream input_from_server = clientsocket.getInputStream();
        BufferedReader input = new BufferedReader(new InputStreamReader(input_from_server));
        System.out.println("Enter x:");
        String x = scanner.nextLine() + "\n";

        Double value = Double.valueOf(x);
        output.write(x.getBytes());
        char[] answer = new char[1024];
        int bytes_read = input.read(answer);
        String message = new String(answer);
        System.out.println(message);

    }
}
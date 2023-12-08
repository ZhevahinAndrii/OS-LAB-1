import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.function.Function;

public class ServerCopy {
    private static final Random random = new Random();
    private static final int ATTEMPTS = 5;

    public static Double InspectFunction(Function<Double, Double> function_to_execute, Double x, int client_port)
            throws Exception {
        if (x <= 0 || x >= 1000000)
            throw new Exception("Critical error!!!(x must be greater than zero, but less than 1000000)");
        Double function_result = 0.0;

        int current_attempt = 0;
        while (current_attempt < ATTEMPTS) {
            function_result = function_to_execute.apply(x);
            if (function_result != 0.0)
                break;
            else
                System.out.printf(
                        "Non breaking error for client %d. Time limit exceeded in one of the methods.%n", client_port);
            current_attempt++;
        }
        if (function_result == 0.0) {
            throw new Exception("Critical error!. Time limit exceeded in each attempt in one or both methods");
        } else {
            return function_result;
        }

    }

    public static Double f(Double x) {
        long start = System.currentTimeMillis();
        long end = start + 2000;
        while (System.currentTimeMillis() < end) {
            x += random.nextDouble(10000);
            try {
                Thread.sleep(random.nextInt(5, 15));
            } catch (InterruptedException e) {
                break;
            }
            if (x > 1000000)
                return x;

        }
        return 0.0;
    }
    public static Double g(Double x) {
        long start = System.currentTimeMillis();
        long end = start + 1700;
        while (System.currentTimeMillis() < end) {
            x += random.nextDouble(10000);
            try {
                Thread.sleep(random.nextInt(1, 7));
            } catch (InterruptedException e) {
                break;
            }
            if (x > 2000000)
                return x;
        }
        return 0.0;
    }
    public static double calculations(Double x, int clientSocketPort) throws Exception{
        FutureTask<Double> first_task = new FutureTask<>(
                () -> InspectFunction(ServerCopy::f, x, clientSocketPort));
        FutureTask<Double> second_task = new FutureTask<>(
                () -> InspectFunction(ServerCopy::g, x, clientSocketPort));

        ExecutorService executor = Executors.newFixedThreadPool(2);

        executor.execute(first_task);
        executor.execute(second_task);
        Double resf = 0.0;
        Double resg = 0.0;
        while(true){
            if (first_task.isDone() && resf == 0.0) {
                try{
                    resf = first_task.get();
                    System.out.println("f returned " + resf + " for client "+clientSocketPort);
                }
                catch (Exception e) {
                    second_task.cancel(true);
                    throw new Exception(e.getMessage());

                }
            }
            if (second_task.isDone() && resg == 0.0) {
                try {
                    resg = second_task.get();
                    System.out.println("g returned " + resg+ " for client "+clientSocketPort);
                } catch (Exception e) {
                    first_task.cancel(true);
                    throw new Exception(e.getMessage());
                }
            }
            if (first_task.isDone() && second_task.isDone())
                break;
            try {
                Thread.sleep(100);
            } catch (InterruptedException exception) {
                System.out.println("Thread was interrupted");
            }
        }
        executor.shutdown();
        executor.close();
        return resf + resg;

    }


    public static void main(String[] args) {

        int port = 8888;
        try {
            ServerSocket serverSocket = new ServerSocket(port);

            // opening a server socket to accept connections
            System.out.println("Server is ready for connections");
            long start = System.currentTimeMillis();
            long end = start + 600000;

            while (System.currentTimeMillis() < end) { // server socket may accept connections only for 10 minutes,then
                                                       // you
                                                       // must reload it

                String message_for_user;

                Socket clientSocket = serverSocket.accept(); // getting client's socket that connected to server
                System.out.println("Client connected from " + clientSocket.getPort() + " port");
                int amount_of_tries = 1;
                while(amount_of_tries<=2) {
                    try {

                        InputStream input_from_client = clientSocket.getInputStream(); // getting input and output stream
                        // for
                        // client's socket to inform it about
                        // the
                        // results and to accept the
                        // parameter
                        OutputStream output_from_server = clientSocket.getOutputStream();
                        BufferedReader input = new BufferedReader(new InputStreamReader(input_from_client));

                        String input_value = input.readLine(); // getting a parameter for functions from client
                        Double x = Double.valueOf(input_value);

                        System.out.println("Client with port " + clientSocket.getPort() + " entered number:" + x); // making
                        // sure
                        // what
                        // value
                        // the
                        // client
                        // entered
                        // from a
                        // specific
                        // port
                        try {
                            double calculationsResult = calculations(x, clientSocket.getPort());
                            message_for_user = "Calculations completed successfully for client " + clientSocket.getPort() + ".It returned the value of " + calculationsResult;
                            output_from_server.write(message_for_user.getBytes());
                            break;
                        } catch (Exception e) {
                            message_for_user = "Critical error for client " + clientSocket.getPort() + ":\t\t" + e.getMessage();
                            output_from_server.write(message_for_user.getBytes());
                            amount_of_tries++;
                        }
                    } catch (IOException exception) {
                        System.out.println(
                                "Error during sending info to/reading info from client.Probably, client disconnected.");
                        clientSocket.close();
                        break;
                    }
                }
                clientSocket.close();
            }

            serverSocket.close();
        } catch (IOException | NumberFormatException exception) {
            System.out.println("Can not start server.Please try to reload it.");
        }
    }
}
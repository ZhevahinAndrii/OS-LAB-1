import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.function.Function;

public class Server {
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
                e.printStackTrace();
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
                e.printStackTrace();
            }
            if (x > 2000000)
                return x;
        }
        return 0.0;
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

                String exception_message = "";

                Socket clientSocket = serverSocket.accept(); // getting client's socket that connected to server
                try {
                    System.out.println("Client connected from " + clientSocket.getPort() + " port");

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

                    FutureTask<Double> first_task = new FutureTask<>(
                            () -> InspectFunction(Server::f, x, clientSocket.getPort())); // creating
                    // FutureTasks
                    // that
                    // will
                    // execute
                    // InspectFunction
                    // for
                    // f
                    // and
                    // g
                    // using
                    // x
                    // parameter

                    FutureTask<Double> second_task = new FutureTask<>(
                            () -> InspectFunction(Server::g, x, clientSocket.getPort()));

                    ExecutorService executor = Executors.newFixedThreadPool(2); // creating executor with ThreadPool
                                                                                // size of
                                                                                // 2,
                                                                                // to execute our FutureTasks
                                                                                // asynchronously
                                                                                // in
                                                                                // two different threads
                    executor.execute(first_task); // starting executing futures
                    executor.execute(second_task);

                    Double resf = 0.0;
                    Double resg = 0.0;
                    while (true) { // cycle which checks if any of two futures is completed, and if it is, printing
                                   // its result on server
                        if (first_task.isDone() && resf == 0.0) { // condition to check if first_task is done, and we
                                                                  // still
                                                                  // don't have a result
                            try {

                                resf = first_task.get();
                                System.out.println("f returned " + resf);
                            } catch (Exception e) { // if we got critical error in this task(which executes function f)
                                                    // we
                                                    // get
                                                    // its exception message
                                // and cancel another task, because wait its result is useless

                                exception_message = e.getMessage();
                                second_task.cancel(true);
                                break;
                            }
                        }
                        // same logic as with the first task
                        if (second_task.isDone() && resg == 0.0) {
                            try {
                                resg = second_task.get();
                                System.out.println("g returned " + resg);
                            } catch (Exception e) {
                                exception_message = e.getMessage();
                                first_task.cancel(true);
                                break;
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
                    double result = resf + resg;
                    String output_for_user = "Function f=" + resf + ",Function g=" + resg + "\nSum of results="
                            + result;
                    if (Objects.equals(exception_message, "")) {
                        output_from_server.write(output_for_user.getBytes());
                        System.out.println("Functions completed successfully for client " + clientSocket.getPort());
                    } else {
                        output_from_server.write(exception_message.getBytes());
                        System.out.println(
                                "Critical error for client " + clientSocket.getPort() + ":" + exception_message);
                    }
                    clientSocket.close();
                } catch (IOException exception) {
                    System.out.println(
                            "Error during sending info to/reading info from client.Probably, client disconnected.");
                    clientSocket.close();
                }

            }
            serverSocket.close();
        } catch (IOException | NumberFormatException exception) {
            System.out.println("Can not start server.Please try to reload it.");

        }

    }

}
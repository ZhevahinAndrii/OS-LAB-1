import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.function.Function;

public class Server {
    private static Random random = new Random();
    private static int ATTEMPTS = 5;
    private static String exception_message = "";

    public static Double InspectFunction(Function<Double, Double> func, Double x) throws Exception {
        Double res = 0.0;

        int current_attempt = 0;
        while (current_attempt < ATTEMPTS) {
            res = func.apply(x);
            if (res != 0.0)
                break;
            else
                System.out.println("Non breaking error.Time limit exceeded in one of the methods.");
            current_attempt++;
        }
        if (res == 0.0) {
            throw new Exception("Time limit exceeded in each attempt in one or both methods");
        } else {
            return res;
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
        long end = start + 2000;
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

    public static int port = 8080;

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException, Exception {
        System.out.println("Server is ready for connections");

        ServerSocket serverSocket = new ServerSocket(port);
        while (true) {
            exception_message = "";
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected from " + clientSocket.getPort() + " port");

            InputStream input_from_client = clientSocket.getInputStream();
            OutputStream output_from_server = clientSocket.getOutputStream();
            BufferedReader input = new BufferedReader(new InputStreamReader(input_from_client));

            String input_value = input.readLine();
            Double x = Double.valueOf(input_value);
            System.out.println("Client with port " + clientSocket.getPort() + " entered number:" + x);
            // FutureTask<Double> first = new FutureTask<Double>(() -> InspectFunction((arg)
            // -> f(arg), x));
            // FutureTask<Double> second = new FutureTask<Double>(() ->
            // InspectFunction((arg) -> g(arg), x));

            FutureTask<Double> third = new FutureTask<Double>(() -> {
                try {
                    return InspectFunction((arg) -> f(arg), x);
                } catch (Exception e) {
                    exception_message = e.getMessage();
                    return 0.0;
                }
            });

            FutureTask<Double> fourth = new FutureTask<Double>(() -> {
                try {
                    return InspectFunction((arg) -> g(arg), x);
                } catch (Exception e) {
                    exception_message = e.getMessage();
                    return 0.0;
                }
            });
            ExecutorService executor = Executors.newFixedThreadPool(2);

            executor.execute(third);
            executor.execute(fourth);
            Double res1 = 0.0;
            Double res2 = 0.0;
            while (true) {

                if (third.isDone() && res1 == 0.0) {
                    try {
                        res1 = third.get();
                        System.out.println("f returned " + res1);
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }

                if (fourth.isDone() && res2 == 0.0) {
                    try {
                        res2 = fourth.get();
                        System.out.println("g returned " + res2);
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
                if (third.isDone() && fourth.isDone())
                    break;
                Thread.sleep(100);
            }

            if (exception_message == "") {
                String answer = "Value of f function:" + res1 + ", value of g function:" + res2 + ";\n Sum = "
                        + (res1 + res2);
                output_from_server.write(answer.getBytes());
            }

        }
    }

}

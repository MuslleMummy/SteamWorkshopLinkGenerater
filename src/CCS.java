import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class CCS {

    public String SERVICE_DISCOVER_MSG = "CCS DISCOVER";
    public String SERVICE_RESPONSE_MSG = "CCS FOUND";

    private final int serverPort;
    private final ExecutorService taskExecutor = Executors.newCachedThreadPool();
    private final ServerStats serverStats = new ServerStats();

    public CCS(int port) {
        this.serverPort = port;
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Error: Port argument missing.");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("Error: Invalid port number.");
            return;
        }

        CCS serverInstance = new CCS(port);
        serverInstance.launch();
    }

    public void launch() {
        Thread udpService = new Thread(this::initUDPService);
        Thread tcpService = new Thread(this::initTCPService);
        Thread statsLogger = new Thread(this::logServerStats);

        udpService.start();
        tcpService.start();
        statsLogger.start();

        try {
            udpService.join();
            tcpService.join();
            statsLogger.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void initUDPService() {
        try (DatagramSocket udpSocket = new DatagramSocket(serverPort)) {
            byte[] receiveBuffer = new byte[1024];
            System.out.println("UDP Service is running on port " + serverPort);

            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                udpSocket.receive(receivePacket);

                String receivedData = new String(receivePacket.getData(), 0, receivePacket.getLength());
                if (receivedData.startsWith(SERVICE_DISCOVER_MSG)) {
                    byte[] responseData = SERVICE_RESPONSE_MSG.getBytes();
                    DatagramPacket responsePacket = new DatagramPacket(
                            responseData, responseData.length, receivePacket.getAddress(), receivePacket.getPort());
                    udpSocket.send(responsePacket);
                    System.out.println("Service discovery request answered.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initTCPService() {
        try (ServerSocket tcpSocket = new ServerSocket(serverPort)) {
            System.out.println("TCP Service is active on port " + serverPort);

            while (true) {
                Socket clientConnection = tcpSocket.accept();
                taskExecutor.submit(() -> handleConnection(clientConnection));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleConnection(Socket connectionSocket) {
        serverStats.incrementClientCount();

        try (BufferedReader inputReader = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
             PrintWriter outputWriter = new PrintWriter(connectionSocket.getOutputStream(), true)) {

            String clientRequest;
            while ((clientRequest = inputReader.readLine()) != null) {
                String response = interpretRequest(clientRequest);
                outputWriter.println(response);
                System.out.println("Client Message: " + clientRequest + " | Response Sent: " + response);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                connectionSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String interpretRequest(String userRequest) {
        String[] components = userRequest.split(" ");
        if (components.length != 3) {
            serverStats.countInvalidCommands();
            return "COMMAND_ERROR";
        }

        String operationType = components[0];
        int operandOne, operandTwo;
        try {
            operandOne = Integer.parseInt(components[1]);
            operandTwo = Integer.parseInt(components[2]);
        } catch (NumberFormatException e) {
            serverStats.countInvalidCommands();
            return "COMMAND_ERROR";
        }

        int computedValue;
        try {
            switch (operationType) {
                case "ADD":
                    computedValue = operandOne + operandTwo;
                    serverStats.recordOperation("ADD", computedValue);
                    return String.valueOf(computedValue);
                case "SUB":
                    computedValue = operandOne - operandTwo;
                    serverStats.recordOperation("SUBTRACT", computedValue);
                    return String.valueOf(computedValue);
                case "MUL":
                    computedValue = operandOne * operandTwo;
                    serverStats.recordOperation("MULTIPLY", computedValue);
                    return String.valueOf(computedValue);
                case "DIV":
                    if (operandTwo == 0) throw new ArithmeticException("Division by zero");
                    computedValue = operandOne / operandTwo;
                    serverStats.recordOperation("DIVIDE", computedValue);
                    return String.valueOf(computedValue);
                default:
                    serverStats.countInvalidCommands();
                    return "COMMAND_ERROR";
            }
        } catch (ArithmeticException e) {
            serverStats.countInvalidCommands();
            return "MATH_ERROR";
        }
    }

    private void logServerStats() {
        while (true) {
            try {
                Thread.sleep(10000);
                serverStats.outputStatistics();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private static class ServerStats {
        private final Map<String, Integer> operationCounts = new ConcurrentHashMap<>();
        private int invalidCommandCount = 0;
        private int connectedClients = 0;
        private int processedRequests = 0;
        private int accumulatedResults = 0;

        public synchronized void recordOperation(String operationName, int result) {
            operationCounts.merge(operationName, 1, Integer::sum);
            processedRequests++;
            accumulatedResults += result;
        }

        public synchronized void countInvalidCommands() {
            invalidCommandCount++;
        }

        public synchronized void incrementClientCount() {
            connectedClients++;
        }

        public synchronized void outputStatistics() {
            System.out.println("\n*** Server Statistics ***");
            System.out.println("Active Clients: " + connectedClients);
            System.out.println("Total Requests Processed: " + processedRequests);
            System.out.println("Sum of Results: " + accumulatedResults);
            System.out.println("Invalid Commands Encountered: " + invalidCommandCount);
            System.out.println("Operations Summary: " + operationCounts);
        }
    }
}


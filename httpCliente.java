import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class httpCliente {
    private static int promptForLineNumber(Scanner scanner) {
        // Solicitar ao usuário um número de linha
        int lineNumber;
        while (true) {
            System.out.print("Enter line number: ");
            try {
                lineNumber = Integer.parseInt(scanner.nextLine());
                if (lineNumber < 1) {
                    System.out.println("Line number must be greater than or equal to 1.");
                } else {
                    break;
                }
            } catch (NumberFormatException e) {
                System.out.println("HTTP/1.1 400 Bad Request");
            }
        }
        return lineNumber;
    }
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
    
        while (true) {
            System.out.print("Enter request type (GET, POST, PUT) [or type 'exit' to quit]: ");
            String requestType = scanner.nextLine().trim().toUpperCase();
    
            if ("EXIT".equals(requestType)) {
                break;
            }
    
            try {
                URL url = new URL("http://localhost:8080");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    
                if ("POST".equals(requestType)) {
                     // Enviar uma solicitação POST
                    System.out.print("Enter data to write: ");
                    String data = scanner.nextLine();
                    sendPostRequest(connection, data);
                } else if ("GET".equals(requestType)) {
                     // Enviar uma solicitação GET
                    int lineNumber = promptForLineNumber(scanner);
                    sendGetRequest(connection, lineNumber);
                } else if ("PUT".equals(requestType)) {
                     // Enviar uma solicitação PUT
                    int lineNumber = promptForLineNumber(scanner);
                    System.out.print("Enter updated data: ");
                    String updatedData = scanner.nextLine();
                    sendPutRequest(connection, lineNumber, updatedData);
                } else {
                    System.out.println("Invalid request type. Use 'GET', 'POST', 'PUT', or 'exit'.");
                }
    
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    
        scanner.close();
    }

    private static void sendPostRequest(HttpURLConnection connection, String data) throws Exception {
        // Enviar uma solicitação POST
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        if (data.length() > 100) {
            System.out.println("HTTP/1.1 413 Payload Too Large");
            return;
        }
        try (OutputStream os = connection.getOutputStream()) {
            os.write(data.getBytes());
        }
    
        int responseCode = connection.getResponseCode();
        
        if (responseCode == 400) {
            System.out.println("HTTP/1.1 400 " + connection.getResponseMessage());
        } else if (responseCode == 428) {
            System.out.println("HTTP/1.1 428 Precondition Required");
        } else {
            System.out.println("HTTP/1.1 " + responseCode + " Created");
        }
    }

    private static void sendGetRequest(HttpURLConnection connection, int lineNumber) throws Exception {
        // Enviar uma solicitação GET
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Line-Number", String.valueOf(lineNumber));

        int responseCode = connection.getResponseCode();
        
        if (responseCode == 200) {
            System.out.println("HTTP/1.1 " + responseCode + " OK");
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            reader.close();
        } else if (responseCode == 404) {
            System.out.println("HTTP/1.1 404 " + connection.getResponseMessage());
        } else if (responseCode == 428) {
            System.out.println("HTTP/1.1 428 Precondition Required");
        }
    }

    private static void sendPutRequest(HttpURLConnection connection, int lineNumber, String data) throws Exception {
        // Enviar uma solicitação PUT
        connection.setRequestMethod("PUT");
        connection.setDoOutput(true);
        connection.setRequestProperty("Line-Number", String.valueOf(lineNumber));
        if (data.length() > 100) {
            System.out.println("HTTP/1.1 413 Payload Too Large");
            return;
        }
    
        try (OutputStream os = connection.getOutputStream()) {
            os.write(data.getBytes());
        }

        int responseCode = connection.getResponseCode();
        

        if (responseCode == 404) {
            System.out.println("HTTP/1.1 404 " + connection.getResponseMessage());
        } else if (responseCode == 400) {
            System.out.println("HTTP/1.1 400 " + connection.getResponseMessage());
        } else {
            System.out.println("HTTP/1.1 " + responseCode + " OK");
        }
    }

    static class NotFoundException extends RuntimeException {
        // Exceção personalizada para Not Found
        public NotFoundException(String message) {
            super(message);
        }
    }

}

// GET - 200 | 428 | 404 | 400
// POST - 201 | 428 | 413 | 400
// PUT - 200 | 413 | 404 | 400
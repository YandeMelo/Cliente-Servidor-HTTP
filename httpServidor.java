import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;

public class httpServidor {
    public static void main(String[] args) throws IOException {
        int port = 8080;
        // Criação do servidor HTTP na porta especificada
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        // Definição do manipulador para as requisições
        server.createContext("/", new MyHandler());
        server.setExecutor(null);
        System.out.println("Server is listening on port " + port);
        server.start();
    }

    static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestMethod = exchange.getRequestMethod();
    
            if ("GET".equals(requestMethod)) {
                // Tratamento de solicitação GET
                try {
                    int lineNumber = Integer.parseInt(exchange.getRequestHeaders().getFirst("Line-Number"));
                    if (lineNumber < 1) {
                        sendResponse(exchange, "Line number must be greater than or equal to 1.", 400);
                    } else {
                        handleGetRequest(exchange, lineNumber);
                    }
                } catch (NumberFormatException e) {
                    sendResponse(exchange, "Invalid line number.", 400);
                }
            } else if ("POST".equals(requestMethod)) {
                // Tratamento de solicitação POST
                handlePostRequest(exchange);
            } else if ("PUT".equals(requestMethod)) {
                // Tratamento de solicitação PUT
                try {
                    int lineNumber = Integer.parseInt(exchange.getRequestHeaders().getFirst("Line-Number"));
                    if (lineNumber < 1) {
                        sendResponse(exchange, "Line number must be greater than or equal to 1.", 400);
                    } else {
                        if (lineNumber > countLines("data.txt")) {
                            sendResponse(exchange, "Line does not exist.", 404);
                        } else {
                            handlePutRequest(exchange, lineNumber);
                        }
                    }
                } catch (NumberFormatException e) {
                    sendResponse(exchange, "Invalid line number.", 400);
                }
            } else {
                sendResponse(exchange, "Unsupported request method.", 400);
            }
        }

        private void handleGetRequest(HttpExchange exchange, int lineNumber) throws IOException {
            // Manipulação da solicitação GET
            int lineCount = countLines("data.txt");
            if (lineCount == 0) {
                sendResponse(exchange, "File is empty.", 428); // Return 428 for empty file
            } else {
                String lineContent = readLineFromFile("data.txt", lineNumber);
                if (lineContent == null) {
                    sendResponse(exchange, "Line does not exist.", 404); // Return 404 for non-existing line
                } else if (lineContent.isEmpty()) {
                    sendResponse(exchange, "Line is empty.", 404); // Return 404 for empty line
                } else {
                    sendResponse(exchange, lineContent, 200);
                }
            }
        }
        
        

        private void handlePostRequest(HttpExchange exchange) throws IOException {
            // Manipulação da solicitação POST
            int maxLines = 10; // Defina o número máximo de linhas permitidas no arquivo
            int currentLines = countLines("data.txt");
    
            if (currentLines >= maxLines) {
            sendResponse(exchange, "Maximum number of lines reached. Cannot add more data.", 428);
            return;
            }
            InputStream requestBody = exchange.getRequestBody();
            String requestData = new BufferedReader(new InputStreamReader(requestBody)).readLine();
        
            if (requestData == null || requestData.isEmpty()) {
                sendResponse(exchange, "Data for writing is missing.", 400);
                return;
            }
            if (requestData.length() > 100) {
                sendResponse(exchange, "Data too long. Maximum allowed length is 100 characters.", 413);
                return;
            }
            appendToFile("data.txt", requestData);
            sendResponse(exchange, "Data written to file.", 201);
        }

        private void handlePutRequest(HttpExchange exchange, int lineNumber) throws IOException {
            // Manipulação da solicitação PUT
            InputStream requestBody = exchange.getRequestBody();
            String requestData = new BufferedReader(new InputStreamReader(requestBody)).readLine();

            if (requestData == null || requestData.isEmpty()) {
                sendResponse(exchange, "Data for updating is missing.", 400);
                return;
            }

            if (requestData.length() > 100) {
                sendResponse(exchange, "Data too long. Maximum allowed length is 100 characters.", 413);
                return;
            }

            if (updateLineInFile("data.txt", lineNumber, requestData)) {
                sendResponse(exchange, "Line updated.", 200);
            } else {
                sendResponse(exchange, "Line does not exist.", 404);
            }
        }

        private void sendResponse(HttpExchange exchange, String response, int statusCode) throws IOException {
            // Envio de resposta HTTP com código de status e mensagem
            exchange.sendResponseHeaders(statusCode, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    private static String readLineFromFile(String fileName, int lineNumber) throws IOException {
        // Leitura de uma linha específica de um arquivo
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String lineContent = null;
            for (int i = 0; i < lineNumber; i++) {
                lineContent = reader.readLine();
                if (lineContent == null) {
                    break;
                }
            }
            return lineContent;
        }
    }

    private static boolean updateLineInFile(String fileName, int lineNumber, String content) throws IOException {
        // Atualização de uma linha específica de um arquivo
        File inputFile = new File(fileName);
        File tempFile = new File("temp.txt");
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {

            String line;
            int currentLineNumber = 1;
            int numberOfLines = countLines(fileName);

            while ((line = reader.readLine()) != null) {
                if (currentLineNumber == lineNumber) {
                    writer.write(content);
                } else {
                    writer.write(line);
                }
                currentLineNumber++;

                if (currentLineNumber > numberOfLines) {
                    break;
                } else {
                
                writer.newLine(); 
                }
                
                
            }
        }
        return inputFile.delete() && tempFile.renameTo(inputFile);
    }

    private static int countLines(String fileName) throws IOException {
        // Contagem do número de linhas em um arquivo
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            int lineCount = 0;
            while (reader.readLine() != null) {
                lineCount++;
            }
            return lineCount;
        }
    }    

    private static void appendToFile(String fileName, String content) throws IOException {
         // Anexar conteúdo a um arquivo
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))) {
            if (!content.isEmpty()) {
                if (new File(fileName).length() > 0) {
                    writer.newLine();
                }
                writer.write(content);
            }
        }
    }
}

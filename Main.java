import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class Main {

    private static final String LINE_BREAK = "\r\n";

    public static void main(String[] args) throws IOException {
        // https://stackoverflow.com/a/10788242/339561
        try (ServerSocket serverSocket = new ServerSocket(8080);) {
            InetAddress inet = serverSocket.getInetAddress();
            System.out.println("HostAddress=" + inet.getHostAddress());
            System.out.println("HostName=" + inet.getHostName());
            System.out.println("Port=" + serverSocket.getLocalPort());

            // repeatedly wait for connections, and process
            while (true) {
                Socket clientSocket = serverSocket.accept();

                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

                String s;
                Url url = null;
                ResponseType responseType = ResponseType.HTML;
                while ((s = in.readLine()) != null) {
                    System.out.println(s);
                    if (s.startsWith("GET")) {
                        url = new Url(s);
                    }
                    if (s.toLowerCase().startsWith("accept: ")) {
                        responseType = ResponseType.getFromAccept(s); 
                    }
                    if (s.isEmpty()) {
                        break;
                    }
                }
                System.out.println(url);

                Response response;
                if (url == null) {
                    response = handleBadRequest();
                } else {
                    response = switch (url.getBasePath()) {
                        case "/" -> handleHomePage(url, responseType);
                        case "/country/name" -> handleCountry(url, responseType, false);
                        case "/country/name-async" -> handleCountry(url, responseType, true);
                        default -> handleBadRequest();
                    };
                }

                response.type = responseType;

                write(out, response);

                out.close();
                in.close();
                clientSocket.close();
            }
        }
    }

    private static class Response {
        String body;
        ResponseStatusCode statusCode;
        ResponseType type;

        public Response(String body, Main.ResponseStatusCode statusCode, ResponseType type) {
            this.body = body;
            this.statusCode = statusCode;
            this.type = type;
        }
    }

    private static List<Country> getByName(String name, boolean async) {
        try {
            String path = "https://restcountries.eu/rest/v2/name/" + name;
            HttpResponse<String> response = fetchUrl(path, async);
            return ParseJson.jsonToClass(response.body(), Country.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static HttpClient client = HttpClient.newBuilder().version(Version.HTTP_1_1)
            .followRedirects(Redirect.NORMAL).connectTimeout(Duration.ofSeconds(20)).build();

    private static HttpResponse<String> fetchUrl(String url, boolean async)
            throws ExecutionException, IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        // TODO probably do async call and immediately call get
        // makes the async call useless
        HttpResponse<String> response = async ? client.sendAsync(request, BodyHandlers.ofString()).get()
                : client.send(request, BodyHandlers.ofString());
        System.out.println(response.statusCode());
        System.out.println(response.body());

        return response;
    }

    private static Response handleBadRequest() {
        String body = "Invalid Request. URL Not Found";
        return new Response(body, ResponseStatusCode.BAD_REQUEST, ResponseType.HTML);
    }

    private static Response handleCountry(Url url, ResponseType responseType, boolean async) {
        String path = url.getFullPath();
        String countryName = path.substring(path.lastIndexOf('/') + 1);
        List<Country> countries = getByName(countryName, async);
        String body = switch (responseType) {
            case JSON -> countries.toString();
            default -> """
                    <!DOCTYPE html>
                    <html>
                    <title>Exemple</title>
                    <meta charset="ISO-8859-1">
                    <body>
                        <div>%s</div>
                    </body>
                    </html>
                """.formatted(countries);
        };
        return new Response(body, ResponseStatusCode.OK, ResponseType.HTML);
    }

    private static Response handleHomePage(Url url, ResponseType responseType) {
        String body = """
                    <!DOCTYPE html>
                    <html>
                    <title>Exemple</title>
                    <body>
                        <div>Home Page</div>
                    </body>
                    </html>
                """;
        return new Response(body, ResponseStatusCode.OK, ResponseType.HTML);
    }

    private static final DateFormat dateFormat = new SimpleDateFormat("E, d MMM y HH:mm:ss z", Locale.ENGLISH);

    private static void write(BufferedWriter out, Response response) throws IOException {
        var now = Calendar.getInstance();
        var lastModified = Calendar.getInstance();
        lastModified.add(Calendar.DAY_OF_MONTH, -3);
        var expires = Calendar.getInstance();
        expires.add(Calendar.MONTH, 3);
        out.write(response.statusCode + LINE_BREAK);
        out.write("Date: " + dateFormat.format(now.getTime()) + LINE_BREAK);
        out.write("Server: Vanilla Java/0.0.1\r\n");
        out.write(response.type + LINE_BREAK);
        out.write("Content-Length: " + (response.body.length() + 1) + LINE_BREAK);
        out.write("Expires: " + dateFormat.format(expires.getTime()) + LINE_BREAK);
        out.write("Last-modified: " + dateFormat.format(lastModified.getTime()) + LINE_BREAK);
        out.write(LINE_BREAK);
        out.write(response.body);
    }

    enum ResponseStatusCode {
        OK, BAD_REQUEST,;

        @Override
        public String toString() {
            return switch (this) {
                case OK -> "HTTP/1.1 200 OK";
                case BAD_REQUEST -> "HTTP/1.1 400 BAD REQUEST";
                default -> "???";
            };
        }
    }

    enum ResponseType {
        HTML, JSON;

        @Override
        public String toString() {
            return switch (this) {
                case HTML -> "Content-Type: text/html";
                case JSON -> "Content-Type: application/json";
                default -> "???";
            };
        }

        public static Main.ResponseType getFromAccept(String s) {
            return switch (s.split(" ")[1]) {
                case "application/json" -> JSON;
                default -> HTML;
            };
        }
    }
}

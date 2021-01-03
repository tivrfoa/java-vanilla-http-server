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

public class Main {

    private static final String OK_HEADER = "HTTP/1.1 200 OK\r\n";

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
                while ((s = in.readLine()) != null) {
                    if (s.startsWith("GET")) {
                        url = new Url(s);
                    }
                    System.out.println(s);
                    if (s.isEmpty()) {
                        break;
                    }
                }
                System.out.println(url);

                String content = switch (url.getBasePath()) {
                    case "/" -> handleHomePage(url.getQueryParams());
                    case "/country/name" -> handleCountry(url);
                    default -> handleBadRequest();
                };

                write(out, content);

                out.close();
                in.close();
                clientSocket.close();
            }
        }
    }

    private static List<Country> getByName(String name) {
        try {
            String path = "https://restcountries.eu/rest/v2/name/" + name;
            HttpResponse<String> response = fetchUrl(path);
            return ParseJson.jsonToClass(response.body(), Country.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static HttpClient client = HttpClient.newBuilder().version(Version.HTTP_1_1)
            .followRedirects(Redirect.NORMAL).connectTimeout(Duration.ofSeconds(20)).build();

    private static HttpResponse<String> fetchUrl(String url) throws IOException, InterruptedException {
        /*return new HttpResponse<String>() {

            @Override
            public int statusCode() {
                return 200;
            }

            @Override
            public HttpRequest request() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Optional<HttpResponse<String>> previousResponse() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public HttpHeaders headers() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String body() {
                return ParseJson.FAKE_JSON;
            }

            @Override
            public Optional<SSLSession> sslSession() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public URI uri() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Version version() {
                // TODO Auto-generated method stub
                return null;
            }

        };*/
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        System.out.println(response.statusCode());
        System.out.println(response.body());

        return response;
    }

    private static String handleBadRequest() {
        return "Invalid Request. URL Not Found";
    }

    private static String handleCountry(Url url) {
        String path = url.getFullPath();
        String countryName = path.substring(path.lastIndexOf('/') + 1);
        List<Country> countries = getByName(countryName);
        return """
                <!DOCTYPE html>
                <html>
                <title>Exemple</title>
                <meta charset="ISO-8859-1">
                <body>
                    <div>%s</div>
                </body>
                </html>
            """.formatted(countries);
    }

    private static String handleHomePage(Map<String, String> queryParams) {
        return """
                <!DOCTYPE html>
                <html>
                <title>Exemple</title>
                <body>
                    <div>Home Page</div>
                </body>
                </html>
            """;
    }

    private static final DateFormat dateFormat = new SimpleDateFormat("E, d MMM y HH:mm:ss z", Locale.ENGLISH);

    private static void write(BufferedWriter out, String content) throws IOException {
        var now = Calendar.getInstance();
        var lastModified = Calendar.getInstance();
        lastModified.add(Calendar.DAY_OF_MONTH, -3);
        var expires = Calendar.getInstance();
        expires.add(Calendar.MONTH, 3);
        out.write(OK_HEADER);
        out.write("Date: " + dateFormat.format(now.getTime()) + "\r\n");
        out.write("Server: Vanilla Java/0.0.1\r\n");
        out.write("Content-Type: text/html\r\n");
        out.write("Content-Length: " + content.length() + "\r\n");
        out.write("Expires: " + dateFormat.format(expires.getTime()) + "\r\n");
        out.write("Last-modified: " + dateFormat.format(lastModified.getTime()) + "\r\n");
        out.write("\r\n");
        out.write(content);
    }
}

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

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
                while ((s = in.readLine()) != null) {
                    System.out.println(s);
                    if (s.isEmpty()) {
                        break;
                    }
                }

                String content = """
                    <!DOCTYPE html>
                    <html>
                    <TITLE>Exemple</TITLE>
                    <body>
                    <p>Ceci est une page d'exemple.</p>
                    </body>
                    </html>
                """;
                write(out, content);

                out.close();
                in.close();
                clientSocket.close();
            }
        }
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

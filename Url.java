import java.util.HashMap;
import java.util.Map;

public class Url {

    private String host;
    private String fullPath;
    private Map<String, String> queryParams = new HashMap<>();;
    private String basePath; // does not contain the value after last '/'

    public Url(String url) {
        String[] tmp = url.split(" ")[1].split("\\?");
        fullPath = tmp[0];
        if (fullPath.charAt(fullPath.length() - 1) == '/') {
            basePath = fullPath;
        } else {
            basePath = fullPath.substring(0, fullPath.lastIndexOf('/'));
        }
        if (tmp.length == 1)
            return;
        String[] params = tmp[1].split("&");
        for (String p : params) {
            String[] keyValue = p.split("=");
            queryParams.put(keyValue[0], keyValue[1]);
        }
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getFullPath() {
        return fullPath;
    }

    public void setFullPath(String fullPath) {
        this.fullPath = fullPath;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(Map<String, String> queryParams) {
        this.queryParams = queryParams;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    @Override
    public String toString() {
        return "Url [basePath=" + basePath + ", fullPath=" + fullPath + ", host=" + host + ", queryParams="
                + queryParams + "]";
    }
}

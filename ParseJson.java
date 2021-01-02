import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * I really don't want to create a json parser ... :( There are a lot of cases
 * to handle. These are valid: [ [ "ola", [] ] ] After '{' there must be a
 * string (key), and after the key there must be a colon ':'
 * 
 * Parses a String to a class
 * 
 * Case matters ...
 * 
 * [{"name":"Brazil","topLevelDomain":[".br"],"alpha2Code":"BR","alpha3Code":"BRA","callingCodes":["55"],"capital":"Brasília","altSpellings":["BR","Brasil","Federative
 * Republic of Brazil","República Federativa do
 * Brasil"],"region":"Americas","subregion":"South
 * America","population":206135893,"latlng":[-10.0,-55.0],"demonym":"Brazilian","area":8515767.0,"gini":54.7,"timezones":["UTC-05:00","UTC-04:00","UTC-03:00","UTC-02:00"],"borders":["ARG","BOL","COL","GUF","GUY","PRY","PER","SUR","URY","VEN"],"nativeName":"Brasil","numericCode":"076","currencies":[{"code":"BRL","name":"Brazilian
 * real","symbol":"R$"}],"languages":[{"iso639_1":"pt","iso639_2":"por","name":"Portuguese","nativeName":"Português"}],"translations":{"de":"Brasilien","es":"Brasil","fr":"Brésil","ja":"ブラジル","it":"Brasile","br":"Brasil","pt":"Brasil","nl":"Brazilië","hr":"Brazil","fa":"برزیل"},"flag":"https://restcountries.eu/data/bra.svg","regionalBlocs":[{"acronym":"USAN","name":"Union
 * of South American
 * Nations","otherAcronyms":["UNASUR","UNASUL","UZAN"],"otherNames":["Unión de
 * Naciones Suramericanas","União de Nações Sul-Americanas","Unie van
 * Zuid-Amerikaanse Naties","South American Union"]}],"cioc":"BRA"}]
 */
public class ParseJson {

    public static final String FAKE_JSON = """
                [{"name":"Brazil","topLevelDomain":[".br"],"alpha2Code":"BR","alpha3Code":"BRA","callingCodes":["55"],"capital":"Brasília","altSpellings":["BR","Brasil","Federative Republic of Brazil","República Federativa do Brasil"],"region":"Americas","subregion":"South America","population":206135893,"latlng":[-10.0,-55.0],"demonym":"Brazilian","area":8515767.0,"gini":54.7,"timezones":["UTC-05:00","UTC-04:00","UTC-03:00","UTC-02:00"],"borders":["ARG","BOL","COL","GUF","GUY","PRY","PER","SUR","URY","VEN"],"nativeName":"Brasil","numericCode":"076","currencies":[{"code":"BRL","name":"Brazilian real","symbol":"R$"}],"languages":[{"iso639_1":"pt","iso639_2":"por","name":"Portuguese","nativeName":"Português"}],"translations":{"de":"Brasilien","es":"Brasil","fr":"Brésil","ja":"ブラジル","it":"Brasile","br":"Brasil","pt":"Brasil","nl":"Brazilië","hr":"Brazil","fa":"برزیل"},"flag":"https://restcountries.eu/data/bra.svg","regionalBlocs":[{"acronym":"USAN","name":"Union of South American Nations","otherAcronyms":["UNASUR","UNASUL","UZAN"],"otherNames":["Unión de Naciones Suramericanas","União de Nações Sul-Americanas","Unie van Zuid-Amerikaanse Naties","South American Union"]}],"cioc":"BRA"}]
            """;

    static {
        Tests.run();
    }

    private static class Json {
        final Map<String, Object> map;

        public Json(Map<String, Object> map) {
            this.map = map;
        }
    }

    private static class JsonObject {
        String str;

        public JsonObject(String str) {
            this.str = str;
        }
    }

    public static int findMatchingChar(String s, char openChar, char closingChar, int startPos) {
        int cnt = 0;
        for (int i = startPos; i < s.length(); ++i) {
            if (s.charAt(i) == closingChar) {
                if (cnt == 0)
                    return i;
                --cnt;
            } else if (s.charAt(i) == openChar) {
                ++cnt;
            }
        }

        throw new RuntimeException("Invalid JSON");
        // return -1;
    }

    private static final class Key {
        String key;
        int startPosition, endPosition;

        public Key(String key, int startPosition, int endPosition) {
            this.key = key;
            this.startPosition = startPosition;
            this.endPosition = endPosition;
        }
    }

    private static final class Value {
        Object value;
        int startPosition, endPosition;

        public Value(Object value, int startPosition, int endPosition) {
            this.value = value;
            this.startPosition = startPosition;
            this.endPosition = endPosition;
        }
    }

    private static final class KeyValue {
        Key key;
        Value value;

        @Override
        public String toString() {
            return "KeyValue [key=" + key + ", value=" + value + "]";
        }
    }

    /**
     * This a very basic parser. It does not handle many cases. It does not check if
     * there are values between '[' and '{'
     * 
     * @param <T>
     * @param json
     * @param clazz
     * @return
     */
    public static <T> List<T> jsonToClass(String json, Class<T> clazz) {
        List<T> list = new ArrayList<>();

        json = json.trim();

        // JSON must start with '[' or '{'
        if (json.charAt(0) != '[' && json.charAt(0) != '{')
            throw new RuntimeException("Invalid start of JSON");

        if (json.charAt(0) == '[') {
            int mIdx = findMatchingChar(json, '[', ']', 1);

            int idx = 1;
            while (true) {
                while (true) {
                    char c = json.charAt(idx);
                    if (c == '\n' || c == '\r' || c == ' ' || c == '\t') { // TODO where else?
                        ++idx;
                    } else {
                        break;
                    }
                }

                if (json.charAt(idx) == '"') {
                    // TODO
                } else if (Character.isDigit(json.charAt(idx))) {
                    // TODO
                } else {
                    // int tmp = json.indexOf('{', idx);
                    boolean tmp = json.charAt(idx) == '{';
                    if (tmp) {
                        mIdx = findMatchingChar(json, '{', '}', idx + 1);
                        Object obj = null;
                        try {
                            obj = clazz.getConstructor().newInstance();
                        } catch (Exception e) { e.printStackTrace(); }

                        KeyValue keyValue = getKeyValue(json, idx + 1);
                        System.out.println(keyValue);
                        if (keyValue.key == null) { // empty object
                            list.add(clazz.cast(obj));
                        } else {

                        }
                    } else {
                        throw new RuntimeException("Invalid JSON");
                    }
                }
                
                idx = json.indexOf(',', mIdx + 1);
                if (idx == -1) break; // end of array
            }
        }

        for (int i = 0; i < json.length(); ++i) {
            // get key
            // get value
        }

        Object o = null;

        Json[] jj = { new Json(Map.of("numericCode", 10, "name", "Leandro")) };

        try {
            for (Json j : jj) {
                var map = j.map;

                o = clazz.getConstructor().newInstance();

                for (var entry : map.entrySet()) {

                    for (var field : clazz.getFields())
                        if (entry.getKey().equals(field.getName())) {
                            // System.out.println(field.getType() + " " + field.getGenericType());
                            // int int
                            // class java.lang.String class java.lang.String
                            // class java.lang.String class java.lang.String
                            // class java.lang.String class java.lang.String
                            // interface java.util.List java.util.List<Country$Currency>
                            System.out.println(field.getName());
                            field.set(o, entry.getValue());
                        }
                }

                list.add(clazz.cast(o));
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    private static ParseJson.KeyValue getKeyValue(String json, int startPosition) {
        KeyValue keyValue = new KeyValue();

        int mIdx = -1;
        // find key
        for (int i = startPosition; i < json.length(); ++i) {
            char c = json.charAt(i);
            if (Character.isDigit(c) || Character.isLetter(c))
                throw new RuntimeException("Invalid JSON");
            if (c == '"') {
                mIdx = json.indexOf('"', i + 1);
                if (mIdx == -1) throw new RuntimeException("Invalid JSON");
                keyValue.key = new Key(json.substring(i + 1, mIdx), i + 1, mIdx - 1);
                break;
            }
        }
        if (keyValue.key == null) return keyValue;
        
        // find value
        for (int i = mIdx + 1; i < json.length(); ++i) {
            char c = json.charAt(i);
            if (c == ':') continue;
            if (Character.isLetter(c))
                throw new RuntimeException("Invalid JSON");
            if (Character.isDigit(c)) {
                int j = i + 1;
                for (; j < json.length() && Character.isDigit(json.charAt(j)); ++j);
                keyValue.value = new Value(Integer.valueOf(json.substring(i, j)), i, j - 1);
                break;
            } else if (c == '"') {
                mIdx = json.indexOf('"', i + 1);
                if (mIdx == -1) throw new RuntimeException("Invalid JSON");
                keyValue.value = new Value(json.substring(i, mIdx), i, mIdx - 1);
                break;
            } else if (c == '{') {
                mIdx = findMatchingChar(json, '{', '}', i + 1);
                keyValue.value = new Value(new JsonObject(json.substring(i, mIdx + 1)), i, mIdx);
                break;
            }
        }

        return keyValue;
    }

    private static class Tests {
        static void run() {
            test1();
        }

        static void test1() {
            String s = """
            [
                [
                   "ola",
                   [
                      
                   ]
                ]
             ]
            """;
            int mIdx = findMatchingChar(s, '[', ']', 1);
            System.out.println("Matching char at: " + mIdx);
        }
    }
}

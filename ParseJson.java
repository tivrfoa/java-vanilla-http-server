import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * I really didn't want to create a json parser ... :(
 * 
 * There are a lot of cases to handle.
 * 
 * Parses a String to a class
 * Case matters ...
 */
public class ParseJson {

    /*
    VSCode does not work properly with this string ...
    public static final String FAKE_JSON = """
                 [{"name":"Brazil","topLevelDomain":[".br"],"alpha2Code":"BR","alpha3Code":"BRA","callingCodes":["55"],"capital":"Brasília","altSpellings":["BR","Brasil","Federative Republic of Brazil","República Federativa do Brasil"],"region":"Americas","subregion":"South America","population":206135893,"latlng":[-10.0,-55.0],"demonym":"Brazilian","area":8515767.0,"gini":54.7,"timezones":["UTC-05:00","UTC-04:00","UTC-03:00","UTC-02:00"],"borders":["ARG","BOL","COL","GUF","GUY","PRY","PER","SUR","URY","VEN"],"nativeName":"Brasil","numericCode":"076","currencies":[{"code":"BRL","name":"Brazilian real","symbol":"R$"}],"languages":[{"iso639_1":"pt","iso639_2":"por","name":"Portuguese","nativeName":"Português"}],"translations":{"de":"Brasilien","es":"Brasil","fr":"Brésil","ja":"ブラジル","it":"Brasile","br":"Brasil","pt":"Brasil","nl":"Brazilië","hr":"Brazil","fa":"برزیل"},"flag":"https://restcountries.eu/data/bra.svg","regionalBlocs":[{"acronym":"USAN","name":"Union of South American Nations","otherAcronyms":["UNASUR","UNASUL","UZAN"],"otherNames":["Unión de Naciones Suramericanas","União de Nações Sul-Americanas","Unie van Zuid-Amerikaanse Naties","South American Union"]}],"cioc":"BRA"}]
             """;
    */

    public static String FAKE_JSON;
    static {
        try {
			FAKE_JSON = new String(Files.readAllBytes(Paths.get("input.json")));
		} catch (IOException e) {
			e.printStackTrace();
        }
        Tests.run();
    }

    private static class Json {
        final Map<String, Object> map;

        public Json(Map<String, Object> map) {
            this.map = map;
        }
    }

    private static class JsonArray {
        String str;

        public JsonArray(String str) {
            this.str = str;
        }

        public List<ParseJson.JsonObject> toJsonObjectList() {
            List<JsonObject> list = new ArrayList<>();
            for (int i = 1; i < str.length() - 1; ++i) {
                if (str.charAt(i) == '{') {
                    int mIdx = findMatchingChar(str, '{', '}', i + 1);
                    list.add(new JsonObject(str.substring(i, mIdx + 1)));
                    i = mIdx + 1;
                }
            }
            return list;
        }
    }

    private static class JsonObject {
        String str;

        public JsonObject(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return "JsonObject [str=" + str + "]";
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

        @Override
        public String toString() {
            return "Key [endPosition=" + endPosition + ", key=" + key + ", startPosition=" + startPosition + "]";
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

        @Override
        public String toString() {
            return "Value [endPosition=" + endPosition + ", startPosition=" + startPosition + ", value=" + value + "]";
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
     * TODO where are other chars that I'm missing?
     */
    private static int nextNonPositionalChar(String str, int fromIndex) {
        int i = fromIndex;
        for (; i < str.length(); ++i) {
            char c = str.charAt(i);
            if (c == '\n' || c == '\r' || c == ' ' || c == '\t') continue;
            return i;
        }
        return -1;
    }

    private static Json getJsonObject(String json, int fromIndex) {
        Map<String, Object> map = new HashMap<>();
        int mIdx = findMatchingChar(json, '{', '}', fromIndex);

        for (int i = fromIndex; i < mIdx; ++i) {
            KeyValue keyValue = getKeyValue(json, i);
            if (keyValue.key == null) { // empty object: {}
                // TODO do nothing for now 
            } else {
                map.put(keyValue.key.key, keyValue.value.value);
            }
            i = keyValue.value.endPosition + 1;
        }

        return new Json(map);
    }

    private static List<Json> getJsonList(String json) {
        List<Json> listJson = new ArrayList<>();

        json = json.trim();

        if (json.charAt(0) != '[' && json.charAt(0) != '{')
            throw new RuntimeException("Invalid start of JSON");

        int idx = nextNonPositionalChar(json, 1);
        if (json.charAt(0) == '[') {
            int mIdx = findMatchingChar(json, '[', ']', 1);

            while (true) {
                if (json.charAt(idx) == '"') {
                    // TODO
                } else if (Character.isDigit(json.charAt(idx))) {
                    // TODO
                } else {
                    if (json.charAt(idx) == '{') {
                        listJson.add(getJsonObject(json, idx + 1));
                    } else {
                        throw new RuntimeException("Invalid JSON");
                    }
                }
                
                idx = json.indexOf(',', mIdx + 1);
                if (idx == -1) break; // end of array
            }
        } else { // it starts with an '{'
            listJson.add(getJsonObject(json, idx));
        }

        return listJson;
    }

    /**
     * This a very bug and very basic parser.
     * 
     * It does not handle many valid and invalid cases.
     * 
     * @param <T>
     * @param json
     * @param clazz
     * @return
     */
    public static <T> List<T> jsonToClass(String json, Class<T> clazz) {
        List<T> list = new ArrayList<>();
        List<Json> listJson = getJsonList(json);
        Object o = null;

        try {
            for (Json j : listJson) {
                var map = j.map;
                o = clazz.getConstructor().newInstance();

                for (var entry : map.entrySet()) {

                    for (var field : clazz.getFields())
                        if (entry.getKey().equals(field.getName())) {
                            System.out.println(field.getName());
                            if (entry.getValue() instanceof JsonArray jsonArray) {
                                // TODO
                                System.out.println("TODO handle array");
                                System.out.println(field.getType().isAssignableFrom(List.class));
                                List<JsonObject> jsonObjects = jsonArray.toJsonObjectList();
                                System.out.println(jsonObjects);
                                List childList = new ArrayList<>();
                                for (var jsonOjbect : jsonObjects) {
                                    System.out.println(field.getType());
                                    System.out.println(field.getGenericType());
                                    System.out.println(field.getGenericType().getClass());
                                    List<?> jsonToClass = jsonToClass(jsonOjbect.str, getActualTypeArgument(field));
                                    Object child = jsonToClass.get(0);
                                    childList.add(child);
                                }
                                field.set(o, childList);
                            } else {
                                field.set(o, entry.getValue());
                            }
                            break;
                        }
                }

                list.add(clazz.cast(o));
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    private static Class<?> getActualTypeArgument(Field field) {
        ParameterizedType stringListType = (ParameterizedType) field.getGenericType();
        Class<?> clazz = (Class<?>) stringListType.getActualTypeArguments()[0];
        return clazz; 
    }

    private static ParseJson.KeyValue getKeyValue(String json, int startPosition) {
        KeyValue keyValue = new KeyValue();

        int mIdx = -1;
        // find key
        for (int i = startPosition; i < json.length(); ++i) {
            char c = json.charAt(i);
            if (c == '}') break;
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
                throw new RuntimeException("Invalid JSON: " + c);
            if (Character.isDigit(c)) {
                int j = i + 1;
                boolean isDouble = false;
                for (; j < json.length(); ++j) {
                    if (Character.isDigit(json.charAt(j))) continue;
                    if (json.charAt(j) != '.') break;
                    isDouble = true;
                }

                if (isDouble)
                    keyValue.value = new Value(Double.valueOf(json.substring(i, j)), i, j - 1);
                else
                    keyValue.value = new Value(Integer.valueOf(json.substring(i, j)), i, j - 1);

                
                break;
            } else if (c == '"') {
                mIdx = json.indexOf('"', i + 1);
                if (mIdx == -1) throw new RuntimeException("Invalid JSON");
                keyValue.value = new Value(json.substring(i + 1, mIdx), i, mIdx - 1);
                break;
            } else if (c == '{') {
                mIdx = findMatchingChar(json, '{', '}', i + 1);
                keyValue.value = new Value(new JsonObject(json.substring(i, mIdx + 1)), i, mIdx);
                break;
            } else if (c == '[') {
                mIdx = findMatchingChar(json, '[', ']', i + 1);
                keyValue.value = new Value(new JsonArray(json.substring(i, mIdx + 1)), i, mIdx);
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

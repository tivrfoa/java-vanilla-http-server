import java.util.List;

public class Country {

    public int population;
    public Double gini;
    public String name;
    public String alpha2Code;
    public String capital;
    public List<Currency> currencies;

    public static class Currency {
        public String code;
        public String name;
        public String symbol;

        @Override
        public String toString() {
            return "Currency [code=" + code + ", name=" + name + ", symbol=" + symbol + "]";
        }
    }

    @Override
    public String toString() {
        return "Country [alpha2Code=" + alpha2Code + ", capital=" + capital + ", currencies=" + currencies + ", gini="
                + gini + ", name=" + name + ", population=" + population + "]";
    }
}

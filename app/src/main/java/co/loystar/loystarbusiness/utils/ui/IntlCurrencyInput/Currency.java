package co.loystar.loystarbusiness.utils.ui.IntlCurrencyInput;

/**
 * Created by laudbruce-tagoe on 1/5/17.
 */

public class Currency {
    /**
     * Name of Currency
     * */
    private String name;

    /**
     * Symbol of Currency
     * */
    private String symbol;

    /**
     * ISO Code of Currency
     * */
    private String code;

    /**
     * Constructor
     * @param name String
     * @param symbol String
     * @param code String
     * */
    public Currency (String name, String symbol, String code) {
        setName(name);
        setSymbol(symbol);
        setCode(code);

    }

    /** Set ISO code of currency
     * @param code String
     * */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * Set symbol of currency
     * @param symbol String
     * */
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    /**
     * Set name of currency
     * @param name String
     * */
    public void setName(String name) {
        this.name = name;
    }

    /** Get name of currency
     * @return String
     * */
    public String getName() {
        return name;
    }

    /** Get symbol of currency
     * @return String
     * */
    public String getSymbol() {
        return symbol;
    }

    /** Get ISO code of currency
     * @return String
     * */
    public String getCode() {
        return code;
    }
}

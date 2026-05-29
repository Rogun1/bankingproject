package com.digitalbanking.bankingproject.constants;


import java.math.BigDecimal;

public final class ApplicationConstants {

    public static final String JWT_SECRET_KEY = "saIm|^8.SNXYY0{IS*2]>,&-]:0#O<fA%?khN)iPC)H";
    public static final String JWT_SECRET_DEFAULT_VALUES = "AnVmoFNDyvB6zGHMneyc2mdwizvXQkQg655jldTe1yn";
    public static final String JWT_HEADER = "Authorization";
    public static final BigDecimal BANK_TRANSFER_FEE = BigDecimal.valueOf(2.5);

    //Simulate currency
    public static final Double RON_IN_EUR = 5.5;
    public static final Double RON_IN_GBP = 6.02;
    public static final Double RON_IN_USD = 4.9;

    public static final Double EUR_IN_GBP = 1.12;
    public static final Double EUR_IN_USD = 0.9;

    public static final Double GBP_IN_US = 0.75;
}

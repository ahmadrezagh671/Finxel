package com.ahmadrezagh671.finxel.models;

import java.math.BigDecimal;

/**
 * Represents the result of comparing two values, including any error message
 * and the numeric difference between them.
 */
public class CompareResult {
    public String error;
    public BigDecimal difference;

    public CompareResult(String error, BigDecimal difference) {
        this.error = error;
        this.difference = difference;
    }
}

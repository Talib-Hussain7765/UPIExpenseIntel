package com.upiexpense.intel.bankaccount;

final class AccountNumberMasker {

    private static final int VISIBLE_SUFFIX_LENGTH = 4;
    private static final String MASK_PREFIX = "XXXX";

    private AccountNumberMasker() {
    }

    static String mask(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();
        String digits = normalized.replaceAll("\\D", "");
        if (!digits.isBlank()) {
            return MASK_PREFIX + lastCharacters(digits, VISIBLE_SUFFIX_LENGTH);
        }

        if (normalized.length() <= VISIBLE_SUFFIX_LENGTH) {
            return MASK_PREFIX + normalized;
        }
        return MASK_PREFIX + normalized.substring(normalized.length() - VISIBLE_SUFFIX_LENGTH);
    }

    private static String lastCharacters(String value, int count) {
        if (value.length() <= count) {
            return value;
        }
        return value.substring(value.length() - count);
    }
}

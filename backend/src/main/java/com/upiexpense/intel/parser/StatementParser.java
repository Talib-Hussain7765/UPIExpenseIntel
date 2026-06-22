package com.upiexpense.intel.parser;

public interface StatementParser {

    boolean supports(StatementParseContext context);

    StatementParseResult parse(StatementParseContext context);
}

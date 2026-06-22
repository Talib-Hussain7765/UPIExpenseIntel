package com.upiexpense.intel.parser;

import com.upiexpense.intel.common.error.BadRequestException;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class StatementParserSelector {

    private final List<StatementParser> parsers;

    public StatementParserSelector(List<StatementParser> parsers) {
        this.parsers = parsers;
    }

    public StatementParser select(StatementParseContext context) {
        return parsers.stream()
                .filter(parser -> parser.supports(context))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Unsupported statement format"));
    }
}

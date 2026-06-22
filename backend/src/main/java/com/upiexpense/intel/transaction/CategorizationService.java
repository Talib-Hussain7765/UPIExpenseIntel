package com.upiexpense.intel.transaction;

import com.upiexpense.intel.category.Category;
import com.upiexpense.intel.category.CategoryRepository;
import com.upiexpense.intel.rule.MerchantRule;
import com.upiexpense.intel.rule.MerchantRuleRepository;
import com.upiexpense.intel.rule.RuleMatchType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CategorizationService {

    private static final Logger log = LoggerFactory.getLogger(CategorizationService.class);

    private static final BigDecimal RULE_CONFIDENCE = new BigDecimal("0.95");
    private static final BigDecimal HIGH_CONFIDENCE = new BigDecimal("0.90");
    private static final BigDecimal MEDIUM_CONFIDENCE = new BigDecimal("0.75");
    private static final BigDecimal FALLBACK_CONFIDENCE = new BigDecimal("0.50");

    private final CategoryRepository categoryRepository;
    private final MerchantRuleRepository merchantRuleRepository;

    public CategorizationService(
            CategoryRepository categoryRepository,
            MerchantRuleRepository merchantRuleRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.merchantRuleRepository = merchantRuleRepository;
    }

    public int categorize(UUID userId, List<Transaction> transactions, boolean overwrite) {
        if (transactions.isEmpty()) {
            log.info("Categorization skipped because transaction list is empty: userId={}, overwrite={}", userId, overwrite);
            return 0;
        }

        Map<String, Category> categoriesByName = categoryRepository.findByUserIdAndActiveTrueOrderByDisplayOrderAscNameAsc(userId)
                .stream()
                .collect(Collectors.toMap(
                        category -> normalize(category.getName()),
                        Function.identity(),
                        (first, second) -> first
                ));
        List<MerchantRule> merchantRules = merchantRuleRepository.findByUserIdAndActiveTrueOrderByPriorityAscCreatedAtAsc(userId);
        log.info("Categorization started: userId={}, transactionCount={}, activeRuleCount={}, overwrite={}",
                userId,
                transactions.size(),
                merchantRules.size(),
                overwrite
        );

        int updated = 0;
        for (Transaction transaction : transactions) {
            if (!overwrite && transaction.getCategory() != null) {
                log.debug("Categorization skipped existing category: userId={}, transactionId={}, category={}, confidence={}, description={}",
                        userId,
                        transaction.getId(),
                        transaction.getCategory().getName(),
                        transaction.getCategorizationConfidence(),
                        truncate(transaction.getDescription())
                );
                continue;
            }

            CategoryMatch match = matchUserRule(transaction, merchantRules)
                    .or(() -> match(transaction, categoriesByName))
                    .orElse(null);
            if (match == null) {
                log.debug("Categorization found no match: userId={}, transactionId={}, description={}",
                        userId,
                        transaction.getId(),
                        truncate(transaction.getDescription())
                );
                continue;
            }

            transaction.setCategory(match.category());
            if (match.merchantName() != null) {
                transaction.setMerchantName(match.merchantName());
            }
            transaction.setCategorizationConfidence(match.confidence());
            updated++;
            log.debug("Categorization applied: userId={}, transactionId={}, category={}, merchantName={}, confidence={}, description={}",
                    userId,
                    transaction.getId(),
                    match.category().getName(),
                    match.merchantName(),
                    match.confidence(),
                    truncate(transaction.getDescription())
            );
        }
        log.info("Categorization completed: userId={}, updatedTransactions={}, overwrite={}", userId, updated, overwrite);
        return updated;
    }

    private Optional<CategoryMatch> matchUserRule(Transaction transaction, List<MerchantRule> merchantRules) {
        String text = searchableText(transaction);
        for (MerchantRule rule : merchantRules) {
            if (matches(rule, text)) {
                log.debug("Merchant rule matched: transactionId={}, ruleId={}, keyword={}, matchType={}, category={}, priority={}",
                        transaction.getId(),
                        rule.getId(),
                        rule.getKeyword(),
                        rule.getMatchType(),
                        rule.getCategory().getName(),
                        rule.getPriority()
                );
                return Optional.of(new CategoryMatch(rule.getCategory(), RULE_CONFIDENCE, rule.getMerchantName()));
            }
        }
        return Optional.empty();
    }

    private boolean matches(MerchantRule rule, String text) {
        String keyword = rule.getKeyword().toUpperCase(Locale.ROOT);
        return switch (rule.getMatchType()) {
            case CONTAINS -> text.contains(keyword);
            case STARTS_WITH -> text.startsWith(keyword);
            case ENDS_WITH -> text.endsWith(keyword);
            case EXACT -> text.equals(keyword);
            case REGEX -> regexMatches(rule.getKeyword(), text);
        };
    }

    private boolean regexMatches(String keyword, String text) {
        try {
            return Pattern.compile(keyword, Pattern.CASE_INSENSITIVE).matcher(text).find();
        } catch (PatternSyntaxException exception) {
            return false;
        }
    }

    private Optional<CategoryMatch> match(Transaction transaction, Map<String, Category> categoriesByName) {
        String text = searchableText(transaction);

        if (transaction.getTransactionType() == TransactionType.CREDIT) {
            if (containsAny(text, "SALARY", "PAYROLL", "COMPENSATION", "NEFT CR SAL")) {
                return category("Salary", HIGH_CONFIDENCE, categoriesByName);
            }
            if (transaction.getPaymentMode() == PaymentMode.INTEREST || containsAny(text, "INTEREST")) {
                return category("Interest", HIGH_CONFIDENCE, categoriesByName);
            }
            if (containsAny(text, "REFUND", "REVERSAL")) {
                return category("Refund", MEDIUM_CONFIDENCE, categoriesByName);
            }
            if (containsAny(text, "CASHBACK", "REWARD")) {
                return category("Cashback", MEDIUM_CONFIDENCE, categoriesByName);
            }
            return category("Other Income", FALLBACK_CONFIDENCE, categoriesByName);
        }

        if (transaction.getPaymentMode() == PaymentMode.ATM || containsAny(text, "ATM")) {
            return category("Cash Withdrawal", HIGH_CONFIDENCE, categoriesByName);
        }
        if (transaction.getPaymentMode() == PaymentMode.CHARGES || containsAny(text, "CHARG", "FEE")) {
            return category("Bank Charges", HIGH_CONFIDENCE, categoriesByName);
        }
        if (containsAny(text, "SWIGGY", "ZOMATO", "RESTAURANT", "CAFE", "HOTEL", "FOOD")) {
            return category("Food", MEDIUM_CONFIDENCE, categoriesByName);
        }
        if (containsAny(text, "GROCERY", "GROCERIES", "MART", "SUPERMARKET", "DMART", "BIGBASKET", "ZEPTO", "BLINKIT")) {
            return category("Groceries", MEDIUM_CONFIDENCE, categoriesByName);
        }
        if (containsAny(text, "AMAZON", "FLIPKART", "MYNTRA", "SHOPPING")) {
            return category("Shopping", MEDIUM_CONFIDENCE, categoriesByName);
        }
        if (containsAny(text, "ELECTRICITY", "BESCOM", "AIRTEL", "JIO", "BILL", "WATER", "GAS")) {
            return category("Bills", MEDIUM_CONFIDENCE, categoriesByName);
        }
        if (containsAny(text, "UBER", "OLA", "RAPIDO", "METRO", "IRCTC", "FUEL", "PETROL")) {
            return category("Travel", MEDIUM_CONFIDENCE, categoriesByName);
        }
        if (containsAny(text, "NETFLIX", "PRIME", "HOTSTAR", "SPOTIFY", "SUBSCRIPTION")) {
            return category("Subscriptions", MEDIUM_CONFIDENCE, categoriesByName);
        }
        if (containsAny(text, "HOSPITAL", "PHARMACY", "MEDICAL", "MEDSOURCE", "CLINIC")) {
            return category("Healthcare", MEDIUM_CONFIDENCE, categoriesByName);
        }
        if (containsAny(text, "SCHOOL", "COLLEGE", "COURSE", "TUITION", "EDUCATION")) {
            return category("Education", MEDIUM_CONFIDENCE, categoriesByName);
        }

        return category("Other Expense", FALLBACK_CONFIDENCE, categoriesByName);
    }

    private Optional<CategoryMatch> category(
            String categoryName,
            BigDecimal confidence,
            Map<String, Category> categoriesByName
    ) {
        return Optional.ofNullable(categoriesByName.get(normalize(categoryName)))
                .map(category -> new CategoryMatch(category, confidence, null));
    }

    private String searchableText(Transaction transaction) {
        return String.join(" ",
                        nullToBlank(transaction.getDescription()),
                        nullToBlank(transaction.getRawDescription()),
                        nullToBlank(transaction.getMerchantName()),
                        nullToBlank(transaction.getReferenceNumber()))
                .toUpperCase(Locale.ROOT);
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 160 ? value : value.substring(0, 160);
    }

    private record CategoryMatch(Category category, BigDecimal confidence, String merchantName) {
    }
}

package com.upiexpense.intel.rule;

import com.upiexpense.intel.category.Category;
import com.upiexpense.intel.category.CategoryRepository;
import com.upiexpense.intel.common.error.BadRequestException;
import com.upiexpense.intel.common.error.NotFoundException;
import com.upiexpense.intel.user.User;
import com.upiexpense.intel.user.UserRepository;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MerchantRuleService {

    private static final Logger log = LoggerFactory.getLogger(MerchantRuleService.class);
    private static final int DEFAULT_PRIORITY = 100;

    private final MerchantRuleRepository merchantRuleRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public MerchantRuleService(
            MerchantRuleRepository merchantRuleRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository
    ) {
        this.merchantRuleRepository = merchantRuleRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<MerchantRuleResponse> list(UUID userId) {
        List<MerchantRuleResponse> rules = merchantRuleRepository.findByUserIdOrderByPriorityAscCreatedAtAsc(userId)
                .stream()
                .map(MerchantRuleResponse::from)
                .toList();
        log.info("Merchant rules listed: userId={}, count={}", userId, rules.size());
        return rules;
    }

    @Transactional
    public MerchantRuleResponse create(UUID userId, CreateMerchantRuleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Category category = findCategory(userId, request.categoryId());

        MerchantRule rule = new MerchantRule();
        rule.setUser(user);
        apply(rule, request.keyword(), request.matchType(), category, request.merchantName(), request.priority(), request.active());

        MerchantRule savedRule = merchantRuleRepository.save(rule);
        log.info("Merchant rule created: userId={}, ruleId={}, keyword={}, matchType={}, category={}, merchantName={}, priority={}, active={}",
                userId,
                savedRule.getId(),
                savedRule.getKeyword(),
                savedRule.getMatchType(),
                savedRule.getCategory().getName(),
                savedRule.getMerchantName(),
                savedRule.getPriority(),
                savedRule.isActive()
        );
        return MerchantRuleResponse.from(savedRule);
    }

    @Transactional
    public MerchantRuleResponse update(UUID userId, UUID ruleId, UpdateMerchantRuleRequest request) {
        MerchantRule rule = findRule(userId, ruleId);
        Category category = findCategory(userId, request.categoryId());
        apply(rule, request.keyword(), request.matchType(), category, request.merchantName(), request.priority(), request.active());

        MerchantRule savedRule = merchantRuleRepository.save(rule);
        log.info("Merchant rule updated: userId={}, ruleId={}, keyword={}, matchType={}, category={}, merchantName={}, priority={}, active={}",
                userId,
                savedRule.getId(),
                savedRule.getKeyword(),
                savedRule.getMatchType(),
                savedRule.getCategory().getName(),
                savedRule.getMerchantName(),
                savedRule.getPriority(),
                savedRule.isActive()
        );
        return MerchantRuleResponse.from(savedRule);
    }

    @Transactional
    public void deactivate(UUID userId, UUID ruleId) {
        MerchantRule rule = findRule(userId, ruleId);
        rule.setActive(false);
        merchantRuleRepository.save(rule);
        log.info("Merchant rule deactivated: userId={}, ruleId={}, keyword={}", userId, ruleId, rule.getKeyword());
    }

    private MerchantRule findRule(UUID userId, UUID ruleId) {
        return merchantRuleRepository.findByIdAndUserId(ruleId, userId)
                .orElseThrow(() -> new NotFoundException("Merchant rule not found"));
    }

    private Category findCategory(UUID userId, UUID categoryId) {
        return categoryRepository.findByIdAndUserIdAndActiveTrue(categoryId, userId)
                .orElseThrow(() -> new NotFoundException("Category not found"));
    }

    private void apply(
            MerchantRule rule,
            String keyword,
            RuleMatchType matchType,
            Category category,
            String merchantName,
            Integer priority,
            Boolean active
    ) {
        String normalizedKeyword = keyword.trim();
        if (normalizedKeyword.isBlank()) {
            throw new BadRequestException("Rule keyword is required");
        }
        if (matchType == RuleMatchType.REGEX) {
            validateRegex(normalizedKeyword);
        }

        rule.setKeyword(normalizedKeyword);
        rule.setMatchType(matchType);
        rule.setCategory(category);
        rule.setMerchantName(trimToNull(merchantName));
        rule.setPriority(priority == null ? DEFAULT_PRIORITY : priority);
        rule.setActive(active == null || active);
    }

    private void validateRegex(String keyword) {
        try {
            Pattern.compile(keyword);
        } catch (PatternSyntaxException exception) {
            log.warn("Merchant rule regex rejected: keyword={}, message={}", keyword, exception.getMessage());
            throw new BadRequestException("Rule regex is invalid");
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

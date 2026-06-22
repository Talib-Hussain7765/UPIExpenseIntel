package com.upiexpense.intel.rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.upiexpense.intel.category.Category;
import com.upiexpense.intel.category.CategoryRepository;
import com.upiexpense.intel.category.CategoryType;
import com.upiexpense.intel.common.error.BadRequestException;
import com.upiexpense.intel.user.User;
import com.upiexpense.intel.user.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MerchantRuleServiceTest {

    @Mock
    private MerchantRuleRepository merchantRuleRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    private MerchantRuleService service;
    private UUID userId;
    private User user;
    private Category category;

    @BeforeEach
    void setUp() {
        service = new MerchantRuleService(merchantRuleRepository, categoryRepository, userRepository);
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        category = new Category();
        category.setId(UUID.randomUUID());
        category.setUser(user);
        category.setName("Food");
        category.setCategoryType(CategoryType.EXPENSE);
        category.setActive(true);
    }

    @Test
    void createsMerchantRule() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(categoryRepository.findByIdAndUserIdAndActiveTrue(category.getId(), userId)).thenReturn(Optional.of(category));
        when(merchantRuleRepository.save(any(MerchantRule.class))).thenAnswer(invocation -> {
            MerchantRule rule = invocation.getArgument(0);
            rule.setId(UUID.randomUUID());
            return rule;
        });
        CreateMerchantRuleRequest request = new CreateMerchantRuleRequest(
                " swiggy ",
                RuleMatchType.CONTAINS,
                category.getId(),
                " Swiggy ",
                5,
                true
        );

        MerchantRuleResponse response = service.create(userId, request);

        assertThat(response.keyword()).isEqualTo("swiggy");
        assertThat(response.matchType()).isEqualTo(RuleMatchType.CONTAINS);
        assertThat(response.category().id()).isEqualTo(category.getId());
        assertThat(response.merchantName()).isEqualTo("Swiggy");
        assertThat(response.priority()).isEqualTo(5);
        assertThat(response.active()).isTrue();
    }

    @Test
    void rejectsInvalidRegexRule() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(categoryRepository.findByIdAndUserIdAndActiveTrue(category.getId(), userId)).thenReturn(Optional.of(category));
        CreateMerchantRuleRequest request = new CreateMerchantRuleRequest(
                "[bad",
                RuleMatchType.REGEX,
                category.getId(),
                null,
                null,
                true
        );

        assertThatThrownBy(() -> service.create(userId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Rule regex is invalid");
    }

    @Test
    void deactivatesMerchantRule() {
        MerchantRule rule = new MerchantRule();
        rule.setId(UUID.randomUUID());
        rule.setUser(user);
        rule.setKeyword("SWIGGY");
        rule.setMatchType(RuleMatchType.CONTAINS);
        rule.setCategory(category);
        rule.setActive(true);
        when(merchantRuleRepository.findByIdAndUserId(rule.getId(), userId)).thenReturn(Optional.of(rule));

        service.deactivate(userId, rule.getId());

        assertThat(rule.isActive()).isFalse();
        verify(merchantRuleRepository).save(rule);
    }
}

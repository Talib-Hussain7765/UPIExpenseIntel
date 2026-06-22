package com.upiexpense.intel.category;

import com.upiexpense.intel.user.User;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DefaultCategorySeeder {

    private static final Logger log = LoggerFactory.getLogger(DefaultCategorySeeder.class);

    private static final List<DefaultCategoryDefinition> DEFAULT_CATEGORIES = List.of(
            new DefaultCategoryDefinition("Food", CategoryType.EXPENSE, "#F97316", "restaurant", 10),
            new DefaultCategoryDefinition("Groceries", CategoryType.EXPENSE, "#22C55E", "shopping_basket", 20),
            new DefaultCategoryDefinition("Rent", CategoryType.EXPENSE, "#6366F1", "home", 30),
            new DefaultCategoryDefinition("Travel", CategoryType.EXPENSE, "#0EA5E9", "directions_car", 40),
            new DefaultCategoryDefinition("Shopping", CategoryType.EXPENSE, "#EC4899", "shopping_bag", 50),
            new DefaultCategoryDefinition("Bills", CategoryType.EXPENSE, "#EAB308", "receipt_long", 60),
            new DefaultCategoryDefinition("Subscriptions", CategoryType.EXPENSE, "#8B5CF6", "subscriptions", 70),
            new DefaultCategoryDefinition("Healthcare", CategoryType.EXPENSE, "#EF4444", "local_hospital", 80),
            new DefaultCategoryDefinition("Education", CategoryType.EXPENSE, "#14B8A6", "school", 90),
            new DefaultCategoryDefinition("Entertainment", CategoryType.EXPENSE, "#A855F7", "movie", 100),
            new DefaultCategoryDefinition("Investments", CategoryType.EXPENSE, "#10B981", "trending_up", 110),
            new DefaultCategoryDefinition("Cash Withdrawal", CategoryType.EXPENSE, "#64748B", "payments", 120),
            new DefaultCategoryDefinition("Bank Charges", CategoryType.EXPENSE, "#DC2626", "account_balance", 130),
            new DefaultCategoryDefinition("Other Expense", CategoryType.EXPENSE, "#78716C", "help_outline", 140),
            new DefaultCategoryDefinition("Salary", CategoryType.INCOME, "#16A34A", "account_balance_wallet", 210),
            new DefaultCategoryDefinition("Refund", CategoryType.INCOME, "#059669", "currency_exchange", 220),
            new DefaultCategoryDefinition("Interest", CategoryType.INCOME, "#65A30D", "percent", 230),
            new DefaultCategoryDefinition("Cashback", CategoryType.INCOME, "#0891B2", "currency_rupee", 240),
            new DefaultCategoryDefinition("Other Income", CategoryType.INCOME, "#0F766E", "add_circle_outline", 250),
            new DefaultCategoryDefinition("Transfer", CategoryType.TRANSFER, "#475569", "swap_horiz", 310)
    );

    private final CategoryRepository categoryRepository;

    public DefaultCategorySeeder(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public void seedForUser(User user) {
        List<Category> categories = DEFAULT_CATEGORIES.stream()
                .filter(definition -> !categoryRepository.existsByUserIdAndNameIgnoreCaseAndActiveTrue(
                        user.getId(),
                        definition.name()
                ))
                .map(definition -> toCategory(user, definition))
                .toList();

        if (categories.isEmpty()) {
            log.info("Default category seeding skipped because categories already exist: userId={}", user.getId());
            return;
        }

        categoryRepository.saveAll(categories);
        log.info("Default categories seeded: userId={}, count={}", user.getId(), categories.size());
    }

    private Category toCategory(User user, DefaultCategoryDefinition definition) {
        Category category = new Category();
        category.setUser(user);
        category.setName(definition.name());
        category.setCategoryType(definition.categoryType());
        category.setColor(definition.color());
        category.setIcon(definition.icon());
        category.setDisplayOrder(definition.displayOrder());
        category.setDefaultCategory(true);
        category.setActive(true);
        return category;
    }

    private record DefaultCategoryDefinition(
            String name,
            CategoryType categoryType,
            String color,
            String icon,
            int displayOrder
    ) {
    }
}

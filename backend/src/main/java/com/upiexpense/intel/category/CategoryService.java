package com.upiexpense.intel.category;

import com.upiexpense.intel.common.error.BadRequestException;
import com.upiexpense.intel.common.error.NotFoundException;
import com.upiexpense.intel.user.User;
import com.upiexpense.intel.user.UserRepository;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final DefaultCategorySeeder defaultCategorySeeder;

    public CategoryService(
            CategoryRepository categoryRepository,
            UserRepository userRepository,
            DefaultCategorySeeder defaultCategorySeeder
    ) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.defaultCategorySeeder = defaultCategorySeeder;
    }

    @Transactional
    public List<CategoryResponse> list(UUID userId) {
        List<Category> categories = categoryRepository.findByUserIdAndActiveTrueOrderByDisplayOrderAscNameAsc(userId);
        if (categories.isEmpty()) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User not found"));
            defaultCategorySeeder.seedForUser(user);
            categories = categoryRepository.findByUserIdAndActiveTrueOrderByDisplayOrderAscNameAsc(userId);
        }

        List<CategoryResponse> responses = categories
                .stream()
                .map(CategoryResponse::from)
                .toList();
        log.info("Categories listed: userId={}, count={}", userId, responses.size());
        return responses;
    }

    @Transactional
    public CategoryResponse create(UUID userId, CreateCategoryRequest request) {
        log.debug("Creating category: userId={}, name={}", userId, request.name());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        ensureNameAvailable(userId, request.name());

        Category category = new Category();
        category.setUser(user);
        category.setDefaultCategory(false);
        category.setActive(true);
        applyRequest(category, request.name(), request.categoryType(), request.color(), request.icon(), request.displayOrder());

        Category savedCategory = categoryRepository.save(category);
        log.info("Category created: userId={}, categoryId={}, name={}", userId, savedCategory.getId(), savedCategory.getName());
        return CategoryResponse.from(savedCategory);
    }

    @Transactional
    public CategoryResponse update(UUID userId, UUID categoryId, UpdateCategoryRequest request) {
        Category category = findActiveOwnedCategory(userId, categoryId);
        if (!category.getName().equalsIgnoreCase(request.name().trim())) {
            ensureNameAvailable(userId, request.name());
        }

        applyRequest(category, request.name(), request.categoryType(), request.color(), request.icon(), request.displayOrder());
        Category savedCategory = categoryRepository.save(category);
        log.info("Category updated: userId={}, categoryId={}, name={}", userId, categoryId, savedCategory.getName());
        return CategoryResponse.from(savedCategory);
    }

    @Transactional
    public void deactivate(UUID userId, UUID categoryId) {
        Category category = findActiveOwnedCategory(userId, categoryId);
        if (category.isDefaultCategory()) {
            log.warn("Default category deletion rejected: userId={}, categoryId={}", userId, categoryId);
            throw new BadRequestException("Default categories cannot be deleted");
        }

        category.setActive(false);
        categoryRepository.save(category);
        log.info("Category deactivated: userId={}, categoryId={}", userId, categoryId);
    }

    private Category findActiveOwnedCategory(UUID userId, UUID categoryId) {
        return categoryRepository.findByIdAndUserIdAndActiveTrue(categoryId, userId)
                .orElseThrow(() -> new NotFoundException("Category not found"));
    }

    private void ensureNameAvailable(UUID userId, String name) {
        if (categoryRepository.existsByUserIdAndNameIgnoreCaseAndActiveTrue(userId, name.trim())) {
            log.warn("Category name rejected because it already exists: userId={}, name={}", userId, name);
            throw new BadRequestException("Category name already exists");
        }
    }

    private void applyRequest(
            Category category,
            String name,
            CategoryType categoryType,
            String color,
            String icon,
            Integer displayOrder
    ) {
        category.setName(name.trim());
        category.setCategoryType(categoryType);
        category.setColor(trimToNull(color));
        category.setIcon(trimToNull(icon));
        category.setDisplayOrder(displayOrder == null ? 0 : displayOrder);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

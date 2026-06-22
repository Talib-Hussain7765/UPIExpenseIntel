package com.upiexpense.intel.category;

import com.upiexpense.intel.common.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    List<CategoryResponse> list(@AuthenticationPrincipal CurrentUser currentUser) {
        return categoryService.list(currentUser.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CategoryResponse create(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody CreateCategoryRequest request
    ) {
        return categoryService.create(currentUser.id(), request);
    }

    @PutMapping("/{id}")
    CategoryResponse update(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        return categoryService.update(currentUser.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable UUID id) {
        categoryService.deactivate(currentUser.id(), id);
    }
}

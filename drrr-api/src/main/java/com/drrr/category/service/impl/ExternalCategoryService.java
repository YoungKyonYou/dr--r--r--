package com.drrr.category.service.impl;

import com.drrr.category.dto.CategoryRequest;
import com.drrr.domain.category.entity.Category;
import com.drrr.domain.category.entity.RedisCategory;
import com.drrr.domain.category.service.CategoryService;
import com.drrr.domain.category.service.CategoryService.CategoryDto;
import com.drrr.domain.category.service.RedisCategoryService;
import com.drrr.domain.jpa.entity.BaseEntity;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExternalCategoryService {
    private final CategoryService categoryService;
    private final RedisCategoryService redisCategoryService;

    /**
     * 모든 카테고리 정보를 가져옴, redis를 사용하지 않음 파라미터 없이 redis에서 findAll할 경우 문제가 발생할 수 있음 기존에 20개중 3개가 들어갔다고 하면 3개만 끌고 오는 문제 발생
     */
    public List<CategoryDto> execute() {
        return categoryService.findAllCategories().stream()
                .sorted(Comparator.comparing(CategoryDto::categoryName)).toList();
    }

    public List<CategoryDto> execute(final Long topN) {
        return categoryService.findTopCategories(topN);
    }

    /**
     * 지정된 카테고리 정보를 가져옴
     */
    public List<CategoryDto> execute(final CategoryRequest request) {
        final List<Category> redisCategories = redisCategoryService.findByIds(request.categoryIds());
        if (redisCategories.size() != request.categoryIds().size()) {
            final Set<Long> requestIds = new HashSet<>(request.categoryIds());
            final Set<Long> redisCategoryIds = redisCategories.stream().map(BaseEntity::getId)
                    .collect(Collectors.toSet());
            //redis에 없는 카테고리를 뽑아냄
            requestIds.removeAll(redisCategoryIds);

            final List<Long> ids = requestIds.stream().toList();

            final List<CategoryDto> selectedCategories = categoryService.findSelectedCategories(ids);

            final List<RedisCategory> redisCategoryList = selectedCategories.stream()
                    .filter(redisCategory -> requestIds.contains(redisCategory.id()))
                    .map(filteredCategory -> RedisCategory.builder()
                            .id(filteredCategory.id())
                            .name(filteredCategory.categoryName())
                            .build())
                    .toList();

            redisCategoryService.saveCategories(redisCategoryList);
            //redis에 기존에 있었던 카테고리 dto로 변환

            return Stream.concat(
                            redisCategories.stream()
                                    .map(redisCategory -> CategoryDto.builder()
                                            .id(redisCategory.getId())
                                            .categoryName(redisCategory.getName())
                                            .build()),
                            selectedCategories.stream())
                    .collect(Collectors.toList());
        }
        return categoryService.findSelectedCategories(request.categoryIds());
    }
}

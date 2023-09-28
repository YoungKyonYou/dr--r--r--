package com.drrr.domain.category.repository;

import com.drrr.domain.category.entity.Category;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long>, CustomCategoryRepository {
    List<Category> findIds(List<Long> categoryIds);
}

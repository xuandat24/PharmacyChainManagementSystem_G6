package fu.se.pharmacy.service.impl;

import fu.se.pharmacy.entity.Category;
import fu.se.pharmacy.repository.CategoryRepository;
import fu.se.pharmacy.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {


    private final CategoryRepository categoryRepository;

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public Category saveCategory(Category category) {
        return categoryRepository.save(category);
    }

    @Override
    public Category getCategoryById(Integer id) {
        return categoryRepository.findById(id).orElse(null);
    }

    @Override
    public void deleteCategory(Integer id) {
        Category cat = getCategoryById(id);
        if (cat != null) {
            cat.setStatus("INACTIVE"); // Tuân thủ rule: Không xóa cứng
            categoryRepository.save(cat);
        }
    }
}
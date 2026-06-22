package fu.se.pharmacy.service;
import fu.se.pharmacy.entity.Category;
import java.util.List;

public interface CategoryService {
    List<Category> getAllCategories();
    Category saveCategory(Category category);
    Category getCategoryById(Integer id);
    void deleteCategory(Integer id);
}
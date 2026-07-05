package fu.se.pharmacy.service;

import fu.se.pharmacy.entity.Supplier;
import java.util.List;

public interface SupplierService {
    List<Supplier> getAllSuppliers();
    Supplier getSupplierById(Integer id);
    Supplier saveSupplier(Supplier supplier);
    void deleteSupplier(Integer id);
    List<Supplier> searchSuppliers(String query);
}

package fu.se.pharmacy.service;

import fu.se.pharmacy.dto.CustomerDTO;

import fu.se.pharmacy.entity.Customer;
import java.util.List;
import java.util.Optional;

public interface CustomerService {
    List<CustomerDTO> findAll();
    Optional<CustomerDTO> findById(Integer id);

    Optional<Customer> findEntityById(Integer id);

    Optional<Customer> findByPhone(String phone);
    List<CustomerDTO> searchByName(String name);
    CustomerDTO save(CustomerDTO dto);
    void deleteById(Integer id);
}
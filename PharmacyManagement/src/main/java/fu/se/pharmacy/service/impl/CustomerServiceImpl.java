package fu.se.pharmacy.service.impl;

import fu.se.pharmacy.dto.CustomerDTO;
import fu.se.pharmacy.entity.Customer;
import fu.se.pharmacy.repository.CustomerRepository;
import fu.se.pharmacy.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CustomerServiceImpl implements CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Override
    public List<CustomerDTO> findAll() {
        return customerRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<CustomerDTO> findById(Integer id) {
        return customerRepository.findById(id).map(this::toResponseDTO);
    }

    @Override
    public Optional<Customer> findEntityById(Integer id) {
        return customerRepository.findById(id);
    }

    @Override
    public Optional<Customer> findByPhone(String phone) {
        return customerRepository.findByPhone(phone);
    }

    @Override
    public List<CustomerDTO> searchByName(String name) {
        return customerRepository.findByFullNameContainingIgnoreCase(name)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public CustomerDTO save(CustomerDTO dto) {
        Customer customer = (dto.getCustomerId() != null)
                ? customerRepository.findById(dto.getCustomerId()).orElse(new Customer())
                : new Customer();
        toEntity(dto, customer);
        return toResponseDTO(customerRepository.save(customer));
    }

    @Override
    public void deleteById(Integer id) {
        customerRepository.deleteById(id);
    }

    // ===== Converters =====

    private CustomerDTO toResponseDTO(Customer c) {
        CustomerDTO dto = new CustomerDTO();
        dto.setCustomerId(c.getCustomerId());
        dto.setFullName(c.getFullName());
        dto.setPhone(c.getPhone());
        dto.setDateOfBirth(c.getDateOfBirth());
        dto.setGender(c.getGender());
        dto.setAddress(c.getAddress());
        dto.setAllergyNote(c.getAllergyNote());
        dto.setCreatedAt(c.getCreatedAt());
        return dto;
    }

    private void toEntity(CustomerDTO dto, Customer c) {
        c.setFullName(dto.getFullName());
        c.setPhone(dto.getPhone());
        c.setDateOfBirth(dto.getDateOfBirth());
        c.setGender(dto.getGender());
        c.setAddress(dto.getAddress());
        c.setAllergyNote(dto.getAllergyNote());
    }
}
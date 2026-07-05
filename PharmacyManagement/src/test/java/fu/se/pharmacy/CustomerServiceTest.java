package fu.se.pharmacy;

import fu.se.pharmacy.dto.CustomerDTO;
import fu.se.pharmacy.entity.Customer;
import fu.se.pharmacy.repository.CustomerRepository;
import fu.se.pharmacy.service.impl.CustomerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerService - Unit Tests")
class CustomerServiceTest {

    @Mock private CustomerRepository customerRepository;
    @InjectMocks private CustomerServiceImpl customerService;

    private Customer sampleCustomer;

    @BeforeEach
    void setUp() {
        sampleCustomer = new Customer();
        sampleCustomer.setCustomerId(1);
        sampleCustomer.setFullName("Nguyen Thi Lan");
        sampleCustomer.setPhone("0912345601");
        sampleCustomer.setDateOfBirth(LocalDate.of(1990, 3, 15));
        sampleCustomer.setGender("FEMALE");
        sampleCustomer.setAddress("So 5 Ba Trieu, HN");
        sampleCustomer.setAllergyNote(null);
        sampleCustomer.setCreatedAt(LocalDateTime.now());
    }

    // ===== findAll =====

    @Test
    @DisplayName("findAll - tra ve danh sach day du")
    void findAll_returnsAllCustomers() {
        Customer c2 = new Customer();
        c2.setCustomerId(2);
        c2.setFullName("Tran Van Minh");
        c2.setPhone("0912345602");

        when(customerRepository.findAll()).thenReturn(List.of(sampleCustomer, c2));

        List<CustomerDTO> result = customerService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getFullName()).isEqualTo("Nguyen Thi Lan");
        assertThat(result.get(1).getFullName()).isEqualTo("Tran Van Minh");
    }

    @Test
    @DisplayName("findAll - khong co khach hang nao tra ve danh sach rong")
    void findAll_emptyList_returnsEmpty() {
        when(customerRepository.findAll()).thenReturn(List.of());
        assertThat(customerService.findAll()).isEmpty();
    }

    // ===== findById =====

    @Test
    @DisplayName("findById - tim thay tra ve DTO day du")
    void findById_found_returnsDTO() {
        when(customerRepository.findById(1)).thenReturn(Optional.of(sampleCustomer));

        Optional<CustomerDTO> result = customerService.findById(1);

        assertThat(result).isPresent();
        CustomerDTO dto = result.get();
        assertThat(dto.getCustomerId()).isEqualTo(1);
        assertThat(dto.getFullName()).isEqualTo("Nguyen Thi Lan");
        assertThat(dto.getPhone()).isEqualTo("0912345601");
        assertThat(dto.getGender()).isEqualTo("FEMALE");
        assertThat(dto.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 3, 15));
    }

    @Test
    @DisplayName("findById - khong tim thay tra ve empty")
    void findById_notFound_returnsEmpty() {
        when(customerRepository.findById(999)).thenReturn(Optional.empty());
        assertThat(customerService.findById(999)).isEmpty();
    }

    // ===== findByPhone =====

    @Test
    @DisplayName("findByPhone - tim dung so dien thoai")
    void findByPhone_found() {
        when(customerRepository.findByPhone("0912345601")).thenReturn(Optional.of(sampleCustomer));
        Optional<Customer> result = customerService.findByPhone("0912345601");
        assertThat(result).isPresent();
        assertThat(result.get().getPhone()).isEqualTo("0912345601");
    }

    @Test
    @DisplayName("findByPhone - so dien thoai khong ton tai tra ve empty")
    void findByPhone_notFound() {
        when(customerRepository.findByPhone("0000000000")).thenReturn(Optional.empty());
        assertThat(customerService.findByPhone("0000000000")).isEmpty();
    }

    // ===== save - tao moi =====

    @Test
    @DisplayName("save - tao moi khach hang (customerId null)")
    void save_createNew_success() {
        CustomerDTO input = new CustomerDTO();
        input.setCustomerId(null); // tao moi
        input.setFullName("Bui Van Cuong");
        input.setPhone("0912345608");
        input.setGender("MALE");
        input.setDateOfBirth(LocalDate.of(1970, 12, 8));

        Customer saved = new Customer();
        saved.setCustomerId(8);
        saved.setFullName("Bui Van Cuong");
        saved.setPhone("0912345608");
        saved.setGender("MALE");
        saved.setDateOfBirth(LocalDate.of(1970, 12, 8));
        saved.setCreatedAt(LocalDateTime.now());

        when(customerRepository.save(any(Customer.class))).thenReturn(saved);

        CustomerDTO result = customerService.save(input);

        assertThat(result.getCustomerId()).isEqualTo(8);
        assertThat(result.getFullName()).isEqualTo("Bui Van Cuong");
        verify(customerRepository, never()).findById(any()); // khong goi findById vi la tao moi
        verify(customerRepository).save(any(Customer.class));
    }

    // ===== save - cap nhat =====

    @Test
    @DisplayName("save - cap nhat khach hang hien co (customerId != null)")
    void save_update_success() {
        CustomerDTO input = new CustomerDTO();
        input.setCustomerId(1);
        input.setFullName("Nguyen Thi Lan Updated");
        input.setPhone("0912345601");
        input.setGender("FEMALE");
        input.setAllergyNote("Di ung Penicilin");

        Customer updated = new Customer();
        updated.setCustomerId(1);
        updated.setFullName("Nguyen Thi Lan Updated");
        updated.setPhone("0912345601");
        updated.setAllergyNote("Di ung Penicilin");

        when(customerRepository.findById(1)).thenReturn(Optional.of(sampleCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(updated);

        CustomerDTO result = customerService.save(input);

        assertThat(result.getFullName()).isEqualTo("Nguyen Thi Lan Updated");
        assertThat(result.getAllergyNote()).isEqualTo("Di ung Penicilin");
        verify(customerRepository).findById(1);
        verify(customerRepository).save(any(Customer.class));
    }

    // ===== deleteById =====

    @Test
    @DisplayName("deleteById - xoa thanh cong")
    void deleteById_success() {
        doNothing().when(customerRepository).deleteById(1);
        customerService.deleteById(1);
        verify(customerRepository).deleteById(1);
    }

    // ===== searchByName =====

    @Test
    @DisplayName("searchByName - tim theo ten tra ve danh sach")
    void searchByName_found() {
        when(customerRepository.findByFullNameContainingIgnoreCase("Lan"))
                .thenReturn(List.of(sampleCustomer));

        List<CustomerDTO> result = customerService.searchByName("Lan");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFullName()).contains("Lan");
    }

    @Test
    @DisplayName("searchByName - khong tim thay tra ve danh sach rong")
    void searchByName_notFound() {
        when(customerRepository.findByFullNameContainingIgnoreCase("XYZ"))
                .thenReturn(List.of());
        assertThat(customerService.searchByName("XYZ")).isEmpty();
    }

    // ===== findEntityById =====

    @Test
    @DisplayName("findEntityById - tra ve entity goc")
    void findEntityById_returnsEntity() {
        when(customerRepository.findById(1)).thenReturn(Optional.of(sampleCustomer));
        Optional<Customer> result = customerService.findEntityById(1);
        assertThat(result).isPresent();
        assertThat(result.get()).isInstanceOf(Customer.class);
    }
}

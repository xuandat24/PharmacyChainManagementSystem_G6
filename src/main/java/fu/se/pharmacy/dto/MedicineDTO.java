package fu.se.pharmacy.dto;

import lombok.Data;

@Data
public class MedicineDTO {
    private Integer medicineId;
    private String medicineCode;
    private String medicineName;
    private Integer categoryId; 
    private String unit;
    private Double price;
    private String description;
    private String status;
}
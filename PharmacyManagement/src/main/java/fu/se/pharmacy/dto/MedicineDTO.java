package fu.se.pharmacy.dto;

import lombok.Data;

@Data
public class MedicineDTO {
    private Integer medicineId;
    private String medicineCode;
    private String barcode;
    private String medicineName;
    private Integer categoryId;
    private String activeIngredient;
    private String strength;
    private String dosageForm;
    private String unit;
    private String manufacturer;
    private String countryOfOrigin;
    private Integer salePrice;
    private Integer minStockLevel;
    private Boolean requiresPrescription;
    private String status;
}

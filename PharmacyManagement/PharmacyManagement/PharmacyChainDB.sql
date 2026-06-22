Create database PharmacyChainDB;

GO
USE PharmacyChainDB;
GO


/* ============================================================
   2. BRANCH - each pharmacy store in the chain, owned by Owner
   ============================================================ */
CREATE TABLE Branch (
    BranchID    INT IDENTITY(1,1) PRIMARY KEY,
    BranchName  NVARCHAR(100) NOT NULL,
    Address     NVARCHAR(255),
    Phone       NVARCHAR(20),
    
);
GO

/* ============================================================
   3. EMPLOYEE - branch-level staff accounts
   ============================================================ */
CREATE TABLE Employee (
    EmployeeID    INT IDENTITY(1,1) PRIMARY KEY,
    FullName      NVARCHAR(100) NOT NULL,
    Username      NVARCHAR(50) NOT NULL UNIQUE,
    PasswordHash  NVARCHAR(255) NOT NULL,
    Role          NVARCHAR(20) NOT NULL
        CHECK (Role IN ('Admin','BranchManager','Pharmacist')),
    BranchID      INT NOT NULL,
    Phone         NVARCHAR(20),
    Email         NVARCHAR(100),
    CONSTRAINT FK_Employee_Branch FOREIGN KEY (BranchID) REFERENCES Branch(BranchID)
);
GO

/* ============================================================
   4. CATEGORY - medicine category
   ============================================================ */
CREATE TABLE Category (
    CategoryID    INT IDENTITY(1,1) PRIMARY KEY,
    CategoryName  NVARCHAR(100) NOT NULL UNIQUE
);
GO

/* ============================================================
   5. SUPPLIER - vendor each medicine is sourced from
   ============================================================ */
CREATE TABLE Supplier (
    SupplierID    INT IDENTITY(1,1) PRIMARY KEY,
    SupplierName  NVARCHAR(100) NOT NULL,
    Phone         NVARCHAR(20),
    Address       NVARCHAR(255)
);
GO

/* ============================================================
   6. MEDICINE - catalog item
   ============================================================ */
CREATE TABLE Medicine (
    MedicineID            INT IDENTITY(1,1) PRIMARY KEY,
    MedicineName          NVARCHAR(150) NOT NULL,
    GenericName           NVARCHAR(150),
    Strength              NVARCHAR(50),
    DosageForm            NVARCHAR(50),
    Barcode               NVARCHAR(50) UNIQUE,
    CategoryID            INT NOT NULL,
    SupplierID            INT NOT NULL,
    UnitPrice             DECIMAL(12,2) NOT NULL CHECK (UnitPrice >= 0),
    RequiresPrescription  BIT NOT NULL DEFAULT 0,
    ReorderLevel          INT NOT NULL DEFAULT 10,

    CONSTRAINT FK_Medicine_Category
        FOREIGN KEY (CategoryID) REFERENCES Category(CategoryID),

    CONSTRAINT FK_Medicine_Supplier
        FOREIGN KEY (SupplierID) REFERENCES Supplier(SupplierID)
);
GO

/* ============================================================
   7. INVENTORY - stock level of one medicine at one branch
   ============================================================ */
CREATE TABLE Inventory (
    InventoryID      INT IDENTITY(1,1) PRIMARY KEY,
    BranchID         INT NOT NULL,
    MedicineID       INT NOT NULL,
    BatchNumber      NVARCHAR(50) NOT NULL,
    Quantity         INT NOT NULL CHECK (Quantity >= 0),
    ManufactureDate  DATE,
    ExpiryDate       DATE,
    PurchasePrice    DECIMAL(12,2),

    CONSTRAINT FK_Inventory_Branch
        FOREIGN KEY (BranchID) REFERENCES Branch(BranchID),

    CONSTRAINT FK_Inventory_Medicine
        FOREIGN KEY (MedicineID) REFERENCES Medicine(MedicineID),
    CONSTRAINT UQ_Inventory_Batch
        UNIQUE (BranchID, MedicineID, BatchNumber)
);
GO

/* ============================================================
   8. STOCK_TRANSFER - moving stock between two branches
   ============================================================ */
CREATE TABLE StockTransfer (
    TransferID      INT IDENTITY(1,1) PRIMARY KEY,
    FromBranchID    INT NOT NULL,
    ToBranchID      INT NOT NULL,
    TransferDate    DATETIME NOT NULL DEFAULT GETDATE(),

    Status NVARCHAR(20) NOT NULL DEFAULT 'Pending'
        CHECK (Status IN ('Pending','Approved','Rejected','Completed')),

    CONSTRAINT FK_ST_FromBranch
        FOREIGN KEY (FromBranchID) REFERENCES Branch(BranchID),

    CONSTRAINT FK_ST_ToBranch
        FOREIGN KEY (ToBranchID) REFERENCES Branch(BranchID),

    CONSTRAINT CK_ST_DiffBranch
        CHECK (FromBranchID <> ToBranchID)
);
GO

CREATE TABLE StockTransferDetail (
    TransferDetailID INT IDENTITY(1,1) PRIMARY KEY,
    TransferID       INT NOT NULL,
    MedicineID       INT NOT NULL,
    BatchNumber      NVARCHAR(50),
    Quantity         INT NOT NULL CHECK (Quantity > 0),

    CONSTRAINT FK_STD_Transfer
        FOREIGN KEY (TransferID)
        REFERENCES StockTransfer(TransferID),

    CONSTRAINT FK_STD_Medicine
        FOREIGN KEY (MedicineID)
        REFERENCES Medicine(MedicineID)
);
GO

/* ============================================================
   9. CUSTOMER - expanded with extra profile attributes
   ============================================================ */
CREATE TABLE Customer (
    CustomerID     INT IDENTITY(1,1) PRIMARY KEY,
    FullName       NVARCHAR(100) NOT NULL,
    Phone          NVARCHAR(20),
    Address        NVARCHAR(255),
    Email          NVARCHAR(100) UNIQUE,
    DateOfBirth    DATE,
    Gender         NVARCHAR(10) CHECK (Gender IN ('Male','Female','Other')),
    Allergies      NVARCHAR(255),          -- known drug/ingredient allergies
    LoyaltyPoints  INT NOT NULL DEFAULT 0,
    CreatedAt      DATETIME NOT NULL DEFAULT GETDATE()
);
GO

/* ============================================================
   10. SALE - transaction header
   ============================================================ */


/* ============================================================
   12. PRESCRIPTION - one prescribed medicine per row (simplified)
   ============================================================ */
CREATE TABLE Prescription (
    PrescriptionID INT IDENTITY(1,1) PRIMARY KEY,
    CustomerID     INT NOT NULL,
    DoctorName     NVARCHAR(100),
    IssueDate      DATE NOT NULL,
    ExpiryDate     DATE,
    Notes          NVARCHAR(500),

    CONSTRAINT FK_Prescription_Customer
        FOREIGN KEY (CustomerID)
        REFERENCES Customer(CustomerID)
);
GO

CREATE TABLE PrescriptionDetail (
    PrescriptionDetailID INT IDENTITY(1,1) PRIMARY KEY,
    PrescriptionID       INT NOT NULL,
    MedicineID           INT NOT NULL,

    Dosage               NVARCHAR(255),
    QuantityPrescribed   INT NOT NULL,
    Instructions         NVARCHAR(500),

    CONSTRAINT FK_PD_Prescription
        FOREIGN KEY (PrescriptionID)
        REFERENCES Prescription(PrescriptionID),

    CONSTRAINT FK_PD_Medicine
        FOREIGN KEY (MedicineID)
        REFERENCES Medicine(MedicineID)
);
GO

CREATE TABLE Sale (
    SaleID        INT IDENTITY(1,1) PRIMARY KEY,
    BranchID      INT NOT NULL,
    EmployeeID    INT NOT NULL,
    CustomerID    INT NULL,             -- nullable: walk-in customer
    PrescriptionID INT NULL,
    SaleDate      DATETIME NOT NULL DEFAULT GETDATE(),
    TotalAmount   DECIMAL(12,2) NOT NULL DEFAULT 0,
    CONSTRAINT FK_Sale_Branch   FOREIGN KEY (BranchID)   REFERENCES Branch(BranchID),
    CONSTRAINT FK_Sale_Employee FOREIGN KEY (EmployeeID) REFERENCES Employee(EmployeeID),
    CONSTRAINT FK_Sale_Customer FOREIGN KEY (CustomerID) REFERENCES Customer(CustomerID),
    CONSTRAINT FK_Sale_Prescription FOREIGN KEY (PrescriptionID) REFERENCES Prescription(PrescriptionID)
);
GO

/* ============================================================
   11. SALE_DETAIL - line items of a sale
   ============================================================ */
CREATE TABLE SaleDetail (
    SaleDetailID  INT IDENTITY(1,1) PRIMARY KEY,
    SaleID        INT NOT NULL,
    MedicineID    INT NOT NULL,
    PrescriptionDetailID INT NULL,
    Quantity      INT NOT NULL CHECK (Quantity > 0),
    UnitPrice     DECIMAL(12,2) NOT NULL CHECK (UnitPrice >= 0),
    CONSTRAINT FK_SD_Sale     FOREIGN KEY (SaleID)     REFERENCES Sale(SaleID),
    CONSTRAINT FK_SD_Medicine FOREIGN KEY (MedicineID) REFERENCES Medicine(MedicineID),
    CONSTRAINT FK_SD_PrescriptionDetail FOREIGN KEY (PrescriptionDetailID) REFERENCES PrescriptionDetail(PrescriptionDetailID)
);
GO

/* ============================================================
   Seed data (optional, for quick testing)
   ============================================================ */


INSERT INTO Branch (BranchName, Address, Phone) VALUES
    ('Branch A - District 1', '123 Le Loi St', '0901234567'),
    ('Branch B - District 3', '45 Vo Van Tan St', '0907654321');
GO

/* ============================================================
   CATEGORY
   ============================================================ */
INSERT INTO Category (CategoryName) VALUES
('Pain Relief'),
('Antibiotics'),
('Vitamins'),
('Cold & Flu'),
('Digestive Health');
GO

/* ============================================================
   SUPPLIER
   ============================================================ */
INSERT INTO Supplier (SupplierName, Phone, Address) VALUES
('Mega Pharma Ltd', '0901111111', 'Ho Chi Minh City'),
('HealthCare Supply', '0902222222', 'Hanoi'),
('VietMed Distribution', '0903333333', 'Da Nang'),
('Global Pharma', '0904444444', 'Can Tho');
GO

/* ============================================================
   EMPLOYEE
   ============================================================ */
INSERT INTO Employee
(FullName, Username, PasswordHash, Role, BranchID, Phone, Email)
VALUES
('Nguyen Van An', 'manager_a', 'hash123', 'BranchManager', 1, '0911111111', 'an@pharmacy.com'),
('Tran Thi Bich', 'pharmacist_a1', 'hash123', 'Pharmacist', 1, '0911111112', 'bich@pharmacy.com'),
('Le Quoc Bao', 'pharmacist_a2', 'hash123', 'Pharmacist', 1, '0911111113', 'bao@pharmacy.com'),

('Pham Minh Duc', 'manager_b', 'hash123', 'BranchManager', 2, '0922222221', 'duc@pharmacy.com'),
('Vo Thi Hoa', 'pharmacist_b1', 'hash123', 'Pharmacist', 2, '0922222222', 'hoa@pharmacy.com'),
('Nguyen Thanh Tung', 'pharmacist_b2', 'hash123', 'Pharmacist', 2, '0922222223', 'tung@pharmacy.com');
GO

/* ============================================================
   MEDICINE
   ============================================================ */
INSERT INTO Medicine
(
    MedicineName,
    GenericName,
    Strength,
    DosageForm,
    Barcode,
    CategoryID,
    SupplierID,
    UnitPrice,
    RequiresPrescription,
    ReorderLevel
)
VALUES
('Panadol Extra', 'Paracetamol + Caffeine', '500mg', 'Tablet', 'MED001', 1, 1, 25000, 0, 20),
('Efferalgan', 'Paracetamol', '500mg', 'Tablet', 'MED002', 1, 1, 22000, 0, 20),
('Amoxicillin', 'Amoxicillin', '500mg', 'Capsule', 'MED003', 2, 2, 85000, 1, 15),
('Augmentin', 'Amoxicillin + Clavulanate', '875mg', 'Tablet', 'MED004', 2, 2, 145000, 1, 10),
('Vitamin C', 'Ascorbic Acid', '1000mg', 'Tablet', 'MED005', 3, 3, 120000, 0, 15),
('Centrum', 'Multivitamin', 'Standard', 'Tablet', 'MED006', 3, 3, 250000, 0, 10),
('Decolgen', 'Paracetamol Combination', '500mg', 'Tablet', 'MED007', 4, 4, 35000, 0, 20),
('Tiffy', 'Cold Relief Formula', 'Standard', 'Tablet', 'MED008', 4, 4, 30000, 0, 20),
('Smecta', 'Diosmectite', '3g', 'Powder', 'MED009', 5, 1, 85000, 0, 15),
('Enterogermina', 'Bacillus Clausii', '2 Billion', 'Liquid', 'MED010', 5, 2, 95000, 0, 15);
GO

/* ============================================================
   INVENTORY
   ============================================================ */
INSERT INTO Inventory
(
    BranchID,
    MedicineID,
    BatchNumber,
    Quantity,
    ManufactureDate,
    ExpiryDate,
    PurchasePrice
)
VALUES
(1,1,'PA001',200,'2025-01-01','2027-01-01',18000),
(1,2,'EF001',150,'2025-02-01','2027-02-01',16000),
(1,3,'AM001',100,'2025-01-15','2026-12-31',70000),
(1,4,'AU001',80,'2025-03-01','2027-03-01',120000),
(1,5,'VC001',120,'2025-01-10','2027-01-10',95000),

(2,1,'PA002',180,'2025-01-01','2027-01-01',18000),
(2,6,'CE001',90,'2025-02-15','2027-02-15',210000),
(2,7,'DE001',160,'2025-01-20','2026-12-20',25000),
(2,8,'TI001',170,'2025-03-05','2027-03-05',22000),
(2,10,'EN001',110,'2025-02-01','2027-02-01',80000);
GO

/* ============================================================
   CUSTOMER
   ============================================================ */
INSERT INTO Customer
(
    FullName,
    Phone,
    Address,
    Email,
    DateOfBirth,
    Gender,
    Allergies,
    LoyaltyPoints
)
VALUES
('Nguyen Van Nam','0981000001','District 1','nam@gmail.com','1990-05-10','Male','Penicillin',120),
('Tran Thi Lan','0981000002','District 3','lan@gmail.com','1988-11-20','Female',NULL,250),
('Le Minh Khoa','0981000003','District 5','khoa@gmail.com','1995-03-14','Male',NULL,60),
('Pham Thu Ha','0981000004','District 7','ha@gmail.com','1992-07-01','Female','Aspirin',90),
('Vo Quoc Huy','0981000005','District 10','huy@gmail.com','1985-09-09','Male',NULL,300);
GO

/* ============================================================
   PRESCRIPTION
   ============================================================ */
INSERT INTO Prescription
(
    CustomerID,
    DoctorName,
    IssueDate,
    ExpiryDate,
    Notes
)
VALUES
(1,'Dr. Nguyen Hai','2026-06-01','2026-07-01','Take after meals'),
(2,'Dr. Tran Minh','2026-06-03','2026-07-03','Complete full course');
GO

/* ============================================================
   PRESCRIPTION DETAIL
   ============================================================ */
INSERT INTO PrescriptionDetail
(
    PrescriptionID,
    MedicineID,
    Dosage,
    QuantityPrescribed,
    Instructions
)
VALUES
(1,3,'1 capsule x 3 times/day',30,'After meals'),
(1,5,'1 tablet/day',10,'Morning only'),

(2,4,'1 tablet x 2 times/day',20,'After meals'),
(2,10,'1 vial/day',10,'Before breakfast');
GO

/* ============================================================
   SALE
   ============================================================ */
INSERT INTO Sale
(
    BranchID,
    EmployeeID,
    CustomerID,
    PrescriptionID,
    SaleDate,
    TotalAmount
)
VALUES
(1,2,1,1,'2026-06-05',205000),
(2,5,2,2,'2026-06-06',240000),
(1,3,3,NULL,'2026-06-07',47000),
(2,5,NULL,NULL,'2026-06-08',60000);
GO

/* ============================================================
   SALE DETAIL
   ============================================================ */
INSERT INTO SaleDetail
(
    SaleID,
    MedicineID,
    PrescriptionDetailID,
    Quantity,
    UnitPrice
)
VALUES
(1,3,1,1,85000),
(1,5,2,1,120000),

(2,4,3,1,145000),
(2,10,4,1,95000),

(3,1,NULL,1,25000),
(3,7,NULL,1,22000),

(4,8,NULL,2,30000);
GO

/* ============================================================
   STOCK TRANSFER
   ============================================================ */
INSERT INTO StockTransfer
(
    FromBranchID,
    ToBranchID,
    TransferDate,
    Status
)
VALUES
(1,2,'2026-06-10','Completed'),
(2,1,'2026-06-12','Pending');
GO

/* ============================================================
   STOCK TRANSFER DETAIL
   ============================================================ */
INSERT INTO StockTransferDetail
(
    TransferID,
    MedicineID,
    BatchNumber,
    Quantity
)
VALUES
(1,3,'AM001',20),
(1,5,'VC001',15),
(2,7,'DE001',30);
GO
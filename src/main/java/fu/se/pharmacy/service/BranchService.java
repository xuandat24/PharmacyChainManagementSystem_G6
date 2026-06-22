package fu.se.pharmacy.service;
import fu.se.pharmacy.entity.Branch;
import java.util.List;

public interface BranchService {
    List<Branch> getAllBranches();
    Branch saveBranch(Branch branch);
    Branch getBranchById(Integer id);
    void deleteBranch(Integer id);
}
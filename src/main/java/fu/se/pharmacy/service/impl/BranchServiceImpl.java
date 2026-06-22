package fu.se.pharmacy.service.impl;

import fu.se.pharmacy.entity.Branch;
import fu.se.pharmacy.repository.BranchRepository;
import fu.se.pharmacy.service.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepository;

    @Override
    public List<Branch> getAllBranches() {
        return branchRepository.findAll();
    }

    @Override
    public Branch saveBranch(Branch branch) {
        return branchRepository.save(branch);
    }

    @Override
    public Branch getBranchById(Integer id) {
        return branchRepository.findById(id).orElse(null);
    }
    @Override
    public void deleteBranch(Integer id) {
        Branch branch = getBranchById(id);
        if (branch != null) {
            branch.setStatus("INACTIVE"); // Không xóa cứng
            branchRepository.save(branch);
        }
    }
}
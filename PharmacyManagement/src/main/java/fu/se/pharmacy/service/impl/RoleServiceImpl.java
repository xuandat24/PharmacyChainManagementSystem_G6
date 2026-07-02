package fu.se.pharmacy.service.impl;

import fu.se.pharmacy.entity.Role;
import fu.se.pharmacy.repository.RoleRepository;
import fu.se.pharmacy.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {
    private final RoleRepository roleRepository;

    @Override
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }
}
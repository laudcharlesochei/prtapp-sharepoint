package prt.springbootthymeleafcrudwebapp.service;

import java.util.List;
import org.springframework.stereotype.Service;
import prt.springbootthymeleafcrudwebapp.model.Employee;
import prt.springbootthymeleafcrudwebapp.repository.SharePointEmployeeRepository;
import prt.springbootthymeleafcrudwebapp.sharepoint.SharePointException;

/**
 * Employee operations backed by the SharePoint Online list
 * (previously JPA + JawsDB MySQL on Heroku).
 */
@Service
public class EmployeeServiceImpl implements EmployeeService {

    private final SharePointEmployeeRepository employeeRepository;

    public EmployeeServiceImpl(SharePointEmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    // return a list of employees to the controller
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    @Override
    public void saveEmployee(Employee employee) {
        this.employeeRepository.save(employee);
    }

    @Override
    public Employee getEmployeeById(long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new SharePointException(404, "Employee not found for id:: " + id));
    }

    @Override
    public void deleteEmployeeById(long id) {
        this.employeeRepository.deleteById(id);
    }
}

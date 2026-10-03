package prt.springbootthymeleafcrudwebapp.service;

import java.util.List;
import prt.springbootthymeleafcrudwebapp.model.Employee;

public interface EmployeeService {
    List<Employee> getAllEmployees();

    //Save employee by passing the Employee object into the method (creates or updates the SharePoint item)
    void saveEmployee(Employee employee);

    //get Employee by ID (SharePoint list item ID)
    Employee getEmployeeById(long id);

    void deleteEmployeeById(long id);
}

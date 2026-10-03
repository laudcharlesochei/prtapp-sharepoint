package prt.springbootthymeleafcrudwebapp.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import prt.springbootthymeleafcrudwebapp.model.Employee;
import prt.springbootthymeleafcrudwebapp.repository.SharePointEmployeeRepository;
import prt.springbootthymeleafcrudwebapp.service.EmployeeService;
import prt.springbootthymeleafcrudwebapp.sharepoint.SharePointException;

/**
 * JSON API.
 *
 * @author Laud.Ochei
 */
@RestController
@RequestMapping("/api/v1")
public class EmployeeControllerAPI {

    private final EmployeeService employeeService;
    private final SharePointEmployeeRepository repository;

    public EmployeeControllerAPI(EmployeeService employeeService, SharePointEmployeeRepository repository) {
        this.employeeService = employeeService;
        this.repository = repository;
    }

    // list of employees from SharePoint
    @GetMapping("/employeelist")
    public List<Employee> displayAllEmployees() {
        return employeeService.getAllEmployees();
    }

    // Diagnostics: resolved site/list ids and the column mapping in use
    @GetMapping("/sharepoint/status")
    public Map<String, Object> sharePointStatus() {
        return repository.status();
    }

    // Diagnostics: the list's columns - use the "name" values for SHAREPOINT_FIELD_* config vars
    @GetMapping("/sharepoint/columns")
    public List<Map<String, Object>> sharePointColumns() {
        return repository.listColumns();
    }

    @ExceptionHandler(SharePointException.class)
    public ResponseEntity<Map<String, Object>> handleSharePointError(SharePointException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("graphStatus", ex.getStatus());
        body.put("error", ex.getMessage());
        body.put("hint", ex.getHint());
        HttpStatus status = ex.getStatus() == 404 ? HttpStatus.NOT_FOUND : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status).body(body);
    }
}

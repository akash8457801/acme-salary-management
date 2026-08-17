package co.acme.salary.service;

/** Something was addressed by id that does not exist. Maps to HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException employee(Long id) {
        return new ResourceNotFoundException("No employee with id " + id);
    }
}

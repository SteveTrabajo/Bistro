package logic.repository;

import java.sql.SQLException;
import java.util.List;

/**
 * Defines the contract for accessing code persistence (the database).
 * This structure ensures testability and separation of concerns.
 */
public interface CodeRepository {
    
    List<String> loadUsedCodes() throws SQLException; 

    void insertAssignment(String code, int userId) throws SQLException; 

    void insertBatch(List<String> codes) throws SQLException; 

    void releaseCodeFromDB(String code) throws SQLException; 

    /**
     * Retrieves the User ID associated with a given code directly from the DB.
     * @return The User ID, or -1 if the code is not found or is inactive.
     */
    int getUserIdByCode(String code) throws SQLException; 
}

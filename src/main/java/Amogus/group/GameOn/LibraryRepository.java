package Amogus.group.GameOn;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LibraryRepository extends JpaRepository<Library, Long> {
    
    List<Library> findByUser_UserId(Long userId);
    
    boolean existsByUser_UserIdAndGame_Id(Long userId, Long gameId);
}
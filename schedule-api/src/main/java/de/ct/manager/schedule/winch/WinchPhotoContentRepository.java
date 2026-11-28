package de.ct.manager.schedule.winch;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WinchPhotoContentRepository extends JpaRepository<WinchPhotoContent, String> {

    Optional<WinchPhotoContent> findByPhoto(WinchPhoto photo);
}

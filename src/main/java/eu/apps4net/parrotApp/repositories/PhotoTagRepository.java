package eu.apps4net.parrotApp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import eu.apps4net.parrotApp.models.MediaFile;
import eu.apps4net.parrotApp.models.PhotoTag;

import java.util.Optional;

@Repository
public interface PhotoTagRepository extends JpaRepository<PhotoTag, Long> {

	Optional<PhotoTag> findByMediaFile(MediaFile mediaFile);
}

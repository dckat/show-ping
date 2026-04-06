package com.ssginc.showpingrefactoring.domain.stream.repository;

import com.ssginc.showpingrefactoring.domain.stream.dto.response.ClipResponseDto;
import com.ssginc.showpingrefactoring.domain.stream.entity.Clip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ClipRepository extends JpaRepository<Clip, Long> {

    @Query(value =
        "SELECT c.clip_no AS clipNo, s.stream_title AS streamTitle, c.clip_path AS clipPath " +
        "FROM clip c " +
        "JOIN stream s ON c.stream_no = s.stream_no " +
        "ORDER BY c.clip_no DESC LIMIT 4",
    nativeQuery = true)
    List<ClipProjection> findLatest4WithStreamNative();

}

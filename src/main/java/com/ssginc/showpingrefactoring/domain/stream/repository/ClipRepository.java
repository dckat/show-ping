package com.ssginc.showpingrefactoring.domain.stream.repository;

import com.ssginc.showpingrefactoring.domain.stream.entity.Clip;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClipRepository extends JpaRepository<Clip, Long> {
}

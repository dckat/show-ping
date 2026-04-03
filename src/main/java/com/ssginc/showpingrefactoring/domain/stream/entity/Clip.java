package com.ssginc.showpingrefactoring.domain.stream.entity;

import com.ssginc.showpingrefactoring.domain.member.entity.Member;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;


@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "clip")
public class Clip {

    @Id
    @Column(name = "clip_no")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long clipNo;

    // 영상
    // 영상클립 : 영상은 N : 1의 관계를 가진다.
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stream_no", referencedColumnName = "stream_no")
    private Stream stream;

    @NotNull
    @Column(name = "clip_path")
    private String clipPath;

}

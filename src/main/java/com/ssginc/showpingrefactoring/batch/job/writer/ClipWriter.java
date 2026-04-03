package com.ssginc.showpingrefactoring.batch.job.writer;

import com.ssginc.showpingrefactoring.domain.stream.dto.object.ClipSegment;
import com.ssginc.showpingrefactoring.domain.stream.entity.Clip;
import com.ssginc.showpingrefactoring.domain.stream.entity.Stream;
import com.ssginc.showpingrefactoring.domain.stream.repository.ClipRepository;
import com.ssginc.showpingrefactoring.domain.stream.repository.LiveRepository;
import com.ssginc.showpingrefactoring.infrastructure.NCP.storage.StorageLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

@Component
@StepScope
@RequiredArgsConstructor
@Slf4j
public class ClipWriter implements ItemWriter<List<ClipSegment>> {

    @Value("#{jobParameters['streamTitle']}")
    private String streamTitle;

    @Value("#{jobParameters['streamNo']}")
    private Long streamNo;

    private final StorageLoader storageLoader;

    private final LiveRepository liveRepository;

    private final ClipRepository clipRepository;

    @Override
    public void write(Chunk<? extends List<ClipSegment>> items) throws Exception {
        String folderPath = "clips/" + streamNo;

        Stream stream = liveRepository.findById(streamNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스트림입니다."));

        for (List<ClipSegment> segments : items) {
            for (int i = 0; i < segments.size(); i++) {
                String output = String.format("%s_%d_%d.mp4", streamTitle, streamNo, i);
                String thumb = output.replace(".mp4", ".jpg");

                File clipFile = new File(output);
                File thumbFile = new File(thumb);

                try {
                    // 클립 추출
                    extractClip(segments.get(i), output, thumb);

                    // 클립 업로드 (NCP)
                    String uploadUrl = storageLoader.uploadShortFormFile(clipFile, thumbFile, folderPath);
                    log.info("NCP 업로드 성공: [업로드 Url] {}", uploadUrl);

                    // 클립 메타데이터 DB 저장
                    Clip clip = Clip.builder()
                            .stream(stream)
                            .clipPath(uploadUrl)
                            .build();

                    clipRepository.save(clip);
                    log.info("DB 저장 완료: Clip No {}", clip.getClipNo());
                } finally {
                    if (clipFile.exists()) clipFile.delete();
                    if (thumbFile.exists()) thumbFile.delete();
                }
            }
        }
    }

    private void extractClip(ClipSegment seg, String out, String thumb) throws Exception {
        new ProcessBuilder("ffmpeg", "-y", "-ss", String.valueOf(seg.startTime()), "-to", String.valueOf(seg.endTime()),
                "-i", "input.mp4", "-c", "copy", out).start().waitFor();

        new ProcessBuilder("ffmpeg", "-y", "-ss", String.valueOf(seg.startTime() + 2),
                "-i", "input.mp4", "-vframes", "1", "-q:v", "2", thumb).start().waitFor();
    }

}

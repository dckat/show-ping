package com.ssginc.showpingrefactoring.batch.job.writer;

import com.ssginc.showpingrefactoring.domain.stream.dto.object.ClipSegment;
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

    @Override
    public void write(Chunk<? extends List<ClipSegment>> items) throws Exception {
        String folderPath = "clips/" + streamNo;

        for (List<ClipSegment> segments : items) {
            for (int i = 0; i < segments.size(); i++) {
                String output = String.format("%s_%d_%d.mp4", streamTitle, streamNo, i);
                String thumb = output.replace(".mp4", ".jpg");

                File clipFile = new File(output);
                File thumbFile = new File(thumb);

                try {
                    extractClip(segments.get(i), output, thumb);

                    String uploadUrl = storageLoader.uploadShortFormFile(clipFile, thumbFile, folderPath);

                    log.info("NCP 업로드 성공: [업로드 Url] {}", uploadUrl);

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

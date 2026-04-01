package com.ssginc.showpingrefactoring.batch.job.writer;

import com.ssginc.showpingrefactoring.domain.stream.dto.object.ClipSegment;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@StepScope
public class ClipWriter implements ItemWriter<List<ClipSegment>> {

    @Value("#{jobParameters['streamTitle']}")
    private String streamTitle;

    @Value("#{jobParameters['streamNo']}")
    private Long streamNo;

    @Override
    public void write(Chunk<? extends List<ClipSegment>> items) throws Exception {
        for (List<ClipSegment> segments : items) {
            for (int i = 0; i < segments.size(); i++) {
                String output = String.format("%s_%d_%d.mp4", streamTitle, streamNo, i);
                extractClip(segments.get(i), output);
            }
        }
    }

    private void extractClip(ClipSegment seg, String out) throws Exception {
        new ProcessBuilder("ffmpeg", "-y", "-ss", String.valueOf(seg.startTime()), "-to", String.valueOf(seg.endTime()),
                "-i", "input.mp4", "-c", "copy", out).start().waitFor();

    }

}

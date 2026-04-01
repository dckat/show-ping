package com.ssginc.showpingrefactoring.batch.job.processor;

import com.ssginc.showpingrefactoring.domain.stream.dto.object.ClipSegment;
import com.ssginc.showpingrefactoring.domain.stream.dto.object.LiveStreamLogInfo;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class ClipProcessor implements ItemProcessor<LiveStreamLogInfo, List<ClipSegment>> {

    private static final double THRESHOLD = 1.8;

    @Override
    public List<ClipSegment> process(LiveStreamLogInfo data) {
        List<ClipSegment> candidates = new ArrayList<>();
        double vEma = -1;

        List<Long> tsList = data.getViewerMap().keySet().stream().sorted().toList();

        for (long t : tsList) {
            double vRaw = data.getViewerMap().get(t);

            if (vEma < 0) {
                vEma = vRaw;
                continue;
            }

            vEma = (vRaw * 0.3) + (vEma * 0.7);

            double baseScore = vRaw / Math.max(vEma, 1);
            double scaleWeight = Math.log10(Math.max(vRaw, 10));

            // 시작과 끝 부분은 점수 패널티 적용 (fake Clip 방지)
            double timePenalty = (t < 300 || (data.getStreamDuration() - t) < 300) ? 0.5 : 1.0;

            if (baseScore * scaleWeight * timePenalty >= THRESHOLD) {
                candidates.add(new ClipSegment(t - 15, t + 5).clamp(data.getStreamDuration()));
            }
        }

        return mergeSegments(candidates);
    }

    private List<ClipSegment> mergeSegments(List<ClipSegment> segments) {
        if (segments.isEmpty()) return segments;

        segments.sort(Comparator.comparingLong(ClipSegment::startTime));

        List<ClipSegment> merged = new ArrayList<>();
        ClipSegment curr = segments.get(0);

        for (int i = 1; i < segments.size(); i++) {
            ClipSegment next = segments.get(i);

            if (next.startTime() <= curr.endTime() + 2) {
                curr = new ClipSegment(curr.startTime(), Math.max(curr.endTime(), next.endTime()));
            } else {
                merged.add(curr);
                curr = next;
            }
        }
        merged.add(curr);
        return merged;
    }

}

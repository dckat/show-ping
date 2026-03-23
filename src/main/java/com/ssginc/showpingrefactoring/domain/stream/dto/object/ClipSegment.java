package com.ssginc.showpingrefactoring.domain.stream.dto.object;

public record ClipSegment(Long startTime, Long endTime) {

    public ClipSegment clamp(Long streamDuration) {
        Long correctedStartTime = Math.max(0, this.startTime);
        Long correctedEndTime = Math.min(streamDuration, endTime);

        return new ClipSegment(correctedStartTime, correctedEndTime);
    }

    public boolean isInvalid() {
        return endTime <= startTime || (endTime - startTime) < 5;
    }
}

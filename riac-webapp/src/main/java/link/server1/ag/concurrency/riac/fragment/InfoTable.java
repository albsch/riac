package link.server1.ag.concurrency.riac.fragment;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class InfoTable {
    private final String percentSolved;
    private final String percentTimeout;
    private final String percentMemory;
    private final String percentEasy;
    private final String percentQueued;

    private final String solved;
    private final String timeout;
    private final String memory;
    private final String easy;
    private final String queued;
}

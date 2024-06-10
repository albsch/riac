package link.server1.ag.concurrency.riac.fragment;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data @Builder
public class BenchmarkHeader {
    private final String shortName;
    private final String fullName;
    private final Integer column;
}

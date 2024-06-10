package link.server1.ag.concurrency.riac.fragment;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data @Builder
public class PrefixedBenchmarkSet {
    private final String prefix;
    private final String name;
    private final List<SingleBenchmarkSetResult> benchmarks;
    private final Integer solved;
}

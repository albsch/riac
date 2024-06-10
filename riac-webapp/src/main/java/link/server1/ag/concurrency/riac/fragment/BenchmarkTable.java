package link.server1.ag.concurrency.riac.fragment;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data @Builder
public class BenchmarkTable {
    private final String firstHeader;
    private final List<BenchmarkHeader> headers;
    private final List<List<SingleBenchmarkResult>> rows;
}

package link.server1.ag.concurrency.riac.fragment;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class BenchmarkSetDescription {
    private final String name;
    private final String prefix;
}

package link.server1.ag.concurrency.riac.fragment;

import lombok.Builder;
import lombok.Data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

@Data @Builder
public class SingleBenchmarkSetResult implements Comparable<SingleBenchmarkSetResult>{
    private final String name;
    private final Integer solved;

    @Override
    public int compareTo(SingleBenchmarkSetResult o) {
        return name.compareTo(o.name);
    }
}


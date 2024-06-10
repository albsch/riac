package link.server1.ag.concurrency.riac;


import com.fasterxml.jackson.databind.ObjectMapper;
import link.server1.ag.concurrency.riac.fragment.SingleBenchmarkResult;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

@Service
@Slf4j
public class ScheduledBenchmarks {
    private static final int TIME_LIMIT=30;

    private Queue<SingleBenchmark> queue = new ArrayBlockingQueue<>(10);

    private SingleBenchmark currentJob = null;
    private ObjectMapper mapper = new ObjectMapper();
    private long lastStart;
    private Process lastProcess;
    private boolean limitReached = false;


    @Scheduled(fixedRate = 5000)
    public void singleBenchmark() {
        if(currentJob == null) {
            SingleBenchmark single = queue.poll();
            if(single != null) {
                currentJob = single;
                processBenchmark(single);
            }
        }
    }

    @Scheduled(fixedRate = 5000)
    public void healthCheck() throws IOException {
        if(lastProcess != null && lastProcess.isAlive()) {
            long currentTime = System.nanoTime();
            long diffSec = (currentTime-lastStart)/1000000000;

            if(diffSec > TIME_LIMIT) {
                log.warn("Time limit reached ({}). Killing job: {}", TIME_LIMIT, currentJob);

                limitReached = true;
                lastProcess.destroyForcibly();
                lastProcess = null;
                lastStart = 0;
            }
        }
    }

    boolean offer(SingleBenchmark request) {
        return queue.contains(request) || request.equals(currentJob) || queue.offer(request);
    }

    private void processBenchmark(SingleBenchmark request) {
        File fA = new File("/tmp/riac/"+request.getA());
        File fB = new File("/tmp/riac/"+request.getB());
        if(!fA.exists()) throw new RiacController.FormatException("Automaton A not found! Upload again:"+ request.getA());
        if(!fB.exists()) throw new RiacController.FormatException("Automaton B not found! Upload again:"+ request.getB());

        ProcessBuilder pb = new ProcessBuilder(new File(RiacApplication.APP_PATH).getAbsolutePath(), fA.getAbsolutePath(), fB.getAbsolutePath());
        if(request.getArgs() != null && !request.getArgs().isEmpty()) {
            pb = new ProcessBuilder(new File(RiacApplication.APP_PATH).getAbsolutePath(), fA.getAbsolutePath(), fB.getAbsolutePath(), request.getArgs());
        }

        pb.directory(new File("."));

        Boolean included = null;
        String elapsed = null;

        try {
            lastStart = System.nanoTime();
            lastProcess = pb.start();

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(lastProcess.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String line;
            while ( (line = reader.readLine()) != null) {
                if (line.startsWith("RESULT:")) {
                    included = !line.split(":")[1].trim().equalsIgnoreCase("false");
                } else if (line.startsWith("T_TOTAL:")) {
                    elapsed = "("+Long.valueOf(line.split(":")[1].trim())/1000+" ms)";
                }

                builder.append(line);
                builder.append(System.getProperty("line.separator"));
            }

            if(limitReached) {
                builder.append("#TIME LIMIT REACHED: "+TIME_LIMIT+"s");
                limitReached = false;
            }

            String result = builder.toString();

            SingleBenchmarkResult benchmarkResult = SingleBenchmarkResult.builder()
                    .output(result)
                    .time(elapsed)
                    .included(included)
                    .build();
            String benchmarkFileName = request.getA()+"_"+request.getB()+".txt";
            File benchmarkFile = new File("benchmarks/"+benchmarkFileName);
            mapper.writeValue(benchmarkFile, benchmarkResult);
        } catch (IOException e) {
            log.error("{}", e.getMessage());
            e.printStackTrace();
        } finally {
            currentJob = null;
            lastProcess = null;
            lastStart = 0;
        }
    }

    JobHealth getPosition(SingleBenchmark request) {
        int pos = 1;
        for (SingleBenchmark singleBenchmark : queue) {
            if(singleBenchmark.equals(request)) {
                break;
            }
            pos++;
        }

        long currentTime = System.nanoTime();
        long diffSec = (currentTime-lastStart)/1000000000;

        if(request.equals(currentJob)) {
            return JobHealth.builder().running(true).time(diffSec).build();
        } else if(queue.contains(request)) {
            return JobHealth.builder().running(false).time(pos).build();
        } else {
            return null;
        }
    }

    @Data @Builder
    static class JobHealth {
        private boolean running;
        private long time;

        @Override
        public String toString() {
            if(!running) {
                return "Positition in Queue: "+time;
            }
            return "Running: "+(time > TIME_LIMIT ? TIME_LIMIT : time)+"s / "+TIME_LIMIT+"s";
        }
    }
}

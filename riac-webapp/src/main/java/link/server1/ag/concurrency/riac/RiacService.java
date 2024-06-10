package link.server1.ag.concurrency.riac;

import com.fasterxml.jackson.databind.ObjectMapper;
import link.server1.ag.concurrency.riac.ScheduledBenchmarks.JobHealth;
import link.server1.ag.concurrency.riac.fragment.SingleBenchmarkResult;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.*;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RiacService {

    private @NonNull final ScheduledBenchmarks benchmarks;
    private ObjectMapper mapper = new ObjectMapper();

    SingleBenchmarkResult check(SingleBenchmark request) {
        String benchmarkFileName = request.getA()+"_"+request.getB()+".txt";
        File benchmarkFile = new File("benchmarks/"+benchmarkFileName);
        if(benchmarkFile.exists()) {
            log.info("Fetching cached result: {}", benchmarkFileName);
            try {
                return mapper.readValue(benchmarkFile, SingleBenchmarkResult.class);
            } catch (IOException e) {
                throw new CapacityException("");
            }
        } else {
            if(!benchmarks.offer(request)) {
                log.info("Queue full");
                throw new CapacityException("Benchmark queue limit exceeded, wait for jobs to finish");
            } else {
                JobHealth pos = benchmarks.getPosition(request);
                return SingleBenchmarkResult.builder().output("Still processing. Try again later. "+pos.toString()).build();
            }
        }
    }

    public String generateRandomAutomaton(Integer states, Integer finalStates, Integer connectivity, Integer alphabet) {
        String uuid = UUID.randomUUID().toString();
        String pathArg = "/tmp/riac/"+uuid+".dot";

        ProcessBuilder pb = new ProcessBuilder(new File(RiacApplication.APP_PATH).getAbsolutePath(),
                "-r",
                "-s", String.valueOf(states),
                "-f", String.valueOf(finalStates),
                "-c", String.valueOf(connectivity),
                "-E", String.valueOf(alphabet)
                );

        pb.directory(new File("."));

        try (PrintWriter pw = new PrintWriter(pathArg)){
            Process p = pb.start();
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            StringBuilder output = new StringBuilder();
            while ( (line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            pw.println(output.toString());

            return uuid+".dot";
        } catch (Exception e) {
            log.error("Could not save randomly generated automaton: {}", e.getMessage());
            return "internal server error";
        }
    }

    String getFileFor(String fileName) {
        return RiacApplication.TMP_PATH+fileName;
    }

    public String getAutomatonFor(String benchmarkSet, String prefix, String automaton) {
        return "results/"+prefix+"/"+benchmarkSet+"/"+automaton;
    }

    @ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE)
    static class CapacityException extends RuntimeException{
        CapacityException(String s){
            super(s);
        }
    }

}

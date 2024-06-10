package link.server1.ag.concurrency.riac;

import jakarta.servlet.http.HttpServletResponse;
import link.server1.ag.concurrency.riac.fragment.*;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static link.server1.ag.concurrency.riac.RiacApplication.TMP_PATH;

@Slf4j
@Controller
@RequiredArgsConstructor
public class RiacController {

    private @NonNull final RiacService service;

    @RequestMapping(value = "/automaton/{file_name}", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseBody
    public FileSystemResource getFile(@PathVariable("file_name") String fileName, HttpServletResponse response) {
        FileSystemResource res = new FileSystemResource(service.getFileFor(fileName));
        response.setHeader("Content-Disposition", "attachment; filename="+fileName);
        return res;
    }

    @RequestMapping(value = "/benchmarks/result/download", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseBody
    public FileSystemResource getAutomaton(
            @RequestParam("automaton") String automaton,
            @RequestParam("set") String benchmarkSet,
            @RequestParam("prefix") String prefix,
            HttpServletResponse response
    ) {
        FileSystemResource res = new FileSystemResource(service.getAutomatonFor(benchmarkSet, prefix, automaton));
        response.setHeader("Content-Disposition", "attachment; filename="+automaton);
        return res;
    }

    @GetMapping("/benchmarks/result")
    @ResponseBody
    public SingleBenchmarkResult getResultInfo(
            @RequestParam("automatonA") String automatonA,
            @RequestParam("automatonB") String automatonB,
            @RequestParam("set") String benchmarkSet,
            @RequestParam("prefix") String prefix,
            @RequestParam(value = "includePre", required = false, defaultValue = "false") boolean pre
    ) {
        File f = new File("results/"+prefix+"/"+benchmarkSet+"/benchmark/results/"+automatonA+"_"+automatonB+".result");

        if(!f.exists()) {
            throw new RequestException("Benchmark result does not exist!");
        }

        return SingleBenchmarkResult.convertFromFile(f, automatonA, automatonB, benchmarkSet, pre);
    }

    @GetMapping("/benchmarks/{set}")
    public ModelAndView showBenchmarks(
            @PathVariable(value = "set") String benchmarkSet,
            @RequestParam("prefix") String prefix
    ) throws IOException {
        Long id = 0L;

        File set = new File( "results/"+prefix+"/"+benchmarkSet+"/benchmark/"+benchmarkSet+".tests");
        if(!set.exists()) {
            throw new RequestException(benchmarkSet + " either does not exists or is not benchmarked yet");
        }
        ModelAndView modelAndView = new ModelAndView("benchmarks");

        int columnCount = 0;
        List<BenchmarkHeader> headers = new ArrayList<>();
        // calculate column count and headers
        try (BufferedReader reader = new BufferedReader(new FileReader(set))) {
            String a1 = reader.readLine().split(" ")[0];
            String a2 = a1;
            headers.add(BenchmarkHeader.builder().fullName(a1).column(columnCount+1).build());
            while(a2.equals(a1)) {
                String [] split = reader.readLine().split(" ");
                a2 = split[0];
                columnCount++;
                headers.add(BenchmarkHeader.builder().fullName(split[1]).column(columnCount+1).build());
            }
        }
        headers.remove(headers.size()-1);

        List<List<SingleBenchmarkResult>> rows = new ArrayList<>();
        for (int i = 0; i < headers.size(); i++) {
            List<SingleBenchmarkResult> col = new ArrayList<>();
            rows.add(col);
        }

        Integer line = 0;
        Integer timeouts = 0;
        Integer memoryOut = 0;
        Integer under10s = 0;
        Integer solved = 0;
        Integer notSolved = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(set))) {
            String str;
            while((str = reader.readLine()) != null){
                String[] split = str.split(" ");

                //hack
                if(split[4].isEmpty()) split[4] = split[5];

                int col = (line / columnCount) + 1;
                int row = (line % columnCount) + 1;

                List<SingleBenchmarkResult> currentCol = rows.get(row-1);

                if(col == 1) {
                    //create left 'header'
                    currentCol.add(SingleBenchmarkResult.builder().rowHeader(true).automatonA(split[1]).build());
                }

                SingleBenchmarkResult.SingleBenchmarkResultBuilder builder = SingleBenchmarkResult.builder();
                builder.rowHeader(false)
                        .included(Boolean.valueOf(split[4]))
                        .automatonA(split[0])
                        .automatonB(split[1])
                        .row(row)
                        .column(col);

                int time = 0;
                //result
                if(split[4].contains("?")) {
                    builder.included(null);
                    notSolved++;
                }

                else if(split[4].contains("timeout")) {
                    builder.included(null);
                    timeouts++;
                    builder.timeout(true);
                }

                else if(split[4].contains("memory")) {
                    builder.included(null);
                    memoryOut++;
                    builder.memory(true);
                }
                else if(split[4].contains("UNDEFINED")) {
                        builder.included(null);
                        builder.error(true);
                } else {
                    time = Integer.MAX_VALUE;
                    try {
                        time = Integer.parseInt(split[3])/1000;
                        solved++;
                    } catch (NumberFormatException ignored) {}
                }


                String timeFormat;
                if(time >= 10000) {
                    timeFormat = time/1000+"s";
                    builder.time(timeFormat).severity(1);
                } else if(time > 0){
                    timeFormat = time+"ms";
                    builder.time(timeFormat).severity(0);
                    under10s++;
                }

                currentCol.add(builder.id(id++).build());

                line++;
            }
        }

        { // model attributes
            // benchmark description
            BenchmarkSetDescription description = BenchmarkSetDescription.builder().name(benchmarkSet).prefix(prefix).build();
            modelAndView.addObject("set", description);

            // info table
            float percentSolved = (solved/(float) line)*100;
            float percentTimeout = (timeouts/(float) line)*100;
            float percentMemory = (memoryOut/(float) line)*100;
            float percentEasy = (under10s/(float) line)*100;
            float percentNotSolved = (notSolved/(float) line)*100;
            InfoTable table = InfoTable.builder()
                    .solved(String.valueOf(solved))
                    .timeout(String.valueOf(timeouts))
                    .memory(String.valueOf(memoryOut))
                    .easy(String.valueOf(under10s))
                    .queued(String.valueOf(notSolved))
                    .percentSolved(String.valueOf(Math.round(percentSolved)+"%"))
                    .percentTimeout(String.valueOf(Math.round(percentTimeout)+"%"))
                    .percentMemory(String.valueOf(Math.round(percentMemory)+"%"))
                    .percentEasy(String.valueOf(Math.round(percentEasy))+"%")
                    .percentQueued(String.valueOf(Math.round(percentNotSolved))+"%")
                    .build();
            modelAndView.addObject("infotable", table);

            // benchmark table
            BenchmarkTable benchmarkTable = BenchmarkTable.builder()
                    .firstHeader("⊇")
                    .headers(headers)
                    .rows(rows)
                    .build();
            modelAndView.addObject("benchmarktable", benchmarkTable);
        }

        return modelAndView;
    }

    @SuppressWarnings("ConstantConditions")
    @GetMapping("/benchmarks")
    public ModelAndView benchmarSummary() throws IOException {
        ModelAndView model = new ModelAndView("summary");

        File results = new File("results");
        List<PrefixedBenchmarkSet> sets = new ArrayList<>();

        if(results.exists() && results.isDirectory()) {
            // go through prefix
            for (File file : results.listFiles()) {
                List<SingleBenchmarkSetResult> benchmarks = new ArrayList<>();
                if(file.isDirectory()) {
                    // go through benchmark sets for the prefix
                    for (File benchmark : file.listFiles()) {
                        if (benchmark.isDirectory()) {
                            benchmarks.add(SingleBenchmarkSetResult.builder()
                                    .name(benchmark.getName())
                                    .solved(getSolvedInPercent(new File(benchmark.getAbsolutePath()+"/benchmark/"+benchmark.getName()+".tests")))
                                    .build());
                        }
                    }
                }

                benchmarks.sort(Comparator.naturalOrder());
                sets.add(PrefixedBenchmarkSet.builder().benchmarks(benchmarks).prefix(file.getName()).build());
            }
        }


        model.addObject("sets", sets);

        return model;
    }

    private Integer getSolvedInPercent(File file) throws IOException {
        int lines = 0;
        int timeouts = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while((line = reader.readLine()) != null) {
                String[] split = line.split(" ");
                if(split[4].isEmpty()) split[4] = split[5];

                if(split[4].contains("timeout")) timeouts++;
                if(split[4].contains("memory")) timeouts++;
                if(split[4].contains("?")) timeouts++;
                lines++;
            }
        }

        float solved = (lines - timeouts)/ (float) lines;
        solved = solved * 100;

        return (int) solved;
    }

    @GetMapping("/")
    public ModelAndView home(
            @RequestParam(required = false, value = "inverse") Boolean inverse,
            @RequestParam(required = false, value = "args") String args,
            @RequestParam(required = false, value = "checker") String version,
            @RequestParam(required = false, value = "automatonA") String automatonA,
            @RequestParam(required = false, value = "automatonB") String automatonB
    ) {
        ModelAndView modelAndView = new ModelAndView("home");
        if (version != null && automatonA != null && automatonB != null){
            log.info("Got request: [Version '{}'][Args '{}'][Automaton A '{}'][Automaton B '{}']"
                    , version, args, automatonA, automatonB);

            SingleBenchmarkResult output;
            if(inverse){
                output = service.check(new SingleBenchmark(version, args, automatonB, automatonA));
            } else{
                output = service.check(new SingleBenchmark(version, args, automatonA, automatonB));
            }
            log.info("Doing the check done: {}", output);
            List<String> consoleOutput = new ArrayList<>();
            consoleOutput.add(output.getOutput());

            modelAndView.addObject("output", consoleOutput);
            modelAndView.addObject("elapsed", output.getTime());
            modelAndView.addObject("result", output.getIncluded());
            modelAndView.addObject("inverted", inverse);
        }

        modelAndView.addObject("automatonA",automatonA);
        modelAndView.addObject("automatonB",automatonB);
        return modelAndView;
    }

    @PostMapping("/")
    public String handleFileUpload(
            @RequestParam("inverse") Boolean inverse,
            @RequestParam("args") String args,
            @RequestParam("version") String version,
            @RequestParam("automatonA") String aName,
            @RequestParam("automatonB") String bName,
            @RequestParam("aFile") MultipartFile aFile,
            @RequestParam("bFile") MultipartFile bFile
    ) throws IOException {
        { // validate
            if(aName == null || aName.isEmpty()) throw new FormatException("Automaton A not specified!");
            if(bName == null || bName.isEmpty()) throw new FormatException("Automaton B not specified!");
            if(version == null || version.isEmpty()) throw new FormatException("Version not specified!");

            if(!new File(TMP_PATH+aName).exists()) {
                if(aFile == null || aFile.isEmpty()) throw new FormatException("Automaton A empty or not specified!");
                if(!aFile.getOriginalFilename().endsWith(".dot") && !aFile.getOriginalFilename().endsWith(".vtf"))
                    throw new FormatException("Automaton A is not a .dot or .vtf file!");

                File localAFile = new File(TMP_PATH+aName);
                aFile.transferTo(localAFile);
                log.info("Uploaded '{}'", aName);
            }
            if(!new File(TMP_PATH+bName).exists()) {
                if(bFile == null || bFile.isEmpty()) throw new FormatException("Automaton B empty or not specified!");
                if(!bFile.getOriginalFilename().endsWith(".dot") && !bFile.getOriginalFilename().endsWith(".vtf"))
                    throw new FormatException("Automaton B is not a .dot or .vtf file!");

                File localBFile = new File(TMP_PATH+bName);
                bFile.transferTo(localBFile);
                log.info("Uploaded '{}'", bName);
            }
        }

        return "redirect:/?inverse="+inverse+"&args="+args+"&checker="+version+"&automatonA="+aName+"&automatonB="+bName;
    }

    @PostMapping("/random")
    @ResponseBody
    public String generateRandomAutomaton(
            @RequestParam("states") Integer states,
            @RequestParam("final") Integer finalStates,
            @RequestParam("connectivity") Integer connectivity,
            @RequestParam("alphabet") Integer alphabet
    ) {
        if(states < 0) throw new RequestException("Number of states must be positive.");
        if(finalStates < 0) throw new RequestException("Number of final states must be positive.");
        if(alphabet < 0) throw new RequestException("Alphabet size must be positive.");
        if(connectivity < 0 || connectivity > 100) throw new RequestException("Connectivity should be between 0 and 100.");
        if(alphabet > 5000) throw new RequestException("Alphabet size is limited because of processing limitation.");
        if(states > 5000) throw new RequestException("Number of states is limited because of processing limitation.");

        log.debug("Generating random automaton: {} | {} | {} | {}",states, finalStates, connectivity, alphabet );
        return service.generateRandomAutomaton(states,finalStates,connectivity,alphabet);
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    static class FormatException extends RuntimeException{
        FormatException(String s){
            super(s);
        }
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    static class RequestException extends RuntimeException{
        RequestException(String s){
            super(s);
        }
    }
}

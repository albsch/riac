package link.server1.ag.concurrency.riac.fragment;

import lombok.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class SingleBenchmarkResult {
    private  long id;
    private  String output;
    private  Boolean error;
    private  Boolean included;
    private  Boolean memory;
    private  Boolean timeout;
    private Boolean rowHeader;
    private  String time;
    private  String timeConvert;
    private String benchmarkSet;
    private String solver;
    private String automatonA;
    private String automatonB;
    private int row;
    private int column;
    private int severity;

    private int automatonAStates;
    private int automatonASymbols;
    private int automatonAFinal;
    private int automatonBStates;
    private int automatonBSymbols;
    private int automatonBFinal;

    public static SingleBenchmarkResult convertFromFile(File f, String A, String B, String set, boolean pre) {
        SingleBenchmarkResultBuilder builder = SingleBenchmarkResult.builder();
        builder.automatonA(A).automatonB(B).benchmarkSet(set);

        StringBuilder fullPre = new StringBuilder();

        boolean automatonA = true;
        try {
            try(BufferedReader reader = new BufferedReader(new FileReader(f))) {
                String line;
                while((line = reader.readLine()) != null) {
                    if(pre) fullPre.append(line).append("\n");

                    if(line.contains("States")) {
                        if(automatonA)
                            builder.automatonAStates(Integer.parseInt(line.split(":")[1].trim()));
                        else
                            builder.automatonBStates(Integer.parseInt(line.split(":")[1].trim()));
                    }
                    else if(line.contains("symbols")) {
                        if(automatonA)
                            builder.automatonASymbols(Integer.parseInt(line.split(":")[1].trim()));
                        else
                            builder.automatonBSymbols(Integer.parseInt(line.split(":")[1].trim()));
                    }
                    else if(line.contains("Final")) {
                        if(automatonA)
                            builder.automatonAFinal(Integer.parseInt(line.split(":")[1].trim()));
                        else
                            builder.automatonBFinal(Integer.parseInt(line.split(":")[1].trim()));
                    }
                    else if(line.contains("Automaton B")) {
                        automatonA = false;
                    }
                    else if(line.contains("T_CONVERT")) {
                        builder.timeConvert(line.split(":")[1].trim());
                    }
                    else if(line.contains("T_CHECK")) {
                        builder.time(line.split(":")[1].trim());
                    }
                    else if(line.contains("RESULT")) {
                        builder.included(Boolean.valueOf(line.split(":")[1].trim()));
                    }
                    else if(line.contains("terminated by signal 6")) {
                        builder.memory(true);
                    }
                    else if(line.contains("terminated by signal 24")) {
                        builder.timeout(true);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return builder.output(fullPre.toString()).build();
    }
}


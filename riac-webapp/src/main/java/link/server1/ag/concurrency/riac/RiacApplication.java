package link.server1.ag.concurrency.riac;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.io.*;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class RiacApplication {
	static String APP_PATH = "/app/main_run";
	static String TMP_PATH = "/tmp/riac/";

	public static void main(String[] args) {
		if(args.length == 1) APP_PATH = "main_run";

		File solver = new File(APP_PATH);
		if(!solver.exists()) throw new IllegalStateException("Solver not found!: "+APP_PATH);

		File tmp = new File(TMP_PATH);
		tmp.mkdirs();
		if(!tmp.exists()) throw new IllegalStateException("Temp directory not found!: "+TMP_PATH);

		File singleBenchmarkFolder = new File("benchmarks");
		singleBenchmarkFolder.mkdir();

		SpringApplication.run(RiacApplication.class, args);
	}

    @Bean()
    public ThreadPoolTaskScheduler taskScheduler(){
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(2);
        return  taskScheduler;
    }


}

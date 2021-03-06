import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Random;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.ArrayWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.ipc.Client;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.lib.TotalOrderPartitioner;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.filecache.DistributedCache;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.partition.InputSampler;
import org.apache.hadoop.mapreduce.v2.hs.HistoryClientService;
import org.apache.hadoop.util.ReflectionUtils;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.PropertyConfigurator;


public class BackSearchRateEntry extends Configured implements Tool {
	
	public static void main(String[] args) throws Exception {
		PropertyConfigurator.configure("log4j.properties");
		System.exit(ToolRunner.run(new BackSearchRateEntry(), args));
	}

	
	@Override
	public int run(String[] args) throws Exception {
		String input = "hdfs://cp01-ma-eval-001.cp01.baidu.com:8020/weisai/xsp/format_log/2015-04-10";
		String output = "hdfs://cp01-ma-eval-001.cp01.baidu.com:8020/weisai/back_search_rate/output/";
		
		Configuration conf = getConf();
			
	    if (true)
	    {
	        conf.set("fs.defaultFS", "hdfs://cp01-ma-eval-001.cp01.baidu.com:8020");
	        
	        conf.set("mapreduce.framework.name", "yarn");
	        conf.set("yarn.resourcemanager.address", "cp01-ma-eval-001.cp01.baidu.com:8032");	
	        conf.set("yarn.resourcemanager.resource-tracker.address", "cp01-ma-eval-001.cp01.baidu.com:8031");
	        conf.set("yarn.resourcemanager.scheduler.address", "cp01-ma-eval-001.cp01.baidu.com:8030");
	        
	        conf.set("mapreduce.jobhistory.address", "cp01-ma-eval-001.cp01.baidu.com:8044");
	        conf.set("mapreduce.jobhistory.webapp.address", "cp01-ma-eval-001.cp01.baidu.com:8045");
	        conf.set("mapreduce.jobhistory.done-dir", "/history/done");
	        conf.set("mapreduce.jobhistory.intermediate-done-dir", "/history/done_intermediate");

	        conf.set("mapreduce.app-submission.cross-platform", "true");
	        conf.set("mapreduce.job.am-access-disabled", "true");
	    }
	    

		String curData = input.substring(input.lastIndexOf("/") + 1);
		conf.set("CURRENT_DATE", curData);
	    
        File jarFile = EJob.createTempJar("bin");  
        EJob.addClasspath("/usr/hadoop/conf");  
        ClassLoader classLoader = EJob.getClassLoader();  
        Thread.currentThread().setContextClassLoader(classLoader);  
        
		FileSystem fs = null;
		try {
			fs = FileSystem.get(new URI(input), conf);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
				
	    URI uri = new URI(output);
	    if (fs.delete(new Path(uri), true))
	    {
	    	System.out.println("Delete output dir first");
	    }
	
	    Job job = Job.getInstance(conf, "BackSearchRate");

	    job.setJar(jarFile.toString());
		job.setJarByClass(BackSearchRateEntry.class);
		//job.setMapperClass(BackSearchRateMapper.class);
		job.setMapperClass(RawDataBackSearchRateMapper.class);
		job.setPartitionerClass(BackSearchRatePartitioner.class);
		job.setReducerClass(BackSearchRateReducer.class);
		
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(NullWritable.class);
		
		FileInputFormat.setInputPaths(job, new Path(input));
		FileOutputFormat.setOutputPath(job, new Path(output));
		
		job.setNumReduceTasks(10);
		job.setInputFormatClass(TextInputFormat.class);
		
		if (!job.waitForCompletion(true))
			return -1;
		
		return 0;
	}

}

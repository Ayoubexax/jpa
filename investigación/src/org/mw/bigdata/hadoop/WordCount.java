package org.mw.bigdata.hadoop;

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;

import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/**
 * https://www.tutorialspoint.com/map_reduce/map_reduce_combiners.htm

   cd /home/hadoop
   mkdir units
   cp files hadoop-core-1.2.1.jar, WordCount.java, ProcessUnits.java, input.txt, sample.txt to /home/hadoop
   javac -classpath hadoop-core-1.2.1.jar -d units *.java
   jar -cvf units.jar -C units/ .

   start-dfs.sh
   start-yarn.sh
   hadoop fs -mkdir -p input_dir    # 1st time need -p, subsequently -mkdir calls w/o -p is okay
   hadoop fs -put /home/hadoop/input.txt input_dir
   hadoop fs -ls input_dir/
   hadoop fs -rmdir output_dir/
   hadoop fs -rm -r output_dir/        # output_dir must not exist before execute the following command 

   hadoop jar units.jar org.mw.bigdata.hadoop.WordCount input_dir output_dir

   hadoop fs -ls output_dir/
   hadoop fs -cat output_dir/part-r-00000

   hadoop fs -get input_dir    ~/Downloads/
   hadoop fs -get input_dir/*  ~/Downloads/
   hadoop fs -get output_dir   ~/Downloads/
   hadoop fs -get output_dir/* ~/Downloads/

   Input data: input.txt
 */
public class WordCount {

   private static final Log LOG = LogFactory.getLog(WordCount.class);

   public static class TokenizerMapper extends Mapper<Object, Text, Text, IntWritable> {

      private final static IntWritable one = new IntWritable(1);
      private Text word = new Text();

      public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
         LOG.info("map - key: '" + key + "', value: '" + value + "'"); // write to hadoop user log, e.g. /j01/srv/hadoop/logs/userlogs/application_../container_.../syslog
                                                                       // use 'egrep.sh -R -n "map - key:" *' to search
         StringTokenizer itr = new StringTokenizer(value.toString());
         while (itr.hasMoreTokens()) {
            word.set(itr.nextToken());
            LOG.info("map - token: " + word.toString());
            context.write(word, one); // how about duplicate words in same line?
         }
      }
   }

   public static class IntSumReducer extends Reducer<Text,IntWritable,Text,IntWritable> {
      private IntWritable result = new IntWritable();
      public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
         LOG.info("reduce - key: '" + key);
         int sum = 0;
         for (IntWritable val : values) {
            LOG.info("reduce - val: '" + val);
            sum += val.get();
         }
         result.set(sum);
         context.write(key, result);
      }
   }

   /**
    * hadoop jar units.jar org.mw.bigdata.hadoop.WordCount input_dir output_dir
    *
    * @param args input_dir output_dir
    * @throws Exception
    */
   public static void main(String[] args) throws Exception {
      Configuration conf = new Configuration();
      Job job = Job.getInstance(conf, "word count");

      job.setJarByClass(WordCount.class);

      job.setMapperClass(TokenizerMapper.class);
      job.setCombinerClass(IntSumReducer.class);
      job.setReducerClass(IntSumReducer.class);

      job.setOutputKeyClass(Text.class);
      job.setOutputValueClass(IntWritable.class);

      FileInputFormat.addInputPath(job, new Path(args[0]));
      FileOutputFormat.setOutputPath(job, new Path(args[1]));

      System.exit(job.waitForCompletion(true) ? 0 : 1);
   }
}

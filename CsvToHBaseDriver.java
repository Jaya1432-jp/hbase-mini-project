import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableOutputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;

public class CsvToHBaseDriver {

    public static void main(String[] args) throws Exception {

        Configuration conf = HBaseConfiguration.create();

        conf.set(TableOutputFormat.OUTPUT_TABLE, "employees");

        Job job = Job.getInstance(conf, "CSV to HBase Loader");

        job.setJarByClass(CsvToHBaseDriver.class);

        job.setInputFormatClass(TextInputFormat.class);

        FileInputFormat.addInputPath(
            job,
            new Path(args[0])
        );

        job.setMapperClass(CsvToHBaseMapper.class);

        job.setMapOutputKeyClass(
            ImmutableBytesWritable.class
        );

        job.setMapOutputValueClass(Put.class);

        job.setOutputFormatClass(
            TableOutputFormat.class
        );

        job.setNumReduceTasks(0);

        TableMapReduceUtil.addDependencyJars(job);

        System.exit(
            job.waitForCompletion(true) ? 0 : 1
        );
    }
}

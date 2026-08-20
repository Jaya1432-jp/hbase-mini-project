import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import java.io.IOException;

public class CsvToHBaseMapper extends Mapper<LongWritable, Text, ImmutableBytesWritable, Put> {

    @Override
    protected void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException {

        String[] fields = value.toString().split(",");
        if (fields.length < 4) return;

        String empId = fields[0];
        String name = fields[1];
        String dept = fields[2];
        String salary = fields[3];

        byte[] rowKey = Bytes.toBytes(empId);
        Put put = new Put(rowKey);
        put.addColumn(Bytes.toBytes("personal"), Bytes.toBytes("name"), Bytes.toBytes(name));
        put.addColumn(Bytes.toBytes("work"), Bytes.toBytes("dept"), Bytes.toBytes(dept));
        put.addColumn(Bytes.toBytes("work"), Bytes.toBytes("salary"), Bytes.toBytes(salary));

        context.write(new ImmutableBytesWritable(rowKey), put);
    }
}

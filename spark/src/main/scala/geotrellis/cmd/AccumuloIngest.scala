package climate.cmd

import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.ingest.{Ingest, Pyramid, AccumuloIngestArgs}
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.hadoop.formats._
import geotrellis.spark.utils.SparkUtils
import geotrellis.spark.io.index._

import org.apache.accumulo.core.client.security.tokens.PasswordToken
import org.apache.spark._
import com.quantifind.sumac.ArgMain

object AccumuloIngestCommand extends ArgMain[AccumuloIngestArgs] with Logging {
  def main(args: AccumuloIngestArgs): Unit = {
    System.setProperty("com.sun.media.jai.disableMediaLib", "true")

    implicit val sparkContext = SparkUtils.createSparkContext("Ingest")

    implicit val accumulo = AccumuloInstance(args.instance, args.zookeeper, args.user, new PasswordToken(args.password))
    val source = sparkContext.netCdfRDD(args.inPath).repartition(args.partitions)

    val layoutScheme = ZoomedLayoutScheme(64)
    Ingest[NetCdfBand, SpaceTimeKey](source, args.destCrs, layoutScheme, args.pyramid){ (rdd, level) =>
      val catalog = AccumuloRasterCatalog("metadata")
      catalog.writer[SpaceTimeKey](ZCurveKeyIndexMethod.byYear, args.table).write(LayerId(args.layerName, level.zoom), rdd)
    }
  }
}

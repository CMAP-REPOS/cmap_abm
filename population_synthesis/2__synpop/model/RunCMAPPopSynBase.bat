copy cmap.base.properties cmap.properties
java -Xms500m -Xmx4000m -Dlog4j.configuration=log4j.xml -cp "./*;." com.pb.cmap.synpop.ARCPopulationSynthesizer

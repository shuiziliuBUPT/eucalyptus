import com.eucalyptus.system.SubDirectory;
import com.eucalyptus.entities.PersistenceContexts;
/* this crap is hsqldb specific */
import com.eucalyptus.auth.crypto.Hmacs;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.util.*;

//Component.db.markLocal( );
//Component.db.markEnabled( );
//Component.db.setHostAddress( "127.0.0.1" );
config = "CREATE SCHEMA PUBLIC AUTHORIZATION DBA\n" + 
  "CREATE USER SA PASSWORD \"${Hmacs.generateSystemSignature( )}\"\n" +  
  "GRANT DBA TO SA\n" + 
  "SET WRITE_DELAY 100 MILLIS\n" +  
  "SET LOG_SIZE 10\n" +  
  "SET SCHEMA PUBLIC\n";
PersistenceContexts.list( ).each{ context_name ->
  d = new File("${SubDirectory.DB.toString()}/eucalyptus_${context_name}.script");
  if( !d.exists() ) {
    d.write(config);
  }
}


package cz.ctu.fit.ddw;

import java.io.IOException;

/**
 * @author Milan Dojchinovski
 * <milan (at) dojchinovski (dot) mk>
 * Twitter: @m1ci
 * www: http://dojchinovski.mk
 */
public class GATEEmbedded {

    public static void main(String[] args) throws IOException {
        
        GateClient client = new GateClient();        
        client.run();    
    }
}

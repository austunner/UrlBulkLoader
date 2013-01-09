/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package urlloader;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

/**
 *
 * @author Austunner
 */
public class UrlThread implements Callable<Integer>{
    private String url;
    private HttpClient client;
    private long startTime;
    private long endTime;
    Logger log = Logger.getLogger(UrlThread.class);
    
    public UrlThread(String url, HttpClient client) {
        this.url = url;
        this.client = client;
        
    }
    
    @Override
    public Integer call() {
        startTime = System.currentTimeMillis();
        HttpResponse resp = null;
        HttpGet get = null;
        HttpEntity entity = null;
        try {
            get = new HttpGet(url);
            resp = client.execute(get);
            
            entity = resp.getEntity();
            String content = EntityUtils.toString(entity);
            
            return resp.getStatusLine().getStatusCode();
            
        } catch (IOException ex) {
            log.error("exception", ex);
        } finally {
            if (get != null) {
                
                get.abort();
                try {
                    EntityUtils.consume(entity);
                } catch (Exception ex) {
                    
                }
            }
            
            endTime = System.currentTimeMillis();
            UrlLoader.statsTime.add(endTime - startTime);
        }
        
        return -1;
    }

}

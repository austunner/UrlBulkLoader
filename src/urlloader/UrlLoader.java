/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package urlloader;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.log4j.Logger;

/**
 *
 * @author Austunner
 */
public class UrlLoader {

    private String url;
    private int num;
    
    private String filePath;
    
    Logger log = Logger.getLogger(UrlLoader.class);
    ExecutorService threadpool;
    ThreadSafeClientConnManager connManager;
    DefaultHttpClient client;
    
    static List<Long> statsTime = Collections.synchronizedList(new ArrayList<Long>(0));
    
    public UrlLoader(String file) {
        filePath = file;
        init();
    }
    
    public UrlLoader(String url, int num) {
        this.url = url;
        this.num = num;
        init();
        
    }
    private void init() {
        threadpool = Executors.newCachedThreadPool();
        connManager = new ThreadSafeClientConnManager();
        connManager.setDefaultMaxPerRoute(500);
        connManager.setMaxTotal(500);
        client = new DefaultHttpClient(connManager);
    }
    
    public void invoke() {
        if(StringUtils.isNotBlank(this.url)) {
            invokeSameUrl();
        } else {
            invokeFileUrls();
        }
    }
    
    private void invokeFileUrls() {
        BufferedReader bf;
        List<UrlThread> urls = new ArrayList<UrlThread>(0);
        try {
            bf = new BufferedReader(new FileReader(this.filePath));
            String line = null;
            while ((line = bf.readLine()) != null) {
                if (StringUtils.isNotBlank(line)) {
                    log.debug("urls: " + line);
                    urls.add(new UrlThread(line, client));
                }
            }
            invokeUrls(urls);
            
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(UrlLoader.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    private void invokeSameUrl() {
        List<UrlThread> urls = new ArrayList<UrlThread>(this.num);
        
        for (int i=0; i<this.num; i++) {
            UrlThread thr = new UrlThread(url, client);
            urls.add(thr);
        }
        
        invokeUrls(urls);
    }
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InterruptedException {
        // TODO code application logic here
        
        
        UrlLoader loader = null;
        if (args.length == 2) {
            loader = new UrlLoader(args[0], Integer.parseInt(args[1]));
        } else {
            loader = new UrlLoader(args[0]);
        }
        loader.invoke();
    }

    private void invokeUrls(List<UrlThread> urls) {
        List<Future<Integer>> futures = new ArrayList<Future<Integer>>(0);
        try {
            futures = threadpool.invokeAll(urls, 2000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            log.error("exception", ex);
        }
        
        Map<Integer, Integer> results = new HashMap<Integer, Integer>(0);
        int notDoneCtr = 0;
        for (int i=0; i<futures.size(); i++) {
            try {
                int code = futures.get(i).get();
                if(!futures.get(i).isDone()) {
                    notDoneCtr++;
                }
                if (results.containsKey(code)) {
                    results.put(code, results.get(code) + 1);
                } else {
                    results.put(code, 1);
                }
            } catch (Exception ex) {
                log.error ("Exception getting one of the http response codes", ex);
            } finally {
                futures.get(i).cancel(true);
            }
        }
        
        log.debug("results: " + results);
        log.debug("time: " + statsTime);
        log.debug("tasks not done yet" + notDoneCtr);
        
        threadpool.shutdown();
    }
}

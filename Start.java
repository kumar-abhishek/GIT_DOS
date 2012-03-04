import java.util.*;
import java.io.*;
class Semaphore{
    boolean isAcquired = false;
    int permit = 1 ;
    int waitingId;
    int acquiredId;
    public int semaphoreId;
    Semaphore(int ID){
        this.semaphoreId = ID;
    }
    synchronized void P(int pId){
        while(permit == 0){
            this.waitingId = pId;
            try{
                wait();
            }
            catch(InterruptedException e){
            }
        }
        --permit;
    //    this.acquiredId = semaphoreId;
        this.acquiredId = pId;
        this.waitingId = 0;
    }
    synchronized void V(){
        ++permit;
        this.acquiredId = 0;
        notify();
        //sleep(2);
    }
}

class Process extends Thread{
    private int pid;
    int opTime;
    int sleepTime;
    Semaphore[] pSemaphore;
    Stack<Integer> stack = null;
    static PrintWriter outPut ;
    static{
        try{
            outPut = new PrintWriter(new FileWriter("event.log",true));
        }
        catch(Exception e){
            System.out.println(e);
        }
    }
    Process(int PID,Semaphore[] s, int opTime, int sleepTime){
        this.pid = PID + Start.n-1 ;
        this.pSemaphore = s;
        this.opTime = opTime;
        this.sleepTime = sleepTime;
        

    }
    public void printStatusOfSemaphores(int run,long UnixTime){
        int semId = 1, times = 1;
        int n = Start.n;

        try{
            outPut = new PrintWriter(new FileWriter("event.log",true));
        }
        catch(Exception e){
            System.out.println(e);
        }

        while(semId< n){
            String ss= "";
            for(int i = 1; i <= times; i++){
               int waitRun = run;
               int heldRun = run;
               int heldId ,waitId;
               if(pSemaphore[semId].acquiredId == 0 ){
                    heldId = 0;
                    heldRun = 0;
                }
                else
                    heldId = pSemaphore[semId].acquiredId - (n-1);
                if(pSemaphore[semId].waitingId == 0 ){
                    waitId = 0;
                    waitRun = 0;
                }
                else
                    waitId = pSemaphore[semId].waitingId - (n-1); 
                try{
                   //outPut.print("(s"+ semId +"," + pSemaphore[semId].permit + ",p[" + heldId  + "," + heldRun + "]"+",p["+ waitId +","+ waitRun + "])\t" );
                   ss += "(s"+ semId +"," + pSemaphore[semId].permit + ",p[" + heldId  + "," + heldRun + "]"+",p["+ waitId +","+ waitRun + "])\t" ;
                   //System.out.println("(s"+ semId +"," + pSemaphore[semId].permit + ",p[" + heldId  + "," + heldRun + "]"+",p["+ waitId +","+ waitRun + "])\t" );
                }
                catch(Exception e){
                    System.out.println(e);
                }
                ++semId;
            }
            outPut.print(ss);
            times *= 2;
            try{ 
                outPut.println();
            }
            catch(Exception e) {
            //System.out.println("error");
            }
        }
        try{
            outPut.print("UnixTime="+UnixTime + "\n");
           // System.out.println("UnixTime="+UnixTime + "\n");
        }
        catch(Exception e){
        }
        outPut.close();
    }

    public void run(){
        int m = Start.m;//change it        
        for(int times = 1 ; times <= m ; times++){
            stack = new Stack<Integer>();
            //sleep time
            try {
                sleep(this.sleepTime);
            }
            catch(InterruptedException e){
            }
            //P operations
            int i = this.pid;
            while(i > 1){
                i /= 2;
                stack.push(new Integer(i));
                pSemaphore[i].P(this.pid);
            }

            //op Time
            try {
                sleep(this.opTime);
                long finishTime = System.currentTimeMillis();
                long UnixTime = finishTime - Start.startTime;
                printStatusOfSemaphores(times,UnixTime);
                
                Start.str2Buffer.append("p[" + (this.pid-(Start.n-1)) + "-" + times + "]:" + UnixTime + "\n");  
            }
            catch(InterruptedException e){
            }

            //V operations
            while(!stack.empty()){
                Integer sId = stack.pop();
                pSemaphore[sId.intValue()].V();
                try{
                    this.sleep(2);
                }
                catch(Exception e){
                }
            }
        }
    }
        private void status(String msg){
            System.out.println("Process id: " + pid + ": " + msg + "\n");   
        }
}

class Start{
    public static int n,m;
    public static long startTime;
    public static int[] opTimes,sleepTimes;
    public static Semaphore[] semaphore;
    public static Process[] process;
    public static StringBuffer str2Buffer;
    public static PrintWriter outPut = null;



    //parse system.properties file
    private static final String PROP_FILE="system.properties";  
    public static void readPropertiesFile(){  
        try{  
            InputStream is = Start.class.getResourceAsStream(PROP_FILE);  
            Properties prop = new Properties();  
            prop.load(is);  
            n = Integer.parseInt(prop.getProperty("n"));  
            m = Integer.parseInt(prop.getProperty("m"));  

            opTimes = new int[n+1];
            sleepTimes = new int[n+1];

            //scan opTimes and sleepTimes
            for(int i =1;i<=n;i++){
                String opTimeParse = "P"+i+".opTime";
                String sleepTimeParse = "P"+i+".sleepTime";
                opTimes[i] = Integer.parseInt((prop.getProperty(opTimeParse)).trim());
                sleepTimes[i] = Integer.parseInt((prop.getProperty(sleepTimeParse)).trim());  
            }
            is.close();
        }catch(Exception e){  
            System.out.println("Failed to read from " + PROP_FILE + " file." + e);  
        }  
    }

    public static void main(String[] args){
        str2Buffer = new StringBuffer();

        try{
            outPut = new PrintWriter(new FileWriter("event.log",true));
        }
        catch(Exception e){
            System.out.println(e);
        }

        readPropertiesFile();
        semaphore = new Semaphore[n];
        process = new Process[n+1];
        for(int i = 1; i < n ; i++)
            semaphore[i] = new Semaphore(i);
        for(int i = 1; i <= n ; i ++) {
            process[i] = new Process(i,semaphore,opTimes[i],sleepTimes[i]);
        }

        startTime = System.currentTimeMillis();
        for(int i = 1 ; i <= n ; i++)
            process[i].start();

        try{
//            System.out.println("Waiting to join");
            for(int i = 1 ; i <= n ; i++)
                process[i].join();

            //System.out.println(str2Buffer.toString());
            outPut.write("\nsequence:\n");
            outPut.write(str2Buffer.toString());
            outPut.close();

        }
        catch(Exception e){
        }
    }
}

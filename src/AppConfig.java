public class AppConfig {
    private String appName,version,logLevel;
    private static volatile AppConfig instance=null;
    private AppConfig(){
    }
    public static AppConfig getInstance(){
        if(instance==null){
            synchronized (AppConfig.class){
                if(instance==null){
                    instance=new AppConfig();
                }
            }
        }
        return instance;
    }
    
}
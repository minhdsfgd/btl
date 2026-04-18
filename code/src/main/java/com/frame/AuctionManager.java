public class AuctionManager{
    private static AuctionManager instance;
    priate AuctionManager(){}
    public static AuctionManager getInstance(){
        if (instance == null){
            instance = new AuctionManager();
        }
        return instance;
    }
}
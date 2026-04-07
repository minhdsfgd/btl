public class BankAccount {
    private final String accountNumber;
    private double balance;
    private String ownerName;
    public BankAccount(String accountNumber, String ownerName){
        this.accountNumber=accountNumber;
        this.ownerName= ownerName;
        this.balance=0.0;
    }
    public BankAccount(String accountNumber, String ownerName, double balance){
        this.accountNumber=accountNumber;
        this.ownerName= ownerName;
        if (balance<0){
            this.balance=0.0;
            System.out.println("Lỗi số dư < 0");
        } else {
            this.balance= balance;
        }
    }
//nạp tiền
    public void deposit(double amount){
        if (amount>0){
            this.balance+=amount;
            System.out.println("Nạp thành công "+amount+". Số dư hiện tại: "+ this.balance);

        }else{
            System.out.println("Lỗi nạp tiền: Số tiền nạp phải lớn hơn 0");
        }
    }
//rút tiền
    public boolean withdraw(double amount){
        if (amount<=0){
            System.out.println("Số tiền rút ra phải > 0");
            return false;
        }
        if (amount> this.balance){
            System.out.println("Số dư không đủ");
            return false;
        }
        this.balance=this.balance- amount;
        System.out.println("Rút thành công: "+ amount +". Số dư hiện tại: "+this.balance);
        return true;
    }
//xem số dư
    public double getBalance(){
        return this.balance;
    }
    public void HelloWorld(){
        System.out.println("Hello World");
    }
}


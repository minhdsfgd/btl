public class Solution{
    int gcd(int a, int b){
        if (a == b) return a;
        return gcd(b,Math.abs(a-b));
    }
    public static void main(String[] args){
        Solution sl = new Solution();
        System.out.println(sl.gcd(-5,10));
    }
}
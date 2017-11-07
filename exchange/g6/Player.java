package exchange.g6;

import exchange.sim.Offer;
import exchange.sim.Request;
import exchange.sim.Sock;
import exchange.sim.Transaction;

import java.util.*;

public class Player extends exchange.sim.Player {
    /*
        Inherited from exchange.sim.Player:
        Random random   -       Random number generator, if you need it

        Remark: you have to manually adjust the order of socks, to minimize the total embarrassment
                the score is calculated based on your returned list of getSocks(). Simulator will pair up socks 0-1, 2-3, 4-5, etc.
     */
    private int id1, id2, id, n;
    private Sock[] socks;
    private Sock median;
    private double max_dist1;//for stage 0 
    private double max_dist2;//for stage 0
    private int no_txn = 0; //count the number times with no txns 
    private int pair_offer = 0; //identify stages: pair_offer = 1 for stage1

    private double dist = 70;
    private ArrayList<Sock> median_list;

    private int num_cluster;
    private boolean txn = true;

    private int t;
    private int t_counter;

    //private HashMap<Double, Sock> offerCandidate;
    private Map<Double, Sock> treeMap;

    private Sock lastRequestSock1, lastRequestSock2;
    private List<Sock> wishList;
    private int wish_comeTrue = 0;

    @Override
    public void init(int id, int n, int p, int t, List<Sock> socks) {
        this.id = id;
        this.n = n;
        this.socks = (Sock[]) socks.toArray(new Sock[2 * n]);
        this.median = new Sock(128, 128, 128);
        //this.median_list = new ArrayList<Sock>();
        this.max_dist1 = -1;
        this.max_dist2 = -1;
        this.t = t;
        this.t_counter = 0;
        this.num_cluster = 4;
        this.num_cluster = 2;
        this.socks = pairSocks(this.socks);
        this.median_list = new ArrayList<Sock>();
        this.wishList = new ArrayList<Sock>();
        this.treeMap = new TreeMap<Double, Sock>();
    }

    @Override
    public Offer makeOffer(List<Request> lastRequests, List<Transaction> lastTransactions) {
        /*
            lastRequests.get(i)     -       Player i's request last round
            lastTransactions        -       All completed transactions last round.
         */
        //increment no_txn
        //socks = pairSocks(socks);
        // for(int i = 0; i < socks.length;){
        //     System.out.print(socks[i].distance(socks[i+1]) + " ");
        //     i+=2;
        // }

        if(this.txn == true){
            socks = pairSocks(socks);
            this.txn = false;
        }
        if(this.txn == false && this.pair_offer == 1){
            if(!this.wishList.contains(this.lastRequestSock1)) this.wishList.add(this.lastRequestSock1);
            //if(!this.wishList.contains(this.lastRequestSock2)) this.wishList.add(this.lastRequestSock2); 
        }
        while(wishList.size() > 6){
            wishList.remove(0);
        }
        if(this.t_counter == 0){
            this.median_list.clear();
            this.treeMap.clear();
            ArrayList<Sock> arrayList_socks = new ArrayList<Sock>(Arrays.asList(socks));
            DBSCANClusterer clusterer = new DBSCANClusterer(arrayList_socks, 2, this.dist);
            ArrayList<ArrayList<Sock>> result = clusterer.performClustering();
            while (result.size() < this.num_cluster) {
                this.dist = this.dist - 5;
                clusterer = new DBSCANClusterer(arrayList_socks, 2, this.dist);
                result = clusterer.performClustering();
            }
            for (int i = 0; i < result.size(); ++i) {
                Sock tmp_median = getMedianSock(result.get(i));
                this.median_list.add(tmp_median);
                for (int j = 0; j < result.get(i).size(); j++) {
                    treeMap.put(tmp_median.distance(result.get(i).get(j)),result.get(i).get(j));
                }
                    
            }
        }
        this.t_counter += 1;
        this.no_txn += 1;
        //if stage 1
         if(this.t_counter == this.t-1){
            System.out.println("wishcome true: " + this.wish_comeTrue);
        }
        
        if (this.no_txn <= 4 && this.wishList.size() != 0) {
            List<Sock> tmp_list = new ArrayList<Sock>(treeMap.values());
            int ind = tmp_list.size()-1;
            int ind1 = tmp_list.size()-1;
            int ind2 = tmp_list.size()-1;
            Sock cand1 = tmp_list.get(ind);
            while(Arrays.asList(socks).indexOf(cand1) < n/5){
                ind -= 1;
                if(ind == -1){
                    ind = tmp_list.size()-1;
                }
                cand1 = tmp_list.get(ind);

            }
            this.id1 = Arrays.asList(socks).indexOf(cand1);
            ind -=1;
            Sock cand2 = tmp_list.get(ind);
            while(Arrays.asList(socks).indexOf(cand2) < n/5){
                ind -= 1;
                if(ind == -1){
                    ind = tmp_list.size()-1;
                }
                cand2 = tmp_list.get(ind);
            }
            this.id2 = Arrays.asList(socks).indexOf(cand2);
            if(this.id1 == this.id2 && this.id1 != socks.length-1){
                this.id2 = socks.length-1;
            }
            else if(this.id1 == this.id2){
                this.id2 = socks.length-2;
            }
            this.pair_offer = 0;
           
            return new Offer(socks[this.id1], socks[this.id2]);
            
        } else {//this is stage 1, we do clustering
            
            if (this.no_txn == 0) this.no_txn = 4;
            //Use Blossom method to pair up socks
            this.id1 = socks.length - ((this.no_txn - 3) / 3) * 2 - 1;
            this.id2 = socks.length - ((this.no_txn - 3) / 3) * 2 - 2;
            if (this.id1 <= socks.length*0.2) {
                this.id1 = socks.length - 1;
                this.id2 = socks.length - 2;
                this.no_txn = 4;
            }
            
            this.pair_offer = 1; // this is stage 1
            return new Offer(this.socks[this.id1], this.socks[this.id2]);
        }
        //return null;
    }

    @Override
    public Request requestExchange(List<Offer> offers) {
        /*
            offers.get(i)           -       Player i's offer
            For each offer:
            offer.getSock(rank = 1, 2)      -       get rank's offer
            offer.getFirst()                -       equivalent to offer.getSock(1)
            offer.getSecond()               -       equivalent to offer.getSock(2)

            Remark: For Request object, rank ranges between 1 and 2
         */

        int c = 0;
        double min_dist1 = 450;
        double min_dist2 = 450;
        int min1 = -1;
        int min2 = -1;
        int rank1 = -1;
        int rank2 = -1;

        if(c == 0 && this.pair_offer == 1){

            double curEm = getEmbarassment(socks);
            double curMin1 = curEm;
            double curMin2 = curEm;
            for(int j = 1; j <= 2; ++j){
                for (int i = 0; i < offers.size(); i++) {
                   if (i == id) continue;
                    double firstEmbarassmentOne = -1;
                    Sock s = offers.get(i).getSock(j);
                    if (s != null) {
                        firstEmbarassmentOne = updatedEmbarassment(s, id1);
                        if (curMin1 > firstEmbarassmentOne) {
                            curMin2 = curMin1;
                            curMin1 = firstEmbarassmentOne;
                            min2 = min1;
                            min1 = i;
                            rank2 = rank1;
                            rank1 = j;
                            c++;
                            this.lastRequestSock2 = this.lastRequestSock1;
                            this.lastRequestSock1 = s;
                        } else if (curMin2 > firstEmbarassmentOne) {
                            min_dist2 = firstEmbarassmentOne;
                            min2 = i;
                            rank2 = 1;
                            c++;
                            this.lastRequestSock2 = s;
                        }
                    }
                }
                if(c >= 2) break; 
            }
        }
        if(c == 0){
            for(int j = 1; j <=2; j++){
                for (int i = 0; i < offers.size(); i++) {
                    if (i == id) continue;
                    Sock s = offers.get(i).getSock(j);
                    if (s != null && this.wishList.contains(s)) {
                        if(min1 == -1){
                            min1 = i;
                            rank1 = j;
                            c+=1;
                        }
                        else if(min2 ==-1){
                            min2 = i;
                            rank2 = j;
                            c+=1;
                        }
                    }
                    if(c >= 2) {
                        break;
                    }
                }
                
            }
        }    

        if (c >= 2) {
            return new Request(min1, rank1, min2, rank2);
             
        } else if (c == 1) {
            if (min1 == -1) {
                return new Request(min2, rank2, -1, -1);
                
            } else {
                return new Request(min1, rank1, -1, -1);
                
            }
        } else {
            return new Request(-1, -1, -1, -1);
        }

    }

    @Override
    public void completeTransaction(Transaction transaction) {
        /*
            transaction.getFirstID()        -       first player ID of the transaction
            transaction.getSecondID()       -       Similar as above
            transaction.getFirstRank()      -       Rank of the socks for first player
            transaction.getSecondRank()     -       Similar as above
            transaction.getFirstSock()      -       Sock offered by the first player
            transaction.getSecondSock()     -       Similar as above

            Remark: rank ranges between 1 and 2
         */

        this.no_txn = 0;
        this.txn = true;
        int rank;
        Sock newSock;
        Sock oldSock;
        if (transaction.getFirstID() == id) {
            rank = transaction.getFirstRank();
            newSock = transaction.getSecondSock();
            oldSock = transaction.getFirstSock();
        } else {
            oldSock = transaction.getSecondSock();
            rank = transaction.getSecondRank();
            newSock = transaction.getFirstSock();
        }
        List<Sock> tmp_list_v = new ArrayList<Sock>(this.treeMap.values());
        double old_dis = -1;
        double dis = -1;
        for(Sock s:median_list){
            if(dis == -1 || s.distance(newSock) < dis){
                dis = s.distance(newSock);
            }
            if(old_dis == -1 || s.distance(oldSock) < old_dis){
                old_dis = s.distance(oldSock);
            }
        }
        this.treeMap.remove(old_dis);
        this.treeMap.put(dis, newSock);
        if (this.wishList.contains(newSock)) {
            this.wishList.remove(newSock);
            this.wish_comeTrue+=1;
        }

        if (rank == 1) socks[id1] = newSock;
        else socks[id2] = newSock;


    }

    @Override
    public List<Sock> getSocks() {
        
        return Arrays.asList(pairSocks(socks));
        //return Arrays.asList(socks);
    }

    public Sock[] pairSocks(Sock[] s) {
        int[] match = new Blossom(getCostMatrix(s), true).maxWeightMatching();
        List<Pair> pairs = new LinkedList<Pair>();
        List<Sock> result = new ArrayList<Sock>();
        for (int i = 0; i < match.length; i++) {
            if (match[i] < i) continue;
            pairs.add(new Pair(s[i], s[match[i]]));
        }

        Pair[] pairArr = pairs.toArray(new Pair[n]);

        Arrays.sort(pairArr);
        for (int i = 0; i < n; i++) {
            result.add(pairArr[i].u);
            result.add(pairArr[i].v);
        }

        return result.toArray(new Sock[2 * n]);
    }

    public Sock getMedianSock(ArrayList<Sock> input) {
        List<Integer> sock_R = new ArrayList<Integer>();
        List<Integer> sock_G = new ArrayList<Integer>();
        List<Integer> sock_B = new ArrayList<Integer>();
        for (Sock s : input) {
            sock_R.add(s.R);
            sock_G.add(s.G);
            sock_B.add(s.B);
        }

        Collections.sort(sock_R);
        Collections.sort(sock_G);
        Collections.sort(sock_B);

        int mid_R = sock_R.get(input.size() / 2);
        int mid_G = sock_G.get(input.size() / 2);
        int mid_B = sock_B.get(input.size() / 2);

        return new Sock(mid_R, mid_G, mid_B);
    }

    private float[][] getCostMatrix(Sock[] s) {
        float[][] matrix = new float[s.length * (s.length - 1) / 2][3];
        int idx = 0;
        for (int i = 0; i < s.length; i++) {
            for (int j = i + 1; j < s.length; j++) {
                matrix[idx] = new float[]{i, j, (float) (-s[i].distance(s[j]))};
                idx++;
            }
        }
        return matrix;
    }

    private double updatedEmbarassment(Sock newSock, int replaceIndex) {
        Sock[] copy = socks.clone();
        copy[replaceIndex] = newSock;
        copy = pairSocks(copy);
        return getEmbarassment(copy);
    }

    private double getEmbarassment(Sock[] s) {
        double result = 0;
        for (int i = 0; i < s.length; i += 2) {
            result += s[i].distance(s[i + 1]);
        }

        return result;
    }

    // public void pairSocksGreedy() {
    //     List<Sock> next = new ArrayList<Sock>();
    //     for (int i = 0; i < socks.length; ++i) {
    //         next.add(new Sock(socks[i].R, socks[i].G, socks[i].B));
    //     }

    //     int c = 0;
    //     ArrayList<Point> tmp_list = new ArrayList<Point>();
    //     for (int i = 0; i < next.size(); ++i) {
    //         tmp_list.add(new Point(next.get(i).R, next.get(i).G, next.get(i).B));
    //     }

    //     while (c < socks.length - 1) {
    //         ClosestPair closep = new ClosestPair(tmp_list);
    //         closep.SolveByDivideAndConquer();
    //         Sock s1 = new Sock(closep.p1.x, closep.p1.y, closep.p1.z);
    //         // System.out.println("C is: " + c);
    //         socks[c] = s1;
    //         c += 1;
    //         Sock s2 = new Sock(closep.p2.x, closep.p2.y, closep.p2.z);
    //         socks[c] = s2;
    //         c += 1;
    //         ArrayList<Point> tmp_1 = new ArrayList<Point>();
    //         for (int i = 0; i < tmp_list.size(); ++i) {
    //             if (tmp_list.get(i).x == closep.p1.x && tmp_list.get(i).y == closep.p1.y && tmp_list.get(i).z == closep.p1.z)
    //                 continue;
    //             if (tmp_list.get(i).x == closep.p2.x && tmp_list.get(i).y == closep.p2.y && tmp_list.get(i).z == closep.p2.z)
    //                 continue;
    //             tmp_1.add(new Point(tmp_list.get(i).x, tmp_list.get(i).y, tmp_list.get(i).z));
    //         }
    //         tmp_list = tmp_1;

    //     }
    // }

}
